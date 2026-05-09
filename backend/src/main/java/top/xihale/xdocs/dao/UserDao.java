package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
public class UserDao extends BaseMapper<User> {

    public static final UserDao INSTANCE = new UserDao();

    public int updateNickname(int id, String nickname) {
        return SqlBuilder.update("UPDATE sys_user SET nickname=? WHERE id=?")
                .param(nickname).param(id)
                .execute();
    }

    public int updatePassword(int id, String passwordHash) {
        return SqlBuilder.update("UPDATE sys_user SET password=? WHERE id=?")
                .param(passwordHash).param(id)
                .execute();
    }

    public int updateAvatar(int id, String avatarUrl) {
        return SqlBuilder.update("UPDATE sys_user SET avatar_url=? WHERE id=?")
                .param(avatarUrl).param(id)
                .execute();
    }

    public Optional<User> findByUsername(String username) {
        return findOne("username = ?", username);
    }

    public Optional<User> findByEmail(String email) {
        return findOne("email = ?", email);
    }

    public List<User> searchByKeyword(String keyword) {
        return findList("username LIKE ? OR nickname LIKE ? OR email LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
    }
}
