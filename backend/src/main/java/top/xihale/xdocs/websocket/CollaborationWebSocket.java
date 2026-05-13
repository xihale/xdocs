package top.xihale.xdocs.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.UserService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 文档协同编辑 WebSocket 端点（纯中继模式）
 * <p>
 * 后端不维护 Yjs 文档状态，仅负责：
 * 1. 消息转发（relay）：将收到的 Sync/Awareness 消息转发给房间内其他用户
 * 2. 用户去重：同一用户刷新/重连时踢掉旧 session
 * 3. Awareness 清理：用户断连时广播 awareness remove
 * <p>
 * 同步流程（y-websocket 协议）：
 * <pre>
 * 新用户加入:
 *   Server → 广播 SyncStep1 [0,0] 给房间内其他人
 *   其他用户 → 回复 SyncStep2 → Server 转发给新用户
 *   新用户 Yjs CRDT 自动合并
 *
 * 编辑:
 *   用户 → Update [0,2,...] → Server 转发给其他人
 *
 * 断连:
 *   Server → 广播 awareness remove + QueryAwareness [3]
 * </pre>
 * <p>
 * 消息类型（y-websocket 协议）：
 * - 0: Sync (SyncStep1=0, SyncStep2=1, Update=2)
 * - 1: Awareness
 * - 2: Auth
 * - 3: QueryAwareness
 */
@ServerEndpoint(value = "/api/collaboration/{docId}", configurator = WebSocketConfigurator.class)
public class CollaborationWebSocket extends BaseWebSocket {

    private static final Logger LOGGER = Logger.getLogger(CollaborationWebSocket.class.getName());

    private final RoomManager roomManager = RoomManager.getInstance();

    @OnOpen
    public void onOpen(Session session, @PathParam("docId") String docId) {
        if (!checkOrigin(session)) return;

        // AuthFilter 已在 HTTP 层完成鉴权，WebSocketConfigurator 从 Cookie 提取 userId
        Integer userId = (Integer) session.getUserProperties().get("userId");
        if (userId == null) {
            closeSession(session, "Unauthorized");
            return;
        }

        User user = UserService.findUserById(userId);
        session.getUserProperties().put("nickname", user.getNickname());
        session.getUserProperties().put("docId", docId);
        session.setMaxIdleTimeout(600000); // 10 分钟超时

        roomManager.join(docId, session);

        // 同用户去重：踢掉旧 session
        Session previousSession = roomManager.bindActiveSession(docId, userId, session);
        roomManager.closeReplacedSession(previousSession);

        // 向房间内其他人广播 SyncStep1，触发他们向新用户发送 SyncStep2
        roomManager.broadcastBinary(docId, new byte[]{0, 0}, session);

        // 如果房间只有自己（第一个用户），没人会回复 SyncStep2。
        // 发送空 SyncStep2 [0,1,0] 让前端 y-websocket 完成同步握手。
        if (roomManager.getRoomSize(docId) <= 1) {
            try {
                session.getBasicRemote().sendBinary(java.nio.ByteBuffer.wrap(new byte[]{0, 1, 0}));
            } catch (IOException ignored) {}
        }

        // 广播 QueryAwareness，让所有人重新发送 awareness 状态
        roomManager.broadcastBinary(docId, new byte[]{3});

        LOGGER.log(Level.INFO, "User {0} joined doc {1}. Room size: {2}",
                new Object[]{user.getNickname(), docId, roomManager.getRoomSize(docId)});
    }

    @OnMessage
    public void onMessage(byte[] message, Session session, @PathParam("docId") String docId) {
        if (message.length == 0) return;

        int messageType = message[0] & 0xFF;

        if (messageType == 0) {
            // Sync 消息（SyncStep1/SyncStep2/Update）— 纯转发
            roomManager.broadcastBinary(docId, message, session);
        } else if (messageType == 1) {
            // Awareness 消息 — 提取 clientID 供断连清理，然后转发
            extractAndStoreClientId(session, message);
            roomManager.broadcastBinary(docId, message, session);
        } else if (messageType == 3) {
            // QueryAwareness 回复 — 转发
            roomManager.broadcastBinary(docId, message, session);
        }
        // messageType=2 (Auth) 不处理
    }

