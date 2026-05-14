package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.constant.KnowledgeBaseRole;
import top.xihale.xdocs.constant.OwnerType;
import top.xihale.xdocs.constant.Visibility;
import top.xihale.xdocs.service.KnowledgeBaseService;
import top.xihale.xdocs.servlet.route.Delete;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.servlet.route.Put;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;

/**
 * 知识库相关接口
 */
@WebServlet("/api/kb/*")
public class KnowledgeBaseServlet extends BaseServlet {

    @Post("/create")
    private void handleCreate(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        String name = requiredParam(req, "name");
        String description = optionalParam(req, "description");
        int visibility = Visibility.parse(optionalParamOrDefault(req, "visibility", null));
        int ownerType = OwnerType.parse(optionalParamOrDefault(req, "ownerType", null));
        int ownerId = ownerType == OwnerType.TEAM.getCode() ? requiredIntParam(req, "ownerId") : userId;

        var kb = KnowledgeBaseService.createKnowledgeBase(name, description, visibility, ownerType, ownerId, userId);
        res.ok(KnowledgeBaseService.toVO(kb));
    }

    @Put("/update")
    private void handleUpdate(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "id");
        String name = optionalParam(req, "name");
        String description = optionalParam(req, "description");

        KnowledgeBaseService.updateKnowledgeBase(kbId, name, description, userId);
        var kb = KnowledgeBaseService.findKnowledgeBaseById(kbId);
        res.ok(KnowledgeBaseService.toVO(kb));
    }

    @Delete("/delete")
    private void handleDelete(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "id");
        KnowledgeBaseService.deleteKnowledgeBase(kbId, userId);
        res.ok();
    }

    @Get("/detail")
    private void handleDetail(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int kbId = requiredIntParam(req, "id");
        var kb = KnowledgeBaseService.findKnowledgeBaseById(kbId);
        res.ok(KnowledgeBaseService.toVO(kb));
    }

    @Get("/list")
    private void handleList(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int ownerType = OwnerType.parse(optionalParamOrDefault(req, "ownerType", null));
        int ownerId = requiredIntParam(req, "ownerId");

        var list = KnowledgeBaseService.findByOwner(ownerType, ownerId);
        res.ok(KnowledgeBaseService.toVOList(list));
    }

    @Get("/list-mine")
    private void handleListMine(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        var list = KnowledgeBaseService.findByOwner(OwnerType.USER.getCode(), userId);
        res.ok(KnowledgeBaseService.toVOList(list));
    }

    @Get("/members")
    private void handleMembers(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int kbId = requiredIntParam(req, "id");
        KnowledgeBaseService.findKnowledgeBaseById(kbId); // 校验存在
        res.ok(KnowledgeBaseService.buildMemberVOList(kbId));
    }

    @Post("/remove-member")
    private void handleRemoveMember(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");
        KnowledgeBaseService.removeMember(kbId, userId, operatorId);
        res.ok();
    }

    @Post("/authorize")
    private void handleAuthorize(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        int userId = resolveUserId(req);
        int role = KnowledgeBaseRole.parse(optionalParamOrDefault(req, "role", null));

        KnowledgeBaseService.authorizeMember(kbId, userId, role, operatorId);
        res.ok();
    }

    @Post("/accept")
    private void handleAccept(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        KnowledgeBaseService.acceptInvite(kbId, userId);
        res.ok();
    }

    @Post("/reject")
    private void handleReject(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");

        KnowledgeBaseService.rejectInvite(kbId, userId);
        res.ok();
    }

    @Post("/cancel-invite")
    private void handleCancelInvite(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");

        KnowledgeBaseService.cancelInvite(kbId, userId, operatorId);
        res.ok();
    }

    @Post("/update-role")
    private void handleUpdateRole(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int kbId = requiredIntParam(req, "kbId");
        int userId = requiredIntParam(req, "userId");
        int role = KnowledgeBaseRole.parse(optionalParamOrDefault(req, "role", null));

        KnowledgeBaseService.updateMemberRole(kbId, userId, role, operatorId);
        res.ok();
    }

    @Get("/pending-invites")
    private void handlePendingInvites(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        res.ok(KnowledgeBaseService.buildPendingInviteList(userId));
    }
}
