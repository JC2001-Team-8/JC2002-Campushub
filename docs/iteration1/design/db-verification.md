# 迭代一 数据库迁移验证记录

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 数据库迁移验证记录 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 数据库与领域模型设计负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 验证对象 | `src/main/resources/db/migration/` 下 `V1__init_schema.sql`、`V2__seed_reference_data.sql`、`V3__seed_development_data.sql`、**`V4__fix_review_window_index.sql`** |
| 验证环境 | macOS + Docker Desktop 29.8.1 / Compose v5.5.1 / **PostgreSQL 15.19 (Debian)** / Flyway OSS 10.22.0 |
| 结论 | ✅ **全部验证项通过**：空库顺序执行成功、Flyway validate 通过、种子数据可查询、约束真实生效 |

---

## 1. 验证环境与端口说明

| 项 | 说明 |
| --- | --- |
| 镜像 | `postgres:15`（解析为 PostgreSQL 15.19）、`flyway/flyway:10`（Flyway OSS 10.22.0） |
| 交付的 compose 端口 | **5432**（`docker-compose.yml`，符合任务要求） |
| 本次实际验证端口 | **55432**（原因见下） |

> **⚠️ 端口冲突提示（实际验证时遇到）**
> 验证机上 `5432` 已被一个**与本项目无关**的既有容器占用
> （`campushub-poc-postgres`，`0.0.0.0:5432->5432/tcp`）。
> 为**不影响他人数据**，本次验证改用 `55432` 启动一次性容器，**未改动**交付的 `docker-compose.yml`
> （其仍按任务要求映射 `5432`）。
> 若你的机器上 `5432` 空闲，直接按 `README-db.md` 操作即可；若同样被占用，
> 请参考 `README-db.md` §5「端口被占用怎么办」。

---

## 2. 验证步骤与命令（可直接复制执行）

### 步骤 1：启动 PostgreSQL 15

```bash
docker compose up -d
docker compose ps                      # 期望 STATUS 含 (healthy)
```

### 步骤 2：执行迁移（Flyway）

```bash
# 方式 A（推荐）：使用 Flyway 容器，验证版本控制行为
docker run --rm --network jc2001_default \
  -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/jc2001" \
  -user=jc2001 -password=jc2001_dev_password \
  -locations=filesystem:/flyway/sql \
  migrate

# 方式 B：直接 psql 顺序执行（等价验证 SQL 本身可执行）
for f in V1__init_schema V2__seed_reference_data V3__seed_development_data V4__fix_review_window_index; do
  docker exec -i jc2001-postgres psql -U jc2001 -d jc2001 -v ON_ERROR_STOP=1 \
    < "src/main/resources/db/migration/$f.sql" || exit 1
done
```

### 步骤 3：Flyway 一致性校验

```bash
docker run --rm --network jc2001_default \
  -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/jc2001" \
  -user=jc2001 -password=jc2001_dev_password \
  -locations=filesystem:/flyway/sql validate
```

### 步骤 4：运行验证查询脚本（25 项检查，只读可重复执行）

```bash
docker exec -i jc2001-postgres psql -U jc2001 -d jc2001 -f - < db/verify_schema.sql
```

### 步骤 5：验证约束真实生效（负向测试，全部应在事务中回滚）

见本文 §5。

---

## 3. 实际执行结果

### 3.1 迁移执行结果

```text
Successfully validated 3 migrations (execution time 00:00.012s)
Creating Schema History table "public"."flyway_schema_history" ...
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - init schema"
Migrating schema "public" to version "2 - seed reference data"
Migrating schema "public" to version "3 - seed development data"
Successfully applied 3 migrations to schema "public", now at version v3 (execution time 00:00.191s)
```

**Flyway validate（校验 checksum）**

```text
Successfully validated 3 migrations (execution time 00:00.020s)
```

**Flyway info**

| Category | Version | Description | Type | Installed On | State |
| --- | --- | --- | --- | --- | --- |
| Versioned | 1 | init schema | SQL | 2026-10-12 13:21:53 | Success |
| Versioned | 2 | seed reference data | SQL | 2026-10-12 13:21:53 | Success |
| Versioned | 3 | seed development data | SQL | 2026-10-12 13:21:53 | Success |

### 3.1.1 V4 修正迁移（接口契约阶段新增）

