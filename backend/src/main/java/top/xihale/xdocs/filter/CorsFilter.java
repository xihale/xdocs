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
 * 跨域过滤器，统一设置 CORS 响应头
 */
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // WebSocket 升级请求不需要 CORS headers（WS 自有 Origin 检查）
        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade"))) {
            chain.doFilter(request, response);
            return;
        }

        String origin = req.getHeader("Origin");

        if (origin != null && WebConfig.isAllowedOrigin(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Methods", WebConfig.getAllowedMethods());
            resp.setHeader("Access-Control-Allow-Headers", WebConfig.getAllowedHeaders());
            resp.setHeader("Access-Control-Allow-Credentials", String.valueOf(WebConfig.isAllowCredentials()));
            resp.setHeader("Access-Control-Max-Age", "3600");
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            if (origin != null && !WebConfig.isAllowedOrigin(origin)) {
                resp.setStatus(ResponseCode.FORBIDDEN.getCode());
                return;
            }
            resp.setStatus(ResponseCode.SUCCESS.getCode());
            return;
        }

        chain.doFilter(request, response);
    }
}
