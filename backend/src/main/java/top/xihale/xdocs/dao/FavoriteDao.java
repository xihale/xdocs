package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.Db;

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
        return Db.sql("INSERT IGNORE INTO favorite(user_id, target_type, target_id) VALUES(:userId, :targetType, :targetId)")
                .param("userId", userId).param("targetType", targetType).param("targetId", targetId)
                .execute() > 0;
    }

    public static boolean delete(int userId, int targetType, int targetId) {
        return Db.sql("DELETE FROM favorite WHERE user_id = :userId AND target_type = :targetType AND target_id = :targetId")
                .param("userId", userId).param("targetType", targetType).param("targetId", targetId)
                .execute() > 0;
    }

    /**
     * 删除指定文章的所有收藏记录
     */
    public static int deleteByTargetId(int targetType, int targetId) {
        return Db.sql("DELETE FROM favorite WHERE target_type = :targetType AND target_id = :targetId")
                .param("targetType", targetType).param("targetId", targetId)
                .execute();
    }

    public static boolean exists(int userId, int targetType, int targetId) {
        return Db.sql("SELECT 1 FROM favorite WHERE user_id = :userId AND target_type = :targetType AND target_id = :targetId LIMIT 1")
                .param("userId", userId).param("targetType", targetType).param("targetId", targetId)
                .query(rs -> rs.getInt(1))
                .exists();
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
        return Db.sql("SELECT target_id FROM favorite WHERE user_id = :userId AND target_type = :targetType ORDER BY create_time DESC")
                .param("userId", userId).param("targetType", targetType)
                .query(rs -> rs.getInt(1))
                .list();
    }
}