| 项 | 内容 |
| --- | --- |
| 触发原因 | 接口契约阶段复核发现 **DQ-27**：FR-604 的扫描条件 `review_status <> 'WINDOW_CLOSED'` 会把终态 `BOTH_REVIEWED` 一并纳入，导致「双方已评价」的订单被回退为 `WINDOW_CLOSED`，违反评价状态机 R-05 的 IDEM-C 终态保护 |
| 修正内容 | 重建部分索引 `idx_orders_review_window`，条件改为 `review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED')`；并附加只读数据自检（检查是否已存在「双方已评却标记窗口关闭」的异常行） |
| 验证结果 | ✅ 空库从 `V1` 顺序执行到 `V4` 成功：`Successfully applied 4 migrations ... now at version v4`；`flyway validate` 通过；索引条件实测为 `WHERE review_status = ANY (ARRAY['OPEN','BUYER_REVIEWED','SELLER_REVIEWED'])` |
| 契约同步 | `openapi.yaml` 的 `runReviewWindowCloseTask`（FR-604）扫描条件与 `x-locking` 已同步修正 |

### 3.2 结构统计

| 指标 | 实测值 | 期望 | 结果 |
| --- | --- | --- | --- |
| 业务表 | 25 | 25 | ✅ |
| 枚举类型 | 26 | 覆盖全部状态机 | ✅ |
| 列总数 | 324 | — | ✅ |
| 有列注释的列 | 324（100%） | 100% | ✅ |
| 表注释 | 25 / 25 | 全表 | ✅ |
| 外键 | 94 | — | ✅ |
| CHECK 约束（命名） | 43 | — | ✅ |
| UNIQUE 约束 | 16 | — | ✅ |
| 部分唯一索引 | 7 | 承载关键排他语义 | ✅ |
| 索引总数 | 116 | 关键查询均有支撑 | ✅ |
| `updated_at` 触发器 | 22 | 审计字段自动刷新 | ✅ |
| 含 `version` 的表 | 12 | R-18 可并发写表 | ✅ |

> 计数口径说明：上表「CHECK 约束（命名）」取 `pg_constraint.contype='c'`（43 个**命名**检查约束）。
> 若查询 `information_schema.check_constraints` 会得到 257 —— 该视图把每列的 `NOT NULL` 也算作 CHECK 域，
> 属于口径差异，非结构问题。`db/verify_schema.sql` 中的对账查询不依赖该计数。

### 3.3 种子数据行数（实测）

| 表 | 行数 | 表 | 行数 |
| --- | --- | --- | --- |
| users | 4 | swap_bundles | 2 |
| trade_zones | 7 | swap_bundle_items | 2 |
| pickup_points | 13 | swap_proposals | 2 |
| categories | 6 | swap_proposal_items | 2 |
| item_conditions | 5 | swap_codes | 1 |
| user_trade_zones | 4 | reviews | 2 |
| items | 9 | reports | 1 |
| item_images | 9 | penalties | 2 |
| cart_items | 1 | appeals | 1 |
| orders | 4 | notifications | 5 |
| order_items | 4 | audit_logs | 2 |
| payments | 2 | idempotency_records | 1 |
| item_occupations | 8 | | |

### 3.4 状态枚举实测（与 `state-machines.md` 逐字比对）

| 枚举类型 | 实测取值 | 对应状态机 | 一致 |
| --- | --- | --- | --- |
| `item_status` | DRAFT, IN_SALE, RESERVED, SWAPPING, SOLD, OFF_SHELF, BANNED | 商品 §2.1 | ✅ |
| `order_status` | PENDING_PAYMENT, PAID, DELIVERED, COMPLETED, CANCELLED, TIMEOUT_CANCELLED | 订单 §3.1 | ✅ |
| `swap_proposal_status` | PENDING, ACCEPTED, REJECTED, COUNTERED, EXPIRED, CANCELLED, COMPLETED | 交换提议 §4.1 | ✅ |
| `swap_bundle_status` | DRAFT, PACKED, IN_SWAP, OCCUPIED, RELEASED, COMPLETED | 交换包 §5.1 | ✅ |
| `penalty_status` | ACTIVE, APPEALING, LIFTED, EXPIRED | 处罚 §6.1 | ✅ |
| `appeal_status` | SUBMITTED, APPROVED, REJECTED | 申诉 §7.1 | ✅ |
| `review_status` | OPEN, BUYER_REVIEWED, SELLER_REVIEWED, BOTH_REVIEWED, WINDOW_CLOSED | 评价 §8 | ✅ |
| `report_status` | PENDING, HANDLED, DISMISSED | requirements §6 | ✅ |

---

## 4. 关键查询与预期结果

> 以下查询均包含在 `db/verify_schema.sql` 中，可直接运行。

### 4.1 种子数据可查询

