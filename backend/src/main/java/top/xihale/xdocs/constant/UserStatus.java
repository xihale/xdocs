package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    NORMAL(0, "正常"),
    BANNED(1, "封禁");

    private final int code;
    private final String message;

    public static UserStatus fromCode(int code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid UserStatus code: " + code);
    }
}
