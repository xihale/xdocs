package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.JoinStatus;
import top.xihale.xdocs.constant.KnowledgeBaseRole;
import top.xihale.xdocs.constant.OwnerType;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.exception.KbException;
import top.xihale.xdocs.exception.KbException.KbError;
import top.xihale.xdocs.po.KnowledgeBase;
import top.xihale.xdocs.po.KnowledgeBaseMember;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.util.Db;
import top.xihale.xdocs.vo.KbMemberVO;
import top.xihale.xdocs.vo.KbPendingInviteVO;
import top.xihale.xdocs.vo.KnowledgeBaseVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库业务逻辑层
 */
public class KnowledgeBaseService {

    public static KnowledgeBase createKnowledgeBase(String name, String description, int visibility, int ownerType, int ownerId, int creatorId) {
        // 创建 TEAM 知识库时，校验操作者是 TEAM 成员
        if (ownerType == OwnerType.TEAM.getCode()) {
            TeamMemberDao.findByTeamIdAndUserId(ownerId, creatorId)
                    .filter(m -> m.getJoinStatus() == JoinStatus.ACCEPTED.getCode())
                    .orElseThrow(() -> new KbException(KbError.NOT_TEAM_MEMBER));
        }

        return Db.inTransaction(conn -> {
            KnowledgeBase kb = new KnowledgeBase(name, description, visibility, ownerType, ownerId, creatorId);
            KnowledgeBaseDao.insert(kb);

            // 自动将创建者添加为 OWNER 成员
            KnowledgeBaseMember member = new KnowledgeBaseMember(
                    kb.getId(), creatorId,
                    KnowledgeBaseRole.OWNER.getCode(),
                    JoinStatus.ACCEPTED.getCode(),
                    creatorId
            );
            KnowledgeBaseMemberDao.insert(member);

            return kb;
        });
    }

    public static KnowledgeBase findKnowledgeBaseById(int id) {
        return KnowledgeBaseDao.findById(id)
                .orElseThrow(() -> new KbException(KbError.KB_NOT_FOUND));
    }

    public static List<KnowledgeBase> findByOwner(int ownerType, int ownerId) {
        return KnowledgeBaseDao.findByOwnerId(ownerType, ownerId);
    }

    public static void updateKnowledgeBase(int kbId, String name, String description, int operatorId) {
        ensureKbPermission(kbId, operatorId, KnowledgeBaseRole.OWNER, KnowledgeBaseRole.ADMIN);
        KnowledgeBase kb = findKnowledgeBaseById(kbId);
        if (name != null) kb.setName(name);
        if (description != null) kb.setDescription(description);
        KnowledgeBaseDao.update(kb);
    }

    public static void deleteKnowledgeBase(int kbId, int operatorId) {
        ensureKbPermission(kbId, operatorId, KnowledgeBaseRole.OWNER);
        // 级联删除：成员 → 文章（含评论、点赞、收藏、浏览记录） → 知识库
        KnowledgeBaseMemberDao.deleteByKbId(kbId);
        var articles = ArticleDao.findByKnowledgeBaseId(kbId);
        for (var article : articles) {
            ArticleService.deleteArticleData(article.getId());
        }
        KnowledgeBaseDao.deleteById(kbId);
    }

    private static void ensureKbPermission(int kbId, int userId, KnowledgeBaseRole... requiredRoles) {
        findKnowledgeBaseById(kbId);
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        for (KnowledgeBaseRole required : requiredRoles) {
            if (member.getRole() == required.getCode()) return;
        }
        throw new KbException(KbError.KB_PERMISSION_DENIED);
    }

