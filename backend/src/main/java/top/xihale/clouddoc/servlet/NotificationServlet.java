package top.xihale.clouddoc.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.clouddoc.service.NotificationService;
import top.xihale.clouddoc.servlet.route.Get;
import top.xihale.clouddoc.servlet.route.Post;
import top.xihale.clouddoc.servlet.route.Delete;
import top.xihale.clouddoc.util.ResponseUtils;

import java.io.IOException;

/**
 * 通知相关接口
 */
@WebServlet("/api/notification/*")
public class NotificationServlet extends BaseServlet {

    @Get("/list")
    private void handleList(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int offset = optionalIntParamOrDefault(req, "offset", 0);
        int limit = optionalIntParamOrDefault(req, "limit", 20);
        res.ok(NotificationService.listNotifications(userId, offset, limit));
    }

    @Get("/unread-count")
    private void handleUnreadCount(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        res.ok(NotificationService.unreadCount(userId));
    }

    @Post("/read")
    private void handleRead(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int id = requiredIntParam(req, "id");
        NotificationService.markRead(id, userId);
        res.ok();
    }

    @Post("/read-all")
    private void handleReadAll(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        NotificationService.markAllRead(userId);
        res.ok();
    }

    @Delete("/delete")
    private void handleDelete(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int id = requiredIntParam(req, "id");
        NotificationService.deleteNotification(id, userId);
        res.ok();
    }
}
