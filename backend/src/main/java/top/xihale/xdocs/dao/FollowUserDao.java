package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * 关注数据访问层
 */
public class FollowUserDao {

    public static boolean insert(int followerId, int followingId) {
        return SqlBuilder.update("INSERT IGNORE INTO follow_user(follower_id, following_id) VALUES(?, ?)")
                .param(followerId).param(followingId)
                .execute() > 0;
    }

    public static boolean delete(int followerId, int followingId) {
        return SqlBuilder.update("DELETE FROM follow_user WHERE follower_id = ? AND following_id = ?")
                .param(followerId).param(followingId)
                .execute() > 0;
    }

    public static boolean exists(int followerId, int followingId) {
        return SqlBuilder.select("SELECT 1 FROM follow_user WHERE follower_id = ? AND following_id = ? LIMIT 1")
                .param(followerId).param(followingId)
                .queryExists();
    }

    /** 获取 following 列表 */
    public static List<Integer> findFollowingIds(int followerId) {
        return SqlBuilder.select("SELECT following_id FROM follow_user WHERE follower_id = ?")
                .param(followerId)
                .queryList(rs -> rs.getInt(1));
    }

    /** 获取粉丝列表 */
    public static List<Integer> findFollowerIds(int followingId) {
        return SqlBuilder.select("SELECT follower_id FROM follow_user WHERE following_id = ?")
                .param(followingId)
                .queryList(rs -> rs.getInt(1));
    }

    public static int countFollowing(int followerId) {
        return SqlBuilder.select("SELECT COUNT(*) FROM follow_user WHERE follower_id = ?")
                .param(followerId)
                .queryCount();
    }

    public static int countFollowers(int followingId) {
        return SqlBuilder.select("SELECT COUNT(*) FROM follow_user WHERE following_id = ?")
                .param(followingId)
                .queryCount();
    }
}
