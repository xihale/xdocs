package top.xihale.clouddoc.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.clouddoc.constant.ResponseCode;
import top.xihale.clouddoc.constant.UserStatus;
import top.xihale.clouddoc.exception.BusinessException;
import top.xihale.clouddoc.po.User;
import top.xihale.clouddoc.service.UserService;
import top.xihale.clouddoc.servlet.ArticleServlet;
import top.xihale.clouddoc.servlet.AuthServlet;
import top.xihale.clouddoc.servlet.SearchServlet;
import top.xihale.clouddoc.servlet.route.RouteRegistry;
import top.xihale.clouddoc.util.JwtUtil;
import top.xihale.clouddoc.util.ResponseUtils;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证过滤器，从 JWT Cookie 恢复用户状态，校验登录与封禁
 */
public class AuthFilter implements Filter {

    public static final String TOKEN_COOKIE_NAME = "clouddoc_token";

    /** 用户缓存，避免每次请求查库。TTL 60 秒 */
    private static final ConcurrentHashMap<Integer, CachedUser> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MILLIS = 60_000L;

    /**
     * 通过 @Public 注解自动收集的公开路径白名单
     */
    private static final Set<String> WHITELIST = RouteRegistry.scanPublicPaths(
            AuthServlet.class,
            ArticleServlet.class,
            SearchServlet.class
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding("UTF-8");
        }

        // WebSocket 升级请求由 @ServerEndpoint 自行通过 query param token 认证，
        // 不走 Cookie 认证，直接放行以免 filter chain 阻断握手。
        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade"))) {
            chain.doFilter(request, response);
            return;
        }

        var res = ResponseUtils.of(resp);
        String requestPath = getRequestPath(req);
        boolean whitelisted = isWhitelisted(requestPath);
        AuthState authState = resolveAuthState(req);

        req.setAttribute("banned", authState.banned());
        if (authState.status() != null) {
            req.setAttribute("userStatus", authState.status());
        }

        if (authState.user() != null) {
            req.setAttribute("userId", authState.userId());
            req.setAttribute("role", authState.role());
            req.setAttribute("currentUser", authState.user());
        }

        if (authState.banned() && !whitelisted) {
            res.error(ResponseCode.FORBIDDEN, "账号已被封禁");
            return;
        }

        if (authState.userId() == null && !whitelisted) {
            res.error(ResponseCode.UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private AuthState resolveAuthState(HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return new AuthState(null);
        }
        Integer role = null;
        Integer status = null;
        try {
            User user = getCachedUser(userId);
            role = user.getRole();
            status = user.getStatus();
            assert (status != null && role != null);
            UserService.requireActive(user);
            return new AuthState(user, role, status, false);
        } catch (BusinessException e) {
            if (e.getCode() == ResponseCode.FORBIDDEN.getCode()) {
                return new AuthState(null, role, status == null ? UserStatus.BANNED.getCode() : status, true);
            }
            return new AuthState(null);
        }
    }

    private User getCachedUser(int userId) {
        CachedUser cached = userCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.user;
        }
        User user = UserService.findUserById(userId);
        userCache.put(userId, new CachedUser(user));
        return user;
    }

    /** 清除指定用户的缓存（密码修改、状态变更时调用） */
    public static void invalidateCache(int userId) {
        userCache.remove(userId);
    }

    private Integer getUserIdFromToken(HttpServletRequest req) {
        String token = getTokenFromCookie(req);
        if (token == null) {
            return null;
        }
        return JwtUtil.getUserId(token);
    }

    /**
     * 从 Cookie 中读取 JWT token
     */
    static String getTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String getRequestPath(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        String requestUri = req.getRequestURI();
        return requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
    }

    private boolean isWhitelisted(String requestPath) {
        return WHITELIST.contains(requestPath);
    }

    private record AuthState(User user, Integer role, Integer status, boolean banned) {
        public AuthState(User user) {
            this(user, user == null ? null : user.getRole(), user == null ? null : user.getStatus(), false);
        }

        public Integer userId() {
            return user == null ? null : user.getId();
        }
    }

    private record CachedUser(User user, long timestamp) {
        CachedUser(User user) {
            this(user, System.currentTimeMillis());
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MILLIS;
        }
    }
}
