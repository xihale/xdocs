package top.xihale.xdocs.exception;

import lombok.Getter;

/**
 * 团队相关异常
 */
@Getter
public class TeamException extends BusinessException {
    private final TeamError error;

    public TeamException(TeamError error) {
        super(error);
        this.error = error;
    }

    /**
     * 团队模块所有可能的业务错误
     */
    @Getter
    public enum TeamError implements ErrorCode {
        TEAM_NOT_FOUND(404, "团队不存在"),
        OPERATOR_NOT_IN_TEAM(403, "操作者不在团队中"),
        INVITE_REQUIRES_OWNER_OR_ADMIN(403, "只有 OWNER 或 ADMIN 才能邀请成员"),
        USER_ALREADY_IN_TEAM(400, "该用户已在团队中或已有待处理邀请"),
        INVITE_NOT_FOUND(404, "邀请记录不存在"),
        INVITE_STATUS_NOT_PENDING(400, "当前状态不是待接受邀请"),
        REMOVE_REQUIRES_OWNER_OR_ADMIN(403, "只有 OWNER 或 ADMIN 才能移除成员"),
        TARGET_MEMBER_NOT_FOUND(404, "目标成员不存在"),
        CANNOT_REMOVE_OWNER(403, "不能移除 OWNER"),
        ROLE_CHANGE_REQUIRES_OWNER(403, "只有 OWNER 才能修改成员角色"),
        CANNOT_CHANGE_OWNER_ROLE(403, "不能修改 OWNER 的角色"),
        CANCEL_INVITE_REQUIRES_OWNER_OR_ADMIN(403, "只有 OWNER 或 ADMIN 才能取消邀请"),
        NO_PENDING_INVITE(400, "该用户没有待处理的邀请"),
        ;

        private final int code;
        private final String message;

        TeamError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
