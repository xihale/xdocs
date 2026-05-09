package top.xihale.clouddoc.service;

import top.xihale.clouddoc.constant.JoinStatus;
import top.xihale.clouddoc.constant.KnowledgeBaseRole;
import top.xihale.clouddoc.constant.OwnerType;
import top.xihale.clouddoc.dao.*;
import top.xihale.clouddoc.exception.ArticleException;
import top.xihale.clouddoc.exception.ArticleException.ArticleError;
import top.xihale.clouddoc.po.Article;
import top.xihale.clouddoc.po.Comment;
import top.xihale.clouddoc.po.KnowledgeBase;
import top.xihale.clouddoc.po.KnowledgeBaseMember;
import top.xihale.clouddoc.po.TeamMember;
import top.xihale.clouddoc.po.User;
import top.xihale.clouddoc.service.NotificationService;
import top.xihale.clouddoc.vo.ArticleVO;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章业务逻辑层
 */
public class ArticleService {

    // ==================== 文章 CRUD ====================

    public static Article createArticle(int kbId, String title, String content, int authorId) {
        if (KnowledgeBaseDao.INSTANCE.findById(kbId).isEmpty()) {
            throw new ArticleException(ArticleError.KB_NOT_FOUND);
        }
        Article article = new Article(kbId, title, content, authorId);
        ArticleDao.INSTANCE.insert(article);

        // 通知团队成员有新文章
        var kb = KnowledgeBaseDao.INSTANCE.findById(kbId).orElse(null);
        if (kb != null && kb.getOwnerType() == OwnerType.TEAM.getCode()) {
            var team = TeamDao.INSTANCE.findById(kb.getOwnerId()).orElse(null);
            if (team != null) {
                NotificationService.notifyTeamNewArticle(
                        team.getId(), team.getName(),
                        article.getId(), title, authorId);
            }
        }

        return article;
    }

    public static Article findArticleById(int id) {
        return ArticleDao.INSTANCE.findById(id)
                .orElseThrow(() -> new ArticleException(ArticleError.ARTICLE_NOT_FOUND));
    }

    public static List<Article> findByKnowledgeBase(int kbId) {
        return ArticleDao.INSTANCE.findByKnowledgeBaseId(kbId);
    }

    public static List<Article> findPublicArticles(int offset, int limit) {
        return ArticleDao.INSTANCE.findPublicArticles(offset, limit);
    }

    public static List<Article> findPublicArticlesOrderByLikes(int offset, int limit) {
        return ArticleDao.INSTANCE.findPublicArticlesOrderByLikes(offset, limit);
    }

