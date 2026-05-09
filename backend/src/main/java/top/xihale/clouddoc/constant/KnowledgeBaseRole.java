package top.xihale.clouddoc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.xihale.clouddoc.exception.ParamException.ParamError;

/**
 * 知识库成员角色枚举
 */
@Getter
@AllArgsConstructor
public enum KnowledgeBaseRole {
    OWNER(0, "所有者"),
    ADMIN(1, "管理员"),
    EDITOR(2, "编辑者"),
    VIEWER(3, "只读");

    private final int code;
    private final String message;

    public static int parse(String value) {
        if (value == null || value.isBlank()) return VIEWER.getCode();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return KnowledgeBaseRole.valueOf(value.toUpperCase()).getCode();
            } catch (IllegalArgumentException ex) {
                throw ParamError.INVALID_ENUM_VALUE.with("role 无效，可选值: OWNER, ADMIN, EDITOR, VIEWER");
            }
        }
    }

    public static KnowledgeBaseRole fromCode(int code) {
        for (KnowledgeBaseRole r : values()) {
            if (r.code == code) return r;
        }
        throw new IllegalArgumentException("Invalid KnowledgeBaseRole code: " + code);
    }
}
