package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class ArticleChatMessageDaoTest extends BaseDaoTest {

    private int createArticle() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        Article a = new Article(kb.getId(), "A", "C", u.getId());
        ArticleDao.insert(a);
        return a.getId();
    }

    @Test void insert_and_findByArticleId() {
        int articleId = createArticle();
        User sender = UserDao.findByUsername("u").get();
        ArticleChatMessage msg = new ArticleChatMessage(articleId, null, sender.getId(), 0, "Hello");
        ArticleChatMessageDao.insert(msg);
        assertNotNull(msg.getId());

        var messages = ArticleChatMessageDao.findByArticleId(articleId, 10);
        assertFalse(messages.isEmpty());
        assertEquals("Hello", messages.get(0).getContent());
    }

    @Test void findByArticleId_limited() {
        int articleId = createArticle();
        User sender = UserDao.findByUsername("u").get();
        ArticleChatMessageDao.insert(new ArticleChatMessage(articleId, null, sender.getId(), 0, "M1"));
        ArticleChatMessageDao.insert(new ArticleChatMessage(articleId, null, sender.getId(), 0, "M2"));

        assertEquals(1, ArticleChatMessageDao.findByArticleId(articleId, 1).size());
    }
}
