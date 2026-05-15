package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ArticleDaoTest extends BaseDaoTest {

    private int createKbAndUser() {
        User user = new User("author", "pw", "author@t.com");
        UserDao.insert(user);
        KnowledgeBase kb = new KnowledgeBase("Test KB", "desc", 1, 0, user.getId(), user.getId());
        KnowledgeBaseDao.insert(kb);
        return kb.getId();
    }

    @Test
    void insert_and_findById() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article article = new Article(kbId, "Title", "Content", user.getId());
        ArticleDao.insert(article);
        assertNotNull(article.getId());

        Optional<Article> found = ArticleDao.findById(article.getId());
        assertTrue(found.isPresent());
        assertEquals("Title", found.get().getTitle());
    }

    @Test
    void findByKnowledgeBaseId() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        ArticleDao.insert(new Article(kbId, "A1", "C1", user.getId()));
        ArticleDao.insert(new Article(kbId, "A2", "C2", user.getId()));

        var articles = ArticleDao.findByKnowledgeBaseId(kbId);
        assertEquals(2, articles.size());
    }

    @Test
    void findPublicArticles() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article a1 = new Article(kbId, "Public Article", "Content", user.getId());
        a1.setStatus(1); // PUBLISHED
        ArticleDao.insert(a1);

        var articles = ArticleDao.findPublicArticles(0, 10);
        assertFalse(articles.isEmpty());
    }

    @Test
    void findPublicArticlesOrderByLikes() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article a = new Article(kbId, "Liked Article", "Content", user.getId());
        a.setStatus(1);
        ArticleDao.insert(a);

        var articles = ArticleDao.findPublicArticlesOrderByLikes(0, 10);
        assertFalse(articles.isEmpty());
    }

    @Test
    void searchPublic() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article a = new Article(kbId, "Searchable Title", "Some content here", user.getId());
        a.setStatus(1);
        ArticleDao.insert(a);

        var results = ArticleDao.searchPublic("Searchable", 0, 10);
        assertEquals(1, results.size());
    }

    @Test
    void searchPublic_noMatch() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article a = new Article(kbId, "Title", "Content", user.getId());
        a.setStatus(1);
        ArticleDao.insert(a);

        assertTrue(ArticleDao.searchPublic("xyzNONEXISTENT", 0, 10).isEmpty());
    }

    @Test
    void findByAuthorId() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        ArticleDao.insert(new Article(kbId, "A1", "C1", user.getId()));

        var articles = ArticleDao.findByAuthorId(user.getId());
        assertFalse(articles.isEmpty());
    }

    @Test
    void update_article() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article article = new Article(kbId, "Old", "Old", user.getId());
        ArticleDao.insert(article);

        article.setTitle("New Title");
        article.setContent("New Content");
        int rows = ArticleDao.update(article);
        assertEquals(1, rows);

        Article updated = ArticleDao.findById(article.getId()).get();
        assertEquals("New Title", updated.getTitle());
    }

    @Test
    void deleteById() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        Article article = new Article(kbId, "Del", "Del", user.getId());
        ArticleDao.insert(article);

        ArticleDao.deleteById(article.getId());
        assertTrue(ArticleDao.findById(article.getId()).isEmpty());
    }

    @Test
    void findAll() {
        int kbId = createKbAndUser();
        var user = UserDao.findByUsername("author").get();
        ArticleDao.insert(new Article(kbId, "A1", "C1", user.getId()));
        ArticleDao.insert(new Article(kbId, "A2", "C2", user.getId()));

        var all = ArticleDao.findAll();
        assertTrue(all.size() >= 2);
    }
}
