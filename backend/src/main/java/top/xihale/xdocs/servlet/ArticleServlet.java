package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.service.NotificationService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.servlet.route.Put;
import top.xihale.xdocs.util.HtmlSanitizer;
import top.xihale.xdocs.util.ResponseUtils;
import top.xihale.xdocs.vo.ArticleVO;

import java.io.IOException;
import java.util.*;

/**
 * 文章相关接口
 */
@WebServlet("/api/article/*")
public class ArticleServlet extends BaseServlet {

    @Post("/create")
    private void handleCreate(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        String title = requiredParam(req, "title");
        String content = optionalParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);

        var article = ArticleService.createArticle(kbId, title, content, userId);
        res.ok(article);
    }

    @Put("/update")
    private void handleUpdate(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        String title = optionalParam(req, "title");
        String content = optionalParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);
        String summary = optionalParam(req, "summary");
        Integer status = optionalIntParam(req, "status");

        ArticleService.checkArticleEditable(articleId, userId);
        Article article = ArticleService.findArticleById(articleId);

        int oldStatus = article.getStatus();
        boolean titleChanged = title != null && !title.equals(article.getTitle());
        boolean contentChanged = content != null;
        boolean summaryChanged = summary != null;

        if (title != null) article.setTitle(title);
        if (content != null) article.setContent(content);
        if (summary != null) article.setSummary(summary);
        if (status != null) article.setStatus(status);

        ArticleService.updateArticle(article);

        // 通知关注者：草稿 → 公开（首次发布）
        if (oldStatus == 0 && article.getStatus() == 1) {
            NotificationService.notifyFollowersNewArticle(articleId, article.getTitle(), article.getAuthorId());
        }
        // 通知关注者：已公开文章内容/标题/摘要更新
        else if (oldStatus == 1 && article.getStatus() == 1 && (titleChanged || contentChanged || summaryChanged)) {
            NotificationService.notifyFollowersArticleUpdated(articleId, article.getTitle(), article.getAuthorId());
        }

        res.ok(article);
    }

    @Delete("/delete")
    private void handleDelete(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        ArticleService.checkArticleEditable(articleId, userId);
        ArticleService.deleteArticle(articleId);
        res.ok();
    }

    @Public
    @Get("/detail")
    private void handleDetail(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int articleId = requiredIntParam(req, "id");
        var article = ArticleService.findArticleById(articleId);
        Integer userId = getOptionalUserId(req);
        res.ok(ArticleService.toVO(article, userId));
    }

    @Get("/list")
    private void handleListByKb(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int kbId = requiredIntParam(req, "kbId");
        var articles = ArticleService.findByKnowledgeBase(kbId);
        res.ok(articles);
    }

    @Public
    @Get("/public-list")
    private void handlePublicList(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int offset = optionalIntParamOrDefault(req, "offset", 0);
        int limit = optionalIntParamOrDefault(req, "limit", 20);
        String sort = optionalParamOrDefault(req, "sort", "time"); // time | likes
        String keyword = optionalParam(req, "keyword");

        List<Article> articles;
        if (keyword != null && !keyword.isBlank()) {
            articles = ArticleService.searchPublic(keyword.trim(), offset, limit);
        } else if ("likes".equals(sort)) {
            articles = ArticleService.findPublicArticlesOrderByLikes(offset, limit);
        } else {
            articles = ArticleService.findPublicArticles(offset, limit);
        }

        Integer userId = getOptionalUserId(req);
        var voList = articles.stream().map(a -> {
            ArticleVO vo = ArticleService.toVO(a);
            vo.setLikeCount(ArticleService.countLikes(a.getId()));
            if (userId != null) {
                vo.setLiked(ArticleService.isLiked(a.getId(), userId));
            }
            return vo;
        }).toList();
        res.ok(voList);
    }

    @Post("/save")
    private void handleSave(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        String content = optionalParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);

        ArticleService.checkArticleEditable(articleId, userId);
        Article article = ArticleService.findArticleById(articleId);
        if (content != null) article.setContent(content);
        ArticleService.updateArticle(article);
        res.ok(article);
    }

    // ==================== 点赞 ====================

    @Post("/like")
    private void handleLike(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        res.ok(ArticleService.likeArticle(articleId, userId));
    }

    @Post("/unlike")
    private void handleUnlike(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        res.ok(ArticleService.unlikeArticle(articleId, userId));
    }

    // ==================== 评论 ====================

    @Post("/comment")
    private void handleAddComment(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        String content = requiredParam(req, "content");
        content = HtmlSanitizer.stripHtml(content);
        Integer parentId = optionalIntParam(req, "parentId");
        Integer replyToId = optionalIntParam(req, "replyToId");

        int commentId = ArticleService.addComment(articleId, userId, content, parentId, replyToId);
        res.ok(Map.of("id", commentId));
    }

    @Public
    @Get("/comments")
    private void handleListComments(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int articleId = requiredIntParam(req, "articleId");
        res.ok(ArticleService.listComments(articleId));
    }

    @Delete("/comment-delete")
    private void handleDeleteComment(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int commentId = requiredIntParam(req, "id");
        ArticleService.deleteComment(commentId, userId);
        res.ok();
    }

    // ==================== 收藏 ====================

    @Post("/favorite")
    private void handleFavorite(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.favoriteArticle(userId, articleId);
        res.ok(Map.of("favorited", true));
    }

    @Post("/unfavorite")
    private void handleUnfavorite(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.unfavoriteArticle(userId, articleId);
        res.ok(Map.of("favorited", false));
    }

    // ==================== 浏览记录 ====================

    @Post("/visit")
    private void handleVisit(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.recordVisit(userId, articleId);
        res.ok();
    }
}
