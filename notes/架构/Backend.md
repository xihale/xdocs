# 后端设计

## 1. 设计目标

后端负责：

- 接收前端请求
- 处理业务逻辑
- 管理数据库访问
- 提供认证与权限控制
- 提供协同编辑和聊天所需的 REST / WebSocket 能力
- 统一返回 JSON 数据

整体设计将参考 `xilixili`，坚持分层清晰、命名明确、便于答辩说明的原则。

---

## 2. 技术环境

- JDK 21
- Maven
- Servlet + Filter + Session
- JDBC
- MariaDB / MySQL
- JSON 工具库
- `websocket` 包用于实时连接

---

## 3. 项目结构

建议后端继续采用如下分层：

```text
src/main/java/top/xihale/xxx/
├── constant/           # 常量与枚举
├── config/             # 配置读取
├── util/               # 工具类
├── po/                 # 实体对象
├── vo/                 # 视图对象
├── dao/                # 数据访问层
├── service/            # 业务逻辑层
├── servlet/            # REST 接口层
├── websocket/          # WebSocket 连接与房间管理
├── filter/             # 过滤器
└── exception/          # 自定义异常
```

---

## 4. 分层职责

### 4.1 constant

- 响应码
- 用户状态
- 平台角色
- TEAM 角色
- 知识库角色
- 知识库可见性
- 文章状态
- 聊天消息类型
- 协同房间状态

### 4.2 config

- 数据库配置
- Web 配置（CORS / CSRF）
- 文件上传配置
- 邮件配置
- WebSocket 相关基础配置

### 4.3 util

- JDBC 工具类
- JSON 工具类
- 密码加密工具类
- 响应输出工具类
- 验证码工具类
- 文件上传辅助工具类
- 日期时间工具类

### 4.4 po

与数据库表一一对应，尽量保持字段直接映射，避免在 PO 内放过多业务逻辑。

### 4.5 vo

用于接口返回的视图对象，封装额外展示字段，例如：

- 文章列表附带作者昵称
- 知识库详情附带创建者名称
- TEAM 成员列表附带用户昵称和头像
- 聊天消息附带发送者名称

### 4.6 dao

- 只负责 SQL 与结果映射
- 不写权限判断
- 使用 `PreparedStatement`
- `findById` 等查询返回 `Optional<T>`

### 4.7 service

- 处理注册登录流程
- 校验 TEAM / 知识库 / 文章权限
- 管理协同保存与消息广播前的业务检查
- 封装多个 DAO 的组合操作

### 4.8 servlet

- 接收 REST 请求
- 调用 Service
- 返回统一 JSON
- 所有业务 Servlet 继承统一基类

### 4.9 websocket

- 管理连接会话
- 处理房间加入与退出
- 维护在线用户
- 转发聊天消息与协同消息
- 处理心跳和异常断开

### 4.10 filter

- CORS
- 全局异常捕获
- 登录恢复
- CSRF 检查

---

## 5. Servlet 设计

建议采用与 `xilixili` 一致的统一基类与注解路由思路。

### 建议的 Servlet 划分

- `AuthServlet`
  - 注册
  - 登录
  - 登出
  - 当前用户
  - 图片验证码
  - 发送邮箱验证码
  - 忘记密码

- `UserServlet`
  - 个人资料查询
  - 修改昵称
  - 修改头像
  - 修改密码
  - 我的 TEAM / 我的知识库 / 我的文章聚合信息

- `TeamServlet`
  - 创建 TEAM
  - TEAM 列表
  - TEAM 详情
  - 邀请成员
  - 退出 TEAM
  - 调整成员角色
  - 处理邀请

- `KnowledgeBaseServlet`
  - 创建知识库
  - 修改知识库
  - 删除知识库
  - 知识库详情
  - 可访问知识库列表
  - 知识库成员授权

- `ArticleServlet`
  - 创建文章
  - 更新文章
  - 删除文章
  - 文章详情
  - 按知识库列出文章
  - 首页公开文章列表
  - 手动保存文章

- `UploadServlet`
  - 图片上传
  - 头像上传

- `ChatServlet`
  - 获取历史聊天消息
  - 获取在线成员等辅助信息

---

## 6. Filter 设计

### `CorsFilter`

统一设置跨域响应头，支持本地前端开发环境访问。

### `ExceptionFilter`

统一捕获异常并转为 JSON 输出，避免接口直接返回容器错误页。

### `AuthFilter`

从 Session 恢复当前用户，并向 request 中写入：

- `userId`
- `currentUser`
- `role`
- `userStatus`

### `CsrfFilter`

对登录用户的不安全请求校验来源，减少 CSRF 风险。

---

## 7. 核心业务模块设计

### 7.1 认证模块

- 注册：用户名 + 邮箱 + 密码 + 邮箱验证码
- 登录：用户名/邮箱 + 密码 + 图片验证码
- 忘记密码：邮箱验证码通过后重置密码
- 会话维持：Session + Cookie
- 密码安全：BCrypt

### 7.2 TEAM 模块

- 一个用户可加入多个 TEAM
- 一个 TEAM 有 OWNER / ADMIN / MEMBER
- TEAM 支持邀请成员、退出成员、查看成员列表
- TEAM 是知识库的归属容器之一

### 7.3 知识库模块

- 支持个人知识库与 TEAM 知识库
- 支持公开 / 私有
- 支持成员授权：OWNER / ADMIN / EDITOR / VIEWER
- 支持文章列表与详情入口

### 7.4 文章模块

- 标题、摘要、正文、封面、作者、所属知识库
- 支持创建、编辑、删除、查看
- 正文落库必须可靠，保证即使不在线协同也能正常展示

---

## 8. 权限校验原则

### 平台层

- ADMIN：平台级管理
- USER：普通用户

### TEAM 层

- OWNER：管理 TEAM 基本信息和成员
- ADMIN：协助管理 TEAM
- MEMBER：普通成员

### 知识库层

- OWNER：知识库最高权限
- ADMIN：管理知识库与文章
- EDITOR：编辑文章
- VIEWER：只读

### 权限判断顺序

建议在 Service 层统一执行：

1. 校验是否登录
2. 校验是否属于目标 TEAM / 知识库
3. 校验角色是否满足操作要求
4. 再进入数据更新逻辑

---

## 9. WebSocket 后端职责

后端实时模块首版只做必要职责：

- 连接建立与身份识别
- 房间加入 / 退出
- 在线成员广播
- 聊天消息转发
- Yjs 协同消息透传
- 心跳检查与房间清理
- 聊天消息落库

不在首版承担过重的 CRDT 逻辑解释和复杂版本系统。

---

## 10. 代码规范要求

- 类、方法、属性要补齐必要注释
- 常量统一放入常量类或枚举类
- 避免魔法值
- 命名清晰
- SQL 放在 DAO 层
- 业务判断放在 Service 层
- 保持分层边界清晰

---

## 11. 后续关联文档

- 数据库设计：`notes/架构/Database.md`
- 前端设计：`notes/架构/Frontend.md`
- 实时协同与聊天：`notes/架构/Realtime.md`
- 路线图：`notes/架构/Roadmap.md`
