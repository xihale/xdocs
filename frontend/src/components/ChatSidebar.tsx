import { useState, useEffect, useRef } from "react";
import { MessageCircle, X, Send, Trash2 } from "lucide-react";
import type { ChatMsg } from "../hooks/useChat";
import { getChatSavedAt, clearChatMessages } from "../stores/chat";

/** Format a timestamp into a human-readable relative time string */
function formatRelativeTime(ts: number): string {
  const now = Date.now();
  const diff = now - ts;
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return "刚刚";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} 分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} 天前`;
  return new Date(ts).toLocaleDateString("zh-CN");
}

interface ChatSidebarProps {
  messages: ChatMsg[];
  onlineUsers: { userId: number; nickname: string; avatarUrl: string | null }[];
  onSend: (content: string) => void;
  connected: boolean;
  onClose: () => void;
  currentUserId?: number;
  articleId: number;
}

export function ChatSidebar({
  messages,
  onlineUsers,
  onSend,
  connected,
  onClose,
  currentUserId,
  articleId,
}: ChatSidebarProps) {
  const savedAtRef = useRef(getChatSavedAt(articleId));
  const [savedAt, setSavedAt] = useState<number | null>(savedAtRef.current);

  const handleClear = () => {
    clearChatMessages(articleId);
    savedAtRef.current = null;
    setSavedAt(null);
  };

  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = () => {
    if (!input.trim()) return;
    onSend(input.trim());
    setInput("");
  };

  return (
    <div className="w-80 border-l border-outline-variant bg-surface-container-lowest flex flex-col fixed right-0 top-14 bottom-0 z-40">
      {/* Header */}
      <div className="h-12 border-b border-outline-variant flex items-center justify-between px-4 shrink-0 gap-3">
        <div className="flex items-center gap-2 min-w-0">
          <MessageCircle className="w-4 h-4 text-primary" />
          <span className="text-sm font-medium text-on-surface truncate">实时聊天</span>
          <span className={`w-2 h-2 rounded-full ${connected ? "bg-green-500" : "bg-gray-400"}`} />
        </div>
        <div className="flex items-center gap-1 shrink-0">
          <button
            type="button"
            onClick={handleClear}
            className="p-1.5 hover:bg-surface-container text-on-surface-variant transition-colors"
            title="清空聊天记录"
            aria-label="清空聊天记录"
          >
            <Trash2 className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 hover:bg-surface-container text-on-surface-variant transition-colors"
            title="关闭聊天"
            aria-label="关闭聊天"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 在线用户 */}
      <div className="h-14 border-b border-outline-variant bg-surface-container-low px-4 flex items-center">
        <div className="flex items-center">
          <div className="flex items-center -space-x-2 pl-1">
            {onlineUsers.map((u) => (
              <div
                key={u.userId}
                className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center text-[9px] font-bold text-primary"
                title={u.nickname}
              >
                {u.nickname.charAt(0).toUpperCase()}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 消息列表 */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3">
        {/* 上次会话时间提示 */}
        {savedAt && messages.length > 0 && (
          <div className="text-center">
            <span className="text-xs text-on-surface-variant/50 bg-surface-container-low px-2 py-0.5 rounded">
              上次聊天记录 ({formatRelativeTime(savedAt)})
            </span>
          </div>
        )}
        {messages.map((msg) => {
          if (msg.type === "system") {
            return (
              <div key={msg.id} className="text-center">
                <span className="text-xs text-on-surface-variant/60 bg-surface-container-low px-2 py-0.5 rounded">
                  {msg.content}
                </span>
              </div>
            );
          }

          const isMine = msg.senderId === currentUserId;
          return (
            <div key={msg.id} className={`flex gap-2 ${isMine ? "flex-row-reverse" : ""}`}>
              <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-[10px] shrink-0">
                {(msg.senderName || "U").charAt(0).toUpperCase()}
              </div>
              <div className={`max-w-[70%] ${isMine ? "items-end" : "items-start"}`}>
                {!isMine && (
                  <span className="text-[10px] text-on-surface-variant mb-0.5 block">
                    {msg.senderName}
                  </span>
                )}
                <div
                  className={`px-3 py-2 text-sm ${
                    isMine
                      ? "bg-primary text-on-primary rounded-br-sm"
                      : "bg-surface-container text-on-surface rounded-bl-sm"
                  }`}
                >
                  {msg.content}
                </div>
              </div>
            </div>
          );
        })}
        {messages.length === 0 && (
          <p className="text-center text-on-surface-variant/50 text-sm py-8">
            暂无消息，发送第一条聊天吧
          </p>
        )}
      </div>

      {/* 输入框 */}
      <div className="p-3 border-t border-outline-variant shrink-0">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder={connected ? "输入消息..." : "连接中..."}
            disabled={!connected}
            className="flex-1 px-3 py-2 text-sm bg-surface-container-low outline-none placeholder:text-on-surface-variant/50 disabled:opacity-50"
          />
          <button
            onClick={handleSend}
            disabled={!connected || !input.trim()}
            className="p-2 bg-primary text-on-primary hover:opacity-90 disabled:opacity-50 transition-colors"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
