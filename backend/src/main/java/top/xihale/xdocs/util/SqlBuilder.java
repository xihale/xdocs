package top.xihale.xdocs.util;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL 构建器 — 封装 JDBC 样板代码
 * <p>
 * 支持链式调用构建 SQL，自动管理参数绑定、资源关闭和结果映射。
 * <p>
 * 用法示例：
 * <pre>
 * // 查询列表
 * List&lt;User&gt; users = SqlBuilder.select("SELECT * FROM user WHERE status = ?")
 *     .param(1)
 *     .queryList(rs -&gt; new User(rs.getInt("id"), rs.getString("username")));
 *
 * // 查询单条
 * Optional&lt;User&gt; user = SqlBuilder.select("SELECT * FROM user WHERE id = ?")
 *     .param(userId)
 *     .queryOne(rs -&gt; mapUser(rs));
 *
 * // 插入/更新/删除
 * int rows = SqlBuilder.update("INSERT INTO user(username, password) VALUES(?, ?)")
 *     .param("admin")
 *     .param("123456")
 *     .execute();
 *
 * // 插入并获取自增主键
 * int id = SqlBuilder.update("INSERT INTO article(title) VALUES(?)")
 *     .param("测试")
 *     .executeReturnKey();
 * </pre>
 *
 * @see RowMapper
 * @see JdbcUtils
 */
public class SqlBuilder {

    private static final Logger LOGGER = Logger.getLogger(SqlBuilder.class.getName());

    private final String sql;
    private final List<Object> params = new ArrayList<>();

    private SqlBuilder(String sql) {
        this.sql = sql;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建 SELECT 查询构建器
     *
     * @param sql SQL 查询语句
     * @return SqlBuilder 实例
     */
    public static SqlBuilder select(String sql) {
        return new SqlBuilder(sql);
    }

    /**
     * 创建 INSERT / UPDATE / DELETE 构建器
     *
     * @param sql SQL 更新语句
     * @return SqlBuilder 实例
     */
    public static SqlBuilder update(String sql) {
        return new SqlBuilder(sql);
    }

    // ==================== 参数绑定 ====================

    /**
     * 添加一个参数
     */
    public SqlBuilder param(Object value) {
        params.add(value);
        return this;
    }

    /**
     * 添加多个参数
     */
    public SqlBuilder params(Object... values) {
        Collections.addAll(params, values);
        return this;
    }

    /**
     * 添加参数集合
     */
    public SqlBuilder params(Collection<?> values) {
        params.addAll(values);
        return this;
    }

    // ==================== 查询操作 ====================

    /**
     * 查询返回列表
     *
     * @param mapper 行映射器
     * @return 结果列表
     */
    public <T> List<T> queryList(RowMapper<T> mapper) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            rs = ps.executeQuery();
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapper.mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 查询返回单条记录（Optional）
     *
     * @param mapper 行映射器
     * @return Optional 包装的结果
     */
    public <T> Optional<T> queryOne(RowMapper<T> mapper) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapper.mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 查询返回单个值（如 COUNT、SUM 等）
     *
     * @param <T> 返回值类型
     * @return 查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> T queryScalar() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            rs = ps.executeQuery();
            if (rs.next()) {
                return (T) rs.getObject(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 查询 COUNT 返回 int
     */
    public int queryCount() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 查询是否存在（SELECT 1 ... LIMIT 1）
     */
    public boolean queryExists() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    // ==================== 更新操作 ====================

    /**
     * 执行 INSERT / UPDATE / DELETE
     *
     * @return 影响行数
     */
    public int execute() {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            bindParams(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(null, ps, conn);
        }
    }

    /**
     * 执行 INSERT 并返回自增主键
     *
     * @return 自增主键值
     */
    public int executeReturnKey() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bindParams(ps);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new RuntimeException("未能获取自增主键");
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败", e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 批量执行（适用于批量 INSERT/UPDATE）
     *
     * @param batchSize 每批大小
     * @return 总影响行数
     */
    public int executeBatch(int batchSize) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(sql);
            int totalRows = 0;
            int paramCount = params.size();
            int totalItems = paramCount / batchSize;
            int paramIndex = 0;

            for (int i = 0; i < totalItems; i++) {
                for (int j = 0; j < batchSize; j++) {
                    ps.setObject(j + 1, params.get(paramIndex++));
                }
                ps.addBatch();
                if ((i + 1) % 100 == 0) {
                    int[] rows = ps.executeBatch();
                    for (int r : rows) totalRows += r;
                }
            }
            int[] remaining = ps.executeBatch();
            for (int r : remaining) totalRows += r;
            return totalRows;
        } catch (SQLException e) {
            throw new RuntimeException("批量执行失败", e);
        } finally {
            JdbcUtils.close(null, ps, conn);
        }
    }

    // ==================== 事务支持 ====================

    /**
     * 在事务中执行操作
     *
     * @param action 事务内操作
     * @param <T>    返回类型
     * @return 操作结果
     */
    public static <T> T inTransaction(TransactionAction<T> action) {
        Connection conn = null;
        try {
            conn = JdbcUtils.getConnection();
            JdbcUtils.beginTransaction(conn);
            JdbcUtils.bindTransactionConnection(conn);
            T result = action.execute(conn);
            JdbcUtils.commit(conn);
            return result;
        } catch (Exception e) {
            if (conn != null) JdbcUtils.rollback(conn);
            throw new RuntimeException("事务执行失败", e);
        } finally {
            JdbcUtils.unbindTransactionConnection();
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "关闭事务连接失败", e);
                }
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 绑定参数到 PreparedStatement
     */
    private void bindParams(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    // ==================== 内部接口 ====================

    /**
     * 事务操作函数式接口
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    public interface TransactionAction<T> {
        T execute(Connection conn) throws Exception;
    }
}
