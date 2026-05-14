package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.Db;

import java.util.List;

/**
 * 浏览记录数据访问层
 */
public class RecentVisitDao {

    /**
     * 记录浏览（INSERT ON DUPLICATE KEY UPDATE 刷新时间）
     */
    public static void upsert(int userId, int articleId) {
        Db.sql("INSERT INTO recent_visit(user_id, article_id) VALUES(:userId, :articleId) ON DUPLICATE KEY UPDATE visit_time = CURRENT_TIMESTAMP")
                .param("userId", userId).param("articleId", articleId)
                .execute();
    }

    /**
     * 获取用户最近浏览的文章ID（按时间倒序）
     */
    public static List<Integer> findRecentArticleIds(int userId, int limit) {
        return Db.sql("SELECT article_id FROM recent_visit WHERE user_id = :userId ORDER BY visit_time DESC LIMIT :limit")
                .param("userId", userId).param("limit", limit)
                .query(rs -> rs.getInt(1))
                .list();
    }

    /**
     * 删除单条浏览记录
     */
    public static void delete(int userId, int articleId) {
        Db.sql("DELETE FROM recent_visit WHERE user_id = :userId AND article_id = :articleId")
                .param("userId", userId).param("articleId", articleId)
                .execute();
    }

    /**
     * 删除指定文章的所有浏览记录
     */
    public static int deleteByArticleId(int articleId) {
        return Db.sql("DELETE FROM recent_visit WHERE article_id = :articleId")
                .param("articleId", articleId)
                .execute();
    }

    /**
     * 清空用户所有浏览记录
     */
    public static void deleteAll(int userId) {
        Db.sql("DELETE FROM recent_visit WHERE user_id = :userId")
                .param("userId", userId)
                .execute();
    }
}
