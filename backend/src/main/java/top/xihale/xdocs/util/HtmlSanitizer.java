package top.xihale.xdocs.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * HTML 内容清洗工具，用于防御 XSS 攻击
 * <p>
 * 策略：
 * - 文章内容（Markdown）：基于正则移除危险 HTML 标签和属性，保留 Markdown 格式
 * - 评论内容：剥离所有 HTML 标签，只保留纯文本
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    // ==================== 文章内容（Markdown）清洗 ====================

    /** 危险 HTML 标签：移除整个标签及其内容 */
    private static final Pattern DANGEROUS_TAGS = Pattern.compile(
            "<\\s*/?(script|iframe|object|embed|applet|form|input|meta|link|base|frame|frameset|noscript|template)"
                    + "(?:\\s[^>]*)?/?>.*?</?\\1[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 自闭合危险标签（无内容） */
    private static final Pattern DANGEROUS_SELF_CLOSING = Pattern.compile(
            "<\\s*/?(script|iframe|object|embed|applet|form|input|meta|link|base|frame|frameset|noscript|template)"
                    + "(?:\\s[^>]*)?/?>",
            Pattern.CASE_INSENSITIVE);

    /** on* 事件属性 */
    private static final Pattern EVENT_ATTRS = Pattern.compile(
            "\\s+on\\w+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)",
            Pattern.CASE_INSENSITIVE);

    /** javascript:/vbscript:/data: 协议链接 */
    private static final Pattern DANGEROUS_PROTOCOLS = Pattern.compile(
            "(href|src|action|data|formaction)\\s*=\\s*[\"']?\\s*(javascript|vbscript|data)\\s*:",
            Pattern.CASE_INSENSITIVE);

    /**
     * 清洗 Markdown 文章内容中的危险 HTML。
     * <p>
     * 使用正则移除 script/iframe/object/embed 等标签及其内容，
     * 移除所有 on* 事件属性和 javascript:/vbscript:/data: 协议链接，
     * 保留 Markdown 格式（换行、缩进等）和其他合法 HTML。
     *
     * @param content 原始 Markdown 内容（可能包含内嵌 HTML）
     * @return 清洗后的内容
     */
    public static String sanitizeArticleContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String result = content;
        result = DANGEROUS_TAGS.matcher(result).replaceAll("");
        result = DANGEROUS_SELF_CLOSING.matcher(result).replaceAll("");
        result = EVENT_ATTRS.matcher(result).replaceAll("");
        result = DANGEROUS_PROTOCOLS.matcher(result).replaceAll("$1=\"\"");
        return result;
    }

    // ==================== 评论内容清洗 ====================

    /** 危险标签内容（用于 stripHtml：移除标签及其内部文本） */
    private static final Pattern DANGEROUS_TAG_CONTENT = Pattern.compile(
            "<(script|iframe|object|embed|applet|style|template)[^>]*>.*?</\\1\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
        // 先移除危险标签及其内容，再移除剩余标签
        String result = DANGEROUS_TAG_CONTENT.matcher(content).replaceAll("");
        return result.replaceAll("<[^>]+>", "");
    }
}
