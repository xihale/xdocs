package top.xihale.xdocs.util;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC 工具类，负责数据库连接的获取与关闭
 * <p>
 * 底层使用 {@link ConnectionPool} 连接池管理连接生命周期。
 * 连接由池统一管理，{@link #close} 归还连接而非真正关闭。
 */
public class JdbcUtils {

    private static final Logger LOGGER = Logger.getLogger(JdbcUtils.class.getName());

    /** 事务连接绑定到当前线程 */
    private static final ThreadLocal<Connection> TRANSACTION_CONN = new ThreadLocal<>();

    static {
        // 启动时初始化连接池
        ConnectionPool.init();
    }

    private JdbcUtils() {
    }

    /**
     * 获取数据库连接
     * <p>
     * 事务中返回绑定到当前线程的连接，否则从连接池获取。
     *
     * @return 数据库连接
     */
    public static Connection getConnection() {
        Connection conn = TRANSACTION_CONN.get();
        if (conn != null) return conn;
        return ConnectionPool.getInstance().getConnection();
    }

    /**
     * 绑定事务连接到当前线程
     */
    static void bindTransactionConnection(Connection conn) {
        TRANSACTION_CONN.set(conn);
    }

    /**
     * 解除当前线程的事务连接绑定
     */
    static void unbindTransactionConnection() {
        TRANSACTION_CONN.remove();
    }

    /**
     * 当前线程是否处于事务中
     */
    static boolean isInTransaction() {
        return TRANSACTION_CONN.get() != null;
    }

    /**
     * 从 ResultSet 中安全获取 LocalDateTime（处理 Timestamp null）
     *
     * @param rs          ResultSet
     * @param columnLabel 列名
     * @return LocalDateTime 或 null
     */
    public static LocalDateTime getLocalDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * 关闭数据库资源
     * <p>
     * 连接会被归还到连接池而非真正关闭。
     *
     * @param rs   ResultSet，可为 null
     * @param st   Statement，可为 null
     * @param conn Connection，可为 null
     */
    public static void close(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "关闭 ResultSet 失败", e);
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "关闭 Statement 失败", e);
            }
        }
        if (conn != null && !isInTransaction()) {
            try {
                conn.close(); // 代理对象：归还到连接池
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "归还 Connection 失败", e);
            }
        }
    }

    /**
     * 开启事务
     */
    static void beginTransaction(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
    }

    /**
     * 提交事务
     */
    static void commit(Connection conn) throws SQLException {
        conn.commit();
        conn.setAutoCommit(true);
    }

    /**
     * 回滚事务
     */
    static void rollback(Connection conn) {
        try {
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "回滚事务失败", e);
        }
    }
}
