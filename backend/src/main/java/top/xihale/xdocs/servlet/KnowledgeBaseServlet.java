package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.constant.KnowledgeBaseRole;
import top.xihale.xdocs.constant.OwnerType;
import top.xihale.xdocs.constant.Visibility;
import top.xihale.xdocs.service.KnowledgeBaseService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Put;
import top.xihale.xdocs.util.Result;

/**
 * 知识库相关接口
 */
@WebServlet("/api/kb/*")
public class KnowledgeBaseServlet extends BaseServlet {

    @Post("/create")
    private Result<?> handleCreate(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String name = requiredParam(req, "name");
        String description = optionalParam(req, "description");
        int visibility = Visibility.parse(optionalParamOrDefault(req, "visibility", null));
        int ownerType = OwnerType.parse(optionalParamOrDefault(req, "ownerType", null));
        int ownerId = ownerType == OwnerType.TEAM.getCode() ? requiredIntParam(req, "ownerId") : userId;

        var kb = KnowledgeBaseService.createKnowledgeBase(name, description, visibility, ownerType, ownerId, userId);
        return Result.success(KnowledgeBaseService.toVO(kb));
    }

    @Put("/update")
    private Result<?> handleUpdate(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "id");
        String name = optionalParam(req, "name");
        String description = optionalParam(req, "description");

        KnowledgeBaseService.updateKnowledgeBase(kbId, name, description, userId);
        var kb = KnowledgeBaseService.findKnowledgeBaseById(kbId);
        return Result.success(KnowledgeBaseService.toVO(kb));
    }

    @Delete("/delete")
    private Result<?> handleDelete(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "id");
        KnowledgeBaseService.deleteKnowledgeBase(kbId, userId);
        return Result.success();
    }

    @Get("/detail")
    private Result<?> handleDetail(HttpServletRequest req, HttpServletResponse resp) {
        int kbId = requiredIntParam(req, "id");
        var kb = KnowledgeBaseService.findKnowledgeBaseById(kbId);
        return Result.success(KnowledgeBaseService.toVO(kb));
    }

    @Get("/list")
    private Result<?> handleList(HttpServletRequest req, HttpServletResponse resp) {
        int ownerType = OwnerType.parse(optionalParamOrDefault(req, "ownerType", null));
        int ownerId = requiredIntParam(req, "ownerId");

        var list = KnowledgeBaseService.findByOwner(ownerType, ownerId);
        return Result.success(KnowledgeBaseService.toVOList(list));
    }

    @Get("/list-mine")
    private Result<?> handleListMine(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        var list = KnowledgeBaseService.findByOwner(OwnerType.USER.getCode(), userId);
        return Result.success(KnowledgeBaseService.toVOList(list));
    }

    @Get("/members")
    private Result<?> handleMembers(HttpServletRequest req, HttpServletResponse resp) {
        int kbId = requiredIntParam(req, "id");
        KnowledgeBaseService.findKnowledgeBaseById(kbId); // 校验存在
        return Result.success(KnowledgeBaseService.buildMemberVOList(kbId));
    }

    @Post("/remove-member")
    private Result<?> handleRemoveMember(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");
        KnowledgeBaseService.removeMember(kbId, userId, operatorId);
        return Result.success();
    }

    @Post("/authorize")
    private Result<?> handleAuthorize(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        int userId = resolveUserId(req);
        int role = KnowledgeBaseRole.parse(optionalParamOrDefault(req, "role", null));

        KnowledgeBaseService.authorizeMember(kbId, userId, role, operatorId);
        return Result.success();
    }

    @Post("/accept")
    private Result<?> handleAccept(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        KnowledgeBaseService.acceptInvite(kbId, userId);
        return Result.success();
    }

    @Post("/reject")
    private Result<?> handleReject(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        KnowledgeBaseService.rejectInvite(kbId, userId);
        return Result.success();
    }

    @Post("/cancel-invite")
    private Result<?> handleCancelInvite(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");

        KnowledgeBaseService.cancelInvite(kbId, userId, operatorId);
        return Result.success();
    }

    @Post("/update-role")
    private Result<?> handleUpdateRole(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");
        int role = KnowledgeBaseRole.parse(optionalParamOrDefault(req, "role", null));

        KnowledgeBaseService.updateMemberRole(kbId, userId, role, operatorId);
        return Result.success();
    }

    @Get("/pending-invites")
    private Result<?> handlePendingInvites(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        return Result.success(KnowledgeBaseService.buildPendingInviteList(userId));
    }
}
