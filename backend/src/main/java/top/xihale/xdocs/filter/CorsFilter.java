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

        var res = ResponseUtils.of(resp);
        String origin = req.getHeader("Origin");

        if (origin != null && WebConfig.isAllowedOrigin(origin)) {
            res.header("Access-Control-Allow-Origin", origin)
                    .header("Vary", "Origin")
                    .header("Access-Control-Allow-Methods", WebConfig.getAllowedMethods())
                    .header("Access-Control-Allow-Headers", WebConfig.getAllowedHeaders())
                    .header("Access-Control-Allow-Credentials", String.valueOf(WebConfig.isAllowCredentials()))
                    .header("Access-Control-Max-Age", "3600");
        }

        // Content-Security-Policy: 禁止 inline script、禁止外部脚本/样式、禁止框架嵌入
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob:; " +
                "frame-ancestors 'none'; " +
                "object-src 'none'");

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            if (origin != null && !WebConfig.isAllowedOrigin(origin)) {
                res.sendStatus(ResponseCode.FORBIDDEN);
                return;
            }
            res.sendStatus(ResponseCode.SUCCESS);
            return;
        }

        chain.doFilter(request, response);
    }
}
