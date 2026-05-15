package top.xihale.xdocs.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.config.WebConfig;
import top.xihale.xdocs.constant.ResponseCode;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;

/**
 * CSRF 过滤器，校验已登录用户的不安全请求来源
 */
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (!requiresCsrfCheck(req)) {
            chain.doFilter(request, response);
            return;
        }

        if (!isTrustedRequest(req)) {
            ResponseUtils.writeError(resp, ResponseCode.FORBIDDEN, "非法跨站请求");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 需要校验 CSRF 的请求：不安全方法（POST/PUT/PATCH/DELETE）或 WebSocket 升级，且携带认证 Cookie。
     * WebSocket 升级虽是 GET，但建立持久连接，等同于不安全方法。
     */
    private boolean requiresCsrfCheck(HttpServletRequest req) {
        return (isUnsafeMethod(req.getMethod()) || isWebSocketUpgrade(req))
                && hasAuthenticatedToken(req);
    }

    private boolean isUnsafeMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isWebSocketUpgrade(HttpServletRequest req) {
        return "websocket".equalsIgnoreCase(req.getHeader("Upgrade"));
    }

    /**
     * 检查请求是否携带已认证的 Cookie Token
     */
    private boolean hasAuthenticatedToken(HttpServletRequest req) {
        return AuthFilter.getTokenFromCookie(req) != null;
    }

    private boolean isTrustedRequest(HttpServletRequest req) {
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return isTrustedSource(origin, req);
        }

        String referer = req.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return WebConfig.getAllowedOrigins()
                    .stream()
                    .anyMatch(originPrefix -> matchesRefererOrigin(referer, originPrefix))
                    || matchesRefererOrigin(referer, getRequestOrigin(req));
        }

        return false;
    }

    private boolean matchesRefererOrigin(String referer, String origin) {
        return referer.equals(origin) || referer.startsWith(origin + "/");
    }

    private boolean isTrustedSource(String origin, HttpServletRequest req) {
        return WebConfig.isAllowedOrigin(origin) || origin.equals(getRequestOrigin(req));
    }

    private String getRequestOrigin(HttpServletRequest req) {
        String scheme = req.getScheme();
        int port = req.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        if (defaultPort) {
            return scheme + "://" + req.getServerName();
        }
        return scheme + "://" + req.getServerName() + ":" + port;
    }
}
