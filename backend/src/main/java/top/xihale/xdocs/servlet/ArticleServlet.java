package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.servlet.route.Put;
import top.xihale.xdocs.util.HtmlSanitizer;
import top.xihale.xdocs.util.Result;
import top.xihale.xdocs.vo.ArticleVO;

import java.util.*;

/**
 * 文章相关接口
 */
@WebServlet("/api/article/*")
public class ArticleServlet extends BaseServlet {

    @Post("/create")
    private Result<?> handleCreate(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        String title = requiredParam(req, "title");
        String content = optionalRawParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);

        var article = ArticleService.createArticle(kbId, title, content, userId);
        return Result.success(ArticleService.toVO(article, userId));
    }

    @Put("/update")
    private Result<?> handleUpdate(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        String title = optionalParam(req, "title");
        String content = optionalRawParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);
        String summary = optionalParam(req, "summary");
        Integer status = optionalIntParam(req, "status");

        ArticleService.updateArticle(articleId, title, content, summary, status, userId);
        var article = ArticleService.findArticleById(articleId);
        return Result.success(ArticleService.toVO(article, userId));
    }

    @Delete("/delete")
    private Result<?> handleDelete(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        ArticleService.deleteArticle(articleId, userId);
        return Result.success();
    }

    @Public
    @Get("/detail")
    private Result<?> handleDetail(HttpServletRequest req, HttpServletResponse resp) {
        int articleId = requiredIntParam(req, "id");
        var article = ArticleService.findArticleById(articleId);
        Integer userId = getOptionalUserId(req);
        return Result.success(ArticleService.toVO(article, userId));
    }

    @Get("/list")
    private Result<?> handleListByKb(HttpServletRequest req, HttpServletResponse resp) {
        int kbId = requiredIntParam(req, "kbId");
        Integer userId = getOptionalUserId(req);
        var articles = ArticleService.findByKnowledgeBase(kbId);
        var voList = articles.stream().map(a -> ArticleService.toVO(a, userId)).toList();
        return Result.success(voList);
    }

    @Public
    @Get("/public-list")
    private Result<?> handlePublicList(HttpServletRequest req, HttpServletResponse resp) {
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
        return Result.success(voList);
    }

    @Post("/save")
    private Result<?> handleSave(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        String content = optionalRawParam(req, "content");
        if (content != null) content = HtmlSanitizer.sanitizeArticleContent(content);

        var article = ArticleService.saveArticle(articleId, content, userId);
        return Result.success(ArticleService.toVO(article, userId));
    }

    // ==================== 点赞 ====================

    @Post("/like")
    private Result<?> handleLike(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        return Result.success(ArticleService.likeArticle(articleId, userId));
    }

    @Post("/unlike")
    private Result<?> handleUnlike(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        return Result.success(ArticleService.unlikeArticle(articleId, userId));
    }

    // ==================== 评论 ====================

    @Post("/comment")
    private Result<?> handleAddComment(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        String content = requiredParam(req, "content");
        content = HtmlSanitizer.stripHtml(content);
        Integer parentId = optionalIntParam(req, "parentId");
        Integer replyToId = optionalIntParam(req, "replyToId");

        int commentId = ArticleService.addComment(articleId, userId, content, parentId, replyToId);
        return Result.success(Map.of("id", commentId));
    }

    @Public
    @Get("/comments")
    private Result<?> handleListComments(HttpServletRequest req, HttpServletResponse resp) {
        int articleId = requiredIntParam(req, "articleId");
        return Result.success(ArticleService.listComments(articleId));
    }

    @Delete("/comment-delete")
    private Result<?> handleDeleteComment(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int commentId = requiredIntParam(req, "id");
        ArticleService.deleteComment(commentId, userId);
        return Result.success();
    }

    // ==================== 收藏 ====================

    @Post("/favorite")
    private Result<?> handleFavorite(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.favoriteArticle(userId, articleId);
        return Result.success(Map.of("favorited", true));
    }

    @Post("/unfavorite")
    private Result<?> handleUnfavorite(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.unfavoriteArticle(userId, articleId);
        return Result.success(Map.of("favorited", false));
    }

    // ==================== 浏览记录 ====================

    @Post("/visit")
    private Result<?> handleVisit(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        ArticleService.recordVisit(userId, articleId);
        return Result.success();
    }
}
