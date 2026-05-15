package top.xihale.xdocs.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 SQL 执行器 — 具名参数 + 自动 Bean 映射
 * <p>
 * 用法示例：
 * <pre>
 * // 具名参数查询 → 自动映射 Bean
 * List&lt;User&gt; users = Db.sql("SELECT * FROM user WHERE status = :status")
 *     .param("status", 1)
 *     .query(User.class)
 *     .list();
 *
 * // 查询单条
 * Optional&lt;User&gt; user = Db.sql("SELECT * FROM user WHERE id = :id")
 *     .param("id", userId)
 *     .query(User.class)
 *     .one();
 *
 * // 更新
 * int rows = Db.sql("UPDATE user SET nickname = :nickname WHERE id = :id")
 *     .param("nickname", "test")
 *     .param("id", 1)
 *     .execute();
 *
 * // 事务
 * Db.inTransaction(() -&gt; {
 *     Db.sql("INSERT INTO ...").param(...).execute();
 *     Db.sql("UPDATE ...").param(...).execute();
 * });
 * </pre>
 *
 * @see JdbcUtils
 */
public class Db {

    private static final Logger LOGGER = Logger.getLogger(Db.class.getName());

    // 匹配 :paramName（排除 :: 这样的转义）
    private static final Pattern NAMED_PARAM = Pattern.compile("(?<!:):(\\w+)");
    private static final Pattern POSITIONAL_PARAM = Pattern.compile("\\?");

    private final String sql;
    private final Map<String, Object> namedParams = new LinkedHashMap<>();
    private final List<Object> indexedParams = new ArrayList<>();

    private Db(String sql) {
        this.sql = sql;
    }

    // ==================== 入口 ====================

    /**
     * 创建 SQL 执行器
     *
     * @param sql SQL 语句，支持 :name 具名参数和 ? 占位符
     * @return Db 实例
     */
    public static Db sql(String sql) {
        return new Db(sql);
    }

    // ==================== 参数绑定 ====================

