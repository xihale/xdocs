package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SvgSanitizerTest {

    @Test
    void sanitize_removesScriptTag() {
        String svg = "<svg><script>alert('xss')</script><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>";
        byte[] result = SvgSanitizer.sanitize(svg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("<script"));
        assertTrue(resultStr.contains("<circle"));
    }

    @Test
    void sanitize_removesEventAttributes() {
        String svg = "<svg><rect onclick=\"alert(1)\" width=\"100\" height=\"100\"/></svg>";
        byte[] result = SvgSanitizer.sanitize(svg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("onclick"));
    }

    @Test
    void sanitize_keepsSafeElements() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">" +
                "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>" +
                "<path d=\"M10 10 L90 90\" stroke=\"black\"/>" +
                "</svg>";
        byte[] result = SvgSanitizer.sanitize(svg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("<circle"));
        assertTrue(resultStr.contains("<path"));
    }

    @Test
    void sanitize_removesForeignObject() {
        String svg = "<svg><foreignObject><body onload=\"alert(1)\">xss</body></foreignObject></svg>";
        byte[] result = SvgSanitizer.sanitize(svg.getBytes(StandardCharsets.UTF_8));
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertFalse(resultStr.contains("foreignObject"));
        assertFalse(resultStr.contains("onload"));
    }
}
