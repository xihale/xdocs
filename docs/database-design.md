# CloudDoc 数据库设计

## 1. 设计概览

数据库使用 MariaDB + InnoDB + utf8mb4。14 张表，7 个域：

| 域 | 表 | 说明 |
|---|---|---|
| 用户 | `sys_user` | 用户主表 |
| 用户 | `follow_user` | 关注关系 |
| 团队 | `team` | 团队元信息 |
| 团队 | `team_member` | 团队成员关系 |
| 知识库 | `knowledge_base` | 知识库元信息 |
| 知识库 | `knowledge_base_member` | 知识库成员关系 |
| 内容 | `article` | 文章主表 |
| 内容 | `comment` | 评论（支持多级） |
| 互动 | `article_like` | 文章点赞 |
| 互动 | `favorite` | 收藏 |
| 互动 | `recent_visit` | 浏览记录 |
| 互动 | `notification` | 通知 |
| 实时 | `article_chat_message` | 文章聊天 |
| 文件 | `upload_file` | 上传文件记录 |

## 2. 核心实体关系

```text
sys_user 1 ── N article
sys_user 1 ── N team(owner)
sys_user N ── M team via team_member
sys_user N ── M knowledge_base via knowledge_base_member
sys_user 1 ── N follow_user (follower/following)
knowledge_base 1 ── N article
article 1 ── N comment
article 1 ── N article_like
article 1 ── N article_chat_message
sys_user 1 ── N favorite
sys_user 1 ── N recent_visit
sys_user 1 ── N notification(recv)
```

## 3. 枚举常量参考

### 3.1 角色与状态

| 枚举 | 值 | 含义 |
|---|---|---|
| `Role` | 0=USER, 1=ADMIN | 平台角色 |
| `UserStatus` | 0=NORMAL, 1=BANNED | 用户状态 |
| `TeamRole` | 0=OWNER, 1=ADMIN, 2=MEMBER | 团队角色 |
| `KnowledgeBaseRole` | 0=OWNER, 1=ADMIN, 2=EDITOR, 3=VIEWER | 知识库角色 |
| `JoinStatus` | 0=INVITED, 1=ACCEPTED, 2=REJECTED | 邀请状态 |
| `Visibility` | 0=PRIVATE, 1=PUBLIC | 可见性 |
| `OwnerType` | 0=USER, 1=TEAM | 归属类型 |
| `ArticleStatus` | 0=DRAFT, 1=PUBLISHED | 文章状态 |
| `MessageType` | 0=TEXT, 1=SYSTEM | 聊天消息类型 |
| `NotificationType` | 0-7 | 通知类型（详见技术细节文档） |

## 4. 表设计细节

### 4.1 `sys_user`

```sql
CREATE TABLE sys_user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,           -- BCrypt hash
    email       VARCHAR(100) NOT NULL UNIQUE,
    nickname    VARCHAR(50)  NULL,
    avatar_url  VARCHAR(500) NULL,
    phone       VARCHAR(20)  NULL,
    role        TINYINT      NOT NULL DEFAULT 0,  -- 0=USER, 1=ADMIN
    status      TINYINT      NOT NULL DEFAULT 0,  -- 0=NORMAL, 1=BANNED
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

索引：`username`(UNIQUE)、`email`(UNIQUE) 由 UNIQUE 约束隐式提供。

### 4.2 `team` / `team_member`

```sql
CREATE TABLE team (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    owner_id    INT          NOT NULL,
    avatar_url  VARCHAR(500) NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
);

CREATE TABLE team_member (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    team_id     INT   NOT NULL,
    user_id     INT   NOT NULL,
    role        TINYINT NOT NULL DEFAULT 2,  -- 0=OWNER, 1=ADMIN, 2=MEMBER
    join_status TINYINT NOT NULL DEFAULT 0,  -- 0=INVITED, 1=ACCEPTED, 2=REJECTED
    invite_by   INT   NULL,
    join_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_user (user_id)
);
```

- `team.owner_id` 是团队所有者。
- `uk_team_user(team_id, user_id)` 防止重复邀请/加入。

### 4.3 `knowledge_base` / `knowledge_base_member`

```sql
CREATE TABLE knowledge_base (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    visibility  TINYINT      NOT NULL DEFAULT 0,  -- 0=PRIVATE, 1=PUBLIC
    owner_type  TINYINT      NOT NULL DEFAULT 0,  -- 0=USER, 1=TEAM
    owner_id    INT          NOT NULL,
    creator_id  INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_type, owner_id)
);

