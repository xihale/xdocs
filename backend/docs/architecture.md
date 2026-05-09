# CloudDoc 后端架构文档

## 1. 项目概览

CloudDoc 是一个多人协作文档平台后端，基于 **Java 21 + 原生 Servlet + JDBC** 构建，不依赖 Spring 等框架。核心能力包括文档管理、知识库组织、团队协作、实时聊天、通知中心与协同编辑。

| 属性 | 值 |
|------|-----|
| 语言 | Java 21 (enable-preview) |
| Web 容器 | Jetty 12 (Embedded, Virtual Threads) |
| 数据库 | MariaDB |
| 构建工具 | Maven |
| 打包方式 | WAR |

---

## 2. 整体架构

```
                        +-----------------+
                        |    Frontend     |
                        |   (Vue 3 SPA)   |
                        +--------+--------+
                                 |
                          HTTP / WebSocket
                                 |
              +------------------+------------------+
              |                  |                  |
        +-----+------+   +------+------+   +-------+-------+
        |  CORS      |   |  CSRF      |   |  Exception    |
        |  Filter    |   |  Filter    |   |  Filter       |
        +-----+------+   +------+------+   +-------+-------+
              |                  |                  |
              +------------------+------------------+
                                 |
                          +------+------+
                          | Auth Filter |
                          +------+------+
                                 |
                    +------------+------------+
                    |            |            |
              +-----+---+  +----+----+  +----+----+
              | Servlet  |  |  Chat   |  | Collab  |
              | (REST)   |  |  WS     |  | WS      |
              +-----+---+  +----+----+  +----+----+
                    |            |            |
              +-----+--------------------------------+
              |            Service Layer             |
              +-----+--------------------------------+
                    |            |            |
              +-----+---+  +----+----+  +----+----+
              |   DAO   |  |   DAO   |  |   DAO   |
              +-----+---+  +----+----+  +----+----+
                    |
              +-----+---+
              |  JDBC   |
              |  Pool   |
              +-----+---+
                    |
              +-----+---+
              | MariaDB |
              +---------+
```

### 请求处理流程

1. **CorsFilter** — 处理跨域，OPTIONS 预检直接返回
2. **CsrfFilter** — 校验不安全方法 (POST/PUT/PATCH/DELETE) 的 Origin/Referer
3. **ExceptionFilter** — 最外层异常兜底，将 `BusinessException` 和未知异常统一转为 JSON 响应
4. **AuthFilter** — 从 Cookie 中提取 JWT，恢复用户状态，白名单路径免登录
5. **Servlet** — 路由分发到具体 handler 方法
6. **Service** — 业务逻辑与权限校验
7. **DAO** — 数据访问

---

## 3. 包结构

```
top.xihale.xdocs
├── config/          # 全局配置（CORS、JWT、存储、邮件）
├── constant/        # 枚举常量（角色、状态、可见性等）
├── dao/             # 数据访问层（静态方法，手写 SQL）
├── exception/       # 业务异常
├── filter/          # Servlet 过滤器（CORS、CSRF、认证、异常）
├── po/              # 持久化对象（Entity，对应数据库表）
├── service/         # 业务逻辑层（静态方法）
├── servlet/         # HTTP 接口层（文章、知识库、团队、通知等）
├── util/            # 工具类（JWT、密码、JSON、JDBC、SQL 构建器等）
├── vo/              # 视图对象（API 响应专用）
└── websocket/       # WebSocket 端点（聊天、协同编辑、通知）
```

---

## 4. 核心模块详解

### 4.1 路由框架 (`servlet/route/`)

项目实现了一套轻量级注解路由框架，替代传统 Servlet 的 `doGet/doPost` 分支判断。

**核心组件：**

