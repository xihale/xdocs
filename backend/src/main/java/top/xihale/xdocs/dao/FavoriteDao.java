package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * 收藏数据访问层
 */
public class FavoriteDao {

    /** 收藏类型：文章 */
    public static final int TYPE_ARTICLE = 0;
    /** 收藏类型：知识库 */
    public static final int TYPE_KB = 1;

    public static boolean insert(int userId, int targetType, int targetId) {
        return SqlBuilder.update("INSERT IGNORE INTO favorite(user_id, target_type, target_id) VALUES(?, ?, ?)")
                .param(userId).param(targetType).param(targetId)
                .execute() > 0;
    }

    public static boolean delete(int userId, int targetType, int targetId) {
        return SqlBuilder.update("DELETE FROM favorite WHERE user_id = ? AND target_type = ? AND target_id = ?")
                .param(userId).param(targetType).param(targetId)
                .execute() > 0;
    }

    /**
     * 删除指定文章的所有收藏记录
     */
    public static int deleteByTargetId(int targetType, int targetId) {
        return SqlBuilder.update("DELETE FROM favorite WHERE target_type = ? AND target_id = ?")
                .param(targetType).param(targetId)
                .execute();
    }

    public static boolean exists(int userId, int targetType, int targetId) {
        return SqlBuilder.select("SELECT 1 FROM favorite WHERE user_id = ? AND target_type = ? AND target_id = ? LIMIT 1")
                .param(userId).param(targetType).param(targetId)
                .queryExists();
    }

    /** 获取用户收藏的文章ID列表 */
    public static List<Integer> findArticleIds(int userId) {
        return findTargetIds(userId, TYPE_ARTICLE);
    }

    /** 获取用户收藏的知识库ID列表 */
    public static List<Integer> findKbIds(int userId) {
        return findTargetIds(userId, TYPE_KB);
    }

    private static List<Integer> findTargetIds(int userId, int targetType) {
        return SqlBuilder.select("SELECT target_id FROM favorite WHERE user_id = ? AND target_type = ? ORDER BY create_time DESC")
                .param(userId).param(targetType)
                .queryList(rs -> rs.getInt(1));
    }
}
