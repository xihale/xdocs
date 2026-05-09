package top.xihale.clouddoc.servlet.route;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记路由方法为公开接口（无需登录即可访问）
 * <p>
 * 添加此注解的路由会被 AuthFilter 自动加入白名单，无需手动维护路径列表。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {
}
