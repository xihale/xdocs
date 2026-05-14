package top.xihale.xdocs.util;

import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.constant.ResponseCode;

import java.io.IOException;

/**
 * HTTP 响应工具类
 */
public final class ResponseUtils {

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private ResponseUtils() {
    }

    /**
     * 将 Result 写入 HttpServletResponse（框架内部使用，保证只写一次）
     */
    public static void writeResult(HttpServletResponse resp, Result<?> result) throws IOException {
        resp.setStatus(result.getCode());
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.getWriter().write(JsonUtils.toJson(result));
    }

    /**
     * 写入错误响应（ExceptionFilter 等使用）
     */
    public static void writeError(HttpServletResponse resp, int code, String message) throws IOException {
        writeResult(resp, Result.error(code, message));
    }

    /**
     * 写入错误响应（ExceptionFilter 等使用）
     */
    public static void writeError(HttpServletResponse resp, ResponseCode responseCode) throws IOException {
        writeResult(resp, Result.error(responseCode));
    }

    /**
     * 写入错误响应（ExceptionFilter 等使用）
     */
    public static void writeError(HttpServletResponse resp, ResponseCode responseCode, String message) throws IOException {
        writeResult(resp, Result.error(responseCode.getCode(), message));
    }

    /**
     * 写入错误响应（ExceptionFilter 等使用）
     */
    public static void writeError(HttpServletResponse resp, int code) throws IOException {
        writeResult(resp, Result.error(code));
    }
}
