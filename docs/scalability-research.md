# CloudDoc 可拓展性研究报告

## 1. 当前基线

CloudDoc 当前是单体 Web 应用：React SPA + Java Servlet WAR + MariaDB + WebSocket。

### 1.1 部署拓扑

```text
┌─────────────────────────────────────────────────┐
│  Browser                                        │
│  React SPA (Vite build)                         │
│  ├─ HTTP JSON → /api/*                          │
│  ├─ WebSocket → /api/chat/ws/{articleId}        │
│  ├─ WebSocket → /api/collaboration/{docId}      │
│  └─ WebSocket → /api/notification/ws            │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│  Jetty 12 (single instance)                     │
│  ├─ ExceptionFilter → CorsFilter → CsrfFilter   │
│  │  → AuthFilter → Servlet                      │
│  ├─ WebSocket endpoints (3)                     │
│  ├─ RoomManager (in-process singleton)          │
│  ├─ ConnectionPool (in-process singleton)       │
│  └─ File storage (local disk)                   │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│  MariaDB (single instance)                      │
│  14 tables, InnoDB, utf8mb4                     │
└─────────────────────────────────────────────────┘
```

### 1.2 优势

- 部署链路短，单进程包含所有逻辑。
- 调试简单，无分布式追踪需求。
- 跨模块调用无网络开销。
- 数据一致性容易理解（单库事务）。

### 1.3 限制

- HTTP、WebSocket、协同状态都在同一进程，无法独立扩缩容。
- `RoomManager` 基于 `ConcurrentHashMap`，多节点下 WebSocket 房间状态不共享。
- 数据库承担所有搜索、列表、互动计数压力。
- 上传文件落本地磁盘，多实例无法共享。

### 1.4 容量估算

基于当前架构的粗略容量上限（单节点）：

| 维度 | 估算值 | 依据 |
|---|---|---|
| 并发 HTTP QPS | 500-1000 | Servlet 同步模型，JDBC 阻塞 |
| WebSocket 连接数 | 2000-5000 | Jetty 默认配置 + JVM 内存 |
| 协同房间数 | 100-500 | 每房间维护 Session 集合 + Awareness |
| 数据库连接 | 20 | ConnectionPool maxTotal 默认值 |
| 文件存储 | 单机磁盘上限 | 无分布式存储 |

## 2. 性能瓶颈预测

| 阶段 | 并发量级 | 主要瓶颈 | 表现 | 优先方案 |
|---|---|---|---|---|
| 低并发 | <50 QPS | SQL 查询和 N+1 VO 构造 | 页面加载慢 | 加索引、批量查询、缓存计数 |
| 中并发 | 50-200 QPS | WebSocket 房间内广播 | 编辑延迟、消息积压 | 房间分片、背压、限流 |
| 中高并发 | 200-500 QPS | 数据库连接池 | 获取连接超时 | HikariCP、读写分离 |
| 高并发 | >500 QPS | 单节点内存状态 | 多实例不一致 | Redis Pub/Sub、状态外置 |
| 大数据量 | >10 万文章 | LIKE 搜索 | 搜索慢 | OpenSearch/Meilisearch |
| 大文件量 | >100 GB | 本地上传目录 | 磁盘压力、无法扩容 | 对象存储 + CDN |

## 3. 横向扩展路线

### 阶段 1：强化单体（当前 → 50 QPS）

目标：在不改变架构的前提下提升单节点吞吐。

#### 3.1.1 连接池升级

当前自研 `ConnectionPool`（`ConnectionPool.java`）基于 `BlockingQueue` + JDK 动态代理：

```text
getConnection()
  1. idleConnections.poll()           → 命中空闲连接
  2. activeCount < maxTotal (20)       → CAS + 新建连接
  3. idleConnections.poll(5000ms)      → 阻塞等待
```

迁移到 HikariCP 的收益：

| 指标 | 自研 | HikariCP |
|---|---|---|
| 连接获取 | BlockingQueue + CAS | ConcurrentBag (无锁) |
| 泄漏检测 | 无 | leakDetectionThreshold |
| 连接池预热 | minIdle=2 | initializationFailTimeout |
| 指标暴露 | getStatus() | Micrometer Prometheus |

#### 3.1.2 SQL 优化

当前热点查询：

```java
// ArticleService — 公开文章列表 (N+1 问题)
articles.stream().map(a -> {
    vo.setLikeCount(ArticleService.countLikes(a.getId())); // 每篇文章一次 COUNT
    if (userId != null) {
        vo.setLiked(ArticleService.isLiked(a.getId(), userId)); // 每篇文章一次 SELECT
    }
    return vo;
}).toList();
```

优化方案：

```sql
-- 批量查点赞数
SELECT article_id, COUNT(*) AS cnt FROM article_like
WHERE article_id IN (?, ?, ...) GROUP BY article_id;

-- 批量查当前用户是否点赞
SELECT article_id FROM article_like
WHERE user_id = ? AND article_id IN (?, ?, ...);
```

#### 3.1.3 其他优化

