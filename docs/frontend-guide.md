# CloudDoc 前端开发指南

## 1. 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| React | 19 | UI 框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 6.x | 构建工具 + 开发服务器 |
| Zustand | 5.x | 全局状态管理 |
| React Router | 7.x | 客户端路由 |
| Yjs | 13.x | CRDT 协同编辑 |
| y-websocket | 2.x | Yjs WebSocket Provider |
| Tailwind CSS | 4.x | 样式 |

## 2. 目录结构

```text
frontend/src/
├── api/                        API 层
│   ├── index.ts                统一请求函数 + 全部 API 函数
│   └── types.ts                TypeScript 类型定义 + 常量枚举
├── stores/                     Zustand 全局状态
│   ├── auth.ts                 用户认证状态
│   ├── chat.ts                 聊天面板状态 + localStorage 持久化
│   └── notification.ts         通知状态 + WebSocket 推送
├── router/                     路由
│   ├── index.tsx               路由定义（BrowserRouter）
│   └── AuthGuard.tsx           认证守卫
├── views/                      页面组件（8 个）
│   ├── LoginPage.tsx           登录/注册
│   ├── DashboardPage.tsx       首页（公开文章列表）
│   ├── WorkspacePage.tsx       工作台（我的知识库/团队）
│   ├── ArticleDetailPage.tsx   文章阅读（评论/点赞/收藏/聊天）
│   ├── ArticleEditPage.tsx     文章编辑（Yjs 协同）
│   ├── KbDetailPage.tsx        知识库详情
│   ├── TeamDetailPage.tsx      团队详情
│   └── ProfilePage.tsx         个人资料
├── components/                 通用组件
│   ├── Navbar.tsx              顶部导航栏
│   ├── Sidebar.tsx             左侧边栏
│   ├── Modal.tsx               通用模态框
│   ├── ChatSidebar.tsx         聊天侧边栏
│   ├── NotificationPanel.tsx   通知面板
│   └── Editor/Editor.tsx       Yjs 编辑器
├── hooks/                      自定义 Hooks
│   ├── useCollabProvider.ts    协同编辑 Provider 管理
│   ├── useChat.ts              文章聊天 WebSocket
│   └── useTheme.tsx            主题切换
├── App.tsx                     布局骨架
└── main.tsx                    入口
```

## 3. 路由

### 3.1 路由定义

路由定义在 `router/index.tsx`：

```tsx
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/article/:id/edit", element: <AuthGuard><ArticleEditPage /></AuthGuard> },
  {
    path: "/",
    element: <AuthGuard><App /></AuthGuard>,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "workspace", element: <WorkspacePage /> },
      { path: "team/:id", element: <TeamDetailPage /> },
      { path: "kb/:id", element: <KbDetailPage /> },
      { path: "article/:id", element: <ArticleDetailPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
]);
```

### 3.2 路由表

| 路径 | 页面 | 守卫 | 说明 |
|---|---|---|---|
| `/login` | LoginPage | 公开 | 登录/注册页 |
| `/` | DashboardPage | 需登录 | 首页，公开文章列表 |
| `/workspace` | WorkspacePage | 需登录 | 工作台，我的知识库和团队 |
| `/article/:id` | ArticleDetailPage | 需登录 | 文章阅读 |
| `/article/:id/edit` | ArticleEditPage | 需登录 | 文章编辑（协同） |
| `/kb/:id` | KbDetailPage | 需登录 | 知识库详情 |
| `/team/:id` | TeamDetailPage | 需登录 | 团队详情 |
| `/profile` | ProfilePage | 需登录 | 个人资料 |

### 3.3 认证守卫

`AuthGuard`（`router/AuthGuard.tsx`）：

```text
1. useEffect 调用 useAuthStore.init() 获取当前用户
2. initialized=false → 显示 loading spinner
3. user=null → Navigate to /login
4. user 存在 → 渲染 children
```

## 4. 布局

### 4.1 应用骨架

`App.tsx` 提供整体布局：

```text
┌─────────────────────────────────────────────────┐
│  Navbar                                          │  ← 顶部导航
├──────────┬──────────────────────────────────────┤
│          │                                       │
│  Sidebar │  <Outlet /> (页面内容)                │  ← 左侧边栏 + 主内容区
│          │                                       │
│          │                                       │
└──────────┴──────────────────────────────────────┘
```

