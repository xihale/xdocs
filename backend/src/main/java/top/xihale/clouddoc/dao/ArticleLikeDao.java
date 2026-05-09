package top.xihale.clouddoc.dao;

import top.xihale.clouddoc.util.SqlBuilder;

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
        return SqlBuilder.update("INSERT IGNORE INTO article_like(article_id, user_id) VALUES(?, ?)")
                .param(articleId).param(userId)
                .execute() > 0;
    }

    /**
     * 取消点赞（删除记录）
     *
     * @return true 表示删除成功，false 表示未点赞
     */
    public static boolean delete(int articleId, int userId) {
        return SqlBuilder.update("DELETE FROM article_like WHERE article_id = ? AND user_id = ?")
                .param(articleId).param(userId)
                .execute() > 0;
    }

    /**
     * 删除文章的所有点赞记录
     */
    public static int deleteByArticleId(int articleId) {
        return SqlBuilder.update("DELETE FROM article_like WHERE article_id = ?")
                .param(articleId)
                .execute();
    }

    /**
     * 查询用户是否已点赞
     */
    public static boolean exists(int articleId, int userId) {
        return SqlBuilder.select("SELECT 1 FROM article_like WHERE article_id = ? AND user_id = ? LIMIT 1")
                .param(articleId).param(userId)
                .queryExists();
    }

    /**
     * 统计文章点赞数
     */
    public static int countByArticle(int articleId) {
        return SqlBuilder.select("SELECT COUNT(*) FROM article_like WHERE article_id = ?")
                .param(articleId)
                .queryCount();
    }
}
