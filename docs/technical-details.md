# CloudDoc 技术细节

## 1. HTTP API 规范

### 1.1 统一响应格式

所有接口返回 JSON envelope：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误响应：

```json
{
  "code": 401,
  "message": "未登录",
  "data": null
}
```

### 1.2 前端 API 层

`frontend/src/api/index.ts` 提供：

- `request<T>(path, init, fallbackMessage)` — 统一 HTTP 请求函数
  - 自动 `credentials: include` 携带 Cookie
  - JSON body 自动设置 `Content-Type: application/json`
  - `parseJson` 安全解析（空 body → null，JSON 异常 → null）
  - `unwrapEnvelope` 解包业务 envelope，`code !== 200` 抛出 Error
  - HTTP 非 2xx 时拼接错误消息
- `query(params)` — `URLSearchParams` 构造查询参数，过滤 null/undefined/空值
- `upload(path, file)` — FormData 上传，不走 `request`（不设 Content-Type）
- 按模块组织 API 函数：`authApi`、`userApi`、`articleApi`、`teamApi`、`kbApi`、`chatApi`、`searchApi`、`uploadApi`、`notificationApi`

## 2. 认证与会话

### 2.1 HTTP 会话

```text
登录/注册成功
  → JwtUtil.generateToken(userId)
  → Cookie: clouddoc_token=<JWT>; HttpOnly; Path=/; Max-Age=604800 (7天)

每次 HTTP 请求
  → AuthFilter 从 Cookie 读取 clouddoc_token
  → JwtUtil.getUserId(token) 解析 userId
  → getCachedUser(userId) 查缓存 (60s TTL, ConcurrentHashMap)
  → request attribute: userId, role, currentUser, userStatus, banned
```

**白名单接口**（`@Public` 注解标记）：登录、注册、登出、获取当前用户、发送验证码、重置密码、文章详情、公开文章列表、评论列表、搜索。

### 2.2 WebSocket 会话

浏览器 WebSocket 无法自定义 Header，采用短期 token：

```text
1. 前端调用 GET /api/auth/ws-token
2. 后端签发 JWT（同密钥，短期有效）
3. 前端连接时放入 query param: ws://host/api/chat/ws/{articleId}?token=<jwt>
4. WebSocketConfigurator 提取 Origin 到 userProperties
5. BaseWebSocket.resolveUserId() 从 query param 解析 token → userId
6. BaseWebSocket.checkOrigin() 校验 Origin 白名单
```

### 2.3 JWT 配置

| 参数 | 默认值 | 来源 |
|---|---|---|
| 密钥 | `default-secret-key-at-least-32-bytes-long` | `web.properties: jwt.secret` |
| 过期时间 | 604800 秒（7 天） | `web.properties: jwt.expiration` |
| Cookie 名 | `clouddoc_token` | 硬编码 `AuthFilter.TOKEN_COOKIE_NAME` |

## 3. CSRF 与 CORS

### 3.1 CSRF

`CsrfFilter` 校验逻辑：

1. 跳过 WebSocket 升级请求
2. 跳过安全方法（GET/HEAD/OPTIONS）
3. 跳过无 Cookie Token 的请求（未登录用户）
4. 检查 Origin Header → 精确匹配 `WebConfig.isAllowedOrigin()`
5. 无 Origin 时检查 Referer → 匹配允许源前缀
6. 不匹配 → 403 "非法跨站请求"

### 3.2 CORS

`CorsFilter` 处理：

1. 跳过 WebSocket 升级请求
2. Origin 在白名单 → 设置 `Access-Control-Allow-Origin`（精确值，非通配）
3. 设置 `Vary: Origin`、Methods、Headers、Credentials、Max-Age=3600
4. OPTIONS 请求：白名单内 → 200；白名单外 → 403

### 3.3 允许源配置

`WebConfig.isAllowedOrigin()`:

- 精确匹配 `cors.allowed.origins`（逗号分隔）
- 自动放行 `http(s)://localhost(:port)` 和 `http(s)://127.0.0.1(:port)`

### 3.4 安全建议

- Cookie 增加 `SameSite=Lax/Strict`、`Secure`
- 生产配置固定允许源，不依赖 localhost 放行
- 对高风险操作增加二次确认或验证码

## 4. 路由与参数绑定

### 4.1 注解路由框架

```java
@WebServlet("/api/article/*")
public class ArticleServlet extends BaseServlet {

    @Get("/detail")
    private void handleDetail(HttpServletRequest req, HttpResponse res) { ... }

    @Post("/create")
    private void handleCreate(HttpServletRequest req, HttpResponse res) { ... }

    @Public
    @Get("/public-list")
    private void handlePublicList(HttpServletRequest req, HttpResponse res) { ... }
}
```

