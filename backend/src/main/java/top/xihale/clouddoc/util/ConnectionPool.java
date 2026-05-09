package top.xihale.clouddoc.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 自定义数据库连接池
 * <p>
 * 功能特性：
 * <ul>
 *   <li>并发安全：基于 BlockingQueue 实现，细粒度锁</li>
 *   <li>动态扩缩容：按需创建连接，最大连接数可配</li>
 *   <li>等待超时：连接耗尽时阻塞等待，超时抛异常</li>
 *   <li>连接校验：取出连接时验证可用性，失效自动重建</li>
 *   <li>优雅关闭：关闭池时释放所有连接</li>
 * </ul>
 */
public class ConnectionPool {

    private static final Logger LOGGER = Logger.getLogger(ConnectionPool.class.getName());

    /** 空闲连接队列 */
    private final BlockingQueue<Connection> idleConnections;

    /** 当前活跃（已借出）连接数 */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    /** 连接池配置 */
    private final PoolConfig config;

    /** 数据库连接参数 */
    private final String url;
    private final String username;
    private final String password;

    /** 池是否已关闭 */
    private volatile boolean closed = false;

    /** 单例实例 */
    private static volatile ConnectionPool instance;

    /**
     * 连接池配置类
     */
    public static class PoolConfig {
        /** 最小空闲连接数 */
        public int minIdle = 2;
        /** 最大连接数 */
        public int maxTotal = 20;
        /** 获取连接超时时间（毫秒），0 表示无限等待 */
        public long maxWaitMillis = 5000;
        /** 连接最大存活时间（毫秒），0 表示不限制 */
        public long maxLifetimeMillis = 30 * 60 * 1000L;
        /** 连接校验超时（秒） */
        public int validationTimeoutSeconds = 2;

        public PoolConfig() {}
    }

    /**
     * 私有构造，通过 {@link #init} 或 {@link #getInstance} 获取实例
     */
    private ConnectionPool(String url, String username, String password, PoolConfig config) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.config = config;
        this.idleConnections = new ArrayBlockingQueue<>(config.maxTotal);

        // 初始化最小空闲连接
        for (int i = 0; i < config.minIdle; i++) {
            try {
                idleConnections.offer(createRawConnection());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "初始化连接池时创建连接失败", e);
            }
        }

        LOGGER.info("连接池初始化完成: minIdle=" + config.minIdle + ", maxTotal=" + config.maxTotal);
    }

    /**
     * 从 db.properties 初始化连接池（单例）
     */
    public static synchronized void init() {
        if (instance != null && !instance.closed) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = ConnectionPool.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("未找到 db.properties");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("读取数据库配置文件失败", e);
        }

        String driver = props.getProperty("jdbc.driver");
        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("数据库驱动加载失败", e);
        }

        PoolConfig config = new PoolConfig();
        // 可通过配置文件覆盖
        config.minIdle = Integer.parseInt(props.getProperty("pool.minIdle", "2"));
        config.maxTotal = Integer.parseInt(props.getProperty("pool.maxTotal", "20"));
        config.maxWaitMillis = Long.parseLong(props.getProperty("pool.maxWaitMillis", "5000"));

        instance = new ConnectionPool(url, username, password, config);
    }

    /**
     * 获取连接池单例
     */
    public static ConnectionPool getInstance() {
        if (instance == null || instance.closed) {
            synchronized (ConnectionPool.class) {
                if (instance == null || instance.closed) {
                    init();
                }
            }
        }
        return instance;
    }

    /**
     * 获取一个可用连接
     * <p>
     * 优先从空闲队列取，队列为空且未达上限时新建，已达上限则阻塞等待。
     *
     * @return 数据库连接（代理对象）
     * @throws RuntimeException 获取超时或池已关闭
     */
    public Connection getConnection() {
        if (closed) {
            throw new RuntimeException("连接池已关闭");
        }

        // 1. 尝试从空闲队列取
        Connection conn = idleConnections.poll();
        if (conn != null) {
            return wrapConnection(conn);
        }

        // 2. 未达上限时新建
        while (true) {
            int current = activeCount.get();
            if (current < config.maxTotal) {
                if (activeCount.compareAndSet(current, current + 1)) {
                    try {
                        Connection raw = createRawConnection();
                        return wrapConnection(raw);
                    } catch (SQLException e) {
                        activeCount.decrementAndGet();
                        throw new RuntimeException("创建数据库连接失败", e);
                    }
                }
            } else {
                break;
            }
        }

        // 3. 已达上限，阻塞等待
        try {
            if (config.maxWaitMillis > 0) {
                conn = idleConnections.poll(config.maxWaitMillis, TimeUnit.MILLISECONDS);
            } else {
                conn = idleConnections.take();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取连接被中断", e);
        }

        if (conn == null) {
            throw new RuntimeException("获取连接超时（等待 " + config.maxWaitMillis + "ms）");
        }
        return wrapConnection(conn);
    }

    /**
     * 归还连接到池中
     */
    public void returnConnection(Connection conn) {
        if (conn == null || closed) {
            safeClose(conn);
            activeCount.decrementAndGet();
            return;
        }

        try {
            // 如果连接已关闭或不可用，直接丢弃
            if (conn.isClosed() || !conn.isValid(config.validationTimeoutSeconds)) {
                activeCount.decrementAndGet();
                return;
            }
        } catch (SQLException e) {
            activeCount.decrementAndGet();
            return;
        }

        if (!idleConnections.offer(conn)) {
            // 队列满（不应该发生），直接关闭
            safeClose(conn);
            activeCount.decrementAndGet();
        }
    }

    /**
     * 关闭连接池，释放所有资源
     */
    public void shutdown() {
        closed = true;
        Connection conn;
        while ((conn = idleConnections.poll()) != null) {
            safeClose(conn);
        }
        LOGGER.info("连接池已关闭");
    }

    /**
     * 获取池状态信息
     */
    public String getStatus() {
        return String.format("连接池状态: 活跃=%d, 空闲=%d, 最大=%d",
                activeCount.get(), idleConnections.size(), config.maxTotal);
    }

    /**
     * 创建原始数据库连接
     */
    private Connection createRawConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 包装连接为代理对象，close() 时归还到池中
     */
    private Connection wrapConnection(Connection rawConn) {
        // 检查连接可用性
        try {
            if (rawConn.isClosed() || !rawConn.isValid(config.validationTimeoutSeconds)) {
                // 连接失效，重新创建
                safeClose(rawConn);
                rawConn = createRawConnection();
            }
        } catch (SQLException e) {
            safeClose(rawConn);
            try {
                rawConn = createRawConnection();
            } catch (SQLException ex) {
                activeCount.decrementAndGet();
                throw new RuntimeException("重建连接失败", ex);
            }
        }

        final Connection raw = rawConn;

        // 使用动态代理包装，拦截 close() 调用
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        // close() 不真正关闭，而是归还到池
                        returnConnection(raw);
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        // 检查代理是否关闭（始终返回 false，因为归还后可复用）
                        return false;
                    }
                    try {
                        return method.invoke(raw, args);
                    } catch (Exception e) {
                        throw e;
                    }
                }
        );
    }

    /**
     * 安全关闭连接
     */
    private void safeClose(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, "关闭连接时出错", e);
            }
        }
    }
}
