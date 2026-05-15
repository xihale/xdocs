package top.xihale.xdocs.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.xihale.xdocs.exception.BusinessException;
import top.xihale.xdocs.support.MockRequest;
import top.xihale.xdocs.support.MockResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExceptionFilterTest {

    private ExceptionFilter filter;
    private FilterChain chain;
    private MockRequest req;
    private MockResponse resp;

    @BeforeEach
    void setUp() {
        filter = new ExceptionFilter();
        chain = mock(FilterChain.class);
        req = new MockRequest();
        resp = new MockResponse();
    }

    @Test void normalRequest_passesThrough() throws Exception {
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test void businessException_convertsToError() throws Exception {
        doThrow(new BusinessException(404, "Not Found"))
            .when(chain).doFilter(req, resp);
        filter.doFilter(req, resp, chain);
        assertEquals(404, resp.getStatusCode());
    }

    @Test void unhandledException_returns500() throws Exception {
        doThrow(new RuntimeException("Boom"))
            .when(chain).doFilter(req, resp);
        filter.doFilter(req, resp, chain);
        assertEquals(500, resp.getStatusCode());
    }
}
