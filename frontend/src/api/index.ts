import type {
  User,
  TeamVO,
  KnowledgeBase,
  ArticleVO,
  ChatMessage,
  TeamMemberVO,
  KbMemberVO,
  CommentItem,
  UserProfile,
  FollowUser,
  FavoriteArticle,
  HistoryArticle,
  SearchUser,
  PendingInvite,
  KbPendingInvite,
  NotificationItem,
} from "./types";

const API_ROOT = "/api";

type ApiEnvelope<T> = {
  code?: number;
  message?: string;
  data?: T;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object";
}

function unwrapEnvelope<T>(payload: unknown, fallbackMessage: string): T {
  if (!isRecord(payload) || !("code" in payload)) {
    return payload as T;
  }

  const envelope = payload as ApiEnvelope<T>;
  const code = Number(envelope.code);
  if (Number.isFinite(code) && code !== 200) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data as T;
}

function query(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function request<T = unknown>(
  path: string,
  init: RequestInit = {},
  fallbackMessage = "请求失败",
): Promise<T> {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...(init.headers || {}),
    },
  });

  const payload = await parseJson(response);

  if (!response.ok) {
    throw new Error(
      (isRecord(payload) && typeof payload.message === "string"
        ? payload.message
        : null) || `${fallbackMessage}（HTTP ${response.status}）`,
    );
  }

  return unwrapEnvelope<T>(payload, fallbackMessage);
}

// ==================== Auth ====================

export const authApi = {
  login: (username: string, password: string, turnstileToken: string) =>
    request<User>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password, turnstileToken }),
    }),

  register: (
    username: string,
    password: string,
    email: string,
    code: string,
  ) =>
    request<User>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, email, code }),
    }),

  logout: () => request<void>("/auth/logout", { method: "POST" }),

  current: () =>
    request<User | null>("/auth/current", {}, "获取当前用户失败"),

  sendEmailCode: (email: string, turnstileToken: string, type: "register" | "reset" = "register") =>
    request<void>("/auth/send-code", {
      method: "POST",
      body: JSON.stringify({ email, turnstileToken, type }),
    }),

  resetPassword: (email: string, code: string, newPassword: string) =>
    request<void>("/auth/reset-password", {
      method: "POST",
      body: JSON.stringify({ email, code, newPassword }),
    }),
};

// ==================== User ====================

export const userApi = {
  profile: (id?: number) =>
    request<UserProfile>(`/user/profile${query({ id })}`, {}, "获取用户信息失败"),

  updateNickname: (nickname: string) =>
    request<void>("/user/update-nickname", {
      method: "POST",
      body: JSON.stringify({ nickname }),
    }),

  updateAvatar: (avatarUrl: string) =>
    request<void>("/user/update-avatar", {
      method: "POST",
      body: JSON.stringify({ avatarUrl }),
    }),

  changePassword: (oldPassword: string, newPassword: string) =>
    request<void>("/user/change-password", {
      method: "POST",
      body: JSON.stringify({ oldPassword, newPassword }),
    }),

  // 关注
  follow: (userId: number) =>
    request<void>("/user/follow", {
      method: "POST",
      body: JSON.stringify({ userId }),
    }),

  unfollow: (userId: number) =>
    request<void>("/user/unfollow", {
      method: "POST",
      body: JSON.stringify({ userId }),
    }),

  following: (userId?: number) =>
    request<FollowUser[]>(`/user/following${query({ userId })}`, {}),

  followers: (userId?: number) =>
    request<FollowUser[]>(`/user/followers${query({ userId })}`, {}),

  // 收藏
  favorites: () => request<FavoriteArticle[]>("/user/favorites", {}),

  // 浏览记录
  history: () => request<HistoryArticle[]>("/user/history", {}),

  deleteHistory: (articleId?: number) =>
    request<void>(`/user/history-delete${query({ articleId })}`, { method: "DELETE" }),
};

// ==================== Search ====================

export const searchApi = {
  articles: (keyword: string) =>
    request<ArticleVO[]>(`/search/articles${query({ keyword })}`, {}),

  kbs: (keyword: string) =>
    request<KnowledgeBase[]>(`/search/kbs${query({ keyword })}`, {}),

  users: (keyword: string) =>
    request<SearchUser[]>(`/search/users${query({ keyword })}`, {}),
};

// ==================== Team ====================

