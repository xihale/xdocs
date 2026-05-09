package top.xihale.clouddoc.util;

import com.google.gson.annotations.SerializedName;
import top.xihale.clouddoc.config.WebConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cloudflare Turnstile 验证工具类
 */
public class TurnstileUtils {
    private static final Logger LOGGER = Logger.getLogger(TurnstileUtils.class.getName());
    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * 验证 Turnstile 令牌
     *
     * @param token 客户端传回的令牌
     * @return 验证是否通过
     */
    public static boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String secret = WebConfig.getTurnstileSecret();
            Map<String, String> params = Map.of(
                    "secret", secret,
                    "response", token
            );

            // 构建表单数据（URL 编码）
            StringBuilder formData = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!formData.isEmpty()) {
                    formData.append("&");
                }
                formData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                formData.append("=");
                formData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData.toString()))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "Turnstile verification failed with status: " + response.statusCode());
                return false;
            }

            TurnstileResponse turnstileResponse = JsonUtils.getGson().fromJson(response.body(), TurnstileResponse.class);
            return turnstileResponse != null && turnstileResponse.success;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying Turnstile token", e);
            return false;
        }
    }

    private static class TurnstileResponse {
        public boolean success;
        @SerializedName("error-codes")
        public List<String> errorCodes;
        @SerializedName("challenge_ts")
        public String challengeTs;
        public String hostname;
    }
}
