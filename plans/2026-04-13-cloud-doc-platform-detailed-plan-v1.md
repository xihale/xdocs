# 云文档在线管理平台总计划

## Objective

在严格遵守原生 JavaWeb 技术边界的前提下，参考 `xilixili` 的项目组织、分层方式、接口风格和文档习惯，完成一个适合学生作业实现的云文档在线管理平台。

本项目固定包含以下方向：

- TEAM 功能
- 一个用户可加入多个 TEAM
- 前端使用 Milkdown
- 多人协同使用 Yjs
- 实时通信使用 WebSocket
- 文档页面提供侧边栏聊天 / 协作交流区

同时坚持以下约束：

- 后端采用 Servlet + JDBC + MySQL(MariaDB) + Maven + Session/Json
- 禁止使用 Spring / SpringBoot / MyBatis 等高级框架
- 第一阶段优先保证“基础版能完整演示”
- 设计尽量向 `xilixili` 靠拢，但业务模型按文档平台重新设计

---

## Detailed Notes Index

详细规划已拆分至 `notes/` 目录，按主题查看：

- 总体规划：`notes/架构/总体规划.md`
- 后端设计：`notes/架构/Backend.md`
- 前端设计：`notes/架构/Frontend.md`
- 数据库设计：`notes/架构/Database.md`
- 实时协同与聊天：`notes/架构/Realtime.md`
- 实施路线图：`notes/架构/Roadmap.md`
- 开发过程记录：`notes/记录/开发记录.md`

---

## Phase Overview

### Phase 1：范围冻结与基础骨架

- 明确 MVP 范围：用户、TEAM、知识库、文章、基础协同、侧边栏聊天
- 搭建仓库结构：`backend / frontend / sql / notes`
- 搭建后端基础层：配置、统一响应、统一异常、JDBC、Filter、BaseServlet 路由体系
- 搭建前端基础层：Vue 3 + Vite + TS + Element Plus + Pinia + Axios

### Phase 2：数据库与认证模块

- 设计并输出核心数据库 `schema.sql`
- 准备测试数据 `init.sql`
- 实现注册、登录、登出、当前用户、忘记密码、验证码链路
- 实现用户中心基础能力

### Phase 3：TEAM 与知识库主业务

- 实现 TEAM 创建、查看、邀请、加入、退出与成员角色管理
- 实现个人知识库与 TEAM 知识库
- 实现知识库可见性与成员授权

### Phase 4：文章与编辑体验

- 实现文章 CRUD
- 实现图片上传
- 接入 Milkdown 单人编辑

### Phase 5：多人协同与聊天

- 实现 WebSocket 连接与房间模型
- 实现文档侧边栏聊天
- 接入 Yjs 协同编辑
- 完成协同内容落库策略
- 预留 TEAM 聊天频道

### Phase 6：页面收口、文档与答辩材料

- 完成首页与导航收口
- 补齐页面权限守卫与异常状态
- 完成 README、架构文档、开发记录
- 整理联调流程与答辩演示脚本

---

## Verification Criteria

- 后端完整采用原生 Servlet + JDBC + Session + JSON 方案，无高级 Java 框架依赖
- 数据库支持用户、TEAM、TEAM 成员、知识库、知识库成员、文章、聊天消息等核心实体
- 一个用户可以加入多个 TEAM，并可在个人中心或 TEAM 页面查看
- 知识库支持归属个人或 TEAM，并有基础可见性与成员权限控制
- 文章可创建、修改、删除、查看，并能在首页或知识库页展示
- 前端使用 Milkdown 完成文章编辑界面
- 多人可进入同一篇文章的协同编辑页面，并通过 Yjs + WebSocket 完成基础实时同步
- 文章页或编辑页具备右侧聊天侧边栏，并支持实时消息收发
- 登录、注册、忘记密码、验证码链路可完成基础演示
- README、数据库 SQL、架构说明、开发记录齐全

---

## Risks Summary

- 协同编辑复杂度高，需要先完成单人编辑再接入协同
- TEAM、知识库、文章三层权限容易混乱，需要先收敛角色
- WebSocket 容易扩散为大系统，首版只做必要房间与必要事件
- 前端库使用前需确认是否需要报备
- 页面数量较多，必须避免在 UI 细节上投入过多时间
- 文档和开发记录必须同步维护，不能最后集中补写
