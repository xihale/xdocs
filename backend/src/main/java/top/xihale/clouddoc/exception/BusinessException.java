package top.xihale.clouddoc.exception;

import lombok.Getter;

/**
 * 业务异常基类，用于在 Service / Servlet 层抛出可预期的错误
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 兼容旧用法：直接传 code + message（不推荐，优先使用 ErrorCode 枚举）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    /**
     * 兼容旧用法：直接传 code + message + cause
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorCode = null;
    }
}
