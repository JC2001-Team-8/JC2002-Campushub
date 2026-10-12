# 迭代一 幂等与并发说明

| 项目 | 内容 |
| --- | --- |
| 标题 | 迭代一 幂等与并发说明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 接口契约负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 依据文档 | [`../rules.md`](../rules.md)（R-01~R-35）、[`../state-machines.md`](../state-machines.md)、[`openapi.yaml`](./openapi.yaml)、[`design/schema.md`](./schema.md) |

> **本文档的定位**：幂等与并发的**唯一实现依据**。实现方不得以「前端已禁用按钮」「大概率不会并发」为理由绕过本文档约定的两层防护。
>
> **相关文档**：[`api-contract.md`](./api-contract.md) §2（逐接口的 `x-idempotency` / `x-locking`）、[`error-codes.md`](./error-codes.md)（错误码与前端处理）、[`schema.md`](./schema.md) §10（`idempotency_records`）、[`db-verification.md`](./db-verification.md)（对账查询）、[`open-questions.md`](./open-questions.md)（DQ-09）、[`../glossary-assumptions-questions.md`](../glossary-assumptions-questions.md)（Q-xx / A-xx）。

---

## 1. 为什么需要两层防护

**结论（强制规则）：幂等键层与条件更新层必须同时具备，缺一不可；禁止「先查后改」。**

| 层次 | 规则 | 解决的失效模式 | 不能解决的失效模式 |
| --- | --- | --- | --- |
| **第一层：幂等键层**（`Idempotency-Key`，R-17） | 同用户 + 同键 + 同请求体 → 返回**首次结果** | 用户**主动重复提交**：双击按钮、网络超时后重试、页面刷新后重放 | 两个**不同用户**（或同一用户换新键）在**同一时刻**竞争同一资源 |
| **第二层：条件更新层**（R-18） | `UPDATE ... WHERE id = ? AND status = ? AND version = ?`，受影响行数 0 = 并发冲突 | 任何来源的**并发竞争**：两个买家抢同一商品、付款与超时取消对撞、接受与过期对撞 | 同一逻辑操作的**重复提交**（第二次请求会因状态已变而报 409，用户看到「失败」但其实已成功） |

### 1.1 两层防护的职责边界

```
用户点击「提交订单」
        │
        ├─(1) Idempotency-Key 抢占 ──► 已存在同键记录？
        │                              ├─ COMPLETED 且 request_hash 相同 → 回放首次响应（IDEM-A / E-06）
        │                              ├─ request_hash 不同                → 409 COMMON_IDEMPOTENCY_KEY_REUSED（E-16）
        │                              └─ IN_PROGRESS                     → 见 §2.2（【待确认】DQ-09）
        │
        └─(2) 条件更新 + 部分唯一索引 ──► 抢占 item_occupations（uq_item_occ_active_item）
                                        └─ UPDATE items SET status='RESERVED' WHERE id=? AND status='IN_SALE'
                                           ├─ 受影响行数 = 1 → 成功，写回 idempotency_records.response_body
                                           └─ 受影响行数 = 0（或 SQLSTATE 23505）→ 409 + details.currentStatus
```

### 1.2 强制规则原文（R-18）

- **禁止「先查后改」**：禁止 `SELECT ... → 应用层判断 → UPDATE ...（无状态/版本条件）` 的两段式写法。两次语句之间存在竞态窗口，任何并发都可能穿透。
- **必须**使用下列两种写法之一：
  1. **条件更新**：`UPDATE <table> SET ..., version = version + 1 WHERE id = ? AND status = ? AND version = ?`
  2. **带冲突处理的插入**：`INSERT ... ON CONFLICT DO NOTHING`（利用部分唯一索引兜底排他语义）
- **必须**把「受影响行数 = 0」或「SQLSTATE 23505（unique_violation）」一律视为**并发冲突**，映射为 **409 + `details.currentStatus`**（必要时附 `details.currentVersion`），而**不是** 500。
- **禁止**把唯一约束冲突直接抛成 `COMMON_INTERNAL_ERROR`（500）。23505 是业务冲突，必须映射为域错误码（见 §7.2）。

### 1.3 两条最容易踩的边界

| 边界 | 约定 |
| --- | --- |
| 幂等键缺失 | 服务端按「无幂等保护」处理，但**业务层仍必须**做状态前置校验 + 条件更新（R-17）；即：少了第一层，第二层仍要挡住重复副作用 |
| 状态已变化但用户其实已成功 | 幂等命中（E-06）返回 200/201 **不是错误**，前端**不得**提示失败（见 §3 注 3） |

---

## 2. 幂等键（Idempotency-Key）约定

### 2.1 基本约定

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 请求头名称 | `Idempotency-Key` | R-17 |
| 生成方 | **前端**生成，**UUID v4** | 假设 **A-15**（源问题 Q-27） |
| 长度约束 | **8–64 字符**（OpenAPI `minLength: 8` / `maxLength: 64`；DB 列 `varchar(64)`） | `openapi.yaml` 参数定义、`schema.md` |
| 必填性 | 16 个接口**必填**（§3）；16 个接口**可选但建议**（§4）；其余接口不涉及 | `api-contract.md` §1 |
| 复用要求 | **同一逻辑操作的所有重试必须复用同一个键**；新的逻辑操作必须用新键 | R-17 |
| 记录保留期 | **24 小时**（`expires_at` 默认 `now() + interval '24 hours'`；过期后同键可重新使用） | 假设 **A-08**（源问题 Q-27） |
| 作用域 | **用户级**：`uq_idempotency_user_key (user_id, idempotency_key)`。不同用户使用相同键字符串互不影响 | R-17、`schema.md` |
| 缺失时的行为 | 按「无幂等保护」处理，但**业务层仍须**做状态校验与条件更新 | R-17 |
| 传输安全 | 键本身不携带业务权限；服务端必须校验 `user_id` 取自鉴权上下文，**不得**信任请求体中的用户标识 | `error-codes.md` E-02 |

> **注意**：`Idempotency-Key` **不是** CSRF/鉴权凭证；它只保证「同一用户同一逻辑操作最多产生一次副作用」。

### 2.2 语义表（R-17 / E-06 / E-16 / DQ-09）

| # | 情形 | 判定依据 | HTTP | 响应 | 依据 / 状态 |
| --- | --- | --- | --- | --- | --- |
| 1 | **同用户 + 同键 + 同请求体**，首次请求已完成 | `status='COMPLETED'` 且 `request_hash` 与本次一致 | **200 / 201** | `code='OK'` + **回放 `response_body` 快照**（与首次**完全一致**，含同一 `resource_id`） | **IDEM-A**；**E-06**（幂等命中无错误码）；安全重试 |
| 2 | **同用户 + 同键 + 不同请求体** | `request_hash` 与本次不一致 | **409** | `COMMON_IDEMPOTENCY_KEY_REUSED` | **E-16**；前端须生成**新键**后重试 |
| 3 | **同用户 + 同键 + 首次请求仍在处理中** | `status='IN_PROGRESS'` | **409**（建议） | 建议 `COMMON_REQUEST_IN_PROGRESS` + 提示稍后重试 / `Retry-After` | **【待确认】DQ-09**：`IN_PROGRESS` 的处理口径尚未冻结。候选策略：①立即 409 拒绝并要求重试（**本文档建议**）；②短时等待首次请求完成后再回放；③返回 202 + `Location`。**在 DQ-09 关闭前，实现方不得自行决定并固化** |
| 4 | **同一键 + 不同用户** | 唯一索引含 `user_id`，两条记录互不冲突 | 各自独立处理 | 各自独立结果 | `uq_idempotency_user_key`；R-17 |
| 5 | **同一用户 + 同一键 + 不同 `endpoint`** | 唯一索引只覆盖 `(user_id, idempotency_key)`，`endpoint` 不参与唯一性 | **409**（建议） | 建议按 E-16 同义处理（`COMMON_IDEMPOTENCY_KEY_REUSED`） | **【待确认】DQ-09**：同键跨接口复用是否算「同键异义」尚未冻结；`endpoint` 列已存在，实现时**必须**校验其一致性，禁止静默回放另一个接口的结果 |
| 6 | 记录已过期（`expires_at < now()`） | 过期记录不参与幂等判定 | 按新请求处理 | 正常业务响应 | 假设 **A-08**；清理任务归属 **【待确认】DQ-09** |
| 7 | 首次请求**业务失败**（4xx 非幂等冲突类） | `status='FAILED'` + `error_message` | 按首次结果回放 | 建议回放首次的失败响应（**不得**静默改判为成功） | **【待确认】DQ-09**：失败响应是否入快照（`FAILED` 枚举已存在，但回放策略未冻结） |

> **注 1**：`request_hash` 列类型为 `varchar(64)`，可容纳 SHA-256 十六进制摘要（64 字符）。**具体摘要算法与规范化规则（字段排序、忽略字段清单）属【待确认】DQ-09**，未冻结前不得作为跨版本兼容承诺。
>
> **注 2**：只有**同键 + 同请求体**才回放。禁止仅凭「键相同」就回放——这正是 E-16 存在的原因（防止把 A 操作的请求误判为 B 操作的成功结果）。
>
> **注 3**：命中路径 1 时，响应体必须原样携带首次的 `resource_id` / 订单列表 / 交换码等业务载荷。前端按**成功**处理并跳转对应资源，**不得**弹出错误提示（`error-codes.md` §前端处理 第 8 条）。

