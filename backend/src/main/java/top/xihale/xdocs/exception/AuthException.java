package top.xihale.xdocs.exception;

import lombok.Getter;

/**
 * 认证相关异常
 */
@Getter
public class AuthException extends BusinessException {
    private final AuthError error;

    public AuthException(AuthError error) {
        super(error);
        this.error = error;
    }

    /**
     * 认证模块所有可能的业务错误
     */
    @Getter
    public enum AuthError implements ErrorCode {
        EMAIL_CODE_INVALID(400, "邮件验证码错误或已失效"),
        EMAIL_CODE_TOO_FREQUENT(429, "验证码发送过于频繁，请稍后再试"),
        TURNSTILE_FAILED(400, "人机验证失败，请重试"),
        EMAIL_NOT_REGISTERED(404, "该邮箱尚未注册"),
        EMAIL_ALREADY_REGISTERED(400, "该邮箱已被注册"),
        VERIFY_CODE_INVALID(400, "验证码错误或已失效"),
        NOT_LOGGED_IN(401, "未登录"),
        ;

        private final int code;
        private final String message;

        AuthError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
