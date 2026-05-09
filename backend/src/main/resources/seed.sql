-- =============================================
-- 示例数据 — 插入详细的示例文章
-- =============================================
-- 前提: 已存在 id=1 的 sys_user 和 id=1 的 knowledge_base (visibility=1 即 PUBLIC)
-- 使用: source seed.sql 或手动执行

-- 确保有公开知识库
INSERT IGNORE INTO knowledge_base (id, name, description, visibility, owner_type, owner_id, creator_id, create_time, update_time)
VALUES (1, '技术博客', '分享编程技术与工程实践', 1, 'USER', 1, 1, NOW(), NOW());

INSERT IGNORE INTO knowledge_base (id, name, description, visibility, owner_type, owner_id, creator_id, create_time, update_time)
VALUES (2, '学习笔记', '日常学习记录与知识整理', 1, 'USER', 1, 1, NOW(), NOW());

-- =============================================
-- 文章 1: Java Servlet 详解
-- =============================================
INSERT IGNORE INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status, cover_image, create_time, update_time)
VALUES (1, 'Java Servlet 从入门到实战',
'系统讲解 Java Servlet 的核心概念、生命周期、请求处理流程以及实战中常见的过滤器与监听器模式。',
'# Java Servlet 从入门到实战

## 1. 什么是 Servlet

Servlet 是 Java EE 规范中用于处理 HTTP 请求的服务端组件。它运行在 Servlet 容器（如 Tomcat）中，可以动态生成 HTML 页面或返回 JSON 数据。

与 CGI（Common Gateway Interface）相比，Servlet 采用**多线程模型**——容器只创建一个 Servlet 实例，通过多线程并发处理请求，性能远优于每次请求 fork 新进程的 CGI 方式。

### 1.1 核心接口

```java
public interface Servlet {
    void init(ServletConfig config) throws ServletException;
    void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException;
    void destroy();
    ServletConfig getServletConfig();
    String getServletInfo();
}
```

通常我们继承 `HttpServlet`，覆写 `doGet` 和 `doPost` 方法即可。

## 2. 生命周期

Servlet 的生命周期由容器管理，分为三个阶段：

1. **初始化**：容器启动或首次访问时调用 `init()`，执行一次性初始化逻辑
2. **服务**：每次请求到达时调用 `service()`，根据 HTTP 方法分发给 `doGet`/`doPost` 等
3. **销毁**：容器关闭时调用 `destroy()`，释放资源

### 2.1 初始化时机

可以在 `web.xml` 中用 `<load-on-startup>` 标签指定初始化顺序：

```xml
<servlet>
    <servlet-name>MyServlet</servlet-name>
    <servlet-class>com.example.MyServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
```

数字越小越先初始化，0 或正数表示容器启动时初始化，负数表示首次请求时初始化。

## 3. 请求与响应

### 3.1 HttpServletRequest

`HttpServletRequest` 封装了客户端请求的全部信息：

```java
// 获取请求参数
String username = request.getParameter("username");

// 获取请求头
String contentType = request.getHeader("Content-Type");

// 获取请求路径
String uri = request.getRequestURI();

// 获取客户端 IP
String ip = request.getRemoteAddr();
```

### 3.2 HttpServletResponse

`HttpServletResponse` 用于构造返回给客户端的内容：

```java
// 设置响应类型和编码
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");

// 写入响应体
PrintWriter out = response.getWriter();
out.print("{\"code\":200,\"msg\":\"success\"}");
out.flush();
```

### 3.3 请求转发与重定向

```java
// 请求转发（服务端跳转，URL 不变）
request.getRequestDispatcher("/target").forward(request, response);

// 重定向（客户端跳转，URL 改变）
response.sendRedirect("/target");
```

## 4. 会话管理

### 4.1 Cookie

Cookie 是客户端存储的小段数据，每次请求自动携带：

```java
// 写入 Cookie
Cookie cookie = new Cookie("token", "abc123");
cookie.setMaxAge(3600); // 1 小时有效
cookie.setHttpOnly(true); // JS 不可读取
response.addCookie(cookie);

// 读取 Cookie
Cookie[] cookies = request.getCookies();
for (Cookie c : cookies) {
    if ("token".equals(c.getName())) {
        String value = c.getValue();
    }
}
```

### 4.2 Session

Session 是服务端存储的会话数据，通过 `JSESSIONID` Cookie 关联：

```java
// 获取 Session
HttpSession session = request.getSession(true);

// 存储数据
session.setAttribute("userId", 123);
session.setAttribute("nickname", "张三");

// 读取数据
Integer userId = (Integer) session.getAttribute("userId");

// 销毁 Session
session.invalidate();
```

## 5. 过滤器 (Filter)

过滤器可以在请求到达 Servlet 之前或之后执行通用逻辑，如编码设置、权限检查、日志记录等：

```java
@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        // 设置编码
        request.setCharacterEncoding("UTF-8");

        // 检查登录状态
        String uri = request.getRequestURI();
        if (isProtectedPath(uri) && request.getSession(false) == null) {
            ((HttpServletResponse) res).sendRedirect("/login");
            return;
        }

        // 继续执行
        chain.doFilter(req, res);
    }
}
```

## 6. 监听器 (Listener)

监听器用于监听 Servlet 容器中的事件：

```java
@WebListener
public class AppInitListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 应用启动时执行
        System.out.println("应用启动");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 应用关闭时执行
        System.out.println("应用关闭");
    }
}
```

## 7. 文件上传

Servlet 3.0+ 原生支持文件上传，使用 `@MultipartConfig` 注解：

```java
@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,  // 1MB 内存阈值
    maxFileSize = 10 * 1024 * 1024,   // 单文件最大 10MB
    maxRequestSize = 50 * 1024 * 1024 // 总请求最大 50MB
)
public class UploadServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Part filePart = req.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        String savePath = "/uploads/" + UUID.randomUUID() + "_" + fileName;
        filePart.write(savePath);

        resp.getWriter().print("{\"path\":\"" + savePath + "\"}");
    }
}
```

## 8. 总结

Servlet 是 Java Web 开发的基础，即使使用 Spring MVC 等框架，底层仍然是 Servlet。理解 Servlet 的工作原理有助于：

- 编写高性能的 Web 应用
- 理解框架底层的请求处理机制
- 排查线上问题（如会话丢失、编码乱码等）

掌握 Servlet 是成为合格 Java 后端工程师的必经之路。',
'markdown', 1, 1, NULL, NOW(), NOW());

