package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.JoinStatus;
import top.xihale.xdocs.constant.TeamRole;
import top.xihale.xdocs.dao.TeamDao;
import top.xihale.xdocs.dao.TeamMemberDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.exception.TeamException;
import top.xihale.xdocs.exception.TeamException.TeamError;
import top.xihale.xdocs.po.Team;
import top.xihale.xdocs.po.TeamMember;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.Db;
import top.xihale.xdocs.vo.TeamMemberVO;
import top.xihale.xdocs.vo.TeamPendingInviteVO;
import top.xihale.xdocs.vo.TeamVO;

import java.util.ArrayList;
import java.util.List;

/**
 * TEAM 业务逻辑层
 */
public class TeamService {

    public static Team createTeam(String name, String description, int ownerId) {
        return Db.inTransaction(conn -> {
            Team team = new Team(name, description, ownerId);
            TeamDao.insert(team);
            // 自动将创建者添加为 OWNER 成员
            TeamMember ownerMember = new TeamMember(
                    team.getId(), ownerId,
                    TeamRole.OWNER.getCode(),
                    JoinStatus.ACCEPTED.getCode(),
                    null
            );
            TeamMemberDao.insert(ownerMember);
            return team;
        });
    }

    public static Team findTeamById(int id) {
        return TeamDao.findById(id)
                .orElseThrow(() -> new TeamException(TeamError.TEAM_NOT_FOUND));
    }

    public static List<Team> findTeamsByUserId(int userId) {
        return TeamDao.findByUserId(userId);
    }

    public static void inviteMember(int teamId, int userId, int inviterId) {
        // 校验 Team 存在
        findTeamById(teamId);
        // 校验操作者是 TEAM 的 OWNER 或 ADMIN
        TeamMember inviter = TeamMemberDao.findByTeamIdAndUserId(teamId, inviterId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (inviter.getRole() != TeamRole.OWNER.getCode()
                && inviter.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.INVITE_REQUIRES_OWNER_OR_ADMIN);
        }
        // 校验目标用户不在 TEAM 中（不存在或已被拒绝）
        TeamMember existing = TeamMemberDao.findByTeamIdAndUserId(teamId, userId).orElse(null);
        if (existing != null && existing.getJoinStatus() != JoinStatus.REJECTED.getCode()) {
            throw new TeamException(TeamError.USER_ALREADY_IN_TEAM);
        }
        // 创建邀请记录
        TeamMember member = new TeamMember(
                teamId, userId,
                TeamRole.MEMBER.getCode(),
                JoinStatus.INVITED.getCode(),
                inviterId
        );
        TeamMemberDao.insert(member);

        // 发送邀请通知
        Team team = TeamDao.findById(teamId).orElse(null);
        NotificationService.notifyTeamInvite(teamId,
                team != null ? team.getName() : "未知团队",
                userId, inviterId);
    }

    public static void acceptInvite(int teamId, int userId) {
        TeamMember member = TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (member.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.INVITE_STATUS_NOT_PENDING);
        }
        TeamMemberDao.updateJoinStatus(teamId, userId, JoinStatus.ACCEPTED.getCode());

