package top.xihale.xdocs.util;

import top.xihale.xdocs.annotation.Column;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于反射的简易 ORM 基类
 * <p>
 * 通过 {@link Table}、{@link Id}、{@link Column} 注解自动推导 SQL。
 * <p>
 * 用法：
 * <pre>
 * // PO 加注解
 * &#64;Table("article")
 * public class Article {
 *     &#64;Id private Integer id;
 *     private String title;
 * }
 *
 * // DAO 组合 BaseMapper，对外暴露静态方法
 * public class ArticleDao {
 *     private static final BaseMapper&lt;Article&gt; MAPPER = new BaseMapper&lt;&gt;(Article.class);
 *     public static void insert(Article a) { MAPPER.insert(a); }
 *     public static Optional&lt;Article&gt; findById(Object id) { return MAPPER.findById(id); }
 * }
 *
 * // 调用
 * ArticleDao.insert(article);
 * ArticleDao.findById(1);
 * </pre>
 *
 * @param <E> 实体类型
 */
public class BaseMapper<E> {

    private static final ConcurrentHashMap<Class<?>, EntityMeta> META_CACHE = new ConcurrentHashMap<>();

    private final EntityMeta meta;
    private final RowMapper<E> rowMapper;

    public BaseMapper(Class<E> clazz) {
        this.meta = META_CACHE.computeIfAbsent(clazz, EntityMeta::new);
        @SuppressWarnings("unchecked")
        RowMapper<E> m = rs -> mapFromResultSet(rs);
        this.rowMapper = m;
    }

    // ==================== SQL 模板 ====================

    private static final String SQL_FIND_BY_ID = "SELECT %s FROM %s WHERE %s = ?";
    private static final String SQL_FIND_ALL = "SELECT %s FROM %s";
    private static final String SQL_FIND_LIST = "SELECT %s FROM %s WHERE %s";
    private static final String SQL_COUNT = "SELECT COUNT(*) FROM %s WHERE %s";
    private static final String SQL_EXISTS = "SELECT 1 FROM %s WHERE %s LIMIT 1";
    private static final String SQL_DELETE_BY_ID = "DELETE FROM %s WHERE %s = ?";

    // ==================== CRUD ====================

    /** 插入实体并回填自增主键 */
    public void insert(E entity) {
        List<FieldMeta> columns = meta.nonIdColumns;
        int id = Db.sql(buildInsertSql(columns))
                .params(extractValues(entity, columns))
                .executeReturnKey();
        setId(entity, id);
    }

    /** 按主键更新全部非主键字段 */
    public int update(E entity) {
        List<FieldMeta> columns = meta.nonIdColumns;
        return Db.sql(buildUpdateSql(columns))
                .params(extractValues(entity, columns))
                .param(getIdValue(entity))
                .execute();
    }

    /** 按主键更新指定字段 */
    public int update(E entity, String... fieldNames) {
        List<FieldMeta> columns = filterFieldMetas(fieldNames);
        return Db.sql(buildUpdateSql(columns))
                .params(extractValues(entity, columns))
                .param(getIdValue(entity))
                .execute();
    }

    /** 按主键删除 */
    public int deleteById(Object id) {
        return Db.sql(SQL_DELETE_BY_ID.formatted(meta.table, meta.idColumn.columnName))
                .param(id)
                .execute();
    }

    /** 按主键查单条 */
    public Optional<E> findById(Object id) {
        return Db.sql(SQL_FIND_BY_ID.formatted(meta.allColumnsStr, meta.table, meta.idColumn.columnName))
                .param(id)
                .query(rowMapper)
                .one();
    }

    /** 查全表 */
    public List<E> findAll() {
        return Db.sql(SQL_FIND_ALL.formatted(meta.allColumnsStr, meta.table))
                .query(rowMapper)
                .list();
    }

    /** 条件查列表。例: {@code findList("status = ? AND author_id = ?", 1, userId)} */
    public List<E> findList(String whereClause, Object... params) {
        return Db.sql(SQL_FIND_LIST.formatted(meta.allColumnsStr, meta.table, whereClause))
                .params(params)
                .query(rowMapper)
                .list();
    }

    /** 条件查单条 */
    public Optional<E> findOne(String whereClause, Object... params) {
        return Db.sql(SQL_FIND_LIST.formatted(meta.allColumnsStr, meta.table, whereClause))
                .params(params)
                .query(rowMapper)
                .one();
    }

    /** 条件统计 */
    public int count(String whereClause, Object... params) {
        return Db.sql(SQL_COUNT.formatted(meta.table, whereClause))
                .params(params)
                .query(rowMapper)
                .count();
    }

    /** 条件判断存在 */
    public boolean exists(String whereClause, Object... params) {
        return Db.sql(SQL_EXISTS.formatted(meta.table, whereClause))
                .params(params)
                .query(rowMapper)
                .exists();
    }

    // ==================== 查询辅助 ====================

