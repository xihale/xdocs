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
import top.xihale.xdocs.util.SqlBuilder;
import top.xihale.xdocs.service.NotificationService;
import top.xihale.xdocs.vo.TeamMemberVO;
import top.xihale.xdocs.vo.TeamVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TEAM 业务逻辑层
 */
public class TeamService {

    public static Team createTeam(String name, String description, int ownerId) {
        return SqlBuilder.inTransaction(conn -> {
            Team team = new Team(name, description, ownerId);
            TeamDao.INSTANCE.insert(team);
            // 自动将创建者添加为 OWNER 成员
            TeamMember ownerMember = new TeamMember(
                    team.getId(), ownerId,
                    TeamRole.OWNER.getCode(),
                    JoinStatus.ACCEPTED.getCode(),
                    null
            );
            TeamMemberDao.INSTANCE.insert(ownerMember);
            return team;
        });
    }

    public static Team findTeamById(int id) {
        return TeamDao.INSTANCE.findById(id)
                .orElseThrow(() -> new TeamException(TeamError.TEAM_NOT_FOUND));
    }

    public static List<Team> findTeamsByUserId(int userId) {
        return TeamDao.INSTANCE.findByUserId(userId);
    }

    public static void inviteMember(int teamId, int userId, int inviterId) {
        // 校验 Team 存在
        findTeamById(teamId);
        // 校验操作者是 TEAM 的 OWNER 或 ADMIN
        TeamMember inviter = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, inviterId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (inviter.getRole() != TeamRole.OWNER.getCode()
                && inviter.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.INVITE_REQUIRES_OWNER_OR_ADMIN);
        }
        // 校验目标用户不在 TEAM 中（不存在或已被拒绝）
        TeamMember existing = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId).orElse(null);
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
        TeamMemberDao.INSTANCE.insert(member);

        // 发送邀请通知
        Team team = TeamDao.INSTANCE.findById(teamId).orElse(null);
        NotificationService.notifyTeamInvite(teamId,
                team != null ? team.getName() : "未知团队",
                userId, inviterId);
    }

    public static void acceptInvite(int teamId, int userId) {
        TeamMember member = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (member.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.INVITE_STATUS_NOT_PENDING);
        }
        TeamMemberDao.INSTANCE.updateJoinStatus(teamId, userId, JoinStatus.ACCEPTED.getCode());

        // 通知邀请者
        TeamMember member2 = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId).orElse(null);
        if (member2 != null && member2.getInviteBy() != null) {
            Team team = TeamDao.INSTANCE.findById(teamId).orElse(null);
            String userName = UserDao.INSTANCE.findById(userId)
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                    .orElse("未知用户");
            NotificationService.notifyMemberChange(member2.getInviteBy(),
                    "邀请已接受",
                    userName + " 已接受加入团队「" + (team != null ? team.getName() : "") + "」的邀请",
                    userId);
        }
    }

    public static void rejectInvite(int teamId, int userId) {
        TeamMember member = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (member.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.INVITE_STATUS_NOT_PENDING);
        }
        TeamMemberDao.INSTANCE.updateJoinStatus(teamId, userId, JoinStatus.REJECTED.getCode());
    }

    public static void removeMember(int teamId, int userId, int operatorId) {
        // 校验操作者是 OWNER 或 ADMIN
        TeamMember operator = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()
                && operator.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.REMOVE_REQUIRES_OWNER_OR_ADMIN);
        }
        // 不能移除 OWNER
        TeamMember target = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.CANNOT_REMOVE_OWNER);
        }
        TeamMemberDao.INSTANCE.delete(teamId, userId);

        // 通知被移除的用户
        Team team = TeamDao.INSTANCE.findById(teamId).orElse(null);
        String operatorName = UserDao.INSTANCE.findById(operatorId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知用户");
        NotificationService.notifyMemberChange(userId,
                "已移出团队",
                operatorName + " 将你移出了团队「" + (team != null ? team.getName() : "") + "」",
                operatorId);
    }

    public static void updateMemberRole(int teamId, int userId, int role, int operatorId) {
        // 校验操作者是 OWNER
        TeamMember operator = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.ROLE_CHANGE_REQUIRES_OWNER);
        }
        // 不能修改 OWNER 的角色
        TeamMember target = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == TeamRole.OWNER.getCode()) {
            throw new TeamException(TeamError.CANNOT_CHANGE_OWNER_ROLE);
        }
        TeamMemberDao.INSTANCE.updateRole(teamId, userId, role);
    }

    public static void cancelInvite(int teamId, int userId, int operatorId) {
        // 校验操作者是 OWNER 或 ADMIN
        TeamMember operator = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new TeamException(TeamError.OPERATOR_NOT_IN_TEAM));
        if (operator.getRole() != TeamRole.OWNER.getCode()
                && operator.getRole() != TeamRole.ADMIN.getCode()) {
            throw new TeamException(TeamError.CANCEL_INVITE_REQUIRES_OWNER_OR_ADMIN);
        }
        TeamMember target = TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamError.INVITE_NOT_FOUND));
        if (target.getJoinStatus() != JoinStatus.INVITED.getCode()) {
            throw new TeamException(TeamError.NO_PENDING_INVITE);
        }
        TeamMemberDao.INSTANCE.delete(teamId, userId);
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
        List<TeamMember> members = TeamMemberDao.INSTANCE.findByTeamId(team.getId());
        User owner = UserDao.INSTANCE.findById(team.getOwnerId()).orElse(null);

        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setName(team.getName());
        vo.setDescription(team.getDescription());
        vo.setOwnerId(team.getOwnerId());
        vo.setOwnerName(owner != null ? owner.getNickname() : null);
        vo.setOwnerAvatar(owner != null ? owner.getAvatarUrl() : null);
        vo.setAvatarUrl(team.getAvatarUrl());
        vo.setMemberCount(members.size());
        vo.setCreateTime(team.getCreateTime());
        vo.setUpdateTime(team.getUpdateTime());
        return vo;
    }

    public static List<TeamMemberVO> buildMemberVOList(int teamId) {
        List<TeamMember> members = TeamMemberDao.INSTANCE.findByTeamId(teamId);
        List<TeamMemberVO> accepted = new ArrayList<>();
        List<TeamMemberVO> invited = new ArrayList<>();
        for (TeamMember m : members) {
            User user = UserDao.INSTANCE.findById(m.getUserId()).orElse(null);
            TeamMemberVO vo = new TeamMemberVO(
                    m.getId(),
                    m.getTeamId(),
                    m.getUserId(),
                    user != null ? user.getUsername() : null,
                    user != null ? user.getNickname() : null,
                    user != null ? user.getAvatarUrl() : null,
                    m.getRole(),
                    TeamRole.fromCode(m.getRole()).name(),
                    m.getJoinStatus(),
                    m.getJoinTime()
            );
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

    public static List<Map<String, Object>> buildPendingInviteList(int userId) {
        List<TeamMember> pending = TeamMemberDao.INSTANCE.findPendingByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamMember member : pending) {
            Team team = TeamDao.INSTANCE.findById(member.getTeamId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", member.getId());
            item.put("teamId", member.getTeamId());
            item.put("teamName", team != null ? team.getName() : null);
            item.put("joinStatus", member.getJoinStatus());
            item.put("joinTime", member.getJoinTime());
            result.add(item);
        }
        return result;
    }
}
