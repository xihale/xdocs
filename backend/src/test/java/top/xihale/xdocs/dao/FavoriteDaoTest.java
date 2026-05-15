package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class FavoriteDaoTest extends BaseDaoTest {

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
        assertTrue(FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
        assertTrue(FavoriteDao.exists(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
    }

    @Test void insert_duplicate() {
        int[] ids = createArticleAndUser();
        FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]);
        assertFalse(FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
    }

    @Test void delete() {
        int[] ids = createArticleAndUser();
        FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]);
        assertTrue(FavoriteDao.delete(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
        assertFalse(FavoriteDao.exists(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
    }

    @Test void findArticleIds() {
        int[] ids = createArticleAndUser();
        FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]);
        var articleIds = FavoriteDao.findArticleIds(ids[1]);
        assertEquals(1, articleIds.size());
        assertTrue(articleIds.contains(ids[0]));
    }

    @Test void deleteByTargetId() {
        int[] ids = createArticleAndUser();
        FavoriteDao.insert(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]);
        FavoriteDao.deleteByTargetId(FavoriteDao.TYPE_ARTICLE, ids[0]);
        assertFalse(FavoriteDao.exists(ids[1], FavoriteDao.TYPE_ARTICLE, ids[0]));
    }
}
