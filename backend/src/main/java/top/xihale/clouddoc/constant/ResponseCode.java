package top.xihale.clouddoc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 统一响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    public static Optional<ResponseCode> fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(rc -> rc.code.equals(code))
                .findFirst();
    }

    public static String defaultMessage(Integer code) {
        return fromCode(code)
                .map(ResponseCode::getMessage)
                .orElse(SERVER_ERROR.getMessage());
    }
}
