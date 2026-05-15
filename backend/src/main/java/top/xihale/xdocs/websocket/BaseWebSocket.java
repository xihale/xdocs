package top.xihale.xdocs.websocket;

import jakarta.websocket.*;

import java.io.IOException;

/**
 * WebSocket 端点基类，提供公共工具方法。
 * <p>
 * Origin 校验已统一到 CorsFilter/CsrfFilter（HTTP Upgrade 阶段）。
 * 认证由 AuthFilter 在 HTTP 层完成，WebSocketConfigurator 将 userId 注入 session userProperties。
 */
public abstract class BaseWebSocket {

    /**
     * 从 session 获取已认证的 userId。
     * AuthFilter 保证 WS 连接建立前已完成鉴权，此处不会返回 null。
     */
    protected int getUserId(Session session) {
        Integer userId = (Integer) session.getUserProperties().get("userId");
        if (userId == null) {
            throw new IllegalStateException("userId not found in session userProperties");
        }
        return userId;
    }

    /**
     * 从 session 获取已认证的 userId，可能为 null。
     */
    protected Integer getUserIdOrNull(Session session) {
        return (Integer) session.getUserProperties().get("userId");
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
