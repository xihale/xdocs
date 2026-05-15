package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.NotificationType;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.po.*;
import top.xihale.xdocs.vo.NotificationVO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private MockedStatic<NotificationDao> notifDao;
    private MockedStatic<UserDao> userDao;
    private MockedStatic<TeamMemberDao> tmDao;
    private MockedStatic<FollowUserDao> followDao;
    private MockedStatic<ArticleDao> articleDao;
    private MockedStatic<KnowledgeBaseDao> kbDao;
    private MockedStatic<KnowledgeBaseMemberDao> kbmDao;
    private MockedStatic<TeamDao> teamDao;

    @BeforeEach
    void setUp() {
        notifDao = mockStatic(NotificationDao.class);
        userDao = mockStatic(UserDao.class);
        tmDao = mockStatic(TeamMemberDao.class);
        followDao = mockStatic(FollowUserDao.class);
        articleDao = mockStatic(ArticleDao.class);
        kbDao = mockStatic(KnowledgeBaseDao.class);
        kbmDao = mockStatic(KnowledgeBaseMemberDao.class);
        teamDao = mockStatic(TeamDao.class);
    }

    @AfterEach
    void tearDown() {
        teamDao.close();
        kbmDao.close();
        kbDao.close();
        articleDao.close();
        followDao.close();
        tmDao.close();
        userDao.close();
        notifDao.close();
    }

    @Test void send_insertsNotification() {
        NotificationService.send(1, NotificationType.LIKE, "T", "C", "/l", 2);
        notifDao.verify(() -> NotificationDao.insert(any(Notification.class)));
    }

    @Test void notifyTeamInvite() {
        User inviter = new User("u", "p", "u@t.com");
        inviter.setNickname("Alice");
        userDao.when(() -> UserDao.findById(2)).thenReturn(Optional.of(inviter));
        NotificationService.notifyTeamInvite(1, "TeamA", 3, 2);
        notifDao.verify(() -> NotificationDao.insert(any(Notification.class)));
    }

    @Test void notifyKbInvite() {
        User inviter = new User("u", "p", "u@t.com");
        inviter.setNickname("Bob");
        userDao.when(() -> UserDao.findById(2)).thenReturn(Optional.of(inviter));
        NotificationService.notifyKbInvite(1, "KB", 3, 2);
        notifDao.verify(() -> NotificationDao.insert(any()));
    }

    @Test void notifyComment_skipsSelfComment() {
        NotificationService.notifyComment(1, "A", 2, 2); // authorId == commenterId
        notifDao.verify(() -> NotificationDao.insert(any()), never());
    }

    @Test void notifyComment_sendsNotification() {
        User commenter = new User("c", "p", "c@t.com");
        commenter.setNickname("C");
        userDao.when(() -> UserDao.findById(3)).thenReturn(Optional.of(commenter));
        NotificationService.notifyComment(1, "A", 2, 3);
        notifDao.verify(() -> NotificationDao.insert(any()));
    }

    @Test void notifyLike_skipsSelfLike() {
        NotificationService.notifyLike(1, "A", 2, 2);
        notifDao.verify(() -> NotificationDao.insert(any()), never());
    }

    @Test void notifyFollow() {
        User follower = new User("f", "p", "f@t.com");
        follower.setNickname("F");
        userDao.when(() -> UserDao.findById(2)).thenReturn(Optional.of(follower));
        NotificationService.notifyFollow(3, 2);
        notifDao.verify(() -> NotificationDao.insert(any()));
    }

    @Test void notifyFollowersNewArticle_publicKb() {
        Article article = new Article(1, "A", "C", 2);
        article.setId(10);
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1); // visibility=PUBLIC
        User author = new User("a", "p", "a@t.com");
        author.setNickname("A");

        userDao.when(() -> UserDao.findById(2)).thenReturn(Optional.of(author));
        followDao.when(() -> FollowUserDao.findFollowerIds(2)).thenReturn(List.of(3));
        articleDao.when(() -> ArticleDao.findById(10)).thenReturn(Optional.of(article));
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));

        NotificationService.notifyFollowersNewArticle(10, "A", 2);
        notifDao.verify(() -> NotificationDao.insert(any()), atLeastOnce());
    }

    @Test void listNotifications() {
        Notification n = new Notification(1, 0, "T", "C", null, null);
        notifDao.when(() -> NotificationDao.findByUserId(1, 0, 10)).thenReturn(List.of(n));
        List<NotificationVO> result = NotificationService.listNotifications(1, 0, 10);
        assertFalse(result.isEmpty());
    }

    @Test void unreadCount() {
        notifDao.when(() -> NotificationDao.countUnread(1)).thenReturn(5);
        assertEquals(5, NotificationService.unreadCount(1));
    }

    @Test void markRead() {
        NotificationService.markRead(1, 2);
        notifDao.verify(() -> NotificationDao.markRead(1, 2));
    }

    @Test void markAllRead() {
        NotificationService.markAllRead(1);
        notifDao.verify(() -> NotificationDao.markAllRead(1));
    }

    @Test void deleteNotification() {
        NotificationService.deleteNotification(1, 2);
        notifDao.verify(() -> NotificationDao.delete(1, 2));
    }
}