### 2.3 承载表 `idempotency_records`

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `user_id` | bigint | 否 | — | 幂等主体；取自鉴权上下文（R-17） |
| `idempotency_key` | varchar(64) | 否 | — | 请求头 `Idempotency-Key`（假设 A-15：UUID v4，8–64 字符） |
| `endpoint` | varchar(255) | 否 | — | 接口标识，如 `POST /orders`；用于拦截同键跨接口复用（§2.2 情形 5） |
| `request_hash` | varchar(64) | 否 | — | 请求体摘要；E-16 的判定依据（算法【待确认】DQ-09） |
| `status` | idempotency_status | 否 | `'IN_PROGRESS'` | 枚举 `IN_PROGRESS` / `COMPLETED` / `FAILED`（V1 迁移 `CREATE TYPE idempotency_status`） |
| `response_status` | integer | 是 | — | 首次响应 HTTP 状态码 |
| `response_body` | jsonb | 是 | — | **首次响应快照**；重复请求直接回放该 JSON |
| `resource_type` | varchar(32) | 是 | — | 首次产生的资源类型（`ORDER` / `SWAP_PROPOSAL` / `ITEM` / `PENALTY` ...） |
| `resource_id` | bigint | 是 | — | 首次产生的资源 ID；幂等断言的核心字段（§12） |
| `error_message` | varchar(500) | 是 | — | 首次失败原因（`status='FAILED'` 时填写） |
| `version` | integer | 否 | 0 | R-18：并发同键请求以 `INSERT ... ON CONFLICT` 抢占后再推进 |
| `expires_at` | timestamptz | 否 | `now() + interval '24 hours'` | 假设 A-08；过期记录由定时任务清理（归属【待确认】DQ-09） |
| `created_at` / `updated_at` | timestamptz | 否 | `now()` | 审计字段（触发器自动刷新 `updated_at`） |
| `created_by` / `updated_by` | bigint | 是 | — | 审计字段，外键 → `users.id`（ON DELETE SET NULL） |

**约束与索引**

| 名称 | 定义 | 作用 |
| --- | --- | --- |
| `uq_idempotency_user_key` | `UNIQUE (user_id, idempotency_key)` | **R-17**：同一用户 + 同一幂等键最多一条记录，是幂等抢占的**数据库级真值来源** |
| `idx_idempotency_endpoint` | `(endpoint)` | 观测与排查 |
| `idx_idempotency_expires` | `(expires_at)` | 过期清理任务扫描 |
| `idx_idempotency_resource` | `(resource_type, resource_id)` | 由资源反查幂等记录/对账 |

> 外键：`user_id → users.id (ON DELETE CASCADE)`；`created_by` / `updated_by → users.id (ON DELETE SET NULL)`。

### 2.4 推荐实现模式（抢占 → 处理 → 写回 → 回放）

**步骤 1：抢占（第一层闸门）**

```sql
-- 抢占式插入：唯一索引 uq_idempotency_user_key 决定谁赢
INSERT INTO idempotency_records
    (user_id, idempotency_key, endpoint, request_hash, status, expires_at)
VALUES
    (:userId, :idempotencyKey, 'POST /orders', :requestHash, 'IN_PROGRESS',
     now() + interval '24 hours')
ON CONFLICT (user_id, idempotency_key) DO NOTHING
RETURNING id;
```

- `RETURNING` **有行** → 本次请求赢得抢占，继续执行业务（步骤 2）。
- `RETURNING` **无行** → 已存在同键记录，转入步骤 4（判定/回放）。

**步骤 2：业务处理（第二层闸门）**

同一事务内完成：状态条件更新 + 副作用 + 占用台账；任何一步受影响行数为 0 或抛 23505，立即回滚并把本次请求判定为**业务失败**。

```sql
-- 例：提交订单占用商品（R-16 / R-18），受影响行数 = 0 表示被并发抢占
UPDATE items
   SET status = 'RESERVED', version = version + 1
 WHERE id = :itemId AND status = 'IN_SALE';
-- 受影响行数 = 0 → 409 ITEM_ALREADY_RESERVED / ITEM_RESERVED_BY_SWAP
```

**步骤 3：写回首次结果（同一事务内，必须）**

```sql
UPDATE idempotency_records
   SET status = :status,               -- COMPLETED | FAILED
       response_status = :httpStatus,
       response_body = :responseJson,  -- 首次响应快照（含 resource_id）
       resource_type = :resourceType,
       resource_id = :resourceId,
       error_message = :errorMessage,
       version = version + 1
 WHERE user_id = :userId AND idempotency_key = :idempotencyKey
   AND status = 'IN_PROGRESS';
```

> **强制**：`response_body` 的写回必须与业务副作用**同事务提交**。否则会出现「业务已成功但幂等记录停留 `IN_PROGRESS`」或「记录了成功但业务已回滚」的不一致。

**步骤 4：冲突方的处理（回放）**

