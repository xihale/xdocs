package top.xihale.xdocs.websocket;

import jakarta.websocket.*;
import top.xihale.xdocs.config.WebConfig;
import top.xihale.xdocs.util.JwtUtil;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket 端点基类，提供公共的 CORS 校验、认证和工具方法
 */
public abstract class BaseWebSocket {

    private static final Logger LOGGER = Logger.getLogger(BaseWebSocket.class.getName());

    /**
     * 校验 Origin 是否在 CORS 白名单中，不在则关闭连接。
     * 逻辑与 CorsFilter 一致。
     *
     * @return true 表示 Origin 合法，false 表示已拒绝并关闭连接
     */
    protected boolean checkOrigin(Session session) {
        String origin = (String) session.getUserProperties().get(WebSocketConfigurator.ORIGIN_KEY);

        if (origin == null || !WebConfig.isAllowedOrigin(origin)) {
            LOGGER.log(Level.WARNING, "WebSocket connection rejected: Origin {0} not allowed",
                    origin);
            closeSession(session, "Origin not allowed");
            return false;
        }
        return true;
    }

    /**
     * 从 query param token 中解析 userId
     */
    protected Integer resolveUserId(Session session) {
        Map<String, java.util.List<String>> params = session.getRequestParameterMap();
        if (params.get("token") != null && !params.get("token").isEmpty()) {
            String token = params.get("token").get(0);
            Integer userId = JwtUtil.getUserId(token);
            if (userId != null) return userId;
        }
        return null;
    }

    /**
     * 关闭 WebSocket 会话
     */
    protected void closeSession(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException ignored) {}
    }

    /**
     * 判断是否为 Jetty 客户端异常断连或已关闭的 channel。
     * 这些都是正常断连场景，不应以 WARNING 级别记录。
     * 使用类名字符串匹配，避免直接依赖 jetty 类（仅在 plugin classpath）。
     */
    protected boolean isAbnormalClose(Throwable throwable) {
        String className = throwable.getClass().getName();
        String message = throwable.getMessage();

        // Jetty ClosedChannelException: channel already closed by onClose
        if (className.contains("ClosedChannelException")) return true;

        // Jetty CloseException: Abnormal Close (client dropped connection)
        return className.contains("CloseException")
                && message != null
                && message.contains("Abnormal Close");
    }
}
