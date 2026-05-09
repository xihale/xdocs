# 数据库设计

## 1. 设计目标

数据库需要支撑以下核心业务：

- 用户注册与登录
- TEAM 与成员关系
- 个人知识库与 TEAM 知识库
- 文章内容存储
- 协同房间标识
- 聊天消息记录
- 图片 / 文件上传

设计原则：

- 表结构直观清楚
- 关系清晰但不过度复杂
- 尽量少连表、少级联
- 便于学生答辩讲解
- 便于后续继续扩展

---

## 2. 核心实体关系

### 用户与 TEAM

- 一个用户可以创建多个 TEAM
- 一个用户可以加入多个 TEAM
- 一个 TEAM 可以拥有多个成员

### 用户与知识库

- 一个用户可以创建多个个人知识库
- 一个知识库可归属某个用户或某个 TEAM
- 一个知识库可以有多个成员

### 知识库与文章

- 一个知识库包含多篇文章
- 一篇文章只属于一个知识库
- 一篇文章可对应一个协同房间
- 一篇文章可对应多条聊天消息

---

## 3. 表设计清单

### 3.1 `sys_user`

用户基础表。

建议字段：

- `id`
- `username`
- `password`
- `email`
- `nickname`
- `avatar_url`
- `status`
- `create_time`
- `update_time`

说明：

- `username` 唯一
- `email` 唯一
- `status` 用于标记正常 / 封禁等状态

### 3.2 `email_code`

邮箱验证码表。

建议字段：

- `id`
- `email`
- `code`
- `biz_type`
- `expire_time`
- `used`
- `create_time`

说明：

- `biz_type` 用于区分注册、重置密码等场景
- 首版只需支持简单过期和已使用标记

### 3.3 `team`

TEAM 表。

建议字段：

- `id`
- `name`
- `description`
- `owner_id`
- `avatar_url`
- `create_time`
- `update_time`

说明：

- `owner_id` 指向创建者
- TEAM 用作多人协作空间

### 3.4 `team_member`

TEAM 成员关系表。

建议字段：

- `id`
- `team_id`
- `user_id`
- `role`
- `join_status`
- `invite_by`
- `join_time`

说明：

- `team_id + user_id` 建立唯一约束
- `role` 可取 OWNER / ADMIN / MEMBER
- `join_status` 支持 invited / accepted / rejected

### 3.5 `knowledge_base`

知识库表。

建议字段：

- `id`
- `name`
- `description`
- `visibility`
- `owner_type`
- `owner_id`
- `creator_id`
- `create_time`
- `update_time`

说明：

- `visibility` 支持 PUBLIC / PRIVATE
- `owner_type` 区分 USER / TEAM
- `owner_id` 指向所属用户或 TEAM

### 3.6 `knowledge_base_member`

知识库成员关系表。

建议字段：

- `id`
- `knowledge_base_id`
- `user_id`
- `role`
- `invite_status`
- `invite_by`
- `join_time`

说明：

- `role` 支持 OWNER / ADMIN / EDITOR / VIEWER
- `knowledge_base_id + user_id` 建立唯一约束

### 3.7 `article`

文章表。

建议字段：

- `id`
- `knowledge_base_id`
- `title`
- `summary`
- `content`
- `content_format`
- `author_id`
- `status`
- `cover_image`
- `create_time`
- `update_time`

说明：

- `content` 至少保留一份可以直接展示的正文快照
- `content_format` 可标记 markdown / html / milkdown-json
- `status` 可先简化为 draft / published

### 3.8 `article_collab_room`

协同房间表。

建议字段：

- `id`
- `article_id`
- `room_key`
- `status`
- `last_active_time`
- `create_time`

说明：

- 一篇文章对应一个主要协同房间
- `room_key` 供 WebSocket / Yjs 房间识别使用

### 3.9 `article_chat_message`

文档侧边栏聊天消息表。

建议字段：

- `id`
- `article_id`
- `team_id`
- `sender_id`
- `message_type`
- `content`
- `create_time`

说明：

- 聊天消息围绕文档场景展开
- 可选记录 `team_id`，便于区分团队语境

### 3.10 `team_chat_message`

TEAM 聊天消息表。

建议字段：

- `id`
- `team_id`
- `sender_id`
- `content`
- `create_time`

说明：

- 首版如果时间紧，可只预留设计
- 若实现，则作为 TEAM 级聊天频道

### 3.11 `upload_file`

上传文件表。

建议字段：

- `id`
- `biz_type`
- `biz_id`
- `file_name`
- `file_url`
- `file_size`
- `uploader_id`
- `create_time`

说明：

- 可用于头像、文章插图等上传记录
- 若时间紧，也可先在业务表中直接存 URL，后续再补文件表

---

## 4. 约束与索引建议

### 唯一约束

- `sys_user.username`
- `sys_user.email`
- `team_member(team_id, user_id)`
- `knowledge_base_member(knowledge_base_id, user_id)`
- `article_collab_room.article_id`

### 常用索引

- `article(knowledge_base_id, update_time)`
- `knowledge_base(owner_type, owner_id)`
- `team_member(user_id)`
- `article_chat_message(article_id, create_time)`
- `team_chat_message(team_id, create_time)`

---

## 5. 权限模型与数据映射

### TEAM 角色

- OWNER：TEAM 创建者
- ADMIN：TEAM 管理员
- MEMBER：普通成员

### 知识库角色

- OWNER：知识库所有者
- ADMIN：知识库管理员
- EDITOR：可编辑文章
- VIEWER：只读

### 关系原则

- TEAM 成员关系不直接等于知识库权限
- TEAM 只是组织容器
- 知识库仍保留自己的成员与角色关系
- 这样更利于之后扩展出部分 TEAM 成员只能访问某些知识库的能力

---

## 6. 首版简化策略

为了控制开发难度，首版建议：

- 不做复杂联表统计
- 不做级联删除
- 删除操作优先用业务逻辑清理关联数据
- 不做复杂历史版本表
- 不做消息未读统计的精细拆表

---

## 7. 后续扩展表

后续若继续扩展，可增加：

- `article_version`：文档历史版本
- `comment`：评论与回复
- `article_like`：点赞
- `favorite`：收藏
- `follow_user`：关注
- `notification`：系统通知
- `recent_visit`：最近访问记录

---

## 8. 关联文档

- 总体规划：`notes/架构/总体规划.md`
- 后端设计：`notes/架构/Backend.md`
- 前端设计：`notes/架构/Frontend.md`
- 实时协同与聊天：`notes/架构/Realtime.md`
- 路线图：`notes/架构/Roadmap.md`