| 已存在记录的状态 | 处理 |
| --- | --- |
| `COMPLETED` 且 `request_hash` 相同 | 直接以 `response_status` + `response_body` 构造响应（**IDEM-A / E-06**），**不再执行业务逻辑** |
| `COMPLETED / FAILED` 且 `request_hash` 不同 | `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16） |
| `IN_PROGRESS` | **【待确认】DQ-09**（建议 `409 COMMON_REQUEST_IN_PROGRESS`） |

---

## 3. 必须携带 `Idempotency-Key` 的接口（16 个）

下表与 [`api-contract.md`](./api-contract.md) 的「幂等 = **必须**」行一一对应，覆盖 `openapi.yaml` 中 `x-idempotency: required` 的全部操作。

| operationId | method | path | 为什么必须 | 幂等语义 |
| --- | --- | --- | --- | --- |
| `createItem` | POST | `/items` | 创建**新资源**且不可撤销；重复提交会产生两条重复商品（R-17） | IDEM-A |
| `submitOrder` | POST | `/orders` | 提交即**拆单 + 占用库存**（R-05 / R-16）；重复提交会重复拆单、重复占用 | IDEM-A |
| `mockPayOrder` | POST | `/orders/{orderId}/mock-pay` | 付款推进状态并写付款记录；重复请求会重复写 `paid_at`、产生多条成功付款（R-08；`uq_payments_order_succeeded` 兜底） | IDEM-A |
| `createOrderReview` | POST | `/orders/{orderId}/reviews` | 评价**不可修改、不可删除**，每人每单一次（R-14）；重复提交产生重复评价 | IDEM-A（同键回放） |
| `createSwapBundle` | POST | `/swap-bundles` | 创建交换包即写入包内物品的 `ACTIVE` 归属（SB-03）；重复提交产生重复包与重复占用 | IDEM-A |
| `createSwapProposal` | POST | `/swap-proposals` | 发起即**同时占用双方全部商品**并启动 72h 倒计时（R-23 / R-25）；重复提交会二次占用并产生第二条提议 | IDEM-A |
| `acceptSwapProposal` | POST | `/swap-proposals/{proposalId}/accept` | 接受会推进状态并**生成一次性交换码**（R-22 / C-05）；重复点击不得二次推进状态、不得生成第二个交换码 | **IDEM-B** |
| `counterSwapProposal` | POST | `/swap-proposals/{proposalId}/counter` | 反提案**仅一轮**，会释放原占用并创建新提议（R-28）；重复提交会突破「仅一轮」上限并产生重复新提议 | **IDEM-B** |
| `createSwapReview` | POST | `/swap-proposals/{proposalId}/reviews` | 同订单互评：交换双方各一次（R-14）；重复提交产生重复评价 | IDEM-A（同键回放） |
| `adminBanUser` | POST | `/admin/users/{userId}/ban` | 封禁写用户状态 + 处罚记录 + 审计 + 通知（R-30 / R-34）；重复提交产生重复处罚与重复审计噪音 | IDEM-A |
| `adminLiftPenalty` | POST | `/admin/penalties/{penaltyId}/lift` | 解封写处罚终态 + 用户恢复 + 审计；与「到期自动解封」并发时只允许一次（R-31） | IDEM-A（同键回放） |
| `adminTakedownItem` | POST | `/admin/items/{itemId}/takedown` | 下架写商品状态、审计，并**联动取消订单 / 整笔作废交换**（R-35）；重复提交会重复联动与重复通知 | IDEM-A（下架本身） |
| `adminRestoreItem` | POST | `/admin/items/{itemId}/restore` | 恢复写商品状态与审计，且必须确认无占用冲突（I-16）；重复提交不得二次改写状态 | IDEM-A（同键回放） |
| `adminResolveAppeal` | POST | `/admin/appeals/{appealId}/resolve` | 处理申诉（通过即**立即解封**）不可逆且写审计（R-32）；重复处理会重复解封、重复审计 | IDEM-A（同键回放首次结论） |
| `adminHandleReport` | POST | `/admin/reports/{reportId}/handle` | 处理举报写终态，并可能触发下架联动（R-34 / R-35）；重复处理会重复联动 | IDEM-A（同键回放） |
| `createAppeal` | POST | `/penalties/{penaltyId}/appeals` | 一个处罚**最多一条申诉**（AP-01），且提交会推动处罚 → `APPEALING`（R-32） | IDEM-A |

### 3.1 关键注记

> **注 1（本文档重点，务必实现）**：`acceptSwapProposal` 与 `counterSwapProposal` 为 **IDEM-B（重复即明确失败 409）**。
> 幂等键在这两个接口上的作用**不是回放成功**，而是**防止「重复点击产生两次状态推进」**：
>
> - 同键重放 → 不重复推进状态、不重复生成交换码 / 不重复创建新提议；
> - 不同键再次接受 → `409 SWAP_PROPOSAL_ALREADY_ACCEPTED`（`details.swapCodeId`）；
> - 不同键再次反提案 → `409 SWAP_COUNTER_NOT_ALLOWED`（R-28）或 `409 SWAP_PROPOSAL_ALREADY_FINALIZED`。
>
> 即：**成功只可能发生一次，且第二次不会伪装成成功。**

> **注 2**：`createOrderReview` / `createSwapReview` 的**状态机档位为 IDEM-B**（R-14：同一订单同一方第二次评价明确失败 `409 REVIEW_ALREADY_SUBMITTED`）。此处标记为 IDEM-A 仅指**同键同请求体的回放**语义；二者的区别是「跨键的第二次评价必须失败」，实现时不得把跨键重复也当成幂等成功。

> **注 3**：`adminLiftPenalty` / `adminResolveAppeal` / `adminHandleReport` / `adminRestoreItem` 的**跨键重复调用**会因状态已终结而返回 409（`GOVERNANCE_PENALTY_ALREADY_RESOLVED` / `APPEAL_ALREADY_RESOLVED` / `REPORT_ALREADY_HANDLED` / `ITEM_STATUS_NOT_EDITABLE`）。同键重放则返回首次结论（R-32：处理申诉 IDEM-A）。

> **注 4**：`adminTakedownItem` 的下架动作本身是 IDEM-A（重复下架不产生重复记录，R-35），但其**联动部分**（取消订单 / 整笔作废交换）是 **IDEM-B**（只作废一次，R-24 / R-35）。

> **注 5**：`createItem` 的必填键由 `openapi.yaml` 明示（"同键重复提交只创建一件商品（IDEM-A）"）；`createSwapProposal` 明示「同键重复发起只创建一条提议（IDEM-A）」。

---

## 4. 建议携带（可选）的接口（16 个）

这 16 个接口的**副作用天然幂等**（重复执行结果一致）或已由**终态保护**兜底，因此 `Idempotency-Key` 为可选；但**强烈建议前端统一携带**，以便在弱网重试场景下直接回放首次响应、避免用户看到「其实已成功」的 409。

| operationId | method | path | 建议携带的原因 | 未携带时的兜底机制 |
| --- | --- | --- | --- | --- |
| `updateItem` | PUT | `/items/{itemId}` | 覆盖式保存，重复保存结果一致（幂等副作用） | `COMMON_VERSION_CONFLICT`（乐观锁）+ `ITEM_LOCKED_BY_OCCUPATION` |
| `takeItemOffShelf` | POST | `/items/{itemId}/off-shelf` | 状态改写；重复下架无新副作用（R-11 占用态禁止下架） | 条件更新 `WHERE status='IN_SALE'` + `ITEM_LOCKED_BY_OCCUPATION` |
| `relistItem` | POST | `/items/{itemId}/relist` | 重新上架为状态改写；重复上架结果一致（R-11 / R-35：违规下架不可自行上架） | 条件更新 `WHERE status='OFF_SHELF'` + `ITEM_BANNED_CANNOT_RELIST` |
| `updateItemSwapPreference` | PUT | `/items/{itemId}/swap-preference` | 设置为「接受 / 不接受」为覆盖式语义，重复设置结果一致（R-20） | 天然幂等 |
| `reportItem` | POST | `/items/{itemId}/reports` | 重复举报由唯一索引兜底，返回既有记录（E-14 / C-06） | `uq_reports_pending_reporter_item` → **200 `REPORT_ALREADY_EXISTS`**（视为成功） |
| `addCartItem` | POST | `/cart/items` | 同一商品不重复加入，重复加购保持原条目（R-04，IDEM-A） | `uq_cart_items_user_item` + 返回既有条目 |
| `cancelOrder` | POST | `/orders/{orderId}/cancel` | 仅待付款可取消，重复取消返回成功态（O-03） | 条件更新 `WHERE status='PENDING_PAYMENT'` |
| `selectPickupPoint` | PUT | `/orders/{orderId}/pickup-point` | 覆盖式选择；重复选择同一点无需重复写入（O-05，IDEM-A） | 覆盖写 + 区域校验（R-06） |
| `markOrderDelivered` | POST | `/orders/{orderId}/deliver` | 重复标记交付返回成功态、不重复推进（O-06，IDEM-A） | 条件更新 `WHERE status='PAID'` + 需已选点 |
| `confirmOrderReceipt` | POST | `/orders/{orderId}/confirm-receipt` | 重复确认返回成功态、不重复把商品置 `SOLD`（O-07，IDEM-A） | 条件更新 `WHERE status='DELIVERED'`（商品转 SOLD 幂等） |
| `updateSwapBundle` | PUT | `/swap-bundles/{bundleId}` | 增删物品幂等（R-21：重复添加同一物品不产生重复项） | `uq_swap_bundle_items_bundle_item` |
| `rejectSwapProposal` | POST | `/swap-proposals/{proposalId}/reject` | 重复拒绝返回成功态（S-03，IDEM-A） | 条件更新 `WHERE status='PENDING'` |
| `cancelSwapProposal` | POST | `/swap-proposals/{proposalId}/cancel` | 重复取消返回成功态（S-05，IDEM-A） | 条件更新 `WHERE status='PENDING'` |
| `verifySwapCode` | POST | `/swap-proposals/{proposalId}/swap-code/verify` | **IDEM-B**：R-26 一次性凭证，重复核销必须明确失败；携带键的意义是避免并发双核销时产生歧义 | 条件更新 `WHERE status='ISSUED'` → 409 `SWAP_CODE_ALREADY_VERIFIED` |
| `confirmSwap` | POST | `/swap-proposals/{proposalId}/confirm` | 同一方重复确认返回成功态、不重复计数（S-09 / S-10，IDEM-A） | 条件更新 + 该方确认唯一约束 |
| `markNotificationsRead` | POST | `/notifications/read` | 标记已读天然幂等 | 天然幂等 |

> **统一建议**：前端可对**所有非 GET 写操作**统一注入 `Idempotency-Key`（同一个逻辑操作复用同一键），服务端仅对 §3 的 16 个接口**强制校验**其存在。这样既不增加前端分支复杂度，也避免「可选接口在弱网重试下返回 409 困惑用户」。

---

## 5. 幂等语义三档

档位定义直接引用 [`../state-machines.md`](../state-machines.md) §1.2，全文复用。

| 档位 | 记号 | 含义 | 返回 | 本迭代适用的接口 / 场景 |
| --- | --- | --- | --- | --- |
| 强幂等 | **IDEM-A** | 重复请求返回**与首次相同的结果**（成功），不产生第二次副作用 | 200 / 201 + 相同业务结果 | `createItem`、`submitOrder`、`mockPayOrder`、`createSwapBundle`、`createSwapProposal`、`adminBanUser`、`adminTakedownItem`（下架本身）、`adminRestoreItem`、`adminResolveAppeal`、`adminHandleReport`、`createAppeal`；可选接口中的 `addCartItem`、`cancelOrder`、`selectPickupPoint`、`markOrderDelivered`、`confirmOrderReceipt`、`rejectSwapProposal`、`cancelSwapProposal`、`confirmSwap`、`updateItem`、`takeItemOffShelf`、`relistItem`、`updateItemSwapPreference`、`updateSwapBundle`、`markNotificationsRead`、`reportItem`（幂等 200） |
| 幂等拒绝 | **IDEM-B** | 重复请求**明确失败**，且**不产生副作用** | 409 + **当前状态** | `acceptSwapProposal`、`counterSwapProposal`、`verifySwapCode`、`createOrderReview` / `createSwapReview`（跨键二次评价）、`adminLiftPenalty`（跨键重复解封）、`adminResolveAppeal`（跨键重复处理）、`adminHandleReport`（跨键重复处理）、`adminRestoreItem`（非 `BANNED` 态）；**占用释放**（R-16）、**下单撞占用**（O-12）、**4 个定时任务**（FR-601~FR-604） |
| 终态保护 | **IDEM-C** | 已处于终态时任何写操作失败，**状态不变** | 409 + **当前状态** | 见下表（§5.1） |

### 5.1 各状态机的终态（一律 IDEM-C）

| 状态机 | 终态编码 | 典型错误码 / 表现 |
| --- | --- | --- |
| 商品（Item） | `SOLD`；`BANNED`（需管理员恢复才脱离） | `ITEM_STATUS_NOT_EDITABLE`、`ITEM_LOCKED_BY_OCCUPATION`、`ITEM_BANNED_CANNOT_RELIST` |
| 订单（Order） | `COMPLETED`、`CANCELLED`、`TIMEOUT_CANCELLED` | `ORDER_ALREADY_CANCELLED`、`ORDER_STATUS_NOT_PAYABLE`、`ORDER_TIMEOUT_RELEASED` |
| 交换提议（SwapProposal） | `REJECTED`、`EXPIRED`、`CANCELLED`、`COMPLETED`（`COUNTERED` 为**中间态**，不是终态） | `SWAP_PROPOSAL_ALREADY_FINALIZED`、`SWAP_PROPOSAL_VOIDED` |
| 交换包（SwapBundle） | `COMPLETED`、`RELEASED` | `SWAP_BUNDLE_STATUS_NOT_EDITABLE` |
| 交换码（SwapCode） | `VERIFIED`、`EXPIRED` | `SWAP_CODE_ALREADY_VERIFIED` |
| 评价（Review） | `BOTH_REVIEWED`、`WINDOW_CLOSED` | `REVIEW_WINDOW_CLOSED`、`REVIEW_ALREADY_SUBMITTED` |
| 处罚（Penalty） | `LIFTED`、`EXPIRED`（`APPEALING` 仍属**生效中**） | `GOVERNANCE_PENALTY_ALREADY_RESOLVED` |
| 申诉（Appeal） | `APPROVED`、`REJECTED` | `APPEAL_ALREADY_RESOLVED` |
| 举报（Report） | `HANDLED`、`DISMISSED` | `REPORT_ALREADY_HANDLED` |

> **强制**：所有终态必须**显式保护**（`../state-machines.md` SM-05）。实现时必须在条件更新中把终态排除在 `WHERE status = ?` 之外，不能依赖「业务上不会发生」。

### 5.2 档位与「幂等键回放」的关系（易混淆点）

| 组合 | 表现 |
| --- | --- |
| **同键** + 首次已成功 | 回放首次响应（**IDEM-A 回放**），无论该接口的状态机档位是 A 还是 B |
| **不同键** + 状态机档位 IDEM-A | 返回成功态（第二次是「无副作用的成功」），例如重复确认收货 |
| **不同键** + 状态机档位 IDEM-B | 明确失败 409 + 当前状态，例如再次接受已接受的提议 |
| **不同键** + 已到终态 | **IDEM-C**：409 + 当前状态，例如对已取消订单付款 |

---

## 6. 条件更新与乐观锁（R-18）

### 6.1 `version` 列覆盖范围（12 张可并发写表）

以下 12 张表均含 `version INTEGER NOT NULL DEFAULT 0`，用于乐观锁与条件更新：

| # | 表 | 并发点 |
| --- | --- | --- |
| 1 | `users` | 封禁 / 解封与用户状态改写 |
| 2 | `items` | 占用、释放、上下架、违规处置 |
| 3 | `item_occupations` | 占用建立与释放（`released_at` 为幂等锚点） |
| 4 | `orders` | 付款 vs 超时取消、取消、交付、确认收货 |
| 5 | `swap_bundles` | 包内组成修改 vs 提议占用 |
| 6 | `swap_proposals` | 接受 vs 拒绝 vs 反提案 vs 过期 vs 作废 |
| 7 | `swap_codes` | 并发核销（一次性） |
| 8 | `reviews` | 评价入口关闭与提交并发 |
| 9 | `reports` | 举报处理 vs 联动下架 |
| 10 | `penalties` | 到期自动解封 vs 手动解封 vs 申诉通过 |
| 11 | `appeals` | 重复处理申诉 |
| 12 | `idempotency_records` | 并发同键请求的抢占与推进 |

> 未列入的可并发写表（如 `notifications`、`cart_items`、`audit_logs`）通过**唯一索引**或**追加写语义**保证幂等，不使用乐观锁。

### 6.2 条件更新写法（强制模板）

```sql
-- 模板：状态 + 版本双条件；受影响行数 = 0 → 并发冲突
UPDATE <table>
   SET status = :targetStatus,
       <其它副作用字段> = ...,
       version = version + 1
 WHERE id = :id
   AND status = :expectedStatus
   AND version = :expectedVersion;   -- 无版本输入时可省略，但不得省略 status 条件
