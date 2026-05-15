package top.xihale.xdocs.websocket;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 房间管理器（单例）。
 * <p>
 * 管理 房间→Session 集合 的映射，提供 join/leave/broadcast/dedup 操作。
 * 供 ChatWebSocket 和 CollaborationWebSocket 共用。
 * <p>
 * 使用方式：通过 {@link #getInstance()} 获取单例。
 */
public class RoomManager {

    private static final RoomManager INSTANCE = new RoomManager();

    /** 房间 ID → 该房间内所有 Session */
    private final ConcurrentHashMap<String, Set<Session>> rooms = new ConcurrentHashMap<>();

    /** 房间 ID → 用户 ID → 当前有效 Session ID（同用户去重） */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> activeUserSessions = new ConcurrentHashMap<>();

    /** 已被某个客户端声明初始化的协同房间。房间清空时重置。 */
    private final Set<String> bootstrappedRooms = ConcurrentHashMap.newKeySet();

    private RoomManager() {}

    public static RoomManager getInstance() {
        return INSTANCE;
    }

    // ==================== Room operations ====================

    /**
     * 将 session 加入房间。
     * @return 房间内当前 session 数量
     */
    public int join(String roomId, Session session) {
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        Set<Session> room = rooms.get(roomId);
        return room != null ? room.size() : 0;
    }

    /**
     * 将 session 从房间移除。房间为空时自动清理。
     */
    public void leave(String roomId, Session session) {
        Set<Session> roomSessions = rooms.get(roomId);
        if (roomSessions == null) return;
        roomSessions.remove(session);
        if (roomSessions.isEmpty()) {
            rooms.remove(roomId);
            activeUserSessions.remove(roomId);
            bootstrappedRooms.remove(roomId);
        }
    }

    /**
     * 获取房间内 session 数量
     */
    public int getRoomSize(String roomId) {
        Set<Session> room = rooms.get(roomId);
        return room != null ? room.size() : 0;
    }

    /**
     * 获取房间内所有 session
     */
    public Set<Session> getRoomSessions(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 房间是否存在且非空
     */
    public boolean hasRoom(String roomId) {
        Set<Session> room = rooms.get(roomId);
        return room != null && !room.isEmpty();
    }

    // ==================== Bootstrap claim ====================

    /**
     * 声明当前客户端负责把数据库中的初始内容写入 Yjs 文档。
     * 同一房间只允许一个客户端成功；房间清空后重置。
     */
    public boolean claimBootstrap(String roomId, int userId) {
        // 如果房间实际上已经空了，允许下一次打开重新从数据库初始化。
        if (!hasRoom(roomId)) {
            bootstrappedRooms.remove(roomId);
            return bootstrappedRooms.add(roomId);
        }

        // 快速刷新时，旧 WebSocket 可能还没来得及 close。
        // 如果房间里只有当前用户的旧连接，允许新页面重新初始化，随后旧连接会被去重关闭。
        ConcurrentHashMap<Integer, String> roomActive = activeUserSessions.get(roomId);
        if (roomActive != null && roomActive.size() == 1 && roomActive.containsKey(userId)) {
            bootstrappedRooms.remove(roomId);
            return bootstrappedRooms.add(roomId);
        }

        return bootstrappedRooms.add(roomId);
    }

    // ==================== User dedup ====================

    /**
     * 绑定用户到房间，返回该用户之前的 session（如果存在）。
     * 用于同用户刷新/重连时踢掉旧 session。
     *
     * @return 旧的 Session 实例，如果没有则返回 null
     */
    public Session bindActiveSession(String roomId, int userId, Session currentSession) {
        ConcurrentHashMap<Integer, String> roomActive = activeUserSessions.computeIfAbsent(
                roomId, k -> new ConcurrentHashMap<>()
        );
        String previousSessionId = roomActive.put(userId, currentSession.getId());
        if (previousSessionId == null || previousSessionId.equals(currentSession.getId())) {
            return null;
        }

        Set<Session> roomSessions = rooms.get(roomId);
        if (roomSessions == null) return null;
        for (Session s : roomSessions) {
            if (previousSessionId.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /**
     * 解绑用户 session。仅在 session ID 匹配时移除（防止误删新 session）。
     */
    public void unbindActiveSession(String roomId, Integer userId, Session closingSession) {
        if (userId == null) return;
        ConcurrentHashMap<Integer, String> roomActive = activeUserSessions.get(roomId);
        if (roomActive == null) return;
        roomActive.computeIfPresent(userId, (key, sessionId) ->
                sessionId.equals(closingSession.getId()) ? null : sessionId
        );
        if (roomActive.isEmpty()) {
            activeUserSessions.remove(roomId, roomActive);
        }
    }

    /**
     * 关闭旧 session（用于同用户去重）。
     * 标记 replaced=true，从房间移除，然后 close。
     */
    public void closeReplacedSession(Session previousSession) {
        if (previousSession == null || !previousSession.isOpen()) return;
        previousSession.getUserProperties().put("replaced", true);
        // 从房间移除（需要在 close 前移除，避免 CME）
        String roomId = (String) previousSession.getUserProperties().get("docId");
        if (roomId == null) roomId = (String) previousSession.getUserProperties().get("articleId");
        if (roomId != null) {
            Set<Session> roomSessions = rooms.get(roomId);
            if (roomSessions != null) roomSessions.remove(previousSession);
        }
        try {
            previousSession.close(new CloseReason(
                    CloseReason.CloseCodes.NORMAL_CLOSURE,
                    "Replaced by a newer session"
            ));
        } catch (IOException ignored) {}
    }

    // ==================== Broadcast ====================

    /**
     * 向房间内所有 session 广播文本消息（排除 sender）
     */
    public void broadcastText(String roomId, String message, Session exclude) {
        Set<Session> roomSessions = rooms.get(roomId);
        if (roomSessions == null) return;
        for (Session s : roomSessions) {
            if (s.isOpen() && (exclude == null || !s.getId().equals(exclude.getId()))) {
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 向房间内所有 session 广播文本消息
     */
    public void broadcastText(String roomId, String message) {
        broadcastText(roomId, message, null);
    }

    /**
     * 向房间内所有 session 广播二进制消息（排除 sender）
     */
    public void broadcastBinary(String roomId, byte[] message, Session exclude) {
        Set<Session> roomSessions = rooms.get(roomId);
        if (roomSessions == null) return;

        ByteBuffer buf = ByteBuffer.wrap(message);
        for (Session s : roomSessions) {
            if (s != exclude && s.isOpen()) {
                try {
                    s.getBasicRemote().sendBinary(buf.duplicate());
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 向房间内所有 session 广播二进制消息
     */
    public void broadcastBinary(String roomId, byte[] message) {
        broadcastBinary(roomId, message, null);
    }
}
