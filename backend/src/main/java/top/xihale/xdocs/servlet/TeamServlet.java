package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import top.xihale.xdocs.constant.TeamRole;
import top.xihale.xdocs.service.TeamService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.util.ResponseUtils;

import java.io.IOException;

/**
 * TEAM 相关接口
 */
@WebServlet("/api/team/*")
public class TeamServlet extends BaseServlet {

    @Post("/create")
    private void handleCreate(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        String name = requiredParam(req, "name");
        String description = optionalParam(req, "description");

        var team = TeamService.createTeam(name, description, userId);
        res.ok(TeamService.buildTeamVO(team));
    }

    @Get("/list")
    private void handleList(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        var teams = TeamService.findTeamsByUserId(userId);
        res.ok(TeamService.buildTeamVOList(teams));
    }

    @Get("/pending-invites")
    private void handlePendingInvites(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        res.ok(TeamService.buildPendingInviteList(userId));
    }

    @Get("/detail")
    private void handleDetail(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int teamId = requiredIntParam(req, "id");
        var team = TeamService.findTeamById(teamId);
        res.ok(TeamService.buildTeamVO(team));
    }

    @Post("/invite")
    private void handleInvite(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = resolveUserId(req);

        TeamService.inviteMember(teamId, userId, operatorId);
        res.ok();
    }

    @Post("/accept")
    private void handleAccept(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.acceptInvite(teamId, userId);
        res.ok();
    }

    @Post("/reject")
    private void handleReject(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.rejectInvite(teamId, userId);
        res.ok();
    }

    @Post("/quit")
    private void handleQuit(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.removeMember(teamId, userId, userId);
        res.ok();
    }

    @Post("/update-role")
    private void handleUpdateRole(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        int role = TeamRole.parse(optionalParamOrDefault(req, "role", null));

        TeamService.updateMemberRole(teamId, userId, role, operatorId);
        res.ok();
    }

    @Get("/members")
    private void handleMembers(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int teamId = requiredIntParam(req, "id");
        TeamService.findTeamById(teamId); // 校验存在
        res.ok(TeamService.buildMemberVOList(teamId));
    }

    @Post("/kick")
    private void handleKick(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        TeamService.removeMember(teamId, userId, operatorId);
        res.ok();
    }

    @Post("/cancel-invite")
    private void handleCancelInvite(HttpServletRequest req, ResponseUtils.HttpResponse res) throws IOException {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        TeamService.cancelInvite(teamId, userId, operatorId);
        res.ok();
    }
}
