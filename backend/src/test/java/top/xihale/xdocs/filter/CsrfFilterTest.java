package top.xihale.xdocs.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.xihale.xdocs.support.MockRequest;
import top.xihale.xdocs.support.MockResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsrfFilterTest {

    private CsrfFilter filter;
    private FilterChain chain;
    private MockRequest req;
    private MockResponse resp;

    @BeforeEach
    void setUp() {
        filter = new CsrfFilter();
        chain = mock(FilterChain.class);
        req = new MockRequest();
        resp = new MockResponse();
    }

    @Test void safeMethod_passesThrough() throws Exception {
        req.method("GET").uri("/api/test");
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test void unsafeMethod_noCookie_passesThrough() throws Exception {
        req.method("POST").uri("/api/test");
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test void unsafeMethod_withCookie_trustedOrigin_passesThrough() throws Exception {
        req.method("POST").uri("/api/test")
            .cookie(new Cookie("xdocs_token", "token"))
            .header("Origin", "http://localhost:5173");
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test void unsafeMethod_withCookie_untrustedOrigin_returns403() throws Exception {
        req.method("POST").uri("/api/test")
            .cookie(new Cookie("xdocs_token", "token"))
            .header("Origin", "https://evil.com");
        filter.doFilter(req, resp, chain);
        assertEquals(403, resp.getStatusCode());
    }
}
