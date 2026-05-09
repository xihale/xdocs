package top.xihale.xdocs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体类对应的数据库表名
 * <p>
 * 用法：
 * <pre>
 * &#64;Table("sys_user")
 * public class User { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    /**
     * 数据库表名
     */
    String value();
}