    @OnClose
    public void onClose(Session session, @PathParam("docId") String docId) {
        Integer userId = (Integer) session.getUserProperties().get("userId");
        roomManager.unbindActiveSession(docId, userId, session);
        roomManager.leave(docId, session);

        // 广播 awareness remove，清理其他客户端的幽灵光标
        Object clientIdObj = session.getUserProperties().get("yjsClientId");
        if (clientIdObj instanceof Long) {
            long clientId = (Long) clientIdObj;
            byte[] removeMsg = buildAwarenessRemoveMessage(clientId);
            if (removeMsg != null) {
                roomManager.broadcastBinary(docId, removeMsg);
            }
        }

        // 广播 QueryAwareness，让剩余用户重新发送 awareness 状态
        roomManager.broadcastBinary(docId, new byte[]{3});

        LOGGER.log(Level.INFO, "Session closed for doc {0}. Room size: {1}",
                new Object[]{docId, roomManager.getRoomSize(docId)});
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (isAbnormalClose(throwable)) {
            LOGGER.log(Level.FINE, "WebSocket closed abnormally", throwable);
        } else {
            LOGGER.log(Level.WARNING, "WebSocket error", throwable);
        }
        if (session.isOpen()) {
            try { session.close(); } catch (IOException ignored) {}
        }
    }

    // ==================== Public API ====================

    /**
     * 获取指定文档房间的在线人数
     */
    public static int getRoomSize(String docId) {
        return RoomManager.getInstance().getRoomSize(docId);
    }

    /**
     * 向指定房间的所有连接发送文本消息
     */
    public static void broadcastText(String docId, String message) {
        RoomManager.getInstance().broadcastText(docId, message);
    }

    // ==================== Awareness Helpers ====================

    /**
     * 从 awareness 消息中提取第一个 clientID 并存储到 session 属性中。
     * <p>
     * awareness 消息格式: [1, VarUint(numClients), VarUint(clientID), VarUint(clock), VarString(state), ...]
     */
    private void extractAndStoreClientId(Session session, byte[] message) {
        try {
            if (message.length < 2) return;
            int offset = 1;
            long numClients = readVarUint(message, offset);
            offset += varUintLength(numClients);
            if (numClients <= 0) return;
            long clientId = readVarUint(message, offset);
            session.getUserProperties().put("yjsClientId", clientId);
        } catch (Exception ignored) {}
    }

    /**
     * Build a y-websocket awareness remove message for a given clientID.
     * <p>
     * Format: [1 (messageAwareness), VarUint(1), VarUint(clientID), VarUint(clock=MAX), VarString("null")]
     */
    private static byte[] buildAwarenessRemoveMessage(long clientId) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(1);
            writeVarUint(out, 1);
            writeVarUint(out, clientId);
            writeVarUint(out, Integer.MAX_VALUE);
            byte[] stateBytes = "null".getBytes("UTF-8");
            writeVarUint(out, stateBytes.length);
            out.write(stateBytes);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // ==================== lib0 encoding helpers ====================

    private static long readVarUint(byte[] data, int offset) {
        long value = 0;
        int shift = 0;
        int pos = offset;
        while (pos < data.length) {
            int b = data[pos] & 0xFF;
            value |= (long) (b & 0x7F) << shift;
            pos++;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return value;
    }

    private static int varUintLength(long value) {
        int len = 0;
        do { len++; value >>>= 7; } while (value > 0);
        return len;
    }

    private static void writeVarUint(ByteArrayOutputStream out, long value) throws IOException {
        do {
            int b = (int) (value & 0x7F);
            value >>>= 7;
            if (value > 0) b |= 0x80;
            out.write(b);
        } while (value > 0);
    }
}
