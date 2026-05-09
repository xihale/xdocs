# CloudDoc 后端代码优雅化、清晰化、Robust 化重构计划

## Objective

对 CloudDoc 后端进行全面重构，按优先级解决 20 项已识别问题，使代码更健壮、更清晰、更优雅。按 **Robustness > Security > Clarity > Elegance** 排序，分为四个阶段推进。

---

## 阶段一：安全与健壮性修复（Critical）

### 1.1 修复文章 CRUD 缺失的权限校验

- [ ] 在 `ArticleService` 中新增 `checkArticleOwner(int articleId, int userId)` 方法，校验当前用户是否为文章作者（或管理员），否则抛出 `BusinessException(403, "无权操作")`
- [ ] `ArticleServlet.handleUpdate` 调用 `checkArticleOwner` 后再执行更新（`ArticleServlet.java:38-53`）
- [ ] `ArticleServlet.handleDelete` 调用 `checkArticleOwner` 后再执行删除（`ArticleServlet.java:56-60`）
- [ ] `ArticleServlet.handleSave` 调用 `checkArticleOwner` 后再保存内容（`ArticleServlet.java:105-113`）

**理由**：当前任何登录用户可修改/删除任意文章，属于严重安全漏洞。

### 1.2 修复知识库 CRUD 缺失的权限校验

- [ ] 在 `KnowledgeBaseService` 中新增 `checkKbPermission(int kbId, int userId, KnowledgeBaseRole... requiredRoles)` 方法
- [ ] `KnowledgeBaseServlet.handleUpdate` 校验 OWNER/ADMIN 权限（`KnowledgeBaseServlet.java:42-51`）
- [ ] `KnowledgeBaseServlet.handleDelete` 校验 OWNER 权限（`KnowledgeBaseServlet.java:53-59`）

### 1.3 为多步操作添加事务支持

- [ ] `TeamService.createTeam` 使用 `SqlBuilder.inTransaction()` 包裹 Team 插入 + TeamMember 插入（`TeamService.java:18-29`）
- [ ] `KnowledgeBaseService.createKnowledgeBase` 使用 `SqlBuilder.inTransaction()` 包裹 KB 插入 + KBMember 插入（`KnowledgeBaseService.java:18-31`）
- [ ] `ArticleServlet.handleLike/handleUnlike` 中先查后写的操作考虑合并为事务（`ArticleServlet.java:121-142`）

### 1.4 修复 CSRF 防护失效问题

- [ ] 重写 `CsrfFilter.hasAuthenticatedToken`：移除 Bearer Token 检测逻辑，改为始终对 cookie-authenticated 的写操作执行 Origin/Referer 校验（`CsrfFilter.java:51-57`）
- [ ] 或：如果 CORS 白名单已足够限制来源，可考虑移除 CsrfFilter 并在 CorsFilter 中严格校验 Origin（需评估）

### 1.5 修复 NumberFormatException 导致 500 错误

- [ ] 在 `BaseServlet` 中增强 `requiredIntParam` / `optionalIntParam`：内部 try-catch `NumberFormatException`，抛出 `BusinessException(400, "参数 {name} 格式错误")`
- [ ] 删除 `ChatServlet.optionalIntParamOrDefault`（`ChatServlet.java:78-86`），迁移到 `BaseServlet` 作为通用方法
- [ ] 全局搜索 `Integer.parseInt` 的其他调用点，统一替换为 `BaseServlet` 的安全方法

### 1.6 修复 KnowledgeBaseDao.mapRow 的 NPE 风险

- [ ] `KnowledgeBaseDao.mapRow` 对 `create_time`、`update_time` 做 null 检查后再 `toLocalDateTime()`（`KnowledgeBaseDao.java:181-182`），与 `ArticleDao.java:247-254` 保持一致

### 1.7 修复 BaseServlet JSON 解析静默吞异常

- [ ] `BaseServlet.getJsonBody` 的 catch 块记录异常到日志并设置 request attribute `jsonParseError`（`BaseServlet.java:56`）
- [ ] `requiredParam` 在发现 `jsonParseError` 时返回 "请求体 JSON 格式错误" 而非 "缺少必填参数"

---

## 阶段二：架构一致性修复（High）

### 2.1 DAO 层迁移到 SqlBuilder

- [ ] 逐个 DAO 文件将手写 JDBC 样板替换为 `SqlBuilder` 调用，消除 ~80 个重复的 try-catch-finally 块
- [ ] 优先迁移：`ArticleDao`（最复杂）、`UserDao`（最高频）、`TeamMemberDao`、`KnowledgeBaseMemberDao`
- [ ] 每迁移一个 DAO，确保所有现有调用方行为不变

**理由**：`SqlBuilder` 已实现但零使用，是最大的技术债务。

### 2.2 消除 Servlet 直接调用 DAO 的层违规

