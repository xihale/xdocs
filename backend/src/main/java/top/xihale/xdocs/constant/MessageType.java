package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聊天消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {
    TEXT(0, "文本"),
    SYSTEM(1, "系统消息");

    private final int code;
    private final String message;

    public static MessageType fromCode(int code) {
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Invalid MessageType code: " + code);
    }
}
