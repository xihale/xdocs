# CloudDoc API 参考

## 1. 概览

### 1.1 基础信息

| 项目 | 值 |
|---|---|
| 基路径 | `/api` |
| 协议 | HTTP/HTTPS + WebSocket |
| 认证 | JWT Cookie (`xdocs_token`) |
| Content-Type | `application/json`（大部分接口） |
| 文件上传 | `multipart/form-data` |

### 1.2 统一响应格式

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

失败：

```json
{
  "code": 401,
  "message": "未登录",
  "data": null
}
```

### 1.3 HTTP 状态码

| HTTP Code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 / 业务异常 |
| 401 | 未登录 |
| 403 | 无权限 / 已封禁 / CSRF 失败 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.4 认证方式

- **HTTP 接口**：自动携带 `xdocs_token` Cookie。标记 `@Public` 的接口无需登录。
- **WebSocket 接口**：先调 `GET /api/auth/ws-token` 获取短期 token，连接时放入 query param `?token=<jwt>`。

---

## 2. 认证 `/api/auth`

### 2.1 POST `/api/auth/register`

注册新用户。

**公开接口**，无需登录。

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名（唯一） |
| password | string | 是 | 密码 |
| email | string | 是 | 邮箱（唯一） |
| code | string | 是 | 邮箱验证码 |
| nickname | string | 否 | 昵称 |

**响应 data：** `User` 对象（不含 password）。

### 2.2 POST `/api/auth/login`

登录。

**公开接口**。

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| turnstileToken | string | 是 | Cloudflare Turnstile 人机验证 token |

**响应 data：** `User` 对象（不含 password）。同时设置 `xdocs_token` Cookie。

### 2.3 POST `/api/auth/logout`

登出。清除 Cookie。

**公开接口**。

### 2.4 GET `/api/auth/current`

获取当前登录用户。

**公开接口**。

**响应 data：** `User` 对象或 `null`。

### 2.5 POST `/api/auth/send-code`

发送邮箱验证码。

**公开接口**。

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| email | string | 是 | 目标邮箱 |
| turnstileToken | string | 是 | Turnstile token |
| type | string | 否 | `register`（默认）或 `reset` |

**限制：**

- 验证码有效期 5 分钟。
- 同一 `type + email` 60 秒内只能发送一次；重复请求返回 `429`。
- `type=register` 时目标邮箱必须未注册；`type=reset` 时目标邮箱必须已注册。

**常见错误：**

| HTTP 状态 | 说明 |
|---|---|
| 400 | Turnstile 校验失败 / 邮箱状态不符合当前类型 |
| 429 | 验证码发送过于频繁，请稍后再试 |

### 2.6 POST `/api/auth/reset-password`

重置密码。

**公开接口**。

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| email | string | 是 | 邮箱 |
| code | string | 是 | 验证码 |
| newPassword | string | 是 | 新密码 |

### 2.7 GET `/api/auth/ws-token`

获取 WebSocket 连接用的短期 token。

**需登录**。

**响应 data：**

```json
{ "token": "eyJhbGciOi..." }
```

---

## 3. 用户 `/api/user`

### 3.1 GET `/api/user/profile`

获取用户资料（含社交统计）。

**需登录**。

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | int | 否 | 目标用户 ID，不传则查当前用户 |

**响应 data：** `UserProfile`

```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "nickname": "管理员",
  "avatarUrl": "/uploads/avatar/xxx.png",
  "role": 0,
  "status": 0,
  "followingCount": 10,
  "followerCount": 5,
  "isFollowed": false
}
```

### 3.2 POST `/api/user/update-nickname`

修改昵称。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| nickname | string | 是 |

### 3.3 POST `/api/user/update-avatar`

修改头像 URL。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| avatarUrl | string | 是 |

### 3.4 POST `/api/user/change-password`

修改密码。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| oldPassword | string | 是 |
| newPassword | string | 是 |

### 3.5 POST `/api/user/follow`

关注用户。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| userId | int | 是 |

**响应 data：** `{ "followed": true }`

### 3.6 POST `/api/user/unfollow`

取消关注。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| userId | int | 是 |

**响应 data：** `{ "followed": false }`

### 3.7 GET `/api/user/following`

获取关注列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| userId | int | 否 |

**响应 data：** `FollowUser[]`

### 3.8 GET `/api/user/followers`

获取粉丝列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| userId | int | 否 |