-- =============================================
-- 文章 2: RESTful API 设计指南
-- =============================================
INSERT IGNORE INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status, cover_image, create_time, update_time)
VALUES (1, 'RESTful API 设计最佳实践',
'从 URL 命名、HTTP 方法选择、状态码使用到分页与错误处理，系统阐述如何设计清晰、一致且易于维护的 RESTful 接口。',
'# RESTful API 设计最佳实践

## 1. REST 是什么

REST（Representational State Transfer）是 Roy Fielding 在 2000 年博士论文中提出的架构风格。它定义了一组约束，符合这些约束的 Web 服务被称为 RESTful 服务。

核心约束：

- **客户端-服务端分离**：前后端独立演进
- **无状态**：每次请求必须包含所有必要信息，服务端不保存客户端上下文
- **统一接口**：通过 URL 标识资源，通过 HTTP 方法表达操作
- **分层系统**：中间层（缓存、代理、网关）对客户端透明

## 2. URL 设计

### 2.1 用名词表示资源

```
✅ GET  /api/users
✅ GET  /api/users/123
❌ GET  /api/getUser?id=123
❌ GET  /api/deleteUser/123
```

URL 应该表示**资源**，而不是**动作**。动作由 HTTP 方法来表达。

### 2.2 使用复数形式

```
✅ /api/users
✅ /api/users/123/articles
❌ /api/user
❌ /api/user/123/article
```

### 2.3 嵌套资源不超过两层

```
✅ GET /api/teams/5/members          // 团队成员列表
✅ GET /api/teams/5/members/10       // 团队中的某个成员
❌ GET /api/teams/5/members/10/roles/2  // 过深嵌套
```

如果嵌套过深，可以改用查询参数：

```
✅ GET /api/roles?teamId=5&memberId=10
```

## 3. HTTP 方法

| 方法 | 用途 | 幂等 | 安全 |
|------|------|------|------|
| GET | 获取资源 | 是 | 是 |
| POST | 创建资源 | 否 | 否 |
| PUT | 全量更新资源 | 是 | 否 |
| PATCH | 部分更新资源 | 否 | 否 |
| DELETE | 删除资源 | 是 | 否 |

### 3.1 示例

```
GET    /api/articles          → 获取文章列表
POST   /api/articles          → 创建新文章
GET    /api/articles/42       → 获取单篇文章
PUT    /api/articles/42       → 全量更新文章
PATCH  /api/articles/42       → 部分更新文章
DELETE /api/articles/42       → 删除文章
```

## 4. 状态码

正确使用 HTTP 状态码可以让 API 语义更清晰：

### 4.1 常用状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | GET 成功、PUT/PATCH 更新成功 |
| 201 | 已创建 | POST 创建资源成功 |
| 204 | 无内容 | DELETE 成功 |
| 400 | 请求错误 | 参数校验失败 |
| 401 | 未认证 | 未登录 |
| 403 | 无权限 | 已登录但权限不足 |
| 404 | 未找到 | 资源不存在 |
| 409 | 冲突 | 重复创建 |
| 500 | 服务器错误 | 代码异常 |

### 4.2 错误响应格式

```json
{
    "code": 400,
    "msg": "参数校验失败",
    "errors": [
        { "field": "username", "message": "用户名不能为空" },
        { "field": "email", "message": "邮箱格式不正确" }
    ]
}
```

## 5. 分页与排序

### 5.1 分页参数

```
GET /api/articles?page=1&pageSize=20
```

### 5.2 分页响应

```json
{
    "data": [
        { "id": 1, "title": "文章标题" }
    ],
    "pagination": {
        "page": 1,
        "pageSize": 20,
        "total": 156,
        "totalPages": 8
    }
}
```

### 5.3 排序

```
GET /api/articles?sortBy=createTime&order=desc
```

## 6. 过滤与搜索

使用查询参数进行过滤：

```
GET /api/articles?status=published&authorId=5
GET /api/articles?keyword=RESTful
GET /api/articles?createdAfter=2025-01-01
```

## 7. 版本控制

推荐在 URL 中包含版本号：

```
/api/v1/users
/api/v2/users
```

也可以用 HTTP Header：

```
Accept: application/vnd.myapp.v2+json
```

URL 方式更直观，适合大多数项目。

## 8. 实际项目中的约定

### 8.1 统一响应格式

```json
{
    "code": 200,
    "msg": "success",
    "data": { ... }
}
```

所有接口统一返回包含 `code`、`msg`、`data` 的 JSON 对象，前端处理更一致。

### 8.2 登录与认证

```
POST /api/auth/login    { username, password }
POST /api/auth/logout
GET  /api/auth/current  → 获取当前登录用户
```

认证使用 Cookie + Session 或 JWT Token。

### 8.3 CORS 配置

前后端分离项目必须处理跨域：

```java
// 在 Filter 中设置 CORS 头
response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
response.setHeader("Access-Control-Allow-Headers", "Content-Type");
response.setHeader("Access-Control-Allow-Credentials", "true");
```

## 9. 总结

好的 API 设计应该让使用者：

1. **看 URL 就知道操作什么资源**
2. **看 HTTP 方法就知道做什么操作**
3. **看状态码就知道结果如何**
4. **看错误信息就知道问题在哪**

遵循 RESTful 约定可以大幅降低前后端沟通成本，提高团队协作效率。',
'markdown', 1, 1, NULL, NOW(), NOW());

