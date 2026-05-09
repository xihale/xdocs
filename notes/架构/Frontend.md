# 前端设计

## 1. 设计目标

前端负责：

- 展示知识库、文章、TEAM 等业务数据
- 提供基础表单交互
- 提供文章阅读与编辑界面
- 提供多人协同体验
- 提供右侧聊天与交流侧边栏

设计风格继续参照 `xilixili`：统一布局、清楚分区、避免花哨设计，优先保证功能完整和演示流畅。

---

## 2. 技术选型

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Axios
- Milkdown
- Yjs
- WebSocket 原生封装

---

## 3. 前端目录规划

建议结构如下：

```text
src/
├── views/
├── components/
│   ├── editor/
│   ├── chat/
│   └── common/
├── router/
├── stores/
├── utils/
├── types/
└── styles/
```

### 推荐子模块

- `components/editor/`
  - Milkdown 编辑器封装
  - 编辑工具栏包装
  - 协同状态提示

- `components/chat/`
  - 侧边栏聊天容器
  - 聊天气泡
  - 在线成员列表
  - 房间状态栏

- `stores/`
  - `user.ts`
  - `team.ts`
  - `collab.ts`
  - `knowledgeBase.ts`（可选）

- `utils/`
  - `request.ts`
  - `api.ts`
  - `ws.ts`
  - `toast.ts`

---

## 4. 页面设计

### 首页 `Home.vue`

- 展示公开知识库中的文章
- 展示推荐知识库或 TEAM 入口
- 提供搜索入口
- 风格与 `xilixili` 首页一致，但卡片内容改为文章 / 知识库

### 登录页 `Login.vue`

- 用户名或邮箱
- 密码
- 图片验证码
- 登录成功后支持跳回原页面

### 注册页 `Register.vue`

- 用户名
- 邮箱
- 昵称
- 密码
- 邮箱验证码

### 忘记密码页 `ForgotPassword.vue`

- 输入邮箱
- 获取验证码
- 重置密码

### 个人中心 `UserCenter.vue`

- 基础资料修改
- 头像上传
- 我的 TEAM
- 我的知识库
- 我的文章
- 邀请处理入口

### TEAM 列表页 `TeamList.vue`

- 展示我加入的所有 TEAM
- 支持创建 TEAM
- 支持进入 TEAM 详情页

### TEAM 详情页 `TeamDetail.vue`

- 展示 TEAM 名称、简介、成员数量
- 展示成员列表
- 展示 TEAM 下的知识库
- 提供成员管理入口

### 知识库详情页 `KnowledgeBaseDetail.vue`

- 左侧文章目录 / 文章列表
- 中间展示知识库简介或文章内容概览
- 管理员可进入创建文章 / 邀请成员等操作

### 文章阅读页 `ArticleDetail.vue`

- 展示标题、摘要、作者、更新时间
- 展示正文内容
- 右侧可显示聊天侧边栏
- 有编辑权限时可跳转协同编辑页

### 文章编辑页 `ArticleEditor.vue`

- 中间为 Milkdown 编辑器
- 左侧可显示文章目录或知识库上下文
- 右侧显示在线成员 + 聊天侧边栏
- 顶部提供保存状态、连接状态、手动保存按钮

### 邀请处理页 `InviteCenter.vue`

- 处理 TEAM 邀请
- 处理知识库成员邀请
- 也可以并入个人中心页

---

## 5. 路由设计

### 游客可访问

- `/`
- `/login`
- `/register`
- `/forgot-password`
- `/kb/:id`
- `/article/:id`

### 登录用户可访问

- `/user`
- `/teams`
- `/team/:id`
- `/kb/:id/manage`（有权限时）
- `/article/:id/edit`（有权限时）

### 权限控制原则

- 未登录访问受限页面时跳转登录页
- 无编辑权限访问编辑页时跳转文章阅读页
- 无 TEAM 管理权限时隐藏管理入口

---

## 6. 状态管理设计

### `user` store

- 当前用户信息
- 是否登录
- 是否管理员
- 登录 / 注册 / 登出
- 从后端恢复会话

### `team` store

- 我加入的 TEAM 列表
- 当前 TEAM
- TEAM 邀请数量
- 当前 TEAM 成员信息

### `collab` store

- 当前文章房间 ID
- WebSocket 连接状态
- 在线成员列表
- 最近一次保存时间
- 当前是否有未保存改动

### `knowledgeBase` store（可选）

- 当前知识库信息
- 当前知识库文章列表
- 当前用户在该知识库中的角色

---

## 7. 页面布局设计

### 整体布局

继续沿用 `xilixili` 的统一框架：

- 顶栏：Logo、搜索框、导航、用户区
- 主内容区：留白较多，区块边界清晰
- 列表页：卡片式展示
- 详情页：主内容 + 辅助侧栏

### 编辑页布局

建议使用三栏：

- 左栏：知识库目录 / 当前文章列表
- 中栏：Milkdown 编辑器
- 右栏：协作侧边栏（在线成员 + 聊天）

若页面宽度不足，可退化为双栏：

- 中间主编辑区
- 右侧折叠聊天侧栏

---

## 8. 请求与通信设计

### REST 请求

沿用统一 Axios 封装：

- 统一 `baseURL`
- 统一携带 Cookie
- 统一错误提示
- 统一 401 处理

### WebSocket 通信

前端通过 `utils/ws.ts` 统一管理：

- 建立连接
- 发送 join / leave / heartbeat
- 发送 chat / collab 消息
- 根据消息类型分发到对应模块

---

## 9. 编辑与协同策略

### 单人编辑阶段

- 先接入 Milkdown
- 完成文章内容加载与保存
- 保证不依赖协同也能稳定使用

### 多人协同阶段

- Yjs 负责共享文档状态
- WebSocket 负责消息传输
- 页面显示在线成员和连接状态
- 提供手动保存和自动保存提示

---

## 10. 前端优先级原则

1. 先把页面框架搭好
2. 再把数据展示和 CRUD 跑通
3. 再接入聊天
4. 最后接入多人协同
5. 不在视觉细节上投入过多时间

---

## 11. 关联文档

- 总体规划：`notes/架构/总体规划.md`
- 后端设计：`notes/架构/Backend.md`
- 数据库设计：`notes/架构/Database.md`
- 实时协同与聊天：`notes/架构/Realtime.md`
- 路线图：`notes/架构/Roadmap.md`