    public static void authorizeMember(int kbId, int userId, int role, int inviterId) {
        // 校验知识库存在
        findKnowledgeBaseById(kbId);

        // 校验操作者是知识库的 OWNER 或 ADMIN
        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, inviterId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.AUTHORIZE_REQUIRES_OWNER_OR_ADMIN);
        }

        // 查看是否已有成员记录
        KnowledgeBaseMember existing = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId).orElse(null);

        if (existing != null) {
            // 已有成员记录，更新角色
            KnowledgeBaseMemberDao.updateRole(kbId, userId, role);
        } else {
            // 没有成员记录，创建新成员
            KnowledgeBaseMember member = new KnowledgeBaseMember(
                    kbId, userId, role,
                    JoinStatus.INVITED.getCode(),
                    inviterId
            );
            KnowledgeBaseMemberDao.insert(member);

            // 发送知识库邀请通知
            var kb = KnowledgeBaseDao.findById(kbId).orElse(null);
            NotificationService.notifyKbInvite(kbId,
                    kb != null ? kb.getName() : "未知知识库",
                    userId, inviterId);
        }
    }

    /**
     * 移除知识库成员
     */
    public static void removeMember(int kbId, int userId, int operatorId) {
        findKnowledgeBaseById(kbId);

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.REMOVE_REQUIRES_OWNER_OR_ADMIN);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.TARGET_NOT_KB_MEMBER));
        if (target.getRole() == KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.CANNOT_REMOVE_KB_OWNER);
        }
        KnowledgeBaseMemberDao.delete(kbId, userId);

        // 通知被移除的用户
        var kb = KnowledgeBaseDao.findById(kbId).orElse(null);
        String operatorName = UserDao.findById(operatorId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知用户");
        NotificationService.notifyMemberChange(userId,
                "已移出知识库",
                operatorName + " 将你移出了知识库「" + (kb != null ? kb.getName() : "") + "」",
                operatorId);
    }

    /**
     * 接受知识库邀请
     */
    public static void acceptInvite(int kbId, int userId) {
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (member.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.KB_INVITE_STATUS_NOT_PENDING);
        }
        KnowledgeBaseMemberDao.updateInviteStatus(kbId, userId, JoinStatus.ACCEPTED.getCode());

        // 通知邀请者
        if (member.getInviteBy() != null) {
            var kb = KnowledgeBaseDao.findById(kbId).orElse(null);
            String userName = UserDao.findById(userId)
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                    .orElse("未知用户");
            NotificationService.notifyMemberChange(member.getInviteBy(),
                    "邀请已接受",
                    userName + " 已接受加入知识库「" + (kb != null ? kb.getName() : "") + "」的邀请",
                    userId);
        }
    }

    /**
     * 拒绝知识库邀请
     */
    public static void rejectInvite(int kbId, int userId) {
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (member.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.KB_INVITE_STATUS_NOT_PENDING);
        }
        KnowledgeBaseMemberDao.updateInviteStatus(kbId, userId, JoinStatus.REJECTED.getCode());
    }

    /**
     * 取消知识库邀请
     */
    public static void cancelInvite(int kbId, int userId, int operatorId) {
        findKnowledgeBaseById(kbId);

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.CANCEL_KB_INVITE_REQUIRES_OWNER_OR_ADMIN);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (target.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.NO_PENDING_KB_INVITE);
        }
        KnowledgeBaseMemberDao.delete(kbId, userId);
    }

    /**
     * 修改知识库成员角色
     */
    public static void updateMemberRole(int kbId, int userId, int role, int operatorId) {
        findKnowledgeBaseById(kbId);

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.KB_ROLE_CHANGE_REQUIRES_OWNER);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.CANNOT_CHANGE_KB_OWNER_ROLE);
        }
        KnowledgeBaseMemberDao.updateRole(kbId, userId, role);
    }

    // ==================== VO 转换 ====================

    public static KnowledgeBaseVO toVO(KnowledgeBase kb) {
        User creator = UserDao.findById(kb.getCreatorId()).orElse(null);
        int articleCount = ArticleDao.findByKnowledgeBaseId(kb.getId()).size();

        // ownerName
        String ownerName;
        if (kb.getOwnerType() == OwnerType.TEAM.getCode()) {
            var team = TeamDao.findById(kb.getOwnerId()).orElse(null);
            ownerName = team != null ? team.getName() : null;
        } else {
            User owner = UserDao.findById(kb.getOwnerId()).orElse(null);
            ownerName = owner != null ? (owner.getNickname() != null ? owner.getNickname() : owner.getUsername()) : null;
        }

        String creatorName = creator != null ? (creator.getNickname() != null ? creator.getNickname() : creator.getUsername()) : null;

        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .visibility(kb.getVisibility())
                .ownerType(kb.getOwnerType())
                .ownerId(kb.getOwnerId())
                .ownerName(ownerName)
                .creatorId(kb.getCreatorId())
                .creatorName(creatorName)
                .articleCount(articleCount)
                .createTime(kb.getCreateTime())
                .updateTime(kb.getUpdateTime())
                .build();
    }

    public static List<KnowledgeBaseVO> toVOList(List<KnowledgeBase> list) {
        List<KnowledgeBaseVO> voList = new ArrayList<>();
        for (KnowledgeBase kb : list) {
            voList.add(toVO(kb));
        }
        return voList;
    }

    // ==================== 查询辅助 ====================

    public static List<KbMemberVO> buildMemberVOList(int kbId) {
        var members = KnowledgeBaseMemberDao.findByKbId(kbId);
        List<KbMemberVO> accepted = new ArrayList<>();
        List<KbMemberVO> invited = new ArrayList<>();
        for (var m : members) {
            if (m.getInviteStatus() == JoinStatus.REJECTED.getCode()) continue;
            var user = UserDao.findById(m.getUserId()).orElse(null);
            KbMemberVO vo = KbMemberVO.builder()
                    .id(m.getId())
                    .kbId(m.getKnowledgeBaseId())
                    .userId(m.getUserId())
                    .username(user != null ? user.getUsername() : null)
                    .nickname(user != null ? user.getNickname() : null)
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .role(m.getRole())
                    .roleName(KnowledgeBaseRole.fromCode(m.getRole()).name())
                    .inviteStatus(m.getInviteStatus())
                    .joinTime(m.getJoinTime())
                    .build();
            if (m.getInviteStatus() == JoinStatus.ACCEPTED.getCode()) {
                accepted.add(vo);
            } else if (m.getInviteStatus() == JoinStatus.INVITED.getCode()) {
                invited.add(vo);
            }
        }
        // 已接受在前，待邀请在后
        List<KbMemberVO> result = new ArrayList<>(accepted);
        result.addAll(invited);
        return result;
    }

    public static List<KbPendingInviteVO> buildPendingInviteList(int userId) {
        List<KnowledgeBaseMember> pending = KnowledgeBaseMemberDao.findPendingByUserId(userId);
        List<KbPendingInviteVO> result = new ArrayList<>();
        for (KnowledgeBaseMember member : pending) {
            var kb = KnowledgeBaseDao.findById(member.getKnowledgeBaseId()).orElse(null);
            result.add(KbPendingInviteVO.builder()
                    .id(member.getId())
                    .kbId(member.getKnowledgeBaseId())
                    .kbName(kb != null ? kb.getName() : null)
                    .role(member.getRole())
                    .inviteStatus(member.getInviteStatus())
                    .joinTime(member.getJoinTime())
                    .build());
        }
        return result;
    }
}
