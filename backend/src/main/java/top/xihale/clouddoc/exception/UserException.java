package top.xihale.clouddoc.exception;

import lombok.Getter;

/**
 * 用户相关异常
 */
@Getter
public class UserException extends BusinessException {
    private final UserError error;

    public UserException(UserError error) {
        super(error);
        this.error = error;
    }

    /**
     * 用户模块所有可能的业务错误
     */
    @Getter
    public enum UserError implements ErrorCode {
        USERNAME_EXISTS(400, "用户名已存在"),
        EMAIL_EXISTS(400, "邮箱已被注册"),
        LOGIN_FAILED(401, "用户名或密码错误"),
        OLD_PASSWORD_WRONG(401, "旧密码错误"),
        USER_NOT_FOUND(404, "用户未找到"),
        USERNAME_NOT_FOUND(404, "用户名不存在"),
        EMAIL_NOT_REGISTERED(404, "该邮箱未注册"),
        REQUIRE_ADMIN(403, "需要管理员权限"),
        USER_BANNED(403, "账号已被封禁"),
        CANNOT_FOLLOW_SELF(400, "不能关注自己"),
        ;

        private final int code;
        private final String message;

        UserError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
