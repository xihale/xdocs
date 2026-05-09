package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TEAM 成员视图对象，附带用户信息
 */
@Getter
@Setter
@AllArgsConstructor
public class TeamMemberVO {
    private Integer id;
    private Integer teamId;
    private Integer userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private int role;
    private String roleName;
    private int joinStatus;
    private LocalDateTime joinTime;
}
