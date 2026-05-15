package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.*;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.exception.KbException;
import top.xihale.xdocs.po.*;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeBaseServiceTest {

    private MockedStatic<KnowledgeBaseDao> kbDao;
    private MockedStatic<KnowledgeBaseMemberDao> kbmDao;
    private MockedStatic<TeamMemberDao> tmDao;
    private MockedStatic<TeamDao> teamDao;
    private MockedStatic<ArticleDao> articleDao;
    private MockedStatic<UserDao> userDao;
    private MockedStatic<NotificationService> notifSvc;
    private MockedStatic<ArticleService> articleSvc;
    private MockedStatic<Db> db;

    @BeforeEach
    void setUp() {
        kbDao = mockStatic(KnowledgeBaseDao.class);
        kbmDao = mockStatic(KnowledgeBaseMemberDao.class);
        tmDao = mockStatic(TeamMemberDao.class);
        teamDao = mockStatic(TeamDao.class);
        articleDao = mockStatic(ArticleDao.class);
        userDao = mockStatic(UserDao.class);
        notifSvc = mockStatic(NotificationService.class);
        articleSvc = mockStatic(ArticleService.class);
        db = mockStatic(Db.class);
        db.when(() -> Db.inTransaction(any(Db.TransactionAction.class)))
                .thenAnswer(inv -> ((Db.TransactionAction<?>) inv.getArgument(0)).execute(null));
    }

    @AfterEach
    void tearDown() {
        db.close();
        articleSvc.close();
        notifSvc.close();
        userDao.close();
        articleDao.close();
        teamDao.close();
        tmDao.close();
        kbmDao.close();
        kbDao.close();
    }

    @Test void createKnowledgeBase_userOwner() {
        kbDao.when(() -> KnowledgeBaseDao.insert(any())).thenAnswer(inv -> {
            ((KnowledgeBase) inv.getArgument(0)).setId(10);
            return null;
        });
        KnowledgeBase kb = KnowledgeBaseService.createKnowledgeBase("KB", "d", 1, OwnerType.USER.getCode(), 1, 1);
        assertNotNull(kb);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.insert(any()));
    }

    @Test void createKnowledgeBase_teamOwner_notMember() {
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2))
                .thenReturn(Optional.empty());
        assertThrows(KbException.class,
                () -> KnowledgeBaseService.createKnowledgeBase("KB", "d", 1, OwnerType.TEAM.getCode(), 1, 2));
    }

    @Test void createKnowledgeBase_teamOwner_success() {
        TeamMember member = new TeamMember(1, 2, 2, JoinStatus.ACCEPTED.getCode(), null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(member));
        kbDao.when(() -> KnowledgeBaseDao.insert(any())).thenAnswer(inv -> {
            ((KnowledgeBase) inv.getArgument(0)).setId(10);
            return null;
        });
        KnowledgeBase kb = KnowledgeBaseService.createKnowledgeBase("KB", "d", 1, OwnerType.TEAM.getCode(), 1, 2);
        assertNotNull(kb);
    }

    @Test void findKnowledgeBaseById_success() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        assertEquals(kb, KnowledgeBaseService.findKnowledgeBaseById(1));
    }

    @Test void findKnowledgeBaseById_notFound() {
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.empty());
        assertThrows(KbException.class, () -> KnowledgeBaseService.findKnowledgeBaseById(1));
    }

    @Test void authorizeMember_newInvite() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        KnowledgeBaseMember op = new KnowledgeBaseMember(1, 2, KnowledgeBaseRole.OWNER.getCode(), 1, 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 2)).thenReturn(Optional.of(op));
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 3)).thenReturn(Optional.empty());
        KnowledgeBaseService.authorizeMember(1, 3, KnowledgeBaseRole.EDITOR.getCode(), 2);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.insert(any()));
    }

    @Test void authorizeMember_updateExisting() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        KnowledgeBaseMember op = new KnowledgeBaseMember(1, 2, KnowledgeBaseRole.OWNER.getCode(), 1, 2);
        KnowledgeBaseMember existing = new KnowledgeBaseMember(1, 3, KnowledgeBaseRole.VIEWER.getCode(), 1, 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 2)).thenReturn(Optional.of(op));
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 3)).thenReturn(Optional.of(existing));
        KnowledgeBaseService.authorizeMember(1, 3, KnowledgeBaseRole.EDITOR.getCode(), 2);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.updateRole(eq(1), eq(3), eq(KnowledgeBaseRole.EDITOR.getCode())));
    }

    @Test void removeMember_success() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        KnowledgeBaseMember op = new KnowledgeBaseMember(1, 2, KnowledgeBaseRole.OWNER.getCode(), 1, 2);
        KnowledgeBaseMember target = new KnowledgeBaseMember(1, 3, KnowledgeBaseRole.VIEWER.getCode(), 1, 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 2)).thenReturn(Optional.of(op));
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 3)).thenReturn(Optional.of(target));
        KnowledgeBaseService.removeMember(1, 3, 2);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.delete(1, 3));
    }

    @Test void removeMember_cannotRemoveOwner() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        KnowledgeBaseMember op = new KnowledgeBaseMember(1, 2, KnowledgeBaseRole.ADMIN.getCode(), 1, 2);
        KnowledgeBaseMember owner = new KnowledgeBaseMember(1, 3, KnowledgeBaseRole.OWNER.getCode(), 1, 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 2)).thenReturn(Optional.of(op));
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 3)).thenReturn(Optional.of(owner));
        assertThrows(KbException.class, () -> KnowledgeBaseService.removeMember(1, 3, 2));
    }

    @Test void acceptInvite_success() {
        KnowledgeBaseMember member = new KnowledgeBaseMember(1, 3, 3, JoinStatus.INVITED.getCode(), 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 3)).thenReturn(Optional.of(member));
        KnowledgeBaseService.acceptInvite(1, 3);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.updateInviteStatus(1, 3, JoinStatus.ACCEPTED.getCode()));
    }

    @Test void deleteKnowledgeBase_cascade() {
        KnowledgeBase kb = new KnowledgeBase("KB", "d", 1, 0, 1, 1);
        kbDao.when(() -> KnowledgeBaseDao.findById(1)).thenReturn(Optional.of(kb));
        KnowledgeBaseMember op = new KnowledgeBaseMember(1, 2, KnowledgeBaseRole.OWNER.getCode(), 1, 2);
        kbmDao.when(() -> KnowledgeBaseMemberDao.findByKbIdAndUserId(1, 2)).thenReturn(Optional.of(op));
        articleDao.when(() -> ArticleDao.findByKnowledgeBaseId(1)).thenReturn(List.of());

        KnowledgeBaseService.deleteKnowledgeBase(1, 2);
        kbmDao.verify(() -> KnowledgeBaseMemberDao.deleteByKbId(1));
        kbDao.verify(() -> KnowledgeBaseDao.deleteById(1));
    }
}
