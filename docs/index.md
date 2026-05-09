# CloudDoc 技术文档

> 在线文档与知识库协作平台。用户、团队、知识库、文章、协同编辑、聊天、通知全链路。

## 文档索引

| 文档 | 内容 |
|---|---|
| [架构设计](./architecture.md) | 系统目标、前后端分层、请求生命周期、核心业务流、部署 |
| [技术细节](./technical-details.md) | 认证、CSRF、路由框架、异常体系、ORM、SqlBuilder、连接池、协同编辑、上传 |
| [数据库设计](./database-design.md) | 14 张表 DDL、索引、关系、枚举常量、演进建议 |
| [API 参考](./api-reference.md) | 全部 HTTP + WebSocket 接口，参数、响应、示例 |
| [前端指南](./frontend-guide.md) | 目录结构、路由、状态管理、Hooks、组件、开发规范 |
| [可拓展性研究](./scalability-research.md) | 容量估算、性能瓶颈、横向扩展路线、协同编辑专项、可观测性 |

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | React 19 + Vite + TypeScript + Zustand + Tailwind CSS |
| 后端 | Java 21 + Jetty 12 + Servlet + JDBC |
| 数据库 | MariaDB + InnoDB + utf8mb4 |
| 实时 | WebSocket（聊天 + Yjs 协同编辑 + 通知推送） |
| 认证 | JWT Cookie + WebSocket 短期令牌 |

## 已完成优化

- 前端 API 层统一响应解包、JSON 解析、查询参数构造、上传封装
- 后端文章权限判断抽出复用函数，`checkArticleEditable` 与 `toVO.canEdit` 共享规则
- 后端 DAO 层引入反射 BaseMapper ORM，`@Table`/`@Id`/`@Column` 注解自动生成 CRUD SQL
- 后端异常体系重构为领域异常（6 个子类），错误码枚举化
- 后端作者展示名兜底：昵称为空回退用户名