**响应 data：** `FollowUser[]`

### 3.9 GET `/api/user/favorites`

获取收藏文章列表。

**需登录**。

**响应 data：** `FavoriteArticle[]`

### 3.10 GET `/api/user/history`

获取浏览记录。

**需登录**。

**响应 data：** `HistoryArticle[]`（最多 50 条）

### 3.11 DELETE `/api/user/history-delete`

删除浏览记录。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| articleId | int | 否 | 指定文章 ID，不传则清空全部 |

---

## 4. 文章 `/api/article`

### 4.1 POST `/api/article/create`

创建文章。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| kbId | int | 是 | 知识库 ID |
| title | string | 是 | 标题 |
| content | string | 否 | 初始内容 |

**响应 data：** `Article` 对象。

### 4.2 PUT `/api/article/update`

更新文章。

**需登录**。需要编辑权限。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | int | 是 | 文章 ID |
| title | string | 否 | 标题 |
| content | string | 否 | 内容 |
| summary | string | 否 | 摘要 |
| status | int | 否 | 0=草稿, 1=公开 |

**响应 data：** `Article` 对象。

### 4.3 DELETE `/api/article/delete`

删除文章。

**需登录**。需要编辑权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

### 4.4 GET `/api/article/detail`

获取文章详情。

**公开接口**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

**响应 data：** `ArticleVO`

```json
{
  "id": 1,
  "knowledgeBaseId": 1,
  "title": "入门指南",
  "summary": "快速上手 CloudDoc",
  "content": "# Hello\n\n世界",
  "authorId": 1,
  "authorName": "admin",
  "authorAvatar": "/uploads/avatar/xxx.png",
  "kbName": "默认知识库",
  "status": 1,
  "likeCount": 5,
  "liked": false,
  "canEdit": true,
  "createTime": "2025-01-01T00:00:00",
  "updateTime": "2025-01-01T00:00:00"
}
```

### 4.5 GET `/api/article/list`

获取知识库内文章列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |

**响应 data：** `ArticleVO[]`

### 4.6 GET `/api/article/public-list`

获取公开文章列表。

**公开接口**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| offset | int | 否 | 偏移量，默认 0 |
| limit | int | 否 | 每页数量，默认 20 |
| sort | string | 否 | `time`（默认）或 `likes` |
| keyword | string | 否 | 搜索关键词 |

**响应 data：** `ArticleVO[]`

### 4.7 POST `/api/article/save`

保存文章内容（协同编辑后的显式保存）。

**需登录**。需要编辑权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |
| content | string | 否 |

**响应 data：** `Article` 对象。

### 4.8 POST `/api/article/like`

点赞文章。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

**响应 data：** `{ "liked": true, "likeCount": 6 }`

### 4.9 POST `/api/article/unlike`

取消点赞。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

**响应 data：** `{ "liked": false, "likeCount": 5 }`

### 4.10 POST `/api/article/comment`

发表评论。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| articleId | int | 是 | |
| content | string | 是 | |
| parentId | int | 否 | 父评论 ID（回复时传） |
| replyToId | int | 否 | 回复目标评论 ID |

**响应 data：** `{ "id": 42 }`

### 4.11 GET `/api/article/comments`

获取文章评论列表。

**公开接口**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

**响应 data：** `CommentItem[]`

### 4.12 DELETE `/api/article/comment-delete`

删除评论。

**需登录**。仅评论作者可删除。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

### 4.13 POST `/api/article/favorite`

收藏文章。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

**响应 data：** `{ "favorited": true }`

### 4.14 POST `/api/article/unfavorite`

取消收藏。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

**响应 data：** `{ "favorited": false }`

### 4.15 POST `/api/article/visit`

记录浏览。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | int | 是 |

---

## 5. 团队 `/api/team`

### 5.1 POST `/api/team/create`

创建团队。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| name | string | 是 |
| description | string | 否 |

**响应 data：** `TeamVO`

### 5.2 GET `/api/team/list`

获取当前用户所属团队列表。

**需登录**。

**响应 data：** `TeamVO[]`

### 5.3 GET `/api/team/pending-invites`

获取待处理的团队邀请。

**需登录**。

**响应 data：** `PendingInvite[]`

### 5.4 GET `/api/team/detail`

获取团队详情。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

**响应 data：** `TeamVO`

### 5.5 POST `/api/team/invite`

