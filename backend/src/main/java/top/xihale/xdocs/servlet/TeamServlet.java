package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.xihale.xdocs.constant.TeamRole;
import top.xihale.xdocs.service.TeamService;
import top.xihale.xdocs.servlet.route.Get;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.util.Result;

/**
 * TEAM 相关接口
 */
@WebServlet("/api/team/*")
public class TeamServlet extends BaseServlet {

    @Post("/create")
    private Result<?> handleCreate(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        String name = requiredParam(req, "name");
        String description = optionalParam(req, "description");

        var team = TeamService.createTeam(name, description, userId);
        return Result.success(TeamService.buildTeamVO(team));
    }

    @Get("/list")
    private Result<?> handleList(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        var teams = TeamService.findTeamsByUserId(userId);
        return Result.success(TeamService.buildTeamVOList(teams));
    }

    @Get("/pending-invites")
    private Result<?> handlePendingInvites(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        return Result.success(TeamService.buildPendingInviteList(userId));
    }

    @Get("/detail")
    private Result<?> handleDetail(HttpServletRequest req, HttpServletResponse resp) {
        int teamId = requiredIntParam(req, "id");
        var team = TeamService.findTeamById(teamId);
        return Result.success(TeamService.buildTeamVO(team));
    }

    @Post("/invite")
    private Result<?> handleInvite(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = resolveUserId(req);

        TeamService.inviteMember(teamId, userId, operatorId);
        return Result.success();
    }

    @Post("/accept")
    private Result<?> handleAccept(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.acceptInvite(teamId, userId);
        return Result.success();
    }

    @Post("/reject")
    private Result<?> handleReject(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.rejectInvite(teamId, userId);
        return Result.success();
    }

    @Post("/quit")
    private Result<?> handleQuit(HttpServletRequest req, HttpServletResponse resp) {
        int userId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");

        TeamService.removeMember(teamId, userId, userId);
        return Result.success();
    }

    @Post("/update-role")
    private Result<?> handleUpdateRole(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        int role = TeamRole.parse(optionalParamOrDefault(req, "role", null));

        TeamService.updateMemberRole(teamId, userId, role, operatorId);
        return Result.success();
    }

    @Get("/members")
    private Result<?> handleMembers(HttpServletRequest req, HttpServletResponse resp) {
        int teamId = requiredIntParam(req, "id");
        TeamService.findTeamById(teamId); // 校验存在
        return Result.success(TeamService.buildMemberVOList(teamId));
    }

    @Post("/kick")
    private Result<?> handleKick(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        TeamService.removeMember(teamId, userId, operatorId);
        return Result.success();
    }

    @Post("/cancel-invite")
    private Result<?> handleCancelInvite(HttpServletRequest req, HttpServletResponse resp) {
        int operatorId = getRequiredUserId(req);
        int teamId = requiredIntParam(req, "teamId");
        int userId = requiredIntParam(req, "userId");
        TeamService.cancelInvite(teamId, userId, operatorId);
        return Result.success();
    }
}
