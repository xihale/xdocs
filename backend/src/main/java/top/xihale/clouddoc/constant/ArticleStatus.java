package top.xihale.clouddoc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章状态枚举
 */
@Getter
@AllArgsConstructor
public enum ArticleStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final int code;
    private final String message;

    public static ArticleStatus fromCode(int code) {
        for (ArticleStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid ArticleStatus code: " + code);
    }
}
