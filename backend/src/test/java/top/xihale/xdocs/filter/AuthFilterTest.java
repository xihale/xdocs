package top.xihale.xdocs.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.Role;
import top.xihale.xdocs.constant.UserStatus;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.support.MockRequest;
import top.xihale.xdocs.support.MockResponse;
import top.xihale.xdocs.util.JwtUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthFilterTest {

    private AuthFilter filter;
    private MockedStatic<JwtUtil> jwtUtil;
    private FilterChain chain;
    private MockRequest req;
    private MockResponse resp;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter();
        chain = mock(FilterChain.class);
        req = new MockRequest();
        resp = new MockResponse();
    }

    @AfterEach
    void tearDown() {
        if (jwtUtil != null) jwtUtil.close();
    }

    private void mockJwt(Integer userId) {
        jwtUtil = mockStatic(JwtUtil.class);
        jwtUtil.when(() -> JwtUtil.getUserId(anyString())).thenReturn(userId);
    }

    private User createUser(int status) {
        User u = new User("alice", "hash", "a@t.com");
        u.setRole(Role.USER.getCode());
        u.setStatus(status);
        return u;
    }

    @Test void whitelistedPath_passesThrough() throws Exception {
        req.uri("/api/auth/login");
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test void missingToken_returns401() throws Exception {
        req.uri("/api/protected");
        filter.doFilter(req, resp, chain);
        assertEquals(401, resp.getStatusCode());
    }

    @Test void invalidToken_returns401() throws Exception {
        mockJwt(null);
        req.uri("/api/protected")
            .cookie(new Cookie("xdocs_token", "bad-token"));
        filter.doFilter(req, resp, chain);
        assertEquals(401, resp.getStatusCode());
    }

    @Test void wsUpgrade_noToken_returns401() throws Exception {
        req.uri("/api/chat/ws/1").header("Upgrade", "websocket");
        filter.doFilter(req, resp, chain);
        assertEquals(401, resp.getStatusCode());
    }
}
