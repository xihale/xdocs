
# Session → JWT 鉴权迁移方案

## Objective

将当前基于 `HttpSession` 的鉴权机制迁移为 **无状态 JWT（JSON Web Token）** 方案，实现服务器重启后用户登录状态不丢失。核心原则：**最小改动、向后兼容、安全可靠**。

---

## 现状分析

### 当前认证流程

1. 用户登录/注册 → `AuthServlet` 将 `userId` 和 `role` 写入 `HttpSession`（`AuthServlet.java:70-73`）
2. 每次请求 → `AuthFilter` 从 `HttpSession` 读取 `userId`，查库获取完整用户信息，设置到 `request.setAttribute`（`AuthFilter.java:51-61`）
3. `CsrfFilter` 通过 `HttpSession` 判断是否已登录（`CsrfFilter.java:48-54`）
4. 前端通过 `withCredentials: true`（即 Cookie）自动携带 Session ID（`request.ts:20`）
5. 前端还通过 `localStorage` 存储用户信息做客户端缓存（`user.ts:30-37`）
6. WebSocket 连接（`ws.ts`）当前无鉴权，直接连接

### 关键影响面

| 组件 | 文件 | 改动程度 |
|------|------|----------|
| AuthFilter | `filter/AuthFilter.java` | **大改** — 从 session 读改为从 token 读 |
| AuthServlet | `servlet/AuthServlet.java` | **大改** — 写 session 改为返回 token |
| CsrfFilter | `filter/CsrfFilter.java` | **中改** — session 判断改为 token 判断 |
| JWT 工具类 | 新建 `util/JwtUtil.java` | **新建** |
| CorsFilter | `filter/CorsFilter.java` | **小改** — 添加 Authorization header 允许 |
| 前端 request.ts | `utils/request.ts` | **中改** — 添加 token 拦截器 |
| 前端 user store | `stores/user.ts` | **中改** — 存储/清除 token |
| 前端 ws.ts | `utils/ws.ts` | **小改** — WebSocket 连接携带 token |
| pom.xml | `pom.xml` | **小改** — 添加 JWT 依赖 |
| web.xml | `WEB-INF/web.xml` | 无需改动 |
| BaseServlet | `servlet/BaseServlet.java` | **无需改动** — 它只从 `request.getAttribute` 读取，AuthFilter 已设置好 |

---

## 技术选型

**推荐方案：自签 JWT（无刷新 Token，单 Token 方案）**

理由：
- 项目体量小（纯 Servlet，无 Spring），引入 Spring Security 或 OAuth 属于过度设计
- 单 Token 方案实现简单，Token 有效期设 7 天即可满足需求
- 项目已有 Gson，不需要额外 JSON 库
- 使用 `io.jsonwebtoken:jjwt-api` 作为 JWT 实现（最主流的 Java JWT 库）

**关于"刷新 Token"的决策**：首版不做 Refresh Token。理由：
- 项目是个人/小团队应用，7 天过期 + 自动重新登录体验足够好
- 减少后端状态管理（Refresh Token 需要存储和撤销机制）
- 如未来需要，可在此基础上扩展

---

## Implementation Plan

### Phase 1: 后端 — JWT 基础设施

- [x] **1.1 添加 JJWT 依赖到 `pom.xml`**
  在 `<dependencies>` 中添加：
  ```
  io.jsonwebtoken:jjwt-api:0.12.6   (compile)
  io.jsonwebtoken:jjwt-impl:0.12.6   (runtime)
  io.jsonwebtoken:jjwt-jackson:0.12.6 (runtime)
  ```
  jjwt 是 Java 生态最成熟的 JWT 库，0.12.x 支持 Java 21。

- [x] **1.2 新建 `JwtUtil.java` 工具类**
  位置：`top.xihale.xdocs.util.JwtUtil`
  功能：
  - `generateToken(int userId)` — 生成 JWT，payload 含 `userId` 和 `role`，有效期 7 天
  - `parseToken(String token)` → `DecodedJWT` 或自定义 record — 解析并验证签名
  - `getUserId(String token)` → `Integer` — 从 token 中提取 userId
  - 密钥从配置文件读取（`web.properties` 中的 `jwt.secret`），启动时校验密钥长度 >= 256 位
  - 使用 HMAC-SHA256 签名

### Phase 2: 后端 — 改造认证链路

- [x] **2.1 改造 `AuthServlet.java`**
  - `handleLogin` / `handleRegister`：不再调用 `writeAuthSession()`，改为调用 `JwtUtil.generateToken(user.getId())`，将 token 放入响应 JSON（如 `{ token: "xxx", user: {...} }`）
  - `handleLogout`：不再 `session.invalidate()`，改为直接返回成功（JWT 无状态，客户端清除即可）
  - `handleCurrent`：从 `AuthFilter` 设置的 `request.getAttribute("currentUser")` 读取（不变）
  - 删除 `writeAuthSession` 方法

- [x] **2.2 改造 `AuthFilter.java`**
  - `resolveAuthState` 方法：不再从 `HttpSession` 读取 userId，改为：
    1. 从 `Authorization: Bearer <token>` Header 提取 token
    2. 调用 `JwtUtil.parseToken(token)` 解析验证
    3. 从 token payload 中获取 `userId`
    4. 查库获取完整用户信息（与现有逻辑一致）
  - `getUserId` 方法：改为从 token 解析，不再从 session 读取
  - 删除所有 `HttpSession` 相关 import 和调用
  - 将解析后的 token 信息存入 `request.setAttribute`（与现有模式一致，下游 BaseServlet 无需改动）