export const teamApi = {
  create: (name: string, description?: string) =>
    request<TeamVO>("/team/create", {
      method: "POST",
      body: JSON.stringify({ name, description }),
    }),

  list: () => request<TeamVO[]>("/team/list", {}, "获取TEAM列表失败"),

  detail: (id: number) =>
    request<TeamVO>(`/team/detail${query({ id })}`, {}, "获取TEAM详情失败"),

  pendingInvites: () =>
    request<PendingInvite[]>("/team/pending-invites", {}, "获取邀请列表失败"),

  invite: (teamId: number, userId?: number, username?: string) =>
    request<void>("/team/invite", {
      method: "POST",
      body: JSON.stringify({ teamId, userId, username }),
    }),

  accept: (teamId: number) =>
    request<void>("/team/accept", {
      method: "POST",
      body: JSON.stringify({ teamId }),
    }),

  reject: (teamId: number) =>
    request<void>("/team/reject", {
      method: "POST",
      body: JSON.stringify({ teamId }),
    }),

  quit: (teamId: number) =>
    request<void>("/team/quit", {
      method: "POST",
      body: JSON.stringify({ teamId }),
    }),

  updateRole: (teamId: number, userId: number, role: number) =>
    request<void>("/team/update-role", {
      method: "POST",
      body: JSON.stringify({ teamId, userId, role }),
    }),

  members: (teamId: number) =>
    request<TeamMemberVO[]>(`/team/members${query({ id: teamId })}`, {}, "获取成员列表失败"),

  kick: (teamId: number, userId: number) =>
    request<void>("/team/kick", {
      method: "POST",
      body: JSON.stringify({ teamId, userId }),
    }),

  cancelInvite: (teamId: number, userId: number) =>
    request<void>("/team/cancel-invite", {
      method: "POST",
      body: JSON.stringify({ teamId, userId }),
    }),
};

// ==================== KnowledgeBase ====================

export const kbApi = {
  create: (
    name: string,
    description?: string,
    visibility?: number,
    ownerType?: number,
    ownerId?: number,
  ) =>
    request<KnowledgeBase>("/kb/create", {
      method: "POST",
      body: JSON.stringify({ name, description, visibility, ownerType, ownerId }),
    }),

  update: (id: number, name?: string, description?: string) =>
    request<KnowledgeBase>("/kb/update", {
      method: "PUT",
      body: JSON.stringify({ id, name, description }),
    }),

  delete: (id: number) =>
    request<void>(`/kb/delete${query({ id })}`, { method: "DELETE" }),

  detail: (id: number) =>
    request<KnowledgeBase>(`/kb/detail${query({ id })}`, {}, "获取知识库详情失败"),

  listByOwner: (ownerType: number, ownerId: number) =>
    request<KnowledgeBase[]>(`/kb/list${query({ ownerType, ownerId })}`, {}),

  listMine: () =>
    request<KnowledgeBase[]>("/kb/list-mine", {}, "获取知识库列表失败"),

  authorize: (
    kbId: number,
    userId?: number,
    username?: string,
    role?: number,
  ) =>
    request<void>("/kb/authorize", {
      method: "POST",
      body: JSON.stringify({ kbId, userId, username, role }),
    }),

  accept: (kbId: number) =>
    request<void>("/kb/accept", {
      method: "POST",
      body: JSON.stringify({ kbId }),
    }),

  reject: (kbId: number) =>
    request<void>("/kb/reject", {
      method: "POST",
      body: JSON.stringify({ kbId }),
    }),

  cancelInvite: (kbId: number, userId: number) =>
    request<void>("/kb/cancel-invite", {
      method: "POST",
      body: JSON.stringify({ kbId, userId }),
    }),

  updateRole: (kbId: number, userId: number, role: number) =>
    request<void>("/kb/update-role", {
      method: "POST",
      body: JSON.stringify({ kbId, userId, role }),
    }),

  pendingInvites: () =>
    request<KbPendingInvite[]>("/kb/pending-invites", {}, "获取邀请列表失败"),

  members: (kbId: number) =>
    request<KbMemberVO[]>(`/kb/members${query({ id: kbId })}`, {}, "获取成员列表失败"),

  removeMember: (kbId: number, userId: number) =>
    request<void>("/kb/remove-member", {
      method: "POST",
      body: JSON.stringify({ kbId, userId }),
    }),
};

// ==================== Article ====================

