package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * 浏览记录数据访问层
 */
public class RecentVisitDao {

    /**
     * 记录浏览（INSERT ON DUPLICATE KEY UPDATE 刷新时间）
     */
    public static void upsert(int userId, int articleId) {
        SqlBuilder.update("INSERT INTO recent_visit(user_id, article_id) VALUES(?, ?) ON DUPLICATE KEY UPDATE visit_time = CURRENT_TIMESTAMP")
                .param(userId).param(articleId)
                .execute();
    }

    /**
     * 获取用户最近浏览的文章ID（按时间倒序）
     */
    public static List<Integer> findRecentArticleIds(int userId, int limit) {
        return SqlBuilder.select("SELECT article_id FROM recent_visit WHERE user_id = ? ORDER BY visit_time DESC LIMIT ?")
                .param(userId).param(limit)
                .queryList(rs -> rs.getInt(1));
    }

    /**
     * 删除单条浏览记录
     */
    public static void delete(int userId, int articleId) {
        SqlBuilder.update("DELETE FROM recent_visit WHERE user_id = ? AND article_id = ?")
                .param(userId).param(articleId)
                .execute();
    }

    /**
     * 删除指定文章的所有浏览记录
     */
    public static int deleteByArticleId(int articleId) {
        return SqlBuilder.update("DELETE FROM recent_visit WHERE article_id = ?")
                .param(articleId)
                .execute();
    }

    /**
     * 清空用户所有浏览记录
     */
    public static void deleteAll(int userId) {
        SqlBuilder.update("DELETE FROM recent_visit WHERE user_id = ?")
                .param(userId)
                .execute();
    }
}
