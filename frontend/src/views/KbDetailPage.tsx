import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { kbApi, articleApi } from "../api";
import { useAuthStore } from "../stores/auth";
import type { KnowledgeBase, ArticleVO, KbMemberVO } from "../api/types";
import { KbRole, OwnerType, Visibility, JoinStatus } from "../api/types";
import { ConfirmModal } from "../components/Modal";
import {
  ChevronLeft,
  Plus,
  FileText,
  UserPlus,
  Shield,
  Edit3,
  Trash2,
  Loader2,
  Clock,
  XCircle,
} from "lucide-react";

const KB_ROLE_LABEL: Record<number, string> = {
  [KbRole.OWNER]: "Owner",
  [KbRole.ADMIN]: "Admin",
  [KbRole.EDITOR]: "Editor",
  [KbRole.VIEWER]: "Viewer",
};

export function KbDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [kb, setKb] = useState<KnowledgeBase | null>(null);
  const [articles, setArticles] = useState<ArticleVO[]>([]);
  const [members, setMembers] = useState<KbMemberVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<"articles" | "members">("articles");
  const [showInvite, setShowInvite] = useState(false);
  const [inviteUsername, setInviteUsername] = useState("");
  const [inviteRole, setInviteRole] = useState<number>(KbRole.EDITOR);
  const [inviting, setInviting] = useState(false);

  const [memberToRemove, setMemberToRemove] = useState<number | null>(null);
  const [articleToDelete, setArticleToDelete] = useState<number | null>(null);
  const [inviteToCancel, setInviteToCancel] = useState<number | null>(null);

  const kbId = Number(id);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      const [k, a, m] = await Promise.all([
        kbApi.detail(Number(id)),
        articleApi.listByKb(Number(id)),
        kbApi.members(Number(id)),
      ]);
      setKb(k);
      setArticles(a);
      setMembers(m);
    } catch {
      navigate("/workspace");
    } finally {
      setLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading || !kb) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 text-primary animate-spin" />
      </div>
    );
  }

  const myMembership = members.find((m) => m.userId === user?.id);
  const isInvited = myMembership?.inviteStatus === JoinStatus.INVITED;
  const isOwnerOrAdmin =
    !isInvited &&
    (myMembership?.role === KbRole.OWNER || myMembership?.role === KbRole.ADMIN);
  const isOwner = !isInvited && myMembership?.role === KbRole.OWNER;

  const handleCreateArticle = async () => {
    try {
      const article = await articleApi.create(kb.id, "未命名文章");
      navigate(`/article/${article.id}/edit`);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "创建失败");
    }
  };

  const handleAuthorize = async () => {
    const val = inviteUsername.trim();
    if (!val) return;
    setInviting(true);
    try {
      await kbApi.authorize(kbId, undefined, val, inviteRole);
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
      await kbApi.accept(kbId);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleRejectInvite = async () => {
    if (!confirm("确认拒绝该知识库邀请？")) return;
    try {
      await kbApi.reject(kbId);
      navigate("/workspace");
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleRemoveMember = async (userId: number) => {
    setMemberToRemove(userId);
  };

  const confirmRemoveMember = async () => {
    if (memberToRemove === null) return;
    try {
      await kbApi.removeMember(kbId, memberToRemove);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    } finally {
      setMemberToRemove(null);
    }
  };

  const handleCancelInvite = async () => {
    if (inviteToCancel === null) return;
    try {
      await kbApi.cancelInvite(kbId, inviteToCancel);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    } finally {
      setInviteToCancel(null);
    }
  };

  const handleUpdateRole = async (userId: number, role: number) => {
    try {
      await kbApi.updateRole(kbId, userId, role);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "操作失败");
    }
  };

  const handleDeleteArticle = async (articleId: number) => {
    setArticleToDelete(articleId);
  };

  const confirmDeleteArticle = async () => {
    if (articleToDelete === null) return;
    try {
      await articleApi.delete(articleToDelete);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "删除失败");
    } finally {
      setArticleToDelete(null);
    }
  };

  return (
    <div>
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
            <p className="text-sm font-medium text-on-surface">你被邀请加入该知识库</p>
            <p className="text-xs text-on-surface-variant mt-0.5">
              你被邀请加入知识库「{kb.name}」
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

      {/* KB Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface">{kb.name}</h1>
          {kb.description && (
            <p className="text-on-surface-variant text-sm mt-1">
              {kb.description}
            </p>
          )}
          <div className="flex items-center gap-3 text-xs text-on-surface-variant mt-2">
            <span
              className={`px-2 py-0.5 rounded ${
                kb.visibility === Visibility.PUBLIC
                  ? "bg-green-100 text-green-700"
                  : "bg-surface-container text-on-surface-variant"
              }`}
            >
              {kb.visibility === Visibility.PUBLIC ? "公开" : "私有"}
            </span>
            {kb.ownerType === OwnerType.TEAM && (
              <span className="bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                TEAM 知识库
              </span>
            )}
          </div>
        </div>
        {isOwnerOrAdmin && (
          <button
            onClick={handleCreateArticle}
            className="flex items-center gap-1 px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-all"
          >
            <Plus className="w-4 h-4" /> 新建文章
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-outline-variant mb-6">
        <button
          onClick={() => setTab("articles")}
          className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
            tab === "articles"
              ? "text-primary border-primary"
              : "text-on-surface-variant border-transparent hover:text-on-surface"
          }`}
        >
          文章 ({articles.length})
        </button>
        <button
          onClick={() => setTab("members")}
          className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
            tab === "members"
              ? "text-primary border-primary"
              : "text-on-surface-variant border-transparent hover:text-on-surface"
          }`}
        >
          成员 ({members.length})
        </button>
      </div>

      {/* Articles tab */}
      {tab === "articles" && (
        <>
          {articles.length === 0 ? (
            <div className="text-center py-20 text-on-surface-variant text-sm">
              暂无文章，点击右上角新建
            </div>
          ) : (
            <div className="space-y-3">
              {articles.map((article) => (
                <div
                  key={article.id}
                  className="bg-surface-container-lowest border border-outline-variant p-4 transition-all flex items-center gap-4"
                >
                  <FileText className="w-5 h-5 text-on-surface-variant shrink-0" />
                  <button
                    onClick={() => navigate(`/article/${article.id}`)}
                    className="flex-1 text-left min-w-0"
                  >
                    <h3 className="font-medium text-on-surface truncate">
                      {article.title}
                    </h3>
                    <p className="text-xs text-on-surface-variant mt-0.5">
                      {article.authorName || `用户${article.authorId}`} ·{" "}
                      {article.updateTime}
                    </p>
                  </button>
                  <span
                    className={`text-xs px-2 py-0.5 rounded ${
                      article.status === 1
                        ? "bg-green-100 text-green-700"
                        : "bg-surface-container text-on-surface-variant"
                    }`}
                  >
                    {article.status === 1 ? "已发布" : "草稿"}
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() =>
                        navigate(`/article/${article.id}/edit`)
                      }
                      className="p-1.5 rounded hover:bg-surface-container text-on-surface-variant hover:text-primary transition-colors"
                      title="编辑"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                    </button>
                    {isOwnerOrAdmin && (
                      <button
                        onClick={() => handleDeleteArticle(article.id)}
                        className="p-1.5 rounded hover:bg-error/10 text-on-surface-variant hover:text-error transition-colors"
                        title="删除"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Members tab */}
      {tab === "members" && (
        <div className="bg-surface-container-lowest border border-outline-variant">
          {isOwnerOrAdmin && (
            <div className="flex items-center justify-between p-4 border-b border-outline-variant">
              <span className="text-sm text-on-surface-variant">
                管理知识库成员与权限
              </span>
              <button
                onClick={() => setShowInvite(!showInvite)}
                className="flex items-center gap-1 px-3 py-1.5 bg-primary text-on-primary text-sm font-medium hover:opacity-90 transition-all"
              >
                <UserPlus className="w-3.5 h-3.5" /> 邀请成员
              </button>
            </div>
          )}

          {showInvite && (
            <div className="p-4 border-b border-outline-variant bg-surface-container-low space-y-3">
              <div className="flex gap-2">
                <input
                  className="flex-1 px-3 py-2 border border-outline-variant text-sm focus:border-primary outline-none"
                  placeholder="输入用户名邀请"
                  value={inviteUsername}
                  onChange={(e) => setInviteUsername(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleAuthorize()}
                />
                <select
                  className="px-3 py-2 border border-outline-variant text-sm focus:border-primary outline-none bg-surface-container-lowest"
                  value={inviteRole}
                  onChange={(e) => setInviteRole(Number(e.target.value))}
                >
                  <option value={KbRole.ADMIN}>Admin</option>
                  <option value={KbRole.EDITOR}>Editor</option>
                  <option value={KbRole.VIEWER}>Viewer</option>
                </select>
              </div>
              <div className="flex justify-end gap-2">
                <button
                  onClick={() => setShowInvite(false)}
                  className="px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container"
                >
                  取消
                </button>
                <button
                  onClick={handleAuthorize}
                  disabled={inviting}
                  className="px-4 py-1.5 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
                >
                  {inviting ? "..." : "确认邀请"}
                </button>
              </div>
            </div>
          )}

          {/* Accepted members */}
          <div className="divide-y divide-outline-variant/50">
            {members
              .filter((m) => m.inviteStatus === JoinStatus.ACCEPTED)
              .map((member) => (
                <div
                  key={member.id}
                  className="flex items-center justify-between px-4 py-3 hover:bg-surface-container-low transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-secondary/10 flex items-center justify-center text-secondary font-bold text-sm">
                      {(member.nickname || member.username || "U")
                        .charAt(0)
                        .toUpperCase()}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-on-surface">
                        {member.nickname ||
                          member.username ||
                          `用户${member.userId}`}
                      </p>
                      <p className="text-xs text-on-surface-variant">
                        @{member.username}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span
                      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${
                        member.role === KbRole.OWNER
                          ? "bg-amber-100 text-amber-700"
                          : member.role === KbRole.ADMIN
                            ? "bg-blue-100 text-blue-700"
                            : member.role === KbRole.EDITOR
                              ? "bg-green-100 text-green-700"
                              : "bg-surface-container text-on-surface-variant"
                      }`}
                    >
                      <Shield className="w-3 h-3" />
                      {KB_ROLE_LABEL[member.role] || "Viewer"}
                    </span>

                    {isOwner && member.role !== KbRole.OWNER && (
                      <div className="flex items-center gap-1 ml-2">
                        {member.role === KbRole.VIEWER && (
                          <button
                            onClick={() => handleUpdateRole(member.userId, KbRole.EDITOR)}
                            className="p-1.5 rounded hover:bg-green-50 text-on-surface-variant hover:text-green-600 transition-colors"
                            title="设为 Editor"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                        )}
                        {member.role === KbRole.EDITOR && (
                          <button
                            onClick={() => handleUpdateRole(member.userId, KbRole.ADMIN)}
                            className="p-1.5 rounded hover:bg-blue-50 text-on-surface-variant hover:text-blue-600 transition-colors"
                            title="设为 Admin"
                          >
                            <Shield className="w-3.5 h-3.5" />
                          </button>
                        )}
                        {member.role === KbRole.ADMIN && (
                          <button
                            onClick={() => handleUpdateRole(member.userId, KbRole.EDITOR)}
                            className="p-1.5 rounded hover:bg-surface-container text-on-surface-variant hover:text-on-surface transition-colors"
                            title="设为 Editor"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                        )}
                        <button
                          onClick={() => handleRemoveMember(member.userId)}
                          className="p-1.5 rounded hover:bg-error/10 text-on-surface-variant hover:text-error transition-colors"
                          title="移除"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
          </div>

          {/* Pending invites section */}
          {(() => {
            const pending = members.filter((m) => m.inviteStatus === JoinStatus.INVITED);
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
      )}

      <ConfirmModal
        open={memberToRemove !== null}
        onClose={() => setMemberToRemove(null)}
        onConfirm={confirmRemoveMember}
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
        open={articleToDelete !== null}
        onClose={() => setArticleToDelete(null)}
        onConfirm={confirmDeleteArticle}
        title="删除文章"
        message="确认删除该文章？此操作不可撤销。"
        confirmText="删除"
        danger
      />
    </div>
  );
}
