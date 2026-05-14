package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import top.xihale.xdocs.exception.AuthException;
import top.xihale.xdocs.exception.AuthException.AuthError;
import top.xihale.xdocs.filter.AuthFilter;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.util.EmailUtils;
import top.xihale.xdocs.util.JwtUtil;
import top.xihale.xdocs.util.Result;
import top.xihale.xdocs.util.TurnstileUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证相关接口
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {
    private static final long EMAIL_CODE_SEND_INTERVAL_SECONDS = 60;
    private static final int MAX_CODE_CHECK_ATTEMPTS = 5;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 24 * 3600; // 1 天
    private static final int IP_RATE_LIMIT_MAX_REQUESTS = 10;
    private static final int ACCOUNT_RATE_LIMIT_MAX_REQUESTS = 10;
    private static final ConcurrentHashMap<String, Long> emailCodeLastSentAtMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> codeCheckAttemptsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateLimitEntry> ipRateLimitMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateLimitEntry> accountRateLimitMap = new ConcurrentHashMap<>();

    static {
        // 后台定时清理过期限流记录，每 5 分钟执行一次
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "email-rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        long interval = EMAIL_CODE_SEND_INTERVAL_SECONDS * 5;
        cleaner.scheduleAtFixedRate(() -> {
            long now = Instant.now().getEpochSecond();
            emailCodeLastSentAtMap.entrySet().removeIf(e -> now - e.getValue() >= EMAIL_CODE_SEND_INTERVAL_SECONDS);
        }, interval, interval, TimeUnit.SECONDS);
        // 定时清理验证码尝试次数记录（5 分钟过期）
        ScheduledExecutorService attemptCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "code-attempt-cleaner");
            t.setDaemon(true);
            return t;
        });
        attemptCleaner.scheduleAtFixedRate(codeCheckAttemptsMap::clear, 5, 5, TimeUnit.MINUTES);
        // 定时清理限流记录（IP + 账号）
        ScheduledExecutorService rateLimitCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        rateLimitCleaner.scheduleAtFixedRate(() -> {
            long now = Instant.now().getEpochSecond();
            ipRateLimitMap.entrySet().removeIf(e -> now - e.getValue().windowStart >= RATE_LIMIT_WINDOW_SECONDS);
            accountRateLimitMap.entrySet().removeIf(e -> now - e.getValue().windowStart >= RATE_LIMIT_WINDOW_SECONDS);
        }, RATE_LIMIT_WINDOW_SECONDS, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    @Public
    @Post("/register")
    private Result<?> handleRegister(HttpServletRequest req, HttpServletResponse resp) {
        checkIpRateLimit(req, "register");
        String username = requiredParam(req, "username");
        String password = requiredParam(req, "password");
        String email = requiredParam(req, "email");
        checkAccountRateLimit("register:" + email.toLowerCase());
        String code = requiredParam(req, "code");
        String nickname = optionalParam(req, "nickname");

        // 校验邮件验证码
        HttpSession session = req.getSession();
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailForCode");

        // 检查验证码尝试次数
        String attemptKey = "register:" + session.getId();
        if (isCodeCheckExhausted(attemptKey, session)) {
            throw new AuthException(AuthError.EMAIL_CODE_INVALID);
        }

        if (sessionCode == null || !sessionCode.equalsIgnoreCase(code) || !email.equals(sessionEmail)) {
            throw new AuthException(AuthError.EMAIL_CODE_INVALID);
        }
        // 校验通过后清除
        session.removeAttribute("emailCode");
        session.removeAttribute("emailForCode");
        codeCheckAttemptsMap.remove(attemptKey);

        User user = UserService.register(username, password, email, nickname);
        setTokenCookie(resp, user.getId());
        return Result.success(user.toVO());
    }

    @Public
    @Post("/login")
    private Result<?> handleLogin(HttpServletRequest req, HttpServletResponse resp) {
        checkIpRateLimit(req, "login");
        String username = requiredParam(req, "username");
        checkAccountRateLimit("login:" + username.toLowerCase());
        String password = requiredParam(req, "password");
        String turnstileToken = requiredParam(req, "turnstileToken");

        // 校验 Turnstile 验证码
        if (!TurnstileUtils.verify(turnstileToken)) {
            throw new AuthException(AuthError.TURNSTILE_FAILED);
        }

        User user = UserService.login(username, password);
        setTokenCookie(resp, user.getId());
        return Result.success(user.toVO());
    }

    @Public
    @Post("/logout")
    private Result<?> handleLogout(HttpServletRequest req, HttpServletResponse resp) {
        clearTokenCookie(resp);
        return Result.success();
    }

    @Public
    @Get("/current")
    private Result<?> handleCurrent(HttpServletRequest req, HttpServletResponse resp) {
        User user = getOptionalCurrentUser(req);
        if (user == null) {
            return Result.success(null);
        }
        return Result.success(user.toVO());
    }

    @Public
    @Post("/send-code")
    private Result<?> handleSendEmailCode(HttpServletRequest req, HttpServletResponse resp) {
        // 人机验证：Turnstile
        String turnstileToken = requiredParam(req, "turnstileToken");
        if (!TurnstileUtils.verify(turnstileToken)) {
            throw new AuthException(AuthError.TURNSTILE_FAILED);
        }

        String email = requiredParam(req, "email");
        String type = optionalParamOrDefault(req, "type", "register");

        // 如果是重置密码，校验邮箱是否存在
        if ("reset".equals(type)) {
            UserService.ensureEmailRegisteredForReset(email);
        } else if ("register".equals(type)) {
            UserService.ensureEmailAvailableForRegister(email);
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
        return Result.success("验证码已发送");
    }

    @Public
    @Post("/reset-password")
    private Result<?> handleResetPassword(HttpServletRequest req, HttpServletResponse resp) {
        checkIpRateLimit(req, "reset-password");
        String email = requiredParam(req, "email");
        checkAccountRateLimit("reset-password:" + email.toLowerCase());
        String code = requiredParam(req, "code");
        String newPassword = requiredParam(req, "newPassword");

        HttpSession session = req.getSession();
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailForCode");

        // 检查验证码尝试次数
        String attemptKey = "reset:" + session.getId();
        if (isCodeCheckExhausted(attemptKey, session)) {
            throw new AuthException(AuthError.VERIFY_CODE_INVALID);
        }

        if (sessionCode == null || !sessionCode.equalsIgnoreCase(code) || !email.equals(sessionEmail)) {
            throw new AuthException(AuthError.VERIFY_CODE_INVALID);
        }
        session.removeAttribute("emailCode");
        session.removeAttribute("emailForCode");
        codeCheckAttemptsMap.remove(attemptKey);

        UserService.resetPassword(email, newPassword);
        return Result.success("密码已重置");
    }

    /**
     * 限流记录
     */
    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    /**
     * IP 级限流检查，超过阈值抛出 429
     */
    private void checkIpRateLimit(HttpServletRequest req, String action) {
        String key = "ip:" + action + ":" + req.getRemoteAddr();
        checkRateLimit(ipRateLimitMap, key, IP_RATE_LIMIT_MAX_REQUESTS);
    }

    /**
     * 账号级限流检查，超过阈值抛出 429
     */
    private void checkAccountRateLimit(String key) {
        checkRateLimit(accountRateLimitMap, "account:" + key, ACCOUNT_RATE_LIMIT_MAX_REQUESTS);
    }

    private void checkRateLimit(ConcurrentHashMap<String, RateLimitEntry> map, String key, int maxRequests) {
        long now = Instant.now().getEpochSecond();
        RateLimitEntry limit = map.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= RATE_LIMIT_WINDOW_SECONDS) {
                return new RateLimitEntry(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (limit.count.get() > maxRequests) {
            throw new AuthException(AuthError.EMAIL_CODE_TOO_FREQUENT);
        }
    }

    /**
     * 检查验证码校验次数是否已耗尽，若未耗尽则递增计数
     */
    private boolean isCodeCheckExhausted(String attemptKey, HttpSession session) {
        AtomicInteger attempts = codeCheckAttemptsMap.computeIfAbsent(attemptKey, k -> new AtomicInteger(0));
        if (attempts.incrementAndGet() > MAX_CODE_CHECK_ATTEMPTS) {
            // 耗尽次数，使验证码失效
            session.removeAttribute("emailCode");
            session.removeAttribute("emailForCode");
            return true;
        }
        return false;
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