- `BaseServlet.init()` 时 `RouteRegistry.scan()` 扫描当前类所有 `@Get/@Post/@Put/@Delete` 方法
- `dispatch()` 根据 HTTP method + pathInfo 查找匹配的 `RouteHandler`
- `@Public` 标记的路径被 `RouteRegistry.scanPublicPaths()` 收集到白名单

### 4.2 参数获取

`BaseServlet` 提供统一的参数获取方法，同时支持 query/form 参数和 JSON body：

| 方法 | 行为 |
|---|---|
| `requiredParam(name)` | 必填字符串，缺失 → `MISSING_PARAM` |
| `optionalParam(name)` | 可选字符串 |
| `requiredIntParam(name)` | 必填整数，非数字 → `PARAM_NOT_INT` |
| `optionalIntParam(name)` | 可选整数 |
| `optionalIntParamOrDefault(name, default)` | 可选整数带默认值 |
| `resolveUserId(req)` | 解析 userId 数字或 username 字符串 |
| `getRequiredUserId(req)` | 必须登录，否则 → `NOT_LOGGED_IN` |
| `getOptionalUserId(req)` | 可选登录 |

**参数查找优先级**：query/form params → JSON body。JSON body 解析后缓存在 request attribute，避免重复读取 InputStream。

**Gson 数字处理**：JSON number 被 Gson 解析为 Double，`optionalParam` 对整数值自动转成整数字符串（避免 `1.0` → `parseInt` 失败）。

## 5. 业务异常

### 5.1 异常体系结构

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
- `BusinessException(code, message)` 兼容旧用法，但推荐使用枚举。

### 5.2 错误码示例

| 模块 | 枚举值 | HTTP Code | 消息 |
|---|---|---|---|
| User | `USERNAME_EXISTS` | 400 | 用户名已存在 |
| User | `LOGIN_FAILED` | 401 | 用户名或密码错误 |
| User | `USER_BANNED` | 403 | 账号已被封禁 |
| Article | `ARTICLE_NOT_FOUND` | 404 | 文章不存在 |
| Article | `NO_EDIT_PERMISSION` | 403 | 无权编辑此文章 |
| Team | `CANNOT_REMOVE_OWNER` | 403 | 不能移除 OWNER |
| Kb | `NOT_KB_MEMBER` | 403 | 您不是该知识库的成员 |
| Auth | `NOT_LOGGED_IN` | 401 | 未登录 |
| Auth | `TURNSTILE_FAILED` | 400 | 人机验证失败 |
| Auth | `EMAIL_CODE_INVALID` | 400 | 验证码无效 |
| Param | `MISSING_PARAM` | 400 | 缺少必填参数 |
| Param | `PARAM_NOT_INT` | 400 | 参数不是整数 |
| Param | `FILE_REQUIRED` | 400 | 请选择文件 |
| Param | `FILE_TOO_LARGE` | 400 | 文件过大 |

ExceptionFilter 统一捕获 `BusinessException`，转换为 JSON envelope。未预期异常返回 500。

## 6. DAO 层 ORM (BaseMapper)

### 6.1 注解

| 注解 | 作用 | 示例 |
|---|---|---|
| `@Table("表名")` | 声明实体对应的数据库表 | `@Table("sys_user")` |
| `@Id` | 标注主键字段（自增回填） | `@Id private Integer id;` |
| `@Column("列名")` | 覆盖默认列名映射 | `@Column("avatar_url")` |
| `@Transient` | 标注不参与 ORM 的字段 | 计算字段、缓存字段 |

字段默认 camelCase → snake_case 自动映射（如 `avatarUrl` → `avatar_url`）。

### 6.2 BaseMapper 方法

| 方法 | 功能 |
|---|---|
| `insert(entity)` | INSERT 并回填自增主键 |
| `update(entity)` | UPDATE 全部非主键字段 |
| `update(entity, "nickname", "avatarUrl")` | UPDATE 指定字段 |
| `deleteById(id)` | 按主键删除 |
| `findById(id)` | 按主键查单条 → `Optional<T>` |
| `findAll()` | 查全表 |
| `findList(where, params...)` | 条件查询列表 |
| `findOne(where, params...)` | 条件查单条 |
| `count(where, params...)` | 条件统计 |
| `exists(where, params...)` | 条件判断存在 |
| `mapper()` | 获取 RowMapper（用于自定义 SQL） |
| `columns()` | 获取逗号分隔列名（用于 JOIN） |
| `table()` | 获取表名 |

