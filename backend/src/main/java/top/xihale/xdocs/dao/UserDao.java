package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
public class UserDao {

    private static final BaseMapper<User> MAPPER = new BaseMapper<>(User.class);

    public static void insert(User user) { MAPPER.insert(user); }
    public static int update(User user) { return MAPPER.update(user); }
    public static Optional<User> findById(Object id) { return MAPPER.findById(id); }
    public static List<User> findAll() { return MAPPER.findAll(); }

    public static Optional<User> findByUsername(String username) {
        return MAPPER.findOne("username = ?", username);
    }

    public static Optional<User> findByEmail(String email) {
        return MAPPER.findOne("email = ?", email);
    }

    public static List<User> searchByKeyword(String keyword) {
        return MAPPER.findList("username LIKE ? OR nickname LIKE ? OR email LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
    }

    public static int updateNickname(int id, String nickname) {
        return Db.sql("UPDATE sys_user SET nickname = :nickname WHERE id = :id")
                .param("nickname", nickname).param("id", id)
                .execute();
    }

    public static int updatePassword(int id, String passwordHash) {
        return Db.sql("UPDATE sys_user SET password = :password WHERE id = :id")
                .param("password", passwordHash).param("id", id)
                .execute();
    }

    public static int updateAvatar(int id, String avatarUrl) {
        return Db.sql("UPDATE sys_user SET avatar_url = :avatarUrl WHERE id = :id")
                .param("avatarUrl", avatarUrl).param("id", id)
                .execute();
    }
}