- [x] **2.3 改造 `CsrfFilter.java`**
  - `hasAuthenticatedSession` 方法：改为检查 `Authorization` Header 是否存在有效 token
  - 不再依赖 `HttpSession`
  - JWT 方案下，CSRF 风险大幅降低（token 不通过 Cookie 自动发送），但保留此过滤器作为额外安全层

- [x] **2.4 改造 `CorsFilter.java`**
  - `Access-Control-Allow-Headers` 添加 `Authorization`
  - 可在 `web.properties` 的 `cors.allowed.headers` 配置中追加，或硬编码追加

### Phase 3: 前端 — Token 管理

- [x] **3.1 改造 `request.ts` — 添加请求拦截器**
  - 删除 `withCredentials: true`（不再需要 Cookie）
  - 添加请求拦截器：从 `localStorage` 读取 `xdocs-token`，设置 `Authorization: Bearer <token>` Header
  - 响应拦截器：遇到 401 时清除 token（现有逻辑已处理）

- [x] **3.2 改造 `stores/user.ts` — Token 持久化**
  - `login` / `register` 成功后，从响应中提取 `token` 并存入 `localStorage('xdocs-token')`
  - `clearSession` 方法：同时清除 `xdocs-token`
  - `hydrate` 方法：先检查本地是否有 token，有则调用 `/api/auth/current` 验证有效性
  - `logout` 方法：清除本地 token

- [x] **3.3 改造 `ws.ts` — WebSocket 携带 Token**
  - WebSocket 不支持自定义 Header，采用 URL query 参数方式：`ws://host/ws/{room}?token=xxx`
  - 在 `createCollabConnection` 中从 `localStorage` 读取 token 并传入 `params`

### Phase 4: 后端 — WebSocket 鉴权（可选增强）

- [x] **4.1 改造 `DocWebSocket.java` — 验证 Token**
  - `@OnOpen` 方法中从 `@PathParam` 或 query 参数提取 token
  - 调用 `JwtUtil.parseToken(token)` 验证有效性
  - 无效则 `session.close()` 拒绝连接
  - 有效则将 `userId` 存入 `session.getUserProperties()` 供后续使用

### Phase 5: 配置与安全

- [x] **5.1 在 `web.properties` 中添加 JWT 配置项**
  ```
  jwt.secret=<一个强随机密钥，至少 32 字节>
  jwt.expiration=604800   # 7天，单位秒
  ```

- [ ] **5.2 密钥安全**
  - 生产环境密钥不得硬编码，从环境变量或配置文件读取
  - 首次启动时可自动生成并打印到日志（开发环境便利性）
  - 密钥变更后所有已发放的 token 自动失效（符合预期）

---

## Verification Criteria

- [ ] 服务器重启后，前端页面不跳转到登录页，用户保持登录状态
- [ ] 登录后关闭浏览器重新打开，仍然保持登录（token 在 localStorage）
- [ ] Token 过期后，前端正确跳转到登录页并提示
- [ ] 被封禁用户的 token 在下次请求时被正确拦截
- [ ] WebSocket 协同编辑连接需要有效 token 才能建立
- [ ] 所有现有 API 功能（团队、知识库、文章、上传、聊天）正常工作
- [ ] 注册 → 自动登录 → 操作 → 退出 → 重新登录 完整流程正常

---

## Potential Risks and Mitigations

1. **Token 无法主动撤销**
   - 场景：用户修改密码或被封禁后，旧 token 仍在有效期内
   - 缓解方案：`AuthFilter` 每次请求都会查库验证用户状态（`UserService.findUserById`），封禁状态可即时生效。密码修改后旧 token 在过期前仍可用，但这是可接受的权衡

2. **WebSocket URL 暴露 Token**
   - Token 出现在 URL 中可能被日志记录或浏览器历史记录
   - 缓解方案：使用 `wss://`（生产环境 HTTPS），且 token 有效期有限。这是 WebSocket 鉴权的标准做法

3. **密钥泄露**
   - 密钥一旦泄露，攻击者可伪造任意用户 token
   - 缓解方案：密钥存储在服务器端配置文件中，不纳入版本控制（`.gitignore` 中排除含敏感信息的配置）

4. **Token 体积比 Session ID 大**
   - JWT 通常数百字节 vs Session ID 几十字节
   - 缓解方案：项目规模小，对性能无实质影响

---

## Alternative Approaches

1. **有状态 Token（Token + 数据库/Redis 存储）**
   - 优点：可主动撤销
   - 缺点：引入外部依赖（Redis）或增加数据库查询，违背"服务器重启不丢"的初衷（除非 Redis 持久化）
   - 结论：当前项目不需要，JWT + 每次查库验证用户状态已足够

2. **Refresh Token 双 Token 方案**
   - 优点：Access Token 短期 + Refresh Token 长期，安全性更高
   - 缺点：实现复杂度翻倍，需要额外的刷新接口和状态管理
   - 结论：首版不需要，可在后续版本扩展

3. **Spring Security + JWT**
   - 优点：功能完善，生态丰富
   - 结论：项目使用纯 Servlet 架构，引入 Spring Security 是过度设计
