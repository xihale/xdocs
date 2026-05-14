package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.service.ArticleService;
import top.xihale.xdocs.service.UserService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.util.Result;
import top.xihale.xdocs.vo.UserVO;

import java.util.*;

/**
 * 用户信息相关接口
 */
@WebServlet({"/api/user/*", "/api/users/*"})
public class UserServlet extends BaseServlet {

    @Get("")
    private Result<?> handleList(HttpServletRequest req, HttpServletResponse resp) {
        return Result.success(UserService.findAll());
    }

    @Get("/profile")
    private Result<?> handleProfile(HttpServletRequest req, HttpServletResponse resp) {
        Integer id = optionalIntParam(req, "id");
        int userId = (id != null) ? id : getRequiredUserId(req);
        UserVO vo = UserService.buildUserProfile(userId, getOptionalUserId(req));
        return Result.success(vo);
    }

    @Post("/update-nickname")
    private Result<?> handleUpdateNickname(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String nickname = requiredParam(req, "nickname");
        UserService.updateNickname(userId, nickname);
        return Result.success();
    }

    @Post("/update-avatar")
    private Result<?> handleUpdateAvatar(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String avatarUrl = requiredParam(req, "avatarUrl");
        UserService.updateAvatar(userId, avatarUrl);
        return Result.success();
    }

    @Post("/change-password")
    private Result<?> handleChangePassword(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String oldPassword = requiredParam(req, "oldPassword");
        String newPassword = requiredParam(req, "newPassword");
        UserService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    // ==================== 关注 ====================

    @Post("/follow")
    private Result<?> handleFollow(HttpServletRequest req, HttpServletResponse resp) {
        int followerId = getRequiredUserId(req);
        int followingId = requiredIntParam(req, "userId");
        UserService.follow(followerId, followingId);
        return Result.success(Map.of("followed", true));
    }

    @Post("/unfollow")
    private Result<?> handleUnfollow(HttpServletRequest req, HttpServletResponse resp) {
        int followerId = getRequiredUserId(req);
        int followingId = requiredIntParam(req, "userId");
        UserService.unfollow(followerId, followingId);
        return Result.success(Map.of("followed", false));
    }

    @Get("/following")
    private Result<?> handleListFollowing(HttpServletRequest req, HttpServletResponse resp) {
        Integer uid = optionalIntParam(req, "userId");
        int userId = uid != null ? uid : getRequiredUserId(req);
        List<Integer> ids = UserService.findFollowingIds(userId);
        return Result.success(UserService.buildUserVOList(ids));
    }

    @Get("/followers")
    private Result<?> handleListFollowers(HttpServletRequest req, HttpServletResponse resp) {
        Integer uid = optionalIntParam(req, "userId");
        int userId = uid != null ? uid : getRequiredUserId(req);
        List<Integer> ids = UserService.findFollowerIds(userId);
        return Result.success(UserService.buildUserVOList(ids));
    }

    // ==================== 收藏 ====================

    @Get("/favorites")
    private Result<?> handleListFavorites(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        return Result.success(ArticleService.buildFavoriteVOList(userId));
    }

    // ==================== 浏览记录 ====================

    @Get("/history")
    private Result<?> handleListHistory(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        return Result.success(ArticleService.buildHistoryVOList(userId, 50));
    }

    @Delete("/history-delete")
    private Result<?> handleDeleteHistory(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        Integer articleId = optionalIntParam(req, "articleId");
        ArticleService.deleteVisitHistory(userId, articleId);
        return Result.success();
    }
}
