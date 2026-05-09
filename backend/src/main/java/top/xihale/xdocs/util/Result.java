package top.xihale.xdocs.util;

import lombok.Getter;
import top.xihale.xdocs.constant.ResponseCode;

/**
 * 统一响应封装
 */
@Getter
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <R> Result<R> success(R data) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <R> Result<R> success() {
        return success(null);
    }

    public static <R> Result<R> error(Integer code, String message) {
        String resolvedMessage = message == null || message.isBlank()
                ? ResponseCode.defaultMessage(code)
                : message;
        return new Result<>(code, resolvedMessage, null);
    }

    public static <R> Result<R> error(Integer code) {
        return error(code, null);
    }

    public static <R> Result<R> error(ResponseCode responseCode) {
        return error(responseCode.getCode(), responseCode.getMessage());
    }
}
