package top.xihale.clouddoc.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.clouddoc.service.ArticleService;
import top.xihale.clouddoc.service.UserService;
import top.xihale.clouddoc.servlet.route.Get;
import top.xihale.clouddoc.servlet.route.Public;
import top.xihale.clouddoc.util.ResponseUtils;

import java.io.IOException;
import java.util.*;

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
        res.ok(articles);
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
        // 脱敏：不返回密码
        List<Map<String, Object>> voList = new ArrayList<>();
        for (var user : users) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", user.getId());
            vo.put("username", user.getUsername());
            vo.put("nickname", user.getNickname());
            vo.put("avatarUrl", user.getAvatarUrl());
            voList.add(vo);
        }
        res.ok(voList);
    }
}