-- =============================================
-- 文章 3: 前后端分离架构实践
-- =============================================
INSERT IGNORE INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status, cover_image, create_time, update_time)
VALUES (2, '前后端分离架构实践',
'探讨前后端分离的开发模式、通信协议选择、跨域处理、部署策略以及常见问题的解决方案。',
'# 前后端分离架构实践

## 1. 为什么前后端分离

传统的单体架构中，JSP/Thymeleaf 模板引擎负责渲染页面，前端和后端代码耦合在一起。前后端分离将前端（HTML/CSS/JS）和后端（Java API）完全解耦：

| 对比项 | 模板渲染 | 前后端分离 |
|--------|----------|------------|
| 开发效率 | 前后端互相等待 | 前后端并行开发 |
| 交互体验 | 页面刷新 | 局部更新，更流畅 |
| 技术选型 | 受限于后端 | 前端自由选型 |
| 部署方式 | 打在一起 | 独立部署 |
| 人才匹配 | 全栈 | 前端+后端各自专注 |

## 2. 技术选型

### 2.1 前端

- **Vue 3** + **TypeScript**：渐进式框架，上手简单，生态丰富
- **Vite**：开发服务器极速热更新，构建速度快
- **Pinia**：Vue 3 官方推荐的状态管理库
- **Axios**：HTTP 客户端，拦截器机制完善

### 2.2 后端

- **Java Servlet**：不使用 Spring，手写原生 Servlet
- **JDBC**：不使用 MyBatis，手写 SQL
- **MariaDB/MySQL**：关系型数据库
- **WebSocket**：原生 Java WebSocket API

### 2.3 通信

- REST API：`application/x-www-form-urlencoded` 或 `application/json`
- WebSocket：实时协同编辑和聊天

## 3. API 通信设计

### 3.1 请求封装

前端使用 Axios 拦截器统一处理：

```typescript
const request = axios.create({
  baseURL: '/api',
  withCredentials: true,  // 携带 Cookie
  timeout: 12000,
})

// 请求拦截：自动转换 JSON 为 form-urlencoded
request.interceptors.request.use((config) => {
  if (config.data && !(config.data instanceof FormData)) {
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries(config.data)) {
      if (value != null) params.append(key, String(value))
    }
    config.data = params
  }
  return config
})

// 响应拦截：统一错误处理
request.interceptors.response.use((response) => {
  const { code, msg, data } = response.data
  if (code === 200) return data
  if (code === 401) { /* 跳转登录 */ }
  return Promise.reject(new Error(msg))
})
```

### 3.2 响应格式

```json
{
    "code": 200,
    "msg": "success",
    "data": {
        "id": 1,
        "username": "admin",
        "nickname": "管理员"
    }
}
```

### 3.3 错误处理策略

| 层级 | 职责 |
|------|------|
| Axios 拦截器 | 统一弹窗提示，处理 401 跳转 |
| 业务函数 | try/catch 处理特定业务错误 |
| 组件层 | 展示错误状态，提供重试按钮 |

## 4. 跨域处理

前后端在不同端口运行时（如前端 5173，后端 8081），需要处理跨域。

### 4.1 开发环境：Vite 代理

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true,
      },
    },
  },
})
```

### 4.2 生产环境：CORS 或 Nginx

方案一：后端 CORS Filter

```java
@WebFilter("/*")
public class CorsFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "https://your-domain.com");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        chain.doFilter(req, res);
    }
}
```

方案二：Nginx 反向代理

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /var/www/frontend;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8081;
    }

    location /ws/ {
        proxy_pass http://localhost:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 5. 用户认证

### 5.1 Session + Cookie

本项目采用传统的 Session 方案：

1. 用户登录，服务端创建 Session，返回 `JSESSIONID` Cookie
2. 前端每次请求自动携带 Cookie（`withCredentials: true`）
3. 服务端通过 `request.getSession()` 获取会话信息

### 5.2 认证流程

```
用户输入账密 → POST /api/auth/login
            → 服务端验证
            → 创建 Session，设置 Cookie
            → 返回用户信息

