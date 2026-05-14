package top.xihale.xdocs.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JWT 工具类，负责 Token 的生成与解析
 */
public final class JwtUtil {

    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());
    private static final long DEFAULT_EXPIRATION_SECONDS = 604800L; // 7 天

    private static final SecretKey KEY;
    private static final long EXPIRATION_MILLIS;

    static {
        Properties props = new Properties();
        try (InputStream in = JwtUtil.class.getClassLoader().getResourceAsStream("web.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "读取 web.properties 失败", e);
        }

        String secret = props.getProperty("jwt.secret");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("未配置 jwt.secret，请在 web.properties 中设置至少 32 字节的密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 密钥长度不足，至少需要 32 字节（256 位）");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expStr = props.getProperty("jwt.expiration");
        long expSeconds = DEFAULT_EXPIRATION_SECONDS;
        if (expStr != null && !expStr.isBlank()) {
            try {
                expSeconds = Long.parseLong(expStr.trim());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "jwt.expiration 配置无效，使用默认值 7 天");
            }
        }
        EXPIRATION_MILLIS = expSeconds * 1000;
    }

    private JwtUtil() {
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户 ID
     * @return 签名后的 JWT 字符串
     */
    public static String generateToken(int userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MILLIS))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析并验证 Token
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims，无效时返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "JWT 解析失败: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取 userId
     *
     * @param token JWT 字符串
     * @return userId，无效 Token 返回 null
     */
    public static Integer getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
