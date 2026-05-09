package top.xihale.xdocs.servlet.route;

/**
 * 路由键，由 HTTP 方法 + 路径组成
 */
public record RouteKey(RouteMethod method, String path) {

    public static RouteKey of(RouteMethod method, String rawPath) {
        return new RouteKey(method, normalizePath(rawPath));
    }

    public static String normalizePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return "/";
        }
        if (pathInfo.length() > 1 && pathInfo.endsWith("/")) {
            return pathInfo.substring(0, pathInfo.length() - 1);
        }
        return pathInfo;
    }

    public String displayName() {
        return STR."\{method.name()} \{path}";
    }
}