    /** 获取行映射器（用于自定义 Db 查询） */
    public RowMapper<E> mapper() {
        return rowMapper;
    }

    /** 获取表名 */
    public String table() {
        return meta.table;
    }

    /** 获取所有列名（逗号分隔，用于 JOIN 查询） */
    public String columns() {
        return meta.allColumnsStr;
    }

    // ==================== 反射映射 ====================

    @SuppressWarnings("unchecked")
    private E mapFromResultSet(ResultSet rs) throws SQLException {
        try {
            E instance = (E) meta.constructor.newInstance();
            for (FieldMeta fm : meta.allColumns) {
                fm.field.set(instance, readColumn(rs, fm));
            }
            return instance;
        } catch (Exception e) {
            throw new SQLException("反射映射失败: " + meta.entityClass.getName(), e);
        }
    }

    private Object readColumn(ResultSet rs, FieldMeta fm) throws SQLException {
        Class<?> type = fm.field.getType();
        if (type == LocalDateTime.class) return JdbcUtils.getLocalDateTime(rs, fm.columnName);
        if (type == Integer.class) return rs.getObject(fm.columnName, Integer.class);
        if (type == int.class) return rs.getInt(fm.columnName);
        if (type == Long.class) return rs.getObject(fm.columnName, Long.class);
        if (type == long.class) return rs.getLong(fm.columnName);
        if (type == String.class) return rs.getString(fm.columnName);
        return rs.getObject(fm.columnName);
    }

    // ==================== SQL 构建 ====================

    private String buildInsertSql(List<FieldMeta> columns) {
        String cols = columns.stream().map(fm -> fm.columnName).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(fm -> "?").collect(Collectors.joining(", "));
        return "INSERT INTO %s(%s) VALUES(%s)".formatted(meta.table, cols, placeholders);
    }

    private String buildUpdateSql(List<FieldMeta> columns) {
        String set = columns.stream().map(fm -> fm.columnName + " = ?").collect(Collectors.joining(", "));
        return "UPDATE %s SET %s WHERE %s = ?".formatted(meta.table, set, meta.idColumn.columnName);
    }

    // ==================== 字段值提取 ====================

    private Object[] extractValues(E entity, List<FieldMeta> columns) {
        Object[] values = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            try {
                values[i] = columns.get(i).field.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法读取字段: " + columns.get(i).field.getName(), e);
            }
        }
        return values;
    }

    private void setId(E entity, int id) {
        try {
            meta.idColumn.field.set(entity, id);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法设置主键", e);
        }
    }

    private Object getIdValue(E entity) {
        try {
            return meta.idColumn.field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法读取主键", e);
        }
    }

    private List<FieldMeta> filterFieldMetas(String... fieldNames) {
        Set<String> nameSet = new HashSet<>(Arrays.asList(fieldNames));
        return meta.allColumns.stream()
                .filter(fm -> nameSet.contains(fm.field.getName()))
                .collect(Collectors.toList());
    }

    // ==================== 元数据 ====================

    private static class EntityMeta {
        final Class<?> entityClass;
        final String table;
        final FieldMeta idColumn;
        final List<FieldMeta> allColumns;
        final List<FieldMeta> nonIdColumns;
        final String allColumnsStr;
        final java.lang.reflect.Constructor<?> constructor;

        EntityMeta(Class<?> clazz) {
            this.entityClass = clazz;
            Table tableAnn = clazz.getAnnotation(Table.class);
            if (tableAnn == null) throw new IllegalArgumentException(clazz.getName() + " 缺少 @Table 注解");
            this.table = tableAnn.value();

            List<FieldMeta> cols = new ArrayList<>();
            FieldMeta idField = null;
            for (Field f : getAllFields(clazz)) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                if (f.isAnnotationPresent(top.xihale.xdocs.annotation.Transient.class)) continue;
                f.setAccessible(true);
                FieldMeta fm = new FieldMeta(f);
                cols.add(fm);
                if (f.isAnnotationPresent(Id.class)) idField = fm;
            }
            if (idField == null) throw new IllegalArgumentException(clazz.getName() + " 缺少 @Id 注解字段");
            this.idColumn = idField;
            this.allColumns = Collections.unmodifiableList(cols);
            final FieldMeta idRef = idField;
            this.nonIdColumns = cols.stream().filter(fm -> fm != idRef).collect(Collectors.toUnmodifiableList());
            this.allColumnsStr = cols.stream().map(fm -> fm.columnName).collect(Collectors.joining(", "));

            try {
                this.constructor = clazz.getDeclaredConstructor();
                this.constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(clazz.getName() + " 需要无参构造器", e);
            }
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

    private static class FieldMeta {
        final Field field;
        final String columnName;

        FieldMeta(Field field) {
            this.field = field;
            Column colAnn = field.getAnnotation(Column.class);
            this.columnName = colAnn != null ? colAnn.value() : camelToSnake(field.getName());
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
