package top.xihale.xdocs.websocket;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import top.xihale.xdocs.filter.AuthFilter;
import top.xihale.xdocs.util.JwtUtil;

import java.util.List;
import java.util.Map;

/**
 * WebSocket Configurator，在握手阶段从 Cookie JWT 提取 userId 存入 userProperties。
 * <p>
 * AuthFilter 在 HTTP 层通过 Cookie JWT 鉴权，此处从 Cookie 中解析同一个 token
 * 提取 userId 传递给 WebSocket endpoint。
 */
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    public static final String ORIGIN_KEY = "ws.origin";

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // Origin
        String origin = null;
        if (request.getHeaders().containsKey("origin")) {
            var origins = request.getHeaders().get("origin");
            if (!origins.isEmpty()) {
                origin = origins.get(0);
            }
        }
        if (origin != null) {
            sec.getUserProperties().put(ORIGIN_KEY, origin);
        }

        // 从 Cookie 中提取 userId（AuthFilter 已校验过，此处仅传递）
        Map<String, List<String>> headers = request.getHeaders();
        if (headers.containsKey("cookie")) {
            for (String cookieHeader : headers.get("cookie")) {
                String token = extractCookieValue(cookieHeader, AuthFilter.TOKEN_COOKIE_NAME);
                if (token != null) {
                    Integer userId = JwtUtil.getUserId(token);
                    if (userId != null) {
                        sec.getUserProperties().put("userId", userId);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 从 Cookie header 字符串中提取指定 cookie 的值
     */
    private static String extractCookieValue(String cookieHeader, String cookieName) {
        String prefix = cookieName + "=";
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length());
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }
}
