package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.service.NotificationService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.util.Result;

/**
 * 通知相关接口
 */
@WebServlet("/api/notification/*")
public class NotificationServlet extends BaseServlet {

    @Get("/list")
    private Result<?> handleList(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int offset = optionalIntParamOrDefault(req, "offset", 0);
        int limit = optionalIntParamOrDefault(req, "limit", 20);
        return Result.success(NotificationService.listNotifications(userId, offset, limit));
    }

    @Get("/unread-count")
    private Result<?> handleUnreadCount(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        return Result.success(NotificationService.unreadCount(userId));
    }

    @Post("/read")
    private Result<?> handleRead(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int id = requiredIntParam(req, "id");
        NotificationService.markRead(id, userId);
        return Result.success();
    }

    @Post("/read-all")
    private Result<?> handleReadAll(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        NotificationService.markAllRead(userId);
        return Result.success();
    }

    @Delete("/delete")
    private Result<?> handleDelete(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int id = requiredIntParam(req, "id");
        NotificationService.deleteNotification(id, userId);
        return Result.success();
    }
}
