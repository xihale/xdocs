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

    public static HttpResponse of(HttpServletResponse resp) {
        return new HttpResponse(resp);
    }

    private static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        resp.resetBuffer();
        resp.setStatus(status);
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.getWriter().write(JsonUtils.toJson(body));
    }

    /**
     * HTTP 响应封装，提供链式调用
     */
    public static final class HttpResponse {

        private final HttpServletResponse resp;

        public HttpResponse(HttpServletResponse resp) {
            this.resp = resp;
        }

        public HttpServletResponse getRawResponse() {
            return resp;
        }

        public void ok(Object data) throws IOException {
            writeJson(resp, ResponseCode.SUCCESS.getCode(), Result.success(data));
        }

        public void ok() throws IOException {
            writeJson(resp, ResponseCode.SUCCESS.getCode(), Result.success());
        }

        public void notFound() throws IOException {
            error(ResponseCode.NOT_FOUND);
        }

        public HttpResponse header(String name, String value) {
            if (resp.isCommitted()) {
                return this;
            }
            resp.setHeader(name, value);
            return this;
        }

        public void sendStatus(int code) {
            if (resp.isCommitted()) {
                return;
            }
            resp.resetBuffer();
            resp.setStatus(code);
        }

        public void sendStatus(ResponseCode responseCode) {
            sendStatus(responseCode.getCode());
        }

        public void error(int code) throws IOException {
            writeJson(resp, code, Result.error(code));
        }

        public void error(ResponseCode responseCode) throws IOException {
            error(responseCode.getCode());
        }

        public void error(int code, String message) throws IOException {
            writeJson(resp, code, Result.error(code, message));
        }

        public void error(ResponseCode responseCode, String message) throws IOException {
            error(responseCode.getCode(), message);
        }
    }
}