- `Navbar`：搜索框、通知铃铛（未读数 badge）、用户菜单。
- `Sidebar`：导航菜单（首页、工作台、团队列表等）。
- `<Outlet />`：React Router 子路由渲染位置。

## 5. API 层

### 5.1 请求核心

`api/index.ts` 的核心函数：

```text
request<T>(path, init, fallbackMessage)
  ├─ fetch(`${API_ROOT}${path}`, { credentials: "include", ... })
  ├─ parseJson(response) — 安全解析（空 body → null）
  ├─ HTTP 非 2xx → throw Error
  └─ unwrapEnvelope<T>(payload) — 解包 { code, message, data }
     ├─ code !== 200 → throw Error
     └─ 返回 data

query(params) — 构造 URLSearchParams，过滤 null/undefined/空值
upload(path, file) — FormData 上传，不设 Content-Type
```

### 5.2 API 模块

| 模块 | 变量 | 涵盖接口 |
|---|---|---|
| 认证 | `authApi` | login, register, logout, current, sendEmailCode, resetPassword, wsToken |
| 用户 | `userApi` | profile, updateNickname, updateAvatar, changePassword, follow/unfollow, following/followers, favorites, history |
| 文章 | `articleApi` | create, update, delete, detail, listByKb, publicList, save, like/unlike, comment/comments/deleteComment, favorite/unfavorite, visit |
| 团队 | `teamApi` | create, list, detail, pendingInvites, invite, accept/reject, quit, updateRole, members, kick, cancelInvite |
| 知识库 | `kbApi` | create, update, delete, detail, listByOwner, listMine, authorize, accept/reject, cancelInvite, updateRole, pendingInvites, members, removeMember |
| 聊天 | `chatApi` | history, send |
| 搜索 | `searchApi` | articles, kbs, users |
| 上传 | `uploadApi` | image, avatar |
| 通知 | `notificationApi` | list, unreadCount, read, readAll, delete |

### 5.3 类型定义

`api/types.ts` 定义所有 VO 类型：

| 类型 | 用途 |
|---|---|
| `User` | 用户基础信息 |
| `UserProfile` | 用户资料（含 followingCount/followerCount/isFollowed） |
| `ArticleVO` | 文章详情（含 authorName/likeCount/liked/canEdit） |
| `TeamVO` | 团队信息（含 ownerName/memberCount） |
| `TeamMemberVO` | 团队成员（含 username/nickname/roleName） |
| `KbMemberVO` | 知识库成员 |
| `CommentItem` | 评论（含 replyToNickname） |
| `ChatMessage` | 聊天消息 |
| `NotificationItem` | 通知 |
| `FollowUser` | 关注/粉丝列表项 |
| `FavoriteArticle` | 收藏文章列表项 |
| `HistoryArticle` | 浏览记录列表项 |
| `SearchUser` | 搜索用户结果 |
| `PendingInvite` | 团队待处理邀请 |
| `KbPendingInvite` | 知识库待处理邀请 |

### 5.4 常量枚举

```typescript
TeamRole      = { OWNER: 0, ADMIN: 1, MEMBER: 2 }
KbRole        = { OWNER: 0, ADMIN: 1, EDITOR: 2, VIEWER: 3 }
JoinStatus    = { INVITED: 0, ACCEPTED: 1, REJECTED: 2 }
Visibility    = { PRIVATE: 0, PUBLIC: 1 }
OwnerType     = { USER: 0, TEAM: 1 }
ArticleStatus = { DRAFT: 0, PUBLISHED: 1 }
NotificationType = { TEAM_INVITE: 0, KB_INVITE: 1, TEAM_NEW_ARTICLE: 2, COMMENT: 3, LIKE: 4, MEMBER_CHANGE: 5, FOLLOW: 6, FOLLOW_ARTICLE: 7 }
```

## 6. 状态管理

### 6.1 认证状态 (`stores/auth.ts`)

Zustand store，管理用户登录态。

```typescript
interface AuthState {
  user: User | null;
  loading: boolean;
  initialized: boolean;

  init: () => Promise<void>;          // 调用 authApi.current() 获取用户
  login: (username, password, turnstileToken) => Promise<void>;
  register: (username, password, email, code) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
}
```

使用方式：