CREATE TABLE knowledge_base_member (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT     NOT NULL,
    user_id            INT     NOT NULL,
    role               TINYINT NOT NULL DEFAULT 3, -- 0=OWNER, 1=ADMIN, 2=EDITOR, 3=VIEWER
    invite_status      TINYINT NOT NULL DEFAULT 0, -- 0=INVITED, 1=ACCEPTED, 2=REJECTED
    invite_by          INT     NULL,
    join_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb_user (knowledge_base_id, user_id),
    INDEX idx_user (user_id)
);
```

- 知识库可归属个人（`owner_type=0`）或团队（`owner_type=1`）。
- `idx_owner(owner_type, owner_id)` 支持按归属列出知识库。

### 4.4 `article`

```sql
CREATE TABLE article (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT          NOT NULL,
    title              VARCHAR(200) NOT NULL,
    summary            VARCHAR(500) NULL,
    content            LONGTEXT     NULL,          -- Markdown
    content_format     VARCHAR(20)  NULL DEFAULT 'markdown',
    author_id          INT          NOT NULL,
    status             TINYINT      NOT NULL DEFAULT 0, -- 0=DRAFT, 1=PUBLISHED
    cover_image        VARCHAR(500) NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb (knowledge_base_id, update_time),
    INDEX idx_author (author_id)
);
```

- `idx_kb(knowledge_base_id, update_time)` 支持知识库内按更新时间列表。
- `idx_author(author_id)` 支持作者维度查询。
- `content` 使用 LONGTEXT 存 Markdown，可容纳大型文档。

### 4.5 `comment`

```sql
CREATE TABLE comment (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT   NOT NULL,
    user_id     INT   NOT NULL,
    parent_id   INT   NULL     COMMENT '父评论ID, NULL为顶级评论',
    reply_to_id INT   NULL     COMMENT '回复目标评论ID',
    content     TEXT  NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id, create_time),
    INDEX idx_parent (parent_id),
    INDEX idx_user (user_id)
);
```

- `parent_id`：顶级评论为 NULL，子评论指向父评论。
- `reply_to_id`：回复特定评论时指向目标评论（楼中楼）。
- `idx_article(article_id, create_time)` 支持文章评论按时间加载。

### 4.6 `article_like` / `favorite` / `recent_visit`

```sql
CREATE TABLE article_like (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    user_id     INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id),
    INDEX idx_article (article_id)
);

CREATE TABLE favorite (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    target_type TINYINT NOT NULL,  -- 0=文章, 1=知识库
    target_id   INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_user (user_id)
);

