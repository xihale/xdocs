package top.xihale.xdocs.service;

import top.xihale.xdocs.constant.JoinStatus;
import top.xihale.xdocs.constant.KnowledgeBaseRole;
import top.xihale.xdocs.constant.OwnerType;
import top.xihale.xdocs.dao.*;
import top.xihale.xdocs.exception.ArticleException;
import top.xihale.xdocs.exception.ArticleException.ArticleError;
import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.po.Comment;
import top.xihale.xdocs.po.KnowledgeBase;
import top.xihale.xdocs.po.KnowledgeBaseMember;
import top.xihale.xdocs.po.TeamMember;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.vo.ArticleVO;
import top.xihale.xdocs.vo.CommentVO;
import top.xihale.xdocs.vo.CompactArticleVO;
import top.xihale.xdocs.vo.LikeResultVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章业务逻辑层
 */
public class ArticleService {

    // ==================== 文章 CRUD ====================

    public static Article createArticle(int kbId, String title, String content, int authorId) {
        if (KnowledgeBaseDao.findById(kbId).isEmpty()) {
            throw new ArticleException(ArticleError.KB_NOT_FOUND);
        }
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("标题长度不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        Article article = new Article(kbId, title, content, authorId);
        ArticleDao.insert(article);

        // 通知团队成员有新文章
        var kb = KnowledgeBaseDao.findById(kbId).orElse(null);
        if (kb != null && kb.getOwnerType() == OwnerType.TEAM.getCode()) {
            var team = TeamDao.findById(kb.getOwnerId()).orElse(null);
            if (team != null) {
                NotificationService.notifyTeamNewArticle(
                        team.getId(), team.getName(),
                        article.getId(), title, authorId);
            }
        }

        return article;
    }

    public static Article findArticleById(int id) {
        return ArticleDao.findById(id)
                .orElseThrow(() -> new ArticleException(ArticleError.ARTICLE_NOT_FOUND));
    }

    public static List<Article> findByKnowledgeBase(int kbId) {
        return ArticleDao.findByKnowledgeBaseId(kbId);
    }

    public static List<Article> findPublicArticles(int offset, int limit) {
        return ArticleDao.findPublicArticles(offset, limit);
    }

    public static List<Article> findPublicArticlesOrderByLikes(int offset, int limit) {
        return ArticleDao.findPublicArticlesOrderByLikes(offset, limit);
    }

    public static List<Article> searchPublic(String keyword, int offset, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return findPublicArticles(offset, limit);
        }
        return ArticleDao.searchPublic(keyword.trim(), offset, limit);
    }

    /** 搜索公开文章（Servlet 层委托） */
    public static List<Article> searchPublicArticles(String keyword, int offset, int limit) {
        return searchPublic(keyword, offset, limit);
    }

    /** 搜索公开知识库（Servlet 层委托） */
    public static List<KnowledgeBase> searchPublicKbs(String keyword) {
        return KnowledgeBaseDao.findPublicByName(keyword);
    }

    public static void ensureArticleOwner(int articleId, int userId) {
        Article article = findArticleById(articleId);
        if (article.getAuthorId() != userId) {
            throw new ArticleException(ArticleError.NOT_ARTICLE_OWNER);
        }
    }

    /**
     * 校验用户可编辑文章：作者 / 知识库 EDITOR 及以上 / TEAM 知识库的 TEAM 成员
     */
    private static void ensureArticleEditable(int articleId, int userId) {
        Article article = findArticleById(articleId);
        if (canUserEditArticle(article, userId)) {
            return;
        }

        throw new ArticleException(ArticleError.NO_EDIT_PERMISSION);
    }

    private static boolean canUserEditArticle(Article article, int userId) {
        if (article.getAuthorId() == userId) {
            return true;
        }

        if (hasKnowledgeBaseEditRole(article.getKnowledgeBaseId(), userId)) {
            return true;
        }

        return KnowledgeBaseDao.findById(article.getKnowledgeBaseId())
                .filter(kb -> kb.getOwnerType() == OwnerType.TEAM.getCode())
                .map(kb -> isAcceptedTeamMember(kb.getOwnerId(), userId))
                .orElse(false);
    }

    private static boolean hasKnowledgeBaseEditRole(int knowledgeBaseId, int userId) {
        return KnowledgeBaseMemberDao.findByKbIdAndUserId(knowledgeBaseId, userId)
                .filter(ArticleService::isAcceptedEditorOrAbove)
                .isPresent();
    }

    private static boolean isAcceptedEditorOrAbove(KnowledgeBaseMember member) {
        return member.getInviteStatus() == JoinStatus.ACCEPTED.getCode()
                && member.getRole() <= KnowledgeBaseRole.EDITOR.getCode();
    }

