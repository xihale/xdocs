package top.xihale.clouddoc.exception;

/**
 * 错误码接口，所有业务错误枚举实现此接口
 */
public interface ErrorCode {
    int getCode();
    String getMessage();
}