    /**
     * 绑定具名参数
     *
     * @param name  参数名（对应 SQL 中的 :name）
     * @param value 参数值
     * @return this
     */
    public Db param(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    /**
     * 绑定位置参数（? 占位符）
     *
     * @param value 参数值
     * @return this
     */
    public Db param(Object value) {
        indexedParams.add(value);
        return this;
    }

    /**
     * 批量绑定位置参数
     *
     * @param values 参数值
     * @return this
     */
    public Db params(Object... values) {
        Collections.addAll(indexedParams, values);
        return this;
    }

    /**
     * 批量绑定位置参数（集合）
     *
     * @param values 参数值集合
     * @return this
     */
    public Db params(Collection<?> values) {
        indexedParams.addAll(values);
        return this;
    }

    // ==================== 查询 ====================

    /**
     * 创建查询上下文，指定自动映射的目标类型
     *
     * @param clazz 目标 Bean 类型
     * @param <T>   类型
     * @return 查询上下文
     */
    public <T> Query<T> query(Class<T> clazz) {
        return new Query<>(this, BeanMapper.of(clazz));
    }

    /**
     * 创建查询上下文，使用自定义行映射器
     *
     * @param mapper 行映射器
     * @param <T>    类型
     * @return 查询上下文
     */
    public <T> Query<T> query(RowMapper<T> mapper) {
        return new Query<>(this, mapper);
    }

    // ==================== 更新 ====================

    /**
     * 执行 INSERT / UPDATE / DELETE
     *
     * @return 影响行数
     */
    public int execute() {
        PreparedResult pr = prepare();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(pr.sql);
            bindParams(ps, pr.params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败: " + sql, e);
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
        PreparedResult pr = prepare();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(pr.sql, Statement.RETURN_GENERATED_KEYS);
            bindParams(ps, pr.params);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new RuntimeException("未能获取自增主键");
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败: " + sql, e);
        } finally {
            JdbcUtils.close(rs, ps, conn);
        }
    }

    /**
     * 批量执行
     *
     * @param batchSize 每批参数个数
     * @return 总影响行数
     */
    public int executeBatch(int batchSize) {
        PreparedResult pr = prepare();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JdbcUtils.getConnection();
            ps = conn.prepareStatement(pr.sql);
            int totalRows = 0;
            int totalItems = pr.params.size() / batchSize;
            int paramIndex = 0;
            for (int i = 0; i < totalItems; i++) {
                for (int j = 0; j < batchSize; j++) {
                    ps.setObject(j + 1, pr.params.get(paramIndex++));
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
            throw new RuntimeException("批量执行失败: " + sql, e);
        } finally {
            JdbcUtils.close(null, ps, conn);
        }
    }

    // ==================== 事务 ====================

    /**
     * 在事务中执行操作（无返回值）
     *
     * @param action 事务内操作
     */
    public static void inTransaction(Runnable action) {
        inTransaction((TransactionAction<Void>) conn -> {
            action.run();
            return null;
        });
    }

    /**
     * 在事务中执行操作（有返回值）
     *
     * @param action 事务内操作
     * @param <R>    返回类型
     * @return 操作结果
     */
    public static <R> R inTransaction(TransactionAction<R> action) {
        Connection conn = null;
        try {
            conn = JdbcUtils.getConnection();
            JdbcUtils.beginTransaction(conn);
            JdbcUtils.bindTransactionConnection(conn);
            R result = action.execute(conn);
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

    // ==================== 内部方法 ====================

    /**
     * 将具名参数 SQL 转换为 ? 占位符 SQL + 有序参数列表
     */
    PreparedResult prepare() {
        // 检测是否有具名参数
        Matcher m = NAMED_PARAM.matcher(sql);
        if (!m.find()) {
            // 纯 ? 占位符模式
            return new PreparedResult(sql, new ArrayList<>(indexedParams));
        }

        // 具名参数模式：替换 :name → ?，按出现顺序收集值
        List<Object> orderedParams = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        m.reset();
        while (m.find()) {
            String name = m.group(1);
            Object value = namedParams.get(name);
            if (!namedParams.containsKey(name)) {
                throw new RuntimeException("缺少具名参数: " + name + " (SQL: " + sql + ")");
            }
            orderedParams.add(value);
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return new PreparedResult(sb.toString(), orderedParams);
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    // ==================== 内部类 ====================

    static class PreparedResult {
        final String sql;
        final List<Object> params;

        PreparedResult(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    /**
     * 事务操作函数式接口
     *
     * @param <R> 返回类型
     */
    @FunctionalInterface
    public interface TransactionAction<R> {
        R execute(Connection conn) throws Exception;
    }

    // ==================== 查询上下文 ====================

    /**
     * 查询上下文 — 链式调用 query().list() / .one() / .count() 等
     *
     * @param <T> 映射目标类型
     */
    public static class Query<T> {
        private final Db db;
        private final RowMapper<T> mapper;

        Query(Db db, RowMapper<T> mapper) {
            this.db = db;
            this.mapper = mapper;
        }

        /**
         * 查询返回列表
         */
        public List<T> list() {
            PreparedResult pr = db.prepare();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = JdbcUtils.getConnection();
                ps = conn.prepareStatement(pr.sql);
                db.bindParams(ps, pr.params);
                rs = ps.executeQuery();
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.mapRow(rs));
                }
                return result;
            } catch (SQLException e) {
                throw new RuntimeException("SQL 执行失败: " + db.sql, e);
            } finally {
                JdbcUtils.close(rs, ps, conn);
            }
        }

        /**
         * 查询返回单条（Optional）
         */
        public Optional<T> one() {
            PreparedResult pr = db.prepare();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = JdbcUtils.getConnection();
                ps = conn.prepareStatement(pr.sql);
                db.bindParams(ps, pr.params);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapper.mapRow(rs));
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new RuntimeException("SQL 执行失败: " + db.sql, e);
            } finally {
                JdbcUtils.close(rs, ps, conn);
            }
        }

        /**
         * 查询 COUNT 返回 int
         */
        public int count() {
            PreparedResult pr = db.prepare();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = JdbcUtils.getConnection();
                ps = conn.prepareStatement(pr.sql);
                db.bindParams(ps, pr.params);
                rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) {
                throw new RuntimeException("SQL 执行失败: " + db.sql, e);
            } finally {
                JdbcUtils.close(rs, ps, conn);
            }
        }

        /**
         * 查询是否存在
         */
        public boolean exists() {
            PreparedResult pr = db.prepare();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = JdbcUtils.getConnection();
                ps = conn.prepareStatement(pr.sql);
                db.bindParams(ps, pr.params);
                rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException("SQL 执行失败: " + db.sql, e);
            } finally {
                JdbcUtils.close(rs, ps, conn);
            }
        }

        /**
         * 查询标量值
         */
        @SuppressWarnings("unchecked")
        public <V> V scalar() {
            PreparedResult pr = db.prepare();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                conn = JdbcUtils.getConnection();
                ps = conn.prepareStatement(pr.sql);
                db.bindParams(ps, pr.params);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return (V) rs.getObject(1);
                }
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("SQL 执行失败: " + db.sql, e);
            } finally {
                JdbcUtils.close(rs, ps, conn);
            }
        }
    }

    // ==================== Bean 映射器 ====================

    /**
     * 自动 ResultSet → Bean 映射器（基于反射，带缓存）
     * <p>
     * 按列名匹配字段（snake_case 自动转 camelCase），支持 @Column 注解。
     */
    static class BeanMapper<T> implements RowMapper<T> {

        private static final ConcurrentHashMap<Class<?>, BeanMeta<?>> CACHE = new ConcurrentHashMap<>();

        static final Set<Class<?>> SCALAR_TYPES = Set.of(
            Integer.class, int.class, Long.class, long.class,
            Double.class, double.class, Float.class, float.class,
            Short.class, short.class, Byte.class, byte.class,
            Boolean.class, boolean.class, String.class
        );

        private final BeanMeta<T> meta;

        @SuppressWarnings("unchecked")
        private BeanMapper(Class<T> clazz) {
            this.meta = (BeanMeta<T>) CACHE.computeIfAbsent(clazz, BeanMeta::new);
        }

        static <T> BeanMapper<T> of(Class<T> clazz) {
            return new BeanMapper<>(clazz);
        }

        @Override
        public T mapRow(ResultSet rs) throws SQLException {
            try {
                if (SCALAR_TYPES.contains(meta.clazz)) {
                    return readScalar(rs);
                }
                T instance = meta.constructor.newInstance();
                for (ColMeta cm : meta.columns) {
                    Object value = readColumn(rs, cm);
                    cm.field.set(instance, value);
                }
                return instance;
            } catch (Exception e) {
                throw new SQLException("Bean 映射失败: " + meta.clazz.getName(), e);
            }
        }

        private Object readColumn(ResultSet rs, ColMeta cm) throws SQLException {
            Class<?> type = cm.field.getType();
            if (type == LocalDateTime.class) return JdbcUtils.getLocalDateTime(rs, cm.columnLabel);
            if (type == Integer.class) return rs.getObject(cm.columnLabel, Integer.class);
            if (type == int.class) return rs.getInt(cm.columnLabel);
            if (type == Long.class) return rs.getObject(cm.columnLabel, Long.class);
            if (type == long.class) return rs.getLong(cm.columnLabel);
            if (type == String.class) return rs.getString(cm.columnLabel);
            if (type == Boolean.class) return rs.getObject(cm.columnLabel, Boolean.class);
            if (type == boolean.class) return rs.getBoolean(cm.columnLabel);
            return rs.getObject(cm.columnLabel);
        }

        @SuppressWarnings("unchecked")
        private T readScalar(ResultSet rs) throws SQLException {
            Class<?> clazz = meta.clazz;
            if (clazz == Integer.class || clazz == int.class) return (T) Integer.valueOf(rs.getInt(1));
            if (clazz == Long.class || clazz == long.class) return (T) Long.valueOf(rs.getLong(1));
            if (clazz == String.class) return (T) rs.getString(1);
            if (clazz == Double.class || clazz == double.class) return (T) Double.valueOf(rs.getDouble(1));
            if (clazz == Float.class || clazz == float.class) return (T) Float.valueOf(rs.getFloat(1));
            if (clazz == Boolean.class || clazz == boolean.class) return (T) Boolean.valueOf(rs.getBoolean(1));
            if (clazz == Short.class || clazz == short.class) return (T) Short.valueOf(rs.getShort(1));
            if (clazz == Byte.class || clazz == byte.class) return (T) Byte.valueOf(rs.getByte(1));
            return (T) rs.getObject(1);
        }
    }

    static class BeanMeta<T> {
        final Class<T> clazz;
        final Constructor<T> constructor;
        final List<ColMeta> columns;

        @SuppressWarnings("unchecked")
        BeanMeta(Class<?> clazz) {
            this.clazz = (Class<T>) clazz;
            if (BeanMapper.SCALAR_TYPES.contains(clazz)) {
                this.constructor = null;
                this.columns = List.of();
                return;
            }
            try {
                this.constructor = (Constructor<T>) clazz.getDeclaredConstructor();
                this.constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(clazz.getName() + " 需要无参构造器", e);
            }

            List<ColMeta> cols = new ArrayList<>();
            for (Field f : getAllFields(clazz)) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                if (f.isAnnotationPresent(top.xihale.xdocs.annotation.Transient.class)) continue;
                f.setAccessible(true);
                cols.add(new ColMeta(f));
            }
            this.columns = Collections.unmodifiableList(cols);
        }

        private static List<Field> getAllFields(Class<?> clazz) {
            List<Field> fields = new ArrayList<>();
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                fields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }
    }

    static class ColMeta {
        final Field field;
        final String columnLabel;

        ColMeta(Field field) {
            this.field = field;
            top.xihale.xdocs.annotation.Column colAnn =
                    field.getAnnotation(top.xihale.xdocs.annotation.Column.class);
            if (colAnn != null) {
                this.columnLabel = colAnn.value();
            } else {
                this.columnLabel = camelToSnake(field.getName());
            }
        }

        private static String camelToSnake(String name) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > 0) sb.append('_');
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
