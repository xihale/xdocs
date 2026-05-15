-- H2-compatible version of schema.sql for DAO integration tests

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
    password    VARCHAR(200) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    nickname    VARCHAR(50)  NULL,
    avatar_url  VARCHAR(500) NULL,
    phone       VARCHAR(20)  NULL,
    role        TINYINT      NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    owner_id    INT          NOT NULL,
    avatar_url  VARCHAR(500) NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE team_member (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    team_id     INT          NOT NULL,
    user_id     INT          NOT NULL,
    role        TINYINT      NOT NULL DEFAULT 2,
    join_status TINYINT      NOT NULL DEFAULT 0,
    invite_by   INT          NULL,
    join_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (team_id, user_id),
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE knowledge_base (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NULL,
    visibility  TINYINT      NOT NULL DEFAULT 0,
    owner_type  TINYINT      NOT NULL DEFAULT 0,
    owner_id    INT          NOT NULL,
    creator_id  INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE knowledge_base_member (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT     NOT NULL,
    user_id            INT     NOT NULL,
    role               TINYINT NOT NULL DEFAULT 3,
    invite_status      TINYINT NOT NULL DEFAULT 0,
    invite_by          INT     NULL,
    join_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (knowledge_base_id, user_id),
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE article (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id  INT          NOT NULL,
    title              VARCHAR(200) NOT NULL,
    summary            VARCHAR(500) NULL,
    content            TEXT         NULL,
    content_format     VARCHAR(20)  NULL DEFAULT 'markdown',
    author_id          INT          NOT NULL,
    status             TINYINT      NOT NULL DEFAULT 0,
    cover_image        VARCHAR(500) NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE article_chat_message (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    article_id   INT      NOT NULL,
    team_id      INT      NULL,
    sender_id    INT      NOT NULL,
    message_type TINYINT  NOT NULL DEFAULT 0,
    content      TEXT     NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE upload_file (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    biz_type    VARCHAR(50)  NULL,
    biz_id      INT          NULL,
    file_name   VARCHAR(200) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT       NULL,
    uploader_id INT          NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploader_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE article_like (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    user_id     INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE comment (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT          NOT NULL,
    user_id     INT          NOT NULL,
    parent_id   INT          NULL,
    reply_to_id INT          NULL,
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE
);

CREATE TABLE follow_user (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    follower_id  INT NOT NULL,
    following_id INT NOT NULL,
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE favorite (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    target_type TINYINT NOT NULL,
    target_id   INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, target_type, target_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE recent_visit (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    article_id  INT NOT NULL,
    visit_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, article_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
);

CREATE TABLE notification (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    type        TINYINT      NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NULL,
    link        VARCHAR(500) NULL,
    sender_id   INT          NULL,
    is_read     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE SET NULL
);
