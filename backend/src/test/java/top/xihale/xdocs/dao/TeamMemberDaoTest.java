package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TeamMemberDaoTest extends BaseDaoTest {

    private int createTeamAndUsers() {
        User owner = new User("o", "p", "o@t.com");
        UserDao.insert(owner);
        User member = new User("m", "p", "m@t.com");
        UserDao.insert(member);
        Team team = new Team("T", "d", owner.getId());
        TeamDao.insert(team);
        TeamMember tm = new TeamMember(team.getId(), member.getId(), 2, 1, owner.getId());
        TeamMemberDao.insert(tm);
        return team.getId();
    }

    @Test void insert_and_findByTeamIdAndUserId() {
        int teamId = createTeamAndUsers();
        int userId = UserDao.findByUsername("m").get().getId();
        Optional<TeamMember> found = TeamMemberDao.findByTeamIdAndUserId(teamId, userId);
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getRole());
    }

    @Test void updateRole() {
        int teamId = createTeamAndUsers();
        int userId = UserDao.findByUsername("m").get().getId();
        TeamMemberDao.updateRole(teamId, userId, 1); // ADMIN
        assertEquals(1, TeamMemberDao.findByTeamIdAndUserId(teamId, userId).get().getRole());
    }

    @Test void updateJoinStatus() {
        int teamId = createTeamAndUsers();
        int userId = UserDao.findByUsername("m").get().getId();
        TeamMemberDao.updateJoinStatus(teamId, userId, 0);
        assertEquals(0, TeamMemberDao.findByTeamIdAndUserId(teamId, userId).get().getJoinStatus());
    }

    @Test void findByTeamId() {
        int teamId = createTeamAndUsers();
        assertFalse(TeamMemberDao.findByTeamId(teamId).isEmpty());
    }

    @Test void findByUserId() {
        createTeamAndUsers();
        int userId = UserDao.findByUsername("m").get().getId();
        assertFalse(TeamMemberDao.findByUserId(userId).isEmpty());
    }

    @Test void findPendingByUserId() {
        User owner = new User("o2", "p", "o2@t.com");
        UserDao.insert(owner);
        User member = new User("m2", "p", "m2@t.com");
        UserDao.insert(member);
        Team team = new Team("T2", "d", owner.getId());
        TeamDao.insert(team);
        TeamMember tm = new TeamMember(team.getId(), member.getId(), 2, 0, owner.getId()); // INVITED=0
        TeamMemberDao.insert(tm);

        var pending = TeamMemberDao.findPendingByUserId(member.getId());
        assertEquals(1, pending.size());
    }

    @Test void delete() {
        int teamId = createTeamAndUsers();
        int userId = UserDao.findByUsername("m").get().getId();
        TeamMemberDao.delete(teamId, userId);
        assertTrue(TeamMemberDao.findByTeamIdAndUserId(teamId, userId).isEmpty());
    }
}
