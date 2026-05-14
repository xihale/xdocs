package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.service.ChatService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.util.ResponseUtils;
import top.xihale.xdocs.websocket.ChatWebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 聊天辅助接口（历史消息查询等）
 */
@WebServlet("/api/chat/*")
public class ChatServlet extends BaseServlet {

    @Get("/history")
    private void handleHistory(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int articleId = requiredIntParam(req, "articleId");
        int limit = optionalIntParamOrDefault(req, "limit", 50);

        res.ok(ChatService.getHistoryWithVO(articleId, limit));
    }

    @Get("/online-members")
    private void handleOnlineMembers(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
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
        res.ok(voList);
    }

}
