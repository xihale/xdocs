import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { teamApi, kbApi } from "../api";
import { useAuthStore } from "../stores/auth";
import type { TeamVO, TeamMemberVO, KnowledgeBase } from "../api/types";
import { TeamRole, OwnerType, Visibility, JoinStatus } from "../api/types";
import { ConfirmModal } from "../components/Modal";
import {
  ChevronLeft,
  UserPlus,
  Shield,
  ShieldCheck,
  User,
  LogOut,
  Trash2,
  Loader2,
  BookOpen,
  Plus,
  X,
  Clock,
  XCircle,
} from "lucide-react";

const ROLE_LABEL: Record<number, string> = {
  [TeamRole.OWNER]: "Owner",
  [TeamRole.ADMIN]: "Admin",
  [TeamRole.MEMBER]: "Member",
};

const ROLE_ICON: Record<number, typeof Shield> = {
  [TeamRole.OWNER]: ShieldCheck,
  [TeamRole.ADMIN]: Shield,
  [TeamRole.MEMBER]: User,
};

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [team, setTeam] = useState<TeamVO | null>(null);
  const [members, setMembers] = useState<TeamMemberVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [inviteUsername, setInviteUsername] = useState("");
  const [inviting, setInviting] = useState(false);
  const [showInvite, setShowInvite] = useState(false);
  
  const [memberToKick, setMemberToKick] = useState<number | null>(null);
  const [showQuitConfirm, setShowQuitConfirm] = useState(false);
  const [inviteToCancel, setInviteToCancel] = useState<number | null>(null);

  // 知识库相关状态
  const [kbs, setKbs] = useState<KnowledgeBase[]>([]);
  const [showCreateKb, setShowCreateKb] = useState(false);
  const [kbName, setKbName] = useState("");
  const [kbDesc, setKbDesc] = useState("");
  const [kbVisibility, setKbVisibility] = useState<number>(Visibility.PRIVATE);
  const [creatingKb, setCreatingKb] = useState(false);

  const teamId = Number(id);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      const [t, m, k] = await Promise.all([
        teamApi.detail(Number(id)),
        teamApi.members(Number(id)),
        kbApi.listByOwner(OwnerType.TEAM, Number(id)),
      ]);
      setTeam(t);
      setMembers(m);
      setKbs(k);
    } catch {
      navigate("/workspace");
    } finally {
      setLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading || !team) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 text-primary animate-spin" />
      </div>
    );
  }

  const myMembership = members.find((m) => m.userId === user?.id);
  const isInvited = myMembership?.joinStatus === JoinStatus.INVITED;
  const isOwnerOrAdmin =
    !isInvited &&
    (myMembership?.role === TeamRole.OWNER ||
      myMembership?.role === TeamRole.ADMIN);
  const isOwner = !isInvited && myMembership?.role === TeamRole.OWNER;

  const handleInvite = async () => {
    if (!inviteUsername.trim()) return;
    setInviting(true);
    try {
      await teamApi.invite(teamId, undefined, inviteUsername.trim());
      setInviteUsername("");
      setShowInvite(false);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "邀请失败");
    } finally {
      setInviting(false);
    }
  };

  const handleAcceptInvite = async () => {
    try {
      await teamApi.accept(teamId);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleRejectInvite = async () => {
    if (!confirm("确认拒绝该团队邀请？")) return;
    try {
      await teamApi.reject(teamId);
      navigate("/workspace");
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleKick = async (userId: number) => {
    setMemberToKick(userId);
  };

  const confirmKick = async () => {
    if (memberToKick === null) return;
    try {
      await teamApi.kick(teamId, memberToKick);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    } finally {
      setMemberToKick(null);
    }
  };

  const handleUpdateRole = async (userId: number, role: number) => {
    try {
      await teamApi.updateRole(teamId, userId, role);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleQuit = async () => {
    setShowQuitConfirm(true);
  };

  const confirmQuit = async () => {
    try {
      await teamApi.quit(teamId);
      navigate("/workspace");
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    } finally {
      setShowQuitConfirm(false);
    }
  };

  const handleCancelInvite = async () => {
    if (inviteToCancel === null) return;
    try {
      await teamApi.cancelInvite(teamId, inviteToCancel);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    } finally {
      setInviteToCancel(null);
    }
  };

  const handleCreateKb = async () => {
    if (!kbName.trim()) return;
    setCreatingKb(true);
    try {
      await kbApi.create(kbName.trim(), kbDesc.trim() || undefined, kbVisibility, OwnerType.TEAM, teamId);
      setShowCreateKb(false);
      setKbName("");
      setKbDesc("");
      setKbVisibility(Visibility.PRIVATE);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "创建失败");
    } finally {
      setCreatingKb(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <button
        onClick={() => navigate("/workspace")}
        className="flex items-center gap-1 text-sm text-on-surface-variant hover:text-primary mb-6"
      >
        <ChevronLeft className="w-4 h-4" /> 返回工作台
      </button>

      {/* Invite banner for invited user */}
      {isInvited && (
        <div className="bg-primary/5 border border-primary/20 p-4 mb-6 flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-on-surface">你被邀请加入该团队</p>
            <p className="text-xs text-on-surface-variant mt-0.5">
              {team.ownerName || `用户${team.ownerId}`} 邀请你加入「{team.name}」
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleRejectInvite}
              className="px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container border border-outline-variant"
            >
              拒绝
            </button>
            <button
              onClick={handleAcceptInvite}
              className="px-4 py-1.5 bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-all"
            >
              同意
            </button>
          </div>
        </div>
      )}

      {/* Team Header */}
      <div className="bg-surface-container-lowest border border-outline-variant p-6 mb-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-on-surface mb-1">
              {team.name}
            </h1>
            {team.description && (
              <p className="text-on-surface-variant text-sm">
                {team.description}
              </p>
            )}
            <div className="flex items-center gap-3 text-xs text-on-surface-variant mt-3">
              <span>
                创建者: {team.ownerName || `用户${team.ownerId}`}
              </span>
              <span>{members.filter((m) => m.joinStatus === JoinStatus.ACCEPTED).length} 成员</span>
            </div>
          </div>
          {!isInvited && myMembership && (
            <button
              onClick={handleQuit}
              className="flex items-center gap-1 px-3 py-1.5 text-sm text-on-surface-variant hover:text-error hover:bg-error/5 transition-colors"
            >
              <LogOut className="w-4 h-4" /> 退出
            </button>
          )}
        </div>
      </div>

      {/* Knowledge Bases */}
      <div className="bg-surface-container-lowest border border-outline-variant mb-6">
        <div className="flex items-center justify-between p-4 border-b border-outline-variant">
          <h2 className="font-medium text-on-surface">知识库</h2>
          {myMembership && (
            <button
              onClick={() => setShowCreateKb(true)}
              className="flex items-center gap-1 px-3 py-1.5 bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-all"
            >
              <Plus className="w-3.5 h-3.5" /> 新建知识库
            </button>
          )}
        </div>

        {kbs.length === 0 ? (
          <p className="text-sm text-on-surface-variant p-4">暂无知识库</p>
        ) : (
          <div className="divide-y divide-outline-variant/50">
            {kbs.map((kb) => (
              <button
                key={kb.id}
                onClick={() => navigate(`/kb/${kb.id}`)}
                className="w-full text-left flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors"
              >
                <div className="w-9 h-9 bg-secondary/10 flex items-center justify-center">
                  <BookOpen className="w-4.5 h-4.5 text-secondary" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-on-surface truncate">{kb.name}</p>
                  {kb.description && (
                    <p className="text-xs text-on-surface-variant truncate">{kb.description}</p>
                  )}
                </div>
                <span
                  className={`shrink-0 px-2 py-0.5 rounded text-xs font-medium ${
                    kb.visibility === Visibility.PUBLIC
                      ? "bg-green-100 text-green-700"
                      : "bg-surface-container text-on-surface-variant"
                  }`}
                >
                  {kb.visibility === Visibility.PUBLIC ? "公开" : "私有"}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Create KB Modal */}
      {showCreateKb && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-surface-container-lowest w-full max-w-md mx-4">
            <div className="flex items-center justify-between p-4 border-b border-outline-variant">
              <h3 className="font-bold text-on-surface">新建知识库</h3>
              <button
                onClick={() => setShowCreateKb(false)}
                className="p-1.5 hover:bg-surface-container text-on-surface-variant"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="p-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-on-surface-variant mb-1">
                  名称 <span className="text-error">*</span>
                </label>
                <input
                  className="w-full px-3 py-2.5 border border-outline-variant text-sm focus:border-primary focus:ring-2 focus:ring-primary/10 outline-none"
                  placeholder="知识库名称"
                  value={kbName}
                  onChange={(e) => setKbName(e.target.value)}
                  autoFocus
                  onKeyDown={(e) => e.key === "Enter" && handleCreateKb()}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface-variant mb-1">
                  描述
                </label>
                <textarea
                  className="w-full px-3 py-2.5 border border-outline-variant text-sm focus:border-primary focus:ring-2 focus:ring-primary/10 outline-none resize-none"
                  rows={3}
                  placeholder="简要描述（可选）"
                  value={kbDesc}
                  onChange={(e) => setKbDesc(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-on-surface-variant mb-2">
                  可见性
                </label>
                <div className="flex gap-2">
                  <button
                    onClick={() => setKbVisibility(Visibility.PRIVATE)}
                    className={`flex-1 py-2 text-sm font-medium border transition-colors ${
                      kbVisibility === Visibility.PRIVATE
                        ? "border-primary bg-primary/5 text-primary"
                        : "border-outline-variant text-on-surface-variant hover:bg-surface-container"
                    }`}
                  >
                    私有
                  </button>
                  <button
                    onClick={() => setKbVisibility(Visibility.PUBLIC)}
                    className={`flex-1 py-2 text-sm font-medium border transition-colors ${
                      kbVisibility === Visibility.PUBLIC
                        ? "border-primary bg-primary/5 text-primary"
                        : "border-outline-variant text-on-surface-variant hover:bg-surface-container"
                    }`}
                  >
                    公开
                  </button>
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 p-4 border-t border-outline-variant">
              <button
                onClick={() => setShowCreateKb(false)}
                className="px-4 py-2 text-sm text-on-surface-variant hover:bg-surface-container"
              >
                取消
              </button>
              <button
                onClick={handleCreateKb}
                disabled={creatingKb || !kbName.trim()}
                className="px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
              >
                {creatingKb ? "..." : "创建"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Members */}
      <div className="bg-surface-container-lowest border border-outline-variant">
        <div className="flex items-center justify-between p-4 border-b border-outline-variant">
          <h2 className="font-medium text-on-surface">成员列表</h2>
          {isOwnerOrAdmin && (
            <button
              onClick={() => setShowInvite(!showInvite)}
              className="flex items-center gap-1 px-3 py-1.5 bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-all"
            >
              <UserPlus className="w-3.5 h-3.5" /> 邀请
            </button>
          )}
        </div>

        {/* Invite form */}
        {showInvite && (
          <div className="p-4 border-b border-outline-variant bg-surface-container-low">
            <div className="flex gap-2">
              <input
                className="flex-1 px-3 py-2 border border-outline-variant text-sm focus:border-primary outline-none"
                placeholder="输入用户名邀请"
                value={inviteUsername}
                onChange={(e) => setInviteUsername(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleInvite()}
              />
              <button
                onClick={handleInvite}
                disabled={inviting}
                className="px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
              >
                {inviting ? "..." : "确认"}
              </button>
            </div>
          </div>
        )}

        {/* Member list */}
        <div className="divide-y divide-outline-variant/50">
          {members
            .filter((m) => m.joinStatus === JoinStatus.ACCEPTED)
            .map((member) => {
            const RoleIcon = ROLE_ICON[member.role] || User;
            return (
              <div
                key={member.id}
                className="flex items-center justify-between px-4 py-3 hover:bg-surface-container-low transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                    {(member.nickname || member.username || "U")
                      .charAt(0)
                      .toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-on-surface">
                      {member.nickname || member.username || `用户${member.userId}`}
                    </p>
                    <p className="text-xs text-on-surface-variant">
                      @{member.username}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {/* Role badge */}
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${
                      member.role === TeamRole.OWNER
                        ? "bg-amber-100 text-amber-700"
                        : member.role === TeamRole.ADMIN
                          ? "bg-blue-100 text-blue-700"
                          : "bg-surface-container text-on-surface-variant"
                    }`}
                  >
                    <RoleIcon className="w-3 h-3" />
                    {ROLE_LABEL[member.role] || "Member"}
                  </span>

                  {/* Actions */}
                  {isOwner && member.role !== TeamRole.OWNER && (
                    <div className="flex items-center gap-1 ml-2">
                      {member.role !== TeamRole.ADMIN && (
                        <button
                          onClick={() => handleUpdateRole(member.userId, TeamRole.ADMIN)}
                          className="p-1.5 rounded hover:bg-blue-50 text-on-surface-variant hover:text-blue-600 transition-colors"
                          title="设为 Admin"
                        >
                          <Shield className="w-3.5 h-3.5" />
                        </button>
                      )}
                      {member.role === TeamRole.ADMIN && (
                        <button
                          onClick={() => handleUpdateRole(member.userId, TeamRole.MEMBER)}
                          className="p-1.5 rounded hover:bg-surface-container text-on-surface-variant hover:text-on-surface transition-colors"
                          title="设为 Member"
                        >
                          <User className="w-3.5 h-3.5" />
                        </button>
                      )}
                      <button
                        onClick={() => handleKick(member.userId)}
                        className="p-1.5 rounded hover:bg-error/10 text-on-surface-variant hover:text-error transition-colors"
                        title="移除成员"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Pending invites section */}
        {(() => {
          const pending = members.filter((m) => m.joinStatus === JoinStatus.INVITED);
          if (pending.length === 0) return null;
          return (
            <>
              <div className="flex items-center gap-2 px-4 py-2.5 bg-tertiary/5 border-t border-b border-outline-variant/50">
                <Clock className="w-3.5 h-3.5 text-on-surface-variant" />
                <span className="text-xs font-medium text-on-surface-variant">
                  待处理邀请 ({pending.length})
                </span>
              </div>
              <div className="divide-y divide-outline-variant/50">
                {pending.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center justify-between px-4 py-3 bg-tertiary/[0.03] hover:bg-tertiary/[0.06] transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-on-surface-variant/10 flex items-center justify-center text-on-surface-variant font-bold text-sm">
                        {(member.nickname || member.username || "U")
                          .charAt(0)
                          .toUpperCase()}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-on-surface-variant">
                          {member.nickname || member.username || `用户${member.userId}`}
                        </p>
                        <p className="text-xs text-on-surface-variant/60">
                          @{member.username}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-amber-50 text-amber-600">
                        <Clock className="w-3 h-3" />
                        待接受
                      </span>
                      {isOwnerOrAdmin && (
                        <button
                          onClick={() => setInviteToCancel(member.userId)}
                          className="p-1.5 rounded hover:bg-error/10 text-on-surface-variant hover:text-error transition-colors"
                          title="取消邀请"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </>
          );
        })()}
      </div>

      <ConfirmModal
        open={memberToKick !== null}
        onClose={() => setMemberToKick(null)}
        onConfirm={confirmKick}
        title="移除成员"
        message="确认移除该成员？此操作不可撤销。"
        confirmText="移除"
        danger
      />

      <ConfirmModal
        open={inviteToCancel !== null}
        onClose={() => setInviteToCancel(null)}
        onConfirm={handleCancelInvite}
        title="取消邀请"
        message="确认取消该邀请？取消后需重新邀请。"
        confirmText="取消邀请"
        danger
      />

      <ConfirmModal
        open={showQuitConfirm}
        onClose={() => setShowQuitConfirm(false)}
        onConfirm={confirmQuit}
        title="退出 TEAM"
        message="确认退出该 TEAM？退出后你将无法访问该 TEAM 的资源。"
        confirmText="确认退出"
        danger
      />
    </div>
  );
}
