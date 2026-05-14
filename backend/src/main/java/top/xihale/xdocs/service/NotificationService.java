package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.NotificationType;
import top.xihale.xdocs.dao.NotificationDao;
import top.xihale.xdocs.dao.TeamDao;
import top.xihale.xdocs.dao.TeamMemberDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.dao.KnowledgeBaseDao;
import top.xihale.xdocs.po.Notification;
import top.xihale.xdocs.po.TeamMember;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.vo.NotificationVO;

import java.util.ArrayList;
import java.util.List;

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
            NotificationDao.insert(notification);
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
        List<TeamMember> members = TeamMemberDao.findByTeamId(teamId);
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
        List<Integer> followerIds = top.xihale.xdocs.dao.FollowUserDao.findFollowerIds(authorId);
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
        List<Integer> followerIds = top.xihale.xdocs.dao.FollowUserDao.findFollowerIds(authorId);
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
        var article = top.xihale.xdocs.dao.ArticleDao.findById(articleId).orElse(null);
        if (article == null) return false;
        var kb = top.xihale.xdocs.dao.KnowledgeBaseDao.findById(article.getKnowledgeBaseId()).orElse(null);
        if (kb == null) return false;

        // 公开知识库 → 所有人可见
        if (kb.getVisibility() == top.xihale.xdocs.constant.Visibility.PUBLIC.getCode()) {
            return true;
        }

        // 私有知识库 → 检查 KB 成员
        var kbMember = top.xihale.xdocs.dao.KnowledgeBaseMemberDao
                .findByKbIdAndUserId(kb.getId(), followerId)
                .orElse(null);
        if (kbMember != null && kbMember.getInviteStatus() == top.xihale.xdocs.constant.JoinStatus.ACCEPTED.getCode()) {
            return true;
        }

        // 私有团队知识库 → 检查团队成员
        if (kb.getOwnerType() == top.xihale.xdocs.constant.OwnerType.TEAM.getCode()) {
            return top.xihale.xdocs.dao.TeamMemberDao
                    .findByTeamIdAndUserId(kb.getOwnerId(), followerId)
                    .map(m -> m.getJoinStatus() == top.xihale.xdocs.constant.JoinStatus.ACCEPTED.getCode())
                    .orElse(false);
        }

        return false;
    }

    // ==================== 查询 ====================

    public static List<NotificationVO> listNotifications(int userId, int offset, int limit) {
        List<Notification> notifications = NotificationDao.findByUserId(userId, offset, limit);
        List<NotificationVO> result = new ArrayList<>();
        for (Notification n : notifications) {
            result.add(toVO(n));
        }
        return result;
    }

    public static int unreadCount(int userId) {
        return NotificationDao.countUnread(userId);
    }

    public static void markRead(int notificationId, int userId) {
        NotificationDao.markRead(notificationId, userId);
    }

    public static void markAllRead(int userId) {
        NotificationDao.markAllRead(userId);
    }

    public static void deleteNotification(int notificationId, int userId) {
        NotificationDao.delete(notificationId, userId);
    }

    // ==================== VO 转换 ====================

    private static NotificationVO toVO(Notification n) {
        // 发送者信息
        Integer senderId = null;
        String senderName = null;
        String senderAvatar = null;
        if (n.getSenderId() != null) {
            senderId = n.getSenderId();
            User sender = UserDao.findById(n.getSenderId()).orElse(null);
            senderName = sender != null ? getDisplayName(sender) : null;
            senderAvatar = sender != null ? sender.getAvatarUrl() : null;
        }

        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .link(n.getLink())
                .isRead(n.isRead())
                .createTime(n.getCreateTime())
                .senderId(senderId)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .build();
    }

    private static String getDisplayName(int userId) {
        return UserDao.findById(userId)
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
            var ws = top.xihale.xdocs.websocket.NotificationWebSocket.getInstance();
            if (ws != null) {
                ws.sendToUser(userId, toVO(notification));
            }
        } catch (Exception e) {
            // WebSocket 推送失败不影响业务
        }
    }
}
