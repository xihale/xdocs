package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class ArticleLikeDaoTest extends BaseDaoTest {

    private int[] createArticleAndUser() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        Article a = new Article(kb.getId(), "A", "C", u.getId());
        ArticleDao.insert(a);
        return new int[]{a.getId(), u.getId()};
    }

    @Test void insert_and_exists() {
        int[] ids = createArticleAndUser();
        assertTrue(ArticleLikeDao.insert(ids[0], ids[1]));
        assertTrue(ArticleLikeDao.exists(ids[0], ids[1]));
    }

    @Test void insert_duplicate_returns_false() {
        int[] ids = createArticleAndUser();
        ArticleLikeDao.insert(ids[0], ids[1]);
        assertFalse(ArticleLikeDao.insert(ids[0], ids[1])); // INSERT IGNORE
    }

    @Test void delete() {
        int[] ids = createArticleAndUser();
        ArticleLikeDao.insert(ids[0], ids[1]);
        assertTrue(ArticleLikeDao.delete(ids[0], ids[1]));
        assertFalse(ArticleLikeDao.exists(ids[0], ids[1]));
    }

    @Test void countByArticle() {
        int[] ids = createArticleAndUser();
        User u2 = new User("u2", "p", "u2@t.com");
        UserDao.insert(u2);
        ArticleLikeDao.insert(ids[0], ids[1]);
        ArticleLikeDao.insert(ids[0], u2.getId());
        assertEquals(2, ArticleLikeDao.countByArticle(ids[0]));
    }

    @Test void deleteByArticleId() {
        int[] ids = createArticleAndUser();
        ArticleLikeDao.insert(ids[0], ids[1]);
        ArticleLikeDao.deleteByArticleId(ids[0]);
        assertEquals(0, ArticleLikeDao.countByArticle(ids[0]));
    }
}