```tsx
// 组件内
const { user, login } = useAuthStore();

// 路由守卫等组件外场景
export const AuthContext = createContext<AuthState>(null!);
export const useAuth = () => useContext(AuthContext);
```

### 6.2 聊天状态 (`stores/chat.ts`)

两部分：

1. **Zustand store** — `openMap: Record<number, boolean>`，管理每篇文章的聊天面板开关状态，持久化到 localStorage。
2. **纯函数辅助** — `getChatMessages` / `saveChatMessages` / `clearChatMessages`，管理每篇文章的聊天消息 localStorage 缓存。

```typescript
// Zustand
const { openMap, setOpen } = useChatStore();

// 纯函数
const messages = getChatMessages(articleId);
saveChatMessages(articleId, newMessages);
```

### 6.3 通知状态 (`stores/notification.ts`)

Zustand store，管理通知列表和 WebSocket 连接。

```typescript
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
  connectWs: () => void;           // 自动获取 ws-token 并建连
  disconnectWs: () => void;        // 关闭连接，阻止重连
}
```

WebSocket 重连机制：

```text
connectWs()
  → authApi.wsToken()
  → new WebSocket(wsUrl)
  → onmessage: notification / unread_count
  → onclose: 5s 后自动重连
  → onerror: ws.close() → 触发 onclose → 重连
```

## 7. 自定义 Hooks

### 7.1 useCollabProvider

管理 Yjs 协同编辑的完整生命周期。

```typescript
function useCollabProvider(
  documentId: string,   // 文章 ID
  username: string,     // 当前用户名
): UseCollabProviderReturn
```

**返回值：**

| 字段 | 类型 | 说明 |
|---|---|---|
| yDoc | `Y.Doc \| null` | Yjs 文档实例 |
| awareness | `Awareness \| null` | 在线状态管理 |
| provider | `WebsocketProvider \| null` | WebSocket 连接 |
| status | `CollabStatus` | 连接状态：connecting/connected/disconnected |
| users | `CollabUser[]` | 在线用户列表 |

**生命周期：**

```text
documentId 变化:
  1. 销毁旧 provider / awareness / yDoc
  2. 创建新 Y.Doc + Awareness
  3. 获取 ws-token
  4. 创建 WebsocketProvider (ws://host/api/collaboration/{docId}?token=)
  5. 设置 awareness 本地状态 (name + color)
  6. 监听 status/connection-close/connection-error 事件
  7. 断线 3s 自动重连

组件卸载:
  1. 清理 retryTimer
  2. provider.destroy()
  3. awareness.destroy()
  4. doc.destroy()
```

**颜色分配：** 根据用户名 hash 从 10 种预设颜色中选择，保证同一用户颜色一致。

### 7.2 useChat

管理文章聊天 WebSocket 连接。

```typescript
function useChat(articleId: number): UseChatReturn
```

**返回值：**

| 字段 | 类型 | 说明 |
|---|---|---|
| messages | `ChatMsg[]` | 消息列表 |
| onlineUsers | `OnlineUser[]` | 在线用户 |
| send | `(content: string) => void` | 发送消息 |
| connected | `boolean` | 连接状态 |

**特性：**

- `articleId=0` 时不连接（聊天面板关闭状态）。
- 自动重连（3s 间隔），卸载后停止。
- 消息去重：基于服务端 `id` 或 `senderId+content+time`。
- 离线消息队列：断线期间的消息在重连后自动发送。
- 消息持久化到 localStorage（500ms debounce）。
- 切换文章时自动加载缓存消息。

**WebSocket 消息类型：**

| type | 方向 | 说明 |
|---|---|---|
| `chat` | 双向 | 聊天消息 |
| `system` | 服务端→客户端 | 系统消息（加入/离开） |
| `online` | 服务端→客户端 | 在线用户列表 |

## 8. 页面组件

### 8.1 LoginPage

登录/注册页，支持：

- 登录表单（用户名 + 密码 + Turnstile）
- 注册表单（用户名 + 密码 + 邮箱 + 验证码 + Turnstile）
- 忘记密码（邮箱 + 验证码 + 新密码）
- 邮箱验证码发送后重置 Turnstile；后端按 `type + email` 做 60 秒发送冷却，重复发送显示 429 错误文案
- Tab 切换登录/注册/重置密码

### 8.2 DashboardPage