后续请求 → Cookie 自动携带
        → Filter 检查 Session
        → 有效则放行，无效则返回 401

退出 → POST /api/auth/logout
     → 销毁 Session
     → 前端清除本地状态
```

## 6. 构建与部署

### 6.1 前端构建

```bash
cd frontend
npm run build    # 生成 dist/ 目录
```

### 6.2 部署选项

**方案一：静态资源部署**

将 `dist/` 目录放到 Nginx 或 Tomcat 的 webapps 下。

**方案二：打 WAR 包**

将前端构建产物放入后端 WAR 的 `webapp/` 目录，前后端共用一个 Tomcat。

## 7. 常见问题

### 7.1 请求参数获取不到

后端使用 `request.getParameter()` 时，前端必须发送 `application/x-www-form-urlencoded` 格式。如果前端发送 JSON，后端需要从 `request.getInputStream()` 读取。

### 7.2 Session 丢失

- 检查 Cookie 域名是否一致
- 检查 `withCredentials: true` 是否设置
- 检查 CORS 是否允许 Credentials
- 检查反向代理是否转发 Cookie

### 7.3 编码乱码

```java
request.setCharacterEncoding("UTF-8");
response.setCharacterEncoding("UTF-8");
response.setContentType("application/json;charset=UTF-8");
```

## 8. 总结

前后端分离不是万能方案，但对于中大型项目和团队协作，它的优势明显：

- 前后端独立开发、独立部署
- 前端可以享受现代框架的工程化能力
- 后端只提供 API，职责更单一
- 扩展性和可维护性更好

本项目采用的就是这套架构：Vue 3 前端 + Java Servlet 后端，通过 REST API 通信，通过 WebSocket 实现实时功能。',
'markdown', 1, 1, NULL, NOW(), NOW());

-- =============================================
-- 文章 4: WebSocket 实时通信入门
-- =============================================
INSERT IGNORE INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status, cover_image, create_time, update_time)
VALUES (2, 'WebSocket 实时通信入门',
'介绍 WebSocket 协议原理、Java WebSocket API 使用方法、以及如何在项目中实现实时聊天与协同编辑。',
'# WebSocket 实时通信入门

## 1. 为什么需要 WebSocket

HTTP 是请求-响应模型：客户端发送请求，服务端返回响应。但很多场景需要**服务端主动推送数据**：

- 即时聊天消息
- 多人文档协同编辑
- 实时通知推送
- 在线状态同步

传统方案（轮询、长轮询）效率低下。WebSocket 在 HTTP 握手后升级为全双工 TCP 连接，双方可以随时发送数据。

### 1.1 与 HTTP 对比

| 特性 | HTTP | WebSocket |
|------|------|-----------|
| 通信方式 | 请求-响应 | 全双工 |
| 连接生命周期 | 短连接 | 持久连接 |
| 服务端推送 | 不支持 | 原生支持 |
| 开销 | 每次请求带完整头部 | 握手后数据帧开销极小 |
| 适用场景 | CRUD 操作 | 实时通信 |

## 2. WebSocket 协议

### 2.1 握手过程

客户端发起 HTTP 请求，携带 Upgrade 头：

```
GET /ws/doc HTTP/1.1
Host: localhost:8081
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

