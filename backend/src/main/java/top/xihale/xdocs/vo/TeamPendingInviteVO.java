package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 团队待处理邀请视图对象
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamPendingInviteVO {
    private Integer id;
    private Integer teamId;
    private String teamName;
    private int joinStatus;
    private LocalDateTime joinTime;
}
