package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import top.xihale.xdocs.exception.AuthException;
import top.xihale.xdocs.exception.AuthException.AuthError;
import top.xihale.xdocs.exception.UserException;
import top.xihale.xdocs.exception.UserException.UserError;
import top.xihale.xdocs.filter.AuthFilter;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.util.EmailUtils;
import top.xihale.xdocs.util.JwtUtil;
import top.xihale.xdocs.util.ResponseUtils;
import top.xihale.xdocs.util.TurnstileUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证相关接口
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {
    private static final long EMAIL_CODE_SEND_INTERVAL_SECONDS = 60;
    private static final ConcurrentHashMap<String, Long> emailCodeLastSentAtMap = new ConcurrentHashMap<>();

    @Public
    @Post("/register")
    private void handleRegister(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String username = requiredParam(req, "username");
        String password = requiredParam(req, "password");
        String email = requiredParam(req, "email");
        String code = requiredParam(req, "code");
        String nickname = optionalParam(req, "nickname");

        // 校验邮件验证码
        HttpSession session = req.getSession();
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailForCode");

        if (sessionCode == null || !sessionCode.equalsIgnoreCase(code) || !email.equals(sessionEmail)) {
            throw new AuthException(AuthError.EMAIL_CODE_INVALID);
        }
        // 校验通过后清除
        session.removeAttribute("emailCode");
        session.removeAttribute("emailForCode");

        User user = UserService.register(username, password, email, nickname);
        setTokenCookie(res.getRawResponse(), user.getId());
        user.setPassword(null);
        res.ok(user);
    }

    @Public
    @Post("/login")
    private void handleLogin(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String username = requiredParam(req, "username");
        String password = requiredParam(req, "password");
        String turnstileToken = requiredParam(req, "turnstileToken");

        // 校验 Turnstile 验证码
        if (!TurnstileUtils.verify(turnstileToken)) {
            throw new AuthException(AuthError.TURNSTILE_FAILED);
        }

        User user = UserService.login(username, password);
        setTokenCookie(res.getRawResponse(), user.getId());
        user.setPassword(null);
        res.ok(user);
    }

    @Public
    @Post("/logout")
    private void handleLogout(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        clearTokenCookie(res.getRawResponse());
        res.ok();
    }

    @Public
    @Get("/current")
    private void handleCurrent(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        User user = getOptionalCurrentUser(req);
        if (user == null) {
            res.ok(null);
            return;
        }
        user.setPassword(null);
        res.ok(user);
    }

    @Public
    @Post("/send-code")
    private void handleSendEmailCode(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        // 人机验证：Turnstile
        String turnstileToken = requiredParam(req, "turnstileToken");
        if (!TurnstileUtils.verify(turnstileToken)) {
            throw new AuthException(AuthError.TURNSTILE_FAILED);
        }

        String email = requiredParam(req, "email");
        String type = optionalParamOrDefault(req, "type", "register");

        // 如果是重置密码，校验邮箱是否存在
        if ("reset".equals(type)) {
            if (UserService.emailNotExists(email)) {
                throw new UserException(UserError.EMAIL_NOT_REGISTERED);
            }
        } else if ("register".equals(type)) {
            if (UserService.emailExists(email)) {
                throw new UserException(UserError.EMAIL_EXISTS);
            }
        }

        HttpSession session = req.getSession();
        long now = Instant.now().getEpochSecond();
        String rateLimitKey = type + ":" + email.toLowerCase();
        Long lastSentAt = emailCodeLastSentAtMap.get(rateLimitKey);
        if (lastSentAt != null && now - lastSentAt < EMAIL_CODE_SEND_INTERVAL_SECONDS) {
            throw new AuthException(AuthError.EMAIL_CODE_TOO_FREQUENT);
        }

        // 生成 6 位数字验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        session.setAttribute("emailCode", code);
        session.setAttribute("emailForCode", email);
        // 设置过期时间 (例如 5 分钟)
        session.setMaxInactiveInterval(300);

        EmailUtils.sendCode(email, code);
        emailCodeLastSentAtMap.put(rateLimitKey, now);
        res.ok("验证码已发送");
    }

    @Get("/ws-token")
    private void handleWsToken(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        Integer userId = getOptionalUserId(req);
        if (userId == null) {
            throw new AuthException(AuthError.NOT_LOGGED_IN);
        }
        String token = JwtUtil.generateToken(userId);
        res.ok(Map.of("token", token));
    }

    @Public
    @Post("/reset-password")
    private void handleResetPassword(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String email = requiredParam(req, "email");
        String code = requiredParam(req, "code");
        String newPassword = requiredParam(req, "newPassword");

        HttpSession session = req.getSession();
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailForCode");

        if (sessionCode == null || !sessionCode.equalsIgnoreCase(code) || !email.equals(sessionEmail)) {
            throw new AuthException(AuthError.VERIFY_CODE_INVALID);
        }
        session.removeAttribute("emailCode");
        session.removeAttribute("emailForCode");

        UserService.resetPassword(email, newPassword);
        res.ok("密码已重置");
    }

    /**
     * 登录/注册成功后，将 JWT 写入 HttpOnly Cookie
     */
    private void setTokenCookie(HttpServletResponse resp, int userId) {
        String token = JwtUtil.generateToken(userId);
        Cookie cookie = new Cookie(AuthFilter.TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 3600); // 7 天，与 JWT 过期时间一致
        // cookie.setSecure(true); // 生产环境启用 HTTPS 时打开
        resp.addCookie(cookie);
    }

    /**
     * 登出时清除 JWT Cookie
     */
    private void clearTokenCookie(HttpServletResponse resp) {
        Cookie cookie = new Cookie(AuthFilter.TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
    }
}
