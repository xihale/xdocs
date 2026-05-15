package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.util.Result;
import top.xihale.xdocs.websocket.RoomManager;

/**
 * 协同编辑辅助接口。
 */
@WebServlet("/api/collaboration-meta/*")
public class CollaborationServlet extends BaseServlet {

    /**
     * 返回当前客户端是否应该把数据库中的初始内容写入 Yjs 文档。
     * 同一 docId 的在线房间只允许一个客户端初始化，避免多客户端同时插入导致内容重复。
     */
    @Get("/claim-bootstrap")
    private Result<?> handleClaimBootstrap(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String docId = requiredParam(req, "docId");
        return Result.success(RoomManager.getInstance().claimBootstrap(docId, userId));
    }
}
