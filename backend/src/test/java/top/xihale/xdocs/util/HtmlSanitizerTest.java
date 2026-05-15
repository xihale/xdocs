package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    @Test
    void sanitizeArticleContent_removesScriptTag() {
        String input = "<p>Hello</p><script>alert('xss')</script>";
        String result = HtmlSanitizer.sanitizeArticleContent(input);
        assertFalse(result.contains("<script"));
        assertTrue(result.contains("<p>Hello</p>"));
    }

    @Test
    void sanitizeArticleContent_removesOnEventAttributes() {
        String input = "<div onclick=\"alert(1)\">click</div>";
        String result = HtmlSanitizer.sanitizeArticleContent(input);
        assertFalse(result.contains("onclick"));
    }

    @Test
    void sanitizeArticleContent_removesIframe() {
        String input = "<iframe src=\"evil.com\"></iframe><p>safe</p>";
        String result = HtmlSanitizer.sanitizeArticleContent(input);
        assertFalse(result.contains("<iframe"));
        assertTrue(result.contains("<p>safe</p>"));
    }

    @Test
    void sanitizeArticleContent_keepsCodeBlock() {
        String input = "<pre><code>int x = 1;</code></pre>";
        String result = HtmlSanitizer.sanitizeArticleContent(input);
        assertTrue(result.contains("<code>"));
        assertTrue(result.contains("<pre>"));
    }

    @Test
    void sanitizeArticleContent_nullInput() {
        assertNull(HtmlSanitizer.sanitizeArticleContent(null));
    }

    @Test
    void sanitizeArticleContent_emptyInput() {
        assertEquals("", HtmlSanitizer.sanitizeArticleContent(""));
    }

    @Test
    void sanitizeArticleContent_preservesMarkdownLineBreaks() {
        String input = "# Title\n\nParagraph with **bold**.\n\n## Checklist\n- [x] Done\n- [ ] Todo";
        String result = HtmlSanitizer.sanitizeArticleContent(input);

        assertEquals(input, result);
    }

    @Test
    void sanitizeArticleContent_removesScriptButKeepsMarkdownShape() {
        String input = "# Title\n\n<script>alert('xss')</script>\n\n- [x] Done";
        String result = HtmlSanitizer.sanitizeArticleContent(input);

        assertFalse(result.contains("<script"));
        assertTrue(result.contains("# Title\n\n"));
        assertTrue(result.contains("\n\n- [x] Done"));
    }

    @Test
    void stripHtml_removesAllTags() {
        String input = "<p>Hello <b>world</b></p><script>xss</script>";
        assertEquals("Hello world", HtmlSanitizer.stripHtml(input));
    }

    @Test
    void stripHtml_nullInput() {
        assertNull(HtmlSanitizer.stripHtml(null));
    }

    @Test
    void stripHtml_plainTextUnchanged() {
        assertEquals("just text", HtmlSanitizer.stripHtml("just text"));
    }
}
