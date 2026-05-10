-- ============================================================
-- 云文档在线管理平台 (xdocs) — 数据库建表脚本
-- 用法: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS xdocs
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE xdocs;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS recent_visit;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS follow_user;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS article_like;
DROP TABLE IF EXISTS article_chat_message;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS knowledge_base_member;
DROP TABLE IF EXISTS knowledge_base;
DROP TABLE IF EXISTS team_member;
DROP TABLE IF EXISTS team;
DROP TABLE IF EXISTS upload_file;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL COMMENT 'BCrypt hash',
    email       VARCHAR(100) NOT NULL UNIQUE,
    nickname    VARCHAR(50)  NULL,
    avatar_url  VARCHAR(500) NULL,
    phone       VARCHAR(20)  NULL,
    role        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=USER, 1=ADMIN',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=NORMAL, 1=BANNED',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 团队表
-- -----------------------------------------------------------
CREATE TABLE team (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    owner_id    INT          NOT NULL,
    avatar_url  VARCHAR(500) NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id),
    CONSTRAINT fk_team_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='团队表';

-- -----------------------------------------------------------
-- 3. 团队成员关系表
-- -----------------------------------------------------------
CREATE TABLE team_member (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    team_id     INT          NOT NULL,
    user_id     INT          NOT NULL,
    role        TINYINT      NOT NULL DEFAULT 2 COMMENT '0=OWNER, 1=ADMIN, 2=MEMBER',
    join_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0=INVITED, 1=ACCEPTED, 2=REJECTED',
    invite_by   INT          NULL,
    join_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_user (user_id),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='团队成员关系表';

-- -----------------------------------------------------------
-- 4. 知识库表
-- -----------------------------------------------------------
CREATE TABLE knowledge_base (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    visibility  TINYINT      NOT NULL DEFAULT 0 COMMENT '0=PRIVATE, 1=PUBLIC',
    owner_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '0=USER, 1=TEAM',
    owner_id    INT          NOT NULL,
    creator_id  INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_type, owner_id),
    CONSTRAINT fk_kb_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识库表';

-- -----------------------------------------------------------
-- 5. 知识库成员关系表
-- -----------------------------------------------------------
CREATE TABLE knowledge_base_member (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT     NOT NULL,
    user_id            INT     NOT NULL,
    role               TINYINT NOT NULL DEFAULT 3 COMMENT '0=OWNER, 1=ADMIN, 2=EDITOR, 3=VIEWER',
    invite_status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=INVITED, 1=ACCEPTED, 2=REJECTED',
    invite_by          INT     NULL,
    join_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb_user (knowledge_base_id, user_id),
    INDEX idx_user (user_id),
    CONSTRAINT fb_kb_member_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fb_kb_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='知识库成员关系表';

-- -----------------------------------------------------------
-- 6. 文章表
-- -----------------------------------------------------------
CREATE TABLE article (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT          NOT NULL,
    title              VARCHAR(200) NOT NULL,
    summary            VARCHAR(500) NULL,
    content            LONGTEXT     NULL,
    content_format     VARCHAR(20)  NULL DEFAULT 'markdown',
    author_id          INT          NOT NULL,
    status             TINYINT      NOT NULL DEFAULT 0 COMMENT '0=DRAFT, 1=PUBLISHED',
    cover_image        VARCHAR(500) NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb (knowledge_base_id, update_time),
    INDEX idx_author (author_id),
    CONSTRAINT fk_article_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='文章表';

-- -----------------------------------------------------------
-- 7. 文档聊天消息表
-- -----------------------------------------------------------
CREATE TABLE article_chat_message (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    article_id   INT      NOT NULL,
    team_id      INT      NULL,
    sender_id    INT      NOT NULL,
    message_type TINYINT  NOT NULL DEFAULT 0 COMMENT '0=TEXT, 1=SYSTEM',
    content      TEXT     NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_time (article_id, create_time),
    CONSTRAINT fk_chat_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender  FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='文档聊天消息表';

-- -----------------------------------------------------------
-- 8. 上传文件表
-- -----------------------------------------------------------
CREATE TABLE upload_file (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    biz_type    VARCHAR(50)  NULL COMMENT '业务类型: avatar/image/cover',
    biz_id      INT          NULL,
    file_name   VARCHAR(200) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT       NULL,
    uploader_id INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='上传文件表';

-- -----------------------------------------------------------
-- 9. 文章点赞表
-- -----------------------------------------------------------
CREATE TABLE article_like (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    user_id     INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id),
    INDEX idx_article (article_id),
    CONSTRAINT fk_like_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='文章点赞表';

-- -----------------------------------------------------------
-- 10. 评论表
-- -----------------------------------------------------------
CREATE TABLE comment (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT          NOT NULL,
    user_id     INT          NOT NULL,
    parent_id   INT          NULL     COMMENT '父评论ID, NULL为顶级评论',
    reply_to_id INT          NULL     COMMENT '回复目标评论ID',
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id, create_time),
    INDEX idx_parent (parent_id),
    INDEX idx_user (user_id),
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent  FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='评论表';

-- -----------------------------------------------------------
-- 11. 关注表
-- -----------------------------------------------------------
CREATE TABLE follow_user (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    follower_id  INT NOT NULL COMMENT '关注者',
    following_id INT NOT NULL COMMENT '被关注者',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id),
    CONSTRAINT fk_follow_follower  FOREIGN KEY (follower_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_following FOREIGN KEY (following_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户关注表';

-- -----------------------------------------------------------
-- 12. 收藏表
-- -----------------------------------------------------------
CREATE TABLE favorite (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    target_type TINYINT NOT NULL COMMENT '0=文章, 1=知识库',
    target_id   INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_user (user_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='收藏表';

-- -----------------------------------------------------------
-- 13. 浏览记录表
-- -----------------------------------------------------------
CREATE TABLE recent_visit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    article_id  INT NOT NULL,
    visit_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id),
    INDEX idx_user_time (user_id, visit_time),
    CONSTRAINT fk_visit_user    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_article FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='浏览记录表';

-- -----------------------------------------------------------
-- 14. 通知表
-- -----------------------------------------------------------
CREATE TABLE notification (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL COMMENT '接收通知的用户',
    type        TINYINT      NOT NULL COMMENT '0=团队邀请, 1=知识库邀请, 2=团队新文章, 3=评论, 4=点赞, 5=成员变动, 6=关注',
    title       VARCHAR(200) NOT NULL COMMENT '通知标题',
    content     TEXT         NULL COMMENT '通知内容',
    link        VARCHAR(500) NULL COMMENT '跳转链接',
    sender_id   INT          NULL COMMENT '触发者用户ID',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未读, 1=已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read, create_time DESC),
    INDEX idx_user_time (user_id, create_time DESC),
    CONSTRAINT fk_notif_user   FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='通知表';
