package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Team;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * TEAM 数据访问层
 */
public class TeamDao extends BaseMapper<Team> {

    public static final TeamDao INSTANCE = new TeamDao();

    public List<Team> findByOwnerId(Integer ownerId) {
        return findList("owner_id = ?", ownerId);
    }

    public List<Team> findByUserId(Integer userId) {
        return SqlBuilder.select("SELECT t.id, t.name, t.description, t.owner_id, t.avatar_url, t.create_time, t.update_time "
                        + "FROM team t INNER JOIN team_member tm ON t.id = tm.team_id WHERE tm.user_id = ? AND tm.join_status = 1")
                .param(userId)
                .queryList(mapper());
    }
}
