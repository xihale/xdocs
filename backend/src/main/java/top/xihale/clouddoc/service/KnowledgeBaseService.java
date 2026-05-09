package top.xihale.clouddoc.service;

import top.xihale.clouddoc.constant.JoinStatus;
import top.xihale.clouddoc.constant.KnowledgeBaseRole;
import top.xihale.clouddoc.constant.OwnerType;
import top.xihale.clouddoc.dao.*;
import top.xihale.clouddoc.exception.KbException;
import top.xihale.clouddoc.exception.KbException.KbError;
import top.xihale.clouddoc.po.KnowledgeBase;
import top.xihale.clouddoc.po.KnowledgeBaseMember;
import top.xihale.clouddoc.po.User;
import top.xihale.clouddoc.service.NotificationService;
import top.xihale.clouddoc.util.SqlBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库业务逻辑层
 */
public class KnowledgeBaseService {

    public static KnowledgeBase createKnowledgeBase(String name, String description, int visibility, int ownerType, int ownerId, int creatorId) {
        // 创建 TEAM 知识库时，校验操作者是 TEAM 成员
        if (ownerType == OwnerType.TEAM.getCode()) {
            TeamMemberDao.INSTANCE.findByTeamIdAndUserId(ownerId, creatorId)
                    .filter(m -> m.getJoinStatus() == JoinStatus.ACCEPTED.getCode())
                    .orElseThrow(() -> new KbException(KbError.NOT_TEAM_MEMBER));
        }

        return SqlBuilder.inTransaction(conn -> {
            KnowledgeBase kb = new KnowledgeBase(name, description, visibility, ownerType, ownerId, creatorId);
            KnowledgeBaseDao.INSTANCE.insert(kb);

            // 自动将创建者添加为 OWNER 成员
            KnowledgeBaseMember member = new KnowledgeBaseMember(
                    kb.getId(), creatorId,
                    KnowledgeBaseRole.OWNER.getCode(),
                    JoinStatus.ACCEPTED.getCode(),
                    creatorId
            );
            KnowledgeBaseMemberDao.INSTANCE.insert(member);

            return kb;
        });
    }

    public static KnowledgeBase findKnowledgeBaseById(int id) {
        return KnowledgeBaseDao.INSTANCE.findById(id)
                .orElseThrow(() -> new KbException(KbError.KB_NOT_FOUND));
    }

    public static List<KnowledgeBase> findByOwner(int ownerType, int ownerId) {
        return KnowledgeBaseDao.INSTANCE.findByOwnerId(ownerType, ownerId);
    }

    public static void checkKbPermission(int kbId, int userId, KnowledgeBaseRole... requiredRoles) {
        findKnowledgeBaseById(kbId);
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        for (KnowledgeBaseRole required : requiredRoles) {
            if (member.getRole() == required.getCode()) return;
        }
        throw new KbException(KbError.KB_PERMISSION_DENIED);
    }

    public static void updateKnowledgeBase(KnowledgeBase kb) {
        KnowledgeBaseDao.INSTANCE.update(kb);
    }

    public static void deleteKnowledgeBase(int id) {
        findKnowledgeBaseById(id);
        // 级联删除：成员 → 文章（含评论、点赞、收藏、浏览记录） → 知识库
        KnowledgeBaseMemberDao.INSTANCE.deleteByKbId(id);
        var articles = ArticleDao.INSTANCE.findByKnowledgeBaseId(id);
        for (var article : articles) {
            ArticleService.deleteArticle(article.getId());
        }
        KnowledgeBaseDao.INSTANCE.deleteById(id);
    }