- [ ] 将 `ArticleServlet` 中的直接 DAO 调用迁移到 `ArticleService`：点赞/取消点赞、评论增删、收藏/取消收藏、浏览记录（涉及 `ArticleLikeDao`、`CommentDao`、`FavoriteDao`、`RecentVisitDao`）
- [ ] 将 `UserServlet` 中的直接 DAO 调用迁移到 `UserService`：关注/取关、粉丝列表、关注列表（涉及 `FollowUserDao`）
- [ ] 将 `TeamServlet.handleList` 中的 `TeamMemberDao.findByTeamId` + `UserDao.findById` 迁移到 `TeamService`
- [ ] 将 `KnowledgeBaseServlet.handleMembers` 中的 `KnowledgeBaseMemberDao.findByKbId` + `UserDao.findById` 迁移到 `KnowledgeBaseService`
- [ ] 将 `SearchServlet` 中的直接 DAO 调用迁移到对应 Service 或新建 `SearchService`
- [ ] 将 `AuthServlet.handleSendEmailCode` 中的 `UserDao.findByEmail` 迁移到 `UserService`
- [ ] 将 `UploadServlet.handleUploadAvatar` 中的 `UserDao.updateAvatar` 迁移到 `UserService`

### 2.3 统一 DAO 错误消息

- [ ] 所有 DAO 的 `catch (SQLException e)` 使用统一格式：`"操作 {表名} 失败: " + e.getMessage()`
- [ ] 补全 `KnowledgeBaseMemberDao`、`TeamMemberDao`、`FollowUserDao` 等缺失的错误消息

### 2.4 修复 ConnectionPool 初始化竞态

- [ ] `ConnectionPool` 构造函数中，先创建所有初始连接放入临时 List，全部成功后再 `idleConnections.addAll(list)` 并 `activeCount.set(list.size())`（`ConnectionPool.java:79-88`）
- [ ] 修复代理 `isClosed` 始终返回 false 的问题：添加 `poolShutdown` 标志位（`ConnectionPool.java:288`）

### 2.5 修复 EmailUtils 线程池未关闭

- [ ] `EmailUtils` 注册 JVM shutdown hook：`Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown))`
- [ ] 或改用 `Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r); t.setDaemon(true); return t; })` 使用守护线程

### 2.6 修复 TurnstileUtils 表单数据未 URL 编码

- [ ] `TurnstileUtils.java:44-50`：对 `entry.getValue()` 使用 `URLEncoder.encode(value, StandardCharsets.UTF_8)`

### 2.7 修复 JwtUtil 硬编码开发密钥

- [ ] `JwtUtil.java:39`：当 `web.properties` 缺失或 `jwt.secret` 为空时，**抛出异常阻止启动**而非静默使用硬编码密钥
- [ ] 仅在系统属性 `xdocs.dev=true` 时允许使用开发密钥

---

## 阶段三：代码清晰度提升（Medium）

### 3.1 统一 VO 使用，消除内联 Map

- [ ] 补全 `UserVO` 的字段，替换 `UserServlet.handleProfile`、`handleListFollowing`、`handleListFollowers` 中的 `LinkedHashMap`（`UserServlet.java:51-143`）
- [ ] 补全 `KnowledgeBaseVO` 的字段，替换 `KnowledgeBaseServlet` 和 `SearchServlet` 中的 `LinkedHashMap`
- [ ] 为评论响应创建 `CommentVO`，替换 `ArticleServlet.handleListComments` 中的 `LinkedHashMap`（`ArticleServlet.java:170-174`）
- [ ] 为搜索结果创建 `SearchResultVO` 或复用已有 VO
- [ ] 为团队待处理邀请创建 `TeamInviteVO`，替换 `TeamServlet.handlePendingInvites` 中的 `LinkedHashMap`
- [ ] `ArticleServlet.handleDetail` 返回 `ArticleVO` 而非原始 `Article` PO（`ArticleServlet.java:34`）

### 3.2 提取重复的 VO 构建逻辑

- [ ] `TeamVO` 构建提取为 `TeamVO.from(Team team, List<TeamMember> members)` 静态工厂方法，消除 `TeamServlet` 中的重复代码（`TeamServlet.java:47-64` vs `TeamServlet.java:95-106`）
- [ ] `UserVO` 构建提取为 `UserVO.from(User user)` 静态工厂方法，统一密码清除逻辑
- [ ] `ArticleVO` 构建提取为 `ArticleVO.from(Article article, int likeCount, boolean liked)` 静态工厂方法

### 3.3 统一 Gson 实例

- [ ] `JsonUtils` 暴露 `public static final Gson GSON`（带 LocalDateTime adapter）
- [ ] 删除 `BaseServlet`、`ChatWebSocket`、`TurnstileUtils` 中的 `private static final Gson GSON`，统一引用 `JsonUtils.GSON`

### 3.4 统一 DAO 的 SELECT 列

- [ ] 所有 DAO 的查询统一使用显式列名（如 `SELECT id, name, ...`），替换 `SELECT *`
- [ ] 优先修复：`KnowledgeBaseDao.java:85`、`TeamDao.java:80`、`UserDao.java:122`

### 3.5 统一 CommentDao.findById 返回类型

- [ ] `CommentDao.findById` 返回 `Optional<Comment>` 而非 `Comment`（`CommentDao.java:81-96`）
- [ ] 更新所有调用方适配 `Optional` 返回值