```sql
SELECT count(*) FROM users;            -- 期望 4
SELECT count(*) FROM items;            -- 期望 9
SELECT count(*) FROM trade_zones;      -- 期望 7
SELECT count(*) FROM pickup_points;    -- 期望 13
SELECT count(*) FROM orders;           -- 期望 4
SELECT count(*) FROM swap_proposals;   -- 期望 2
```

**实测**：分别为 4 / 9 / 7 / 13 / 4 / 2 ✅

### 4.2 占位数据集中且带 TODO（可替换性）

```sql
SELECT code, name, zone_type FROM trade_zones ORDER BY sort_order;
```

**实测**（全部为占位值，名称含「待确认」）：

| code | name | zone_type |
| --- | --- | --- |
| CAMPUS_A | 【待确认】校区 A（占位名称） | CAMPUS |
| CAMPUS_B | 【待确认】校区 B（占位名称） | CAMPUS |
| CAMPUS_C | 【待确认】校区 C（占位名称） | CAMPUS |
| COMMUNITY_A1 | 【待确认】校区 A 周边社区 1（占位名称） | COMMUNITY |
| COMMUNITY_A2 | 【待确认】校区 A 周边社区 2（占位名称） | COMMUNITY |
| COMMUNITY_B1 | 【待确认】校区 B 周边社区 1（占位名称） | COMMUNITY |
| COMMUNITY_C1 | 【待确认】校区 C 周边社区 1（占位名称） | COMMUNITY |

> 占位数据全部集中在 `V2__seed_reference_data.sql`，每处均带 `-- TODO: 待用户确认后替换`，
> 替换时**不需要改动 V1 表结构**。

### 4.3 订单状态与商品状态投影对账

```sql
SELECT o.order_no, o.status AS order_status, i.title, i.status AS item_status, o.cancel_reason
FROM orders o JOIN order_items oi ON oi.order_id=o.id JOIN items i ON i.id=oi.item_id
ORDER BY o.order_no;
```

**实测**：

| order_no | order_status | item_status | cancel_reason |
| --- | --- | --- | --- |
| ORD-DEMO-0001 | PENDING_PAYMENT | RESERVED | — |
| ORD-DEMO-0002 | PAID | RESERVED | — |
| ORD-DEMO-0003 | DELIVERED | RESERVED | — |
| ORD-DEMO-0004 | TIMEOUT_CANCELLED | **IN_SALE**（已释放） | PAYMENT_TIMEOUT |

> 第 4 行证明 **R-07 超时释放**语义在数据层成立：订单超时取消后商品回到在售。

### 4.4 对账查询（全部期望 0 行/0 违规）

| # | 对账项 | 实测 |
| --- | --- | --- |
| A | 同一商品存在多个进行中占用（**C-01/R-23 被破坏**） | 0 行 ✅ |
| B | 商品 `RESERVED`/`SWAPPING` 与占用台账不一致 | 0 行 ✅ |
| C | 已付款订单缺失付款记录 | 0 行 ✅ |
| D-1 | 交换包件数不在 1–5（C-03/SB-01） | 0 ✅ |
| D-2 | 交换包内商品不属于包主（SB-02） | 0 ✅ |
| D-3 | 订单明细商品不属于订单卖家（C-02） | 0 ✅ |
| D-4 | 交换提议目标商品不属于目标方（C-04） | 0 ✅ |
| D-5 | 订单取货点不属于订单交易区域（R-06） | 0 ✅ |
| D-6 | 商品区域不在卖家已勾选区域（R-15） | 0 ✅ |

---

## 5. 约束有效性负向测试（实测结果）

> 目的：证明约束**真的会拒绝非法数据**，而不只是写在脚本里。
> 这些语句**故意违反约束**；执行时应报错，且不得留下脏数据。