- 对公开文章列表、点赞数、收藏数做缓存（Guava/Caffeine 60s TTL）。
- 上传迁移到对象存储（MinIO/S3/OSS）。
- 增加结构化日志和 `requestId`（MDC）。
- 补充慢 SQL 日志（`long_query_time = 0.5`）。

### 阶段 2：多实例部署（50 → 200 QPS）

```text
┌──────────────┐
│   Nginx LB   │  sticky session for WebSocket
└──────┬───────┘
       │
  ┌────┼────┐
  │    │    │
┌─▼─┐┌─▼─┐┌─▼─┐
│ J1 ││ J2 ││ J3 │   Jetty instances (stateless HTTP)
└─┬─┘└─┬─┘└─┬─┘
  │    │    │
  └────┼────┘
       │
  ┌────▼────┐
  │  MariaDB │  主从复制 (read replica)
  └─────────┘
  ┌─────────┐
  │  Redis   │  缓存 + Pub/Sub
  └─────────┘
  ┌─────────┐
  │  MinIO   │  对象存储
  └─────────┘
```

关键改造：

- **HTTP 无状态化**：JWT 已满足，无 session 依赖。
- **上传文件外置**：S3/MinIO/OSS。
- **WebSocket sticky session**：Nginx `ip_hash` 或 `cookie` 路由，保证同一房间落到同节点。
- **Redis 缓存**：用户信息、权限摘要、热点文章、点赞计数。

### 阶段 3：实时能力外置（200 → 1000 QPS）

WebSocket 协同服务从主应用拆出：

```text
React Editor
  → Collab Gateway (Node.js / Java)
  → Redis / NATS / Kafka
  → Yjs state service
  → Snapshot storage (DB / S3)
```

收益：

- 主后端专注业务 API，WebSocket 不争抢 Servlet 线程。
- 协同服务可独立扩容。
- 房间状态可跨节点共享（通过 Redis Pub/Sub）。
- 协同快照和 update log 可持久化。

### 阶段 4：搜索与推荐拆分（>1000 QPS）

- 用 OpenSearch/Meilisearch 承载文章、知识库、用户搜索。
- 数据库 binlog 或业务事件同步索引。
- Dashboard 可引入推荐排序：热度、时间、关注关系、协作关系。

## 4. 数据演进方向

### 4.1 版本历史

新增文章版本表，实现：

- 自动保存快照。
- 历史对比（diff）。
- 一键回滚。
- 作者贡献追踪。

建议 schema：

```sql
CREATE TABLE article_version (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    version     INT NOT NULL,
    content     LONGTEXT,
    summary     VARCHAR(500),
    editor_id   INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_version (article_id, version),
    INDEX idx_article_time (article_id, create_time DESC)
);
```

### 4.2 权限快照

当前权限实时查成员表。大规模下可维护权限摘要：

```sql
CREATE TABLE user_resource_permission (
    user_id         INT NOT NULL,
    resource_type   TINYINT NOT NULL,  -- 0=KB, 1=TEAM
    resource_id     INT NOT NULL,
    permission_mask INT NOT NULL,      -- 位图：read=1, write=2, admin=4, owner=8
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, resource_type, resource_id)
);
```

成员变动时异步刷新。风险是权限缓存一致性，需事件驱动和失效策略。

### 4.3 软删除与审计

增加软删除与审计日志：

```sql
ALTER TABLE article ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE article ADD COLUMN deleted_at DATETIME NULL;

CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    action      VARCHAR(50) NOT NULL,   -- CREATE/UPDATE/DELETE/PUBLISH
    resource    VARCHAR(50) NOT NULL,   -- ARTICLE/KB/TEAM/USER
    resource_id INT NOT NULL,
    detail      TEXT NULL,              -- JSON diff
    ip          VARCHAR(45) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resource (resource, resource_id),
    INDEX idx_user_time (user_id, create_time DESC)
);
```

## 5. 协同编辑专项研究

### 5.1 当前方案

客户端 Yjs 负责 CRDT 合并；Java WebSocket 主要转发消息。

```text
用户 A 编辑 → Yjs update → WS → CollaborationWebSocket → 广播给房间内其他 Session
                                                                    ↓
用户 B 收到 ← Yjs CRDT merge ← WS ← CollaborationWebSocket ← 转发
```

适用场景：

- 小团队协作（<10 人同时编辑）。
- 单节点部署。
- 显式保存为主要持久化方式。

### 5.2 推荐方案：Update Log + Snapshot

长期推荐使用 update log + snapshot：

```text
┌──────────────────────────────────────────────────────┐
│  Yjs Update Pipeline                                 │
│                                                      │
│  Client update → WS → Server                         │
│    ├─ 1. 持久化到 article_yjs_update (append only)   │
│    ├─ 2. 广播给房间内其他用户                          │
│    └─ 3. 后台定时任务:                                │
│         merge updates → snapshot → article_snapshot   │
│                                                      │
│  新用户加入:                                          │
│    1. 加载最新 snapshot                               │
│    2. replay snapshot 之后的 updates                  │
│    3. 加入房间开始实时同步                             │
└──────────────────────────────────────────────────────┘
```

建议 schema：

