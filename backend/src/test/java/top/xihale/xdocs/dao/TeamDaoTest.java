package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class TeamDaoTest extends BaseDaoTest {

    private User createUser(String name) {
        User u = new User(name, "pw", name + "@t.com");
        UserDao.insert(u);
        return u;
    }

    @Test void insert_and_findById() {
        User owner = createUser("owner");
        Team team = new Team("Team A", "desc", owner.getId());
        TeamDao.insert(team);
        assertNotNull(team.getId());
        assertTrue(TeamDao.findById(team.getId()).isPresent());
    }

    @Test void findByOwnerId() {
        User owner = createUser("owner2");
        TeamDao.insert(new Team("T1", "d", owner.getId()));
        TeamDao.insert(new Team("T2", "d", owner.getId()));
        assertEquals(2, TeamDao.findByOwnerId(owner.getId()).size());
    }

    @Test void findByUserId() {
        User owner = createUser("owner3");
        User member = createUser("member");
        Team team = new Team("T", "d", owner.getId());
        TeamDao.insert(team);
        TeamMember tm = new TeamMember(team.getId(), member.getId(), 2, 1, owner.getId());
        TeamMemberDao.insert(tm);

        var teams = TeamDao.findByUserId(member.getId());
        assertEquals(1, teams.size());
    }

    @Test void update_and_delete() {
        User owner = createUser("owner4");
        Team team = new Team("Old", "old", owner.getId());
        TeamDao.insert(team);
        team.setName("New");
        assertEquals(1, TeamDao.update(team));
        assertEquals("New", TeamDao.findById(team.getId()).get().getName());
        TeamDao.deleteById(team.getId());
        assertTrue(TeamDao.findById(team.getId()).isEmpty());
    }

    @Test void findAll() {
        User owner = createUser("owner5");
        TeamDao.insert(new Team("A", "d", owner.getId()));
        TeamDao.insert(new Team("B", "d", owner.getId()));
        assertTrue(TeamDao.findAll().size() >= 2);
    }
}
