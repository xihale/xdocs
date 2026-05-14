package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库待处理邀请视图对象
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KbPendingInviteVO {
    private Integer id;
    private Integer kbId;
    private String kbName;
    private int role;
    private int inviteStatus;
    private LocalDateTime joinTime;
}
