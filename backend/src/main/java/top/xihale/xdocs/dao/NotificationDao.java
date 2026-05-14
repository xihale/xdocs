package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Notification;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * 通知数据访问层
 */
public class NotificationDao {

    private static final BaseMapper<Notification> MAPPER = new BaseMapper<>(Notification.class);

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT %s FROM notification WHERE user_id = :userId ORDER BY create_time DESC LIMIT :limit OFFSET :offset".formatted(MAPPER.columns());

    public static void insert(Notification notification) { MAPPER.insert(notification); }
    public static Optional<Notification> findById(Object id) { return MAPPER.findById(id); }

    public static List<Notification> findByUserId(Integer userId, int offset, int limit) {
        return Db.sql(SQL_FIND_BY_USER_ID)
                .param("userId", userId).param("limit", limit).param("offset", offset)
                .query(MAPPER.mapper())
                .list();
    }

    public static int countUnread(Integer userId) {
        return MAPPER.count("user_id = ? AND is_read = 0", userId);
    }

    public static int markRead(Integer id, Integer userId) {
        return Db.sql("UPDATE notification SET is_read = 1 WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId)
                .execute();
    }

    public static int markAllRead(Integer userId) {
        return Db.sql("UPDATE notification SET is_read = 1 WHERE user_id = :userId AND is_read = 0")
                .param("userId", userId)
                .execute();
    }

    public static int delete(Integer id, Integer userId) {
        return Db.sql("DELETE FROM notification WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId)
                .execute();
    }
}
