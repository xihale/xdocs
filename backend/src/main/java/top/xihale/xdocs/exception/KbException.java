package top.xihale.xdocs.exception;

import lombok.Getter;

/**
 * 知识库相关异常
 */
@Getter
public class KbException extends BusinessException {
    private final KbError error;

    public KbException(KbError error) {
        super(error);
        this.error = error;
    }

    /**
     * 知识库模块所有可能的业务错误
     */
    @Getter
    public enum KbError implements ErrorCode {
        NOT_TEAM_MEMBER(403, "您不是该 TEAM 的成员，无法创建知识库"),
        KB_NOT_FOUND(404, "知识库不存在"),
        NOT_KB_MEMBER(403, "您不是该知识库的成员"),
        KB_PERMISSION_DENIED(403, "权限不足"),
        AUTHORIZE_REQUIRES_OWNER_OR_ADMIN(403, "权限不足，只有 OWNER 或 ADMIN 可以授权成员"),
        TARGET_NOT_KB_MEMBER(404, "该用户不是知识库成员"),
        CANNOT_REMOVE_KB_OWNER(403, "不能移除 OWNER"),
        REMOVE_REQUIRES_OWNER_OR_ADMIN(403, "权限不足，只有 OWNER 或 ADMIN 可以移除成员"),
        KB_INVITE_NOT_FOUND(404, "邀请记录不存在"),
        KB_INVITE_STATUS_NOT_PENDING(400, "当前状态不是待接受邀请"),
        CANCEL_KB_INVITE_REQUIRES_OWNER_OR_ADMIN(403, "权限不足，只有 OWNER 或 ADMIN 可以取消邀请"),
        NO_PENDING_KB_INVITE(400, "该用户没有待处理的邀请"),
        KB_ROLE_CHANGE_REQUIRES_OWNER(403, "只有 OWNER 才能修改成员角色"),
        KB_TARGET_MEMBER_NOT_FOUND(404, "目标成员不存在"),
        CANNOT_CHANGE_KB_OWNER_ROLE(403, "不能修改 OWNER 的角色"),
        ;

        private final int code;
        private final String message;

        KbError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
