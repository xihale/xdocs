package top.xihale.clouddoc.service;

import top.xihale.clouddoc.constant.NotificationType;
import top.xihale.clouddoc.dao.NotificationDao;
import top.xihale.clouddoc.dao.TeamDao;
import top.xihale.clouddoc.dao.TeamMemberDao;
import top.xihale.clouddoc.dao.UserDao;
import top.xihale.clouddoc.dao.KnowledgeBaseDao;
import top.xihale.clouddoc.po.Notification;
import top.xihale.clouddoc.po.TeamMember;
import top.xihale.clouddoc.po.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知业务逻辑层
 */
public class NotificationService {

    // ==================== 发送通知 ====================

    /**
     * 创建通知并推送给在线用户。
     * 通知发送失败不影响主业务。
     */
    public static void send(int userId, NotificationType type, String title, String content, String link, Integer senderId) {
        try {
            Notification notification = new Notification(userId, type.getCode(), title, content, link, senderId);
            NotificationDao.INSTANCE.insert(notification);
            // 实时推送
            pushToUser(userId, notification);
        } catch (Exception e) {
            // 通知表不存在或写入失败，不影响主业务
            System.getLogger(NotificationService.class.getName())
                    .log(System.Logger.Level.WARNING, "发送通知失败: " + e.getMessage(), e);
        }
    }

    /**
     * 团队邀请通知
     */
    public static void notifyTeamInvite(int teamId, String teamName, int userId, int inviterId) {
        String inviterName = getDisplayName(inviterId);
        send(userId, NotificationType.TEAM_INVITE,
                "团队邀请",
                inviterName + " 邀请你加入团队「" + teamName + "」",
                "/team/" + teamId,
                inviterId);
    }

    /**
     * 知识库邀请通知
     */
    public static void notifyKbInvite(int kbId, String kbName, int userId, int inviterId) {
        String inviterName = getDisplayName(inviterId);
        send(userId, NotificationType.KB_INVITE,
                "知识库邀请",
                inviterName + " 邀请你加入知识库「" + kbName + "」",
                "/kb/" + kbId,
                inviterId);
    }

    /**
     * 团队新文章通知（通知团队内除作者外的所有成员）
     */
    public static void notifyTeamNewArticle(int teamId, String teamName, int articleId, String articleTitle, int authorId) {
        List<TeamMember> members = TeamMemberDao.INSTANCE.findByTeamId(teamId);
        for (TeamMember m : members) {
            if (m.getUserId() == authorId) continue;
            send(m.getUserId(), NotificationType.TEAM_NEW_ARTICLE,
                    "团队新文章",
                    "团队「" + teamName + "」发布了新文章「" + articleTitle + "」",
                    "/article/" + articleId,
                    authorId);
        }
    }

    /**
     * 评论通知
     */
    public static void notifyComment(int articleId, String articleTitle, int authorId, int commenterId) {
        if (authorId == commenterId) return;
        String commenterName = getDisplayName(commenterId);
        send(authorId, NotificationType.COMMENT,
                "新评论",
                commenterName + " 评论了你的文章「" + articleTitle + "」",
                "/article/" + articleId,
                commenterId);
    }

    /**
     * 点赞通知
     */
    public static void notifyLike(int articleId, String articleTitle, int authorId, int likerId) {
        if (authorId == likerId) return;
        String likerName = getDisplayName(likerId);
        send(authorId, NotificationType.LIKE,
                "收到点赞",
                likerName + " 赞了你的文章「" + articleTitle + "」",
                "/article/" + articleId,
                likerId);
    }

    /**
     * 成员变动通知（被移除/角色变更）
     */
    public static void notifyMemberChange(int userId, String title, String content, Integer operatorId) {
        send(userId, NotificationType.MEMBER_CHANGE, title, content, null, operatorId);
    }

    /**
     * 关注通知
     */
    public static void notifyFollow(int followedUserId, int followerId) {
        String followerName = getDisplayName(followerId);
        send(followedUserId, NotificationType.FOLLOW,
                "新关注",
                followerName + " 关注了你",
                "/profile?id=" + followerId,
                followerId);
    }

    /**
     * 关注者文章发布通知（首次从草稿转为公开，且关注者有权限查看）
     */
    public static void notifyFollowersNewArticle(int articleId, String articleTitle, int authorId) {
        List<Integer> followerIds = top.xihale.clouddoc.dao.FollowUserDao.findFollowerIds(authorId);
        String authorName = getDisplayName(authorId);
        for (int followerId : followerIds) {
            if (followerId == authorId) continue;
            if (!canFollowerViewArticle(articleId, followerId)) continue;
            send(followerId, NotificationType.FOLLOW_ARTICLE,
                    "关注者发布了新文章",
                    authorName + " 发布了新文章「" + articleTitle + "」",
                    "/article/" + articleId,
                    authorId);
        }
    }