### 6.3 DAO 结构

```java
// 继承 BaseMapper，暴露 INSTANCE
public class ArticleDao extends BaseMapper<Article> {
    public static final ArticleDao INSTANCE = new ArticleDao();

    // 自定义查询（复用 BaseMapper 方法）
    public List<Article> findByAuthorId(Integer authorId) {
        return findList("author_id = ?", authorId);
    }

    // 复杂查询（手写 SQL + mapper()）
    public List<Article> findPublicArticles(int offset, int limit) {
        return SqlBuilder.select("SELECT " + columns() + " FROM article ...")
                .param(limit).param(offset)
                .queryList(mapper());
    }
}

// Service 调用
ArticleDao.INSTANCE.findById(articleId);
ArticleDao.INSTANCE.findByAuthorId(userId);
```

### 6.4 适用范围

- 10 个实体 DAO 继承 BaseMapper：User、Article、KnowledgeBase、Team、Comment、Notification、UploadFile、TeamMember、KnowledgeBaseMember、ArticleChatMessage。
- 4 个无 PO 的 DAO 保持纯静态：FavoriteDao、RecentVisitDao、FollowUserDao、ArticleLikeDao。

## 7. SqlBuilder

链式 SQL 构建器，封装 JDBC 样板代码。

### 7.1 查询

```java
// 列表查询
List<User> users = SqlBuilder.select("SELECT * FROM user WHERE status = ?")
    .param(1)
    .queryList(rs -> new User(rs.getInt("id"), rs.getString("username")));

// 单条查询
Optional<User> user = SqlBuilder.select("SELECT * FROM user WHERE id = ?")
    .param(userId)
    .queryOne(rs -> mapUser(rs));

// 标量查询
Long count = SqlBuilder.select("SELECT COUNT(*) FROM article WHERE status = 1")
    .queryScalar();

// COUNT 查询
int count = SqlBuilder.select("SELECT COUNT(*) FROM article WHERE author_id = ?")
    .param(userId)
    .queryCount();

// 存在性判断
boolean exists = SqlBuilder.select("SELECT 1 FROM article WHERE id = ? LIMIT 1")
    .param(articleId)
    .queryExists();
```

### 7.2 更新

```java
// INSERT/UPDATE/DELETE
int rows = SqlBuilder.update("INSERT INTO user(username, password) VALUES(?, ?)")
    .param("admin")
    .param("123456")
    .execute();

// INSERT 并获取自增主键
int id = SqlBuilder.update("INSERT INTO article(title) VALUES(?)")
    .param("测试")
    .executeReturnKey();

// 批量执行
int totalRows = SqlBuilder.update("INSERT INTO tag(article_id, tag) VALUES(?, ?)")
    .params(articleId1, "tag1")
    .params(articleId2, "tag2")
    .executeBatch(2);
```

### 7.3 事务

```java
SqlBuilder.inTransaction(conn -> {
    SqlBuilder.update("UPDATE article SET content = ? WHERE id = ?")
        .param(content).param(articleId).execute();
    SqlBuilder.update("INSERT INTO article_version(...) VALUES(...)")
        .params(...).execute();
    return null;
});
```

事务通过 `JdbcUtils.bindTransactionConnection()` 将连接绑定到 ThreadLocal，事务内所有 `SqlBuilder` 操作复用同一连接。

## 8. 连接池

自研 `ConnectionPool`（单例），基于 `BlockingQueue` + 动态代理。

### 8.1 配置

| 参数 | 默认值 | 来源 |
|---|---|---|
| `pool.minIdle` | 2 | `db.properties` |
| `pool.maxTotal` | 20 | `db.properties` |
| `pool.maxWaitMillis` | 5000 | `db.properties` |
| `maxLifetimeMillis` | 30 分钟 | 硬编码 |
| `validationTimeoutSeconds` | 2 秒 | 硬编码 |

### 8.2 获取连接流程

```text
getConnection()
  1. idleConnections.poll()           → 命中空闲连接
  2. activeCount < maxTotal            → CAS + 新建连接
  3. idleConnections.poll(timeout)     → 阻塞等待（超时抛异常）
```

### 8.3 代理机制

```text
wrapConnection(rawConn)
  → JDK 动态代理拦截 Connection 方法
  → close() → returnConnection(raw)  归还到池
  → isClosed() → false               代理对象始终视为打开
  → 其他方法 → 委托原始连接
```

### 8.4 连接校验

取出连接时 `isValid(validationTimeoutSeconds)` 校验可用性，失效自动重建。

