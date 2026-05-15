package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class CommentDaoTest extends BaseDaoTest {

    private int createArticleAndComment() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, u.getId(), u.getId());
        KnowledgeBaseDao.insert(kb);
        Article a = new Article(kb.getId(), "A", "C", u.getId());
        ArticleDao.insert(a);
        Comment c = new Comment();
        c.setArticleId(a.getId());
        c.setUserId(u.getId());
        c.setContent("Nice article");
        CommentDao.insert(c);
        return a.getId();
    }

    @Test void insert_and_findById() {
        int articleId = createArticleAndComment();
        var comments = CommentDao.findByArticleId(articleId);
        assertFalse(comments.isEmpty());
    }

    @Test void findByArticleId() {
        int articleId = createArticleAndComment();
        assertFalse(CommentDao.findByArticleId(articleId).isEmpty());
    }

    @Test void update() {
        int articleId = createArticleAndComment();
        var comments = CommentDao.findByArticleId(articleId);
        Comment c = comments.get(0);
        c.setContent("Updated");
        CommentDao.update(c);
        assertEquals("Updated", CommentDao.findById(c.getId()).get().getContent());
    }

    @Test void deleteById() {
        int articleId = createArticleAndComment();
        var comments = CommentDao.findByArticleId(articleId);
        CommentDao.deleteById(comments.get(0).getId());
        assertTrue(CommentDao.findById(comments.get(0).getId()).isEmpty());
    }

    @Test void deleteByArticleId() {
        int articleId = createArticleAndComment();
        CommentDao.deleteByArticleId(articleId);
        assertTrue(CommentDao.findByArticleId(articleId).isEmpty());
    }
}
