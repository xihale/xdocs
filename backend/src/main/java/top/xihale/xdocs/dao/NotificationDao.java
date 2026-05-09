package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Notification;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * 通知数据访问层
 */
public class NotificationDao extends BaseMapper<Notification> {

    public static final NotificationDao INSTANCE = new NotificationDao();

    public List<Notification> findByUserId(Integer userId, int offset, int limit) {
        return SqlBuilder.select("SELECT " + columns() + " FROM notification WHERE user_id=? ORDER BY create_time DESC LIMIT ? OFFSET ?")
                .param(userId).param(limit).param(offset)
                .queryList(mapper());
    }

    public int countUnread(Integer userId) {
        return count("user_id=? AND is_read=0", userId);
    }

    public int markRead(Integer id, Integer userId) {
        return SqlBuilder.update("UPDATE notification SET is_read=1 WHERE id=? AND user_id=?")
                .param(id).param(userId)
                .execute();
    }

    public int markAllRead(Integer userId) {
        return SqlBuilder.update("UPDATE notification SET is_read=1 WHERE user_id=? AND is_read=0")
                .param(userId)
                .execute();
    }

    public int delete(Integer id, Integer userId) {
        return SqlBuilder.update("DELETE FROM notification WHERE id=? AND user_id=?")
                .param(id).param(userId)
                .execute();
    }
}
