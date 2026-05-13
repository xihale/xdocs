package top.xihale.xdocs.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import top.xihale.xdocs.util.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通知 WebSocket 端点，用于实时推送通知给在线用户
 * <p>
 * 消息格式 (JSON):
 * <pre>
 * // 服务端推送
 * { "type": "notification", "data": { "id": 1, "title": "...", ... } }
 * { "type": "unread_count", "count": 5 }
 * </pre>
 */
@ServerEndpoint(value = "/api/notification/ws", configurator = WebSocketConfigurator.class)
public class NotificationWebSocket extends BaseWebSocket {

    private static final Logger LOGGER = Logger.getLogger(NotificationWebSocket.class.getName());

    /** 单例引用，供 Service 层调用 */
    private static volatile NotificationWebSocket instance;

    /** userId → 该用户的所有 Session */
    private static final ConcurrentHashMap<Integer, Set<Session>> userSessions = new ConcurrentHashMap<>();

    public static NotificationWebSocket getInstance() {
        return instance;
    }

    @OnOpen
    public void onOpen(Session session) {
        if (!checkOrigin(session)) return;

        // AuthFilter 已在 HTTP 层完成 Cookie JWT 鉴权，userId 已注入 request attribute
        Integer userId = (Integer) session.getUserProperties().get("userId");
        if (userId == null) {
            closeSession(session, "Unauthorized");
            return;
        }

        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        instance = this;
        LOGGER.log(Level.INFO, "Notification WebSocket opened for user {0}", userId);
    }

    @OnClose
    public void onClose(Session session) {
        Integer userId = (Integer) session.getUserProperties().get("userId");
        if (userId != null) {
            Set<Session> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        LOGGER.log(Level.INFO, "Notification WebSocket closed for user {0}", userId);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (isAbnormalClose(throwable)) {
            LOGGER.log(Level.FINE, "Notification WebSocket closed abnormally", throwable);
        } else {
            LOGGER.log(Level.WARNING, "Notification WebSocket error", throwable);
        }
        if (session.isOpen()) {
            try { session.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 向指定用户推送通知
     */
    public void sendToUser(int userId, Map<String, Object> notificationData) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "notification");
        message.put("data", notificationData);
        String json = JsonUtils.toJson(message);

        for (Session s : sessions) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to send notification to user " + userId, e);
                }
            }
        }
    }

    /**
     * 向指定用户推送未读数
     */
    public void sendUnreadCount(int userId, int count) {
        Set<Session> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "unread_count");
        message.put("count", count);
        String json = JsonUtils.toJson(message);

        for (Session s : sessions) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to send unread count to user " + userId, e);
                }
            }
        }
    }
}