    public static void authorizeMember(int kbId, int userId, int role, int inviterId) {
        // 校验知识库存在
        findKnowledgeBaseById(kbId);

        // 校验操作者是知识库的 OWNER 或 ADMIN
        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, inviterId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.AUTHORIZE_REQUIRES_OWNER_OR_ADMIN);
        }

        // 查看是否已有成员记录
        KnowledgeBaseMember existing = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId).orElse(null);

        if (existing != null) {
            // 已有成员记录，更新角色
            KnowledgeBaseMemberDao.INSTANCE.updateRole(kbId, userId, role);
        } else {
            // 没有成员记录，创建新成员
            KnowledgeBaseMember member = new KnowledgeBaseMember(
                    kbId, userId, role,
                    JoinStatus.INVITED.getCode(),
                    inviterId
            );
            KnowledgeBaseMemberDao.INSTANCE.insert(member);

            // 发送知识库邀请通知
            var kb = KnowledgeBaseDao.INSTANCE.findById(kbId).orElse(null);
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

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.REMOVE_REQUIRES_OWNER_OR_ADMIN);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.TARGET_NOT_KB_MEMBER));
        if (target.getRole() == KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.CANNOT_REMOVE_KB_OWNER);
        }
        KnowledgeBaseMemberDao.INSTANCE.delete(kbId, userId);

        // 通知被移除的用户
        var kb = KnowledgeBaseDao.INSTANCE.findById(kbId).orElse(null);
        String operatorName = UserDao.INSTANCE.findById(operatorId)
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
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (member.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.KB_INVITE_STATUS_NOT_PENDING);
        }
        KnowledgeBaseMemberDao.INSTANCE.updateInviteStatus(kbId, userId, JoinStatus.ACCEPTED.getCode());

        // 通知邀请者
        if (member.getInviteBy() != null) {
            var kb = KnowledgeBaseDao.INSTANCE.findById(kbId).orElse(null);
            String userName = UserDao.INSTANCE.findById(userId)
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
        KnowledgeBaseMember member = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (member.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.KB_INVITE_STATUS_NOT_PENDING);
        }
        KnowledgeBaseMemberDao.INSTANCE.updateInviteStatus(kbId, userId, JoinStatus.REJECTED.getCode());
    }

    /**
     * 取消知识库邀请
     */
    public static void cancelInvite(int kbId, int userId, int operatorId) {
        findKnowledgeBaseById(kbId);

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()
                && operator.getRole() != KnowledgeBaseRole.ADMIN.getCode()) {
            throw new KbException(KbError.CANCEL_KB_INVITE_REQUIRES_OWNER_OR_ADMIN);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_INVITE_NOT_FOUND));
        if (target.getInviteStatus() != JoinStatus.INVITED.getCode()) {
            throw new KbException(KbError.NO_PENDING_KB_INVITE);
        }
        KnowledgeBaseMemberDao.INSTANCE.delete(kbId, userId);
    }

    /**
     * 修改知识库成员角色
     */
    public static void updateMemberRole(int kbId, int userId, int role, int operatorId) {
        findKnowledgeBaseById(kbId);

        KnowledgeBaseMember operator = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, operatorId)
                .orElseThrow(() -> new KbException(KbError.NOT_KB_MEMBER));
        if (operator.getRole() != KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.KB_ROLE_CHANGE_REQUIRES_OWNER);
        }

        KnowledgeBaseMember target = KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(kbId, userId)
                .orElseThrow(() -> new KbException(KbError.KB_TARGET_MEMBER_NOT_FOUND));
        if (target.getRole() == KnowledgeBaseRole.OWNER.getCode()) {
            throw new KbException(KbError.CANNOT_CHANGE_KB_OWNER_ROLE);
        }
        KnowledgeBaseMemberDao.INSTANCE.updateRole(kbId, userId, role);
    }

    // ==================== 查询辅助 ====================

    public static List<Map<String, Object>> buildMemberVOList(int kbId) {
        var members = KnowledgeBaseMemberDao.INSTANCE.findByKbId(kbId);
        List<Map<String, Object>> accepted = new ArrayList<>();
        List<Map<String, Object>> invited = new ArrayList<>();
        for (var m : members) {
            if (m.getInviteStatus() == JoinStatus.REJECTED.getCode()) continue;
            var user = UserDao.INSTANCE.findById(m.getUserId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("kbId", m.getKnowledgeBaseId());
            item.put("userId", m.getUserId());
            item.put("username", user != null ? user.getUsername() : null);
            item.put("nickname", user != null ? user.getNickname() : null);
            item.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
            item.put("role", m.getRole());
            item.put("roleName", KnowledgeBaseRole.fromCode(m.getRole()).name());
            item.put("inviteStatus", m.getInviteStatus());
            item.put("joinTime", m.getJoinTime());
            if (m.getInviteStatus() == JoinStatus.ACCEPTED.getCode()) {
                accepted.add(item);
            } else if (m.getInviteStatus() == JoinStatus.INVITED.getCode()) {
                invited.add(item);
            }
        }
        // 已接受在前，待邀请在后
        List<Map<String, Object>> result = new ArrayList<>(accepted);
        result.addAll(invited);
        return result;
    }

    public static List<Map<String, Object>> buildPendingInviteList(int userId) {
        List<KnowledgeBaseMember> pending = KnowledgeBaseMemberDao.INSTANCE.findPendingByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeBaseMember member : pending) {
            var kb = KnowledgeBaseDao.INSTANCE.findById(member.getKnowledgeBaseId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", member.getId());
            item.put("kbId", member.getKnowledgeBaseId());
            item.put("kbName", kb != null ? kb.getName() : null);
            item.put("role", member.getRole());
            item.put("inviteStatus", member.getInviteStatus());
            item.put("joinTime", member.getJoinTime());
            result.add(item);
        }
        return result;
    }
}
