package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.JoinStatus;
import top.xihale.xdocs.constant.KnowledgeBaseRole;
import top.xihale.xdocs.constant.OwnerType;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.exception.ArticleException;
import top.xihale.xdocs.po.*;
import top.xihale.xdocs.vo.ArticleVO;
import top.xihale.xdocs.vo.LikeResultVO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleServiceTest {

    private MockedStatic<ArticleDao> articleDao;
    private MockedStatic<KnowledgeBaseDao> kbDao;
    private MockedStatic<TeamDao> teamDao;
    private MockedStatic<TeamMemberDao> tmDao;
    private MockedStatic<KnowledgeBaseMemberDao> kbmDao;
    private MockedStatic<CommentDao> commentDao;
    private MockedStatic<ArticleLikeDao> likeDao;
    private MockedStatic<FavoriteDao> favDao;
    private MockedStatic<RecentVisitDao> visitDao;
    private MockedStatic<UserDao> userDao;
    private MockedStatic<NotificationService> notifSvc;

    @BeforeEach
    void setUp() {
        articleDao = mockStatic(ArticleDao.class);
        kbDao = mockStatic(KnowledgeBaseDao.class);
        teamDao = mockStatic(TeamDao.class);
        tmDao = mockStatic(TeamMemberDao.class);
        kbmDao = mockStatic(KnowledgeBaseMemberDao.class);
        commentDao = mockStatic(CommentDao.class);
        likeDao = mockStatic(ArticleLikeDao.class);
        favDao = mockStatic(FavoriteDao.class);
        visitDao = mockStatic(RecentVisitDao.class);
        userDao = mockStatic(UserDao.class);
        notifSvc = mockStatic(NotificationService.class);
    }

    @AfterEach
    void tearDown() {
        notifSvc.close();
        userDao.close();
        visitDao.close();
        favDao.close();
        likeDao.close();
        commentDao.close();
        kbmDao.close();
        tmDao.close();
        teamDao.close();
        kbDao.close();
        articleDao.close();
    }

    private KnowledgeBase createMockKb() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        return kb;
    }

    @Test void createArticle_success() {
        createMockKb();
        Article saved = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.insert(any())).thenAnswer(inv -> {
            ((Article) inv.getArgument(0)).setId(10);
            return null;
        });
        Article result = ArticleService.createArticle(1, "T", "C", 2);
        assertNotNull(result);
        assertEquals("T", result.getTitle());
    }

    @Test void createArticle_kbNotFound() {
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.empty());
        assertThrows(ArticleException.class, () -> ArticleService.createArticle(1, "T", "C", 2));
    }

    @Test void findArticleById_success() {
        Article a = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));
        assertEquals(a, ArticleService.findArticleById(10));
    }

    @Test void findArticleById_notFound() {
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.empty());
        assertThrows(ArticleException.class, () -> ArticleService.findArticleById(10));
    }

    @Test void ensureArticleOwner_success() {
        Article a = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));
        assertDoesNotThrow(() -> ArticleService.ensureArticleOwner(10, 2));
    }

    @Test void ensureArticleOwner_notOwner() {
        Article a = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));
        assertThrows(ArticleException.class, () -> ArticleService.ensureArticleOwner(10, 3));
    }

    @Test void updateArticle_draftToPublished_notifies() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        Article a = new Article(1, "Old", "Old", 3); // authorId=3, operatorId=3
        a.setStatus(0);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));

        ArticleService.updateArticle(10, null, null, null, 1, 3);
        notifSvc.verify(() -> NotificationService.notifyFollowersNewArticle(eq(10), anyString(), eq(3)));
    }

    @Test void updateArticle_contentUpdate_notifies() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        Article a = new Article(1, "Old", "Old", 3); // authorId=3
        a.setStatus(1);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));

        ArticleService.updateArticle(10, null, "NewContent", null, null, 3);
        notifSvc.verify(() -> NotificationService.notifyFollowersArticleUpdated(eq(10), anyString(), eq(3)));
    }

    @Test void deleteArticle_cascade() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        Article a = new Article(1, "T", "C", 3); // authorId=3
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));

        ArticleService.deleteArticle(10, 3);
        commentDao.verify(() -> CommentDao.deleteByArticleId(10));
        likeDao.verify(() -> ArticleLikeDao.deleteByArticleId(10));
        articleDao.verify(() -> ArticleDao.deleteById(10));
    }

    @Test void toVO_basic() {
        Article a = new Article(1, "T", "C", 2);
        a.setStatus(1);
        articleDao.when(() -> ArticleDao.findById(1)).thenReturn(Optional.empty()); // for kb lookup
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(new KnowledgeBase("KB", "d", 1, 0, 1, 1)));
        User author = new User("a", "p", "a@t.com");
        author.setNickname("A");
        userDao.when(() -> UserDao.findById(2)).thenReturn(Optional.of(author));
        
        ArticleVO vo = ArticleService.toVO(a);
        assertEquals("T", vo.getTitle());
        assertEquals("A", vo.getAuthorName());
    }

    @Test void likeArticle_success() {
        Article a = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));
        likeDao.when(() -> ArticleLikeDao.insert(10, 3)).thenReturn(true);
        likeDao.when(() -> ArticleLikeDao.exists(10, 3)).thenReturn(true);
        likeDao.when(() -> ArticleLikeDao.countByArticle(10)).thenReturn(1);

        LikeResultVO result = ArticleService.likeArticle(10, 3);
        assertTrue(result.isLiked());
        assertEquals(1, result.getLikeCount());
    }

    @Test void unlikeArticle() {
        likeDao.when(() -> ArticleLikeDao.countByArticle(10)).thenReturn(0);
        LikeResultVO result = ArticleService.unlikeArticle(10, 3);
        assertFalse(result.isLiked());
        likeDao.verify(() -> ArticleLikeDao.delete(10, 3));
    }

    @Test void addComment_success() {
        Article a = new Article(1, "T", "C", 2);
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(a));
        commentDao.when(() -> CommentDao.insert(any(Comment.class))).thenAnswer(inv -> {
            ((Comment) inv.getArgument(0)).setId(1);
            return null;
        });
        int id = ArticleService.addComment(10, 3, "Nice", null, null);
        assertEquals(1, id);
    }

    @Test void favoriteArticle() {
        ArticleService.favoriteArticle(1, 10);
        favDao.verify(() -> FavoriteDao.insert(1, FavoriteDao.TYPE_ARTICLE, 10));
    }

    @Test void unfavoriteArticle() {
        ArticleService.unfavoriteArticle(1, 10);
        favDao.verify(() -> FavoriteDao.delete(1, FavoriteDao.TYPE_ARTICLE, 10));
    }

    @Test void recordVisit() {
        ArticleService.recordVisit(1, 10);
        visitDao.verify(() -> RecentVisitDao.upsert(1, 10));
    }

    @Test void deleteVisitHistory_specific() {
        ArticleService.deleteVisitHistory(1, 10);
        visitDao.verify(() -> RecentVisitDao.delete(1, 10));
    }

    @Test void deleteVisitHistory_all() {
        ArticleService.deleteVisitHistory(1, null);
        visitDao.verify(() -> RecentVisitDao.deleteAll(1));
    }
}
