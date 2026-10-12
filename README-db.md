# 迭代一 数据库使用说明（README-db）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 数据库使用说明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 数据库与领域模型设计负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 适用对象 | 后端开发者、测试、评审者 |
| 数据库 | PostgreSQL 15（Docker 镜像 `postgres:15`） |
| 迁移工具 | Flyway（`flyway/flyway:10` 容器，或应用内 Flyway 依赖） |

> 本文档只覆盖**迭代一的数据库**（迁移验证用）。后端应用本体不在本步骤范围。

---

## 1. 一分钟快速开始

```bash
# 1) 启动 PostgreSQL 15（后台）
docker compose up -d

# 2) 等待 healthy
docker compose ps                     # 期望 STATUS 显示 (healthy)

# 3) 执行迁移（Flyway）
docker run --rm --network jc2001_default \
  -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/jc2001" \
  -user=jc2001 -password=jc2001_dev_password \
  -locations=filesystem:/flyway/sql migrate

# 4) 验证（25 项只读检查）
docker exec -i jc2001-postgres psql -U jc2001 -d jc2001 -f - < db/verify_schema.sql

# 5) 随手查一下
docker exec -it jc2001-postgres psql -U jc2001 -d jc2001 -c "SELECT count(*) FROM users;"
```

---

## 2. 连接参数

| 参数 | 值 | 来源 |
| --- | --- | --- |
| Host | `localhost` | `docker-compose.yml` 端口映射 |
| Port | **5432** | `POSTGRES_*` + `ports` |
| Database | `jc2001` | `POSTGRES_DB` |
| User | `jc2001` | `POSTGRES_USER` |
| Password | `jc2001_dev_password` | `POSTGRES_PASSWORD` |
| JDBC URL | `jdbc:postgresql://localhost:5432/jc2001` | Spring Boot 配置用 |
| 时区 | `Asia/Shanghai` | `TZ` / `PGTZ` / `-c timezone`（A-20） |

> ⚠️ 以上为**开发环境**凭据，写在 `docker-compose.yml` 中便于本地验证。生产环境必须改为环境变量/密钥管理。

### 2.1 psql 连接

```bash
# 方式 A：进入容器
docker exec -it jc2001-postgres psql -U jc2001 -d jc2001

# 方式 B：宿主机有 psql 时
psql "postgresql://jc2001:jc2001_dev_password@localhost:5432/jc2001"
```

### 2.2 常用 psql 命令

```sql
\dt                      -- 列出所有表（应看到 25 张业务表 + flyway_schema_history）
\d+ items                -- 查看表结构、约束、索引与**列注释**
\d+ item_occupations     -- 占用台账（C-01/R-23 的核心表）
SELECT * FROM flyway_schema_history;   -- 迁移历史
```

---

## 3. 迁移脚本说明

| 文件 | 内容 | 可重复执行 | 生产可用 |
| --- | --- | --- | --- |
| `V1__init_schema.sql` | 26 个枚举类型、25 张业务表、全部约束/索引/触发器、324 条列注释 | ❌（Flyway 只执行一次） | ✅ |
| `V2__seed_reference_data.sql` | 预设字典：7 个交易区域、13 个取货点、6 个分类、5 个成色 | ❌ | ✅（但含**占位名称**，需先替换） |
| `V3__seed_development_data.sql` | 开发种子：管理员、示例用户/商品/订单/交换/处罚/通知 | ❌ | ❌ **禁止生产执行**（含占位账号与密码） |

### 3.1 迁移顺序与依赖

```text
V1（表结构） → V2（预设字典，依赖 V1） → V3（开发数据，依赖 V1+V2）
```

三者必须**按版本号顺序**执行；Flyway 自动保证。

### 3.2 执行方式

**方式 A：Flyway 容器（推荐，验证版本控制）**

```bash
docker run --rm --network jc2001_default \
  -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/jc2001" \
  -user=jc2001 -password=jc2001_dev_password \
  -locations=filesystem:/flyway/sql migrate
```

**方式 B：psql 顺序执行（等价验证 SQL 本身）**

```bash
for f in V1__init_schema V2__seed_reference_data V3__seed_development_data; do
  echo "== $f"
  docker exec -i jc2001-postgres psql -U jc2001 -d jc2001 -v ON_ERROR_STOP=1 \
    < "src/main/resources/db/migration/$f.sql" || { echo "FAILED: $f"; exit 1; }
done
```

