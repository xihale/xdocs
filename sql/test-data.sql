-- ============================================================
-- 云文档在线管理平台 - 测试数据
-- ============================================================

USE clouddoc;

-- 密码统一为 bcrypt 加密的 "123456"
-- bcrypt hash for "123456": $2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq
-- 使用 at.favre.lib:bcrypt 生成

-- ==================== 用户 ====================
INSERT INTO sys_user (username, password, email, nickname, role, status) VALUES
('admin',   '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'admin@clouddoc.com',   '管理员',   1, 0),
('zhangsan','$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'zhangsan@clouddoc.com','张三',     0, 0),
('lisi',    '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'lisi@clouddoc.com',    '李四',     0, 0),
('wangwu',  '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'wangwu@clouddoc.com',  '王五',     0, 0),
('zhaoliu', '$2a$10$bALbGIwK4ay0EBQymF6VS.FMn1Eqe.QFPCDoEnjpTGdnoPqGYiGiq', 'zhaoliu@clouddoc.com', '赵六',     0, 0);

-- ==================== 团队 ====================
INSERT INTO team (name, description, owner_id) VALUES
('前端学习小组', '一起学习前端技术的团队', 1),
('Java研发组',   'Java后端技术交流',       2);

INSERT INTO team_member (team_id, user_id, role, join_status, invite_by) VALUES
(1, 1, 0, 1, NULL),
(1, 2, 2, 1, 1),
(1, 3, 2, 1, 1),
(2, 2, 0, 1, NULL),
(2, 4, 2, 1, 2);

-- ==================== 知识库 ====================
INSERT INTO knowledge_base (name, description, visibility, owner_type, owner_id, creator_id) VALUES
('Java核心技术',    'Java编程语言核心知识整理',     1, 0, 2, 2),
('前端进阶之路',    '前端框架与工程化实践',         1, 0, 3, 3),
('私人笔记',        '我的私人知识库',               0, 0, 1, 1),
('团队知识库',      '团队共享知识库',               1, 1, 1, 1),
('数据库设计',      'MySQL数据库设计与优化',        1, 0, 4, 4);

INSERT INTO knowledge_base_member (knowledge_base_id, user_id, role, invite_status, invite_by) VALUES
(1, 2, 0, 1, NULL),
(1, 1, 2, 1, 2),
(2, 3, 0, 1, NULL),
(3, 1, 0, 1, NULL),
(4, 1, 0, 1, NULL),
(4, 2, 1, 1, 1),
(5, 4, 0, 1, NULL),
(5, 5, 2, 1, 4);

-- ==================== 文章 ====================
INSERT INTO article (knowledge_base_id, title, summary, content, content_format, author_id, status) VALUES
(1, 'Java多线程编程入门',          'Java多线程基础概念与实现方式',            '<h2>Java多线程编程入门</h2><p>Java提供了丰富的多线程支持...</p>',     'html', 2, 1),
(1, 'JVM内存模型详解',             '深入理解JVM内存区域划分与垃圾回收机制',  '<h2>JVM内存模型</h2><p> JVM将内存划分为多个区域...</p>',              'html', 2, 1),
(1, 'Java集合框架源码分析',        'ArrayList、HashMap等核心集合的实现原理', '<h2>集合框架</h2><p>Java集合框架是日常开发中最常用的API之一...</p>','html', 2, 1),
(2, 'React Hooks最佳实践',         'React Hooks的常用模式与性能优化技巧',    '<h2>React Hooks</h2><p>Hooks是React 16.8引入的新特性...</p>',       'html', 3, 1),
(2, 'TypeScript高级类型体操',       '掌握TypeScript的高级类型编程技巧',       '<h2>TypeScript高级类型</h2><p>TypeScript的类型系统非常强大...</p>',   'html', 3, 1),
(3, '个人周报模板',                 '每周工作总结模板',                       '<h2>本周工作</h2><p>1. 完成了...</p>',                                'html', 1, 0),
(4, '团队开发规范',                 '代码规范与Git工作流',                    '<h2>开发规范</h2><p>统一的代码风格是团队协作的基础...</p>',           'html', 1, 1),
(5, 'MySQL索引优化实战',            '常见索引类型与查询优化策略',              '<h2>MySQL索引</h2><p>索引是提升查询性能的关键手段...</p>',           'html', 4, 1),
(5, '数据库范式与反范式设计',       '理解数据库设计中的范式理论',              '<h2>数据库范式</h2><p>范式是关系型数据库设计的理论基础...</p>',       'html', 4, 1);

-- ==================== 点赞 ====================
INSERT INTO article_like (article_id, user_id) VALUES
(1, 1), (1, 3), (1, 4), (1, 5),
(2, 1), (2, 3),
(4, 1), (4, 2), (4, 4), (4, 5),
(7, 2), (7, 3),
(8, 1), (8, 2), (8, 3), (8, 5);

-- ==================== 评论 ====================
INSERT INTO comment (article_id, user_id, parent_id, reply_to_id, content) VALUES
-- 文章1的评论
(1, 1, NULL, NULL, '写得非常好，多线程一直是我比较薄弱的地方，学到了很多！'),
(1, 3, 1, 1,      '谢谢分享！建议可以补充一下线程池的内容'),
(1, 2, 1, 1,      '感谢建议，后续会补充线程池相关的文章'),
(1, 4, NULL, NULL, '请问synchronized和ReentrantLock有什么区别？'),
-- 文章4的评论
(4, 2, NULL, NULL, 'Hooks确实比class组件好用多了，尤其是useEffect'),
(4, 5, 5, 2,      '同意！自定义Hook的复用性也更好'),
-- 文章8的评论
(8, 1, NULL, NULL, '索引优化讲得很清楚，实战案例很有参考价值'),
(8, 3, NULL, NULL, '建议加一下EXPLAIN的使用说明');

-- ==================== 关注 ====================
INSERT INTO follow_user (follower_id, following_id) VALUES
(1, 2), (1, 3),
(2, 3), (2, 4),
(3, 1), (3, 4),
(4, 1), (4, 2),
(5, 1), (5, 2), (5, 3);

-- ==================== 收藏 ====================
INSERT INTO favorite (user_id, target_type, target_id) VALUES
(1, 0, 1), (1, 0, 4), (1, 0, 8),
(2, 0, 4), (2, 0, 8),
(3, 0, 1), (3, 0, 2);

-- ==================== 浏览记录 ====================
INSERT INTO recent_visit (user_id, article_id) VALUES
(1, 1), (1, 4), (1, 8),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 8);
