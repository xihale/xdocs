package top.xihale.xdocs.util;

import org.apache.tika.Tika;

import java.util.Map;
import java.util.Set;

/**
 * 文件类型校验工具，基于 Apache Tika magic number 检测真实 MIME 类型
 */
public final class FileTypeUtil {

    private FileTypeUtil() {
    }

    private static final Tika TIKA = new Tika();

    /**
     * 扩展名 → 允许的 MIME 类型集合
     */
    private static final Map<String, Set<String>> EXT_TO_MIME = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif"),
            "webp", Set.of("image/webp"),
            "svg", Set.of("image/svg+xml", "text/xml", "application/xml")
    );

    /**
     * 检测字节数组的真实 MIME 类型
     */
    public static String detectMimeType(byte[] content) {
        try {
            return TIKA.detect(content);
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    /**
     * 判断检测到的 MIME 类型是否与声明的扩展名匹配
     *
     * @param detectedType Tika 检测到的 MIME 类型
     * @param ext          声明的文件扩展名
     * @return true 如果匹配
     */
    public static boolean isAllowed(String detectedType, String ext) {
        Set<String> allowed = EXT_TO_MIME.get(ext.toLowerCase());
        if (allowed == null) {
            return false;
        }
        return allowed.contains(detectedType);
    }
}
