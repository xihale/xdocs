package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库成员视图对象，附带用户信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KbMemberVO {
    private Integer id;
    private Integer kbId;
    private Integer userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private int role;
    private String roleName;
    private int inviteStatus;
    private LocalDateTime joinTime;
}