> 方式 B 绕过 Flyway 版本表，仅用于**排查**。正式流程请用方式 A。

### 3.3 其他 Flyway 命令

```bash
FLY="docker run --rm --network jc2001_default \
  -v $PWD/src/main/resources/db/migration:/flyway/sql:ro \
  flyway/flyway:10 \
  -url=jdbc:postgresql://postgres:5432/jc2001 \
  -user=jc2001 -password=jc2001_dev_password -locations=filesystem:/flyway/sql"

$FLY info        # 查看迁移状态
$FLY validate    # 校验 checksum 一致性
$FLY repair      # 修复版本表（仅在明确知道原因时使用）
```

---

## 4. 常用查询

### 4.1 结构与统计

```sql
-- 业务表清单
SELECT table_name FROM information_schema.tables
WHERE table_schema='public' AND table_name<>'flyway_schema_history' ORDER BY 1;

-- 各表行数（快速巡检）
SELECT relname AS table_name, n_live_tup AS approx_rows
FROM pg_stat_user_tables ORDER BY relname;

-- 枚举类型与取值（核对状态机）
SELECT t.typname, string_agg(e.enumlabel, ', ' ORDER BY e.enumsortorder)
FROM pg_type t JOIN pg_enum e ON t.oid=e.enumtypid
JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public'
GROUP BY t.typname ORDER BY t.typname;
```

### 4.2 业务态查看

```sql
-- 当前所有进行中占用（应每个商品最多 1 行）
SELECT io.item_id, i.title, io.ref_type, io.ref_id, io.occupied_at
FROM item_occupations io JOIN items i ON i.id=io.item_id
WHERE io.released_at IS NULL ORDER BY io.item_id;

-- 待付款订单（含剩余时间，R-07）
SELECT order_no, expires_at, expires_at - now() AS remaining
FROM orders WHERE status='PENDING_PAYMENT' ORDER BY expires_at;

-- 待处理交换提议（含剩余时间，R-25）
SELECT proposal_no, expires_at, expires_at - now() AS remaining
FROM swap_proposals WHERE status='PENDING' ORDER BY expires_at;

-- 生效中处罚（R-30）
SELECT u.username, p.penalty_type, p.reason, p.ends_at
FROM penalties p JOIN users u ON u.id=p.target_user_id
WHERE p.status IN ('ACTIVE','APPEALING');

-- 待处理申诉与举报
SELECT * FROM appeals WHERE status='SUBMITTED';
SELECT * FROM reports WHERE status='PENDING';
```

### 4.3 定时任务将扫描的行（验证部分索引有效）

```sql
-- FR-601 订单超时
SELECT id, order_no FROM orders
WHERE status='PENDING_PAYMENT' AND now() >= expires_at;

-- FR-602 交换过期
SELECT id, proposal_no FROM swap_proposals
WHERE status='PENDING' AND now() >= expires_at;

-- FR-603 处罚到期
SELECT id, target_user_id FROM penalties
WHERE status IN ('ACTIVE','APPEALING') AND now() >= ends_at;

-- FR-604 评价窗口关闭
SELECT id, order_no FROM orders
WHERE review_status <> 'WINDOW_CLOSED' AND review_deadline IS NOT NULL
  AND now() > review_deadline;
```

---

## 5. 常见问题

### 5.1 端口 5432 被占用怎么办？

**症状**

```text
Error response from daemon: ... Bind for 0.0.0.0:5432 failed: port is already allocated
```

**原因**：宿主机已有 PostgreSQL 或其他容器占用 5432。

**解法（任选）**

```bash
# 先看是谁占用
lsof -nP -iTCP:5432 -sTCP:LISTEN
docker ps --format '{{.Names}}: {{.Ports}}'

# 解法 A：临时改用其他端口启动一次性容器（不改交付文件）
docker run -d --name jc2001-pg \
  -e POSTGRES_DB=jc2001 -e POSTGRES_USER=jc2001 -e POSTGRES_PASSWORD=jc2001_dev_password \
  -p 55432:5432 postgres:15
# 迁移时把 URL 改为 jdbc:postgresql://<容器名或host>:5432/jc2001（容器网络内仍用 5432）

# 解法 B：若确认 5432 上的容器可以停，停掉它
docker stop <占用容器名>

# 解法 C：修改 docker-compose.yml 的端口映射为 "55432:5432"（本地改动，注意别提交）
```