CREATE TABLE recent_visit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    article_id  INT NOT NULL,
    visit_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id),
    INDEX idx_user_time (user_id, visit_time)
);
```

- `article_like.uk_article_user` 防重复点赞。
- `favorite.uk_user_target` 支持收藏文章/知识库扩展。
- `recent_visit.uk_user_article` 配合 upsert 更新最近访问时间。

### 4.7 `article_chat_message`

```sql
CREATE TABLE article_chat_message (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT          NOT NULL,
    team_id     INT          NULL,
    sender_id   INT          NOT NULL,
    message_type TINYINT     NOT NULL DEFAULT 0,  -- 0=TEXT, 1=SYSTEM
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_time (article_id, create_time)
);
```

- `article_id` 房间主维度。
- `team_id` 可选，用于团队上下文。

### 4.8 `upload_file`

```sql
CREATE TABLE upload_file (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    biz_type    VARCHAR(50)  NULL COMMENT 'avatar/image/cover',
    biz_id      INT          NULL,
    file_name   VARCHAR(200) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT       NULL,
    uploader_id INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4.9 `notification`

```sql
CREATE TABLE notification (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL COMMENT '接收通知的用户',
    type        TINYINT      NOT NULL COMMENT '0-7 通知类型',
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NULL,
    link        VARCHAR(500) NULL COMMENT '跳转链接',
    sender_id   INT          NULL COMMENT '触发者用户ID',
    is_read     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read, create_time DESC),
    INDEX idx_user_time (user_id, create_time DESC)
);
```

- `idx_user_read` 查询未读列表。
- `idx_user_time` 按时间拉取通知流。

### 4.10 `follow_user`

```sql
CREATE TABLE follow_user (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    follower_id  INT NOT NULL,
    following_id INT NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);
```

## 5. 索引总览

| 表 | 索引 | 类型 | 用途 |
|---|---|---|---|
| `sys_user` | `username` | UNIQUE | 登录查询 |
| `sys_user` | `email` | UNIQUE | 注册/找回密码 |
| `team` | `idx_owner` | INDEX | 按拥有者查团队 |
| `team_member` | `uk_team_user` | UNIQUE | 防重复邀请 |
| `team_member` | `idx_user` | INDEX | 查用户所属团队 |
| `knowledge_base` | `idx_owner` | INDEX | 按归属查知识库 |
| `knowledge_base_member` | `uk_kb_user` | UNIQUE | 防重复授权 |
| `knowledge_base_member` | `idx_user` | INDEX | 查用户所属知识库 |
| `article` | `idx_kb` | INDEX | 知识库内文章列表 |
| `article` | `idx_author` | INDEX | 作者文章列表 |
| `comment` | `idx_article` | INDEX | 文章评论按时间 |
| `comment` | `idx_parent` | INDEX | 楼中楼扩展 |
| `comment` | `idx_user` | INDEX | 用户评论 |
| `article_like` | `uk_article_user` | UNIQUE | 防重复点赞 |
| `favorite` | `uk_user_target` | UNIQUE | 防重复收藏 |
| `recent_visit` | `uk_user_article` | UNIQUE | upsert 更新 |
| `recent_visit` | `idx_user_time` | INDEX | 浏览历史列表 |
| `follow_user` | `uk_follower_following` | UNIQUE | 防重复关注 |
| `article_chat_message` | `idx_article_time` | INDEX | 聊天历史 |
| `notification` | `idx_user_read` | INDEX | 未读列表 |
| `notification` | `idx_user_time` | INDEX | 通知流 |

## 6. 当前约束取舍

当前 schema 主要依赖业务层维护关系，未声明外键。

收益：

- 初始化简单。
- 开发阶段删除/重建成本低。
- 避免外键约束影响快速迭代。

成本：

- 级联删除必须由 Service 保证。
- 脏数据风险更高。
- 数据库无法自动阻止无效引用。

## 7. 推荐演进

### 7.1 外键与级联

生产建议逐步增加外键：

- `article.knowledge_base_id → knowledge_base.id`
- `article.author_id → sys_user.id`
- `comment.article_id → article.id`
- `team_member.team_id → team.id`
- `knowledge_base_member.knowledge_base_id → knowledge_base.id`

对评论、点赞、收藏、历史可使用 `ON DELETE CASCADE` 或由应用事务删除，二选一保持一致。

### 7.2 索引增强

建议补充：

- `article(status, update_time)`：公开列表排序。
- `article(title)` 或全文索引：标题搜索。
- `knowledge_base(visibility, name)`：公开知识库搜索。
- `upload_file(uploader_id, create_time)`：用户上传历史。
- `favorite(target_type, target_id)`：反查收藏数。

### 7.3 内容版本化

协同编辑建议新增：

- `article_version(id, article_id, version, content, editor_id, create_time)`
- `article_yjs_update(id, article_id, update_blob, client_id, create_time)`
- `article_snapshot(id, article_id, snapshot_blob, version, create_time)`

用于历史版本、回滚、协同状态恢复。

### 7.4 审计字段

高价值表可补充：

- `deleted` 软删除标记。
- `delete_time` 删除时间。
- `created_by` / `updated_by` 操作人。
- `version` 乐观锁。