    /**
     * 关注者文章更新通知（已公开的文章被更新，且关注者有权限查看）
     */
    public static void notifyFollowersArticleUpdated(int articleId, String articleTitle, int authorId) {
        List<Integer> followerIds = top.xihale.clouddoc.dao.FollowUserDao.findFollowerIds(authorId);
        String authorName = getDisplayName(authorId);
        for (int followerId : followerIds) {
            if (followerId == authorId) continue;
            if (!canFollowerViewArticle(articleId, followerId)) continue;
            send(followerId, NotificationType.FOLLOW_ARTICLE,
                    "关注者更新了文章",
                    authorName + " 更新了文章「" + articleTitle + "」",
                    "/article/" + articleId,
                    authorId);
        }
    }

    /**
     * 判断关注者是否有权限查看文章所在的知识库
     * - 公开知识库：所有人可见
     * - 私有个人知识库：KB 成员可见
     * - 私有团队知识库：团队成员 或 KB 成员可见
     */
    private static boolean canFollowerViewArticle(int articleId, int followerId) {
        var article = top.xihale.clouddoc.dao.ArticleDao.INSTANCE.findById(articleId).orElse(null);
        if (article == null) return false;
        var kb = top.xihale.clouddoc.dao.KnowledgeBaseDao.INSTANCE.findById(article.getKnowledgeBaseId()).orElse(null);
        if (kb == null) return false;

        // 公开知识库 → 所有人可见
        if (kb.getVisibility() == top.xihale.clouddoc.constant.Visibility.PUBLIC.getCode()) {
            return true;
        }

        // 私有知识库 → 检查 KB 成员
        var kbMember = top.xihale.clouddoc.dao.KnowledgeBaseMemberDao.INSTANCE
                .findByKbIdAndUserId(kb.getId(), followerId)
                .orElse(null);
        if (kbMember != null && kbMember.getInviteStatus() == top.xihale.clouddoc.constant.JoinStatus.ACCEPTED.getCode()) {
            return true;
        }

        // 私有团队知识库 → 检查团队成员
        if (kb.getOwnerType() == top.xihale.clouddoc.constant.OwnerType.TEAM.getCode()) {
            return top.xihale.clouddoc.dao.TeamMemberDao.INSTANCE
                    .findByTeamIdAndUserId(kb.getOwnerId(), followerId)
                    .map(m -> m.getJoinStatus() == top.xihale.clouddoc.constant.JoinStatus.ACCEPTED.getCode())
                    .orElse(false);
        }

        return false;
    }

    // ==================== 查询 ====================

    public static List<Map<String, Object>> listNotifications(int userId, int offset, int limit) {
        List<Notification> notifications = NotificationDao.INSTANCE.findByUserId(userId, offset, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifications) {
            result.add(toVO(n));
        }
        return result;
    }

    public static int unreadCount(int userId) {
        return NotificationDao.INSTANCE.countUnread(userId);
    }

    public static void markRead(int notificationId, int userId) {
        NotificationDao.INSTANCE.markRead(notificationId, userId);
    }

    public static void markAllRead(int userId) {
        NotificationDao.INSTANCE.markAllRead(userId);
    }

    public static void deleteNotification(int notificationId, int userId) {
        NotificationDao.INSTANCE.delete(notificationId, userId);
    }

    // ==================== VO 转换 ====================

    private static Map<String, Object> toVO(Notification n) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", n.getId());
        vo.put("type", n.getType());
        vo.put("title", n.getTitle());
        vo.put("content", n.getContent());
        vo.put("link", n.getLink());
        vo.put("isRead", n.getIsRead());
        vo.put("createTime", n.getCreateTime());

        // 发送者信息
        if (n.getSenderId() != null) {
            User sender = UserDao.INSTANCE.findById(n.getSenderId()).orElse(null);
            vo.put("senderId", n.getSenderId());
            vo.put("senderName", sender != null ? getDisplayName(sender) : null);
            vo.put("senderAvatar", sender != null ? sender.getAvatarUrl() : null);
        }
        return vo;
    }

    private static String getDisplayName(int userId) {
        return UserDao.INSTANCE.findById(userId)
                .map(NotificationService::getDisplayName)
                .orElse("未知用户");
    }

    private static String getDisplayName(User user) {
        if (user == null) return "未知用户";
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }

    // ==================== WebSocket 推送 ====================

    private static void pushToUser(int userId, Notification notification) {
        try {
            var ws = top.xihale.clouddoc.websocket.NotificationWebSocket.getInstance();
            if (ws != null) {
                ws.sendToUser(userId, toVO(notification));
            }
        } catch (Exception e) {
            // WebSocket 推送失败不影响业务
        }
    }
}
