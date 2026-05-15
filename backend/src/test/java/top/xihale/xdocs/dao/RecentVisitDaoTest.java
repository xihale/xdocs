package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class RecentVisitDaoTest extends BaseDaoTest {

    private int[] createArticleAndUser() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        Article a = new Article(kb.getId(), "A", "C", u.getId());
        ArticleDao.insert(a);
        return new int[]{a.getId(), u.getId()};
    }

    @Test void upsert_new() {
        int[] ids = createArticleAndUser();
        RecentVisitDao.upsert(ids[1], ids[0]);
        var recent = RecentVisitDao.findRecentArticleIds(ids[1], 10);
        assertEquals(1, recent.size());
    }

    @Test void upsert_updateExisting() {
        int[] ids = createArticleAndUser();
        RecentVisitDao.upsert(ids[1], ids[0]);
        RecentVisitDao.upsert(ids[1], ids[0]); // ON DUPLICATE KEY UPDATE
        var recent = RecentVisitDao.findRecentArticleIds(ids[1], 10);
        assertEquals(1, recent.size());
    }

    @Test void findRecentArticleIds_limited() {
        int[] ids = createArticleAndUser();
        RecentVisitDao.upsert(ids[1], ids[0]);
        assertEquals(1, RecentVisitDao.findRecentArticleIds(ids[1], 1).size());
    }

    @Test void delete() {
        int[] ids = createArticleAndUser();
        RecentVisitDao.upsert(ids[1], ids[0]);
        RecentVisitDao.delete(ids[1], ids[0]);
        assertTrue(RecentVisitDao.findRecentArticleIds(ids[1], 10).isEmpty());
    }

    @Test void deleteAll() {
        int[] ids = createArticleAndUser();
        RecentVisitDao.upsert(ids[1], ids[0]);
        RecentVisitDao.deleteAll(ids[1]);
        assertTrue(RecentVisitDao.findRecentArticleIds(ids[1], 10).isEmpty());
    }
}
