import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { teamApi, kbApi } from "../api";
import type { TeamVO, KnowledgeBase } from "../api/types";
import { Plus, BookOpen, Users, X, Loader2 } from "lucide-react";
import { Visibility } from "../api/types";

export function WorkspacePage() {
  const navigate = useNavigate();
  const [teams, setTeams] = useState<TeamVO[]>([]);
  const [kbs, setKbs] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(true);

  // Create dialog state
  const [dialogType, setDialogType] = useState<"team" | "kb" | null>(null);
  const [formName, setFormName] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formVisibility, setFormVisibility] = useState<number>(Visibility.PRIVATE);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    try {
      const [t, k] = await Promise.all([teamApi.list(), kbApi.listMine()]);
      setTeams(t);
      setKbs(k);
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openDialog = (type: "team" | "kb") => {
    setDialogType(type);
    setFormName("");
    setFormDesc("");
    setFormVisibility(Visibility.PRIVATE);
  };

  const closeDialog = () => {
    setDialogType(null);
  };

  const handleSubmit = async () => {
    if (!formName.trim()) return;
    setSubmitting(true);
    try {
      if (dialogType === "team") {
        await teamApi.create(formName.trim(), formDesc.trim() || undefined);
      } else {
        await kbApi.create(
          formName.trim(),
          formDesc.trim() || undefined,
          formVisibility,
        );
      }
      closeDialog();
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "创建失败");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 text-primary animate-spin" />
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-on-surface mb-8">工作台</h1>

      {/* TEAM */}
      <section className="mb-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-on-surface">我的 TEAM</h2>
          <button
            onClick={() => openDialog("team")}
            className="flex items-center gap-1 text-sm text-primary hover:underline font-medium"
          >
            <Plus className="w-4 h-4" /> 新建 TEAM
          </button>
        </div>
        {teams.length === 0 ? (
          <p className="text-sm text-on-surface-variant py-4">
            暂未加入任何 TEAM
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {teams.map((team) => (
              <button
                key={team.id}
                onClick={() => navigate(`/team/${team.id}`)}
                className="text-left bg-surface-container-lowest border border-outline-variant p-4 transition-all"
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-10 h-10 bg-primary/10 flex items-center justify-center">
                    <Users className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h3 className="font-medium text-on-surface">{team.name}</h3>
                    <p className="text-xs text-on-surface-variant">
                      {team.memberCount} 成员
                    </p>
                  </div>
                </div>
                {team.description && (
                  <p className="text-sm text-on-surface-variant line-clamp-2">
                    {team.description}
                  </p>
                )}
              </button>
            ))}
          </div>
        )}
      </section>

      {/* Knowledge Bases */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-on-surface">我的知识库</h2>
          <button
            onClick={() => openDialog("kb")}
            className="flex items-center gap-1 text-sm text-primary hover:underline font-medium"
          >
            <Plus className="w-4 h-4" /> 新建知识库
          </button>
        </div>
        {kbs.length === 0 ? (
          <p className="text-sm text-on-surface-variant py-4">暂无知识库</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {kbs.map((kb) => (
              <button
                key={kb.id}
                onClick={() => navigate(`/kb/${kb.id}`)}
                className="text-left bg-surface-container-lowest border border-outline-variant p-4 transition-all"
              >
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-10 h-10 bg-secondary/10 flex items-center justify-center">
                    <BookOpen className="w-5 h-5 text-secondary" />
                  </div>
                  <div>
                    <h3 className="font-medium text-on-surface">{kb.name}</h3>
                    <p className="text-xs text-on-surface-variant">
                      {kb.visibility === Visibility.PUBLIC ? "公开" : "私有"}
                    </p>
                  </div>
                </div>
                {kb.description && (
                  <p className="text-sm text-on-surface-variant line-clamp-2">
                    {kb.description}
                  </p>
                )}
              </button>
            ))}
          </div>
        )}
      </section>

      {/* Create Dialog */}
      {dialogType && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-surface-container-lowest w-full max-w-md mx-4">
            <div className="flex items-center justify-between p-4 border-b border-outline-variant">
              <h3 className="font-bold text-on-surface">
                {dialogType === "team" ? "新建 TEAM" : "新建知识库"}
              </h3>
              <button
                onClick={closeDialog}
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
                  placeholder={
                    dialogType === "team" ? "TEAM 名称" : "知识库名称"
                  }
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  autoFocus
                  onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
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
                  value={formDesc}
                  onChange={(e) => setFormDesc(e.target.value)}
                />
              </div>

              {dialogType === "kb" && (
                <div>
                  <label className="block text-sm font-medium text-on-surface-variant mb-2">
                    可见性
                  </label>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setFormVisibility(Visibility.PRIVATE)}
                      className={`flex-1 py-2 text-sm font-medium border transition-colors ${
                        formVisibility === Visibility.PRIVATE
                          ? "border-primary bg-primary/5 text-primary"
                          : "border-outline-variant text-on-surface-variant hover:bg-surface-container"
                      }`}
                    >
                      私有
                    </button>
                    <button
                      onClick={() => setFormVisibility(Visibility.PUBLIC)}
                      className={`flex-1 py-2 text-sm font-medium border transition-colors ${
                        formVisibility === Visibility.PUBLIC
                          ? "border-primary bg-primary/5 text-primary"
                          : "border-outline-variant text-on-surface-variant hover:bg-surface-container"
                      }`}
                    >
                      公开
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 p-4 border-t border-outline-variant">
              <button
                onClick={closeDialog}
                className="px-4 py-2 text-sm text-on-surface-variant hover:bg-surface-container"
              >
                取消
              </button>
              <button
                onClick={handleSubmit}
                disabled={submitting || !formName.trim()}
                className="px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
              >
                {submitting ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  "创建"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
