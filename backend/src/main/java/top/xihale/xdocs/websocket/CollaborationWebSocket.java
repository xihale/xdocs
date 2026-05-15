package top.xihale.xdocs.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.UserService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 文档协同编辑 WebSocket 端点（纯中继模式）
 * <p>
 * 后端不维护 Yjs 文档状态，仅负责：
 * 1. 消息转发（relay）：将收到的 Sync/Awareness 消息转发给房间内其他用户
 * 2. 用户去重：同一用户刷新/重连时踢掉旧 session
 * 3. 保持协议透明：不主动构造 Yjs 消息，避免损坏 y-websocket/lib0 编码
 * <p>
 * 同步流程（y-websocket 协议）：
 * <pre>
 * 新用户加入:
 *   新用户 → SyncStep1 → Server 转发给房间内其他人
 *   其他用户 → SyncStep2 → Server 转发给新用户
 *   新用户 Yjs CRDT 自动合并
 *
 * 编辑:
 *   用户 → Update → Server 转发给其他人
 *
 * 断连:
 *   Server 只移除 session；Awareness 由客户端/provider 生命周期处理
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
        Integer userId = getUserIdOrNull(session);
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

        // 纯中继模式：不要主动构造 sync/awareness 协议包。
        // y-websocket 的消息格式是 lib0 varUint + payload；硬编码 byte[]{0,0}/byte[]{3}
        // 会产生不完整包，前端 readMessage/readSyncMessage 抛 Unexpected end of array。
        // 新客户端会在 onopen 自动发送 SyncStep1，房间内其他客户端收到转发后会回 SyncStep2。

        LOGGER.log(Level.INFO, "User {0} joined doc {1}. Room size: {2}",
                new Object[]{user.getNickname(), docId, roomManager.getRoomSize(docId)});
    }

    @OnMessage
    public void onMessage(byte[] message, Session session, @PathParam("docId") String docId) {
        if (!isValidYWebsocketMessage(message)) {
            LOGGER.log(Level.FINE, "Drop invalid y-websocket message. doc={0}, length={1}",
                    new Object[]{docId, message == null ? 0 : message.length});
            return;
        }

        // Standard y-websocket relay mode.
        // Do not parse/rebuild messages. Forward exact binary frame to other clients in same room.
        roomManager.broadcastBinary(docId, message, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("docId") String docId) {
        Integer userId = getUserIdOrNull(session);
        if (userId != null) {
            roomManager.unbindActiveSession(docId, userId, session);
        }
        roomManager.leave(docId, session);

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

    // ==================== y-websocket frame validation ====================

    /**
     * Validate only outer y-websocket frame shape.
     *
     * y-websocket message format:
     * - 0 Sync: [0, syncMessageType, payload...]
     *   - SyncStep1 [0,0] has no payload
     *   - SyncStep2/Update carry VarUint8Array payload
     * - 1 Awareness: [1, VarUint8Array(awarenessUpdate)]
     * - 2 Auth: [2, payload...]
     * - 3 QueryAwareness: [3]
     *
     * Server is relay, not Yjs decoder. Validation exists only to avoid forwarding
     * known-bad truncated frames like [0,0], which crash frontend lib0 decoder with
     * "Unexpected end of array".
     */
    private static boolean isValidYWebsocketMessage(byte[] message) {
        if (message == null || message.length == 0) return false;

        int messageType = message[0] & 0xFF;
        if (messageType == 3) return message.length == 1;
        if (messageType == 2) return true;

        if (messageType == 0) {
            if (message.length < 2) return false;
            int syncType = message[1] & 0xFF;
            if (syncType > 2) return false;
            if (syncType == 0) return message.length == 2;
            return hasCompleteVarUint8Array(message, 2);
        }

        if (messageType == 1) {
            return hasCompleteVarUint8Array(message, 1);
        }

        return false;
    }

    private static boolean hasCompleteVarUint8Array(byte[] message, int offset) {
        VarUintResult length = readVarUint(message, offset);
        if (!length.valid) return false;
        long end = (long) length.nextOffset + length.value;
        return end == message.length;
    }

    private static VarUintResult readVarUint(byte[] data, int offset) {
        long value = 0;
        int shift = 0;
        int pos = offset;
        while (pos < data.length) {
            int b = data[pos] & 0xFF;
            value |= (long) (b & 0x7F) << shift;
            pos++;
            if ((b & 0x80) == 0) {
                return new VarUintResult(true, value, pos);
            }
            shift += 7;
            if (shift > 63) return new VarUintResult(false, 0, pos);
        }
        return new VarUintResult(false, 0, pos);
    }

    private record VarUintResult(boolean valid, long value, int nextOffset) {}
}
