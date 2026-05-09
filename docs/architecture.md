# CloudDoc 架构设计

## 1. 设计目标

CloudDoc 是在线文档与知识库协作平台。设计优先级：

1. **功能闭环**：账户、知识库、团队、文章、评论、收藏、浏览记录、协同编辑、聊天、通知全链路可用。
2. **实现透明**：后端不用 Spring，采用 Servlet + JDBC + 自定义路由，降低框架黑盒。
3. **协同优先**：编辑体验用 Yjs CRDT，在客户端保证并发合并正确性，后端负责认证、房间、广播。
4. **安全基线**：JWT Cookie、CSRF Origin/Referer 校验、统一异常 JSON、权限服务层兜底。
5. **渐进扩展**：当前单体足够简单，保留服务拆分、消息总线、对象存储、全文搜索等演进空间。

## 2. 总体架构

```text
Browser / React SPA
  │ HTTP JSON / Multipart
  │ WebSocket (chat / collaboration / notification)
  ▼
Jetty 12 + Servlet WAR
  │ Filters: Exception → CORS → CSRF → Auth
  ▼
Servlet route layer (@Get/@Post/@Put/@Delete)
  ▼
Service layer: business rules + permissions
  ▼
DAO layer: BaseMapper ORM + SqlBuilder
  ▼
MariaDB
```

### 2.1 请求生命周期

每个 HTTP 请求经过四层过滤器链，最终到达业务 Servlet：

```text
Request
  → ExceptionFilter      全局异常兜底，BusinessException → JSON envelope
  → CorsFilter           CORS 响应头 + OPTIONS 预检
  → CsrfFilter           写操作校验 Origin/Referer（仅携带 Cookie 的不安全方法）
  → AuthFilter           JWT Cookie 解析 → userId/currentUser/role/status 写入 request attribute
  → BaseServlet.dispatch 注解路由分发 → handler 方法
    → Response
```

**WebSocket 请求**在所有过滤器中直接放行（`Upgrade: websocket`），认证由 `WebSocketConfigurator` + query param token 完成。

### 2.2 WebSocket 端点

| 端点 | 路径 | 用途 |
|---|---|---|
| `ChatWebSocket` | `/api/chat/ws/{articleId}?token=` | 文章聊天，文本消息 |
| `CollaborationWebSocket` | `/api/collaboration/{docId}?token=` | Yjs 协同编辑，二进制消息 |
| `NotificationWebSocket` | `/api/notification/ws?token=` | 实时通知推送 |

共用 `RoomManager` 单例管理房间（房间 ID → Session 集合），支持：
- 同用户去重：重连时踢掉旧 Session
- Awareness 清理：断连时广播 remove 消息
- 房间空时自动清理

## 3. 后端目录结构

