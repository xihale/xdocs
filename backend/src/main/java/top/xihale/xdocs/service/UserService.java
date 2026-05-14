package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.Role;
import top.xihale.xdocs.constant.UserStatus;
import top.xihale.xdocs.dao.FollowUserDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.exception.UserException;
import top.xihale.xdocs.exception.UserException.UserError;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.PasswordUtils;
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
        if (UserDao.findByUsername(username).isPresent()) {
            throw new UserException(UserError.USERNAME_EXISTS);
        }
        if (UserDao.findByEmail(email).isPresent()) {
            throw new UserException(UserError.EMAIL_EXISTS);
        }

        var user = new User(username, PasswordUtils.hash(password), email);
        user.setNickname(nickname != null ? nickname : username);
        UserDao.insert(user);

        return user;
    }

    public static User login(String username, String password) {
        var user = UserDao.findByUsername(username)
                .orElseThrow(() -> new UserException(UserError.LOGIN_FAILED));

        if (!PasswordUtils.verify(password, user.getPassword())) {
            throw new UserException(UserError.LOGIN_FAILED);
        }

        requireActive(user);

        return user;
    }

    // ==================== 用户操作 ====================

    public static void update(User user) {
        UserDao.update(user);
    }

    public static void updateNickname(int id, String nickname) {
        UserDao.updateNickname(id, nickname);
    }

    public static void changePassword(int id, String oldPass, String newPass) {
        var user = UserDao.findById(id)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        if (!PasswordUtils.verify(oldPass, user.getPassword())) {
            throw new UserException(UserError.OLD_PASSWORD_WRONG);
        }
        UserDao.updatePassword(id, PasswordUtils.hash(newPass));
    }

    public static void resetPassword(String email, String newPassword) {
        var user = UserDao.findByEmail(email)
                .orElseThrow(() -> new UserException(UserError.EMAIL_NOT_REGISTERED));
        UserDao.updatePassword(user.getId(), PasswordUtils.hash(newPassword));
    }

    public static void updateAvatar(int id, String avatarUrl) {
        UserDao.updateAvatar(id, avatarUrl);
    }

    // ==================== 查询相关 ====================

    public static User findUserByUsername(String username) {
        return UserDao.findByUsername(username)
                .orElseThrow(() -> new UserException(UserError.USERNAME_NOT_FOUND));
    }

    public static User findUserById(int id) {
        return UserDao.findById(id)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }

    public static Optional<User> findUserByIdOptional(int id) {
        return UserDao.findById(id);
    }

    public static boolean emailExists(String email) {
        return UserDao.findByEmail(email).isPresent();
    }

    public static boolean emailNotExists(String email) {
        return UserDao.findByEmail(email).isEmpty();
    }

    public static List<UserVO> findAll() {
        List<User> users = UserDao.findAll();
        return users.stream().map(User::toVO).toList();
    }

    public static List<User> searchByKeyword(String keyword) {
        return UserDao.searchByKeyword(keyword);
    }

    // ==================== VO 构建 ====================

    /**
     * 构建用户 Profile VO
     */
    public static UserVO buildUserProfile(int userId, Integer currentUserId) {
        User user = findUserById(userId);
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .followingCount(countFollowing(userId))
                .followerCount(countFollowers(userId))
                .isFollowed(currentUserId != null && !currentUserId.equals(userId)
                        && isFollowing(currentUserId, userId))
                .build();
    }

    /**
     * 批量构建用户简要 VO 列表
     */
    public static List<UserVO> buildUserVOList(List<Integer> ids) {
        List<UserVO> voList = new ArrayList<>();
        for (int id : ids) {
            var user = UserDao.findById(id).orElse(null);
            if (user == null) continue;
            voList.add(UserVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .build());
        }
        return voList;
    }

    // ==================== 关注相关 ====================

    public static void follow(int followerId, int followingId) {
        if (followerId == followingId) {
            throw new UserException(UserError.CANNOT_FOLLOW_SELF);
        }
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

    public static void ensureEmailAvailableForRegister(String email) {
        if (UserDao.findByEmail(email).isPresent()) {
            throw new UserException(UserError.EMAIL_EXISTS);
        }
    }

    public static void ensureEmailRegisteredForReset(String email) {
        if (UserDao.findByEmail(email).isEmpty()) {
            throw new UserException(UserError.EMAIL_NOT_REGISTERED);
        }
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
