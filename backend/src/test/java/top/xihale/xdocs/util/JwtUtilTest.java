package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test void generateToken_producesString() {
        String token = JwtUtil.generateToken(1);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test void generateToken_and_getUserId_roundTrip() {
        String token = JwtUtil.generateToken(42);
        Integer userId = JwtUtil.getUserId(token);
        assertEquals(42, userId);
    }

    @Test void getUserId_invalidToken_returnsNull() {
        assertNull(JwtUtil.getUserId("invalid.token.string"));
    }

    @Test void getUserId_emptyString_returnsNull() {
        assertNull(JwtUtil.getUserId(""));
    }

    @Test void parseToken_invalidToken_returnsNull() {
        assertNull(JwtUtil.parseToken("bad"));
    }

    @Test void differentUsers_produceDifferentTokens() {
        String t1 = JwtUtil.generateToken(1);
        String t2 = JwtUtil.generateToken(2);
        assertNotEquals(t1, t2);
    }
}