| 类 | 职责 |
|----|------|
| `BaseServlet` | 所有业务 Servlet 的基类，拦截所有 HTTP 方法，委托给 `RouteRegistry` |
| `RouteRegistry` | `init()` 时扫描子类方法上的路由注解，建立 `RouteKey → RouteHandler` 映射 |
| `RouteKey` | `(RouteMethod, path)` 二元组，path 做尾斜杠归一化 |
| `RouteHandler` | 封装反射调用，方法签名固定为 `(HttpServletRequest, HttpResponse)` |
| `@Get` / `@Post` / `@Put` / `@Delete` / `@Patch` | 快捷注解，等价于 `@Route(method=GET, value=...)` |
| `@Route` | 通用路由注解，支持 `@Repeatable` |

**使用示例：**

```java
@WebServlet("/api/article/*")
public class ArticleServlet extends BaseServlet {

    @Get("/detail")
    private void handleDetail(HttpServletRequest req, HttpResponse res) throws IOException {
        // ...
    }

    @Post("/create")
    private void handleCreate(HttpServletRequest req, HttpResponse res) throws IOException {
        // ...
    }
}
```

**路由匹配规则：**
- 每个 Servlet 绑定一个 URL pattern（如 `/api/article/*`）
- 路由注解的 `value` 是相对于 Servlet 路径的子路径
- 请求到达时，取 `pathInfo` 与注册表匹配
- 未匹配返回 404

### 4.2 过滤器链 (`filter/`)

过滤器执行顺序由 `@WebFilter` 或 `web.xml` 定义：

| 过滤器 | 职责 | 顺序 |
|--------|------|------|
| `ExceptionFilter` | 最外层，捕获所有异常转 JSON | 1 (最外) |
| `CorsFilter` | 跨域头设置，OPTIONS 预检 | 2 |
| `CsrfFilter` | 校验写操作的 Origin/Referer | 3 |
| `AuthFilter` | JWT 解析，用户状态注入，白名单放行 | 4 (最内) |

**认证流程 (AuthFilter)：**
1. 从 Cookie `xdocs_token` 读取 JWT
2. `JwtUtil.getUserId(token)` 提取 userId
3. `UserService.findUserById()` 查数据库获取完整用户
4. 将 `userId`、`role`、`currentUser`、`banned` 写入 request attribute
5. 非白名单路径未登录返回 401，封禁返回 403

**白名单路径：** 登录、注册、登出、获取当前用户、验证码、重置密码、公开文章列表/详情/评论、搜索接口。

### 4.3 数据访问层 (`dao/`)

DAO 层采用**静态方法**模式，每个 DAO 对应一张数据库表。

**通用模式：**
```java
public class XxxDao {
    public static void insert(Xxx entity) { ... }
    public static int update(Xxx entity) { ... }
    public static int delete(Integer id) { ... }
    public static Optional<Xxx> findById(Integer id) { ... }
    public static List<Xxx> findBySomeCondition(...) { ... }
    private static Xxx mapRow(ResultSet rs) { ... }
}
```

**资源管理：** 所有 DAO 方法遵循 `try-finally` 模式，通过 `JdbcUtils.close()` 归还连接到连接池。

**辅助工具：**
- `SqlBuilder` — 链式 SQL 构建器，封装 JDBC 样板代码，支持 `queryList`、`queryOne`、`queryScalar`、`queryCount`、`queryExists`、`execute`、`executeReturnKey`、`executeBatch`、`inTransaction`
- `RowMapper<T>` — 函数式接口，`ResultSet → T`

### 4.4 连接池 (`util/ConnectionPool`)

自实现的数据库连接池，基于 `BlockingQueue`：

| 特性 | 实现 |
|------|------|
| 并发安全 | `ArrayBlockingQueue` + `AtomicInteger` |
| 动态扩容 | 按需创建，CAS 保证不超过 maxTotal |
| 等待超时 | `poll(maxWaitMillis)` |
| 连接校验 | 取出时 `isValid()`，失效自动重建 |
| 代理模式 | JDK 动态代理拦截 `close()`，改为归还池 |

**配置项（db.properties）：**

| 键 | 默认值 | 说明 |
|----|--------|------|
| `pool.minIdle` | 2 | 最小空闲连接 |
| `pool.maxTotal` | 20 | 最大连接数 |
| `pool.maxWaitMillis` | 5000 | 获取连接超时 |

