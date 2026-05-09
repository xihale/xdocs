# 测试覆盖文档

## 概述

- **后端**：12 个测试类，135 个测试方法，全部通过。使用 `mockStatic` 隔离 DAO 层，覆盖 Service 层核心业务逻辑。
- **前端**：2 个测试文件，86 个测试方法，全部通过。使用 MSW 拦截 HTTP 请求，覆盖 API 层 + auth store。

## 后端测试

### 测试框架

- JUnit 5
- Mockito 5.x（含 `mockStatic`）
- AssertJ

### 测试文件列表

| 测试类 | 方法数 | 覆盖范围 |
|--------|--------|----------|
| UserServiceTest | 24 | 注册/登录/改密/查用户/管理员重置密码 |
| TeamServiceTest | 22 | 创建团队/邀请/接受拒绝/角色变更/踢人/退出 |
| ArticleServiceTest | 11 | CRUD/权限检查/公开文章查询/搜索 |
| KnowledgeBaseServiceTest | 13 | CRUD/成员授权/移除/权限校验 |
| ChatServiceTest | 4 | 保存消息/历史消息查询 |
| UploadServiceTest | 4 | 保存/查询/删除文件 |
| PasswordUtilsTest | 7 | 加密/验证/null边界 |
| JwtUtilTest | 9 | 生成/解析/过期/null边界 |
| JsonUtilsTest | 9 | 序列化/反序列化/null/空对象 |
| ConstantTest | 14 | 枚举值转换/fromValue边界 |
| PoTest | 13 | PO模型getter/setter/构造/toString |
| BusinessExceptionTest | 5 | 异常构造/responseCode |

**总计：135 个测试方法，0 failures, 0 errors**

### 测试策略

由于项目 DAO/Service 全部为 `static` 方法，无法用常规 Mockito mock 接口。采用 `mockStatic` mock 所有 DAO 调用，测试 Service 层业务逻辑：

- **正常路径**：注册成功、登录成功、创建团队成功等
- **异常路径**：用户名已存在、密码错误、团队不存在、无权限操作
- **边界条件**：null 参数、空字符串、自己邀请自己、所有者不能退出团队

### 发现的边界行为

- `PasswordUtils.verify(password, null)` 会抛 NPE（BCrypt 库限制），测试已记录此行为

### 运行测试

```bash
cd backend
mvn test

# 运行单个测试类
mvn test -Dtest=UserServiceTest

# 运行单个测试方法
mvn test -Dtest=UserServiceTest#register_Success
```

## 前端测试

### 测试框架

- Vitest
- MSW（Mock Service Worker）— 拦截 HTTP 请求
- React Testing Library
- JSDOM

### 测试文件列表

| 测试文件 | 方法数 | 覆盖范围 |
|----------|--------|----------|
| api.test.ts | 78 | request()工具函数/Auth/User/Article/KB/Team/Chat/Upload/Search API |
| auth.test.ts | 8 | init/login/register/logout/setUser — 含成功和失败路径 |

**总计：86 个测试方法，0 failures**

### 测试策略

- **MSW 拦截请求**：用 `setupServer` 拦截所有 `/api/*` 请求，不依赖真实后端
- **请求体验证**：捕获 request body，验证每个 API 发送了正确的参数
- **URL 参数验证**：捕获请求 URL，验证 query string 编码正确
- **错误路径**：HTTP 错误、业务 code != 200、fallback 错误消息
- **边界条件**：null data、空数组、无参数调用
- **Store 测试**：验证 zustand store 状态流转（loading/user/initialized）

### 覆盖的 API 模块

| 模块 | 测试数 | 覆盖内容 |
|------|--------|----------|
| request() | 7 | envelope解析/HTTP错误/业务错误/Content-Type/credentials |
| Auth | 12 | 登录/注册/登出/当前用户/验证码/重置密码/wsToken/captchaUrl |
| User | 13 | profile/昵称/头像/改密/关注/取关/粉丝/收藏/历史/删除历史 |
| Article | 17 | CRUD/公开列表/保存/点赞/评论/收藏/浏览/参数验证 |
| KnowledgeBase | 10 | CRUD/成员授权/移除/列表查询 |
| Team | 13 | 创建/列表/详情/邀请/接受拒绝/退出/角色/踢人 |
| Chat | 6 | 历史消息/发送/limit参数/错误fallback |
| Upload | 4 | 图片上传/头像上传/FormData验证/错误处理 |
| Search | 4 | 文章/KB/用户搜索/关键词编码/空结果 |
| Auth Store | 8 | init/login/register/logout/setUser — 成功和失败 |

### 运行测试

```bash
cd frontend
npm test

# 单次运行
npx vitest run

# 覆盖率
npx vitest run --coverage
```

## 待完善

- [ ] DAO 层集成测试（需要 H2 内存数据库）
- [ ] Servlet 层测试（HTTP 请求/响应）
- [ ] AuthFilter 过滤器测试
- [ ] 前端通知中心测试（列表/未读数/已读/删除/WS 推送）
- [ ] 前端组件测试（React 组件渲染/交互）
- [ ] useChat hook 测试（WebSocket mock）
- [ ] E2E 测试