```

**示例 1：模拟付款（O-02，R-08）**

```sql
UPDATE orders
   SET status = 'PAID', paid_at = now(), version = version + 1
 WHERE id = :orderId AND status = 'PENDING_PAYMENT';
-- 受影响行数 = 0 → 409 + details.currentStatus
--   currentStatus='TIMEOUT_CANCELLED' → ORDER_TIMEOUT_RELEASED（E-05）
--   currentStatus='CANCELLED'         → ORDER_ALREADY_CANCELLED
--   currentStatus='PAID'              → ORDER_STATUS_NOT_PAYABLE（跨键重复付款）
```

**示例 2：超时取消（O-04，R-07）**

```sql
UPDATE orders
   SET status = 'TIMEOUT_CANCELLED',
       cancelled_at = now(), cancel_reason = 'PAYMENT_TIMEOUT',
       version = version + 1
 WHERE id = :orderId AND status = 'PENDING_PAYMENT' AND now() >= expires_at;
-- 受影响行数 = 0 → 跳过（skipped + 1），绝不触碰已 PAID 的订单
```

**示例 3：整包接受（S-02，R-22）**

```sql
UPDATE swap_proposals
   SET status = 'ACCEPTED', accepted_at = now(), version = version + 1
 WHERE id = :proposalId AND status = 'PENDING';
-- 受影响行数 = 0 → 409 SWAP_PROPOSAL_ALREADY_ACCEPTED / SWAP_PROPOSAL_ALREADY_FINALIZED（E-09）
-- 同一事务内完成：占用校验（全部商品）+ 生成唯一交换码 + 通知
```

**示例 4：交换码核销（S-08，R-26）**

```sql
UPDATE swap_codes
   SET status = 'VERIFIED', verified_at = now(), verified_by = :userId,
       version = version + 1
 WHERE id = :codeId AND status = 'ISSUED';
-- 受影响行数 = 0 → 409 SWAP_CODE_ALREADY_VERIFIED（E-12，details.verifiedAt）
```

**示例 5：解封（P-07，R-31）**

```sql
UPDATE penalties
   SET status = 'LIFTED', lifted_at = now(), lift_source = :source,
       version = version + 1
 WHERE id = :penaltyId AND status IN ('ACTIVE', 'APPEALING');
-- 受影响行数 = 0 → 409 GOVERNANCE_PENALTY_ALREADY_RESOLVED（已解封/已过期）
```

**示例 6：释放占用（R-16，`released_at` 作为幂等锚点）**

```sql
UPDATE item_occupations
   SET released_at = now(), release_reason = :reason, version = version + 1
 WHERE id = :occupationId AND released_at IS NULL;
