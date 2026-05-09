import { useState, useEffect, useCallback, useMemo, memo, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { articleApi } from "../api";
import { useAuthStore } from "../stores/auth";
import { ArticleStatus, type ArticleVO, type CommentItem } from "../api/types";
import { useChat } from "../hooks/useChat";
import { useChatStore } from "../stores/chat";
import { ChatSidebar } from "../components/ChatSidebar";
import { ConfirmModal } from "../components/Modal";
import { marked } from "marked";
import { markedHighlight } from "marked-highlight";
import hljs from "highlight.js";
import {
  Edit3,
  Heart,
  MessageSquare,
  Bookmark,
  Send,
  Trash2,
  Reply,
  Loader2,
  MessageCircle,
  Shield,
} from "lucide-react";

marked.use(
  markedHighlight({
    highlight(code: string, lang: string) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value;
      }
      return hljs.highlightAuto(code).value;
    },
  }),
);

/** Extract first H1 from markdown content */
function extractTitle(md: string): string {
  const match = md.match(/^#\s+(.+)$/m);
  return match ? match[1]!.trim() : "";
}

/** Decode common HTML entities back to plain code text */
function decodeHtmlEntities(text: string): string {
  return text
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex: string) =>
      String.fromCodePoint(Number.parseInt(hex, 16)),
    )
    .replace(/&#(\d+);/g, (_, dec: string) =>
      String.fromCodePoint(Number.parseInt(dec, 10)),
    )
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&(apos|#39);/g, "'")
    .replace(/&amp;/g, "&");
}

/** Normalize pasted/exported highlighted HTML inside fenced code blocks */
function normalizeHighlightedCodeFences(md: string): string {
  return md.replace(/```([^\n`]*)\n([\s\S]*?)```/g, (_match, lang: string, code: string) => {
    // Only strip highlight.js <span class="hljs-...">...</span> wrappers, not generic <...> like C++ includes
    if (!/<span\s+class\s*=\s*["']hljs-/i.test(code)) {
      return `\`\`\`${lang}\n${code}\`\`\``;
    }

    const normalizedCode = decodeHtmlEntities(
      code
        .replace(/<span\s+class\s*=\s*["']hljs-[^"']*["']\s*>/gi, "")
        .replace(/<\/span>/gi, ""),
    );
    return `\`\`\`${lang}\n${normalizedCode}\`\`\``;
  });
}

