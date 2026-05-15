package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseMemberDaoTest extends BaseDaoTest {

    private int createKbAndMember() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        User m = new User("m", "p", "m@t.com");
        UserDao.insert(m);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        KnowledgeBaseMemberDao.insert(new KnowledgeBaseMember(kb.getId(), m.getId(), 3, 1, u.getId()));
        return kb.getId();
    }

    @Test void insert_and_findByKbIdAndUserId() {
        int kbId = createKbAndMember();
        int userId = UserDao.findByUsername("m").get().getId();
        assertTrue(KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId).isPresent());
    }

    @Test void updateRole() {
        int kbId = createKbAndMember();
        int userId = UserDao.findByUsername("m").get().getId();
        KnowledgeBaseMemberDao.updateRole(kbId, userId, 2); // EDITOR
        assertEquals(2, KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId).get().getRole());
    }

    @Test void updateInviteStatus() {
        int kbId = createKbAndMember();
        int userId = UserDao.findByUsername("m").get().getId();
        KnowledgeBaseMemberDao.updateInviteStatus(kbId, userId, 0);
        assertEquals(0, KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId).get().getInviteStatus());
    }

    @Test void findByKbId() { assertFalse(KnowledgeBaseMemberDao.findByKbId(createKbAndMember()).isEmpty()); }
    
    @Test void findByUserId() {
        createKbAndMember();
        int userId = UserDao.findByUsername("m").get().getId();
        assertFalse(KnowledgeBaseMemberDao.findByUserId(userId).isEmpty());
    }

    @Test void findPendingByUserId() {
        User u = new User("o2", "p", "o2@t.com");
        UserDao.insert(u);
        User m = new User("m2", "p", "m2@t.com");
        UserDao.insert(m);
        KnowledgeBase kb = new KnowledgeBase("KB2", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        KnowledgeBaseMemberDao.insert(new KnowledgeBaseMember(kb.getId(), m.getId(), 3, 0, u.getId())); // PENDING
        assertEquals(1, KnowledgeBaseMemberDao.findPendingByUserId(m.getId()).size());
    }

    @Test void delete_and_deleteByKbId() {
        int kbId = createKbAndMember();
        int userId = UserDao.findByUsername("m").get().getId();
        KnowledgeBaseMemberDao.delete(kbId, userId);
        assertTrue(KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId).isEmpty());
    }
}