-- 受影响行数 = 0 → 视为已释放（IDEM-B，无副作用，不报错、不重复通知）
```

### 6.3 冲突响应契约

| 项 | 约定 |
| --- | --- |
| HTTP | **409 Conflict** |
| 错误码 | 优先使用**域错误码**（`ORDER_TIMEOUT_RELEASED`、`ITEM_ALREADY_RESERVED`、`SWAP_PROPOSAL_ALREADY_ACCEPTED`、`SWAP_CODE_ALREADY_VERIFIED` ...）；纯版本号不匹配（无更具体的域状态冲突）用 `COMMON_VERSION_CONFLICT` |
| `details.currentStatus` | **必须**返回数据库中的最新状态 |
| `details.currentVersion` | 使用 `version` 乐观锁的接口**建议**返回 |
| 前端处理 | 刷新详情后**最多重试一次**，禁止无限重试（`error-codes.md` §前端处理 第 4 条） |

### 6.4 跨聚合一致性

| 范围 | 要求 |
| --- | --- |
| 同一聚合内（状态 + 副作用） | **必须同事务** |
| 跨聚合（订单取消 + 商品释放 + 通知、下架 + 订单取消/交换作废） | 允许**最终一致**，但必须有**幂等补偿**与**对账手段** |
| 兜底检测 | `db/verify_schema.sql` 的对账查询（§10 占用互斥、§11 投影一致性、§12 付款记录完整性、§13 业务约束），期望 **0 行** |

---

## 7. 部分唯一索引承载的排他语义

有 7 个（**部分）唯一索引**是排他语义的**数据库级真值来源**。应用层可以校验，但**不得**把它当作唯一防线——并发穿透时只有数据库能兜住。

### 7.1 7 个索引

| # | 索引名 | 定义（WHERE 子句） | 承载的约束 / 规则 |
| --- | --- | --- | --- |
| 1 | `uq_item_occ_active_item` | `ON item_occupations (item_id) WHERE released_at IS NULL` | **C-01 + R-23：一个商品同时最多一个进行中占用**；订单占用（`RESERVED` / `ref_type='ORDER'`）与交换占用（`SWAPPING` / `ref_type='SWAP'`）**互斥** |
| 2 | `uq_swap_bundle_items_active` | `ON swap_bundle_items (item_id) WHERE item_status = 'ACTIVE'` | **SB-03**：同一商品同时**只在一个进行中交换包**内 |
| 3 | `uq_swap_codes_proposal` | `UNIQUE (swap_proposal_id)` | **C-05**：一条提议**最多一个交换码**（R-26：防止重复接受生成第二个码） |
| 4 | `uq_payments_order_succeeded` | `ON payments (order_id) WHERE status = 'SUCCEEDED'` | **R-08**：一单一**成功**付款记录（重复付款不新增，IDEM-A） |
| 5 | `uq_penalties_one_active` | `ON penalties (target_user_id, penalty_type) WHERE status IN ('ACTIVE','APPEALING') AND penalty_type = 'BAN_3D'` | **R-30 / E-13**：同一用户同时**最多一条生效中封禁**（对应 Q-10 假设：拒绝重复封禁） |
| 6 | `uq_reports_pending_reporter_item` | `ON reports (reporter_id, item_id) WHERE status = 'PENDING'` | **C-06**：同一用户对同一商品最多一条**未处理**举报（E-14 幂等 200） |
| 7 | `uq_idempotency_user_key` | `UNIQUE (user_id, idempotency_key)` | **R-17**：同一用户 + 同一幂等键最多一条幂等记录 |

> 另有 2 个**非部分**唯一索引同样承载幂等语义，实现时一并纳入映射表：`uq_reviews_order_reviewer`（`(order_id, reviewer_id) WHERE order_id IS NOT NULL`）与 `uq_reviews_swap_reviewer`（`(swap_proposal_id, reviewer_id) WHERE swap_proposal_id IS NOT NULL`），承载 **R-14 / R-02 / R-03「同一订单同一方只能评一次」**。

### 7.2 违反时的处理（强制）

违反上述任一索引时，PostgreSQL 抛出 **SQLSTATE `23505`（`unique_violation`）**。实现要求：

| 要求 | 说明 |
| --- | --- |
| **必须**捕获 `23505` | 在持久层统一拦截（如 Spring 的 `DataIntegrityViolationException` + `ConstraintViolationException`），按 `constraint name` 分派 |
| **必须**映射为**域 409 错误码** | 参照下表；**严禁**直接冒泡为 `COMMON_INTERNAL_ERROR`（500） |
| **必须**返回当前状态 | 409 响应带 `details.currentStatus`（必要时 `details.currentVersion`），供前端刷新 |
| **禁止**在 23505 后无脑重试业务写 | 23505 表示「排他语义已被满足」，重试不会改变结果；只有**换新幂等键**或**用户修正输入**后才可重试 |
| **禁止**依赖「先 `count(*)` / 先 `SELECT`」判断占用 | 存在竞态窗口（见 §11） |

**约束名 → 域错误码映射表（实现必须照此分派）**

| 违反的索引 | HTTP | 域错误码 | 前端处理 |
| --- | --- | --- | --- |
| `uq_item_occ_active_item`（`ref_type='ORDER'` 竞争） | 409 | `ITEM_ALREADY_RESERVED`（`details.currentStatus='RESERVED'`） | 刷新商品状态；禁用下单/加购按钮 |
| `uq_item_occ_active_item`（`ref_type='SWAP'` 竞争） | 409 | `SWAP_ITEM_OCCUPIED`（`details.occupiedBy='SWAP'`）；订单场景可返回 `ITEM_RESERVED_BY_SWAP` | 刷新商品状态；提示「该商品正在交换中」 |
| `uq_swap_bundle_items_active` | 409 | `SWAP_ITEM_IN_ACTIVE_BUNDLE` | 提示该商品已在其他进行中交换包内 |
| `uq_swap_codes_proposal` | 409 | `SWAP_PROPOSAL_ALREADY_ACCEPTED`（不重复生成交换码，E-09） | 跳转交换码页，不重新生成 |
| `uq_payments_order_succeeded` | 200（IDEM-A 回放）或 409 | 订单已 `PAID` → 回放首次付款结果；若状态不可付款 → `ORDER_STATUS_NOT_PAYABLE` | 按已付款处理，刷新订单 |
| `uq_penalties_one_active` | 409 | `GOVERNANCE_ACTIVE_PENALTY_EXISTS`（`details.activePenaltyId` / `restrictedUntil`，E-13） | 禁用封禁按钮，展示现有处罚与到期时间 |
| `uq_reports_pending_reporter_item` | **200** | `REPORT_ALREADY_EXISTS`（**幂等成功**，返回既有记录，E-14） | 提示「你已举报过该商品」，**按成功处理** |
| `uq_idempotency_user_key` | 非错误 | 不抛给用户；转入幂等判定流程（§2.4 步骤 4） | — |
| `uq_reviews_order_reviewer` / `uq_reviews_swap_reviewer` | 409 | `REVIEW_ALREADY_SUBMITTED`（IDEM-B，R-14） | 展示已提交的评价（只读） |

> **特例说明**：`uq_reports_pending_reporter_item` 与 `uq_payments_order_succeeded` 的语义是「**幂等成功**」而非冲突，因此映射为 200；实现时**不得**因捕获到 23505 就统一返回 409。

---

## 8. 典型并发场景逐一说明

### 8.1 总表

| # | 场景 | 竞争双方 | 数据库层保证 | 失败方返回的错误码 | 前端处理 |
| --- | --- | --- | --- | --- | --- |
| C-01 | **重复点击提交订单（同键）** | 前端两次提交（同一 `Idempotency-Key`） | `uq_idempotency_user_key` + `INSERT ... ON CONFLICT DO NOTHING` 抢占；订单、占用、购物车清理**只由抢占成功者**执行 | **无错误码**：回放首次响应（200/201 `OK`，**E-06**），返回**同一组订单** | 按**成功**处理，跳转首次结果；不重复提示、不重复拆单展示 |
| C-02 | **两个买家抢同一商品下单** | 买家 A vs 买家 B（两个不同用户 → 两个不同键） | `uq_item_occ_active_item`（`WHERE released_at IS NULL`）+ `UPDATE items SET status='RESERVED' WHERE status='IN_SALE'` | **409 `ITEM_ALREADY_RESERVED`**（`details.currentStatus='RESERVED'`）；已被交换占用时为 `ITEM_RESERVED_BY_SWAP`（E-04） | 刷新商品详情；禁用下单/加购按钮；提示「商品已被占用」（R-16） |
| C-03 | **「付款 vs 超时取消」并发** | `mockPayOrder`（买家）vs `runOrderTimeoutReleaseTask`（系统） | 条件更新 `WHERE id=? AND status='PENDING_PAYMENT'`（可带 `version`）；受影响行数 0 = 冲突 | 付款方失败 → **409 `ORDER_TIMEOUT_RELEASED`**（E-05）；超时任务方失败 → `skipped+1`，**已付款订单绝不被取消**（R-07） | 展示「订单已超时」；**停止倒计时**；隐藏付款按钮 |
| C-04 | **两方同时对同一商品发起交换** | 提议方 A vs 提议方 B | `uq_item_occ_active_item`；R-23 一物一占用 | **409 `SWAP_ITEM_OCCUPIED`**（E-10，`details.occupiedBy='SWAP'`） | 刷新商品状态；提示已被占用；保留草稿供换商品重试 |
| C-05 | **「接受 vs 拒绝 vs 72 小时过期」三方竞争** | 目标方 `acceptSwapProposal` / `rejectSwapProposal` vs `runSwapProposalExpireTask` | 条件更新 `WHERE id=? AND status='PENDING'`；交换码由 `uq_swap_codes_proposal` 保证只生成一个 | 后到者 → **409 `SWAP_PROPOSAL_ALREADY_ACCEPTED`** / **`SWAP_PROPOSAL_ALREADY_FINALIZED`**（E-09）；过期任务方 → `skipped+1`，**已 `ACCEPTED` 的提议绝不参与过期**（R-25） | 展示终态与 `terminationReason`；隐藏操作按钮；已接受则跳转交换码页 |
| C-06 | **交换码并发核销** | 交换双方同时调用 `verifySwapCode` | 条件更新 `WHERE id=? AND status='ISSUED'` + `ck_swap_codes_verified_trace`（已核销必须有核销时间与核销人） | **409 `SWAP_CODE_ALREADY_VERIFIED`**（E-12，`details.verifiedAt`） | 提示「交换码已失效」；进入双方确认流程（S-09） |
| C-07 | **「到期自动解封 vs 管理员手动解封 vs 申诉通过」** | `runPenaltyAutoUnbanTask` / `adminLiftPenalty` / `adminResolveAppeal`（结论=通过） | 条件更新 `WHERE id=? AND status IN ('ACTIVE','APPEALING')`；同一处罚**只解封一次**（R-31） | **409 `GOVERNANCE_PENALTY_ALREADY_RESOLVED`**；定时任务侧 → `skipped+1` | 刷新处罚详情；展示 `liftSource`（`SYSTEM_EXPIRY` / `APPEAL_APPROVED` / `ADMIN_MANUAL`）与解封时间 |
| C-08 | **重复封禁同一用户** | 两个管理员 / 同一管理员两次提交（不同键） | `uq_penalties_one_active`（`WHERE status IN ('ACTIVE','APPEALING') AND penalty_type='BAN_3D'`） | **409 `GOVERNANCE_ACTIVE_PENALTY_EXISTS`**（E-13，`details.activePenaltyId` / `restrictedUntil`） | 禁用封禁按钮；展示现有处罚与到期时间（Q-10 假设：拒绝重复封禁） |
| C-09 | **重复举报同一商品** | 同一用户两次举报（同键或异键） | `uq_reports_pending_reporter_item`（`WHERE status='PENDING'`） | **200 `REPORT_ALREADY_EXISTS`** + 既有记录（**E-14 幂等成功，不是错误**） | 提示「你已举报过该商品」；**按成功处理**，不报错、不阻断 |
| C-10 | **定时任务重复执行** | 同一任务的两次调度（重试 / 多实例 / 人工触发） | 全部 **IDEM-B**：条件更新 + 时间阈值（`expires_at` / `ends_at` / `review_deadline`）；重复执行不产生重复副作用 | 内部接口**不返回 409**；响应 `processed` / `skipped`（`ScheduledTaskResult`） | 不涉及前端；**监控要求**：`processed=0 且 skipped=0` 表示无待处理数据；若 `skipped` 长期恒为 0 而 `processed` 恒正，需排查阈值条件（E-16） |
| C-11 | **违规下架联动（R-35）** | `adminTakedownItem` vs（买家付款 / 对方接受提议 / 卖家编辑商品） | 同事务：`items.status → 'BANNED'`；被订单占用 → `orders.status='CANCELLED' AND cancel_reason='ADMIN_TAKEDOWN'`；被交换占用 → `swap_proposals.status='CANCELLED' AND termination_reason='VOID_ITEM_BANNED'`；占用 `released_at` 作为幂等锚点 | 受影响方的后续操作 → **409 `ORDER_ALREADY_CANCELLED`** / **409 `SWAP_PROPOSAL_VOIDED`**（`details.terminationReason='VOID_ITEM_BANNED'`，E-11）；并发接受方 → **409 `SWAP_ITEM_OCCUPIED`** | 展示作废/取消原因；下架响应以 `affectedOrders` / `voidedSwapProposals` 明示影响范围；提示可重新发起 |
| C-12 | **商品占用态与 `items.status` 投影一致性** | 占用方（下单/发起交换）vs 释放方（取消/超时/作废/完成） | **数据库无法用约束表达跨表一致性** → 必须**同事务**写 `item_occupations` 与 `items.status`；兜底对账查询 `db/verify_schema.sql` §11（期望 0 行；DQ-07 设计决策：应用层同事务维护，不建触发器） | 事务任一步失败即**整体回滚**，对外表现为 409（排他冲突）或 5xx（未预期异常），**绝不留下半写状态** | 冲突时刷新详情；若前端读到瞬时不一致，以再次拉取的结果为准 |

### 8.2 C-03「付款 vs 超时取消」的时序（唯一允许的结果组合）

```
时刻 T  订单 PENDING_PAYMENT，expires_at = T+30min
        │
        ├── 请求 A：mockPayOrder ─┐
        │                          ├── 同时对 orders 行发起条件更新
        └── 请求 B：超时任务 ──────┘
                                    │
        ┌───────────────────────────┴───────────────────────────┐
        │ A 先获得行锁：status='PENDING_PAYMENT' 命中 → PAID     │
        │ B 后到：status 已 ='PAID' → 受影响行数 0 → skipped+1   │
        │ 结果：订单 PAID，商品保持 RESERVED（R-07 已付款保护）  │
        └───────────────────────────────────────────────────────┘
        ┌───────────────────────────────────────────────────────┐
        │ B 先获得行锁：now>=expires_at → TIMEOUT_CANCELLED      │
        │              并释放占用（released_at=now, items→IN_SALE）│
        │ A 后到：status='PENDING_PAYMENT' 不命中 → 0 行         │
        │ 结果：订单 TIMEOUT_CANCELLED；A 返回 409 ORDER_TIMEOUT_RELEASED │
        └───────────────────────────────────────────────────────┘
        ❌ 不允许：订单既 PAID 又被取消；商品既被占用又回到 IN_SALE