### 3.6 修复 UserServlet optionalIntParam 双重解析

- [ ] `UserServlet.java:112` 和 `UserServlet.java:130`：先缓存 `optionalIntParam` 结果再判断，避免重复解析

---

## 阶段四：架构优雅化（Elegance）

### 4.1 提取 WebSocket 公共基类

- [ ] 创建 `BaseWebSocket` 抽象类，包含：
  - `resolveUserId(Session session)` — JWT 解析逻辑
  - `closeSession(Session session)` — 安全关闭
  - `getRoom(String roomId)` / `joinRoom(String roomId, Session session)` / `leaveRoom(String roomId, Session session)` — 房间管理
- [ ] `ChatWebSocket` 和 `CollaborationWebSocket` 继承 `BaseWebSocket`，消除重复代码

### 4.2 路由框架增强：路径参数支持

- [ ] `RouteKey` 和 `RouteRegistry` 支持 `{id}` 风格的路径参数
- [ ] 匹配时将路径参数存入 request attribute（如 `pathParam.id`）
- [ ] `BaseServlet` 新增 `pathParam(req, "id")` 便捷方法

### 4.3 路由框架增强：方法级注解守卫

- [ ] 新增 `@RequireAuth` 注解，`RouteRegistry` 在分发前检查用户是否登录
- [ ] 新增 `@RequireRole(Role.ADMIN)` 注解，支持角色校验
- [ ] 逐步将现有手动权限检查迁移为注解声明

### 4.4 AuthFilter 性能优化

- [ ] 引入简单的用户信息缓存（如 `ConcurrentHashMap<Integer, SoftReference<User>>`，TTL 5 分钟），避免每次请求查库（`AuthFilter.java:94`）
- [ ] 白名单路径在未携带 token 时跳过数据库查询

### 4.5 解决 N+1 查询问题

- [ ] `ArticleServlet.handlePublicList`：新增 `ArticleLikeDao.countByArticleIds(List<Integer>)` 和 `ArticleLikeDao.findLikedByUser(List<Integer>, int userId)` 批量查询方法，替换循环内单条查询（`ArticleServlet.java:93-100`）
- [ ] `ArticleServlet.handleListComments`：新增 `UserDao.findByIds(List<Integer>)` 批量查询，替换循环内 `UserDao.findById`（`ArticleServlet.java:170-174`）
- [ ] 修复 `CommentDao.findById` 在 replyTo 处理中被调用两次的问题（`ArticleServlet.java:171`）

### 4.6 WebSocket 访问控制

- [ ] `ChatWebSocket.onOpen` 校验用户是否有权限访问该 article 的聊天室（`ChatWebSocket.java:51-68`）
- [ ] `CollaborationWebSocket.onOpen` 同理校验文档权限
- [ ] 添加消息频率限制（如每秒最多 5 条消息）

### 4.7 UserService.findAll 消除密码泄露风险

- [ ] `UserService.findAll` 返回前统一清除 password 字段，或返回 `List<UserVO>` 而非 `List<User>`

---

## Verification Criteria

- [ ] 所有现有 API 接口行为不变（回归测试通过）
- [ ] 非作者用户无法修改/删除他人文章（权限测试）
- [ ] Team/KB 创建失败时数据库无脏数据（事务测试）
- [ ] 非法数字参数返回 400 而非 500（参数校验测试）
- [ ] DAO 层不再有手写 try-catch-finally（代码审查）
- [ ] Servlet 层不再直接调用 DAO（代码审查）
- [ ] 所有 API 响应使用 VO 类而非 Map（代码审查）
- [ ] WebSocket 连接有权限校验（安全测试）

## Potential Risks and Mitigations

1. **大规模重构可能引入回归 Bug**
   Mitigation：每个阶段完成后运行全量测试；阶段内每个任务独立可回滚

2. **SqlBuilder 迁移可能改变异常行为**
   Mitigation：先迁移简单 DAO 验证行为一致，再迁移复杂 DAO

3. **事务引入可能导致连接占用时间变长**
   Mitigation：事务内操作保持简短，不包含外部调用（如邮件发送）

4. **AuthFilter 缓存可能导致权限变更延迟**
   Mitigation：缓存 TTL 设为较短值（如 5 分钟），用户封禁/角色变更时主动清除缓存

5. **路由框架增强可能影响现有路由匹配**
   Mitigation：路径参数作为新增特性，不影响已有精确匹配路由

## Alternative Approaches

1. **引入轻量级 DI 框架（如 Guice）**：可替代静态方法模式，实现真正的依赖注入和可测试性提升。但改动面极大，与项目"不依赖框架"的定位冲突，建议暂不采用。

2. **使用 JDBI 或类似库替代手写 JDBC**：可大幅减少 DAO 样板代码。但项目已有 SqlBuilder，优先用好自己的工具。

3. **引入 AOP 处理权限校验**：可通过切面统一权限逻辑。但在原生 Servlet 环境中实现 AOP 复杂度高，注解守卫方案更轻量实用。
