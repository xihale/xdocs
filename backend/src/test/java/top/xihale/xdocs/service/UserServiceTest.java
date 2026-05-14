package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.Role;
import top.xihale.xdocs.constant.UserStatus;
import top.xihale.xdocs.dao.FollowUserDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.exception.UserException;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.PasswordUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private MockedStatic<UserDao> userDaoMock;
    private MockedStatic<FollowUserDao> followUserDaoMock;
    private MockedStatic<PasswordUtils> passwordUtilsMock;
    private MockedStatic<NotificationService> notificationServiceMock;

    @BeforeEach
    void setUp() {
        userDaoMock = mockStatic(UserDao.class);
        followUserDaoMock = mockStatic(FollowUserDao.class);
        passwordUtilsMock = mockStatic(PasswordUtils.class);
        notificationServiceMock = mockStatic(NotificationService.class);
    }

    @AfterEach
    void tearDown() {
        notificationServiceMock.close();
        passwordUtilsMock.close();
        followUserDaoMock.close();
        userDaoMock.close();
    }

    // ==================== register ====================

    @Test
    void register_success() {
        userDaoMock.when(() -> UserDao.findByUsername("alice")).thenReturn(Optional.empty());
        userDaoMock.when(() -> UserDao.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        passwordUtilsMock.when(() -> PasswordUtils.hash("pass123")).thenReturn("$2a$12$hashed");

        User result = UserService.register("alice", "pass123", "alice@test.com", "Alice");

        assertEquals("alice", result.getUsername());
        assertEquals("alice@test.com", result.getEmail());
        assertEquals("Alice", result.getNickname());
        userDaoMock.verify(() -> UserDao.insert(any(User.class)));
    }

    @Test
    void register_nicknameDefaultsToUsername() {
        userDaoMock.when(() -> UserDao.findByUsername("bob")).thenReturn(Optional.empty());
        userDaoMock.when(() -> UserDao.findByEmail("bob@test.com")).thenReturn(Optional.empty());
        passwordUtilsMock.when(() -> PasswordUtils.hash("pass")).thenReturn("hash");

        User result = UserService.register("bob", "pass", "bob@test.com", null);
        assertEquals("bob", result.getNickname());
    }

    @Test
    void register_usernameExists_throws() {
        userDaoMock.when(() -> UserDao.findByUsername("alice"))
                .thenReturn(Optional.of(new User()));

        assertThrows(UserException.class,
                () -> UserService.register("alice", "pass", "new@test.com", null));
    }

    @Test
    void register_emailExists_throws() {
        userDaoMock.when(() -> UserDao.findByUsername("newuser")).thenReturn(Optional.empty());
        userDaoMock.when(() -> UserDao.findByEmail("taken@test.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(UserException.class,
                () -> UserService.register("newuser", "pass", "taken@test.com", null));
    }

    // ==================== login ====================

    @Test
    void login_success() {
        User user = new User("alice", "hashed", "alice@test.com");
        user.setStatus(UserStatus.NORMAL.getCode());

        userDaoMock.when(() -> UserDao.findByUsername("alice")).thenReturn(Optional.of(user));
        passwordUtilsMock.when(() -> PasswordUtils.verify("pass123", "hashed")).thenReturn(true);

        User result = UserService.login("alice", "pass123");
        assertEquals("alice", result.getUsername());
    }

    @Test
    void login_userNotFound_throws() {
        userDaoMock.when(() -> UserDao.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> UserService.login("nobody", "pass"));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User("alice", "hashed", "alice@test.com");
        user.setStatus(UserStatus.NORMAL.getCode());

        userDaoMock.when(() -> UserDao.findByUsername("alice")).thenReturn(Optional.of(user));
        passwordUtilsMock.when(() -> PasswordUtils.verify("wrong", "hashed")).thenReturn(false);

        assertThrows(UserException.class, () -> UserService.login("alice", "wrong"));
    }

    @Test
    void login_bannedUser_throws() {
        User user = new User("alice", "hashed", "alice@test.com");
        user.setStatus(UserStatus.BANNED.getCode());

        userDaoMock.when(() -> UserDao.findByUsername("alice")).thenReturn(Optional.of(user));
        passwordUtilsMock.when(() -> PasswordUtils.verify("pass", "hashed")).thenReturn(true);

        assertThrows(UserException.class, () -> UserService.login("alice", "pass"));
    }

    // ==================== changePassword ====================

    @Test
    void changePassword_success() {
        User user = new User("alice", "oldHash", "a@t.com");
        user.setId(1);

        userDaoMock.when(() -> UserDao.findById(1)).thenReturn(Optional.of(user));
        passwordUtilsMock.when(() -> PasswordUtils.verify("oldPass", "oldHash")).thenReturn(true);
        passwordUtilsMock.when(() -> PasswordUtils.hash("newPass")).thenReturn("newHash");

        assertDoesNotThrow(() -> UserService.changePassword(1, "oldPass", "newPass"));
        userDaoMock.verify(() -> UserDao.updatePassword(1, "newHash"));
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        User user = new User("alice", "oldHash", "a@t.com");
        user.setId(1);

        userDaoMock.when(() -> UserDao.findById(1)).thenReturn(Optional.of(user));
        passwordUtilsMock.when(() -> PasswordUtils.verify("wrong", "oldHash")).thenReturn(false);

        assertThrows(UserException.class, () -> UserService.changePassword(1, "wrong", "newPass"));
    }

    // ==================== follow / unfollow ====================

    @Test
    void follow_self_throws() {
        assertThrows(UserException.class, () -> UserService.follow(1, 1));
    }

    @Test
    void follow_success() {
        assertDoesNotThrow(() -> UserService.follow(1, 2));
        followUserDaoMock.verify(() -> FollowUserDao.insert(1, 2));
        notificationServiceMock.verify(() -> NotificationService.notifyFollow(2, 1));
    }

    @Test
    void unfollow_success() {
        UserService.unfollow(1, 2);
        followUserDaoMock.verify(() -> FollowUserDao.delete(1, 2));
    }

    // ==================== role / status checks ====================

    @Test
    void isAdmin_adminUser() {
        User admin = new User();
        admin.setRole(Role.ADMIN.getCode());
        assertTrue(UserService.isAdmin(admin));
    }

    @Test
    void isAdmin_normalUser() {
        User user = new User();
        user.setRole(Role.USER.getCode());
        assertFalse(UserService.isAdmin(user));
    }

    @Test
    void requireAdmin_nonAdmin_throws() {
        User user = new User();
        user.setRole(Role.USER.getCode());
        assertThrows(UserException.class, () -> UserService.requireAdmin(user));
    }

    @Test
    void requireActive_bannedUser_throws() {
        User user = new User();
        user.setStatus(UserStatus.BANNED.getCode());
        assertThrows(UserException.class, () -> UserService.requireActive(user));
    }

    @Test
    void requireActive_normalUser_ok() {
        User user = new User();
        user.setStatus(UserStatus.NORMAL.getCode());
        assertDoesNotThrow(() -> UserService.requireActive(user));
    }

    // ==================== query helpers ====================

    @Test
    void emailExists_true() {
        userDaoMock.when(() -> UserDao.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertTrue(UserService.emailExists("a@b.com"));
    }

    @Test
    void emailExists_false() {
        userDaoMock.when(() -> UserDao.findByEmail("a@b.com")).thenReturn(Optional.empty());
        assertFalse(UserService.emailExists("a@b.com"));
    }

    @Test
    void emailNotExists_true() {
        userDaoMock.when(() -> UserDao.findByEmail("a@b.com")).thenReturn(Optional.empty());
        assertTrue(UserService.emailNotExists("a@b.com"));
    }

    // ==================== ensureEmail* ====================

    @Test
    void ensureEmailAvailableForRegister_taken_throws() {
        userDaoMock.when(() -> UserDao.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));
        assertThrows(UserException.class, () -> UserService.ensureEmailAvailableForRegister("taken@test.com"));
    }

    @Test
    void ensureEmailRegisteredForReset_notFound_throws() {
        userDaoMock.when(() -> UserDao.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> UserService.ensureEmailRegisteredForReset("none@test.com"));
    }

    // ==================== buildUserVOList ====================

    @Test
    void buildUserVOList_skipsMissingUsers() {
        User alice = new User("alice", "h", "a@t.com");
        alice.setId(1);
        alice.setNickname("Alice");

        userDaoMock.when(() -> UserDao.findById(1)).thenReturn(Optional.of(alice));
        userDaoMock.when(() -> UserDao.findById(999)).thenReturn(Optional.empty());

        var result = UserService.buildUserVOList(List.of(1, 999));
        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
    }
}
