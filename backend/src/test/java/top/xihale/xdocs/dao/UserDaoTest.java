package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest extends BaseDaoTest {

    @Test
    void insert_and_findById() {
        User user = new User("alice", "hashed_pw", "alice@test.com");
        user.setNickname("Alice");
        UserDao.insert(user);

        assertNotNull(user.getId());

        Optional<User> found = UserDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
        assertEquals("Alice", found.get().getNickname());
    }

    @Test
    void findById_notFound() {
        assertTrue(UserDao.findById(99999).isEmpty());
    }

    @Test
    void findByUsername_exists() {
        User user = new User("bob", "pw", "bob@test.com");
        UserDao.insert(user);

        Optional<User> found = UserDao.findByUsername("bob");
        assertTrue(found.isPresent());
        assertEquals("bob", found.get().getUsername());
    }

    @Test
    void findByUsername_notFound() {
        assertTrue(UserDao.findByUsername("nobody").isEmpty());
    }

    @Test
    void findByEmail_exists() {
        User user = new User("carol", "pw", "carol@test.com");
        UserDao.insert(user);

        Optional<User> found = UserDao.findByEmail("carol@test.com");
        assertTrue(found.isPresent());
    }

    @Test
    void findByEmail_notFound() {
        assertTrue(UserDao.findByEmail("no@test.com").isEmpty());
    }

    @Test
    void searchByKeyword_matchesUsername() {
        User user = new User("testuser", "pw", "t@t.com");
        UserDao.insert(user);

        var results = UserDao.searchByKeyword("test");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(u -> "testuser".equals(u.getUsername())));
    }

    @Test
    void searchByKeyword_noMatch() {
        User user = new User("abc", "pw", "a@b.com");
        UserDao.insert(user);

        assertTrue(UserDao.searchByKeyword("xyz123nonexistent").isEmpty());
    }

    @Test
    void updateNickname() {
        User user = new User("dave", "pw", "dave@test.com");
        UserDao.insert(user);

        int rows = UserDao.updateNickname(user.getId(), "David");
        assertEquals(1, rows);

        Optional<User> updated = UserDao.findById(user.getId());
        assertEquals("David", updated.get().getNickname());
    }

    @Test
    void updatePassword() {
        User user = new User("eve", "old", "eve@test.com");
        UserDao.insert(user);

        int rows = UserDao.updatePassword(user.getId(), "new_hash");
        assertEquals(1, rows);
    }

    @Test
    void updateAvatar() {
        User user = new User("frank", "pw", "frank@test.com");
        UserDao.insert(user);

        UserDao.updateAvatar(user.getId(), "/avatars/frank.png");
        assertEquals("/avatars/frank.png", UserDao.findById(user.getId()).get().getAvatarUrl());
    }

    @Test
    void update() {
        User user = new User("grace", "pw", "grace@test.com");
        UserDao.insert(user);

        user.setNickname("Grace");
        user.setStatus(1);
        int rows = UserDao.update(user);
        assertEquals(1, rows);

        User updated = UserDao.findById(user.getId()).get();
        assertEquals("Grace", updated.getNickname());
        assertEquals(1, updated.getStatus());
    }

    @Test
    void findAll() {
        UserDao.insert(new User("u1", "p1", "u1@t.com"));
        UserDao.insert(new User("u2", "p2", "u2@t.com"));

        var all = UserDao.findAll();
        assertTrue(all.size() >= 2);
    }
}
