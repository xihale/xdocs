package top.xihale.xdocs.support;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Minimal HttpServletRequest mock for filter unit tests.
 */
public class MockRequest extends HttpServletRequestWrapper {

    private String method = "GET";
    private String requestURI = "/";
    private String contextPath = "";
    private String contentType;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String[]> parameters = new HashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private String scheme = "http";
    private String serverName = "localhost";
    private int serverPort = 80;
    private byte[] body = new byte[0];
    private String remoteAddr = "127.0.0.1";

    public MockRequest() {
        super(new jakarta.servlet.http.HttpServletRequest() {
            // Minimal stub — all meaningful calls delegated by wrapper
            public String getAuthType() { return null; }
            public Cookie[] getCookies() { return null; }
            public long getDateHeader(String name) { return -1; }
            public String getHeader(String name) { return null; }
            public java.util.Enumeration<String> getHeaders(String name) { return java.util.Collections.emptyEnumeration(); }
            public java.util.Enumeration<String> getHeaderNames() { return java.util.Collections.emptyEnumeration(); }
            public int getIntHeader(String name) { return -1; }
            public String getMethod() { return "GET"; }
            public String getPathInfo() { return null; }
            public String getPathTranslated() { return null; }
            public String getContextPath() { return ""; }
            public String getQueryString() { return null; }
            public String getRemoteUser() { return null; }
            public boolean isUserInRole(String role) { return false; }
            public java.security.Principal getUserPrincipal() { return null; }
            public String getRequestedSessionId() { return null; }
            public String getRequestURI() { return "/"; }
            public StringBuffer getRequestURL() { return new StringBuffer("/"); }
            public String getServletPath() { return ""; }
            public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
            public jakarta.servlet.http.HttpSession getSession() { return null; }
            public String changeSessionId() { return null; }
            public boolean isRequestedSessionIdValid() { return false; }
            public boolean isRequestedSessionIdFromCookie() { return false; }
            public boolean isRequestedSessionIdFromURL() { return false; }
            public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
            public void login(String username, String password) {}
            public void logout() {}
            public java.util.Collection<jakarta.servlet.http.Part> getParts() { return java.util.List.of(); }
            public jakarta.servlet.http.Part getPart(String name) { return null; }
            public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
            public Object getAttribute(String name) { return null; }
            public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.emptyEnumeration(); }
            public String getCharacterEncoding() { return null; }
            public void setCharacterEncoding(String env) {}
            public int getContentLength() { return -1; }
            public long getContentLengthLong() { return -1; }
            public String getContentType() { return null; }
            public ServletInputStream getInputStream() throws IOException { return null; }
            public String getParameter(String name) { return null; }
            public java.util.Enumeration<String> getParameterNames() { return java.util.Collections.emptyEnumeration(); }
            public String[] getParameterValues(String name) { return null; }
            public Map<String, String[]> getParameterMap() { return Collections.emptyMap(); }
            public String getProtocol() { return "HTTP/1.1"; }
            public String getScheme() { return "http"; }
            public String getServerName() { return "localhost"; }
            public int getServerPort() { return 80; }
            public java.io.BufferedReader getReader() throws IOException { return null; }
            public String getRemoteAddr() { return "127.0.0.1"; }
            public String getRemoteHost() { return "127.0.0.1"; }
            public void setAttribute(String name, Object o) {}
            public void removeAttribute(String name) {}
            public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
            public java.util.Enumeration<java.util.Locale> getLocales() { return java.util.Collections.enumeration(java.util.List.of(java.util.Locale.getDefault())); }
            public boolean isSecure() { return false; }
            public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
            public int getRemotePort() { return 0; }
            public String getLocalName() { return "localhost"; }
            public String getLocalAddr() { return "127.0.0.1"; }
            public int getLocalPort() { return 80; }
            public jakarta.servlet.ServletContext getServletContext() { return null; }
            public jakarta.servlet.AsyncContext startAsync() throws IllegalStateException { return null; }
            public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) throws IllegalStateException { return null; }
            public boolean isAsyncStarted() { return false; }
            public boolean isAsyncSupported() { return false; }
            public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
            public jakarta.servlet.DispatcherType getDispatcherType() { return jakarta.servlet.DispatcherType.REQUEST; }
            public String getRequestId() { return null; }
            public String getProtocolRequestId() { return null; }
            public jakarta.servlet.ServletConnection getServletConnection() { return null; }
            public Map<String, String> getTrailerFields() { return Map.of(); }
            public boolean isTrailerFieldsReady() { return false; }
            public jakarta.servlet.http.HttpServletMapping getHttpServletMapping() { return null; }
            public jakarta.servlet.http.PushBuilder newPushBuilder() { return null; }
        });
    }

    public MockRequest method(String m) { method = m; return this; }
    public MockRequest uri(String u) { requestURI = u; return this; }
    public MockRequest contextPath(String cp) { contextPath = cp; return this; }
    public MockRequest header(String name, String value) { headers.put(name.toLowerCase(), value); return this; }
    public MockRequest param(String name, String value) { parameters.put(name, new String[]{value}); return this; }
    public MockRequest cookie(Cookie c) { cookies.add(c); return this; }
    public MockRequest scheme(String s) { scheme = s; return this; }
    public MockRequest serverName(String n) { serverName = n; return this; }
    public MockRequest serverPort(int p) { serverPort = p; return this; }
    public MockRequest contentType(String ct) { contentType = ct; return this; }
    public MockRequest body(String b) { body = b.getBytes(); return this; }
    public MockRequest remoteAddr(String addr) { remoteAddr = addr; return this; }

    @Override public String getMethod() { return method; }
    @Override public String getRequestURI() { return requestURI; }
    @Override public String getContextPath() { return contextPath; }
    @Override public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    @Override public String getContentType() { return contentType; }
    @Override public Cookie[] getCookies() { return cookies.isEmpty() ? null : cookies.toArray(new Cookie[0]); }
    @Override public String getScheme() { return scheme; }
    @Override public String getServerName() { return serverName; }
    @Override public int getServerPort() { return serverPort; }
    @Override public String getRemoteAddr() { return remoteAddr; }
    @Override public String getRemoteHost() { return remoteAddr; }
    @Override public String getParameter(String name) { String[] v = parameters.get(name); return v != null && v.length > 0 ? v[0] : null; }
    @Override public Map<String, String[]> getParameterMap() { return parameters; }

    @Override public ServletInputStream getInputStream() throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            public int read() { return bis.read(); }
            public boolean isFinished() { return bis.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(jakarta.servlet.ReadListener r) {}
        };
    }
}
