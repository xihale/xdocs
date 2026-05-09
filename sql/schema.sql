-- ============================================================
-- 云文档在线管理平台 - 数据库建表脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS clouddoc
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE clouddoc;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
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
-- 2. TEAM 表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS team (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    owner_id    INT          NOT NULL,
    avatar_url  VARCHAR(500) NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB COMMENT='TEAM 表';

-- -----------------------------------------------------------
-- 3. TEAM 成员关系表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS team_member (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    team_id     INT          NOT NULL,
    user_id     INT          NOT NULL,
    role        TINYINT      NOT NULL DEFAULT 2 COMMENT '0=OWNER, 1=ADMIN, 2=MEMBER',
    join_status TINYINT      NOT NULL DEFAULT 0 COMMENT '0=INVITED, 1=ACCEPTED, 2=REJECTED',
    invite_by   INT          NULL,
    join_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='TEAM 成员关系表';

-- -----------------------------------------------------------
-- 4. 知识库表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_base (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    visibility  TINYINT      NOT NULL DEFAULT 0 COMMENT '0=PRIVATE, 1=PUBLIC',
    owner_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '0=USER, 1=TEAM',
    owner_id    INT          NOT NULL,
    creator_id  INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_type, owner_id)
) ENGINE=InnoDB COMMENT='知识库表';

-- -----------------------------------------------------------
-- 5. 知识库成员关系表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_base_member (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT     NOT NULL,
    user_id            INT     NOT NULL,
    role               TINYINT NOT NULL DEFAULT 3 COMMENT '0=OWNER, 1=ADMIN, 2=EDITOR, 3=VIEWER',
    invite_status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=INVITED, 1=ACCEPTED, 2=REJECTED',
    invite_by          INT     NULL,
    join_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb_user (knowledge_base_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='知识库成员关系表';

-- -----------------------------------------------------------
-- 6. 文章表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS article (
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
    INDEX idx_author (author_id)
) ENGINE=InnoDB COMMENT='文章表';

-- -----------------------------------------------------------
-- 7. 文档聊天消息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_chat_message (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    article_id   INT      NOT NULL,
    team_id      INT      NULL,
    sender_id    INT      NOT NULL,
    message_type TINYINT  NOT NULL DEFAULT 0 COMMENT '0=TEXT, 1=SYSTEM',
    content      TEXT     NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_time (article_id, create_time)
) ENGINE=InnoDB COMMENT='文档聊天消息表';

-- -----------------------------------------------------------
-- 8. 上传文件表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS upload_file (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    biz_type    VARCHAR(50)  NULL COMMENT '业务类型: avatar/image/cover',
    biz_id      INT          NULL,
    file_name   VARCHAR(200) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT       NULL,
    uploader_id INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='上传文件表';

-- -----------------------------------------------------------
-- 9. 文章点赞表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_like (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    user_id     INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id),
    INDEX idx_article (article_id)
) ENGINE=InnoDB COMMENT='文章点赞表';

-- -----------------------------------------------------------
-- 10. 评论表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS comment (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT          NOT NULL,
    user_id     INT          NOT NULL,
    parent_id   INT          NULL     COMMENT '父评论ID, NULL为顶级评论',
    reply_to_id INT          NULL     COMMENT '回复目标评论ID',
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id, create_time),
    INDEX idx_parent (parent_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='评论表';

-- -----------------------------------------------------------
-- 11. 关注表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS follow_user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    follower_id INT NOT NULL COMMENT '关注者',
    following_id INT NOT NULL COMMENT '被关注者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
) ENGINE=InnoDB COMMENT='用户关注表';

-- -----------------------------------------------------------
-- 12. 收藏表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorite (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    target_type TINYINT NOT NULL COMMENT '0=文章, 1=知识库',
    target_id   INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='收藏表';

-- -----------------------------------------------------------
-- 13. 浏览记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS recent_visit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    article_id  INT NOT NULL,
    visit_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id),
    INDEX idx_user_time (user_id, visit_time)
) ENGINE=InnoDB COMMENT='浏览记录表';

-- -----------------------------------------------------------
-- 14. 通知表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
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
    INDEX idx_user_time (user_id, create_time DESC)
) ENGINE=InnoDB COMMENT='通知表';