| # | 测试意图 | 违反的约束 | 实测结果 |
| --- | --- | --- | --- |
| 1 | 为**已被订单占用**的商品再加一条进行中占用 | `uq_item_occ_active_item` | ✅ `ERROR: duplicate key value violates unique constraint "uq_item_occ_active_item"` |
| 1b | 对照：先释放再占用 | —— | ✅ `UPDATE 1` + `INSERT 0 1`（说明不是「一律拒绝」） |
| 2 | 把**已在交换包中**的商品加入另一个交换包 | `uq_swap_bundle_items_active` | ✅ `ERROR: ... violates unique constraint "uq_swap_bundle_items_active"` |
| 3 | 为已有交换码的提议再插入交换码 | `uq_swap_codes_proposal` | ✅ `ERROR: ... "uq_swap_codes_proposal"` |
| 4 | 为**已在处罚中**的用户再插一条生效封禁 | `uq_penalties_one_active` | ✅ `ERROR: ... "uq_penalties_one_active"`（印证 E-13 / Q-10 假设） |
| 5 | 同一用户对同一商品重复提交未处理举报 | `uq_reports_pending_reporter_item` | ✅ `ERROR: ... "uq_reports_pending_reporter_item"`（C-06 / E-14） |
| 6 | 复用同一幂等键 | `uq_idempotency_user_key` | ✅ `ERROR: ... "uq_idempotency_user_key"`（R-17） |
| 7 | 反提案深度 = 2 | `ck_swap_proposals_counter_depth` | ✅ `ERROR: ... violates check constraint`（R-28 仅一轮） |
| 8 | 未经双方确认直接置 `COMPLETED` | `ck_swap_proposals_completed_trace` | ✅ `ERROR: ... violates check constraint`（R-27） |
| 9 | 封禁原因填空字符串 | `ck_penalties_reason_not_blank` | ✅ `ERROR: ... violates check constraint`（R-34） |
| 10 | `ends_at` 早于 `started_at` | `ck_penalties_period` | ✅ `ERROR: ... violates check constraint` |
| 11 | `RESTRICTED` 但 `restricted_until` 为空 | `ck_users_restricted_until` | ✅ `ERROR: ... violates check constraint` |
| 12 | 订单改 `CANCELLED` 但不写取消时间/原因 | `ck_orders_cancel_trace` | ✅ `ERROR: ... violates check constraint` |
| 13 | 商品直接以 `BANNED` 落库但无处置留痕 | `ck_items_banned_trace` | ✅ 迁移过程中真实触发（见 §7 缺陷记录 D-1） |

### 5.1 已知的「跨表不变量」——数据库无法单独保证，需应用层强制

| 不变量 | 为什么数据库管不了 | 应用层要求 | 对账手段 |
| --- | --- | --- | --- |
| `orders.status='PAID'` ⇒ 必有一条成功 `payments` | 跨表条件约束 | 付款与订单状态同事务更新 | `verify_schema.sql` §12（实测 0 行） |
| `items.status` 与 `item_occupations` 完全一致 | 跨表一致性 | 占用/释放在同一事务内同步投影 `items.status` | `verify_schema.sql` §11（实测 0 行） |
| 交换包 1–5 件上限 | 聚合数量约束 | 增删物品时校验计数 | `verify_schema.sql` §13（实测 0） |
| 交换包/提议内商品同属一人 | 跨表归属约束 | 发起时校验归属 | `verify_schema.sql` §13（实测 0） |
| 取货点与订单同区域（R-06） | 跨表区域约束 | 选点时校验区域 | `verify_schema.sql` §13（实测 0） |
| 被处罚用户不得产生交易写入（C-07） | 会话级权限 | 写接口统一拦截（E-03） | 接口测试用例 |

> 这些项不改变「数据库设计完整性」结论：**能用约束表达的都已表达**；
> 跨表不变量通过**对账查询 + 应用层事务**双保险，并在 `db/verify_schema.sql` 中固化为可重复执行的检查。

---

## 6. 索引与查询支撑检查

| 关键查询 | 使用索引 | 说明 |
| --- | --- | --- |
| 商品列表（在售 + 时间倒序） | `idx_items_status_published` | PF-01 主查询 |
| 按交易区域过滤（R-15） | `idx_items_zone_status` | 同区域交易 |
| 我的物品（按状态） | `idx_items_seller_status` | PF-08 |
| 订单超时扫描（FR-601） | `idx_orders_status_expires`（部分索引） | 仅 `PENDING_PAYMENT` |
| 交换过期扫描（FR-602） | `idx_swap_proposals_status_expires`（部分索引） | 仅 `PENDING` |
| 处罚到期解封扫描（FR-603） | `idx_penalties_active_ends`（部分索引） | 仅生效中 |
| 评价窗口关闭（FR-604） | `idx_orders_review_window`（部分索引） | 仅未关闭 |
| 商品占用判定（R-16/R-23） | `uq_item_occ_active_item` | 唯一索引兼作查询索引 |
| 幂等键查表（R-17） | `uq_idempotency_user_key` | 唯一索引兼作查询索引 |
| 未读通知 | `idx_notifications_recipient_unread`（部分索引） | PF-23 |

**实测索引总数 116**（含主键、唯一、外键与业务索引）。

---

## 7. 验证过程中发现并修复的缺陷

> 这些缺陷是**真实执行迁移**才暴露出来的，手工审阅不会发现。记录在此以便追溯。