    private static boolean isAcceptedTeamMember(int teamId, int userId) {
        return TeamMemberDao.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getJoinStatus)
                .filter(status -> status == JoinStatus.ACCEPTED.getCode())
                .isPresent();
    }

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 500;

    /**
     * 更新文章
     */
    public static void updateArticle(int articleId, String title, String content, String summary, Integer status, int operatorId) {
        ensureArticleEditable(articleId, operatorId);
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("标题长度不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        if (summary != null && summary.length() > MAX_SUMMARY_LENGTH) {
            throw new IllegalArgumentException("摘要长度不能超过 " + MAX_SUMMARY_LENGTH + " 个字符");
        }
        Article article = findArticleById(articleId);
        int oldStatus = article.getStatus();
        boolean titleChanged = title != null && !title.equals(article.getTitle());
        boolean contentChanged = content != null;
        boolean summaryChanged = summary != null;

        if (title != null) article.setTitle(title);
        if (content != null) article.setContent(content);
        if (summary != null) article.setSummary(summary);
        if (status != null) article.setStatus(status);

        ArticleDao.update(article);

        // 通知关注者：草稿 → 公开（首次发布）
        if (oldStatus == 0 && article.getStatus() == 1) {
            NotificationService.notifyFollowersNewArticle(articleId, article.getTitle(), article.getAuthorId());
        }
        // 通知关注者：已公开文章内容/标题/摘要更新
        else if (oldStatus == 1 && article.getStatus() == 1 && (titleChanged || contentChanged || summaryChanged)) {
            NotificationService.notifyFollowersArticleUpdated(articleId, article.getTitle(), article.getAuthorId());
        }
    }

    /**
     * 保存文章内容
     */
    public static Article saveArticle(int articleId, String content, int operatorId) {
        ensureArticleEditable(articleId, operatorId);
        Article article = findArticleById(articleId);
        if (content != null) article.setContent(content);
        ArticleDao.update(article);
        return article;
    }

    /**
     * 删除文章
     */
    public static void deleteArticle(int articleId, int operatorId) {
        ensureArticleEditable(articleId, operatorId);
        deleteArticleData(articleId);
    }

    /**
     * 级联删除文章数据（无鉴权，仅供内部级联调用）
     */
    public static void deleteArticleData(int id) {
        CommentDao.deleteByArticleId(id);
        ArticleLikeDao.deleteByArticleId(id);
        FavoriteDao.deleteByTargetId(FavoriteDao.TYPE_ARTICLE, id);
        RecentVisitDao.deleteByArticleId(id);
        ArticleDao.deleteById(id);
    }

    // ==================== VO 转换 ====================

    public static ArticleVO toVO(Article article) {
        return toVO(article, null);
    }

    public static ArticleVO toVO(Article article, Integer userId) {
        var author = UserDao.findById(article.getAuthorId()).orElse(null);
        var kb = KnowledgeBaseDao.findById(article.getKnowledgeBaseId()).orElse(null);

        String teamName = null;
        Integer teamId = null;
        // TEAM 信息
        if (kb != null && kb.getOwnerType() == OwnerType.TEAM.getCode()) {
            var team = TeamDao.findById(kb.getOwnerId()).orElse(null);
            if (team != null) {
                teamName = team.getName();
                teamId = team.getId();
            }
        }

        // 计算 canEdit
        boolean canEdit = userId != null && canUserEditArticle(article, userId);

        // 点赞 & 收藏信息
        Integer likeCount = null;
        Boolean liked = null;
        Boolean favorited = null;
        if (userId != null) {
            likeCount = countLikes(article.getId());
            liked = isLiked(article.getId(), userId);
            favorited = FavoriteDao.exists(userId, FavoriteDao.TYPE_ARTICLE, article.getId());
        }

        return ArticleVO.builder()
                .id(article.getId())
                .knowledgeBaseId(article.getKnowledgeBaseId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(article.getContent())
                .contentFormat(article.getContentFormat())
                .authorId(article.getAuthorId())
                .authorName(displayName(author))
                .authorAvatar(author != null ? author.getAvatarUrl() : null)
                .status(article.getStatus())
                .coverImage(article.getCoverImage())
                .knowledgeBaseName(kb != null ? kb.getName() : null)
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .likeCount(likeCount)
                .liked(liked)
                .favorited(favorited)
                .canEdit(canEdit)
                .teamName(teamName)
                .teamId(teamId)
                .build();
    }

    private static String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
    }

    private static CompactArticleVO compactArticleVO(Article article) {
        return CompactArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .authorId(article.getAuthorId())
                .updateTime(article.getUpdateTime())
                .build();
    }

    // ==================== 点赞 ====================

    public static LikeResultVO likeArticle(int articleId, int userId) {
        findArticleById(articleId); // 确保文章存在
        boolean inserted = ArticleLikeDao.insert(articleId, userId);

        // 通知文章作者
        if (inserted) {
            var article = ArticleDao.findById(articleId).orElse(null);
            if (article != null) {
                NotificationService.notifyLike(articleId, article.getTitle(), article.getAuthorId(), userId);
            }
        }

        return LikeResultVO.builder()
                .liked(inserted || ArticleLikeDao.exists(articleId, userId))
                .likeCount(ArticleLikeDao.countByArticle(articleId))
                .build();
    }

    public static LikeResultVO unlikeArticle(int articleId, int userId) {
        ArticleLikeDao.delete(articleId, userId);
        return LikeResultVO.builder().liked(false).likeCount(ArticleLikeDao.countByArticle(articleId)).build();
    }

    public static int countLikes(int articleId) {
        return ArticleLikeDao.countByArticle(articleId);
    }

    public static boolean isLiked(int articleId, int userId) {
        return ArticleLikeDao.exists(articleId, userId);
    }

    // ==================== 评论 ====================

    public static int addComment(int articleId, int userId, String content, Integer parentId, Integer replyToId) {
        findArticleById(articleId); // 确保文章存在
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToId(replyToId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        CommentDao.insert(comment);

        // 通知文章作者
        var article = ArticleDao.findById(articleId).orElse(null);
        if (article != null) {
            NotificationService.notifyComment(articleId, article.getTitle(), article.getAuthorId(), userId);
        }

        return comment.getId();
    }

    public static List<CommentVO> listComments(int articleId) {
        var comments = CommentDao.findByArticleId(articleId);
        List<CommentVO> voList = new java.util.ArrayList<>();
        for (Comment c : comments) {
            var author = UserDao.findById(c.getUserId()).orElse(null);
            var replyToUser = c.getReplyToId() != null ? UserDao.findById(
                    CommentDao.findById(c.getReplyToId()).map(Comment::getUserId).orElse(0)
            ).orElse(null) : null;

            voList.add(CommentVO.builder()
                    .id(c.getId())
                    .articleId(c.getArticleId())
                    .userId(c.getUserId())
                    .username(author != null ? author.getUsername() : null)
                    .nickname(author != null ? author.getNickname() : null)
                    .avatarUrl(author != null ? author.getAvatarUrl() : null)
                    .parentId(c.getParentId())
                    .replyToId(c.getReplyToId())
                    .replyToNickname(replyToUser != null ? replyToUser.getNickname() : null)
                    .content(c.getContent())
                    .createTime(c.getCreateTime() != null ? c.getCreateTime().toString() : null)
                    .build());
        }
        return voList;
    }

    public static void deleteComment(int commentId, int userId) {
        Comment comment = CommentDao.findById(commentId)
                .orElseThrow(() -> new ArticleException(ArticleError.COMMENT_NOT_FOUND));
        // 只能删自己的评论（或文章作者可删）
        Article article = ArticleDao.findById(comment.getArticleId()).orElse(null);
        if (comment.getUserId() != userId && (article == null || article.getAuthorId() != userId)) {
            throw new ArticleException(ArticleError.CANNOT_DELETE_OTHERS_COMMENT);
        }
        CommentDao.deleteById(commentId);
    }

    // ==================== 收藏 ====================

    public static void favoriteArticle(int userId, int articleId) {
        FavoriteDao.insert(userId, FavoriteDao.TYPE_ARTICLE, articleId);
    }

    public static void unfavoriteArticle(int userId, int articleId) {
        FavoriteDao.delete(userId, FavoriteDao.TYPE_ARTICLE, articleId);
    }

    public static List<Integer> findFavoriteArticleIds(int userId) {
        return FavoriteDao.findArticleIds(userId);
    }

    /** 构建收藏文章 VO 列表 */
    public static List<CompactArticleVO> buildFavoriteVOList(int userId) {
        List<Integer> articleIds = findFavoriteArticleIds(userId);
        List<CompactArticleVO> voList = new java.util.ArrayList<>();
        for (int aid : articleIds) {
            var article = ArticleDao.findById(aid).orElse(null);
            if (article == null) continue;
            voList.add(CompactArticleVO.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .authorId(article.getAuthorId())
                    .updateTime(article.getUpdateTime())
                    .createTime(article.getCreateTime())
                    .build());
        }
        return voList;
    }

    /** 构建浏览历史 VO 列表 */
    public static List<CompactArticleVO> buildHistoryVOList(int userId, int limit) {
        List<Integer> articleIds = findRecentArticleIds(userId, limit);
        List<CompactArticleVO> voList = new java.util.ArrayList<>();
        for (int aid : articleIds) {
            var article = ArticleDao.findById(aid).orElse(null);
            if (article == null) continue;
            voList.add(compactArticleVO(article));
        }
        return voList;
    }

    // ==================== 浏览记录 ====================

    public static void recordVisit(int userId, int articleId) {
        RecentVisitDao.upsert(userId, articleId);
    }

    public static List<Integer> findRecentArticleIds(int userId, int limit) {
        return RecentVisitDao.findRecentArticleIds(userId, limit);
    }

    public static void deleteVisitHistory(int userId, Integer articleId) {
        if (articleId != null) {
            RecentVisitDao.delete(userId, articleId);
        } else {
            RecentVisitDao.deleteAll(userId);
        }
    }
}
