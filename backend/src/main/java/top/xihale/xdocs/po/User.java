package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;
import top.xihale.xdocs.constant.Role;
import top.xihale.xdocs.constant.UserStatus;
import top.xihale.xdocs.vo.UserVO;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "password")
@Table("sys_user")
public class User {

    @Id
    private Integer id;
    private String username;
    private transient String password;
    private String email;
    private String nickname;
    private String avatarUrl;
    private int role;
    private int status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = Role.USER.getCode();
        this.status = UserStatus.NORMAL.getCode();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public UserStatus getStatusEnum() {
        return UserStatus.fromCode(status);
    }

    public void setStatusEnum(UserStatus status) {
        if (status != null) {
            this.status = status.getCode();
        }
    }

    public Role getRoleEnum() {
        return Role.fromCode(role);
    }

    public void setRoleEnum(Role role) {
        if (role != null) {
            this.role = role.getCode();
        }
    }

    /**
     * 转为视图对象，不包含密码等敏感字段
     */
    public UserVO toVO() {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setEmail(email);
        vo.setNickname(nickname);
        vo.setAvatarUrl(avatarUrl);
        vo.setRole(role);
        vo.setStatus(status);
        vo.setCreateTime(createTime);
        vo.setUpdateTime(updateTime);
        return vo;
    }
}
