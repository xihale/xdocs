package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.xihale.xdocs.exception.ParamException.ParamError;

/**
 * 知识库可见性枚举
 */
@Getter
@AllArgsConstructor
public enum Visibility {
    PRIVATE(0, "私有"),
    PUBLIC(1, "公开");

    private final int code;
    private final String message;

    public static int parse(String value) {
        if (value == null || value.isBlank()) return PRIVATE.getCode();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return Visibility.valueOf(value.toUpperCase()).getCode();
            } catch (IllegalArgumentException ex) {
                throw ParamError.INVALID_ENUM_VALUE.with("visibility 无效，可选值: PRIVATE, PUBLIC");
            }
        }
    }

    public static Visibility fromCode(int code) {
        for (Visibility v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("Invalid Visibility code: " + code);
    }
}