首页，展示：

- 公开文章列表（分页、按时间/点赞排序、关键词搜索）
- 每篇文章卡片：标题、摘要、作者、点赞数、更新时间

### 8.3 WorkspacePage

工作台，展示：

- 我的个人知识库列表
- 我的团队列表
- 创建知识库/团队入口

### 8.4 ArticleDetailPage

文章阅读页，包含：

- 文章标题、内容渲染
- 作者信息、点赞/收藏按钮
- 评论列表（支持多级回复）
- 聊天侧边栏（ChatSidebar 组件）

### 8.5 ArticleEditPage

文章编辑页，核心组件：

- Yjs 协同编辑器（Editor 组件）
- 在线用户列表（来自 useCollabProvider）
- 连接状态指示器
- 保存按钮（调用 articleApi.save）

### 8.6 KbDetailPage

知识库详情页：

- 知识库信息
- 文章列表
- 成员管理（邀请、角色变更）

### 8.7 TeamDetailPage

团队详情页：

- 团队信息
- 成员列表
- 邀请、踢人、角色变更

### 8.8 ProfilePage

个人资料页：

- 用户信息编辑（昵称、头像）
- 密码修改
- 关注/粉丝列表
- 收藏文章列表
- 浏览记录

## 9. 通用组件

### 9.1 Navbar

顶部导航栏，包含：

- Logo/品牌名
- 搜索框（全局搜索）
- 通知铃铛（显示未读数 badge，点击展开 NotificationPanel）
- 用户菜单（头像 + 下拉：个人资料、退出）

### 9.2 Sidebar

左侧导航栏，包含：

- 首页
- 工作台
- 我的团队（列表）
- 我的收藏

### 9.3 ChatSidebar

文章聊天侧边栏：

- 在线用户头像列表
- 消息列表（自动滚动到底部）
- 消息输入框
- 通过 `useChat(articleId)` hook 管理连接和消息

### 9.4 NotificationPanel

通知面板：

- 通知列表（类型图标、标题、时间）
- 未读/已读状态
- 标记已读/全部已读
- 删除通知
- 通过 `useNotificationStore` 管理状态和 WebSocket

### 9.5 Modal

通用模态框组件，用于：

- 创建知识库/团队
- 邀请成员
- 确认操作

### 9.6 Editor

Yjs 协同编辑器组件：

- 集成 `useCollabProvider` hook
- 显示在线用户和连接状态
- Yjs 内容绑定到编辑器 UI

## 10. 开发指南

### 10.1 开发环境启动

```bash
cd frontend
npm install
npm run dev    # Vite dev server, 默认 :5173
```

### 10.2 构建

```bash
npm run build  # 输出到 dist/
```

### 10.3 新增页面步骤

1. 在 `views/` 创建页面组件。
2. 在 `router/index.tsx` 添加路由。
3. 如需登录保护，包裹 `<AuthGuard>`。
4. 在 `Sidebar` 添加导航链接。

### 10.4 新增 API 步骤

1. 在 `api/types.ts` 添加类型定义。
2. 在 `api/index.ts` 对应模块添加 API 函数。
3. 在页面/组件中调用。

### 10.5 状态管理规范

- **全局状态**用 Zustand store（`stores/`）。
- **组件局部状态**用 `useState`。
- **跨组件共享状态**考虑提升到 store 或通过 props 传递。
- **WebSocket 连接**封装在自定义 hook 中（`hooks/`），store 只管理数据。

### 10.6 样式规范

- 使用 Tailwind CSS 工具类。
- 自定义主题色通过 CSS 变量（`useTheme` hook）。
- 响应式设计优先考虑桌面端。

## 11. 前端取舍

| 选择 | 收益 | 成本 |
|---|---|---|
| React + Vite | 启动快、生态完整、组件化清晰 | 需要约束页面状态膨胀 |
| Zustand | 登录态实现轻、样板少 | 大型状态流需后续规范 action 和 selector |
| API 函数集中管理 | 类型集中、调用简单 | 文件已 490+ 行，后续可按 domain 拆分 |
| Yjs 客户端协同 | 冲突合并成熟、无需服务端 CRDT | 后端不理解 CRDT，持久化策略需加强 |
| localStorage 聊天缓存 | 离线/刷新不丢消息 | 容量有限，大量消息需清理策略 |
