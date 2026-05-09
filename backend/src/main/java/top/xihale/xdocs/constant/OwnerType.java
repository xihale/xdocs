package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.xihale.xdocs.exception.ParamException.ParamError;

/**
 * 知识库归属类型枚举
 */
@Getter
@AllArgsConstructor
public enum OwnerType {
    USER(0, "个人"),
    TEAM(1, "团队");

    private final int code;
    private final String message;

    public static int parse(String value) {
        if (value == null || value.isBlank()) return USER.getCode();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return OwnerType.valueOf(value.toUpperCase()).getCode();
            } catch (IllegalArgumentException ex) {
                throw ParamError.INVALID_ENUM_VALUE.with("ownerType 无效，可选值: USER, TEAM");
            }
        }
    }

    public static OwnerType fromCode(int code) {
        for (OwnerType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Invalid OwnerType code: " + code);
    }
}
