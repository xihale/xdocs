package top.xihale.xdocs.servlet.route;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.util.ResponseUtils;
import top.xihale.xdocs.util.Result;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 路由处理器，封装反射调用目标方法
 */
public final class RouteHandler {

    private final Method method;

    private RouteHandler(Method method) {
        this.method = method;
    }

    public static RouteHandler create(Method method) throws ServletException {
        validate(method);
        method.setAccessible(true);
        return new RouteHandler(method);
    }

    public void invoke(Object target, RouteKey routeKey, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Result<?> result;
        try {
            result = (Result<?>) method.invoke(target, req, resp);
        } catch (IllegalAccessException e) {
            throw new ServletException(STR."无法访问路由处理方法: \{describe()} [\{routeKey.displayName()}]", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof ServletException servletException) {
                throw servletException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ServletException(STR."执行路由处理方法失败: \{describe()} [\{routeKey.displayName()}]", cause);
        }
        ResponseUtils.writeResult(resp, result);
    }

    public String describe() {
        return STR."\{method.getDeclaringClass().getName()}.\{method.getName()}";
    }

    private static void validate(Method method) throws ServletException {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new ServletException(STR."路由处理方法不能是 static: \{describe(method)}");
        }
        if (!Result.class.isAssignableFrom(method.getReturnType())) {
            throw new ServletException(STR."路由处理方法必须返回 Result: \{describe(method)}");
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2
                || parameterTypes[0] != HttpServletRequest.class
                || parameterTypes[1] != HttpServletResponse.class) {
            throw new ServletException(STR."路由处理方法签名必须为 (HttpServletRequest, HttpServletResponse): \{describe(method)}");
        }
    }

    private static String describe(Method method) {
        return STR."\{method.getDeclaringClass().getName()}.\{method.getName()}";
    }
}