        // 通知邀请者
        TeamMember member2 = TeamMemberDao.findByTeamIdAndUserId(teamId, userId).orElse(null);
        if (member2 != null && member2.getInviteBy() != null) {
            Team team = TeamDao.findById(teamId).orElse(null);
            String userName = UserDao.findById(userId)
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                    .orElse("未知用户");
            NotificationService.notifyMemberChange(member2.getInviteBy(),
                    "邀请已接受",
                    userName + " 已接受加入团队「" + (team != null ? team.getName() : "") + "」的邀请",
                    userId);
        }
    }

    public static void rejectInvite(int teamId, int userId) {
        TeamMember member = TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (member.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.INVITE_STATUS_NOT_PENDING);
        }
        TeamMemberDao.updateJoinStatus(teamId, userId, JoinStatus.REJECTED.getCode());
    }

    public static void removeMember(int teamId, int userId, int operatorId) {
        // 校验操作者是 OWNER 或 ADMIN
        TeamMember operator = TeamMemberDao.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()
                && operator.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.REMOVE_REQUIRES_OWNER_OR_ADMIN);
        }
        // 不能移除 OWNER
        TeamMember target = TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.CANNOT_REMOVE_OWNER);
        }
        TeamMemberDao.delete(teamId, userId);

        // 通知被移除的用户
        Team team = TeamDao.findById(teamId).orElse(null);
        String operatorName = UserDao.findById(operatorId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知用户");
        NotificationService.notifyMemberChange(userId,
                "已移出团队",
                operatorName + " 将你移出了团队「" + (team != null ? team.getName() : "") + "」",
                operatorId);
    }

    public static void updateMemberRole(int teamId, int userId, int role, int operatorId) {
        // 校验操作者是 OWNER
        TeamMember operator = TeamMemberDao.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.ROLE_CHANGE_REQUIRES_OWNER);
        }
        // 不能修改 OWNER 的角色
        TeamMember target = TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.CANNOT_CHANGE_OWNER_ROLE);
        }
        TeamMemberDao.updateRole(teamId, userId, role);
    }

    public static void cancelInvite(int teamId, int userId, int operatorId) {
        // 校验操作者是 OWNER 或 ADMIN
        TeamMember operator = TeamMemberDao.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()
                && operator.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.CANCEL_INVITE_REQUIRES_OWNER_OR_ADMIN);
        }
        TeamMember target = TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (target.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.NO_PENDING_INVITE);
        }
        TeamMemberDao.delete(teamId, userId);
    }

    // ==================== 查询辅助 ====================

    public static List<TeamVO> buildTeamVOList(List<Team> teams) {
        List<TeamVO> voList = new ArrayList<>();
        for (Team team : teams) {
            voList.add(buildTeamVO(team));
        }
        return voList;
    }

    public static TeamVO buildTeamVO(Team team) {
        List<TeamMember> members = TeamMemberDao.findByTeamId(team.getId());
        User owner = UserDao.findById(team.getOwnerId()).orElse(null);

        return TeamVO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .ownerId(team.getOwnerId())
                .ownerName(owner != null ? owner.getNickname() : null)
                .ownerAvatar(owner != null ? owner.getAvatarUrl() : null)
                .avatarUrl(team.getAvatarUrl())
                .memberCount(members.size())
                .createTime(team.getCreateTime())
                .updateTime(team.getUpdateTime())
                .build();
    }

    public static List<TeamMemberVO> buildMemberVOList(int teamId) {
        List<TeamMember> members = TeamMemberDao.findByTeamId(teamId);
        List<TeamMemberVO> accepted = new ArrayList<>();
        List<TeamMemberVO> invited = new ArrayList<>();
        for (TeamMember m : members) {
            User user = UserDao.findById(m.getUserId()).orElse(null);
            TeamMemberVO vo = TeamMemberVO.builder()
                    .id(m.getId())
                    .teamId(m.getTeamId())
                    .userId(m.getUserId())
                    .username(user != null ? user.getUsername() : null)
                    .nickname(user != null ? user.getNickname() : null)
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .role(m.getRole())
                    .roleName(TeamRole.fromCode(m.getRole()).name())
                    .joinStatus(m.getJoinStatus())
                    .joinTime(m.getJoinTime())
                    .build();
            if (m.getJoinStatus() == JoinStatus.ACCEPTED.getCode()) {
                accepted.add(vo);
            } else if (m.getJoinStatus() == JoinStatus.INVITED.getCode()) {
                invited.add(vo);
            }
        }
        // 已接受在前，待邀请在后
        List<TeamMemberVO> result = new ArrayList<>(accepted);
        result.addAll(invited);
        return result;
    }

    public static List<TeamPendingInviteVO> buildPendingInviteList(int userId) {
        List<TeamMember> pending = TeamMemberDao.findPendingByUserId(userId);
        List<TeamPendingInviteVO> result = new ArrayList<>();
        for (TeamMember member : pending) {
            Team team = TeamDao.findById(member.getTeamId()).orElse(null);
            result.add(TeamPendingInviteVO.builder()
                    .id(member.getId())
                    .teamId(member.getTeamId())
                    .teamName(team != null ? team.getName() : null)
                    .joinStatus(member.getJoinStatus())
                    .joinTime(member.getJoinTime())
                    .build());
        }
        return result;
    }
}
