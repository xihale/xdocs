-- ============================================================
-- 云文档在线管理平台 (xdocs) — 测试数据
-- 用法: mysql -u root -p xdocs < test-data.sql
-- 密码统一为 BCrypt("123456")
-- ============================================================

USE xdocs;

-- ==================== 用户 ====================
-- BCrypt hash for "123456" (cost=10, at.favre.lib:bcrypt)
INSERT INTO sys_user (username, password, email, nickname, role, status) VALUES
('admin',   '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'admin@xdocs.com',   '管理员', 1, 0),
('zhangsan','$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'zhangsan@xdocs.com','张三',   0, 0),
('lisi',    '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'lisi@xdocs.com',    '李四',   0, 0),
('wangwu',  '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'wangwu@xdocs.com',  '王五',   0, 0),
('zhaoliu', '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'zhaoliu@xdocs.com', '赵六',   0, 0);

-- ==================== 团队 ====================
INSERT INTO team (name, description, owner_id) VALUES
('前端学习小组', '一起学习前端技术的团队', 2),
('Java研发组',   'Java后端技术交流',       3);

INSERT INTO team_member (team_id, user_id, role, join_status, invite_by) VALUES
-- 前端学习小组 (id=1): 张三OWNER, 李四MEMBER, 王五MEMBER
(1, 2, 0, 1, NULL),
(1, 3, 2, 1, 2),
(1, 4, 2, 1, 2),
-- Java研发组 (id=2): 李四OWNER, 张三MEMBER, 赵六MEMBER
(2, 3, 0, 1, NULL),
(2, 2, 2, 1, 3),
(2, 5, 2, 1, 3);

-- ==================== 知识库 ====================
INSERT INTO knowledge_base (name, description, visibility, owner_type, owner_id, creator_id) VALUES
-- id=1: 张三的公开知识库
('Java核心技术',    'Java编程语言核心知识整理',     1, 0, 2, 2),
-- id=2: 李四的公开知识库
('前端进阶之路',    '前端框架与工程化实践',         1, 0, 3, 3),
-- id=3: admin的私有知识库
('私人笔记',        '我的私人知识库',               0, 0, 1, 1),
-- id=4: 前端学习小组的团队知识库
('团队知识库',      '前端学习小组共享知识库',       1, 1, 1, 2),
-- id=5: 王四的公开知识库
('数据库设计',      'MySQL数据库设计与优化',        1, 0, 4, 4);

INSERT INTO knowledge_base_member (knowledge_base_id, user_id, role, invite_status, invite_by) VALUES
-- Java核心技术 (kb=1): 张三OWNER, admin EDITOR
(1, 2, 0, 1, NULL),
(1, 1, 2, 1, 2),
-- 前端进阶之路 (kb=2): 李四OWNER
(2, 3, 0, 1, NULL),
-- 私人笔记 (kb=3): admin OWNER
(3, 1, 0, 1, NULL),
-- 团队知识库 (kb=4): 张三OWNER, 李四ADMIN, 王五VIEWER
(4, 2, 0, 1, NULL),
(4, 3, 1, 1, 2),
(4, 4, 3, 1, 2),
-- 数据库设计 (kb=5): 王五OWNER, 赵六EDITOR
(5, 4, 0, 1, NULL),
(5, 5, 2, 1, 4);

-- ==================== 文章 ====================
INSERT INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status) VALUES
-- kb=1 (Java核心技术)
(1, 'Java多线程编程入门',     'Java多线程基础概念与实现方式',           '# Java多线程编程入门\n\n## 1. 线程创建\n\nJava创建线程有两种方式...\n\n## 2. 线程同步\n\nsynchronized关键字...',          'markdown', 2, 1),
(1, 'JVM内存模型详解',        '深入理解JVM内存区域划分与垃圾回收机制', '# JVM内存模型\n\n## 1. 运行时数据区\n\n- 堆\n- 栈\n- 方法区\n- 程序计数器\n\n## 2. GC算法...', 'markdown', 2, 1),
(1, 'Java集合框架源码分析',   'ArrayList、HashMap等核心集合的实现原理', '# Java集合框架\n\n## ArrayList\n\n动态数组实现...\n\n## HashMap\n\n数组+链表+红黑树...', 'markdown', 2, 1),
-- kb=2 (前端进阶之路)
(2, 'React Hooks最佳实践',    'React Hooks的常用模式与性能优化技巧',   '# React Hooks最佳实践\n\n## useState\n\n## useEffect\n\n## useCallback & useMemo...', 'markdown', 3, 1),
(2, 'TypeScript高级类型体操',  '掌握TypeScript的高级类型编程技巧',      '# TypeScript高级类型\n\n## 条件类型\n\n## 映射类型\n\n## 模板字面量类型...', 'markdown', 3, 1),
-- kb=3 (私人笔记)
(3, '个人周报模板',           '每周工作总结模板',                      '# 本周工作\n\n1. 完成了...\n\n# 下周计划\n\n1. 计划...', 'markdown', 1, 0),
-- kb=4 (团队知识库)
(4, '团队开发规范',           '代码规范与Git工作流',                   '# 开发规范\n\n## 代码风格\n\n## Git工作流\n\n## Code Review流程...', 'markdown', 2, 1),
-- kb=5 (数据库设计)
(5, 'MySQL索引优化实战',      '常见索引类型与查询优化策略',             '# MySQL索引优化\n\n## B+树索引\n\n## 覆盖索引\n\n## EXPLAIN使用...', 'markdown', 4, 1),
(5, '数据库范式与反范式设计', '理解数据库设计中的范式理论',             '# 数据库范式\n\n## 第一范式\n\n## 第二范式\n\n## 第三范式\n\n## 反范式设计...', 'markdown', 4, 1);

-- ==================== 点赞 ====================
INSERT INTO article_like (article_id, user_id) VALUES
-- 文章1 (Java多线程): admin, 李四, 王五, 赵六
(1, 1), (1, 3), (1, 4), (1, 5),
-- 文章2 (JVM): admin, 李四
(2, 1), (2, 3),
-- 文章4 (React Hooks): admin, 张三, 王五, 赵六
(4, 1), (4, 2), (4, 4), (4, 5),
-- 文章7 (团队开发规范): 李四, 王五
(7, 2), (7, 3),
-- 文章8 (MySQL索引): admin, 张三, 李四, 赵六
(8, 1), (8, 2), (8, 3), (8, 5);

-- ==================== 评论 ====================
INSERT INTO comment (article_id, user_id, parent_id, reply_to_id, content) VALUES
-- 文章1 (Java多线程) 的评论
(1, 1, NULL, NULL, '写得非常好，多线程一直是我比较薄弱的地方，学到了很多！'),
(1, 3, 1, 1,      '谢谢分享！建议可以补充一下线程池的内容'),
(1, 2, 1, 1,      '感谢建议，后续会补充线程池相关的文章'),
(1, 4, NULL, NULL, '请问synchronized和ReentrantLock有什么区别？'),
-- 文章4 (React Hooks) 的评论
(4, 2, NULL, NULL, 'Hooks确实比class组件好用多了，尤其是useEffect'),
(4, 5, 5, 2,      '同意！自定义Hook的复用性也更好'),
-- 文章8 (MySQL索引) 的评论
(8, 1, NULL, NULL, '索引优化讲得很清楚，实战案例很有参考价值'),
(8, 3, NULL, NULL, '建议加一下EXPLAIN的使用说明');

-- ==================== 关注 ====================
INSERT INTO follow_user (follower_id, following_id) VALUES
-- admin 关注: 张三, 李四
(1, 2), (1, 3),
-- 张三 关注: 李四, 王五
(2, 3), (2, 4),
-- 李四 关注: admin, 王五
(3, 1), (3, 4),
-- 王五 关注: admin, 张三
(4, 1), (4, 2),
-- 赵六 关注: admin, 张三, 李四
(5, 1), (5, 2), (5, 3);

-- ==================== 收藏 ====================
INSERT INTO favorite (user_id, target_type, target_id) VALUES
-- admin 收藏文章: 1, 4, 8
(1, 0, 1), (1, 0, 4), (1, 0, 8),
-- 张三 收藏文章: 4, 8
(2, 0, 4), (2, 0, 8),
-- 李四 收藏文章: 1, 2
(3, 0, 1), (3, 0, 2);

-- ==================== 浏览记录 ====================
INSERT INTO recent_visit (user_id, article_id) VALUES
(1, 1), (1, 4), (1, 8),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 8);

-- ==================== 聊天消息 ====================
INSERT INTO article_chat_message (article_id, team_id, sender_id, message_type, content) VALUES
-- 团队知识库文章7的讨论 (前端学习小组)
(7, 1, 2, 0, '大家看看这个规范有没有需要补充的？'),
(7, 1, 3, 0, '建议加上错误处理规范'),
(7, 1, 4, 0, 'Git分支策略部分写得很清楚'),
-- Java核心技术文章1的讨论 (Java研发组)
(1, 2, 3, 0, '多线程这块内容很全'),
(1, 2, 2, 0, '后续会补充线程池和并发工具类');

-- ==================== 通知 ====================
INSERT INTO notification (user_id, type, title, content, link, sender_id, is_read) VALUES
-- admin 的通知
(1, 3, '张三评论了你的文章', '评论内容：写得非常好，学到了很多！', '/article/1', 2, 1),
(1, 4, '张三点赞了你的文章', '文章《Java多线程编程入门》获得点赞', '/article/1', 2, 0),
-- 张三 的通知
(2, 0, '邀请你加入Java研发组', '李四邀请你加入Java研发组', '/team/2', 3, 1),
(2, 6, '王五关注了你', '', '/user/4', 4, 0),
-- 李四 的通知
(3, 1, '邀请你编辑知识库', '张三邀请你编辑《Java核心技术》', '/kb/1', 2, 1),
(3, 4, 'admin点赞了你的文章', '文章《React Hooks最佳实践》获得点赞', '/article/4', 1, 0);
