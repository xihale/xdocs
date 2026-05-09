package top.xihale.xdocs.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web 全局配置（CORS、前端地址等）
 */
public class WebConfig {
    private static final Logger LOGGER = Logger.getLogger(WebConfig.class.getName());
    private static final Properties PROPS = new Properties();
    private static volatile boolean initialized = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream in = WebConfig.class.getClassLoader().getResourceAsStream("web.properties")) {
            if (in == null) {
                LOGGER.log(Level.SEVERE, "无法在 classpath 中找到 web.properties，CORS 配置将为空");
                return;
            }
            PROPS.load(in);
            initialized = true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "读取 web.properties 失败，CORS 配置将为空", e);
        }
    }

    private WebConfig() {
    }

    public static List<String> getAllowedOrigins() {
        return splitCsv("cors.allowed.origins");
    }

    public static String getAllowedMethods() {
        return PROPS.getProperty("cors.allowed.methods", "GET,POST,PUT,DELETE,OPTIONS");
    }

    public static String getAllowedHeaders() {
        return PROPS.getProperty("cors.allowed.headers", "Content-Type,X-Requested-With");
    }

    public static boolean isAllowCredentials() {
        return Boolean.parseBoolean(PROPS.getProperty("cors.allow.credentials", "true"));
    }

    public static String getJwtSecret() {
        return PROPS.getProperty("jwt.secret", "default-secret-key-at-least-32-bytes-long");
    }

    public static String getTurnstileSecret() {
        return PROPS.getProperty("turnstile.secret", "1x0000000000000000000000000000000AA");
    }

    public static long getJwtExpiration() {
        return Long.parseLong(PROPS.getProperty("jwt.expiration", "604800"));
    }

    public static String getMailSmtpHost() {
        return PROPS.getProperty("mail.smtp.host");
    }

    public static int getMailSmtpPort() {
        return Integer.parseInt(PROPS.getProperty("mail.smtp.port", "465"));
    }

    public static String getMailSmtpUsername() {
        return PROPS.getProperty("mail.smtp.username");
    }

    public static String getMailSmtpPassword() {
        return PROPS.getProperty("mail.smtp.password");
    }

    public static String getMailFrom() {
        return PROPS.getProperty("mail.from");
    }

    public static boolean isMailSslEnable() {
        return Boolean.parseBoolean(PROPS.getProperty("mail.ssl.enable", "true"));
    }

    public static boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        // 精确匹配
        if (getAllowedOrigins().contains(origin)) return true;
        // localhost / 127.0.0.1 任意端口放行
        return origin.matches("^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?(/.*)?$");
    }

    private static List<String> splitCsv(String key) {
        String raw = PROPS.getProperty(key, "");
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
