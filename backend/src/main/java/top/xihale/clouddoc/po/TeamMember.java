package top.xihale.clouddoc.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.clouddoc.annotation.Id;
import top.xihale.clouddoc.annotation.Table;

import java.time.LocalDateTime;

/**
 * TEAM 成员关系实体，对应 team_member 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("team_member")
public class TeamMember {

    @Id
    private Integer id;
    private Integer teamId;
    private Integer userId;
    private int role;
    private int joinStatus;
    private Integer inviteBy;
    private LocalDateTime joinTime;

    public TeamMember(Integer teamId, Integer userId, int role, int joinStatus, Integer inviteBy) {
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.joinStatus = joinStatus;
        this.inviteBy = inviteBy;
        this.joinTime = LocalDateTime.now();
    }
}