    public static List<Article> searchPublic(String keyword, int offset, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return findPublicArticles(offset, limit);
        }
        return ArticleDao.INSTANCE.searchPublic(keyword.trim(), offset, limit);
    }

    /** 搜索公开文章（Servlet 层委托） */
    public static List<Article> searchPublicArticles(String keyword, int offset, int limit) {
        return searchPublic(keyword, offset, limit);
    }

    /** 搜索公开知识库（Servlet 层委托） */
    public static List<KnowledgeBase> searchPublicKbs(String keyword) {
        return KnowledgeBaseDao.INSTANCE.findPublicByName(keyword);
    }

    public static void checkArticleOwner(int articleId, int userId) {
        Article article = findArticleById(articleId);
        if (article.getAuthorId() != userId) {
            throw new ArticleException(ArticleError.NOT_ARTICLE_OWNER);
        }
    }

    /**
     * 校验用户可编辑文章：作者 / 知识库 EDITOR 及以上 / TEAM 知识库的 TEAM 成员
     */
    public static void checkArticleEditable(int articleId, int userId) {
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

        return KnowledgeBaseDao.INSTANCE.findById(article.getKnowledgeBaseId())
                .filter(kb -> kb.getOwnerType() == OwnerType.TEAM.getCode())
                .map(kb -> isAcceptedTeamMember(kb.getOwnerId(), userId))
                .orElse(false);
    }

    private static boolean hasKnowledgeBaseEditRole(int knowledgeBaseId, int userId) {
        return KnowledgeBaseMemberDao.INSTANCE.findByKbIdAndUserId(knowledgeBaseId, userId)
                .filter(ArticleService::isAcceptedEditorOrAbove)
                .isPresent();
    }

    private static boolean isAcceptedEditorOrAbove(KnowledgeBaseMember member) {
        return member.getInviteStatus() == JoinStatus.ACCEPTED.getCode()
                && member.getRole() <= KnowledgeBaseRole.EDITOR.getCode();
    }

    private static boolean isAcceptedTeamMember(int teamId, int userId) {
        return TeamMemberDao.INSTANCE.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getJoinStatus)
                .filter(status -> status == JoinStatus.ACCEPTED.getCode())
                .isPresent();
    }

    public static void updateArticle(Article article) {
        ArticleDao.INSTANCE.update(article);
    }

    public static void deleteArticle(int id) {
        // 级联删除：评论、点赞、收藏、浏览记录
        CommentDao.INSTANCE.deleteByArticleId(id);
        ArticleLikeDao.deleteByArticleId(id);
        FavoriteDao.deleteByTargetId(FavoriteDao.TYPE_ARTICLE, id);
        RecentVisitDao.deleteByArticleId(id);
        ArticleDao.INSTANCE.deleteById(id);
    }

    // ==================== VO 转换 ====================

    public static ArticleVO toVO(Article article) {
        return toVO(article, null);
    }

    public static ArticleVO toVO(Article article, Integer userId) {
        var author = UserDao.INSTANCE.findById(article.getAuthorId()).orElse(null);
        var kb = KnowledgeBaseDao.INSTANCE.findById(article.getKnowledgeBaseId()).orElse(null);
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setKnowledgeBaseId(article.getKnowledgeBaseId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setContentFormat(article.getContentFormat());
        vo.setAuthorId(article.getAuthorId());
        vo.setAuthorName(displayName(author));
        vo.setAuthorAvatar(author != null ? author.getAvatarUrl() : null);
        vo.setStatus(article.getStatus());
        vo.setCoverImage(article.getCoverImage());
        vo.setKnowledgeBaseName(kb != null ? kb.getName() : null);
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());

        // TEAM 信息
        if (kb != null && kb.getOwnerType() == OwnerType.TEAM.getCode()) {
            var team = TeamDao.INSTANCE.findById(kb.getOwnerId()).orElse(null);
            if (team != null) {
                vo.setTeamName(team.getName());
                vo.setTeamId(team.getId());
            }
        }

        // 计算 canEdit
        if (userId != null) {
            vo.setCanEdit(canUserEditArticle(article, userId));
        } else {
            vo.setCanEdit(false);
        }

        // 点赞 & 收藏信息
        if (userId != null) {
            vo.setLikeCount(countLikes(article.getId()));
            vo.setLiked(isLiked(article.getId(), userId));
            vo.setFavorited(FavoriteDao.exists(userId, FavoriteDao.TYPE_ARTICLE, article.getId()));
        }

        return vo;
    }

    private static String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
    }

    private static Map<String, Object> compactArticleVO(Article article) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", article.getId());
        vo.put("title", article.getTitle());
        vo.put("authorId", article.getAuthorId());
        vo.put("updateTime", article.getUpdateTime());
        return vo;
    }

    // ==================== 点赞 ====================

    public static Map<String, Object> likeArticle(int articleId, int userId) {
        findArticleById(articleId); // 确保文章存在
        boolean inserted = ArticleLikeDao.insert(articleId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", inserted || ArticleLikeDao.exists(articleId, userId));
        result.put("likeCount", ArticleLikeDao.countByArticle(articleId));

        // 通知文章作者
        if (inserted) {
            var article = ArticleDao.INSTANCE.findById(articleId).orElse(null);
            if (article != null) {
                NotificationService.notifyLike(articleId, article.getTitle(), article.getAuthorId(), userId);
            }
        }

        return result;
    }

    public static Map<String, Object> unlikeArticle(int articleId, int userId) {
        ArticleLikeDao.delete(articleId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", false);
        result.put("likeCount", ArticleLikeDao.countByArticle(articleId));
        return result;
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
        CommentDao.INSTANCE.insert(comment);

        // 通知文章作者
        var article = ArticleDao.INSTANCE.findById(articleId).orElse(null);
        if (article != null) {
            NotificationService.notifyComment(articleId, article.getTitle(), article.getAuthorId(), userId);
        }

        return comment.getId();
    }

    public static List<Map<String, Object>> listComments(int articleId) {
        var comments = CommentDao.INSTANCE.findByArticleId(articleId);
        List<Map<String, Object>> voList = new java.util.ArrayList<>();
        for (Comment c : comments) {
            var author = UserDao.INSTANCE.findById(c.getUserId()).orElse(null);
            var replyToUser = c.getReplyToId() != null ? UserDao.INSTANCE.findById(
                    CommentDao.INSTANCE.findById(c.getReplyToId()).map(Comment::getUserId).orElse(0)
            ).orElse(null) : null;

            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", c.getId());
            vo.put("articleId", c.getArticleId());
            vo.put("userId", c.getUserId());
            vo.put("username", author != null ? author.getUsername() : null);
            vo.put("nickname", author != null ? author.getNickname() : null);
            vo.put("avatarUrl", author != null ? author.getAvatarUrl() : null);
            vo.put("parentId", c.getParentId());
            vo.put("replyToId", c.getReplyToId());
            vo.put("replyToNickname", replyToUser != null ? replyToUser.getNickname() : null);
            vo.put("content", c.getContent());
            vo.put("createTime", c.getCreateTime() != null ? c.getCreateTime().toString() : null);
            voList.add(vo);
        }
        return voList;
    }

    public static void deleteComment(int commentId, int userId) {
        Comment comment = CommentDao.INSTANCE.findById(commentId)
                .orElseThrow(() -> new ArticleException(ArticleError.COMMENT_NOT_FOUND));
        // 只能删自己的评论（或文章作者可删）
        Article article = ArticleDao.INSTANCE.findById(comment.getArticleId()).orElse(null);
        if (comment.getUserId() != userId && (article == null || article.getAuthorId() != userId)) {
            throw new ArticleException(ArticleError.CANNOT_DELETE_OTHERS_COMMENT);
        }
        CommentDao.INSTANCE.deleteById(commentId);
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
    public static List<Map<String, Object>> buildFavoriteVOList(int userId) {
        List<Integer> articleIds = findFavoriteArticleIds(userId);
        List<Map<String, Object>> voList = new java.util.ArrayList<>();
        for (int aid : articleIds) {
            var article = ArticleDao.INSTANCE.findById(aid).orElse(null);
            if (article == null) continue;
            Map<String, Object> vo = compactArticleVO(article);
            vo.put("createTime", article.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }

    /** 构建浏览历史 VO 列表 */
    public static List<Map<String, Object>> buildHistoryVOList(int userId, int limit) {
        List<Integer> articleIds = findRecentArticleIds(userId, limit);
        List<Map<String, Object>> voList = new java.util.ArrayList<>();
        for (int aid : articleIds) {
            var article = ArticleDao.INSTANCE.findById(aid).orElse(null);
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