```text
backend/src/main/java/top/xihale/clouddoc/
├── annotation/          ORM 注解
│   ├── Column.java      覆盖列名映射
│   ├── Id.java          标注主键字段
│   ├── Table.java       声明实体对应的表
│   └── Transient.java   标注不参与 ORM 的字段
├── config/              配置类
│   ├── StorageConfig    文件存储路径、扩展名白名单、大小限制
│   └── WebConfig        CORS 白名单、JWT 密钥、Turnstile、邮件 SMTP
├── constant/            业务常量枚举
│   ├── ArticleStatus    DRAFT=0, PUBLISHED=1
│   ├── JoinStatus       INVITED=0, ACCEPTED=1, REJECTED=2
│   ├── KnowledgeBaseRole OWNER=0, ADMIN=1, EDITOR=2, VIEWER=3
│   ├── MessageType      TEXT=0, SYSTEM=1
│   ├── NotificationType 团队邀请/知识库邀请/新文章/评论/点赞/成员变动/关注/关注者文章
│   ├── OwnerType        USER=0, TEAM=1
│   ├── ResponseCode     HTTP 状态码常量
│   ├── Role             USER=0, ADMIN=1
│   ├── TeamRole         OWNER=0, ADMIN=1, MEMBER=2
│   ├── UserStatus       NORMAL=0, BANNED=1
│   └── Visibility       PRIVATE=0, PUBLIC=1
├── dao/                 数据访问层（14 个 DAO）
│   ├── ArticleDao       继承 BaseMapper<Article>
│   ├── UserDao          继承 BaseMapper<User>
│   ├── ...              其他 8 个 BaseMapper 子类
│   ├── ArticleLikeDao   纯静态方法（无 PO）
│   ├── FavoriteDao      纯静态方法
│   ├── FollowUserDao    纯静态方法
│   └── RecentVisitDao   纯静态方法
├── exception/           领域异常体系
│   ├── BusinessException 基类 (code + ErrorCode)
│   ├── ErrorCode        接口: getCode() + getMessage()
│   ├── ArticleException 内嵌 ArticleError 枚举
│   ├── AuthException    内嵌 AuthError 枚举
│   ├── KbException      内嵌 KbError 枚举
│   ├── ParamException   内嵌 ParamError 枚举，支持 with(detail)
│   ├── TeamException    内嵌 TeamError 枚举
│   └── UserException    内嵌 UserError 枚举
├── filter/              Servlet 过滤器（按注册顺序）
│   ├── ExceptionFilter  全局异常捕获 → JSON envelope
│   ├── CorsFilter       CORS 响应头 + OPTIONS 预检
│   ├── CsrfFilter       写操作 Origin/Referer 校验
│   └── AuthFilter       JWT Cookie 解析 + 用户缓存 (60s TTL)
├── po/                  持久化对象（10 个）
│   ├── User, Article, Team, TeamMember
│   ├── KnowledgeBase, KnowledgeBaseMember
│   ├── Comment, Notification, UploadFile
│   └── ArticleChatMessage
├── service/             业务服务层（7 个）
│   ├── UserService      注册/登录/密码/关注/资料
│   ├── ArticleService   文章 CRUD/点赞/评论/收藏/浏览/权限
│   ├── TeamService      团队 CRUD/邀请/角色/成员
│   ├── KnowledgeBaseService 知识库 CRUD/授权/成员
│   ├── ChatService      聊天消息持久化
│   ├── NotificationService 通知创建/推送/已读
│   └── UploadService    上传文件记录
├── servlet/             HTTP 接口入口
│   ├── BaseServlet      路由分发基类 + 参数获取工具
│   ├── AuthServlet      /api/auth/*     认证（登录/注册/登出/验证码/重置密码/ws-token）
│   ├── UserServlet      /api/user/*     用户资料/关注/收藏/浏览记录
│   ├── ArticleServlet   /api/article/*  文章 CRUD/点赞/评论/收藏/浏览
│   ├── TeamServlet      /api/team/*     团队 CRUD/邀请/角色
│   ├── KnowledgeBaseServlet /api/kb/*   知识库 CRUD/授权/成员
│   ├── ChatServlet      /api/chat/*     聊天历史/在线成员
│   ├── SearchServlet    /api/search/*   全局搜索（文章/知识库/用户）
│   ├── NotificationServlet /api/notification/* 通知 CRUD
│   ├── UploadServlet    /api/upload/*   文件上传（图片/头像）
│   └── route/           自研注解路由框架
│       ├── Get/Post/Put/Delete/Patch/Options/Head  HTTP 方法注解
│       ├── Public       标记公开接口（免登录）
│       ├── Route        路径 + 方法组合注解
│       ├── RouteHandler 函数式路由处理器
│       ├── RouteKey     HTTP method + path 组合键
│       ├── RouteMethod  HTTP 方法枚举
│       ├── RouteRegistry 路由注册表（扫描 + 分发）
│       └── Routes       路由集合工具
├── util/                工具类
│   ├── BaseMapper       泛型 ORM 基类（注解驱动 CRUD）
│   ├── ConnectionPool   自研连接池（BlockingQueue + 动态代理）
│   ├── JdbcUtils        JDBC 工具（连接获取/关闭/事务绑定）
│   ├── JsonUtils        Gson 封装
│   ├── JwtUtil          JWT 签发/解析
│   ├── PasswordUtils    BCrypt hash/verify
│   ├── ResponseUtils    JSON 响应构建器
│   ├── Result           响应 envelope
│   ├── RowMapper        函数式 ResultSet → T
│   ├── SqlBuilder       链式 SQL 构建器（查询/更新/事务）
│   ├── EmailUtils       邮件发送
│   └── TurnstileUtils   Cloudflare Turnstile 验证
├── vo/                  视图对象（6 个）
│   ├── UserVO, ArticleVO, TeamVO, TeamMemberVO
│   ├── KnowledgeBaseVO, ChatMessageVO
└── websocket/           WebSocket 端点
    ├── BaseWebSocket    基类（Origin 校验 + token 解析 + 异常处理）
    ├── ChatWebSocket    文章聊天
    ├── CollaborationWebSocket Yjs 协同编辑
    ├── NotificationWebSocket 通知推送
    ├── RoomManager      房间管理器单例
    └── WebSocketConfigurator 握手配置（Origin 提取）
```

## 4. 前端架构

前端基于 React 19、Vite、TypeScript、Zustand、React Router：

### 4.1 目录结构