邀请用户加入团队。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| teamId | int | 是 | |
| userId | int | 否 | 用户 ID（与 username 二选一） |
| username | string | 否 | 用户名（与 userId 二选一） |

### 5.6 POST `/api/team/accept`

接受团队邀请。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| teamId | int | 是 |

### 5.7 POST `/api/team/reject`

拒绝团队邀请。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| teamId | int | 是 |

### 5.8 POST `/api/team/quit`

退出团队。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| teamId | int | 是 |

### 5.9 POST `/api/team/update-role`

修改成员角色。

**需登录**。需要 OWNER 权限。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| teamId | int | 是 | |
| userId | int | 是 | 目标用户 |
| role | int | 否 | 0=OWNER, 1=ADMIN, 2=MEMBER |

### 5.10 GET `/api/team/members`

获取团队成员列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

**响应 data：** `TeamMemberVO[]`

### 5.11 POST `/api/team/kick`

踢出成员。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| teamId | int | 是 |
| userId | int | 是 |

### 5.12 POST `/api/team/cancel-invite`

取消邀请。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| teamId | int | 是 |
| userId | int | 是 |

---

## 6. 知识库 `/api/kb`

### 6.1 POST `/api/kb/create`

创建知识库。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | |
| description | string | 否 | |
| visibility | int | 否 | 0=私有（默认）, 1=公开 |
| ownerType | int | 否 | 0=个人（默认）, 1=团队 |
| ownerId | int | 条件必填 | ownerType=1 时必填（团队 ID） |

**响应 data：** `KnowledgeBase` 对象。

### 6.2 PUT `/api/kb/update`

更新知识库。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |
| name | string | 否 |
| description | string | 否 |

**响应 data：** `KnowledgeBase` 对象。

### 6.3 DELETE `/api/kb/delete`

删除知识库。

**需登录**。需要 OWNER 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

### 6.4 GET `/api/kb/detail`

获取知识库详情。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

**响应 data：** `KnowledgeBase` 对象。

### 6.5 GET `/api/kb/list`

按归属获取知识库列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| ownerType | int | 否 | 0=个人, 1=团队 |
| ownerId | int | 是 |

**响应 data：** `KnowledgeBase[]`

### 6.6 GET `/api/kb/list-mine`

获取当前用户的个人知识库列表。

**需登录**。

**响应 data：** `KnowledgeBase[]`

### 6.7 GET `/api/kb/members`

获取知识库成员列表。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

**响应 data：** `KbMemberVO[]`

### 6.8 POST `/api/kb/authorize`

授权用户加入知识库。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| kbId | int | 是 | |
| userId | int | 否 | 用户 ID（与 username 二选一） |
| username | string | 否 | 用户名 |
| role | int | 否 | 0=OWNER, 1=ADMIN, 2=EDITOR, 3=VIEWER |

### 6.9 POST `/api/kb/accept`

接受知识库邀请。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |

### 6.10 POST `/api/kb/reject`

拒绝知识库邀请。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |

### 6.11 POST `/api/kb/cancel-invite`

取消知识库邀请。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |
| userId | int | 是 |

### 6.12 POST `/api/kb/update-role`

修改知识库成员角色。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |
| userId | int | 是 |
| role | int | 否 |

### 6.13 GET `/api/kb/pending-invites`

获取待处理的知识库邀请。

**需登录**。

**响应 data：** `KbPendingInvite[]`

### 6.14 POST `/api/kb/remove-member`

移除知识库成员。

**需登录**。需要 OWNER/ADMIN 权限。

| 参数 | 类型 | 必填 |
|---|---|---|
| kbId | int | 是 |
| userId | int | 是 |

---

## 7. 聊天 `/api/chat`

### 7.1 GET `/api/chat/history`

获取文章聊天历史。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| articleId | int | 是 | |
| limit | int | 否 | 默认 50 |

**响应 data：** `ChatMessageVO[]`

### 7.2 POST `/api/chat/send`

发送聊天消息（HTTP 方式，WebSocket 也可发）。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| articleId | int | 是 | |
| content | string | 是 | |
| teamId | int | 否 | 团队上下文 |
| messageType | int | 否 | 0=文本（默认）, 1=系统 |

### 7.3 GET `/api/chat/online-members`

获取文章聊天在线用户。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| articleId | string | 是 |

**响应 data：**

```json
[
  { "userId": 1, "nickname": "admin", "avatarUrl": "/uploads/avatar/xxx.png" }
]
```

---