## 9. 协同编辑

### 9.1 客户端

- 每篇文章对应一个 Y.Doc。
- Awareness 保存在线用户、昵称、颜色、光标状态。
- `WebsocketProvider` 管理连接、重连、状态回调。
- 编辑器内容变更通过 Yjs update 同步。
- `useCollabProvider(documentId, username)` hook 管理完整生命周期：
  - documentId 变化时销毁旧资源、创建新资源
  - 自动获取 ws-token、建立 WebSocket 连接
  - 断线 3s 自动重连
  - Awareness change 事件 → 在线用户列表
  - 组件卸载时完整清理（destroy provider/awareness/doc）

### 9.2 服务端

`CollaborationWebSocket` 纯中继模式，不维护 Yjs 文档状态：

```text
新用户加入:
  Server → 广播 SyncStep1 [0,0] 给房间内其他人
  其他用户 → 回复 SyncStep2 → Server 转发给新用户
  新用户 Yjs CRDT 自动合并
  如果房间只有自己 → Server 发送空 SyncStep2 [0,1,0]

编辑:
  用户 → Update [0,2,...] → Server 转发给其他人

Awareness:
  用户 → Awareness [1,...] → Server 提取 clientID 存储，转发
  断连 → Server 广播 awareness remove + QueryAwareness [3]
```

消息类型（y-websocket 协议）：

| 值 | 类型 | 说明 |
|---|---|---|
| 0 | Sync | SyncStep1=0, SyncStep2=1, Update=2 |
| 1 | Awareness | 在线状态/光标 |
| 2 | Auth | 不处理 |
| 3 | QueryAwareness | 请求重新发送 awareness |

### 9.3 当前边界

- Java 服务端未引入 Yjs CRDT 引ngine，不做 update merge 与冲突解析。
- 持久化主要依赖显式保存文章内容。
- 若用户长时间协作但未保存，服务重启可能丢失未落盘状态。

建议后续：

- 方案 A：引入 y-java 或独立 Node y-websocket 服务负责 CRDT merge。
- 方案 B：存储 update log，定期 compact 成 snapshot。
- 方案 C：保存时做版本号/CAS，降低覆盖风险。

### 9.4 通知系统

- `NotificationService.send(...)` 统一完成通知写库与实时推送，失败不影响主业务。
- 通知类型（`NotificationType`）：

| 值 | 类型 | 触发场景 |
|---|---|---|
| 0 | TEAM_INVITE | 团队邀请 |
| 1 | KB_INVITE | 知识库邀请 |
| 2 | TEAM_NEW_ARTICLE | 团队新文章 |
| 3 | COMMENT | 评论 |
| 4 | LIKE | 点赞 |
| 5 | MEMBER_CHANGE | 成员变动 |
| 6 | FOLLOW | 关注 |
| 7 | FOLLOW_ARTICLE | 关注者文章发布/更新 |

- REST 接口提供列表、未读数、标记已读、全部已读、删除。
- `NotificationWebSocket` 向在线用户推送 `notification` 与 `unread_count`。
- 前端 `notificationApi` + `useNotificationStore` 负责拉取、标记、删除和自动重连（5s）。

## 10. 上传

### 10.1 接口

| 端点 | 方法 | 用途 |
|---|---|---|
| `/api/upload/image` | POST | 文章图片上传 |
| `/api/upload/avatar` | POST | 头像上传（同时更新用户头像） |

请求：`multipart/form-data`，字段名 `file`。

响应：

```json
{
  "code": 200,
  "data": {
    "url": "/uploads/image/abc123.png",
    "fileName": "screenshot.png"
  }
}
```

### 10.2 处理流程

```text
1. 获取 Part filePart
2. 校验扩展名 → StorageConfig.getAllowedExtensions()
3. 校验大小 → StorageConfig.getMaxSizeBytes()
4. 生成 UUID 文件名
5. 保存到 StorageConfig.getStoragePath()/{bizType}/
6. 记录到 upload_file 表
7. 返回 URL 路径 /uploads/{bizType}/{uuid}.{ext}
```

### 10.3 生产建议

- 文件类型 magic number 检查（不仅靠扩展名）
- 图片压缩和缩略图
- 对象存储 + CDN
- 私有资源签名 URL

## 11. 代码清晰化原则

- 一个权限规则只保留一个服务层函数，VO 和写接口都调用它。
- 前端请求逻辑只在 API 层写一次，页面不直接拼 URL。
- 列表轻量 VO 使用专门构造函数，避免字段遗漏和重复 Map 代码。
- 错误消息在边界层翻译，内部保留明确异常。