### 4.5 业务逻辑层 (`service/`)

Service 层同样采用**静态方法**模式，负责：
- 业务校验（存在性、权限、状态）
- 跨 DAO 协调（如创建 Team 时同时创建 Owner 成员记录）
- 抛出 `BusinessException` 表示业务错误

**权限模型：**

```
平台级别：Role { USER, ADMIN }

团队级别：TeamRole { OWNER, ADMIN, MEMBER }
  └─ OWNER/ADMIN 可邀请、踢人
  └─ 只有 OWNER 可修改角色

知识库级别：KnowledgeBaseRole { OWNER, ADMIN, EDITOR, VIEWER }
  └─ OWNER/ADMIN 可授权、移除成员
  └─ 归属类型：OwnerType { USER（个人）, TEAM（团队） }

通知系统：NotificationService + NotificationServlet + NotificationWebSocket
  └─ 团队邀请、知识库邀请、团队新文章、评论、点赞、成员变动、关注、关注者文章发布/更新
```

### 4.6 WebSocket (`websocket/`)

两个 WebSocket 端点，均采用**房间模型**：

#### ChatWebSocket (`/api/chat/ws/{articleId}`)

- **协议：** 文本 JSON
- **认证：** 连接时通过 query param `token` 传递 JWT
- **房间：** 以 articleId 为 key，`ConcurrentHashMap<String, Set<Session>>`
- **消息类型：**
  - `chat` — 用户消息，持久化到 DB 后广播
  - `system` — 加入/离开通知
  - `online` — 在线用户列表

#### CollaborationWebSocket (`/api/collaboration/{docId}`)

- **协议：** 二进制，y-websocket 协议
- **认证：** 同上
- **消息类型：**
  - `0` (Sync) — SyncStep1/2/Update
  - `1` (Awareness) — 用户光标/选区状态
  - `3` (QueryAwareness) — 请求重新发送 awareness
- **状态管理：** 后端缓存每个房间的 `SyncStep2`（完整文档状态），新用户加入时发送
- **注意：** Java 端无 Yjs，Update 消息只转发不合并，依赖客户端 CRDT 保证一致性

#### NotificationWebSocket (`/api/notification/ws`)

- **协议：** 文本 JSON
- **认证：** 同上
- **房间：** 以 userId 为 key，`ConcurrentHashMap<Integer, Set<Session>>`
- **消息类型：**
  - `notification` — 通知对象，包含通知正文与跳转链接
  - `unread_count` — 未读数同步
- **状态管理：** `NotificationService.pushToUser()` 直接向在线用户推送

---

## 5. 数据模型

### 5.1 核心实体关系

```
User ──────────────────────────────────────────┐
  │                                            │
  ├── 1:N ── Article (author)                  │
  │            │                               │
  │            └── N:1 ── KnowledgeBase        │
  │                         │  │               │
  │                         │  └── N:M ── KnowledgeBaseMember ── User
  │                         │
  │                         └── OwnerType: USER | TEAM
  │                                    │
  │                              Team (if TEAM)
  │                                │  │
  │                                │  └── N:M ── TeamMember ── User
  │                                │
  ├── 1:N ── Comment (on Article)
  ├── 1:N ── ArticleLike
  ├── 1:N ── Favorite (polymorphic: article | kb)
  ├── 1:N ── FollowUser (follower → following)
  ├── 1:N ── RecentVisit
  ├── 1:N ── UploadFile
  └── 1:N ── ArticleChatMessage
```

### 5.2 实体表

| PO 类 | 数据库表 | 说明 |
|-------|----------|------|
| `User` | `sys_user` | 用户（username, password, email, role, status） |
| `Article` | `article` | 文章（属于 KnowledgeBase，有 status: 草稿/已发布） |
| `KnowledgeBase` | `knowledge_base` | 知识库（visibility: 私有/公开，ownerType: 个人/团队） |
| `Team` | `team` | 团队 |
| `Comment` | `comment` | 评论（支持嵌套：parentId, replyToId） |
| `TeamMember` | `team_member` | 团队-用户关系（role, joinStatus） |
| `KnowledgeBaseMember` | `knowledge_base_member` | 知识库-用户关系（role, inviteStatus） |
| `ArticleChatMessage` | `article_chat_message` | 文档聊天消息 |
| `UploadFile` | `upload_file` | 上传文件（bizType + bizId 多态关联） |

