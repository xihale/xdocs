package top.xihale.clouddoc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.xihale.clouddoc.exception.ParamException.ParamError;

/**
 * TEAM 成员角色枚举
 */
@Getter
@AllArgsConstructor
public enum TeamRole {
    OWNER(0, "所有者"),
    ADMIN(1, "管理员"),
    MEMBER(2, "普通成员");

    private final int code;
    private final String message;

    public static TeamRole fromCode(int code) {
        for (TeamRole r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("Invalid TeamRole code: " + code);
    }

    /**
     * 从字符串解析角色（支持 code 数字或枚举名称）
     */
    public static int parse(String value) {
        if (value == null || value.isBlank()) return MEMBER.getCode();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return TeamRole.valueOf(value.toUpperCase()).getCode();
            } catch (IllegalArgumentException ex) {
                throw ParamError.INVALID_ENUM_VALUE.with("role 无效，可选值: OWNER, ADMIN, MEMBER");
            }
        }
    }
}
