package top.xihale.clouddoc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 文件存储配置
 */
public class StorageConfig {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = StorageConfig.class.getClassLoader().getResourceAsStream("storage.properties")) {
            if (in == null) {
                throw new IllegalStateException("无法读取 storage.properties");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("无法读取 storage.properties", e);
        }
    }

    private StorageConfig() {
    }

    public static String getStoragePath() {
        return PROPS.getProperty("file.storage.path");
    }

    public static List<String> getAllowedExtensions() {
        String raw = PROPS.getProperty("file.allowed.extensions");
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .map(s -> s.startsWith(".") ? s.substring(1) : s)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static long getMaxSizeBytes() {
        return Long.parseLong(PROPS.getProperty("file.max-size-bytes"));
    }
}