服务端同意升级，返回 101：

```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

握手完成后，连接从 HTTP 协议切换为 WebSocket 协议，双方可以自由收发消息。

### 2.2 数据帧

WebSocket 通信的最小单位是帧（Frame）：

- **文本帧**：传输 UTF-8 文本数据（如 JSON）
- **二进制帧**：传输二进制数据（如图片、Protobuf）
- **控制帧**：Close、Ping、Pong

## 3. Java WebSocket API

### 3.1 添加依赖

Tomcat 10+ 内置 WebSocket 支持，无需额外依赖。如果是 Maven 项目：

```xml
<dependency>
    <groupId>jakarta.websocket</groupId>
    <artifactId>jakarta.websocket-api</artifactId>
    <version>2.1.0</version>
    <scope>provided</scope>
</dependency>
```

### 3.2 创建服务端端点

```java
@ServerEndpoint("/ws/doc")
public class DocWebSocket {

    // 所有连接的会话
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        System.out.println("连接建立: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 收到消息，广播给所有连接
        for (Session s : sessions.values()) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(message);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        System.out.println("连接关闭: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        sessions.remove(session.getId());
    }
}
```

### 3.3 关键注解

| 注解 | 触发时机 |
|------|----------|
| `@ServerEndpoint` | 声明 WebSocket 端点 URL |
| `@OnOpen` | 连接建立时 |
| `@OnMessage` | 收到消息时 |
| `@OnClose` | 连接关闭时 |
| `@OnError` | 发生异常时 |

## 4. 房间模型

在实际项目中，不能简单广播给所有人。需要**房间**概念——同一篇文章的协作者在同一个房间内通信。

### 4.1 房间管理

```java
public class RoomManager {
    // 文章ID → 该房间内的所有会话
    private static final Map<String, Set<Session>> rooms = new ConcurrentHashMap<>();