```sql
CREATE TABLE article_yjs_update (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT NOT NULL,
    update_blob BLOB NOT NULL,
    client_id   INT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_article_time (article_id, create_time)
);

CREATE TABLE article_snapshot (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id     INT NOT NULL,
    snapshot_blob  BLOB NOT NULL,
    version        INT NOT NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_version (article_id, version DESC)
);
```

### 5.3 服务拆分选项

| 方案 | 优点 | 缺点 | 推荐场景 |
|---|---|---|---|
| Java 内实现 Yjs 协议 | 技术栈统一，运维简单 | CRDT 库成熟度需验证 | 团队只有 Java 技术栈 |
| Node y-websocket 服务 | 生态成熟，y-websocket 开箱即用 | 增加运行时和服务治理 | 追求快速落地 |
| Hocuspocus | 扩展能力强，适合生产，内置持久化 | 引入新框架，需学习成本 | 中大型项目 |

### 5.4 协同编辑容量估算

| 场景 | 每房间并发 | 消息频率 | 带宽估算 |
|---|---|---|---|
| 2 人编辑 | 2 | ~10 msg/s | ~50 KB/s |
| 5 人编辑 | 5 | ~25 msg/s | ~125 KB/s |
| 10 人编辑 | 10 | ~50 msg/s | ~250 KB/s |

单节点 100 个活跃房间（平均 3 人）≈ 750 KB/s 带宽，可接受。

## 6. 安全拓展

### 6.1 认证增强

| 措施 | 当前 | 建议 |
|---|---|---|
| Token 类型 | 单 JWT (7 天) | Refresh Token (30 天) + Access Token (15 分钟) |
| 登录限流 | 无 | Redis 计数器，5 次/分钟/IP |
| 验证码频控 | Session 过期 5 分钟 | Redis 限频，1 次/分钟/邮箱 |
| WebSocket Token | 同 JWT 密钥 | 独立短期 token (5 分钟有效) |

### 6.2 内容安全

- XSS 清洗：文章内容存储前 HTML sanitize。
- CSP Header：`Content-Security-Policy` 限制脚本来源。
- 上传文件病毒扫描：ClamAV 集成。
- 上传文件 magic number 校验。

### 6.3 权限缓存失效

当前 `AuthFilter` 用户缓存 60s TTL。生产环境需：

- 权限变更时主动失效：`AuthFilter.invalidateCache(userId)`。
- 管理员操作审计。
- 文章公开访问权限缓存但需强制失效。

## 7. 可观测性

### 7.1 指标

建议补齐的 Prometheus 指标：

| 指标 | 类型 | 用途 |
|---|---|---|
| `http_requests_total{method, path, status}` | Counter | QPS、错误率 |
| `http_request_duration_seconds{method, path}` | Histogram | P50/P95/P99 |
| `db_pool_active` | Gauge | 连接池使用率 |
| `db_pool_idle` | Gauge | 空闲连接数 |
| `db_pool_wait_total` | Counter | 连接等待次数 |
| `ws_connections_total{endpoint}` | Counter | WebSocket 连接数 |
| `ws_rooms_active` | Gauge | 活跃房间数 |
| `ws_room_members{room_id}` | Gauge | 房间成员数 |
| `upload_bytes_total` | Counter | 上传流量 |
| `upload_errors_total{reason}` | Counter | 上传失败 |

### 7.2 日志

建议结构化日志格式：

```json
{
  "timestamp": "2025-01-01T12:00:00.000Z",
  "level": "INFO",
  "requestId": "abc-123",
  "userId": 42,
  "method": "POST",
  "path": "/api/article/create",
  "latencyMs": 45,
  "status": 200,
  "message": "request completed"
}
```

### 7.3 告警

| 告警 | 条件 | 级别 |
|---|---|---|
| DB 连接池耗尽 | active >= maxTotal 持续 30s | CRITICAL |
| HTTP 错误率 | 5xx > 5% 持续 2 分钟 | WARNING |
| WebSocket 异常断开 | close rate > 10/min | WARNING |
| 上传失败 | error rate > 10% 持续 5 分钟 | WARNING |
| 磁盘空间 | > 85% | WARNING |

## 8. 推荐优先级

| 优先级 | 任务 | 预期收益 | 工作量 |
|---|---|---|---|
| P0 | 修复测试环境依赖，提供无数据库单测或 Testcontainers | 开发效率 | 中 |
| P1 | 数据库补索引（公开列表、搜索） | 查询性能 | 小 |
| P1 | 公开文章列表 N+1 优化（批量查点赞） | 列表加载 | 小 |
| P2 | 连接池迁移 HikariCP | 稳定性 | 小 |
| P2 | 文件上传迁移对象存储 | 可扩展性 | 中 |
| P2 | 前端 API 按 domain 拆文件 | 代码可维护性 | 小 |
| P3 | 协同编辑增加 update log + snapshot | 数据安全 | 大 |
| P3 | Redis 引入缓存和 Pub/Sub | 多实例支持 | 大 |
| P4 | 搜索服务外置 | 搜索性能 | 大 |
| P4 | 结构化日志 + 指标 + 告警 | 可观测性 | 中 |
