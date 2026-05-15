package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.Db;

/**
 * 文章点赞数据访问层
 */
public class ArticleLikeDao {

    /**
     * 点赞（插入记录）
     *
     * @return true 表示插入成功，false 表示已点赞
     */
    public static boolean insert(int articleId, int userId) {
        return Db.sql("INSERT IGNORE INTO article_like(article_id, user_id) VALUES(:articleId, :userId)")
                .param("articleId", articleId).param("userId", userId)
                .execute() > 0;
    }

    /**
     * 取消点赞（删除记录）
     *
     * @return true 表示删除成功，false 表示未点赞
     */
    public static boolean delete(int articleId, int userId) {
        return Db.sql("DELETE FROM article_like WHERE article_id = :articleId AND user_id = :userId")
                .param("articleId", articleId).param("userId", userId)
                .execute() > 0;
    }

    /**
     * 删除文章的所有点赞记录
     */
    public static int deleteByArticleId(int articleId) {
        return Db.sql("DELETE FROM article_like WHERE article_id = :articleId")
                .param("articleId", articleId)
                .execute();
    }

    /**
     * 查询用户是否已点赞
     */
    public static boolean exists(int articleId, int userId) {
        return Db.sql("SELECT 1 FROM article_like WHERE article_id = :articleId AND user_id = :userId LIMIT 1")
                .param("articleId", articleId).param("userId", userId)
                .query(rs -> rs.getInt(1))
                .exists();
    }

    /**
     * 统计文章点赞数
     */
    public static int countByArticle(int articleId) {
        return Db.sql("SELECT COUNT(*) FROM article_like WHERE article_id = :articleId")
                .param("articleId", articleId)
                .query(rs -> rs.getInt(1))
                .count();
    }
}