```

> **核心不变量（R-07）**：`orders.status='PAID'` 的订单**绝不被**超时任务取消。这不是靠「任务跑得早/晚」保证，而是靠超时 SQL 的 `WHERE status='PENDING_PAYMENT'` 保证。

### 8.3 C-06 交换码并发的实现要点

```sql
BEGIN;
-- 1) 一次性核销（唯一真值来源）
UPDATE swap_codes
   SET status = 'VERIFIED', verified_at = now(), verified_by = :userId,
       version = version + 1
 WHERE id = :codeId AND swap_proposal_id = :proposalId AND status = 'ISSUED';
-- 2) 受影响行数 = 1 → 继续；= 0 → 抛 409 SWAP_CODE_ALREADY_VERIFIED
-- 3) 同一事务内：写核销人、通知对方（通知失败不回滚核销，由补偿任务兜底）
COMMIT;
```

> **禁止**先 `SELECT status` 判断再 `UPDATE`：两个并发的核销请求可能都读到 `ISSUED`，各自「判断通过」，最终双核销。

### 8.4 C-11 违规下架联动的实现要点

```sql
BEGIN;
-- 1) 抢占商品下架（I-14 / I-15），重复下架无副作用（IDEM-A）
UPDATE items
   SET status = 'BANNED', banned_reason = :reason, banned_at = now(), banned_by = :adminId,
       version = version + 1
 WHERE id = :itemId AND status <> 'BANNED';
-- 2) 若存在进行中占用（released_at IS NULL），按占用来源联动
--    2a) 订单占用 → 取消订单（O-11）
UPDATE orders
   SET status = 'CANCELLED', cancelled_at = now(),
       cancel_reason = 'ADMIN_TAKEDOWN', version = version + 1
 WHERE id = :orderId AND status = 'PENDING_PAYMENT';
--    2b) 交换占用 → 整笔作废（S-07 / S-13，R-24）
UPDATE swap_proposals
   SET status = 'CANCELLED', termination_reason = 'VOID_ITEM_BANNED',
       terminated_at = now(), version = version + 1
 WHERE id = :proposalId AND status IN ('PENDING', 'ACCEPTED');
-- 3) 释放占用：released_at 作为幂等锚点，重复触发不再改写
UPDATE item_occupations
   SET released_at = now(), release_reason = 'ADMIN_TAKEDOWN', version = version + 1
 WHERE ref_type = 'ORDER' AND ref_id = :orderId AND released_at IS NULL;
-- 4) 仍处于本提议占用的其余商品回到 IN_SALE（被下架的除外）
-- 5) 写审计（R-34）、通知所有受影响方
COMMIT;
```

> **【待确认】（本文件提出，尚未登记问题编号，建议登记为 DQ-26）**：当提议已处于 `ACCEPTED` 阶段（交换码已生成）才发生违规下架时，除提议置 `CANCELLED` 外，`swap_codes` 应一并失效。建议置为 `EXPIRED`（`ck_swap_codes_verified_trace` 已允许「`EXPIRED` 且 `verified_at IS NULL`」的组合）。该口径在 `rules.md` / `schema.md` 中未显式声明，**冻结前请勿固化为对外契约**。

### 8.5 C-10 定时任务的共享要求

| 要求 | 说明 |
| --- | --- |
| 幂等档位 | 4 个任务**全部 IDEM-B**（见 §10） |
| 时间来源 | **服务端时间**（`now()`）；**禁止**信任前端传入的剩余时间（R-19） |
| 边界 | `now() >= expires_at` 即视为超时/过期（避免歧义） |
| 并发跳过 | 条件更新受影响行数 0 → 计入 `skipped`，**不重试、不报错** |
| 通知 | 通知发送必须幂等（同一实体同一事件只发一次），或在事务外由补偿任务保证「至少一次 + 去重」 |
| 观测 | 返回 `processed` / `skipped`；两者长期异常需告警 |

---

## 9. 整包接受与整笔作废的并发语义（R-22 / R-24）

### 9.1 核心不变量

**不存在部分占用、不存在部分接受、不存在部分作废。** 接受与作废在粒度上**严格对称**（`rules.md` §8 依赖检查）。

| 项 | 规则 | 并发含义 |
| --- | --- | --- |
| 接受（R-22） | 接受即接受提议中**全部**目标商品，并占用交换包内**全部**商品 | 逐一抢占**所有**涉及商品的占用；**任一件失败 → 整个事务回滚**，不产生任何占用 |
| 作废（R-24） | 任一涉及商品出现「被其他订单占用 / 被其他交换占用 / 被卖家下架 / 被管理员处置 / 被删除」→ **整笔**作废 | 以条件更新抢占提议行（`WHERE status IN ('PENDING','ACCEPTED')`）；抢占失败方不产生任何副作用 |
| 为什么整笔 | 交换是「包对包」的价值对等行为，缺一件即不成立；部分执行会留下不公平的半成品交易（R-24） | 因此**不允许**「部分成功 + 部分失败」的中间态 |

### 9.2 接受接口的参数约束（OOS-04）

| 项 | 约定 |
| --- | --- |
| 接受的请求体 | **不接受任何物品子集参数**（`acceptSwapProposal` 的请求体为空；见 `api-contract.md` 该行 DTO 为 `—`） |
| 若请求体携带物品列表 | 返回 **422 `SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED`**（OOS-04）；实现可「拒绝」或「忽略」，但**契约推荐拒绝**以暴露前端错误 |
| 前端约束 | **不得**提供物品级勾选 / 逐件接受 UI；只能「整包接受」或「整包拒绝」 |
| 只想要其中一部分怎么办 | ① 拒绝原提议；② 发起**反提案**，在反提案中给出自己想要的组合（R-28，仅一轮） |

### 9.3 整包接受的并发实现要点

```sql
BEGIN;
-- 1) 抢占提议状态（唯一闸门）
UPDATE swap_proposals
   SET status = 'ACCEPTED', accepted_at = now(), version = version + 1
 WHERE id = :proposalId AND status = 'PENDING';
-- 受影响行数 = 0 → 409 SWAP_PROPOSAL_ALREADY_ACCEPTED / SWAP_PROPOSAL_ALREADY_FINALIZED (E-09)

-- 2) 校验并确认全部占用仍属本提议（原子读，用于给出精确错误）
SELECT count(*) FROM item_occupations
 WHERE ref_type = 'SWAP' AND ref_id = :proposalId AND released_at IS NULL;
-- 与提议涉及商品总数不一致 → 整笔失败，回滚 → 409 SWAP_ITEM_OCCUPIED (E-10)

-- 3) 生成唯一交换码（uq_swap_codes_proposal 兜底，重复接受不会生成第二个码）
INSERT INTO swap_codes (swap_proposal_id, code, status, expires_at)
VALUES (:proposalId, :randomCode, 'ISSUED', :expiresAt);
-- 23505 on uq_swap_codes_proposal → 判定为重复接受 → 回滚 → 409 (E-09)

