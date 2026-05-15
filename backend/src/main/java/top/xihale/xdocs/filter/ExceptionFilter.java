package top.xihale.xdocs.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.constant.ResponseCode;
import top.xihale.xdocs.exception.BusinessException;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 全局异常捕获过滤器，将异常统一转为 JSON 响应
 */
public class ExceptionFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(ExceptionFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            int code = e.getCode();
            logBusinessException(req, code, e);
            ResponseUtils.writeError(resp, code, e.getMessage());
        } catch (Exception e) {
            logUnhandledException(req, e);
            ResponseUtils.writeError(resp, ResponseCode.SERVER_ERROR);
        }
    }

    private void logBusinessException(HttpServletRequest req, int code, BusinessException e) {
        Level level = code >= 500 ? Level.SEVERE : Level.WARNING;
        String label = code >= 500 ? "SERVER ERROR" : "CLIENT ERROR";
        String message = """
                [%s] %s
                  Code: %d
                  Path: %s %s
                  User: %s
                  From: %s""".formatted(
                label, e.getMessage(), code,
                req.getMethod(), getRequestPath(req),
                req.getAttribute("userId"),
                req.getRemoteAddr());
        Throwable cause = e.getCause();
        if (cause != null) {
            LOGGER.log(level, message, cause);
            return;
        }
        if (code >= 500) {
            LOGGER.log(level, message, e);
            return;
        }
        LOGGER.log(level, message);
    }

    private void logUnhandledException(HttpServletRequest req, Exception e) {
        String message = """
                [UNHANDLED] %s
                  Path: %s %s
                  User: %s
                  From: %s""".formatted(
                e.getClass().getSimpleName(),
                req.getMethod(), getRequestPath(req),
                req.getAttribute("userId"),
                req.getRemoteAddr());
        LOGGER.log(Level.SEVERE, message, e);
    }

    private String getRequestPath(HttpServletRequest req) {
        String query = req.getQueryString();
        if (query == null || query.isBlank()) {
            return req.getRequestURI();
        }
        return "%s?%s".formatted(req.getRequestURI(), query);
    }

}