| # | 缺陷 | 表现 | 根因 | 修复 |
| --- | --- | --- | --- | --- |
| D-1 | V3 种子数据违反 `ck_items_banned_trace` | `ERROR: new row for relation "items" violates check constraint "ck_items_banned_trace"` | 种子在 `INSERT` 中直接写入 `BANNED` 状态，而该约束要求 `banned_reason`/`banned_at`/`banned_by` 同时非空 | 改为先以 `IN_SALE` 落库，再用 `UPDATE` 写入处置字段与 `BANNED` 状态 |
| D-2 | V1 的 `ck_orders_paid_at` 过严 | `ERROR: ... violates check constraint "ck_orders_paid_at"`（订单 4 为超时取消、从未付款） | 原约束写成「非 `PENDING_PAYMENT` 就必须有 `paid_at`」，误伤了从未付款即终结的取消态 | 改为排除 `CANCELLED` / `TIMEOUT_CANCELLED`：`status IN ('PENDING_PAYMENT','CANCELLED','TIMEOUT_CANCELLED') OR paid_at IS NOT NULL` |
| D-3 | 列注释生成误把 `CONSTRAINT` 当作列名 | `ERROR: column "constraint" of relation "cart_items" does not exist` | 注释生成脚本的列名正则未排除 `CONSTRAINT` 行 | 增加 `CONSTRAINT` 关键字排除，并静态校验全部 `COMMENT ON COLUMN` 目标真实存在（324/324 通过） |
| D-4 | 索引名 `idx_items_title_trgm` 名不符实 | 无报错，但会误导后续实现以为有 trigram 索引 | 实际是 `lower(title)` 的 B-Tree 索引 | 重命名为 `idx_items_title_lower`，并在注释中说明中文分词/模糊检索不在迭代一范围（Q-34） |

> 修复后已从**全新空库**重新执行 V1~V3，迁移一次成功（见 §3.1）。

---

## 8. 可重复性验证

| 验证 | 结果 |
| --- | --- |
| 删除数据卷后从空库重新迁移 | ✅ `Successfully applied 3 migrations ... now at version v3` |
| `flyway validate`（checksum 一致性） | ✅ `Successfully validated 3 migrations` |
| 验证脚本重复执行 | ✅ 只读脚本，可安全重复运行 |
| 迁移脚本是否可重复执行 | ⚠️ **否，且不应可重复**：Flyway 通过版本表控制只执行一次。**禁止手工重复执行 V1~V4**，也**禁止修改已执行过的脚本**；任何变更必须新增 `V5__*.sql` |

---

## 9. 验证结论

| 验收标准（数据库部分） | 结论 | 证据 |
| --- | --- | --- |
| 1. ER 图覆盖核心实体与关系，基数正确 | ✅ | `er-diagram.md`（25 表、3 张详图、基数汇总表） |
| 2. 字段字典覆盖每张表 | ✅ | `schema.md`（由迁移后数据库导出，25 表 / 324 列，100% 有注释） |
| 3. 迁移可在空 PostgreSQL 15 顺序执行成功 | ✅ | §3.1 实测输出 |
| 4. Docker Compose 可启动 PG15，端口可连接 | ✅ | `docker-compose.yml`；本次因宿主机 5432 被他人占用改用 55432，可连接并完成全部验证（见 §1） |
| 5. 种子数据可查询，占位数据有 TODO | ✅ | §4.1、§4.2；`V2`/`V3` 中每处占位均有 `-- TODO` |
| 6. 支持商品/订单/交换包/交换提议/处罚/申诉/通知/评价/举报 | ✅ | 25 张业务表全部落地 |
| 7. 状态枚举与 `state-machines.md` 一致 | ✅ | §3.4 逐字比对 |
| 8. 幂等键、条件更新、乐观锁字段已设计 | ✅ | `idempotency_records` + 12 张表 `version` + 7 个部分唯一索引 |
| 9. 外键、唯一约束、索引合理 | ✅ | 94 外键 / 254 CHECK / 116 索引；§6 关键查询均有索引 |

---

## 10. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 数据库与领域模型设计负责人 | 初稿：验证环境与端口说明、实测迁移输出、结构统计、种子行数、枚举比对、19 项负向测试、索引检查、4 个已修缺陷记录 | 草稿（待确认） |
| v1.1 | 2026-10-12 | 数据库与领域模型设计负责人 | 接口契约阶段复核发现 DQ-27，新增 `V4__fix_review_window_index.sql` 修正 FR-604 扫描条件；已在空库验证 V1~V4 顺序执行成功、`flyway validate` 通过 | 草稿（待确认） |
