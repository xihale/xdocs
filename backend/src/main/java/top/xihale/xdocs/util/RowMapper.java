package top.xihale.xdocs.util;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 行映射器接口 — 将 {@link ResultSet} 当前行映射为 Java 对象
 * <p>
 * 配合 {@link SqlBuilder#queryList} 和 {@link SqlBuilder#queryOne} 使用。
 * <p>
 * 示例：
 * <pre>
 * RowMapper&lt;User&gt; mapper = rs -&gt; {
 *     User user = new User();
 *     user.setId(rs.getInt("id"));
 *     user.setUsername(rs.getString("username"));
 *     return user;
 * };
 * </pre>
 *
 * @param <R> 映射目标类型
 * @see SqlBuilder
 */
@FunctionalInterface
public interface RowMapper<R> {


    /**
     * 将 ResultSet 当前行映射为目标对象
     * <p>
     * 调用方负责移动 cursor（next），实现方只需读取当前行。
     *
     * @param rs ResultSet（已定位到目标行）
     * @return 映射后的对象
     * @throws SQLException 数据库异常
     */
    R mapRow(ResultSet rs) throws SQLException;

}