export const articleApi = {
  create: (kbId: number, title: string, content?: string) =>
    request<ArticleVO>("/article/create", {
      method: "POST",
      body: JSON.stringify({ kbId, title, content }),
    }),

  update: (
    id: number,
    data: { title?: string; content?: string; summary?: string; status?: number },
  ) =>
    request<ArticleVO>("/article/update", {
      method: "PUT",
      body: JSON.stringify({ id, ...data }),
    }),

  delete: (id: number) =>
    request<void>(`/article/delete${query({ id })}`, { method: "DELETE" }),

  detail: (id: number) =>
    request<ArticleVO>(`/article/detail${query({ id })}`, {}, "获取文章详情失败"),

  listByKb: (kbId: number) =>
    request<ArticleVO[]>(`/article/list${query({ kbId })}`, {}),

  publicList: (offset = 0, limit = 20, sort = "time", keyword?: string) =>
    request<ArticleVO[]>(`/article/public-list${query({ offset, limit, sort, keyword })}`, {}),

  save: (id: number, content?: string) =>
    request<ArticleVO>("/article/save", {
      method: "POST",
      body: JSON.stringify({ id, content }),
    }),

  // 点赞
  like: (articleId: number) =>
    request<{ liked: boolean; likeCount: number }>("/article/like", {
      method: "POST",
      body: JSON.stringify({ articleId }),
    }),

  unlike: (articleId: number) =>
    request<{ liked: boolean; likeCount: number }>("/article/unlike", {
      method: "POST",
      body: JSON.stringify({ articleId }),
    }),

  // 评论
  addComment: (articleId: number, content: string, parentId?: number, replyToId?: number) =>
    request<{ id: number }>("/article/comment", {
      method: "POST",
      body: JSON.stringify({ articleId, content, parentId, replyToId }),
    }),

  comments: (articleId: number) =>
    request<CommentItem[]>(`/article/comments${query({ articleId })}`, {}),

  deleteComment: (id: number) =>
    request<void>(`/article/comment-delete${query({ id })}`, { method: "DELETE" }),

  // 收藏
  favorite: (articleId: number) =>
    request<{ favorited: boolean }>("/article/favorite", {
      method: "POST",
      body: JSON.stringify({ articleId }),
    }),

  unfavorite: (articleId: number) =>
    request<{ favorited: boolean }>("/article/unfavorite", {
      method: "POST",
      body: JSON.stringify({ articleId }),
    }),

  // 浏览记录
  visit: (articleId: number) =>
    request<void>("/article/visit", {
      method: "POST",
      body: JSON.stringify({ articleId }),
    }),
};

// ==================== Chat ====================

export const chatApi = {
  history: (articleId: number, limit = 50) =>
    request<ChatMessage[]>(
      `/chat/history${query({ articleId, limit })}`,
      {},
      "获取聊天记录失败",
    ),

  send: (articleId: number, content: string, teamId?: number) =>
    request<void>("/chat/send", {
      method: "POST",
      body: JSON.stringify({ articleId, content, teamId }),
    }),
};

// ==================== Upload ====================

async function upload(path: "/upload/image" | "/upload/avatar", file: File) {
  const form = new FormData();
  form.append("file", file);

  const response = await fetch(`${API_ROOT}${path}`, {
    method: "POST",
    credentials: "include",
    body: form,
  });
  const payload = await parseJson(response);

  if (!response.ok) {
    throw new Error(
      (isRecord(payload) && typeof payload.message === "string"
        ? payload.message
        : null) || `上传失败（HTTP ${response.status}）`,
    );
  }
  return unwrapEnvelope<{ url: string }>(payload, "上传失败");
}

export const uploadApi = {
  image: (file: File) => upload("/upload/image", file),
  avatar: (file: File) => upload("/upload/avatar", file),
};

// ==================== Notification ====================

export const notificationApi = {
  list: (offset = 0, limit = 20) =>
    request<NotificationItem[]>(`/notification/list${query({ offset, limit })}`, {}, "获取通知列表失败"),

  unreadCount: () =>
    request<number>("/notification/unread-count", {}, "获取未读数失败"),

  read: (id: number) =>
    request<void>("/notification/read", {
      method: "POST",
      body: JSON.stringify({ id }),
    }),

  readAll: () =>
    request<void>("/notification/read-all", { method: "POST" }),

  delete: (id: number) =>
    request<void>(`/notification/delete${query({ id })}`, { method: "DELETE" }),
};
