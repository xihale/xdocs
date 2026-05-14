package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Team;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * TEAM 数据访问层
 */
public class TeamDao {

    private static final BaseMapper<Team> MAPPER = new BaseMapper<>(Team.class);

    private static final String SQL_FIND_BY_USER_ID =
            "SELECT t.id, t.name, t.description, t.owner_id, t.avatar_url, t.create_time, t.update_time FROM team t INNER JOIN team_member tm ON t.id = tm.team_id WHERE tm.user_id = :userId AND tm.join_status = 1";

    public static void insert(Team team) { MAPPER.insert(team); }
    public static int update(Team team) { return MAPPER.update(team); }
    public static int deleteById(Object id) { return MAPPER.deleteById(id); }
    public static Optional<Team> findById(Object id) { return MAPPER.findById(id); }
    public static List<Team> findAll() { return MAPPER.findAll(); }

    public static List<Team> findByOwnerId(Integer ownerId) {
        return MAPPER.findList("owner_id = ?", ownerId);
    }

    public static List<Team> findByUserId(Integer userId) {
        return Db.sql(SQL_FIND_BY_USER_ID)
                .param("userId", userId)
                .query(MAPPER.mapper())
                .list();
    }
}
