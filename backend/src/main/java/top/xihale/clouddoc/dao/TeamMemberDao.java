package top.xihale.clouddoc.dao;

import top.xihale.clouddoc.po.TeamMember;
import top.xihale.clouddoc.util.BaseMapper;
import top.xihale.clouddoc.util.SqlBuilder;

import java.util.List;
import java.util.Optional;

/**
 * TEAM 成员关系数据访问层
 */
public class TeamMemberDao extends BaseMapper<TeamMember> {

    public static final TeamMemberDao INSTANCE = new TeamMemberDao();

    public int updateRole(Integer teamId, Integer userId, int role) {
        return SqlBuilder.update("UPDATE team_member SET role=? WHERE team_id=? AND user_id=?")
                .param(role).param(teamId).param(userId)
                .execute();
    }

    public int updateJoinStatus(Integer teamId, Integer userId, int joinStatus) {
        return SqlBuilder.update("UPDATE team_member SET join_status=? WHERE team_id=? AND user_id=?")
                .param(joinStatus).param(teamId).param(userId)
                .execute();
    }

    public int delete(Integer teamId, Integer userId) {
        return SqlBuilder.update("DELETE FROM team_member WHERE team_id=? AND user_id=?")
                .param(teamId).param(userId)
                .execute();
    }

    public Optional<TeamMember> findByTeamIdAndUserId(Integer teamId, Integer userId) {
        return findOne("team_id=? AND user_id=?", teamId, userId);
    }

    public List<TeamMember> findByTeamId(Integer teamId) {
        return findList("team_id=?", teamId);
    }

    public List<TeamMember> findByUserId(Integer userId) {
        return findList("user_id=?", userId);
    }

    public List<TeamMember> findPendingByUserId(Integer userId) {
        return findList("user_id=? AND join_status=0", userId);
    }
}
