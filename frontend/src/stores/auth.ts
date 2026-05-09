import { createContext, useContext } from "react";
import { create } from "zustand";
import type { User } from "../api/types";
import { authApi } from "../api";

interface AuthState {
  user: User | null;
  loading: boolean;
  initialized: boolean;

  init: () => Promise<void>;
  login: (username: string, password: string, turnstileToken: string) => Promise<void>;
  register: (
    username: string,
    password: string,
    email: string,
    code: string,
  ) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  loading: false,
  initialized: false,

  init: async () => {
    if (get().initialized) return;
    try {
      const user = await authApi.current();
      set({ user, initialized: true });
    } catch {
      set({ user: null, initialized: true });
    }
  },

  login: async (username, password, turnstileToken) => {
    set({ loading: true });
    try {
      const user = await authApi.login(username, password, turnstileToken);
      set({ user, loading: false });
    } catch (e) {
      set({ loading: false });
      throw e;
    }
  },

  register: async (username, password, email, code) => {
    set({ loading: true });
    try {
      const user = await authApi.register(username, password, email, code);
      set({ user, loading: false });
    } catch (e) {
      set({ loading: false });
      throw e;
    }
  },

  logout: async () => {
    await authApi.logout();
    set({ user: null });
  },

  setUser: (user) => set({ user }),
}));

// Context 形式供组件外使用（如路由守卫）
export const AuthContext = createContext<AuthState>(null!);
export const useAuth = () => useContext(AuthContext);