## 8. 搜索 `/api/search`

### 8.1 GET `/api/search/articles`

搜索公开文章。

**公开接口**。

| 参数 | 类型 | 必填 |
|---|---|---|
| keyword | string | 是 |
| offset | int | 否 |
| limit | int | 否 |

**响应 data：** `ArticleVO[]`

### 8.2 GET `/api/search/kbs`

搜索公开知识库。

**公开接口**。

| 参数 | 类型 | 必填 |
|---|---|---|
| keyword | string | 是 |

**响应 data：** `KnowledgeBase[]`

### 8.3 GET `/api/search/users`

搜索用户。

**公开接口**。

| 参数 | 类型 | 必填 |
|---|---|---|
| keyword | string | 是 |

**响应 data：** `SearchUser[]`（脱敏，不含密码和邮箱）

---

## 9. 通知 `/api/notification`

### 9.1 GET `/api/notification/list`

获取通知列表。

**需登录**。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| offset | int | 否 | 默认 0 |
| limit | int | 否 | 默认 20 |

**响应 data：** `NotificationItem[]`

### 9.2 GET `/api/notification/unread-count`

获取未读通知数。

**需登录**。

**响应 data：** `number`

### 9.3 POST `/api/notification/read`

标记通知已读。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

### 9.4 POST `/api/notification/read-all`

标记全部通知已读。

**需登录**。

### 9.5 DELETE `/api/notification/delete`

删除通知。

**需登录**。

| 参数 | 类型 | 必填 |
|---|---|---|
| id | int | 是 |

---

## 10. 上传 `/api/upload`

### 10.1 POST `/api/upload/image`

上传文章图片。

**需登录**。

**请求：** `multipart/form-data`，字段名 `file`。

**响应 data：**

```json
{
  "url": "/uploads/image/abc123.png",
  "fileName": "screenshot.png"
}
```

### 10.2 POST `/api/upload/avatar`

上传头像（同时更新用户头像 URL）。

**需登录**。

**请求：** `multipart/form-data`，字段名 `file`。

**响应 data：**

```json
{
  "url": "/uploads/avatar/def456.jpg"
}
```

---

## 11. WebSocket 接口

### 11.1 文章聊天

**端点：** `ws(s)://host/api/chat/ws/{articleId}?token=<jwt>`

**消息格式（服务端 → 客户端）：**

```json
// 聊天消息
{ "type": "chat", "id": 42, "articleId": 1, "senderId": 1, "senderName": "admin", "senderAvatar": "/uploads/avatar/xxx.png", "content": "你好", "createTime": "2025-01-01T12:00:00" }

// 系统消息
{ "type": "system", "content": "admin 加入了聊天" }

// 在线用户列表
{ "type": "online", "users": [{ "userId": 1, "nickname": "admin", "avatarUrl": "/uploads/avatar/xxx.png" }] }
```

**消息格式（客户端 → 服务端）：**

```json
{ "type": "chat", "content": "你好" }
```

### 11.2 协同编辑

**端点：** `ws(s)://host/api/collaboration/{docId}?token=<jwt>`

**协议：** y-websocket 二进制协议。

| 消息类型值 | 类型 | 说明 |
|---|---|---|
| 0 | Sync | SyncStep1=0, SyncStep2=1, Update=2 |
| 1 | Awareness | 在线状态/光标 |
| 3 | QueryAwareness | 请求 awareness |

### 11.3 通知推送

**端点：** `ws(s)://host/api/notification/ws?token=<jwt>`

**消息格式（服务端 → 客户端）：**

```json
// 新通知
{ "type": "notification", "data": { "id": 1, "type": 6, "title": "新关注", "content": "user1 关注了你", "isRead": 0, "createTime": "2025-01-01T12:00:00" } }

// 未读数更新
{ "type": "unread_count", "count": 5 }
```

---

## 12. 公开接口汇总

以下接口无需登录即可访问：

| 接口 | 方法 |
|---|---|
| `/api/auth/register` | POST |
| `/api/auth/login` | POST |
| `/api/auth/logout` | POST |
| `/api/auth/current` | GET |
| `/api/auth/send-code` | POST |
| `/api/auth/reset-password` | POST |
| `/api/article/detail` | GET |
| `/api/article/public-list` | GET |
| `/api/article/comments` | GET |
| `/api/search/articles` | GET |
| `/api/search/kbs` | GET |
| `/api/search/users` | GET |