    public static void join(String articleId, Session session) {
        rooms.computeIfAbsent(articleId, k -> ConcurrentHashMap.newKeySet())
             .add(session);
    }

    public static void leave(String articleId, Session session) {
        Set<Session> room = rooms.get(articleId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) {
                rooms.remove(articleId);
            }
        }
    }

    public static void broadcast(String articleId, String message) {
        Set<Session> room = rooms.get(articleId);
        if (room == null) return;
        for (Session s : room) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(message);
            }
        }
    }
}
```

### 4.2 消息格式

```json
{
    "type": "chat_message",
    "room": "article:42:chat",
    "payload": {
        "userId": 1,
        "nickname": "张三",
        "content": "你好！"
    },
    "timestamp": 1713001234567
}
```

## 5. 心跳机制

WebSocket 连接可能因为网络问题静默断开。心跳可以及时发现并处理：

### 5.1 服务端心跳

```java
@ServerEndpoint("/ws/doc")
public class DocWebSocket {

    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    static {
        // 每 30 秒检查一次
        scheduler.scheduleAtFixedRate(() -> {
            for (Session session : sessions.values()) {
                if (!session.isOpen()) {
                    sessions.remove(session.getId());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}
```

### 5.2 客户端心跳

```javascript
const ws = new WebSocket('ws://localhost:8081/ws/doc')

// 每 30 秒发送心跳
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'heartbeat' }))
    }
}, 30000)

// 监听断线
ws.onclose = () => {
    console.log('连接断开，3 秒后重连...')
    setTimeout(connect, 3000)
}
```

## 6. 与 Yjs 集成

Yjs 是一个 CRDT（无冲突复制数据类型）框架，用于实时协同编辑。它通过 WebSocket 同步文档状态。

### 6.1 基本流程

```
1. 用户打开文章编辑页
2. 前端创建 Y.Doc 对象
3. 建立 WebSocket 连接
4. 加入文章对应的房间
5. Yjs 通过 WebSocket 同步文档变更
6. 离开页面时断开连接
```

### 6.2 前端代码示例

```javascript
import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'

const ydoc = new Y.Doc()
const provider = new WebsocketProvider(
    'ws://localhost:8081/ws/doc',
    'article-42',
    ydoc
)

