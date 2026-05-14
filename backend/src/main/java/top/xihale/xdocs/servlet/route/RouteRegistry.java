package top.xihale.xdocs.servlet.route;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.constant.ResponseCode;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * 路由注册中心，扫描 Servlet 类上的路由注解并建立映射
 */
public final class RouteRegistry {

    private static final List<ShortcutAnnotation<?>> SHORTCUT_ANNOTATIONS = List.of(
            new ShortcutAnnotation<>(Get.class, RouteMethod.GET, "@Get", Get::value),
            new ShortcutAnnotation<>(Post.class, RouteMethod.POST, "@Post", Post::value),
            new ShortcutAnnotation<>(Put.class, RouteMethod.PUT, "@Put", Put::value),
            new ShortcutAnnotation<>(Delete.class, RouteMethod.DELETE, "@Delete", Delete::value),
            new ShortcutAnnotation<>(Patch.class, RouteMethod.PATCH, "@Patch", Patch::value),
            new ShortcutAnnotation<>(Options.class, RouteMethod.OPTIONS, "@Options", Options::value),
            new ShortcutAnnotation<>(Head.class, RouteMethod.HEAD, "@Head", Head::value)
    );

    private static final RouteRegistry EMPTY = new RouteRegistry(Map.of());

    private final Map<RouteKey, RouteHandler> routes;

    private RouteRegistry(Map<RouteKey, RouteHandler> routes) {
        this.routes = routes;
    }

    public static RouteRegistry empty() {
        return EMPTY;
    }

    /**
     * 扫描指定 Servlet 类上的路由注解，建立路由映射表
     */
    public static RouteRegistry scan(Class<?> servletClass, Class<?> stopClassExclusive) throws ServletException {
        Map<RouteKey, RouteHandler> routes = new LinkedHashMap<>();

        for (Class<?> current = servletClass; current != null && current != stopClassExclusive; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                List<RouteDefinition> definitions = resolveRouteDefinitions(method);
                if (definitions.isEmpty()) {
                    continue;
                }

                RouteHandler handler = RouteHandler.create(method);
                for (RouteDefinition definition : definitions) {
                    RouteHandler existing = routes.putIfAbsent(definition.key(), handler);
                    if (existing != null) {
                        throw new ServletException(STR."检测到重复路由: [\{definition.key().displayName()}] 已映射到 \{existing.describe()}，不能再次映射到 \{handler.describe()}");
                    }
                }
            }
        }

        return new RouteRegistry(Map.copyOf(routes));
    }

    /**
     * 根据 HTTP 方法和请求路径分发到对应的路由处理器
     */
    public void dispatch(Object target, RouteMethod routeMethod, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RouteKey routeKey = RouteKey.of(routeMethod, req.getPathInfo());
        RouteHandler handler = routes.get(routeKey);
        if (handler == null) {
            ResponseUtils.writeError(resp, ResponseCode.NOT_FOUND);
            return;
        }
        handler.invoke(target, routeKey, req, resp);
    }

    private static List<RouteDefinition> resolveRouteDefinitions(Method method) throws ServletException {
        Map<RouteKey, RouteDefinition> definitions = new LinkedHashMap<>();

        for (ShortcutAnnotation<?> shortcut : SHORTCUT_ANNOTATIONS) {
            collectShortcut(definitions, method, shortcut);
        }

        for (Route route : method.getAnnotationsByType(Route.class)) {
            register(definitions, method, RouteKey.of(route.method(), route.value()), "@Route");
        }

        return List.copyOf(definitions.values());
    }

    private static <A extends Annotation> void collectShortcut(Map<RouteKey, RouteDefinition> definitions,
                                                               Method method,
                                                               ShortcutAnnotation<A> shortcut) throws ServletException {
        for (A annotation : method.getAnnotationsByType(shortcut.annotationType())) {
            RouteKey routeKey = RouteKey.of(shortcut.routeMethod(), shortcut.pathExtractor().apply(annotation));
            register(definitions, method, routeKey, shortcut.source());
        }
    }

    private static void register(Map<RouteKey, RouteDefinition> definitions,
                                 Method method,
                                 RouteKey routeKey,
                                 String source) throws ServletException {
        RouteDefinition definition = new RouteDefinition(routeKey, source);
        RouteDefinition existing = definitions.putIfAbsent(routeKey, definition);
        if (existing != null) {
            throw new ServletException(STR."方法 \{describe(method)} 上存在重复路由声明: [\{routeKey.displayName()}]，来源 \{existing.source()} 与 \{source}");
        }
    }

    private static String describe(Method method) {
        return STR."\{method.getDeclaringClass().getName()}.\{method.getName()}";
    }

    private record ShortcutAnnotation<A extends Annotation>(Class<A> annotationType,
                                                            RouteMethod routeMethod,
                                                            String source,
                                                            Function<A, String> pathExtractor) {
    }

    private record RouteDefinition(RouteKey key, String source) {
    }

    /**
     * 扫描一组 Servlet 类，收集标记了 @Public 注解的路由的完整路径。
     * <p>
     * 完整路径 = @WebServlet 的 basePath + 路由注解的 value。
     *
     * @param servletClasses 要扫描的 Servlet 类
     * @return 所有公开路由的完整路径集合
     */
    public static Set<String> scanPublicPaths(Class<?>... servletClasses) {
        Set<String> publicPaths = new HashSet<>();
        for (Class<?> clazz : servletClasses) {
            WebServlet ws = clazz.getAnnotation(WebServlet.class);
            if (ws == null) continue;
            String[] urlPatterns = ws.value().length > 0 ? ws.value() : ws.urlPatterns();
            if (urlPatterns.length == 0) continue;
            // 取第一个 pattern 作为 basePath，去掉末尾的 "/*"
            String basePath = urlPatterns[0];
            if (basePath.endsWith("/*")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Public.class)) continue;
                for (String routePath : extractRoutePaths(method)) {
                    String fullPath = basePath + routePath;
                    // 去除重复斜杠
                    fullPath = fullPath.replace("//", "/");
                    publicPaths.add(fullPath);
                }
            }
        }
        return Collections.unmodifiableSet(publicPaths);
    }

    private static List<String> extractRoutePaths(Method method) {
        List<String> paths = new ArrayList<>();
        for (ShortcutAnnotation<?> shortcut : SHORTCUT_ANNOTATIONS) {
            for (Annotation ann : method.getAnnotationsByType(shortcut.annotationType())) {
                @SuppressWarnings("unchecked")
                Function<Annotation, String> extractor = (Function<Annotation, String>) shortcut.pathExtractor();
                paths.add(extractor.apply(ann));
            }
        }
        for (Route route : method.getAnnotationsByType(Route.class)) {
            paths.add(route.value());
        }
        return paths;
    }
}
