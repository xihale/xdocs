package top.xihale.xdocs.websocket;

import com.google.gson.reflect.TypeToken;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.ChatService;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.util.HtmlSanitizer;
import top.xihale.xdocs.util.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 文档聊天 WebSocket 端点
 * <p>
 * 消息格式 (JSON):
 * <pre>
 * // 客户端发送
 * { "type": "chat", "content": "消息内容" }
 *
 * // 服务端广播
 * { "type": "chat", "id": 1, "articleId": 1, "senderId": 1,
 *   "senderName": "张三", "senderAvatar": "url", "content": "消息内容",
 *   "createTime": "2026-04-30T22:00:00" }
 * { "type": "system", "content": "张三 加入了聊天" }
 * { "type": "online", "users": [...] }
 * </pre>
 */
@ServerEndpoint(value = "/api/chat/ws/{articleId}", configurator = WebSocketConfigurator.class)
public class ChatWebSocket extends BaseWebSocket {

    private static final Logger LOGGER = Logger.getLogger(ChatWebSocket.class.getName());

    private final RoomManager roomManager = RoomManager.getInstance();

    /** sessionId → 用户信息 */
    private static final Map<String, UserInfo> sessionUserMap = Collections.synchronizedMap(new LinkedHashMap<>());

    public record UserInfo(int userId, String nickname, String avatarUrl) {}

    @OnOpen
    public void onOpen(Session session, @PathParam("articleId") String articleId) {
        if (!checkOrigin(session)) return;

        // AuthFilter 已在 HTTP 层完成鉴权，WebSocketConfigurator 从 Cookie 提取 userId
        Integer userId = (Integer) session.getUserProperties().get("userId");
        if (userId == null) {
            closeSession(session, "Unauthorized");
            return;
        }

        User user = UserService.findUserById(userId);
        UserInfo userInfo = new UserInfo(userId,
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getAvatarUrl());
        sessionUserMap.put(session.getId(), userInfo);

        session.getUserProperties().put("articleId", articleId);

        roomManager.join(articleId, session);

        // 同用户去重：踢掉旧 session
        Session previousSession = roomManager.bindActiveSession(articleId, userId, session);
        roomManager.closeReplacedSession(previousSession);

        // 广播系统消息：xxx 加入了聊天
        Map<String, Object> sysMsg = new LinkedHashMap<>();
        sysMsg.put("type", "system");
        sysMsg.put("content", userInfo.nickname + " 加入了聊天");
        roomManager.broadcastText(articleId, JsonUtils.toJson(sysMsg), session);

        // 广播在线列表
        broadcastOnlineList(articleId);

        LOGGER.log(Level.INFO, "User {0} joined chat room {1}. Room size: {2}",
                new Object[]{userInfo.nickname, articleId, roomManager.getRoomSize(articleId)});
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("articleId") String articleId) {
        UserInfo userInfo = sessionUserMap.get(session.getId());
        if (userInfo == null) return;

        try {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> msg = JsonUtils.getGson().fromJson(message, mapType);
            String type = (String) msg.get("type");

            if ("chat".equals(type)) {
                String content = (String) msg.get("content");
                if (content == null || content.isBlank()) return;
                content = HtmlSanitizer.stripHtml(content);

                // 持久化到数据库
                ChatService.sendMessage(Integer.parseInt(articleId), null, userInfo.userId, 0, content);

                // 广播给房间所有人
                Map<String, Object> chatMsg = new LinkedHashMap<>();
                chatMsg.put("type", "chat");
                chatMsg.put("articleId", Integer.parseInt(articleId));
                chatMsg.put("senderId", userInfo.userId);
                chatMsg.put("senderName", userInfo.nickname);
                chatMsg.put("senderAvatar", userInfo.avatarUrl);
                chatMsg.put("content", content);
                chatMsg.put("createTime", LocalDateTime.now().toString());
                roomManager.broadcastText(articleId, JsonUtils.toJson(chatMsg));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to process chat message", e);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("articleId") String articleId) {
        UserInfo userInfo = sessionUserMap.remove(session.getId());
        Integer userId = (Integer) session.getUserProperties().get("userId");
        boolean replaced = Boolean.TRUE.equals(session.getUserProperties().get("replaced"));
        LOGGER.log(Level.INFO, "Chat WebSocket closed: user={0}, article={1}, replaced={2}, roomSize={3}",
                new Object[]{userId, articleId, replaced, roomManager.getRoomSize(articleId)});
        roomManager.unbindActiveSession(articleId, userId, session);
        roomManager.leave(articleId, session);

        // 被替代的 session 不广播离开消息（已由新 session 的加入消息替代）
        if (userInfo != null && !replaced) {
            Map<String, Object> sysMsg = new LinkedHashMap<>();
            sysMsg.put("type", "system");
            sysMsg.put("content", userInfo.nickname + " 离开了聊天");
            roomManager.broadcastText(articleId, JsonUtils.toJson(sysMsg));
            broadcastOnlineList(articleId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (isAbnormalClose(throwable)) {
            LOGGER.log(Level.FINE, "Chat WebSocket closed abnormally", throwable);
        } else {
            LOGGER.log(Level.WARNING, "Chat WebSocket error", throwable);
        }
        if (session.isOpen()) {
            try { session.close(); } catch (IOException ignored) {}
        }
    }

    // ==================== Public API ====================

    /**
     * 获取房间在线人数
     */
    public static int getRoomSize(String articleId) {
        return RoomManager.getInstance().getRoomSize(articleId);
    }

    /**
     * 获取房间在线用户列表
     */
    public static List<UserInfo> getOnlineUsers(String articleId) {
        Set<Session> room = RoomManager.getInstance().getRoomSessions(articleId);
        if (room == null) return List.of();
        List<UserInfo> users = new ArrayList<>();
        for (Session s : room) {
            UserInfo info = sessionUserMap.get(s.getId());
            if (info != null) users.add(info);
        }
        return users;
    }

    // ==================== Internal ====================

    private void broadcastOnlineList(String articleId) {
        List<UserInfo> onlineUsers = getOnlineUsers(articleId);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "online");
        msg.put("users", onlineUsers.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.userId);
            m.put("nickname", u.nickname);
            m.put("avatarUrl", u.avatarUrl);
            return m;
        }).toList());
        roomManager.broadcastText(articleId, JsonUtils.toJson(msg));
    }
}
