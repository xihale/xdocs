package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.Role;
import top.xihale.xdocs.constant.UserStatus;
import top.xihale.xdocs.dao.FollowUserDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.exception.UserException;
import top.xihale.xdocs.exception.UserException.UserError;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.PasswordUtils;
import top.xihale.xdocs.service.NotificationService;
import top.xihale.xdocs.vo.UserVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户业务逻辑层
 */
public class UserService {

    // ==================== 认证相关 ====================

    public static User register(String username, String password, String email, String nickname) {
        if (UserDao.INSTANCE.findByUsername(username).isPresent()) {
            throw new UserException(UserError.USERNAME_EXISTS);
        }
        if (UserDao.INSTANCE.findByEmail(email).isPresent()) {
            throw new UserException(UserError.EMAIL_EXISTS);
        }

        var user = new User(username, PasswordUtils.hash(password), email);
        user.setNickname(nickname != null ? nickname : username);
        UserDao.INSTANCE.insert(user);

        return user;
    }

    public static User login(String username, String password) {
        var user = UserDao.INSTANCE.findByUsername(username)
                .orElseThrow(() -> new UserException(UserError.LOGIN_FAILED));

        if (!PasswordUtils.verify(password, user.getPassword())) {
            throw new UserException(UserError.LOGIN_FAILED);
        }

        requireActive(user);

        return user;
    }

    // ==================== 用户操作 ====================

    public static void update(User user) {
        UserDao.INSTANCE.update(user);
    }

    public static void updateNickname(int id, String nickname) {
        UserDao.INSTANCE.updateNickname(id, nickname);
    }

    public static void changePassword(int id, String oldPass, String newPass) {
        var user = UserDao.INSTANCE.findById(id)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        if (!PasswordUtils.verify(oldPass, user.getPassword())) {
            throw new UserException(UserError.OLD_PASSWORD_WRONG);
        }
        UserDao.INSTANCE.updatePassword(id, PasswordUtils.hash(newPass));
    }

    public static void resetPassword(String email, String newPassword) {
        var user = UserDao.INSTANCE.findByEmail(email)
                .orElseThrow(() -> new UserException(UserError.EMAIL_NOT_REGISTERED));
        UserDao.INSTANCE.updatePassword(user.getId(), PasswordUtils.hash(newPassword));
    }

    public static void updateAvatar(int id, String avatarUrl) {
        UserDao.INSTANCE.updateAvatar(id, avatarUrl);
    }

    // ==================== 查询相关 ====================

    public static User findUserByUsername(String username) {
        return UserDao.INSTANCE.findByUsername(username)
                .orElseThrow(() -> new UserException(UserError.USERNAME_NOT_FOUND));
    }

    public static User findUserById(int id) {
        return UserDao.INSTANCE.findById(id)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }

    public static Optional<User> findUserByIdOptional(int id) {
        return UserDao.INSTANCE.findById(id);
    }

    public static boolean emailExists(String email) {
        return UserDao.INSTANCE.findByEmail(email).isPresent();
    }

    public static boolean emailNotExists(String email) {
        return UserDao.INSTANCE.findByEmail(email).isEmpty();
    }

    public static List<User> findAll() {
        List<User> users = UserDao.INSTANCE.findAll();
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public static List<User> searchByKeyword(String keyword) {
        return UserDao.INSTANCE.searchByKeyword(keyword);
    }

    // ==================== VO 构建 ====================

    /**
     * 构建用户 Profile VO
     */
    public static UserVO buildUserProfile(int userId, Integer currentUserId) {
        User user = findUserById(userId);
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setFollowingCount(countFollowing(userId));
        vo.setFollowerCount(countFollowers(userId));
        vo.setIsFollowed(currentUserId != null && !currentUserId.equals(userId)
                ? isFollowing(currentUserId, userId) : false);
        return vo;
    }

    /**
     * 批量构建用户简要 VO 列表
     */
    public static List<UserVO> buildUserVOList(List<Integer> ids) {
        List<UserVO> voList = new ArrayList<>();
        for (int id : ids) {
            var user = UserDao.INSTANCE.findById(id).orElse(null);
            if (user == null) continue;
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
            voList.add(vo);
        }
        return voList;
    }

    // ==================== 关注相关 ====================

    public static void follow(int followerId, int followingId) {
        FollowUserDao.insert(followerId, followingId);
        NotificationService.notifyFollow(followingId, followerId);
    }

    public static void unfollow(int followerId, int followingId) {
        FollowUserDao.delete(followerId, followingId);
    }

    public static int countFollowing(int userId) {
        return FollowUserDao.countFollowing(userId);
    }

    public static int countFollowers(int userId) {
        return FollowUserDao.countFollowers(userId);
    }

    public static boolean isFollowing(int followerId, int followingId) {
        return FollowUserDao.exists(followerId, followingId);
    }

    public static List<Integer> findFollowingIds(int userId) {
        return FollowUserDao.findFollowingIds(userId);
    }

    public static List<Integer> findFollowerIds(int userId) {
        return FollowUserDao.findFollowerIds(userId);
    }

    // ==================== 校验工具 ====================

    public static boolean isAdmin(User user) {
        return user.getRoleEnum() == Role.ADMIN;
    }

    public static void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new UserException(UserError.REQUIRE_ADMIN);
        }
    }

    public static void requireActive(User user) {
        if (user.getStatusEnum() == UserStatus.BANNED) {
            throw new UserException(UserError.USER_BANNED);
        }
    }
}