-- 4) 通知发起方、写审计
COMMIT;
```

> 步骤 3 使用 `INSERT`（而非「先查有无码、再插入」）是 **C-05 的关键**：`uq_swap_codes_proposal` 是「一条提议最多一个交换码」的数据库级真值来源。

### 9.4 整笔作废的触发矩阵

| 触发条件 | 提议状态 | 目标状态 | `termination_reason` | 幂等 |
| --- | --- | --- | --- | --- |
| 被其他订单占用 | `PENDING` | `CANCELLED` | `VOID_ITEM_OCCUPIED` | IDEM-B（只作废一次） |
| 被其他交换占用 | `PENDING` | `CANCELLED` | `VOID_ITEM_OCCUPIED` | IDEM-B |
| 被卖家主动下架 | `PENDING` | `CANCELLED` | `VOID_ITEM_OFF_SHELF` | IDEM-B |
| 被管理员违规下架 / 处置（R-35） | `PENDING` 或 `ACCEPTED` | `CANCELLED` | `VOID_ITEM_BANNED` | IDEM-B（S-07 / S-13） |
| 目标方拒绝（对照项） | `PENDING` | `REJECTED` | `TARGET_REJECTED` | IDEM-A（重复拒绝返回成功态） |
| 发起方取消（对照项） | `PENDING` | `CANCELLED` | `PROPOSER_CANCELLED` | IDEM-A |
| 72 小时过期（对照项） | `PENDING` | `EXPIRED` | `EXPIRED_72H` | IDEM-B |

> 作废的**后果一次性完成**：提议 → `CANCELLED`；**全部**占用一次性释放（`released_at` 幂等锚点）；所有涉及商品回到 `IN_SALE`（被下架的保持 `OFF_SHELF` / `BANNED`）；通知双方并说明原因。**重复触发不再二次释放、不再重复通知。**

---

## 10. 定时任务的幂等要求

4 个任务均为**内部接口**（`/internal/tasks/*`，使用内部调用凭证而非用户 JWT），幂等档位**全部 IDEM-B**，统一返回 `ScheduledTaskResult`（`task` / `processed` / `skipped` / `executedAt`）。

| FR | operationId | task 枚举 | 扫描条件 | 频率 | 幂等要点 |
| --- | --- | --- | --- | --- | --- |
| **FR-601**（R-07 / FR-307） | `runOrderTimeoutReleaseTask` | `ORDER_TIMEOUT_RELEASE` | `orders.status = 'PENDING_PAYMENT' AND now() >= orders.expires_at` | **每分钟** | 条件更新 `WHERE status='PENDING_PAYMENT'`；`now() >= expires_at` 即超时；**已 `PAID` 的订单绝不被取消**（付款与超时并发只允许一方成功）；订单置 `TIMEOUT_CANCELLED`（`cancel_reason='PAYMENT_TIMEOUT'`）与商品释放、通知**只执行一次** |
| **FR-602**（R-25 / FR-408） | `runSwapProposalExpireTask` | `SWAP_PROPOSAL_EXPIRE` | `swap_proposals.status = 'PENDING' AND now() >= expires_at` | **每分钟** | 条件更新 `WHERE status='PENDING'`；置 `EXPIRED`（`termination_reason='EXPIRED_72H'`），释放**全部**占用（双方商品 → `IN_SALE`）；**已 `ACCEPTED` 的提议绝不参与过期**（避免打断已进入交付环节的交换）；占用只释放一次、通知只发一次 |
| **FR-603**（R-31 / FR-506） | `runPenaltyAutoUnbanTask` | `PENALTY_AUTO_UNBAN` | `penalties.status IN ('ACTIVE','APPEALING') AND now() >= ends_at` | **每分钟** | 条件更新 `WHERE status IN ('ACTIVE','APPEALING')`；置 `EXPIRED`（`lift_source='SYSTEM_EXPIRY'`），用户恢复 `ACTIVE`；同一处罚**只解封一次**；与「管理员手动解封」「申诉通过」并发时**只允许一方成功**；`APPEALING` 照常到期（处罚不停表，Q-18） |
| **FR-604**（R-14） | `runReviewWindowCloseTask` | `REVIEW_WINDOW_CLOSE` | `review_status <> 'WINDOW_CLOSED' AND now() > review_deadline` | **每小时** | 条件更新 `WHERE review_status <> 'WINDOW_CLOSED'`；置 `WINDOW_CLOSED` 关闭评价入口；重复执行不报错；**不自动生成差评**（R-14 / 假设 A-07） |

### 10.1 定时任务的通用实现要求

| # | 要求 |
| --- | --- |
| 1 | 所有时间判断以**服务端时间**为准；**禁止**信任前端传入的剩余时间（R-19） |
| 2 | 状态推进**必须**使用条件更新；受影响行数 0 → `skipped + 1`，**不重试、不报错** |
| 3 | 每个实体在单次扫描中**只处理一次**（同一任务内按主键去重） |
| 4 | 取批策略建议：`ORDER BY <阈值时间> LIMIT N`，分页推进，避免长事务锁表 |
| 5 | 释放占用必须使用 `released_at IS NULL` 作为条件，天然幂等（IDEM-B） |
| 6 | 通知发送必须去重（同一实体同一事件只发一次），或由补偿任务兜底 |
| 7 | 必须记录审计（系统操作者 `ROLE_SYSTEM` / `actor_id IS NULL`） |
| 8 | **`processed` / `skipped` 是验收指标**：并发用例中「失败方」应体现为 `skipped` 增长 |

> **【待确认】（本文件提出，尚未登记问题编号，建议登记为 DQ-27）**：FR-604 的扫描条件 `review_status <> 'WINDOW_CLOSED'` **未排除 `BOTH_REVIEWED`**，而 `BOTH_REVIEWED` 在 [`../state-machines.md`](../state-machines.md) §8 中已被列为**终态**（R-05 要求 IDEM-C 保护）。若按字面实现，窗口关闭任务会把已是 `BOTH_REVIEWED` 的订单改写为 `WINDOW_CLOSED`，抹掉「双方均已评价」的事实，并违反终态保护。
> **建议口径**：扫描条件改为 `review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED') AND now() > review_deadline`。请需求方确认后再落地。

---

## 11. 禁止的实现方式（反模式）

以下写法**一律禁止**。评审时若发现任一模式，视为**阻塞性缺陷**。

| # | 反模式 | 为什么错 | 正确做法 |
| --- | --- | --- | --- |
| 1 | **先 `SELECT` 再无条件 `UPDATE`**（「先查后改」） | 两条语句之间存在竞态窗口：两个并发请求可能都读到「可占用」，然后都执行成功 | `UPDATE ... WHERE id = ? AND status = ?` + 受影响行数判定（R-18） |
| 2 | 用**前端传入的剩余时间**判断超时/过期 | 客户端时钟可伪造、可回拨；同一订单可能被提前或推迟取消（R-19） | 一律以**服务端时间**与库中 `expires_at` / `ends_at` 比较 |
| 3 | 用 `count(*)`（或先 `SELECT` 台账）判断占用 | 计数与写入之间存在竞态；并发下两个请求都会数到 0 | 依赖部分唯一索引 `uq_item_occ_active_item` + 条件更新（R-16 / R-23） |
| 4 | 把**唯一约束冲突**当成 500 | 23505 是**业务冲突**，不是服务端故障；返回 500 会让前端提示「系统错误」并触发无意义重试 | 按 §7.2 的映射表分派为域 **409**（举报/付款等幂等语义映射为 **200**） |
| 5 | **未加 `version` / 状态条件的整体保存**（UPDATE 全字段覆盖） | 会静默覆盖他人的并发修改（丢失更新） | 条件更新 + `version = version + 1`；冲突返回 `COMMON_VERSION_CONFLICT` |
| 6 | **依赖前端禁用按钮**来防重复提交 | 网络重试、多标签页、脚本调用、页面刷新都能绕过；按钮禁用不是契约 | 服务端幂等键（R-17）+ 状态校验 + 条件更新 |
| 7 | 同键请求**不问请求体**就直接回放首次结果 | 会把 A 操作的成功响应回放给 B 操作，产生「同键异义」的数据污染（E-16） | 比对 `request_hash`（以及 `endpoint`）；不一致 → 409 `COMMON_IDEMPOTENCY_KEY_REUSED` |
| 8 | 业务副作用提交后**再**写幂等记录（两个事务） | 中间崩溃会留下 `IN_PROGRESS` 悬挂记录，或造成「有副作用但无记录」 | 幂等记录写回必须与业务副作用**同事务**（§2.4 步骤 3） |
| 9 | 占用/释放**只写 `item_occupations`，不同步 `items.status`**（或反之） | 跨表不一致无法用约束表达；前端会看到错误的可售状态 | 两者**同事务**写；用 `db/verify_schema.sql` §11 对账 |
| 10 | 接受接口提供**物品级子集参数**，或前端提供逐件勾选 | 违反 R-22 整包接受与 OOS-04；会产生「部分占用」的不公平半成品状态 | 接受接口无物品列表参数；携带则 422 `SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED` |
| 11 | 定时任务用「先查列表、再逐条无条件更新」 | 与用户在途操作（付款 / 接受）并发时会互相覆盖 | 逐条条件更新 + `skipped` 统计（§10） |
| 12 | 在 23505 之后**无脑重试**同一业务写 | 排他语义已满足，重试不会改变结果，只会放大冲突 | 换新幂等键或让用户修正输入后重试 |
| 13 | 对终态实体执行写操作时**只改状态不校验前置状态** | 会破坏 IDEM-C，使终态可回退 | `WHERE status = ?` 中排除全部终态；终态 → 409 + 当前状态 |
| 14 | 并发冲突响应**不带 `details.currentStatus`** | 前端无法刷新到正确状态，只能盲目重试 | 409 必须携带最新状态（必要时 `currentVersion`） |

---

## 12. 测试与验收要求

### 12.1 并发测试（R-18 验证要求）

> **强制**：接口测试必须包含并发用例（**≥2 并发**），断言「**恰好一个成功**」。仅测试串行重复调用**不能**替代并发用例。

| # | 用例 | 并发手法 | 断言 |
| --- | --- | --- | --- |
| T-01 | 两个买家抢同一商品下单 | 两个用户、两个幂等键，同商品，同时发起 `submitOrder` | **恰好 1 个 200/201**；另 1 个 **409 `ITEM_ALREADY_RESERVED`**（或 `ITEM_RESERVED_BY_SWAP`）；库中该商品 `item_occupations` 进行中记录**恰好 1 条** |
| T-02 | 两笔交换抢同一商品 | 两个提议方同时 `createSwapProposal`（或同时 `acceptSwapProposal`） | **恰好 1 个成功**；另 1 个 **409 `SWAP_ITEM_OCCUPIED`**（R-23 明文要求） |
| T-03 | 「付款 vs 超时取消」 | 将订单 `expires_at` 设为已过，同时调用 `mockPayOrder` 与 `runOrderTimeoutReleaseTask` | 结果只能是「`PAID` 且商品 `RESERVED`」**或**「`TIMEOUT_CANCELLED` 且商品 `IN_SALE`」；失败方为 409 `ORDER_TIMEOUT_RELEASED` 或 `skipped+1`；**绝不出现既 PAID 又被取消** |
| T-04 | 「接受 vs 拒绝 vs 过期」 | 三方同时作用于同一 `PENDING` 提议 | **恰好 1 个成功**；其余为 409 `SWAP_PROPOSAL_ALREADY_ACCEPTED` / `SWAP_PROPOSAL_ALREADY_FINALIZED` / `skipped+1`；库中该提议**恰好 1 条** `swap_codes` |
| T-05 | 交换码并发核销 | 双方同时 `verifySwapCode` | **恰好 1 个成功**；另 1 个 409 `SWAP_CODE_ALREADY_VERIFIED` |
| T-06 | 「自动解封 vs 手动解封」 | 同时调用 `adminLiftPenalty` 与 `runPenaltyAutoUnbanTask` | **恰好 1 个生效**；另 1 个 409 `GOVERNANCE_PENALTY_ALREADY_RESOLVED` 或 `skipped+1`；`penalties.status` 只能是 `LIFTED` 或 `EXPIRED` 之一 |
| T-07 | 重复封禁并发 | 两个管理员同时对同一用户 `adminBanUser` | **恰好 1 个 200**；另 1 个 **409 `GOVERNANCE_ACTIVE_PENALTY_EXISTS`**；`penalties` 中生效中 `BAN_3D` **恰好 1 条**（E-13） |

### 12.2 幂等测试

| # | 用例 | 断言 |
| --- | --- | --- |
| T-08 | **同键 + 同请求体 + 同请求体内容**重复请求（对 §3 的 16 个接口各一次；`submitOrder` 必测） | 两次响应 **HTTP 状态码一致**、`code='OK'`、**返回同一 `resource_id`**（`submitOrder` 为同一组订单、同一 `order_no`）；`idempotency_records` 中 `(user_id, idempotency_key)` **只有一条**；业务表未产生第二份记录 |
| T-09 | **同键 + 不同请求体** | 第二次返回 **409 `COMMON_IDEMPOTENCY_KEY_REUSED`**（E-16）；**未执行**第二次业务逻辑（无新资源产生） |
| T-10 | 同键不同用户 | 两者**互不影响**，各自成功（唯一索引含 `user_id`） |
| T-11 | 幂等键缺失（针对「可选」接口） | 业务**仍不产生重复副作用**（靠状态前置校验 + 条件更新兜底），不返回 500 |
| T-12 | 同键 + 首次请求处理中（`IN_PROGRESS`） | 按 **【待确认】DQ-09** 冻结后的口径断言（当前建议：409 `COMMON_REQUEST_IN_PROGRESS`）。**在 DQ-09 关闭前该用例标记为「待定」，不得作为冻结验收项** |
| T-13 | 幂等记录回放一致性 | 回放的 `response_body` 与首次响应**逐字段一致**（含 `resource_id`、`order_no`、`swapCode`） |

### 12.3 部分唯一索引覆盖测试

> **建议对 7 个部分唯一索引各写一个覆盖测试**（§7.1），并额外补充 2 个评价唯一索引。

| # | 索引 | 测试手法 | 期望 |
| --- | --- | --- | --- |
| T-14 | `uq_item_occ_active_item` | 并发构造两条进行中占用（订单 + 交换） | 第二条抛 23505 → 映射 **409**（`ITEM_ALREADY_RESERVED` / `SWAP_ITEM_OCCUPIED`），**不是 500** |
| T-15 | `uq_swap_bundle_items_active` | 同一商品加入两个进行中交换包 | 第二条 → **409 `SWAP_ITEM_IN_ACTIVE_BUNDLE`** |
| T-16 | `uq_swap_codes_proposal` | 重复接受同一提议 / 重复插入交换码 | → **409**（E-09），且交换码仍只有 1 条 |
| T-17 | `uq_payments_order_succeeded` | 重复付款成功写入 | → **幂等成功**（IDEM-A 回放），付款成功记录仍只有 1 条 |
| T-18 | `uq_penalties_one_active` | 并发两次封禁同一用户 | → **409 `GOVERNANCE_ACTIVE_PENALTY_EXISTS`**（E-13）；解封后再次封禁**必须成功**（验证部分索引的 `WHERE` 条件正确） |
| T-19 | `uq_reports_pending_reporter_item` | 同一用户重复举报同一 `PENDING` 商品 | → **200 `REPORT_ALREADY_EXISTS`**（E-14，幂等成功）；举报被 `HANDLED` 后**可再次举报**（验证部分索引语义） |
| T-20 | `uq_idempotency_user_key` | 同键并发抢占 | 只有一个请求执行业务；另一请求回放（或按 DQ-09 处理） |
| T-21 | `uq_reviews_order_reviewer` / `uq_reviews_swap_reviewer` | 同一方重复评价 | → **409 `REVIEW_ALREADY_SUBMITTED`**（IDEM-B） |

### 12.4 一致性与对账验收

| # | 验收项 | 判定标准 |
| --- | --- | --- |
| T-22 | 占用互斥对账（`db/verify_schema.sql` §10） | 并发用例执行后**期望 0 行**（同一商品不得有多个进行中占用） |
| T-23 | **投影一致性对账**（`db/verify_schema.sql` §11） | **期望 0 行**：`RESERVED` 必对应 `ORDER` 占用；`SWAPPING` 必对应 `SWAP` 占用；`IN_SALE` 必无进行中占用 |
| T-24 | 付款记录完整性（`§12`） | **期望 0 行**（`PAID`/`DELIVERED`/`COMPLETED` 订单必有成功付款记录） |
| T-25 | 业务约束对账（`§13`） | **期望 0 行** |
| T-26 | 定时任务重复执行 | 连续执行同一任务 N 次，`processed` 之和等于实际待处理实体数；第二次起 `skipped` 覆盖剩余、`processed=0`；无重复通知、无重复释放 |
| T-27 | R-35 联动 | 下架被订单占用的商品 → 订单 `CANCELLED` + `cancel_reason='ADMIN_TAKEDOWN'` + 商品释放；下架被交换占用的商品 → 提议 `CANCELLED` + `termination_reason='VOID_ITEM_BANNED'` + **全部**涉及商品释放；重复下架不产生第二次联动 |

### 12.5 验收门槛

| 项 | 门槛 |
| --- | --- |
| 并发用例 | §12.1 的 T-01 ~ T-07 **全部通过**，且断言「恰好一个成功」 |
| 幂等用例 | T-08 / T-09 对 16 个必填接口**全部通过** |
| 23505 映射 | T-14 ~ T-21 的每一条冲突**均未**出现 `500 COMMON_INTERNAL_ERROR` |
| 对账 | T-22 ~ T-25 全部**0 行** |
| 待确认项 | T-12 在 DQ-09 关闭前**不作为**冻结验收项；§8.4 与 §10.1 提出的两个【待确认】项需在实现前确认 |

---

## 13. 版本与变更记录

| 版本 | 日期 | 作者 | 变更说明 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 接口契约负责人（待填写姓名） | 首版：确立两层防护（幂等键层 R-17 + 条件更新层 R-18）、`Idempotency-Key` 约定与语义表、16 必填 / 16 可选接口清单、幂等语义三档、`version` 覆盖的 12 张表、7 个部分唯一索引及其 23505 → 域错误码映射、12 个典型并发场景、整包接受与整笔作废（R-22 / R-24）、4 个定时任务的幂等要求、反模式清单、并发与幂等验收要求 | **冻结候选（FROZEN-CANDIDATE）** |

### 13.1 本文档引用的待确认项汇总

| 编号 | 内容 | 影响 |
| --- | --- | --- |
| **DQ-09** | 幂等记录的清理策略、`request_hash` 算法、并发同键时 `IN_PROGRESS` 的处理口径（立即 409 / 等待 / 202） | 阻塞 §2.2 情形 3 与情形 5、§2.4 步骤 4、§12.2 T-12 |
| **A-15**（源 Q-27） | 幂等键由前端生成 UUID v4 | 已按假设落地；若改为服务端下发需重构前端与契约 |
| **A-08**（源 Q-27） | 幂等键记录保留 24 小时 | 已按假设落地；过期清理任务归属仍待 DQ-09 明确 |
| **DQ-07** | 占用态与占用台账的一致性由**应用层同事务**维护（不建触发器） | 直接决定 §8 C-12 与 §12.4 T-23 的验收方式 |
| **Q-10** | 对已在处罚中的用户再次封禁：假设**拒绝** | `uq_penalties_one_active` 已强制；若改为「延长/覆盖」需放宽该部分唯一索引（结构性变更，DQ-05） |
| **Q-08** | 已付款订单是否允许买家自助取消：假设**不允许** | 决定 `cancelOrder` 的 `WHERE status='PENDING_PAYMENT'` 条件 |
| **Q-13 / A-14** | 处罚前已存在的订单/交换，其收尾操作假设**允许** | 决定「处罚校验 + 条件更新」的先后与白名单（R-33） |
| **Q-18** | 申诉中的处罚（`APPEALING`）照常到期 | 决定 FR-603 的扫描条件包含 `APPEALING` |
| **【待确认】（新，建议 DQ-26）** | 已 `ACCEPTED` 阶段发生违规下架时，`swap_codes` 是否一并置 `EXPIRED` | 影响 §8.4 的实现细节 |
| **【待确认】（新，建议 DQ-27）** | FR-604 扫描条件是否应排除 `BOTH_REVIEWED`（终态保护冲突） | 影响 §10 表格中 FR-604 的扫描条件 |

> 本文档为**冻结候选**：待上述待确认项闭环后转为**冻结（FROZEN）**。变更流程见 [`api-freeze.md`](./api-freeze.md)（幂等要求与并发语义属**强冻结**内容，变更需走契约变更流程）。
