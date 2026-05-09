package top.xihale.clouddoc.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户视图对象，用于公开信息展示
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Integer role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 关注数 */
    private Integer followingCount;
    /** 粉丝数 */
    private Integer followerCount;
    /** 当前登录用户是否关注了此人 */
    private Boolean isFollowed;
}
