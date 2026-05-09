package top.xihale.xdocs.exception;

import lombok.Getter;

/**
 * 参数校验相关异常（Servlet 层参数解析）
 */
@Getter
public class ParamException extends BusinessException {
    private final ParamError error;

    public ParamException(ParamError error) {
        super(error);
        this.error = error;
    }

    private ParamException(int code, String message) {
        super(code, message);
        this.error = null;
    }

    /**
     * 参数校验模块所有可能的业务错误
     */
    @Getter
    public enum ParamError implements ErrorCode {
        MISSING_PARAM(400, "缺少必填参数"),
        JSON_FORMAT_ERROR(400, "JSON 格式错误"),
        PARAM_NOT_INT(400, "参数应为整数"),
        USER_ID_OR_USERNAME_REQUIRED(400, "需要提供 userId 或 username"),
        USER_ID_NOT_INT(400, "userId 应为整数"),
        INVALID_ENUM_VALUE(400, "枚举值无效"),
        FILE_REQUIRED(400, "请选择要上传的文件"),
        FILE_NAME_EMPTY(400, "文件名不能为空"),
        FILE_TYPE_NOT_ALLOWED(400, "不支持的文件类型"),
        FILE_TOO_LARGE(400, "文件大小超出限制"),
        ;

        private final int code;
        private final String message;

        ParamError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        /**
         * 创建带动态消息的异常（用于 "缺少必填参数 'xxx'" 等场景）
         */
        public ParamException with(String detail) {
            return new ParamException(this.code, this.message + " " + detail);
        }
    }
}
