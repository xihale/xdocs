package top.xihale.xdocs.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加入/邀请状态枚举
 */
@Getter
@AllArgsConstructor
public enum JoinStatus {
    INVITED(0, "已邀请"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String message;

    public static JoinStatus fromCode(int code) {
        for (JoinStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Invalid JoinStatus code: " + code);
    }
}