// 监听连接状态
provider.on('status', event => {
    console.log('WebSocket:', event.status)
})
```

## 7. 安全考虑

- **认证**：WebSocket 连接时检查用户身份（通过 HTTP Session 或 URL 参数中的 Token）
- **授权**：验证用户是否有权限加入该文章的房间
- **限流**：防止消息洪水攻击
- **输入验证**：过滤 XSS 等恶意内容

## 8. 总结

WebSocket 为 Web 应用提供了实时通信能力，是实现协同编辑、即时聊天的关键技术。Java 原生 WebSocket API 简洁易用，结合 Yjs 可以快速实现多人实时协同编辑功能。',
'markdown', 1, 1, NULL, NOW(), NOW());

-- =============================================
-- 文章 5: Git 团队协作流程
-- =============================================
INSERT IGNORE INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status, cover_image, create_time, update_time)
VALUES (1, 'Git 团队协作流程规范',
'从分支策略、提交规范到代码审查，梳理小团队使用 Git 进行高效协作的完整流程。',
'# Git 团队协作流程规范

## 1. 分支策略

小团队推荐使用 **GitHub Flow**——简单、高效、够用。

### 1.1 核心分支

| 分支 | 用途 | 保护 |
|------|------|------|
| `main` | 生产代码，永远可部署 | 是 |
| `feature/*` | 功能开发 | 否 |
| `fix/*` | Bug 修复 | 否 |

### 1.2 工作流程

```
1. 从 main 拉取最新代码
   git checkout main
   git pull

2. 创建功能分支
   git checkout -b feature/user-auth

3. 开发并提交
   git add .
   git commit -m "feat: 添加用户登录功能"

4. 推送到远程
   git push -u origin feature/user-auth

5. 创建 Pull Request
   在 GitHub/GitLab 上创建 PR，请求合并到 main

6. 代码审查通过后合并
   使用 Squash Merge 保持 main 历史整洁
```

## 2. 提交规范

### 2.1 Conventional Commits

格式：`<type>(<scope>): <subject>`

```
feat(auth): 添加用户登录功能
fix(article): 修复文章保存时空内容的问题
docs(api): 更新 API 接口文档
refactor(utils): 重构请求拦截器逻辑
style(css): 统一按钮样式
test(user): 添加用户模块单元测试
chore(deps): 升级依赖版本
```

### 2.2 Type 说明

| Type | 含义 |
|------|------|
| feat | 新功能 |
| fix | 修复 Bug |
| docs | 文档变更 |
| style | 代码格式（不影响逻辑） |
| refactor | 重构（不是新功能也不是修 Bug） |
| test | 测试相关 |
| chore | 构建或辅助工具变动 |

### 2.3 好的提交 vs 坏的提交

```bash
# 好：意图清晰，粒度合适
git commit -m "feat(team): 添加团队成员邀请功能"
git commit -m "fix(login): 修复 Session 过期后未跳转登录页"

# 坏：模糊不清
git commit -m "update"
git commit -m "fix bug"
git commit -m "完成了今天的工作"
```

## 3. Pull Request 规范

### 3.1 PR 标题

使用与提交信息相同的格式：

```
feat(auth): 添加用户注册与登录功能
```

### 3.2 PR 描述模板

```markdown
## 变更说明

简要描述本次变更的内容和目的。

## 变更类型

- [ ] 新功能
- [ ] Bug 修复
- [ ] 重构
- [ ] 文档

## 测试

- [ ] 手动测试通过
- [ ] 已添加/更新测试用例

## 关联 Issue

Closes #12
```

### 3.3 Code Review 检查点

- 功能是否正确实现
- 代码风格是否一致
- 是否有明显的性能问题
- 是否有安全隐患
- 是否需要补充测试

## 4. 冲突处理

### 4.1 预防冲突

- 频繁从 main 合并/变基最新代码
- 小步提交，减少单次变更范围
- 团队成员避免同时修改同一文件

### 4.2 解决冲突

```bash
# 方式一：merge（保留完整历史）
git checkout feature/user-auth
git merge main
# 解决冲突后
git add .
git commit

# 方式二：rebase（保持线性历史）
git checkout feature/user-auth
git rebase main
# 解决冲突后
git add .
git rebase --continue
```

### 4.3 选择哪种

- 个人功能分支：rebase 更清爽
- 多人协作分支：merge 更安全
- **不确定就用 merge**

## 5. .gitignore 配置

```gitignore
# 编译产物
*.class
*.jar
target/

# 前端构建
node_modules/
dist/

# IDE
.idea/
*.iml
.vscode/

# 系统文件
.DS_Store
Thumbs.db

# 环境配置（敏感信息）
.env
*.local

# 日志
*.log
logs/
```

## 6. 常用 Git 技巧

### 6.1 暂存工作

```bash
git stash           # 暂存当前修改
git stash pop       # 恢复暂存
git stash list      # 查看暂存列表
```

### 6.2 撤销操作

```bash
# 撤销工作区修改（未 add）
git checkout -- file.txt

# 撤销暂存（已 add，未 commit）
git reset HEAD file.txt

# 撤销最后一次提交（已 commit，未 push）
git reset --soft HEAD~1    # 保留修改
git reset --hard HEAD~1    # 丢弃修改（危险）
```

### 6.3 查看历史

```bash
# 简洁查看
git log --oneline --graph --all

# 搜索特定变更
git log -S "functionName"

# 查看某个文件的修改历史
git log --follow -p file.java
```

## 7. 团队约定

### 7.1 开发前

1. 拉取最新 main
2. 创建功能分支
3. 确认需求理解一致

### 7.2 开发中

1. 小步提交，频繁推送
2. 提交信息遵循 Conventional Commits
3. 定期从 main 同步最新代码

### 7.3 开发后

1. 自测通过
2. 创建 PR，填写完整描述
3. 请求 Code Review
4. 合并后删除功能分支

## 8. 总结

好的 Git 工作流不在于多复杂，而在于团队是否一致执行。GitHub Flow 适合大多数小团队：

- `main` 永远可部署
- 功能在分支开发
- PR + Code Review 保证质量
- 规范的提交信息方便回溯',
'markdown', 1, 1, NULL, NOW(), NOW());