```text
frontend/src/
├── api/
│   ├── index.ts         统一 HTTP 请求层 + 全部 API 函数
│   └── types.ts         VO 类型定义 + 常量枚举
├── stores/
│   ├── auth.ts          Zustand: 用户登录态、初始化、登录/注册/退出
│   ├── chat.ts          Zustand: 聊天面板开关 + localStorage 持久化
│   └── notification.ts  Zustand: 通知列表、未读数、WebSocket 推送
├── router/
│   ├── index.tsx        路由定义（BrowserRouter）
│   └── AuthGuard.tsx    认证守卫（未登录 → /login）
├── views/               页面组件
│   ├── LoginPage        登录/注册
│   ├── DashboardPage    首页（公开文章列表）
│   ├── WorkspacePage    工作台（我的知识库/团队）
│   ├── ArticleDetailPage 文章阅读（评论/点赞/收藏/聊天）
│   ├── ArticleEditPage  文章编辑（Yjs 协同）
│   ├── KbDetailPage     知识库详情
│   ├── TeamDetailPage   团队详情
│   └── ProfilePage      个人资料
├── components/          通用组件
│   ├── Navbar           顶部导航栏（搜索/通知/用户菜单）
│   ├── Sidebar          左侧边栏（导航菜单）
│   ├── Modal            通用模态框
│   ├── ChatSidebar      聊天侧边栏（WebSocket）
│   ├── NotificationPanel 通知面板（WebSocket）
│   └── Editor/Editor    Yjs 编辑器组件
├── hooks/
│   ├── useCollabProvider.ts  Yjs Doc + Awareness + WS Provider 生命周期
│   ├── useChat.ts       文章聊天 WebSocket（自动重连/去重/持久化）
│   └── useTheme.tsx     主题切换
├── App.tsx              布局骨架（Navbar + Sidebar + Outlet）
└── main.tsx             入口（RouterProvider）
```

### 4.2 路由表

| 路径 | 页面 | 守卫 |
|---|---|---|
| `/login` | LoginPage | 公开 |
| `/` | DashboardPage | 需登录 |
| `/workspace` | WorkspacePage | 需登录 |
| `/article/:id` | ArticleDetailPage | 需登录 |
| `/article/:id/edit` | ArticleEditPage | 需登录 |
| `/kb/:id` | KbDetailPage | 需登录 |
| `/team/:id` | TeamDetailPage | 需登录 |
| `/profile` | ProfilePage | 需登录 |

### 4.3 前端取舍

| 选择 | 收益 | 成本 |
|---|---|---|
| React + Vite | 启动快、生态完整、组件化清晰 | 需要约束页面状态膨胀 |
| Zustand | 登录态实现轻、样板少 | 大型状态流需后续规范 action 和 selector |
| API 函数集中管理 | 类型集中、调用简单 | 文件可能继续变大，后续可按 domain 拆分 |
| Yjs 客户端协同 | 冲突合并成熟 | 后端当前不理解 CRDT，持久化策略需加强 |

## 5. 后端架构

后端基于 Java 21、Jetty 12、Jakarta Servlet、JDBC、MariaDB。

### 5.1 过滤器链详解

| 过滤器 | 顺序 | 职责 | 特殊处理 |
|---|---|---|---|
| `ExceptionFilter` | 1 | 捕获 `BusinessException` → JSON envelope；未预期异常 → 500 | WS 升级放行 |
| `CorsFilter` | 2 | 设置 `Access-Control-*` 响应头；OPTIONS 直接返回 | WS 升级放行；非白名单 Origin → 403 |
| `CsrfFilter` | 3 | 写操作（POST/PUT/PATCH/DELETE）+ 携带 Cookie → 校验 Origin/Referer | WS 升级放行；无 Cookie 放行 |
| `AuthFilter` | 4 | JWT Cookie → userId/currentUser/role/status；白名单放行 | WS 升级放行；用户缓存 60s TTL |

### 5.2 后端取舍

| 选择 | 收益 | 成本 |
|---|---|---|
| 原生 Servlet | 学习价值高、依赖少、部署可控 | 需自建路由、参数绑定、异常处理 |
| 静态 Service/DAO | 简单直接，无 DI 心智负担 | 测试 mock 困难，事务边界不够自然 |
| JDBC + BaseMapper ORM | 基础 CRUD 自动化，复杂查询仍手写 SQL | 需手写注解，无级联/懒加载 |
| 领域异常体系 | 错误码枚举化、类型安全、可按 catch 区分模块 | 枚举类较多，需维护 |
| 自研连接池 | 理解连接池机制 | 生产建议迁移 HikariCP |
| WebSocket 房间模型 | 实现简单，适合单节点 | 多节点需 Redis Pub/Sub 或消息总线 |

