package top.xihale.clouddoc.po;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import top.xihale.clouddoc.annotation.Id;
import top.xihale.clouddoc.annotation.Table;
import top.xihale.clouddoc.constant.Role;
import top.xihale.clouddoc.constant.UserStatus;

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
}
