package top.xihale.xdocs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段不参与 ORM 映射（不查询、不插入、不更新）
 * <p>
 * 用法：
 * <pre>
 * &#64;Transient
 * private String computedField;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {
}
