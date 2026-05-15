package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.constant.JoinStatus;
import top.xihale.xdocs.constant.TeamRole;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.exception.TeamException;
import top.xihale.xdocs.po.*;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeamServiceTest {

    private MockedStatic<TeamDao> teamDao;
    private MockedStatic<TeamMemberDao> tmDao;
    private MockedStatic<UserDao> userDao;
    private MockedStatic<NotificationService> notifSvc;
    private MockedStatic<Db> db;

    @BeforeEach
    void setUp() {
        teamDao = mockStatic(TeamDao.class);
        tmDao = mockStatic(TeamMemberDao.class);
        userDao = mockStatic(UserDao.class);
        notifSvc = mockStatic(NotificationService.class);
        db = mockStatic(Db.class);
        db.when(() -> Db.inTransaction(any(Db.TransactionAction.class)))
                .thenAnswer(inv -> ((Db.TransactionAction<?>) inv.getArgument(0)).execute(null));
    }

    @AfterEach
    void tearDown() {
        db.close();
        notifSvc.close();
        userDao.close();
        tmDao.close();
        teamDao.close();
    }

    @Test void createTeam_success() {
        Team t = new Team("T", "d", 1);
        teamDao.when(() -> TeamDao.insert(any(Team.class))).thenAnswer(inv -> {
            ((Team) inv.getArgument(0)).setId(10);
            return null;
        });
        Team result = TeamService.createTeam("T", "d", 1);
        assertNotNull(result);
        tmDao.verify(() -> TeamMemberDao.insert(any(TeamMember.class)));
    }

    @Test void findTeamById_success() {
        Team t = new Team("T", "d", 1);
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.of(t));
        assertEquals(t, TeamService.findTeamById(1));
    }

    @Test void findTeamById_notFound() {
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.empty());
        assertThrows(TeamException.class, () -> TeamService.findTeamById(1));
    }

    @Test void findTeamsByUserId() {
        teamDao.when(() -> TeamDao.findByUserId(1)).thenReturn(List.of(new Team("T", "d", 1)));
        assertEquals(1, TeamService.findTeamsByUserId(1).size());
    }

    @Test void inviteMember_success() {
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.of(new Team("T", "d", 1)));
        TeamMember inviter = new TeamMember(1, 2, TeamRole.OWNER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(inviter));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> TeamService.inviteMember(1, 3, 2));
        tmDao.verify(() -> TeamMemberDao.insert(any(TeamMember.class)));
    }

    @Test void inviteMember_operatorNotInTeam() {
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.of(new Team("T", "d", 1)));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.empty());
        assertThrows(TeamException.class, () -> TeamService.inviteMember(1, 3, 2));
    }

    @Test void inviteMember_requiresOwnerOrAdmin() {
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.of(new Team("T", "d", 1)));
        TeamMember inviter = new TeamMember(1, 2, TeamRole.MEMBER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(inviter));
        assertThrows(TeamException.class, () -> TeamService.inviteMember(1, 3, 2));
    }

    @Test void inviteMember_userAlreadyInTeam() {
        teamDao.when(() -> TeamDao.findById(1)).thenReturn(Optional.of(new Team("T", "d", 1)));
        TeamMember inviter = new TeamMember(1, 2, TeamRole.OWNER.getCode(), 1, null);
        TeamMember existing = new TeamMember(1, 3, TeamRole.MEMBER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(inviter));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(existing));
        assertThrows(TeamException.class, () -> TeamService.inviteMember(1, 3, 2));
    }

    @Test void acceptInvite_success() {
        TeamMember member = new TeamMember(1, 3, TeamRole.MEMBER.getCode(), JoinStatus.INVITED.getCode(), 2);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(member));
        assertDoesNotThrow(() -> TeamService.acceptInvite(1, 3));
        tmDao.verify(() -> TeamMemberDao.updateJoinStatus(1, 3, JoinStatus.ACCEPTED.getCode()));
    }

    @Test void acceptInvite_notFound() {
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.empty());
        assertThrows(TeamException.class, () -> TeamService.acceptInvite(1, 3));
    }

    @Test void rejectInvite_success() {
        TeamMember member = new TeamMember(1, 3, 2, JoinStatus.INVITED.getCode(), null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(member));
        TeamService.rejectInvite(1, 3);
        tmDao.verify(() -> TeamMemberDao.updateJoinStatus(1, 3, JoinStatus.REJECTED.getCode()));
    }

    @Test void removeMember_success() {
        TeamMember operator = new TeamMember(1, 2, TeamRole.OWNER.getCode(), 1, null);
        TeamMember target = new TeamMember(1, 3, TeamRole.MEMBER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(operator));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(target));
        assertDoesNotThrow(() -> TeamService.removeMember(1, 3, 2));
        tmDao.verify(() -> TeamMemberDao.delete(1, 3));
    }

    @Test void removeMember_cannotRemoveOwner() {
        TeamMember operator = new TeamMember(1, 2, TeamRole.ADMIN.getCode(), 1, null);
        TeamMember owner = new TeamMember(1, 3, TeamRole.OWNER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(operator));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(owner));
        assertThrows(TeamException.class, () -> TeamService.removeMember(1, 3, 2));
    }

    @Test void updateMemberRole_success() {
        TeamMember operator = new TeamMember(1, 2, TeamRole.OWNER.getCode(), 1, null);
        TeamMember target = new TeamMember(1, 3, TeamRole.MEMBER.getCode(), 1, null);
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 2)).thenReturn(Optional.of(operator));
        tmDao.when(() -> TeamMemberDao.findByTeamIdAndUserId(1, 3)).thenReturn(Optional.of(target));
        TeamService.updateMemberRole(1, 3, TeamRole.ADMIN.getCode(), 2);
        tmDao.verify(() -> TeamMemberDao.updateRole(1, 3, TeamRole.ADMIN.getCode()));
    }
}
