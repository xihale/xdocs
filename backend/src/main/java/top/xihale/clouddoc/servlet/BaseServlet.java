package top.xihale.clouddoc.servlet;

import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.clouddoc.constant.Role;
import top.xihale.clouddoc.constant.UserStatus;
import top.xihale.clouddoc.exception.AuthException;
import top.xihale.clouddoc.exception.AuthException.AuthError;
import top.xihale.clouddoc.exception.ParamException;
import top.xihale.clouddoc.exception.ParamException.ParamError;
import top.xihale.clouddoc.po.User;
import top.xihale.clouddoc.service.UserService;
import top.xihale.clouddoc.servlet.route.RouteMethod;
import top.xihale.clouddoc.servlet.route.RouteKey;
import top.xihale.clouddoc.servlet.route.RouteRegistry;
import top.xihale.clouddoc.util.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 * 业务 Servlet 基类，提供统一的路由分发、参数获取和用户状态读取
 */
public abstract class BaseServlet extends HttpServlet {

    private RouteRegistry routeRegistry = RouteRegistry.empty();

    protected String normalizePath(String pathInfo) {
        return RouteKey.normalizePath(pathInfo);
    }

    /** JSON body 缓存 key */
    private static final String JSON_BODY_KEY = "_jsonBody";
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    /**
     * 解析 JSON body 并缓存到 request attribute
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonBody(HttpServletRequest req) {
        Map<String, Object> body = (Map<String, Object>) req.getAttribute(JSON_BODY_KEY);
        if (body != null) return body;

        body = Map.of();
        try {
            String ct = req.getContentType();
            if (ct != null && ct.contains("application/json")) {
                String raw = new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                if (!raw.isBlank()) {
                    body = JsonUtils.getGson().fromJson(raw, MAP_TYPE);
                    if (body == null) body = Map.of();
                }
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            throw ParamError.JSON_FORMAT_ERROR.with(": " + e.getMessage());
        } catch (Exception e) {
            // 非 JSON Content-Type 或空 body，忽略
        }
        req.setAttribute(JSON_BODY_KEY, body);
        return body;
    }

    protected String optionalParam(HttpServletRequest req, String name) {
        // 1. query / form params
        String value = req.getParameter(name);
        if (value != null) return value.trim();

        // 2. JSON body
        Map<String, Object> body = getJsonBody(req);
        Object v = body.get(name);
        if (v == null) return null;
        // Gson 将 JSON number 解析为 Double，String.valueOf(1.0) = "1.0"
        // 如果是整数型 Number，转成整数字符串避免下游 parseInt("1.0") 失败
        if (v instanceof Number n) {
            if (n.doubleValue() == n.intValue() && !Double.isInfinite(n.doubleValue())) {
                return String.valueOf(n.intValue());
            }
            return String.valueOf(v).trim();
        }
        return String.valueOf(v).trim();
    }

    protected String optionalParamOrDefault(HttpServletRequest req, String name, String defaultValue) {
        String value = optionalParam(req, name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    protected String optionalRawParam(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value != null) return value;
        Map<String, Object> body = getJsonBody(req);
        Object v = body.get(name);
        if (v == null) return null;
        if (v instanceof Number n) {
            if (n.doubleValue() == n.intValue() && !Double.isInfinite(n.doubleValue())) {
                return String.valueOf(n.intValue());
            }
        }
        return String.valueOf(v);
    }

    protected String requiredParam(HttpServletRequest req, String name) {
        String value = optionalParam(req, name);
        if (value == null || value.isBlank()) {
            throw ParamError.MISSING_PARAM.with("'" + name + "'");
        }
        return value;
    }

    protected String requiredRawParam(HttpServletRequest req, String name) {
        String value = optionalRawParam(req, name);
        if (value == null || value.isBlank()) {
            throw ParamError.MISSING_PARAM.with("'" + name + "'");
        }
        return value;
    }

    protected Integer optionalIntParam(HttpServletRequest req, String name) {
        // 1. query / form params
        String strVal = req.getParameter(name);
        if (strVal != null) {
            try {
                return Integer.parseInt(strVal.trim());
            } catch (NumberFormatException e) {
                throw ParamError.PARAM_NOT_INT.with("'" + name + "' 实际值: '" + strVal.trim() + "'");
            }
        }
        // 2. JSON body — 直接取 Number，避免 Double→"1.0"→parseInt 失败
        Map<String, Object> body = getJsonBody(req);
        Object v = body.get(name);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw ParamError.PARAM_NOT_INT.with("'" + name + "' 实际值: '" + v + "'");
        }
    }

    protected int requiredIntParam(HttpServletRequest req, String name) {
        Integer val = optionalIntParam(req, name);
        if (val == null) {
            throw ParamError.MISSING_PARAM.with("'" + name + "'");
        }
        return val;
    }

    protected int optionalIntParamOrDefault(HttpServletRequest req, String name, int defaultValue) {
        Integer val = optionalIntParam(req, name);
        return val != null ? val : defaultValue;
    }

    /**
     * 从请求参数中解析用户 ID（支持 userId 数字或 username 字符串）
     */
    protected int resolveUserId(HttpServletRequest req) {
        String userIdStr = optionalParam(req, "userId");
        String username = optionalParam(req, "username");
        if (userIdStr != null) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                throw new ParamException(ParamError.USER_ID_NOT_INT);
            }
        } else if (username != null) {
            return UserService.findUserByUsername(username).getId();
        } else {
            throw new ParamException(ParamError.USER_ID_OR_USERNAME_REQUIRED);
        }
    }

    protected Integer getOptionalUserId(HttpServletRequest req) {
        return (req.getAttribute("userId") instanceof Number n) ? n.intValue() : null;
    }

    protected User getOptionalCurrentUser(HttpServletRequest req) {
        Object user = req.getAttribute("currentUser");
        return user instanceof User currentUser ? currentUser : null;
    }

    protected Integer getOptionalRoleCode(HttpServletRequest req) {
        return (req.getAttribute("role") instanceof Number n) ? n.intValue() : null;
    }

    protected Role getOptionalCurrentRole(HttpServletRequest req) {
        Integer code = getOptionalRoleCode(req);
        return code != null ? Role.fromCode(code) : null;
    }

    protected Integer getOptionalUserStatusCode(HttpServletRequest req) {
        return (req.getAttribute("userStatus") instanceof Number n) ? n.intValue() : null;
    }

    protected UserStatus getOptionalCurrentUserStatus(HttpServletRequest req) {
        Integer code = getOptionalUserStatusCode(req);
        return code != null ? UserStatus.fromCode(code) : null;
    }

    protected boolean isCurrentUserBanned(HttpServletRequest req) {
        Object banned = req.getAttribute("banned");
        return banned instanceof Boolean bannedValue && bannedValue;
    }

    protected int getRequiredUserId(HttpServletRequest req) {
        return Optional.ofNullable(getOptionalUserId(req))
                .orElseThrow(() -> new AuthException(AuthError.NOT_LOGGED_IN));
    }

    protected User getRequiredCurrentUser(HttpServletRequest req) {
        return Optional.ofNullable(getOptionalCurrentUser(req))
                .orElseThrow(() -> new AuthException(AuthError.NOT_LOGGED_IN));
    }

    protected Role getRequiredCurrentRole(HttpServletRequest req) {
        return Optional.ofNullable(getOptionalCurrentRole(req))
                .orElseThrow(() -> new AuthException(AuthError.NOT_LOGGED_IN));
    }

    @Override
    public void init() throws ServletException {
        super.init();
        routeRegistry = RouteRegistry.scan(getClass(), BaseServlet.class);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.GET, req, resp);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.POST, req, resp);
    }

    @Override
    protected final void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.PUT, req, resp);
    }

    @Override
    protected final void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.DELETE, req, resp);
    }

    @Override
    protected final void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.PATCH, req, resp);
    }

    @Override
    protected final void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.OPTIONS, req, resp);
    }

    @Override
    protected final void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(RouteMethod.HEAD, req, resp);
    }

    private void dispatch(RouteMethod routeMethod, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        routeRegistry.dispatch(this, routeMethod, req, resp);
    }
}
