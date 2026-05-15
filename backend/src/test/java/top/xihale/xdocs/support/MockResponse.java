package top.xihale.xdocs.support;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal HttpServletResponse mock for filter unit tests.
 */
public class MockResponse extends HttpServletResponseWrapper {

    private int status = 200;
    private String characterEncoding = "UTF-8";
    private String contentType;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final PrintWriter writer = new PrintWriter(body, true, StandardCharsets.UTF_8);
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private int contentLength = -1;
    private Locale locale = Locale.getDefault();

    public MockResponse() {
        super(new jakarta.servlet.http.HttpServletResponse() {
            public void addCookie(Cookie cookie) {}
            public boolean containsHeader(String name) { return false; }
            public String encodeURL(String url) { return url; }
            public String encodeRedirectURL(String url) { return url; }
            public String encodeUrl(String url) { return url; }
            public String encodeRedirectUrl(String url) { return url; }
            public void sendError(int sc, String msg) throws IOException {}
            public void sendError(int sc) throws IOException {}
            public void sendRedirect(String location) throws IOException {}
            public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {}
            public void setDateHeader(String name, long date) {}
            public void addDateHeader(String name, long date) {}
            public void setHeader(String name, String value) {}
            public void addHeader(String name, String value) {}
            public void setIntHeader(String name, int value) {}
            public void addIntHeader(String name, int value) {}
            public void setStatus(int sc) {}
            public int getStatus() { return 200; }
            public String getHeader(String name) { return null; }
            public java.util.Collection<String> getHeaders(String name) { return java.util.List.of(); }
            public java.util.Collection<String> getHeaderNames() { return java.util.List.of(); }
            public String getCharacterEncoding() { return null; }
            public String getContentType() { return null; }
            public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException { return null; }
            public java.io.PrintWriter getWriter() throws IOException { return null; }
            public void setCharacterEncoding(String charset) {}
            public void setContentLength(int len) {}
            public void setContentLengthLong(long len) {}
            public void setContentType(String type) {}
            public void setBufferSize(int size) {}
            public int getBufferSize() { return 0; }
            public void flushBuffer() throws IOException {}
            public void resetBuffer() {}
            public boolean isCommitted() { return false; }
            public void reset() {}
            public void setLocale(java.util.Locale loc) {}
            public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
            public void setTrailerFields(java.util.function.Supplier<Map<String, String>> supplier) {}
            public java.util.function.Supplier<Map<String, String>> getTrailerFields() { return null; }
        });
    }

    public int getStatusCode() { return status; }
    public String getBodyAsString() { writer.flush(); return body.toString(StandardCharsets.UTF_8); }
    public String getResponseHeader(String name) { return headers.get(name.toLowerCase()); }

    @Override public String getCharacterEncoding() { return characterEncoding; }
    @Override public void setCharacterEncoding(String charset) { characterEncoding = charset; }
    @Override public String getContentType() { return contentType; }
    @Override public void setContentType(String type) { contentType = type; }
    @Override public void setStatus(int sc) { status = sc; }
    @Override public int getStatus() { return status; }
    @Override public void sendError(int sc) throws IOException { status = sc; }
    @Override public void sendError(int sc, String msg) throws IOException { status = sc; }
    @Override public void setHeader(String name, String value) { headers.put(name.toLowerCase(), value); }
    @Override public void addHeader(String name, String value) { setHeader(name, value); }
    @Override public void setIntHeader(String name, int value) { headers.put(name.toLowerCase(), String.valueOf(value)); }
    @Override public void addIntHeader(String name, int value) { setIntHeader(name, value); }
    @Override public boolean containsHeader(String name) { return headers.containsKey(name.toLowerCase()); }
    @Override public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    @Override public Collection<String> getHeaders(String name) { String v = headers.get(name.toLowerCase()); return v != null ? List.of(v) : List.of(); }
    @Override public Collection<String> getHeaderNames() { return headers.keySet(); }
    @Override public PrintWriter getWriter() { return writer; }
    @Override public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            public boolean isReady() { return true; }
            public void setWriteListener(WriteListener l) {}
            public void write(int b) { body.write(b); }
        };
    }
    @Override public void setContentLength(int len) { contentLength = len; }
    @Override public void setContentLengthLong(long len) { contentLength = (int) len; }
    @Override public void addCookie(Cookie cookie) { cookies.add(cookie); }
    @Override public void sendRedirect(String location) throws IOException { status = 302; setHeader("Location", location); }
    @Override public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException { status = sc; setHeader("Location", location); }
    @Override public Locale getLocale() { return locale; }
    @Override public void setLocale(Locale loc) { locale = loc; }
}
