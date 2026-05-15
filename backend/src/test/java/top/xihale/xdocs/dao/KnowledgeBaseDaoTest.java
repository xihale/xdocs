package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseDaoTest extends BaseDaoTest {

    private User createUser(String name) {
        User u = new User(name, "pw", name + "@t.com");
        UserDao.insert(u);
        return u;
    }

    @Test void insert_and_findById() {
        User u = createUser("u1");
        KnowledgeBase kb = new KnowledgeBase("KB", "desc", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        assertTrue(KnowledgeBaseDao.findById(kb.getId()).isPresent());
    }

    @Test void findByOwnerId() {
        User u = createUser("u2");
        KnowledgeBaseDao.insert(new KnowledgeBase("KB1", "d", 1, 0, u.getId(), u.getId()));
        KnowledgeBaseDao.insert(new KnowledgeBase("KB2", "d", 1, 0, u.getId(), u.getId()));
        assertEquals(2, KnowledgeBaseDao.findByOwnerId(0, u.getId()).size());
    }

    @Test void findPublicKnowledgeBases() {
        User u = createUser("u3");
        KnowledgeBase kb = new KnowledgeBase("PubKB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        assertFalse(KnowledgeBaseDao.findPublicKnowledgeBases().isEmpty());
    }

    @Test void findPublicByName() {
        User u = createUser("u4");
        KnowledgeBase kb = new KnowledgeBase("My Public KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        var results = KnowledgeBaseDao.findPublicByName("Public");
        assertFalse(results.isEmpty());
    }

    @Test void update() {
        User u = createUser("u5");
        KnowledgeBase kb = new KnowledgeBase("Old", "old", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        kb.setName("New");
        KnowledgeBaseDao.update(kb);
        assertEquals("New", KnowledgeBaseDao.findById(kb.getId()).get().getName());
    }

    @Test void deleteById() {
        User u = createUser("u6");
        KnowledgeBase kb = new KnowledgeBase("Del", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        KnowledgeBaseDao.deleteById(kb.getId());
        assertTrue(KnowledgeBaseDao.findById(kb.getId()).isEmpty());
    }
}
