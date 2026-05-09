package top.xihale.clouddoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段对应的数据库列名
 * <p>
 * 未标注的字段自动按 camelCase → snake_case 推断列名。
 * 标注后以注解值为准。
 * <p>
 * 用法：
 * <pre>
 * &#64;Column("avatar_url")
 * private String avatarUrl;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * 数据库列名
     */
    String value();
}
