import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { articleApi } from "../api";
import { useAuthStore } from "../stores/auth";
import type { ArticleVO } from "../api/types";
import { ChevronLeft, Loader2, X, Code2, Eye, MessageCircle } from "lucide-react";
import { ChatSidebar } from "../components/ChatSidebar";
import Editor from "../components/Editor/Editor";
import type { CollabStatus } from "../components/Editor/Editor";
import { useChat } from "../hooks/useChat";
import { useChatStore } from "../stores/chat";

/** Extract first H1 from markdown content, truncated to 200 chars */
const MAX_TITLE_LENGTH = 200;

function extractTitle(md: string): string {
  const match = md.match(/^#\s+(.+)$/m);
  const title = match ? match[1]!.trim() : "";
  return title.length > MAX_TITLE_LENGTH ? title.slice(0, MAX_TITLE_LENGTH) : title;
}

export function ArticleEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [article, setArticle] = useState<ArticleVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [dirty, setDirty] = useState(false);
  const [noPermission, setNoPermission] = useState(false);
  const articleId = Number(id);
  const chatOpen = useChatStore((s) => s.openMap[articleId] ?? false);
  const [collabStatus, setCollabStatus] = useState<CollabStatus>("connecting");
  const [collabUsers, setCollabUsers] = useState<{ name: string; color: string }[]>([]);
  const [sourceMode, setSourceMode] = useState(false);
  const contentRef = useRef<string>("");
  const lastSavedContentRef = useRef<string>("");
  const lastSavedTitleRef = useRef<string>("");
  const articleRef = useRef(article);
  useEffect(() => {
    articleRef.current = article;
  }, [article]);
  const { messages, onlineUsers, send, connected } = useChat(chatOpen ? articleId : 0);

  // Live title extracted from editor content
  const [liveTitle, setLiveTitle] = useState("");

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    setLoading(true);
    setNoPermission(false);
    setArticle(null);
    articleApi
      .detail(Number(id))
      .then((data) => {
        if (cancelled) return;
        setArticle(data);
        contentRef.current = data.content || "";
        lastSavedContentRef.current = data.content || "";
        lastSavedTitleRef.current = data.title;
        setLiveTitle(extractTitle(data.content || ""));
        if (data.canEdit === false) {
          setNoPermission(true);
        }
      })
      .catch(() => {
        if (!cancelled) navigate(-1);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, navigate]);

  const handleSave = useCallback(async (manual = false) => {
    if (!articleRef.current) return;
    const currentContent = contentRef.current;
    const currentTitle = extractTitle(currentContent);
    const contentChanged = currentContent !== lastSavedContentRef.current;
    const titleChanged = currentTitle !== lastSavedTitleRef.current;

    // Avoid destructive autosave while collaborative editor is still bootstrapping.
    // Milkdown/Yjs can briefly report empty markdown before initial content is applied.
    if (!manual && lastSavedContentRef.current.trim() && !currentContent.trim()) {
      return;
    }

    if (!contentChanged && !titleChanged) {
      setDirty(false);
      return;
    }

    setSaving(true);
    setSaveError("");
    try {
      // Save content. Preserve intentional empty document on manual save.
      if (contentChanged) {
        await articleApi.save(articleRef.current.id, currentContent);
      }
      // Sync title if changed
      if (titleChanged && currentTitle) {
        await articleApi.update(articleRef.current.id, { title: currentTitle });
        lastSavedTitleRef.current = currentTitle;
      }
      lastSavedContentRef.current = currentContent;
      setDirty(false);
    } catch (e: unknown) {
      console.error("Save failed", e);
      setSaveError(e instanceof Error ? e.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }, []);

  const handleEditorChange = useCallback((md: string) => {
    contentRef.current = md;
    setDirty(true);
    setLiveTitle(extractTitle(md));
  }, []);

  // 自动保存：编辑器加载完成后每 2 秒保存一次
  useEffect(() => {
    if (loading || noPermission || !article) return;
    const timer = setInterval(() => handleSave(false), 2000);
    return () => clearInterval(timer);
  }, [loading, noPermission, article, handleSave]);

  // Ctrl+S
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        handleSave(true);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [handleSave]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-surface-container-lowest">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="w-10 h-10 text-primary animate-spin" />
          <p className="text-on-surface-variant animate-pulse">正在加载编辑器...</p>
        </div>
      </div>
    );
  }

  if (noPermission) {
    return (
      <div className="flex items-center justify-center h-screen bg-surface-container-lowest">
        <div className="flex flex-col items-center gap-4 max-w-md mx-auto p-6">
          <div className="w-16 h-16 rounded-full bg-error/10 flex items-center justify-center">
            <X className="w-8 h-8 text-error" />
          </div>
          <h2 className="text-lg font-bold text-on-surface">无权编辑此文章</h2>
          <p className="text-sm text-on-surface-variant text-center">
            您没有编辑此文章的权限。文章作者、知识库成员或 TEAM 成员可编辑。
          </p>
          <button
            onClick={() => navigate(`/article/${id}`)}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-on-primary text-xs font-bold hover:opacity-90 transition-all"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
            返回文章详情
          </button>
        </div>
      </div>
    );
  }

  if (!article) return null;

  const displayTitle = liveTitle || article.title;

  return (
    <div className="fixed inset-0 bg-surface-container-lowest flex flex-col">
      {/* Header */}
      <header className="h-12 bg-surface-container-lowest border-b border-outline-variant/60 flex items-center justify-between px-4 shrink-0 z-20">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/article/${article.id}`)}
            className="p-1.5 hover:bg-surface-container text-on-surface-variant transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <div className="flex items-center gap-2">
            <h1 className="text-sm font-semibold text-on-surface truncate max-w-[180px] sm:max-w-sm">
              {displayTitle || "无标题"}
            </h1>
            <div className={`flex items-center gap-1 text-[10px] font-medium ${
              saving ? "text-primary" : saveError ? "text-error" : dirty ? "text-amber-600" : "text-on-surface-variant/60"
            }`}>
              {saving && <Loader2 className="w-3 h-3 animate-spin" />}
              {saving ? "保存中" : saveError ? "保存失败" : dirty ? "未保存" : "已保存"}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {/* 协作状态 */}
          <div className="flex items-center gap-1.5 text-[10px] text-on-surface-variant/60">
            <div className={`w-1.5 h-1.5 rounded-full ${
              collabStatus === "connected" ? "bg-green-500" : "bg-amber-400 animate-pulse"
            }`} />
            <span>{collabStatus === "connected" ? "已连接" : collabStatus === "connecting" ? "连接中" : "已断开"}</span>
          </div>

          {/* 协作者头像 + 人数 */}
          {collabUsers.length > 0 && (
            <div className="flex items-center gap-1">
              <div className="flex -space-x-1.5">
                {collabUsers.map((u, i) => (
                  <div
                    key={i}
                    className="w-5 h-5 rounded-full border-2 border-surface-container-lowest flex items-center justify-center text-[8px] font-bold text-white"
                    style={{ backgroundColor: u.color }}
                    title={u.name}
                  >
                    {u.name.charAt(0).toUpperCase()}
                  </div>
                ))}
              </div>
              <span className="text-[10px] text-on-surface-variant/60">{collabUsers.length}人在线</span>
            </div>
          )}

          <div className="w-px h-4 bg-outline-variant/40" />

          {/* 源码/可视化切换 */}
          <button
            onClick={() => setSourceMode(!sourceMode)}
            className={`flex items-center gap-1 px-2 py-1.5 text-[11px] font-medium transition-colors ${
              sourceMode
                ? "bg-primary/10 text-primary"
                : "text-on-surface-variant/60 hover:bg-surface-container"
            }`
          }
          >
            {sourceMode ? <Eye className="w-3.5 h-3.5" /> : <Code2 className="w-3.5 h-3.5" />}
            {sourceMode ? "可视化" : "源码"}
          </button>

          <div className="w-px h-4 bg-outline-variant/40" />

          {/* 聊天 */}
          <button
            onClick={() => useChatStore.getState().setOpen(articleId, !chatOpen)}
            className={`flex items-center gap-1 px-2 py-1.5 text-[11px] font-medium transition-colors ${
              chatOpen
                ? "bg-primary/10 text-primary"
                : "text-on-surface-variant/60 hover:bg-surface-container"
            }`}
          >
            <MessageCircle className="w-3.5 h-3.5" />
            {connected && <span className="w-1 h-1 bg-green-500 rounded-full" />}
          </button>
        </div>
      </header>

      {/* 保存错误提示 */}
      {saveError && (
        <div className="absolute top-14 right-4 bg-error/10 text-error text-xs p-2.5 max-w-xs z-50">
          <div className="flex items-start gap-2">
            <span className="flex-1">{saveError}</span>
            <button onClick={() => setSaveError("")} className="hover:text-error/70">
              <X className="w-3 h-3" />
            </button>
          </div>
        </div>
      )}

      {/* Editor + Chat */}
      <main className={`flex-1 overflow-hidden flex relative transition-all ${chatOpen ? "mr-80" : ""}`}>
        <div className="flex-1 overflow-y-auto custom-scrollbar">
          <div className="w-full h-full max-w-3xl mx-auto px-4 sm:px-8 py-8">
            <Editor
              documentId={`article-${article.id}`}
              username={user?.nickname || user?.username || "anonymous"}
              initialContent={article.content ?? undefined}
              onChange={handleEditorChange}
              onCollabStatusChange={setCollabStatus}
              onCollabUsersChange={setCollabUsers}
              sourceMode={sourceMode}
            />
          </div>
        </div>

        {/* 聊天侧边栏 */}
        {chatOpen && (
          <ChatSidebar
            messages={messages}
            onlineUsers={onlineUsers}
            onSend={send}
            connected={connected}
            onClose={() => useChatStore.getState().setOpen(articleId, false)}
            currentUserId={user?.id}
            articleId={articleId}
          />
        )}
      </main>
    </div>
  );
}

