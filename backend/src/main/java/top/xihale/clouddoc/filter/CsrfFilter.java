package top.xihale.clouddoc.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.clouddoc.config.WebConfig;
import top.xihale.clouddoc.constant.ResponseCode;
import top.xihale.clouddoc.util.ResponseUtils;

import java.io.IOException;

/**
 * CSRF 过滤器，校验已登录用户的不安全请求来源
 */
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        var res = ResponseUtils.of(resp);

        // WebSocket 升级请求不走 CSRF 校验（WS 自有 Origin 检查）
        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade"))) {
            chain.doFilter(request, response);
            return;
        }

        if (!isUnsafeMethod(req.getMethod()) || !hasAuthenticatedToken(req)) {
            chain.doFilter(request, response);
            return;
        }

        if (!isTrustedRequest(req)) {
            res.error(ResponseCode.FORBIDDEN, "非法跨站请求");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isUnsafeMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
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
