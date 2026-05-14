package top.xihale.xdocs.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML 内容清洗工具，用于防御 XSS 攻击
 * <p>
 * 基于 Jsoup 实现，策略：
 * - 文章内容：使用宽松白名单，允许大部分 HTML 标签，但移除危险标签和属性
 * - 评论内容：剥离所有 HTML 标签，只保留纯文本
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * 文章内容白名单：基于 relaxed，额外允许 code/pre/samp/kbd/var 等代码标签
     */
    private static final Safelist ARTICLE_SAFELIST = Safelist.relaxed()
            .addTags("code", "pre", "samp", "kbd", "var", "sub", "sup", "mark", "abbr", "details", "summary")
            .addAttributes(":all", "class", "id", "title", "lang", "dir");

    /**
     * 清洗 Markdown 文章内容中的危险 HTML。
     * <p>
     * 使用 Jsoup 白名单机制，移除 script/iframe/object/embed 等标签，
     * 移除所有 on* 事件属性和 javascript:/vbscript:/data: 协议链接，
     * 保留其他合法 HTML。
     *
     * @param content 原始 Markdown 内容（可能包含内嵌 HTML）
     * @return 清洗后的内容
     */
    public static String sanitizeArticleContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return Jsoup.clean(content, ARTICLE_SAFELIST);
    }

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
        return Jsoup.clean(content, Safelist.none());
    }
}
