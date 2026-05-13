package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Public;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;

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
    private void handleSearchArticles(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String keyword = requiredParam(req, "keyword");
        int offset = optionalIntParamOrDefault(req, "offset", 0);
        int limit = optionalIntParamOrDefault(req, "limit", 20);

        var articles = ArticleService.searchPublicArticles(keyword, offset, limit);
        var voList = articles.stream().map(ArticleService::toVO).toList();
        res.ok(voList);
    }

    /**
     * 搜索公开知识库
     */
    @Public
    @Get("/kbs")
    private void handleSearchKbs(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String keyword = requiredParam(req, "keyword");
        var all = ArticleService.searchPublicKbs(keyword);
        res.ok(all);
    }

    /**
     * 搜索用户
     */
    @Public
    @Get("/users")
    private void handleSearchUsers(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        String keyword = requiredParam(req, "keyword");
        var users = UserService.searchByKeyword(keyword);
        res.ok(users.stream().map(top.xihale.xdocs.po.User::toVO).toList());
    }
}
