import { create } from "zustand";
import type { NotificationItem } from "../api/types";
import { notificationApi } from "../api";

interface NotificationState {
  unreadCount: number;
  notifications: NotificationItem[];
  loading: boolean;

  fetchUnreadCount: () => Promise<void>;
  fetchNotifications: () => Promise<void>;
  markRead: (id: number) => Promise<void>;
  markAllRead: () => Promise<void>;
  deleteNotification: (id: number) => Promise<void>;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  unreadCount: 0,
  notifications: [],
  loading: false,

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
}));
