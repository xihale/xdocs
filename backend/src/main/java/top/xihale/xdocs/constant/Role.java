package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台角色枚举
 */
@Getter
@AllArgsConstructor
public enum Role {
    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String message;

    public static Role fromCode(int code) {
        for (Role r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("Invalid Role code: " + code);
    }
}