### 5.3 枚举常量

| 枚举 | 值 | 说明 |
|------|-----|------|
| `Role` | USER(0), ADMIN(1) | 平台角色 |
| `UserStatus` | NORMAL(0), BANNED(1) | 用户状态 |
| `ArticleStatus` | DRAFT(0), PUBLISHED(1) | 文章状态 |
| `Visibility` | PRIVATE(0), PUBLIC(1) | 知识库可见性 |
| `OwnerType` | USER(0), TEAM(1) | 知识库归属 |
| `TeamRole` | OWNER(0), ADMIN(1), MEMBER(2) | 团队角色 |
| `KnowledgeBaseRole` | OWNER(0), ADMIN(1), EDITOR(2), VIEWER(3) | 知识库角色 |
| `JoinStatus` | INVITED(0), ACCEPTED(1), REJECTED(2) | 邀请状态 |
| `MessageType` | TEXT(0), SYSTEM(1) | 聊天消息类型 |
| `ResponseCode` | 200/400/401/403/404/500 | HTTP 响应码 |

---

## 6. API 路由总览

### 认证 `/api/auth/*`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/register` | 注册 | 否 |
| POST | `/login` | 登录 | 否 |
| POST | `/logout` | 登出 | 否 |
| GET | `/current` | 获取当前用户 | 否 |
| POST | `/send-code` | 发送邮件验证码 | 否 |
| POST | `/reset-password` | 重置密码 | 否 |
| GET | `/ws-token` | 获取 WebSocket 专用 Token | 是 |

### 文章 `/api/article/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建文章 |
| PUT | `/update` | 更新文章 |
| DELETE | `/delete` | 删除文章 |
| GET | `/detail` | 文章详情 |
| GET | `/list` | 按知识库查询 |
| GET | `/public-list` | 公开文章列表（支持搜索、排序） |
| POST | `/save` | 保存内容 |
| POST | `/like` | 点赞 |
| POST | `/unlike` | 取消点赞 |
| POST | `/comment` | 添加评论 |
| GET | `/comments` | 评论列表 |
| DELETE | `/comment-delete` | 删除评论 |
| POST | `/favorite` | 收藏 |
| POST | `/unfavorite` | 取消收藏 |
| POST | `/visit` | 记录浏览 |

### 知识库 `/api/kb/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建知识库 |
| PUT | `/update` | 更新知识库 |
| DELETE | `/delete` | 删除知识库 |
| GET | `/detail` | 知识库详情 |
| GET | `/list` | 按归属查询 |
| GET | `/list-mine` | 我的个人知识库 |
| GET | `/members` | 成员列表 |
| POST | `/remove-member` | 移除成员 |
| POST | `/authorize` | 授权成员 |

### 团队 `/api/team/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建团队 |
| GET | `/list` | 我的团队列表 |
| GET | `/detail` | 团队详情 |
| GET | `/pending-invites` | 待处理邀请 |
| POST | `/invite` | 邀请成员 |
| POST | `/accept` | 接受邀请 |
| POST | `/reject` | 拒绝邀请 |
| POST | `/quit` | 退出团队 |
| POST | `/kick` | 踢出成员 |
| POST | `/update-role` | 修改成员角色 |
| GET | `/members` | 成员列表 |

### 用户 `/api/user/*`, `/api/users/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 用户列表 |
| GET | `/profile` | 用户详情 |
| POST | `/update-nickname` | 修改昵称 |
| POST | `/update-avatar` | 修改头像 |
| POST | `/change-password` | 修改密码 |
| POST | `/follow` | 关注 |
| POST | `/unfollow` | 取消关注 |
| GET | `/following` | 关注列表 |
| GET | `/followers` | 粉丝列表 |
| GET | `/favorites` | 收藏列表 |
| GET | `/history` | 浏览记录 |
| DELETE | `/history-delete` | 删除浏览记录 |

