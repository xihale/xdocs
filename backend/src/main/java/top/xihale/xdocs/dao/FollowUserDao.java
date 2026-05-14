package top.xihale.xdocs.dao;

import top.xihale.xdocs.util.Db;

import java.util.List;

/**
 * 关注数据访问层
 */
public class FollowUserDao {

    public static boolean insert(int followerId, int followingId) {
        return Db.sql("INSERT IGNORE INTO follow_user(follower_id, following_id) VALUES(:followerId, :followingId)")
                .param("followerId", followerId).param("followingId", followingId)
                .execute() > 0;
    }

    public static boolean delete(int followerId, int followingId) {
        return Db.sql("DELETE FROM follow_user WHERE follower_id = :followerId AND following_id = :followingId")
                .param("followerId", followerId).param("followingId", followingId)
                .execute() > 0;
    }

    public static boolean exists(int followerId, int followingId) {
        return Db.sql("SELECT 1 FROM follow_user WHERE follower_id = :followerId AND following_id = :followingId LIMIT 1")
                .param("followerId", followerId).param("followingId", followingId)
                .query(Integer.class)
                .exists();
    }

    /** 获取 following 列表 */
    public static List<Integer> findFollowingIds(int followerId) {
        return Db.sql("SELECT following_id FROM follow_user WHERE follower_id = :followerId")
                .param("followerId", followerId)
                .query(rs -> rs.getInt(1))
                .list();
    }

    /** 获取粉丝列表 */
    public static List<Integer> findFollowerIds(int followingId) {
        return Db.sql("SELECT follower_id FROM follow_user WHERE following_id = :followingId")
                .param("followingId", followingId)
                .query(rs -> rs.getInt(1))
                .list();
    }

    public static int countFollowing(int followerId) {
        return Db.sql("SELECT COUNT(*) FROM follow_user WHERE follower_id = :followerId")
                .param("followerId", followerId)
                .query(Integer.class)
                .count();
    }

    public static int countFollowers(int followingId) {
        return Db.sql("SELECT COUNT(*) FROM follow_user WHERE following_id = :followingId")
                .param("followingId", followingId)
                .query(Integer.class)
                .count();
    }
}
