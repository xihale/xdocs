package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.service.ArticleService;
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
        res.ok(ArticleService.toVO(article, userId));
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

        ArticleService.updateArticle(articleId, title, content, summary, status, userId);
        var article = ArticleService.findArticleById(articleId);
        res.ok(ArticleService.toVO(article, userId));
    }

    @Delete("/delete")
    private void handleDelete(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int articleId = requiredIntParam(req, "id");
        ArticleService.deleteArticle(articleId, userId);
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
        Integer userId = getOptionalUserId(req);
        var articles = ArticleService.findByKnowledgeBase(kbId);
        var voList = articles.stream().map(a -> ArticleService.toVO(a, userId)).toList();
        res.ok(voList);
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

        var article = ArticleService.saveArticle(articleId, content, userId);
        res.ok(ArticleService.toVO(article, userId));
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
