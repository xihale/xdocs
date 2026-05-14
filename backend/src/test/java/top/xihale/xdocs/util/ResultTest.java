package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_withData() {
        Result<String> r = Result.success("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void success_noData() {
        Result<Void> r = Result.success();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void error_withCodeAndMessage() {
        Result<Void> r = Result.error(400, "参数错误");
        assertEquals(400, r.getCode());
        assertEquals("参数错误", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void error_withNullMessage_usesDefault() {
        Result<Void> r = Result.error(404, null);
        assertEquals(404, r.getCode());
        assertEquals("资源不存在", r.getMessage());
    }

    @Test
    void error_withBlankMessage_usesDefault() {
        Result<Void> r = Result.error(500, "   ");
        assertEquals(500, r.getCode());
        assertEquals("服务器内部错误", r.getMessage());
    }

    @Test
    void error_withResponseCode() {
        Result<Void> r = Result.error(top.xihale.xdocs.constant.ResponseCode.UNAUTHORIZED);
        assertEquals(401, r.getCode());
        assertEquals("未登录", r.getMessage());
    }

    @Test
    void error_codeOnly_usesDefaultMessage() {
        Result<Void> r = Result.error(403);
        assertEquals(403, r.getCode());
        assertEquals("无权限", r.getMessage());
    }

    @Test
    void error_unknownCode_usesServerErrorMessage() {
        Result<Void> r = Result.error(999);
        assertEquals(999, r.getCode());
        assertEquals("服务器内部错误", r.getMessage());
    }
}
