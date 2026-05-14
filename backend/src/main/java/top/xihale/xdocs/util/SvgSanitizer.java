package top.xihale.xdocs.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * SVG 内容清洗工具，剥离 script 标签和事件属性，防止存储型 XSS
 */
public final class SvgSanitizer {

    private SvgSanitizer() {
    }

    /**
     * SVG 白名单：只允许安全的绘图标签和属性
     */
    private static final Safelist SVG_SAFELIST = Safelist.none()
            .addTags("svg", "g", "path", "circle", "rect", "line",
                    "ellipse", "polyline", "polygon", "text", "tspan",
                    "defs", "clipPath", "linearGradient", "radialGradient", "stop",
                    "use", "symbol", "title", "desc")
            .addAttributes("svg", "xmlns", "viewBox", "width", "height",
                    "fill", "stroke", "stroke-width", "class")
            .addAttributes("path", "d", "fill", "stroke", "stroke-width",
                    "stroke-linecap", "stroke-linejoin", "transform", "opacity")
            .addAttributes("circle", "cx", "cy", "r", "fill", "stroke", "stroke-width")
            .addAttributes("rect", "x", "y", "width", "height", "rx", "ry",
                    "fill", "stroke", "stroke-width")
            .addAttributes("line", "x1", "y1", "x2", "y2", "stroke", "stroke-width")
            .addAttributes("ellipse", "cx", "cy", "rx", "ry", "fill", "stroke")
            .addAttributes("polyline", "points", "fill", "stroke", "stroke-width")
            .addAttributes("polygon", "points", "fill", "stroke", "stroke-width")
            .addAttributes("text", "x", "y", "font-size", "font-family",
                    "fill", "text-anchor", "dominant-baseline")
            .addAttributes("tspan", "x", "y", "dx", "dy")
            .addAttributes("linearGradient", "id", "x1", "y1", "x2", "y2")
            .addAttributes("radialGradient", "id", "cx", "cy", "r", "fx", "fy")
            .addAttributes("stop", "offset", "stop-color", "stop-opacity")
            .addAttributes("clipPath", "id")
            .addAttributes("use", "href", "x", "y", "width", "height")
            .addAttributes("g", "transform", "fill", "stroke", "opacity")
            .addAttributes(":all", "id", "class");

    /**
     * 清洗 SVG 字节内容，返回安全后的字节
     *
     * @param svgBytes 原始 SVG 内容
     * @return 清洗后的 SVG 字节
     */
    public static byte[] sanitize(byte[] svgBytes) {
        String svg = new String(svgBytes, java.nio.charset.StandardCharsets.UTF_8);
        String clean = Jsoup.clean(svg, SVG_SAFELIST);
        return clean.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