/** Remove first H1 from markdown body so page header own title */
function stripFirstH1(md: string): string {
  return md.replace(/^#\s+.+\n+/m, "").trim();
}

const ArticleContent = memo(function ArticleContent({ html }: { html: string }) {
  return (
    <div className="article-content mb-12 text-on-surface">
      {html ? (
        <div dangerouslySetInnerHTML={{ __html: html }} />
      ) : (
        <p className="text-on-surface-variant">暂无内容</p>
      )}
    </div>
  );
});

/** Get article access summary label */
function getArticleAccessSummary(article: ArticleVO): string {
  return article.status === ArticleStatus.DRAFT ? "草稿" : "已发布";
}

export function ArticleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [article, setArticle] = useState<ArticleVO | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [favorited, setFavorited] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [replyTo, setReplyTo] = useState<CommentItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const articleId = Number(id);
  const chatOpen = useChatStore((s) => s.openMap[articleId] ?? false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingCommentId, setDeletingCommentId] = useState<number | null>(null);
  const [likeLoading, setLikeLoading] = useState(false);
  const [favLoading, setFavLoading] = useState(false);
  const [permissionLoading, setPermissionLoading] = useState(false);
  const [permissionMenuOpen, setPermissionMenuOpen] = useState(false);
  const permissionMenuRef = useRef<HTMLDivElement | null>(null);

  const { messages, onlineUsers, send, connected } = useChat(chatOpen ? articleId : 0);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      const [a, c] = await Promise.all([
        articleApi.detail(Number(id)),
        articleApi.comments(Number(id)),
      ]);
      setArticle(a);
      setComments(c);
      setLiked(a.liked || false);
      setLikeCount(a.likeCount || 0);
      setFavorited(a.favorited || false);
    } catch {
      navigate("/");
    } finally {
      setLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!permissionMenuOpen) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (!permissionMenuRef.current?.contains(event.target as Node)) {
        setPermissionMenuOpen(false);
      }
    };

    window.addEventListener("mousedown", handlePointerDown);
    return () => window.removeEventListener("mousedown", handlePointerDown);
  }, [permissionMenuOpen]);

  // 记录浏览
  useEffect(() => {
    if (articleId && user) {
      articleApi.visit(articleId).catch(() => {});
    }
  }, [articleId, user]);

  const handleLike = async () => {
    if (!article || likeLoading) return;
    setLikeLoading(true);
    try {
      const r = liked
        ? await articleApi.unlike(article.id)
        : await articleApi.like(article.id);
      setLiked(r.liked);
      setLikeCount(r.likeCount);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setLikeLoading(false);
    }
  };

  const handleFavorite = async () => {
    if (!article || favLoading) return;
    setFavLoading(true);
    try {
      const r = favorited
        ? await articleApi.unfavorite(article.id)
        : await articleApi.favorite(article.id);
      setFavorited(r.favorited);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setFavLoading(false);
    }
  };

  const handlePermissionChange = async (nextStatus: number) => {
    if (!article || permissionLoading || article.status === nextStatus) {
      setPermissionMenuOpen(false);
      return;
    }

    setPermissionLoading(true);
    try {
      const updated = await articleApi.update(article.id, { status: nextStatus });
      setArticle((current) => (
        current
          ? {
              ...current,
              ...updated,
              liked: current.liked,
              likeCount: current.likeCount,
              favorited: current.favorited,
            }
          : updated
      ));
      setPermissionMenuOpen(false);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setPermissionLoading(false);
    }
  };

  const handleComment = async () => {
    if (!commentText.trim() || !article) return;
    setSubmitting(true);
    try {
      await articleApi.addComment(
        article.id,
        commentText.trim(),
        replyTo?.parentId ?? replyTo?.id ?? undefined,
        replyTo?.id ?? undefined,
      );
      setCommentText("");
      setReplyTo(null);
      const c = await articleApi.comments(article.id);
      setComments(c);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    setDeletingCommentId(commentId);
    setDeleteConfirmOpen(true);
  };

  const confirmDeleteComment = async () => {
    if (deletingCommentId == null) return;
    try {
      await articleApi.deleteComment(deletingCommentId);
      if (article) {
        const c = await articleApi.comments(article.id);
        setComments(c);
      }
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setDeleteConfirmOpen(false);
      setDeletingCommentId(null);
    }
  };

  const articleContent = article?.content || "";
  const displayTitle = extractTitle(articleContent) || article?.title || "";
  const renderedContent = normalizeHighlightedCodeFences(stripFirstH1(articleContent));
  const accessSummary = article ? getArticleAccessSummary(article) : "";
  const permissionOptions = [
    {
      key: "draft",
      label: "草稿",
      description: "仅自己可见，未发布状态",
      active: article?.status === ArticleStatus.DRAFT,
      onClick: () => handlePermissionChange(ArticleStatus.DRAFT),
    },
    {
      key: "published",
      label: "已发布",
      description: "发布后可见范围由所属知识库权限控制",
      active: article?.status === ArticleStatus.PUBLISHED,
      onClick: () => handlePermissionChange(ArticleStatus.PUBLISHED),
    },
  ];
  const renderedHtml = useMemo(
    () => (renderedContent ? (marked(renderedContent) as string) : ""),
    [renderedContent],
  );

  if (loading || !article) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 text-primary animate-spin" />
      </div>
    );
  }

  // 构建评论树
  const topLevel = comments.filter((c) => c.parentId == null);
  const getReplies = (parentId: number) =>
    comments.filter((c) => c.parentId === parentId);

  return (
    <div>
      {/* 主内容区 */}
      <div className={`transition-all ${chatOpen ? "mr-80" : ""}`}>
        <div className="max-w-3xl mx-auto px-4 py-8">
          <article>
            <h1 className="font-heading text-4xl font-semibold tracking-tight text-on-surface mb-4">
              {displayTitle}
            </h1>
            <div className="flex items-center gap-3 text-sm text-on-surface-variant mb-6 pb-4 border-b border-outline-variant">
              <span>{article.authorName || `用户${article.authorId}`}</span>
              <span>·</span>
              <span>{article.updateTime}</span>
              {article.teamName && (
                <>
                  <span>·</span>
                  <span className="text-primary bg-primary/10 px-2 py-0.5 rounded">{article.teamName}</span>
                </>
              )}
              {article.status === ArticleStatus.DRAFT && (
                <>
                  <span>·</span>
                  <span className="text-amber-600">草稿</span>
                </>
              )}
            </div>

            {/* Action bar */}
            <div className="flex items-center gap-4 mb-8 min-h-9">
              <button
                onClick={handleLike}
                disabled={likeLoading}
                className={`inline-flex h-9 shrink-0 items-center gap-1.5 px-3 py-1.5 text-sm font-medium transition-colors ${
                  liked
                    ? "bg-error/10 text-error"
                    : "text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                <Heart className={`w-4 h-4 shrink-0 ${liked ? "fill-error" : ""}`} />
                <span className="min-w-[1ch] text-center tabular-nums">{likeCount}</span>
              </button>
              <button
                onClick={handleFavorite}
                disabled={favLoading}
                className={`inline-flex h-9 shrink-0 items-center gap-1.5 px-3 py-1.5 text-sm font-medium transition-colors ${
                  favorited
                    ? "bg-amber-100 text-amber-700"
                    : "text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                <Bookmark className={`w-4 h-4 shrink-0 ${favorited ? "fill-amber-500" : ""}`} />
                收藏
              </button>
              <button
                onClick={() => useChatStore.getState().setOpen(articleId, !chatOpen)}
                className={`inline-flex h-9 shrink-0 items-center gap-1.5 px-3 py-1.5 text-sm font-medium transition-colors ml-auto ${
                  chatOpen
                    ? "bg-primary/10 text-primary"
                    : "text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                <MessageCircle className="w-4 h-4" />
                聊天
                {onlineUsers.length > 0 && (
                  <span className="ml-1 px-1.5 py-0.5 bg-green-100 text-green-700 text-[10px] rounded-full">
                    {onlineUsers.length}
                  </span>
                )}
              </button>
              {article.canEdit && (
                <>
                  <div className="relative" ref={permissionMenuRef}>
                    <button
                      onClick={() => setPermissionMenuOpen((open) => !open)}
                      disabled={permissionLoading}
                      className="inline-flex h-9 shrink-0 items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-on-surface-variant transition-colors hover:bg-surface-container disabled:opacity-60"
                    >
                      {permissionLoading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Shield className="h-4 w-4" />
                      )}
                      权限
                      <span className="bg-surface-container px-1.5 py-0.5 text-[11px] text-on-surface">
                        {accessSummary}
                      </span>
                    </button>
                    {permissionMenuOpen && (
                      <div className="absolute right-0 top-[calc(100%+0.5rem)] z-20 w-72 overflow-hidden border border-outline-variant bg-surface">
                        <div className="border-b border-outline-variant px-3 py-2">
                          <p className="text-sm font-medium text-on-surface">文章权限</p>
                          <p className="mt-0.5 text-xs text-on-surface-variant">
                            切换草稿/已发布状态，可见范围由知识库权限管理
                          </p>
                        </div>
                        <div className="p-2">
                          {permissionOptions.map((option) => (
                            <button
                              key={option.key}
                              type="button"
                              onClick={option.onClick}
                              disabled={permissionLoading}
                              className={`flex w-full items-start gap-3 px-3 py-2.5 text-left transition-colors ${
                                option.active
                                  ? "bg-primary/10 text-on-surface"
                                  : "text-on-surface-variant hover:bg-surface-container"
                              }`}
                            >
                              <span
                                className={`mt-1 h-2.5 w-2.5 shrink-0 rounded-full ${
                                  option.active ? "bg-primary" : "bg-outline"
                                }`}
                              />
                              <span className="min-w-0">
                                <span className="block text-sm font-medium text-inherit">{option.label}</span>
                                <span className="mt-0.5 block text-xs text-on-surface-variant">{option.description}</span>
                              </span>
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                  <button
                    onClick={() => navigate(`/article/${article.id}/edit`)}
                    className="inline-flex h-9 shrink-0 items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors"
                  >
                    <Edit3 className="w-4 h-4" /> 编辑
                  </button>
                </>
              )}
            </div>

            {/* Content */}
            <ArticleContent html={renderedHtml} />
          </article>

          {/* Comments Section */}
          <section className="border-t border-outline-variant pt-8">
            <h2 className="text-lg font-bold text-on-surface mb-6 flex items-center gap-2">
              <MessageSquare className="w-5 h-5" /> 评论 ({comments.length})
            </h2>

            {/* Comment input */}
            <div className="bg-surface-container-lowest border border-outline-variant p-4 mb-6">
              {replyTo && (
                <div className="flex items-center gap-2 text-sm text-on-surface-variant mb-2">
                  <span>
                    回复 <strong>{replyTo.nickname || replyTo.username}</strong>
                  </span>
                  <button
                    onClick={() => setReplyTo(null)}
                    className="text-primary hover:underline"
                  >
                    取消
                  </button>
                </div>
              )}
              <textarea
                className="w-full text-sm min-h-[80px] resize-none outline-none placeholder:text-on-surface-variant/50"
                placeholder="写评论..."
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                onKeyDown={(e) => {
                  if ((e.ctrlKey || e.metaKey) && e.key === "Enter") handleComment();
                }}
              />
              <div className="flex justify-end mt-2">
                <button
                  onClick={handleComment}
                  disabled={submitting || !commentText.trim()}
                  className="flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  {submitting ? "发送中..." : "发送"}
                </button>
              </div>
            </div>

            {/* Comments list */}
            <div className="space-y-4">
              {topLevel.map((comment) => (
                <CommentBlock
                  key={comment.id}
                  comment={comment}
                  replies={getReplies(comment.id)}
                  allComments={comments}
                  currentUserId={user?.id}
                  onReply={(c) => setReplyTo(c)}
                  onDelete={handleDeleteComment}
                />
              ))}
              {topLevel.length === 0 && (
                <p className="text-center text-on-surface-variant py-8 text-sm">
                  暂无评论
                </p>
              )}
            </div>
          </section>
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

      {/* 删除确认弹窗 */}
      <ConfirmModal
        open={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={confirmDeleteComment}
        title="删除评论"
        message="确认删除此评论吗？删除后不可恢复。"
        danger
        confirmText="删除"
      />
    </div>
  );
}

// ==================== 评论组件 ====================

function CommentBlock({
  comment,
  replies,
  allComments,
  currentUserId,
  onReply,
  onDelete,
  depth = 0,
}: {
  comment: CommentItem;
  replies: CommentItem[];
  allComments: CommentItem[];
  currentUserId?: number;
  onReply: (c: CommentItem) => void;
  onDelete: (id: number) => void;
  depth?: number;
}) {
  return (
    <div className={depth > 0 ? "ml-8 border-l-2 border-outline-variant/30 pl-4" : ""}>
      <div className="flex items-start gap-3 py-3">
        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-xs shrink-0">
          {(comment.nickname || comment.username || "U").charAt(0).toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm font-medium text-on-surface">
              {comment.nickname || comment.username || `用户${comment.userId}`}
            </span>
            {comment.replyToNickname && (
              <span className="text-xs text-on-surface-variant">
                回复 <strong>{comment.replyToNickname}</strong>
              </span>
            )}
            <span className="text-xs text-on-surface-variant">{comment.createTime}</span>
          </div>
          <p className="text-sm text-on-surface-variant leading-relaxed">{comment.content}</p>
          <div className="flex items-center gap-3 mt-2">
            <button
              onClick={() => onReply(comment)}
              className="flex items-center gap-1 text-xs text-on-surface-variant hover:text-primary transition-colors"
            >
              <Reply className="w-3 h-3" /> 回复
            </button>
            {comment.userId === currentUserId && (
              <button
                onClick={() => onDelete(comment.id)}
                className="flex items-center gap-1 text-xs text-on-surface-variant hover:text-error transition-colors"
              >
                <Trash2 className="w-3 h-3" /> 删除
              </button>
            )}
          </div>
        </div>
      </div>
      {replies.map((reply) => (
        <CommentBlock
          key={reply.id}
          comment={reply}
          replies={allComments.filter((c) => c.parentId === reply.id)}
          allComments={allComments}
          currentUserId={currentUserId}
          onReply={onReply}
          onDelete={onDelete}
          depth={depth + 1}
        />
      ))}
    </div>
  );
}