### 聊天 `/api/chat/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/history` | 聊天历史 |
| POST | `/send` | 发送消息 |
| GET | `/online-members` | 在线用户 |

### 搜索 `/api/search/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/articles` | 搜索公开文章 |
| GET | `/kbs` | 搜索公开知识库 |
| GET | `/users` | 搜索用户 |

### 上传 `/api/upload/*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/image` | 上传图片 |
| POST | `/avatar` | 上传头像 |

### WebSocket

| 端点 | 说明 |
|------|------|
| `/api/chat/ws/{articleId}?token=xxx` | 文档聊天 |
| `/api/collaboration/{docId}?token=xxx` | 协同编辑 (y-websocket) |

---

## 7. 统一响应格式

所有 HTTP API 返回统一 JSON 结构：

```json
{
    "code": 200,
    "message": "操作成功",
    "data": { ... }
}
```

- `Result.success(data)` — code=200
- `Result.error(code, message)` — 错误响应
- `BusinessException(code, message)` — Service 层抛出，由 `ExceptionFilter` 统一捕获转换

---

## 8. 安全机制

| 机制 | 实现 |
|------|------|
| 认证 | JWT 存储在 HttpOnly Cookie，`AuthFilter` 每次请求解析 |
| 密码 | BCrypt (cost=12) |
| CSRF | `CsrfFilter` 校验写操作的 Origin/Referer |
| CORS | `CorsFilter` 白名单域名，支持 Credentials |
| 人机验证 | Cloudflare Turnstile（登录、发送验证码时校验） |
| 邮箱验证 | 注册/重置密码需邮件验证码（Session 存储，5 分钟有效） |

---

## 9. 配置文件

| 文件 | 用途 |
|------|------|
| `web.properties` | CORS 域名、JWT 密钥/过期时间、Turnstile 密钥、邮件 SMTP |
| `db.properties` | 数据库连接、连接池参数 |
| `storage.properties` | 文件存储路径、允许的扩展名、最大文件大小 |

---

## 10. 技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jakarta Servlet API | 6.1.0 | HTTP 请求处理 |
| Jakarta WebSocket API | 2.2.0 | 实时通信 |
| MariaDB JDBC | 3.5.6 | 数据库驱动 |
| Gson | 2.13.2 | JSON 序列化 |
| JJWT | 0.12.6 | JWT 生成与验证 |
| BCrypt | 0.10.2 | 密码加密 |
| Jakarta Mail | 2.0.3 | 邮件发送 |
| Lombok | 1.18.44 | 减少样板代码 |
| Commons IO | 2.19.0 | IO 工具 |

**测试依赖：** JUnit 5、Mockito、AssertJ、JSONAssert。

---

## 11. 设计特点

### 自研路由框架
通过注解 + 反射实现类似 Spring MVC 的路由体验。`BaseServlet.init()` 时扫描子类方法上的 `@Get`/`@Post` 等注解，建立路由表，请求到达时自动分发。方法签名统一为 `(HttpServletRequest, HttpResponse): void`。

### 静态方法 Service/DAO
不使用 Spring IoC，所有 Service 和 DAO 方法都是 `static`。简单直接，适合项目规模。

### 自实现连接池
基于 `BlockingQueue` + JDK 动态代理，不依赖 HikariCP 等第三方池。代理拦截 `Connection.close()` 实现连接复用。

### 参数解析双通道
`BaseServlet` 的 `optionalParam()` 同时支持 query/form 参数和 JSON body，Gson 解析后缓存到 request attribute。

### 统一异常处理
`ExceptionFilter` 在过滤器链最外层捕获所有异常，`BusinessException` 转为对应 HTTP 状态码的 JSON 响应，未知异常返回 500 并记录日志。