> 本次验证即遇到该情况（`campushub-poc-postgres` 占用 5432），已改用 55432 完成全部验证，
> 详见 `docs/iteration1/design/db-verification.md` §1。

### 5.2 迁移失败提示 checksum mismatch

```text
Migration checksum mismatch for migration version 1
```

**原因**：修改了**已执行过**的迁移脚本。

**解法**：**不要**改已执行的脚本。把变更写进新的 `V4__xxx.sql`。
若只是本地验证库弄脏，可清库重来：

```bash
docker compose down -v      # ⚠️ 删除数据卷，数据全部丢失
docker compose up -d
# 重新执行迁移
```

### 5.3 想彻底重来

```bash
docker compose down -v      # 删容器 + 数据卷
docker compose up -d
docker run --rm --network jc2001_default \
  -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:10 -url="jdbc:postgresql://postgres:5432/jc2001" \
  -user=jc2001 -password=jc2001_dev_password \
  -locations=filesystem:/flyway/sql migrate
```

### 5.4 `CREATE EXTENSION citext` 报权限不足

`citext` 是 PostgreSQL contrib 扩展，官方 `postgres:15` 镜像已内置。
若使用受限权限的非超级用户，需要超级用户先执行一次：

```sql
CREATE EXTENSION IF NOT EXISTS citext;
```

### 5.5 占位数据在哪里？如何替换？

全部占位数据集中在 **`V2__seed_reference_data.sql`**（区域、取货点）与
**`V3__seed_development_data.sql`**（账号、示例业务数据），每处都有 `-- TODO: 待用户确认后替换`。

替换方式（保持可追溯）：

```sql
-- 新增 V4__replace_trade_zones.sql（不要改 V2）
UPDATE trade_zones SET name='真实校区名称' WHERE code='CAMPUS_A';
-- 或按确认后的清单 INSERT / 停用 is_active=FALSE
```

对应的待确认问题：**Q-01 / Q-02 / Q-03 / Q-04**，详见
`docs/iteration1/design/open-questions.md` §2。

### 5.6 为什么查不到某个状态值？

状态使用 PostgreSQL ENUM，取值必须与 `docs/iteration1/state-machines.md` 完全一致。
若需要新增状态值：

```sql
-- 必须放在新迁移中，且不要在同一事务里使用新值
ALTER TYPE order_status ADD VALUE 'REFUNDING';
```

> 注意：迭代一明确**不做退款**（OOS-14），故没有退款类状态。

---

## 6. 生产环境注意事项

| # | 事项 |
| --- | --- |
| 1 | **禁止执行 `V3__seed_development_data.sql`**：含占位管理员账号与密码 |
| 2 | `V2` 的占位区域/取货点名称必须先替换（Q-01/Q-02/Q-03） |
| 3 | 连接凭据必须改为环境变量/密钥管理，不要用 `docker-compose.yml` 中的开发密码 |
| 4 | `users.password_hash` 为占位串，需由应用初始化流程写入真实哈希（哈希算法待接口契约冻结） |
| 5 | 本 compose **仅用于本地验证**，不含高可用、备份、监控（OOS-17 明确不做） |
| 6 | 时区需与 JDBC/应用配置一致（`Asia/Shanghai`，A-20） |

---

## 7. 相关文档

| 文档 | 用途 |
| --- | --- |
| `docs/iteration1/design/README.md` | 需求→表映射、设计决策、覆盖度自检 |
| `docs/iteration1/design/er-diagram.md` | ER 图（含占用/交换/治理详图） |
| `docs/iteration1/design/schema.md` | 25 张表 / 324 列字段字典 |
| `docs/iteration1/design/db-verification.md` | 迁移验证记录与 19 项负向测试 |
| `docs/iteration1/design/open-questions.md` | 25 个 DQ 待确认问题 |
| `docs/iteration1/requirements.md` | 需求基线（54 FR） |
| `docs/iteration1/rules.md` | 34 条业务规则（R-01~R-35） |
| `docs/iteration1/state-machines.md` | 状态机与枚举定义（数据库枚举的唯一依据） |

---

## 8. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 数据库与领域模型设计负责人 | 初稿：快速开始、连接参数、迁移说明、常用查询、常见问题（含端口冲突）、生产注意事项 | 草稿（待确认） |
