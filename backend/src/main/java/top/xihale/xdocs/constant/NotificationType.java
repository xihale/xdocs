package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {
    TEAM_INVITE(0, "团队邀请"),
    KB_INVITE(1, "知识库邀请"),
    TEAM_NEW_ARTICLE(2, "团队新文章"),
    COMMENT(3, "评论"),
    LIKE(4, "点赞"),
    MEMBER_CHANGE(5, "成员变动"),
    FOLLOW(6, "关注"),
    FOLLOW_ARTICLE(7, "关注者新文章");

    private final int code;
    private final String message;

    public static NotificationType fromCode(int code) {
        for (NotificationType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Invalid NotificationType code: " + code);
    }
}
