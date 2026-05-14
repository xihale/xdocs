package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.TeamMember;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * TEAM 成员关系数据访问层
 */
public class TeamMemberDao {

    private static final BaseMapper<TeamMember> MAPPER = new BaseMapper<>(TeamMember.class);

    public static void insert(TeamMember member) { MAPPER.insert(member); }

    public static int updateRole(Integer teamId, Integer userId, int role) {
        return Db.sql("UPDATE team_member SET role = :role WHERE team_id = :teamId AND user_id = :userId")
                .param("role", role).param("teamId", teamId).param("userId", userId)
                .execute();
    }

    public static int updateJoinStatus(Integer teamId, Integer userId, int joinStatus) {
        return Db.sql("UPDATE team_member SET join_status = :joinStatus WHERE team_id = :teamId AND user_id = :userId")
                .param("joinStatus", joinStatus).param("teamId", teamId).param("userId", userId)
                .execute();
    }

    public static int delete(Integer teamId, Integer userId) {
        return Db.sql("DELETE FROM team_member WHERE team_id = :teamId AND user_id = :userId")
                .param("teamId", teamId).param("userId", userId)
                .execute();
    }

    public static Optional<TeamMember> findByTeamIdAndUserId(Integer teamId, Integer userId) {
        return MAPPER.findOne("team_id = ? AND user_id = ?", teamId, userId);
    }

    public static List<TeamMember> findByTeamId(Integer teamId) {
        return MAPPER.findList("team_id = ?", teamId);
    }

    public static List<TeamMember> findByUserId(Integer userId) {
        return MAPPER.findList("user_id = ?", userId);
    }

    public static List<TeamMember> findPendingByUserId(Integer userId) {
        return MAPPER.findList("user_id = ? AND join_status = 0", userId);
    }
}
