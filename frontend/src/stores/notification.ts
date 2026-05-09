import { create } from "zustand";
import type { NotificationItem } from "../api/types";
import { notificationApi, authApi } from "../api";

interface NotificationState {
  unreadCount: number;
  notifications: NotificationItem[];
  loading: boolean;
  ws: WebSocket | null;

  fetchUnreadCount: () => Promise<void>;
  fetchNotifications: () => Promise<void>;
  markRead: (id: number) => Promise<void>;
  markAllRead: () => Promise<void>;
  deleteNotification: (id: number) => Promise<void>;
  connectWs: () => void;
  disconnectWs: () => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  unreadCount: 0,
  notifications: [],
  loading: false,
  ws: null,

  fetchUnreadCount: async () => {
    try {
      const count = await notificationApi.unreadCount();
      set({ unreadCount: count });
    } catch {
      // ignore
    }
  },

  fetchNotifications: async () => {
    set({ loading: true });
    try {
      const list = await notificationApi.list(0, 30);
      set({ notifications: list, loading: false });
    } catch {
      set({ loading: false });
    }
  },

  markRead: async (id) => {
    try {
      await notificationApi.read(id);
      set((s) => ({
        notifications: s.notifications.map((n) =>
          n.id === id ? { ...n, isRead: 1 } : n,
        ),
        unreadCount: Math.max(0, s.unreadCount - 1),
      }));
    } catch {
      // ignore
    }
  },

  markAllRead: async () => {
    try {
      await notificationApi.readAll();
      set((s) => ({
        notifications: s.notifications.map((n) => ({ ...n, isRead: 1 })),
        unreadCount: 0,
      }));
    } catch {
      // ignore
    }
  },

  deleteNotification: async (id) => {
    try {
      await notificationApi.delete(id);
      set((s) => {
        const n = s.notifications.find((n) => n.id === id);
        return {
          notifications: s.notifications.filter((n) => n.id !== id),
          unreadCount: n && !n.isRead ? Math.max(0, s.unreadCount - 1) : s.unreadCount,
        };
      });
    } catch {
      // ignore
    }
  },

  connectWs: () => {
    const existing = get().ws;
    if (existing && existing.readyState === WebSocket.OPEN) return;

    authApi.wsToken().then(({ token }) => {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
      const wsUrl = `${protocol}//${window.location.host}/api/notification/ws?token=${token}`;
      const ws = new WebSocket(wsUrl);

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.type === "notification" && msg.data) {
            const notification = msg.data as NotificationItem;
            set((s) => ({
              notifications: [notification, ...s.notifications].slice(0, 50),
              unreadCount: s.unreadCount + 1,
            }));
          } else if (msg.type === "unread_count") {
            set({ unreadCount: msg.count });
          }
        } catch {
          // ignore
        }
      };

      ws.onclose = () => {
        set({ ws: null });
        // Reconnect after 5 seconds
        setTimeout(() => {
          if (!get().ws) get().connectWs();
        }, 5000);
      };

      ws.onerror = () => {
        ws.close();
      };

      set({ ws });
    }).catch(() => {
      // wsToken failed, retry later
      setTimeout(() => get().connectWs(), 10000);
    });
  },

  disconnectWs: () => {
    const ws = get().ws;
    if (ws) {
      ws.onclose = null; // prevent reconnect
      ws.close();
      set({ ws: null });
    }
  },
}));
