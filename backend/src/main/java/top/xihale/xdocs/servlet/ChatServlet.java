package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.service.ChatService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.util.Result;
import top.xihale.xdocs.websocket.ChatWebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 聊天辅助接口（历史消息查询等）
 */
@WebServlet("/api/chat/*")
public class ChatServlet extends BaseServlet {

    @Get("/history")
    private Result<?> handleHistory(HttpServletRequest req, HttpServletResponse resp) {
        getRequiredUserId(req);
        int articleId = requiredIntParam(req, "articleId");
        int limit = optionalIntParamOrDefault(req, "limit", 50);

        return Result.success(ChatService.getHistoryWithVO(articleId, limit));
    }

    @Get("/online-members")
    private Result<?> handleOnlineMembers(HttpServletRequest req, HttpServletResponse resp) {
        getRequiredUserId(req);
        String articleId = requiredParam(req, "articleId");
        var users = ChatWebSocket.getOnlineUsers(articleId);
        List<Map<String, Object>> voList = new ArrayList<>();
        for (var u : users) {
            Map<String, Object> vo = new java.util.LinkedHashMap<>();
            vo.put("userId", u.userId());
            vo.put("nickname", u.nickname());
            vo.put("avatarUrl", u.avatarUrl());
            voList.add(vo);
        }
        return Result.success(voList);
    }

}
