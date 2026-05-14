package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.service.KnowledgeBaseService;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.util.Result;

/**
 * 全局搜索接口
 */
@WebServlet("/api/search/*")
public class SearchServlet extends BaseServlet {

    /**
     * 搜索公开文章
     */
    @Public
    @Get("/articles")
    private Result<?> handleSearchArticles(HttpServletRequest req, HttpServletResponse resp) {
        String keyword = requiredParam(req, "keyword");
        int offset = optionalIntParamOrDefault(req, "offset", 0);
        int limit = optionalIntParamOrDefault(req, "limit", 20);

        var articles = ArticleService.searchPublicArticles(keyword, offset, limit);
        var voList = articles.stream().map(ArticleService::toVO).toList();
        return Result.success(voList);
    }

    /**
     * 搜索公开知识库
     */
    @Public
    @Get("/kbs")
    private Result<?> handleSearchKbs(HttpServletRequest req, HttpServletResponse resp) {
        String keyword = requiredParam(req, "keyword");
        var all = ArticleService.searchPublicKbs(keyword);
        return Result.success(KnowledgeBaseService.toVOList(all));
    }

    /**
     * 搜索用户
     */
    @Public
    @Get("/users")
    private Result<?> handleSearchUsers(HttpServletRequest req, HttpServletResponse resp) {
        String keyword = requiredParam(req, "keyword");
        var users = UserService.searchByKeyword(keyword);
        return Result.success(users.stream().map(top.xihale.xdocs.po.User::toVO).toList());
    }
}
