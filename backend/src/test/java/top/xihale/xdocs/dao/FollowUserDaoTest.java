package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class FollowUserDaoTest extends BaseDaoTest {

    private int[] createTwoUsers() {
        User u1 = new User("u1", "p", "u1@t.com");
        UserDao.insert(u1);
        User u2 = new User("u2", "p", "u2@t.com");
        UserDao.insert(u2);
        return new int[]{u1.getId(), u2.getId()};
    }

    @Test void insert_and_exists() {
        int[] ids = createTwoUsers();
        assertTrue(FollowUserDao.insert(ids[0], ids[1]));
        assertTrue(FollowUserDao.exists(ids[0], ids[1]));
    }

    @Test void insert_duplicate() {
        int[] ids = createTwoUsers();
        FollowUserDao.insert(ids[0], ids[1]);
        assertFalse(FollowUserDao.insert(ids[0], ids[1]));
    }

    @Test void delete() {
        int[] ids = createTwoUsers();
        FollowUserDao.insert(ids[0], ids[1]);
        assertTrue(FollowUserDao.delete(ids[0], ids[1]));
        assertFalse(FollowUserDao.exists(ids[0], ids[1]));
    }

    @Test void findFollowingIds() {
        int[] ids = createTwoUsers();
        FollowUserDao.insert(ids[0], ids[1]);
        var following = FollowUserDao.findFollowingIds(ids[0]);
        assertEquals(1, following.size());
        assertTrue(following.contains(ids[1]));
    }

    @Test void findFollowerIds() {
        int[] ids = createTwoUsers();
        FollowUserDao.insert(ids[0], ids[1]);
        var followers = FollowUserDao.findFollowerIds(ids[1]);
        assertEquals(1, followers.size());
        assertTrue(followers.contains(ids[0]));
    }

    @Test void countFollowing_and_countFollowers() {
        int[] ids = createTwoUsers();
        FollowUserDao.insert(ids[0], ids[1]);
        assertEquals(1, FollowUserDao.countFollowing(ids[0]));
        assertEquals(1, FollowUserDao.countFollowers(ids[1]));
        assertEquals(0, FollowUserDao.countFollowing(ids[1]));
    }
}
