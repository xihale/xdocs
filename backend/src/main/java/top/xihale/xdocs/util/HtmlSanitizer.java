package top.xihale.xdocs.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * HTML 内容清洗工具，用于防御 XSS 攻击
 * <p>
 * 策略：
 * - 文章内容：移除危险标签（script/iframe/object/embed），移除所有 on* 事件属性，保留其他 HTML
 * - 评论内容：移除所有 HTML 标签
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * 危险 HTML 标签名（小写），这些标签的内容也会被一并移除
     */
    private static final Set<String> DANGEROUS_TAGS = Set.of(
            "script", "iframe", "object", "embed", "applet", "form", "base"
    );

    /**
     * 匹配危险标签及其内容，如 <script ...>...</script>（含多行内容）
     * 用 DOTALL 模式让 . 匹配换行
     */
    private static final Pattern DANGEROUS_TAG_PATTERN = buildDangerousTagPattern();

    /**
     * 匹配 on* 事件属性，如 onclick="..."、onerror='...'、onload = ...
     * 也处理无引号属性值的情况
     */
    private static final Pattern EVENT_ATTR_PATTERN = Pattern.compile(
            "\\bon\\w+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配 javascript: / vbscript: / data: 协议的 href/src/action 属性
     */
    private static final Pattern DANGEROUS_PROTOCOL_PATTERN = Pattern.compile(
            "(href|src|action|formaction)\\s*=\\s*[\"']\\s*(?:javascript|vbscript|data)\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配所有 HTML 标签（用于评论等纯文本场景）
     */
    private static final Pattern ALL_TAGS_PATTERN = Pattern.compile("<[^>]+>");

    // ==================== 文章内容：粗粒度清洗 ====================

    /**
     * 清洗 Markdown 文章内容中的危险 HTML。
     * <p>
     * 移除 script/iframe/object/embed 等标签及其内容，
     * 移除所有 on* 事件属性，
     * 移除 javascript:/vbscript:/data: 协议链接，
     * 保留其他合法 HTML（如 div、table、code 等）。
     *
     * @param content 原始 Markdown 内容（可能包含内嵌 HTML）
     * @return 清洗后的内容
     */
    public static String sanitizeArticleContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String result = content;
        // 1. 移除危险标签及其内容
        result = DANGEROUS_TAG_PATTERN.matcher(result).replaceAll("");
        // 2. 移除 on* 事件属性
        result = EVENT_ATTR_PATTERN.matcher(result).replaceAll("");
        // 3. 移除危险协议
        result = DANGEROUS_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        return result;
    }

    // ==================== 评论内容：剥离所有 HTML ====================

    /**
     * 剥离评论中的所有 HTML 标签，只保留纯文本。
     *
     * @param content 原始评论内容
     * @return 纯文本内容
     */
    public static String stripHtml(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return ALL_TAGS_PATTERN.matcher(content).replaceAll("");
    }

    // ==================== 内部工具 ====================

    private static Pattern buildDangerousTagPattern() {
        // 构建正则：<tag ...>...</tag> 或 <tag .../>（自闭合）
        // 对每个危险标签生成 (<tag\b[^>]*>[\s\S]*?</\1>|<tag\b[^>]*/?>)
        StringBuilder sb = new StringBuilder("(?i)(");
        boolean first = true;
        for (String tag : DANGEROUS_TAGS) {
            if (!first) sb.append("|");
            first = false;
            sb.append("<")
                    .append(tag)
                    .append("\\b[^>]*>[\\s\\S]*?</")
                    .append(tag)
                    .append("\\s*>")
                    .append("|<")
                    .append(tag)
                    .append("\\b[^>]*/?>");
        }
        sb.append(")");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
