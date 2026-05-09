-- ============================================================
-- 云文档在线管理平台 - 测试数据
-- ============================================================

USE xdocs;

-- 用户 (密码均为 123456 的 BCrypt hash, cost=12)
INSERT INTO sys_user (username, password, email, nickname, avatar_url, role, status) VALUES
('admin',  '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'admin@example.com',  '管理员',   NULL, 1, 0),
('zhangsan', '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'zhangsan@example.com', '张三', NULL, 0, 0),
('lisi',     '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'lisi@example.com',     '李四', NULL, 0, 0),
('wangwu',   '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'wangwu@example.com',   '王五', NULL, 0, 0);

-- TEAM
INSERT INTO team (name, description, owner_id) VALUES
('前端开发组', '前端技术交流与协作', 2),
('Java学习小组', 'Java后端学习交流', 3);

-- TEAM 成员
INSERT INTO team_member (team_id, user_id, role, join_status, invite_by) VALUES
(1, 2, 0, 1, NULL),  -- 张三是前端组 OWNER
(1, 3, 2, 1, 2),     -- 李四是前端组 MEMBER
(1, 4, 1, 1, 2),     -- 王五是前端组 ADMIN
(2, 3, 0, 1, NULL),  -- 李四是Java组 OWNER
(2, 2, 2, 1, 3);     -- 张三是Java组 MEMBER

-- 知识库
INSERT INTO knowledge_base (name, description, visibility, owner_type, owner_id, creator_id) VALUES
('个人笔记', '张三的个人知识库', 0, 0, 2, 2),
('前端技术文档', '前端开发组共享知识库', 1, 1, 1, 2),
('Java学习笔记', 'Java学习小组共享知识库', 1, 1, 2, 3);

-- 知识库成员
INSERT INTO knowledge_base_member (knowledge_base_id, user_id, role, invite_status, invite_by) VALUES
(1, 2, 0, 1, NULL),  -- 张三是自己知识库的 OWNER
(2, 2, 0, 1, NULL),  -- 张三是前端知识库的 OWNER
(2, 3, 2, 1, 2),     -- 李四是前端知识库的 EDITOR
(2, 4, 3, 1, 2),     -- 王五是前端知识库的 VIEWER
(3, 3, 0, 1, NULL),  -- 李三是Java知识库的 OWNER
(3, 2, 2, 1, 3);     -- 张三是Java知识库的 EDITOR

-- 文章
INSERT INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status) VALUES
(1, 'Vue3 学习笔记', 'Vue3 Composition API 基础', '# Vue3 学习笔记\n\n## Composition API\n\n...', 'markdown', 2, 1),
(2, 'React Hooks 入门', 'React Hooks 基础教程', '# React Hooks 入门\n\n## useState\n\n...', 'markdown', 2, 1),
(2, 'TypeScript 泛型', 'TypeScript 泛型详解', '# TypeScript 泛型\n\n## 基本用法\n\n...', 'markdown', 3, 1),
(3, 'Java 并发编程', 'Java 多线程基础', '# Java 并发编程\n\n## Thread\n\n...', 'markdown', 3, 0),
(3, 'Spring 基础', 'Spring 框架入门', '# Spring 基础\n\n## IoC\n\n...', 'markdown', 2, 1);

-- 聊天消息
INSERT INTO article_chat_message (article_id, team_id, sender_id, message_type, content) VALUES
(2, 1, 2, 0, '这篇文章写得很不错！'),
(2, 1, 3, 0, '谢谢！还在持续更新中'),
(2, 1, 4, 0, '可以加一些实际案例吗？'),
(4, 2, 3, 0, 'Java并发这块内容很全'),
(4, 2, 2, 0, '是的，后续会补充更多示例');