## 6. 核心业务流

### 6.1 登录认证

```text
1. 前端提交 username + password + turnstileToken
2. 后端校验 Turnstile → 校验 BCrypt 密码
3. 签发 JWT，写入 HttpOnly Cookie（7 天有效期）
4. 后续 HTTP 请求：AuthFilter 解析 Cookie → 恢复 userId/role/status
5. WebSocket 连接：先调 /auth/ws-token 获取短期 token → query param 建连
```

### 6.2 文章编辑

```text
1. 用户进入文章编辑页
2. 前端加载文章详情，检查 canEdit
3. 编辑器初始化 Y.Doc + Awareness
4. WebSocket 连接 /api/collaboration/{docId}
5. 本地编辑产生 Yjs update，经 WebSocket 广播
6. 保存时走 HTTP /article/save 持久化 Markdown 内容
```

### 6.3 权限模型

四层角色体系：

| 层级 | 角色 | 权限范围 |
|---|---|---|
| 平台 | USER / ADMIN | 全局管理 |
| 团队 (TEAM) | OWNER / ADMIN / MEMBER | 团队管理、成员邀请 |
| 知识库 (KB) | OWNER / ADMIN / EDITOR / VIEWER | 知识库内容管理 |
| 文章 | 继承知识库权限 | 文章可编辑条件：作者本人、KB EDITOR+、TEAM 已接受成员 |

文章可编辑判断由 `ArticleService.checkArticleEditable()` 统一实现，VO 构造和写接口共享同一规则。

### 6.4 DAO 层 ORM

- PO 类通过 `@Table("表名")` + `@Id` 注解声明元数据，字段默认 camelCase → snake_case 映射，`@Column("列名")` 可覆盖。
- DAO 继承 `BaseMapper<T>`，自动获得 `insert`/`update`/`deleteById`/`findById`/`findList`/`findOne`/`count`/`exists` 等方法。
- 每个 DAO 暴露 `public static final XxxDao INSTANCE`，Service 层通过 `XxxDao.INSTANCE.method()` 调用。
- 复杂查询（JOIN、聚合）仍使用 `SqlBuilder` + `mapper()` 手写。
- 4 个无 PO 的 DAO（Favorite、RecentVisit、FollowUser、ArticleLike）保持纯静态方法。

### 6.5 异常体系

```text
BusinessException (基类: code + ErrorCode)
  ├── UserException      (UserError 枚举)
  ├── ArticleException   (ArticleError 枚举)
  ├── TeamException      (TeamError 枚举)
  ├── KbException        (KbError 枚举)
  ├── AuthException      (AuthError 枚举)
  └── ParamException     (ParamError 枚举)
```

- 每个领域异常内嵌枚举类，实现 `ErrorCode` 接口（`getCode()` + `getMessage()`）。
- 错误码集中定义，类型安全，可按 `catch (UserException e)` 区分模块。
- `ParamException` 支持 `with(detail)` 动态拼接消息。
- `ExceptionFilter` 统一捕获并转为 JSON envelope。

## 7. 部署

### 7.1 开发环境

```text
前端: npm run dev (Vite dev server, 默认 :5173)
后端: mvn jetty:run (Jetty, 默认 :8080)
数据库: MariaDB, 配置 backend/src/main/resources/db.properties
```

### 7.2 配置文件

| 文件 | 位置 | 内容 |
|---|---|---|
| `db.properties` | classpath | JDBC URL/用户名/密码、连接池参数 |
| `web.properties` | classpath | CORS 白名单、JWT 密钥/过期时间、Turnstile 密钥、邮件 SMTP |
| `storage.properties` | classpath | 文件存储路径、扩展名白名单、大小限制 |

### 7.3 生产部署建议

- WAR 包部署到 Jetty/Tomcat
- 前端 `npm run build` 后静态资源由后端或 Nginx 托管
- 数据库连接池建议迁移 HikariCP
- Cookie 启用 `Secure` + `SameSite=Strict`
- 文件存储迁移对象存储

## 8. 边界与风险

- 当前协同后端只转发 Yjs update，不做服务端 CRDT merge；断线恢复依赖客户端与缓存状态，长期需持久化更新日志或服务端 Y.Doc。
- 多处 DAO 无数据库外键，删除依赖服务层级联；生产需补充外键或事务级删除策略。
- 文件上传当前偏本地存储模型，容量和 CDN 分发需演进。
- 自研连接池无连接泄漏检测，生产建议替换。
- WebSocket 无心跳机制（仅依赖 Jetty idleTimeout），网络不稳定可能产生僵尸连接。
