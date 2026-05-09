package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.exception.UserException;
import top.xihale.xdocs.exception.UserException.UserError;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.util.ResponseUtils;
import top.xihale.xdocs.vo.UserVO;

import java.io.IOException;
import java.util.*;

/**
 * 用户信息相关接口
 */
@WebServlet({"/api/user/*", "/api/users/*"})
public class UserServlet extends BaseServlet {

    @Get("")
    private void handleList(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        res.ok(UserService.findAll());
    }

    @Get("/profile")
    private void handleProfile(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        Integer id = optionalIntParam(req, "id");
        int userId = (id != null) ? id : getRequiredUserId(req);
        UserVO vo = UserService.buildUserProfile(userId, getOptionalUserId(req));
        res.ok(vo);
    }

    @Post("/update-nickname")
    private void handleUpdateNickname(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        String nickname = requiredParam(req, "nickname");
        UserService.updateNickname(userId, nickname);
        res.ok();
    }

    @Post("/update-avatar")
    private void handleUpdateAvatar(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        String avatarUrl = requiredParam(req, "avatarUrl");
        UserService.updateAvatar(userId, avatarUrl);
        res.ok();
    }

    @Post("/change-password")
    private void handleChangePassword(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        String oldPassword = requiredParam(req, "oldPassword");
        String newPassword = requiredParam(req, "newPassword");
        UserService.changePassword(userId, oldPassword, newPassword);
        res.ok();
    }

    // ==================== 关注 ====================

    @Post("/follow")
    private void handleFollow(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int followerId = getRequiredUserId(req);
        int followingId = requiredIntParam(req, "userId");
        if (followerId == followingId) {
            throw new UserException(UserError.CANNOT_FOLLOW_SELF);
        }
        UserService.follow(followerId, followingId);
        res.ok(Map.of("followed", true));
    }

    @Post("/unfollow")
    private void handleUnfollow(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int followerId = getRequiredUserId(req);
        int followingId = requiredIntParam(req, "userId");
        UserService.unfollow(followerId, followingId);
        res.ok(Map.of("followed", false));
    }

    @Get("/following")
    private void handleListFollowing(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        Integer uid = optionalIntParam(req, "userId");
        int userId = uid != null ? uid : getRequiredUserId(req);
        List<Integer> ids = UserService.findFollowingIds(userId);
        res.ok(UserService.buildUserVOList(ids));
    }

    @Get("/followers")
    private void handleListFollowers(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        Integer uid = optionalIntParam(req, "userId");
        int userId = uid != null ? uid : getRequiredUserId(req);
        List<Integer> ids = UserService.findFollowerIds(userId);
        res.ok(UserService.buildUserVOList(ids));
    }

    // ==================== 收藏 ====================

    @Get("/favorites")
    private void handleListFavorites(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        res.ok(ArticleService.buildFavoriteVOList(userId));
    }

    // ==================== 浏览记录 ====================

    @Get("/history")
    private void handleListHistory(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        res.ok(ArticleService.buildHistoryVOList(userId, 50));
    }

    @Delete("/history-delete")
    private void handleDeleteHistory(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        Integer articleId = optionalIntParam(req, "articleId");
        ArticleService.deleteVisitHistory(userId, articleId);
        res.ok();
    }
}
