import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, Check, CheckCheck, Trash2 } from "lucide-react";
import { useNotificationStore } from "../stores/notification";
import type { NotificationItem } from "../api/types";

const TYPE_ICON: Record<number, string> = {
  0: "👥", // TEAM_INVITE
  1: "📚", // KB_INVITE
  2: "📄", // TEAM_NEW_ARTICLE
  3: "💬", // COMMENT
  4: "❤️", // LIKE
  5: "🔄", // MEMBER_CHANGE
  6: "👤", // FOLLOW
  7: "📝", // FOLLOW_ARTICLE
};

function timeAgo(dateStr: string): string {
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = now - then;
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}天前`;
  return new Date(dateStr).toLocaleDateString();
}

export function NotificationPanel() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const {
    unreadCount,
    notifications,
    fetchUnreadCount,
    fetchNotifications,
    markRead,
    markAllRead,
    connectWs,
    disconnectWs,
  } = useNotificationStore();

  // Initial fetch + WebSocket
  useEffect(() => {
    fetchUnreadCount();
    connectWs();
    return () => disconnectWs();
  }, []);

  // Close on click outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener("mousedown", handleClick);
      return () => document.removeEventListener("mousedown", handleClick);
    }
  }, [open]);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next) {
      await fetchNotifications();
    }
  };

  const handleClick = async (n: NotificationItem) => {
    if (!n.isRead) await markRead(n.id);
    if (n.link) {
      setOpen(false);
      navigate(n.link);
    }
  };

  const handleMarkAllRead = async () => {
    await markAllRead();
  };

  return (
    <div className="relative" ref={panelRef}>
      <button
        onClick={toggle}
        className="relative p-2 text-on-surface-variant hover:bg-surface-container transition-colors"
        title="通知"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center rounded-full bg-error text-on-error text-[10px] font-bold px-1">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-96 max-h-[480px] bg-surface-container-lowest border border-outline-variant overflow-hidden z-50">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-outline-variant">
            <h3 className="text-sm font-semibold text-on-surface">通知</h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="flex items-center gap-1 text-xs text-primary hover:text-primary/80 transition-colors"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                全部已读
              </button>
            )}
          </div>

          {/* List */}
          <div className="overflow-y-auto max-h-[400px]">
            {notifications.length === 0 ? (
              <div className="py-12 text-center text-sm text-on-surface-variant">
                暂无通知
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => handleClick(n)}
                  className={`flex gap-3 px-4 py-3 border-b border-outline-variant/50 cursor-pointer transition-colors hover:bg-surface-container ${
                    !n.isRead ? "bg-primary/5" : ""
                  }`}
                >
                  {/* Icon */}
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-sm">
                    {n.senderAvatar ? (
                      <img
                        src={n.senderAvatar}
                        alt=""
                        className="w-8 h-8 rounded-full object-cover"
                      />
                    ) : (
                      TYPE_ICON[n.type] || "📢"
                    )}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-on-surface truncate">
                        {n.title}
                      </span>
                      {!n.isRead && (
                        <span className="flex-shrink-0 w-2 h-2 rounded-full bg-primary" />
                      )}
                    </div>
                    {n.content && (
                      <p className="text-xs text-on-surface-variant mt-0.5 line-clamp-2">
                        {n.content}
                      </p>
                    )}
                    <span className="text-[10px] text-on-surface-variant/60 mt-1 block">
                      {timeAgo(n.createTime)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
