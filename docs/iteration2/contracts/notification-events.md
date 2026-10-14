# 迭代二 广义契约 ④：通知事件（Notification Events）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 ④：通知事件 |
| 契约编号 | GC-04 |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— P0 项（OQ-33 / OQ-35）**已裁决并固化**；未裁决项以【待确认】标注 |
| 上游依据 | `docs/iteration1/design/openapi.yaml`（`Notification` / `NotificationType` / `NotificationSettings`）、`docs/iteration1/design/schema.md` §9（`notifications`）、`docs/iteration1/rules.md`（R-07/R-24/R-25/R-30/R-31/R-32/R-34/R-35）、`docs/iteration1/state-machines.md`、`docs/iteration1/design/sequence-diagrams.md`、`docs/iteration1/design/api-open-questions.md`（AQ-01/AQ-15） |
| 适用迭代 | 迭代二及后续 |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
>
> **定位**：定义「什么业务事件产生什么通知、通知长什么样、前端怎么用」。
> 通知是**异步副作用**，不参与业务状态判定 —— 通知失败**不得**回滚业务事务（见 §5.2）。

---

## 0. 字段名以契约原文为准 —— **已裁决（OQ-33 / CQ-06）**

> **裁决（2026-10-13）**：**以 `openapi.yaml` 为唯一真源**。通知 JSON 结构**强制**使用 **`body`**、**`refType`**、**`refId`**。
> 任何文档、口头表述、任务书中的 `content` / `relatedId` **一律不采用**。

### 0.1 强制字段映射关系（前端必须照此实现）

| 任务书 / 口头表述（**不采用**） | **契约字段名（唯一正确）** | 类型 | 说明 |
| --- | --- | --- | --- |
| ~~`content`~~ | **`body`** | `string`（可空） | `notifications.body`（`varchar(500)`）。**响应中被省略即表示无值**（Jackson `NON_NULL`，见 [`data-conventions.md`](./data-conventions.md) §1） |
| ~~`relatedId`~~ | **拆解为两个字段**：**`refType`** + **`refId`** | `string` + `int64`（均可空） | `notifications.ref_type` + `notifications.ref_id`。**两者必须成对出现或成对缺失**（库级约束 `ck_notifications_ref_pair`） |
| `isRead` | **`isRead`** ✅ | `boolean`（必填） | 一致，无需映射 |
| `createdAt` | **`createdAt`** ✅ | `date-time`（必填） | 一致，无需映射 |

> **注意事项（强制）**：**任务书中的字段名与契约不一致，前端必须以后者（`openapi.yaml`）为准。**
> **字段映射关系为：`content` → `body`；`relatedId` → `refType` + `refId`（一个字段拆成两个）。**
> 前端**禁止**定义 `content` / `relatedId` 类型或读取这两个字段（将永远得到 `undefined`）。
>
> **登记状态**：OQ-33 / CQ-06 **已裁决**。本裁决**不修改** `openapi.yaml`（`body` / `refType` / `refId` 本就是契约原文）。
> 结论：**由任务书/文档口径对齐契约**，契约侧零变更。

---

## 1. 数据模型

### 1.1 `notifications` 表结构（来自 `schema.md` §9）

| 字段 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY） |
| `recipient_id` | bigint | 否 | — | **通知接收人**（每个接收人一条记录） |
| `actor_id` | bigint | 是 | — | 触发者；**系统通知为 `NULL`**；`ON DELETE SET NULL` |
| `type` | `notification_type` | 否 | — | 类型枚举（25 个取值，见 §2） |
| `title` | varchar(120) | 否 | — | 标题（**≤120 字符**） |
| `body` | varchar(500) | 是 | — | 正文（**≤500 字符**，可为空） |
| `ref_type` | varchar(32) | 是 | — | 多态业务引用类型（**无外键**，D-03）；用于前端跳转 |
| `ref_id` | bigint | 是 | — | 多态业务引用 ID |
| `is_read` | boolean | 否 | **`false`** | 是否已读 |
| `read_at` | timestamptz | 是 | — | 已读时间 |
| `created_at` | timestamptz | 否 | `now()` | 创建时间 |
| `updated_at` | timestamptz | 否 | `now()` | 更新时间 |
| `created_by` / `updated_by` | bigint | 是 | — | **审计字段，不得出现在响应中** |

**库级约束（不可违背）**

| 约束 | 内容 | 对实现的含义 |
| --- | --- | --- |
| `ck_notifications_read_pair` | `(is_read=false AND read_at IS NULL) OR (is_read=true AND read_at IS NOT NULL)` | **`isRead` 与 `readAt` 必须成对**：标记已读时必须同时写 `read_at` |
| `ck_notifications_ref_pair` | `(ref_type IS NULL AND ref_id IS NULL) OR (ref_type IS NOT NULL AND ref_id IS NOT NULL)` | **`refType` 与 `refId` 必须同时有值或同时为 `null`**；只给一个会违反约束（→ 500） |
| 索引 | `idx_notifications_recipient_all`、`idx_notifications_recipient_unread`、`idx_notifications_ref` | 未读查询与角标查询走 `recipient_unread` |
| 外键 | `recipient_id` → `users.id`（**CASCADE**）；`actor_id` → `users.id`（SET NULL） | 用户注销会级联删除其通知（OOS-13 本迭代不做注销） |

### 1.2 响应 DTO（`Notification` Schema）

| 字段 | 类型 | 必填 | 可为空 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | int64 | ✅ | 否 | 通知 ID |
| `type` | `NotificationType` | ✅ | 否 | 类型枚举 |
| `title` | string | ✅ | 否 | 标题 |
| `body` | string | 否 | **是** | 正文（系统通知可能只有标题） |
| `actor` | `UserProfile` | 否 | **是** | 触发者；**系统通知为 `null`** |
| `refType` | string | 否 | **是** | `ORDER` / `SWAP` / `ITEM` / `PENALTY` / `APPEAL` / `REPORT` / `null` |
| `refId` | int64 | 否 | **是** | 与 `refType` 成对 |
| `isRead` | boolean | ✅ | 否 | 是否已读 |
| `readAt` | date-time | 否 | **是** | 已读时间（`isRead=true` 时必有值） |
| `createdAt` | date-time | ✅ | 否 | 创建时间（ISO 8601 UTC） |

> **`recipientId` 不出现在响应中**（隐含为「我」）。

### 1.3 通知接口（3 个，tag 12）

| operationId | 方法 | 路径 | 权限 | 关键语义 |
| --- | --- | --- | --- | --- |
| `listMyNotifications` | GET | `/api/v1/notifications` | 本人（被处罚用户可访问） | 分页 + `unreadOnly`（默认 `false`）+ `type` 过滤；默认按 `created_at desc` |
| `getUnreadNotificationCount` | GET | `/api/v1/notifications/unread-count` | 本人 | 返回 `unreadCount`（**0 也返回 200**） |
| `markNotificationsRead` | POST | `/api/v1/notifications/read` | 本人 | `ids` 为空/不传/null → **全部标记已读**；幂等（IDEM-A）；成功返回**剩余未读数** |

### 1.4 通知设置（`NotificationSettings`）

| 字段 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `inAppEnabled` | boolean | ✅ | **`true`（默认开启）** | 是否接收站内通知；关闭后**仍保留处罚类强制性通知**（见 §4.2） |
| `emailEnabled` | boolean | 否 | **`false`（恒为 false）** | 迭代一**未实现投递**（Q-05），字段为预留；即使传 `true` 也应归一为 `false` |

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `getMyNotificationSettings` | GET | `/api/v1/users/me/notification-settings` |
| `updateMyNotificationSettings` | PUT | `/api/v1/users/me/notification-settings` |

> **AQ-01 说明**：需求基线无独立 FR，「通知开关」由任务书要求而纳入，登记为 **AQ-01（P0）**。当前默认：**纳入且默认开启**。
> **【待确认 OQ-34】**：`inAppEnabled=false` 时，**非强制**通知还应不应该落库（建议：**落库但不展示**或**不落库**二选一，需明确）。

---

## 2. 通知类型枚举（`NotificationType`，25 个取值）

> **逐字对齐** `V1__init_schema.sql`。前端**必须**按字面量分支；**禁止**依赖 `title` / `body` 文案。

| 分组 | 取值 | 含义 | 典型接收人 |
| --- | --- | --- | --- |
| **订单（7）** | `ORDER_CREATED` | 你的商品有新订单 | 卖家 |
| | `ORDER_PAID` | 买家已付款 | 卖家 |
| | `ORDER_DELIVERED` | 卖家已交付，请确认收货 | 买家 |
| | `ORDER_COMPLETED` | 订单已完成 | 双方 |
| | `ORDER_CANCELLED` | 订单已取消（买家主动） | 双方 |
| | `ORDER_TIMEOUT_CANCELLED` | 订单付款超时已自动取消 | 双方 |
| | `ORDER_ADMIN_CANCELLED` | 订单因商品违规处置被管理员取消 | 双方 |
| **交换（9）** | `SWAP_PROPOSED` | 收到新的交换提议 | 目标方 |
| | `SWAP_ACCEPTED` | 提议已被接受（可线下交换） | 发起方 |
| | `SWAP_REJECTED` | 提议已被拒绝 | 发起方 |
| | `SWAP_COUNTERED` | 收到反提案 | 原发起方 |
| | `SWAP_EXPIRED` | 提议已超时失效（72 小时） | 双方 |
| | `SWAP_CANCELLED` | 提议已被取消 | 另一侧 |
| | `SWAP_VOIDED` | 交换因商品被占用/下架而**整笔作废** | 双方 |
| | `SWAP_COMPLETED` | 交换已完成 | 双方 |
| **处罚（3）** | `PENALTY_BANNED` | 账号已被限制（封禁 3 天） | 被处罚用户 |
| | `PENALTY_LIFTED` | 处罚已解除（申诉通过或管理员手动解封） | 被处罚用户 |
| | `PENALTY_EXPIRED` | 处罚已到期自动解封 | 被处罚用户 |
| **商品（2）** | `ITEM_BANNED` | 你的商品因违规被下架 | 卖家 |
| | `ITEM_RESTORED` | 你的商品已恢复上架 | 卖家 |
| **申诉与举报（3）** | `APPEAL_SUBMITTED` | 有新的申诉待处理 | 管理员 |
| | `APPEAL_RESOLVED` | 申诉已有处理结果 | 申诉人 |
| | `REPORT_HANDLED` | 你的举报已处理 | 举报人 |
| **评价（2）** | `REVIEW_WINDOW_OPENED` | 互评窗口已开启 | 双方 |
| | `REVIEW_WINDOW_CLOSED` | 互评窗口已关闭（不生成差评） | 双方 |

> **枚举命名提醒（来自 `sequence-diagrams.md` 的口语化写法）**：时序图中出现过 `PENALTY_ISSUED`、`APPEAL_APPROVED` 等非契约写法，
> **契约只有 25 个取值**（上表）。实现与前端**必须**以 `openapi.yaml` 的 `NotificationType` 为准。

### 2.1 【未来迭代待定】两个未纳入契约的通知事件 —— **已裁决（OQ-35 / CQ-07）**

> **裁决（2026-10-13）**：**暂不加入 `openapi.yaml`**，作为「**迭代五或未来版本需求**」登记。**迭代二不实现**，前端**不预留** UI 分支。

| 占位类型名（**未定义，禁止使用**） | 事件 | 契约现状 | 迭代二行为 |
| --- | --- | --- | --- |
| `PICKUP_CONFIRMED` | **取货点确认**（买家 `selectPickupPoint` 成功，O-05 / R-06） | `NotificationType` **无对应取值** | **不发通知**。卖家可通过订单详情查看取货点；`markOrderDelivered` 的前置条件 `ORDER_PICKUP_POINT_NOT_SELECTED` 会强制其关注 |
| `NEW_REVIEW` | **收到新评价**（`createOrderReview` / `createSwapReview` 成功） | `NotificationType` **无对应取值** | **不发通知**。被评价人可在订单/交换详情查看；已有 `REVIEW_WINDOW_CLOSED` 作为窗口收敛通知 |

> **占位说明原文**：
> **【未来迭代待定】取货点确认（`PICKUP_CONFIRMED`）、收到新评价（`NEW_REVIEW`）。**
> **这两个事件在契约中没有对应枚举，迭代二暂不实现，待迭代五评价系统完善后统一定义。**
>
> **强制约束**：
> 1. 上表两个**占位类型名不是契约枚举**，前端**禁止**在 `NotificationType` 联合类型或 `switch` 分支中出现；
> 2. 后端**禁止**为这两个事件写入 `notifications` 记录（否则会违反 `notification_type` 枚举约束导致插入失败）；
> 3. 未来若要启用，**必须**走 `api-freeze.md` §4 变更流程（新增枚举值 = 类型 C），并同步更新 `data-conventions.md` §2.9 与本节；
> 4. 在上表被正式纳入契约前，**禁止**用任何"临时枚举值"绕过（如借用 `REVIEW_WINDOW_OPENED`）。

---

## 3. 触发时机矩阵（逐事件）

> **列含义**：
> - **触发点**：状态转换 / 事务内落库位置。
> - **接收人**：每条记录对应一个接收人（「双方」= 落 **2 条**记录）。
> - **`refType`/`refId`**：前端跳转目标。
> - **幂等**：重复执行是否可能重复发通知。

| # | 事件 | 触发点（规则/状态机） | 通知类型 | 接收人 | `refType` / `refId` | `actor` | 幂等要求 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| N-01 | **下单成功** | `submitOrder` 成功，订单 → `PENDING_PAYMENT`（O-01 / R-16） | `ORDER_CREATED` | **卖家** | `ORDER` / `orderId` | 买家 | **IDEM-A**（幂等键命中不重复发） |
| N-02 | **付款成功** | `mockPayOrder` 成功，订单 → `PAID`（O-02 / R-08） | `ORDER_PAID` | **卖家** | `ORDER` / `orderId` | 买家 | **IDEM-A**（重复付款不重复发） |
| N-03 | **买家主动取消** | `cancelOrder` 成功，`PENDING_PAYMENT` → `CANCELLED`（O-03） | `ORDER_CANCELLED` | **买卖双方（2 条）** | `ORDER` / `orderId` | 买家 | IDEM-A |
| N-04 | **付款超时释放** | 定时任务 `runOrderTimeoutReleaseTask`：`PENDING_PAYMENT` → `TIMEOUT_CANCELLED`（O-04 / R-07） | `ORDER_TIMEOUT_CANCELLED` | **买卖双方（2 条）** | `ORDER` / `orderId` | `null`（系统） | **IDEM-B**（重复扫描不重复发） |
| N-05 | **交付** | `markOrderDelivered` 成功，`PAID` → `DELIVERED`（O-06） | `ORDER_DELIVERED` | **买家** | `ORDER` / `orderId` | 卖家 | IDEM-A |
| N-06 | **确认收货 / 订单完成** | `confirmOrderReceipt` 成功，`DELIVERED` → `COMPLETED`（O-07）；同时**开启互评窗口** | `ORDER_COMPLETED` + `REVIEW_WINDOW_OPENED` | **卖家**（完成）+ **双方**（窗口） | `ORDER` / `orderId` | 买家 | **IDEM-A**（不重复置 `SOLD`、不重复发） |
| N-07 | **管理员下架联动取消订单** | `adminTakedownItem` 联动作废：订单 → `CANCELLED`（`cancel_reason=ADMIN_TAKEDOWN`）（O-11 / R-35） | `ORDER_ADMIN_CANCELLED` | **买卖双方（2 条）** | `ORDER` / `orderId` | 管理员 | **IDEM-B**（联动只执行一次） |
| N-08 | **取货点已选择** | `selectPickupPoint` 成功（O-05 / R-06） | ⚠️ **无对应枚举** | — | — | — | 见 §3.1 |
| N-09 | **发起交换提议** | `createSwapProposal` 成功，提议 → `PENDING`；占用双方商品（S-01 / R-23） | `SWAP_PROPOSED` | **目标方** | `SWAP` / `proposalId` | 发起方 | **IDEM-A** |
| N-10 | **整包接受** | `acceptSwapProposal` 成功，`PENDING` → `ACCEPTED`；生成交换码（S-02 / R-22） | `SWAP_ACCEPTED` | **发起方** | `SWAP` / `proposalId` | 目标方 | **IDEM-B**（重复接受失败且**不重复发**） |
| N-11 | **拒绝** | `rejectSwapProposal` 成功，`PENDING` → `REJECTED`；释放占用（S-03） | `SWAP_REJECTED` | **发起方** | `SWAP` / `proposalId` | 目标方 | IDEM-A |
| N-12 | **反提案** | `counterSwapProposal` 成功，原提议 → `COUNTERED`；新建提议（S-04 / R-28 仅一轮） | `SWAP_COUNTERED` | 原**发起方**（即新提议的目标方） | `SWAP` / **新** `proposalId` | 原目标方 | **IDEM-B**（仅一轮，不重复发） |
| N-13 | **发起方取消** | `cancelSwapProposal` 成功，`PENDING` → `CANCELLED`（S-05） | `SWAP_CANCELLED` | **目标方** | `SWAP` / `proposalId` | 发起方 | IDEM-A |
| N-14 | **72 小时过期** | 定时任务 `runSwapProposalExpireTask`：`PENDING` → `EXPIRED`（S-06 / R-25） | `SWAP_EXPIRED` | **双方（2 条）** | `SWAP` / `proposalId` | `null`（系统） | **IDEM-B** |
| N-15 | **整笔作废** | 任一件被其他占用 / 被下架 / 被违规下架 → `CANCELLED`（`termination_reason=VOID_ITEM_*`）（S-07 / S-13 / R-24 / R-35） | `SWAP_VOIDED` | **双方（2 条）** | `SWAP` / `proposalId` | 系统或管理员 | **IDEM-B**（只作废一次、只通知一次） |
| N-16 | **交换完成** | 双方确认齐备，提议 → `COMPLETED`，商品 → `SOLD`；开启互评窗口（S-10 / R-27） | `SWAP_COMPLETED` (+ `REVIEW_WINDOW_OPENED`) | **双方（2 条）** | `SWAP` / `proposalId` | 后确认的一方 | **IDEM-A** |
| N-17 | **封禁** | `adminBanUser` 成功；用户 → `RESTRICTED`（R-30） | `PENALTY_BANNED` | **被处罚用户** | `PENALTY` / `penaltyId` | 管理员 | **IDEM-A**（同键不产生第二条处罚，也不重复发） |
| N-18 | **申诉通过解封** | `adminResolveAppeal` 结论 `APPROVED`；处罚 → `LIFTED`（`lift_source=APPEAL_APPROVED`）（R-32） | `PENALTY_LIFTED` **+** `APPEAL_RESOLVED` | **申诉人（2 条）** | `PENALTY` / `penaltyId`；`APPEAL` / `appealId` | 管理员 | **IDEM-A**（重复处理返回首次结论，不重复发） |
| N-19 | **申诉驳回** | `adminResolveAppeal` 结论 `REJECTED`；处罚恢复 `ACTIVE` 续计到期 | `APPEAL_RESOLVED` | **申诉人** | `APPEAL` / `appealId` | 管理员 | IDEM-A |
| N-20 | **提交申诉** | `createAppeal` 成功；处罚 → `APPEALING`（**处罚仍生效**，P-03） | `APPEAL_SUBMITTED` | **管理员** | `APPEAL` / `appealId` | 申诉人 | **IDEM-A**（一个处罚最多一条申诉） |
| N-21 | **自动解封（到期）** | 定时任务 `runPenaltyAutoUnbanTask`：`ACTIVE`/`APPEALING` 且 `now >= endAt` → `EXPIRED`（R-31） | `PENALTY_EXPIRED` | **被处罚用户** | `PENALTY` / `penaltyId` | `null`（系统） | **IDEM-B** |
| N-22 | **管理员手动解封** | `adminLiftPenalty` 成功；处罚 → `LIFTED`（`ADMIN_MANUAL`）（P-07） | `PENALTY_LIFTED` | **被处罚用户** | `PENALTY` / `penaltyId` | 管理员 | **IDEM-B**（与自动解封并发只一方成功） |
| N-23 | **违规下架商品** | `adminTakedownItem` 成功；商品 → `BANNED`（R-35） | `ITEM_BANNED` | **卖家** | `ITEM` / `itemId` | 管理员 | **IDEM-A**（重复下架不重复发） |
| N-24 | **恢复商品** | `adminRestoreItem` 成功；商品 → `IN_SALE` / `OFF_SHELF` | `ITEM_RESTORED` | **卖家** | `ITEM` / `itemId` | 管理员 | IDEM-A |
| N-25 | **处理举报** | `adminHandleReport` 成功（`HANDLED` / `DISMISSED`） | `REPORT_HANDLED` | **举报人** | `REPORT` / `reportId` | 管理员 | **IDEM-A**（重复处理返回首次结论，不重复发） |
| N-26 | **收到新评价** | `createOrderReview` / `createSwapReview` 成功 | ⚠️ **无对应枚举** | 被评价人 | — | 评价人 | 见 §3.1 |
| N-27 | **互评窗口关闭** | 定时任务 `runReviewWindowCloseTask`（≤7 天窗口到期，A-07） | `REVIEW_WINDOW_CLOSED` | **双方（2 条）** | `ORDER` / `orderId` 或 `SWAP` / `proposalId` | `null`（系统） | 幂等（重复执行无副作用） |

### 3.1 两个「应有但无枚举」的通知 —— **已裁决：迭代二不实现（OQ-35 / CQ-07）**

> **裁决摘要（详见 §2.1）**：**暂不加入 `openapi.yaml`**，登记为**迭代五或未来版本需求**；**迭代二不实现**，前端**不预留** UI 分支。

| # | 业务事件 | 触发点 | 契约现状 | **已裁决的迭代二行为** |
| --- | --- | --- | --- | --- |
| G-1 | **取货点确认**（`selectPickupPoint` 成功，O-05 / R-06） | 买家选定取货点 | `NotificationType` **无对应取值** | **不发通知**。卖家通过订单详情可见取货点；`ORDER_PICKUP_POINT_NOT_SELECTED`（409）会强制卖家关注 |
| G-2 | **收到新评价**（`createOrderReview` / `createSwapReview` 成功） | 任一方提交评价 | `NotificationType` **无对应取值** | **不发通知**。被评价人可在订单/交换详情查看；`REVIEW_WINDOW_CLOSED` 作为窗口收敛通知 |

> **强制约束（裁决后生效）**：
> 1. **后端禁止**为 G-1 / G-2 写入 `notifications` 记录 —— `notification_type` 为库级枚举类型，写入未定义值会**插入失败**并可能导致业务事务异常；
> 2. **前端禁止**为 G-1 / G-2 预留 UI 分支、类型成员或文案；
> 3. 未来启用**必须**走 `api-freeze.md` §4 变更流程（新增枚举值 = 类型 C，需接口负责人 + 后端负责人审批 + 台账登记），并同步更新 `data-conventions.md` §2.9 与本节；
> 4. **禁止**用任何「临时枚举值」代替（如借用 `REVIEW_WINDOW_OPENED` 表达 `NEW_REVIEW`）。

### 3.2 一条重要边界：**通知不参与业务判定**

| 约定 | 说明 |
| --- | --- |
| 通知**不是**状态转换的前置条件 | 任何业务规则（R-07/R-16/R-22/R-23/R-24/R-26/R-27/R-31）**不得**依赖「通知已发送」 |
| 通知**失败不得回滚**业务事务 | 建议实现：通知写在同一事务内（保证一致），但**若违反唯一约束/异常，只记录日志，不抛出**（业务事务必须提交）；**或**在事务提交后异步补发 |
| 通知**不重复**优于**不丢失** | 幂等要求优先：宁可少发一条，也不重复轰炸（见 §3 幂等列） |

---

## 4. 通知开关（User Preference）

### 4.1 开关语义

| 项 | 约定 |
| --- | --- |
| 默认值 | **`inAppEnabled = true`（默认开启）**，与任务书「默认开启」一致 |
| 设置入口 | `GET/PUT /users/me/notification-settings` |
| 修改权限 | **仅本人**（`/users/me` 路径，无跨用户入口）；**被处罚用户可修改**（非交易写入，白名单 R-14） |
| 管理员 | 与普通用户同为「自身设置」；**不得**修改他人设置 |
| `emailEnabled` | 恒为 `false`（迭代一未实现投递）；传 `true` 归一为 `false` |
| 生效时机 | 建议**立即生效**（下一个产生通知的事件开始遵守）；历史通知不受影响 |

### 4.2 强制通知（**不受开关控制**）

| 通知类型 | 是否强制 | 理由 |
| --- | --- | --- |
| `PENALTY_BANNED` | **强制** | 用户必须知道账号被限制及解封时间（P-00-C / E-03） |
| `PENALTY_LIFTED` / `PENALTY_EXPIRED` | **强制** | 用户必须知道何时恢复交易能力 |
| `APPEAL_RESOLVED` | **强制** | 申诉结论必须送达（R-32） |
| `ITEM_BANNED` | **强制** | 卖家必须知道商品被下架及原因（R-35） |
| `ORDER_ADMIN_CANCELLED` / `SWAP_VOIDED` | **强制** | 交易被系统/管理员打断，必须告知受影响方（R-24 / R-35） |
| 其余（订单常规流、交换常规流、评价窗口、举报处理、申诉提交给管理员） | **可关闭** | 受 `inAppEnabled` 控制 |

> **契约明文依据**：`NotificationSettings.inAppEnabled` 描述 —— 「关闭后仍保留**处罚类强制性通知**」。
> **【待确认 OQ-36】**：「强制」清单是否仅限**处罚类**（上表前 3 项），还是包含 ITEM/ORDER/SWAP 的治理类联动（本表 5 项）？本文档建议 5 项全强制。

### 4.3 被处罚用户与通知

| 项 | 约定 |
| --- | --- |
| 查看通知 | **允许**（白名单 R-14） |
| 标记已读 | **允许** |
| 查看 / 修改通知设置 | **允许** |
| 说明 | 通知是**只读 + 设置类**能力，不构成交易写入，故处罚期间不受限 |

---

## 5. 产生方式：同步 or 异步

### 5.1 结论（建议）

| 项 | 约定 |
| --- | --- |
| **迭代二采用：同步落库** | 通知记录在**触发它的同一业务事务内**写入 `notifications` 表 |
| 理由 | ① 实现简单，无需消息队列（OOS-17 不做生产级运维）；② 与业务状态**强一致**：订单取消成功 ⇒ 通知必已落库；③ 便于幂等（同事务内用条件更新 + 唯一约束保证不重复） |
| 代价 | 写路径变长（每条业务写多 1–2 次 INSERT）；通知表增长快 |
| **前端获取方式：轮询** | 建议**轮询** `GET /notifications/unread-count` 更新角标 |
| WebSocket | **迭代二不引入**（OOS-17）；若后续引入，**不改变**通知的数据结构、枚举与触发时机，仅改变**投递方式** |
| 事务边界 | 通知落库**必须**与业务状态变更**同事务**；若通知写入失败，**只记日志不回滚业务**（见 §3.2） |
| 定时任务 | 同样同步落库；`processed` 计数**不含**通知条数 |

### 5.2 轮询约定（前端）

| 项 | 约定 |
| --- | --- |
| 角标接口 | `GET /api/v1/notifications/unread-count` |
| 建议间隔 | 页面可见时 **30 秒**；页面隐藏（`document.hidden`）时**暂停轮询** |
| 触发时机 | ① 进入应用时；② 完成写操作后立即刷新一次（下单、付款、接受/拒绝交换、标记已读）；③ 间隔轮询 |
| 退避策略 | 收到 **429** → 按 `Retry-After` 倍数退避；收到 **401** → 停止轮询并跳登录；连续 3 次 5xx → 退避到 5 分钟 |
| 列表刷新 | 打开通知中心时用 `listMyNotifications` 拉取第 0 页（`size: 20`）；标记已读后**本地乐观更新** + 重新拉角标 |
| 幂等重试 | 标记已读为幂等操作，网络失败可直接重试（**无需**幂等键，该接口未标注必填） |

> **性能提示**：轮询间隔与并发用户数成正比；迭代二为课程/内部系统规模，**30 秒可接受**。若后续压力上升，优先改为**长轮询**或 **SSE**，最后才是 WebSocket。

---

## 6. 通知文案与前端跳转约定

### 6.1 文案约定（`title` / `body`）

| 项 | 约定 |
| --- | --- |
| 语言 | **中文** |
| 长度 | `title` ≤ **120** 字符；`body` ≤ **500** 字符（**库级约束，超出会插入失败**） |
| 生成位置 | **后端生成**（前端**不得**自行拼装业务文案，否则多端不一致） |
| 可读性 | `message` 类文案可优化，但 `type` **不可改**（AD-03 同理） |
| 结构化优先 | 前端**优先**按 `type` + `refType`/`refId` 决定 UI 与跳转；`title`/`body` 仅用于展示 |
| 禁止内容 | 不得包含：联系方式、交换码明文、完整取货点地址、内部 ID 之外的敏感信息 |

### 6.2 跳转映射（`refType` → 前端路由）

| `refType` | 跳转目标（前端路由建议） | 需登录 | 权限不足时的降级 |
| --- | --- | --- | --- |
| `ORDER` | 订单详情页 `/orders/{refId}` | 是 | 提示无权限 → 回订单列表 |
| `SWAP` | 交换提议详情页 `/swap-proposals/{refId}` | 是 | 提示无权限 → 回交换列表 |
| `ITEM` | 商品详情页 `/items/{refId}` | **否**（公开） | 商品不存在（已删）→ 回列表 |
| `PENALTY` | 我的处罚页 `/me/penalties`（或 `/{refId}`） | 是 | 仅本人可见 |
| `APPEAL` | 我的申诉页 `/me/appeals`（或 `/{refId}`） | 是 | 管理员看治理队列 |
| `REPORT` | 举报详情 `/reports/{refId}` | 是 | 举报人或管理员 |
| `null` | **纯文本通知**，不做跳转（系统提示类） | — | — |

> **约束**：`refType`/`refId` **无外键**（D-03），可能指向**已被软删除**的资源。前端**必须**处理「目标不存在」的降级路径（显示「该内容已不可访问」）。

### 6.3 展示约定

| 项 | 约定 |
| --- | --- |
| 未读样式 | `isRead=false` 显示未读圆点/加粗；已读常规展示 |
| 时间展示 | 相对时间（如「3 分钟前」），hover 显示绝对时间（ISO 8601 UTC 转本地） |
| 空状态 | `totalElements === 0` 时展示「暂无通知」（**不要**用 `content.length === 0` 单独判定，避免加载中误判） |
| 角标 | `unreadCount > 99` 显示 `99+` |
| 全部已读 | 调用 `markNotificationsRead`（**不传 `ids`**）；成功后本地全部置为已读 |
| 强制通知 | **无法删除**（AQ-15：通知不支持删除），只能标记已读 |

---

## 7. 示例 JSON

> **重要（OQ-22 裁决的体现）**：由于后端配置 Jackson `NON_NULL`，**值为 `null` 的字段不会出现在 JSON 中**（字段被省略）。
> 因此下列示例中：**没有 `null` 值**；**字段缺失即表示该字段无值**。
> 例如第 2 条通知的 `actor`（系统通知无触发者）与 `readAt`（未读）**直接不出现**，而**不是** `"actor": null`。
> 详见 [`data-conventions.md`](./data-conventions.md) §1。

### 7.1 列表响应（`GET /api/v1/notifications?page=0&size=20`）

```json
{
  "code": "OK",
  "message": "成功",
  "data": {
    "content": [
      {
        "id": 2,
        "type": "ORDER_CREATED",
        "title": "你的商品有新订单",
        "body": "买家已下单，请等待付款。",
        "actor": { "id": 3, "nickname": "鲍勃", "avatarUrl": "/static/avatars/bob.png", "itemsInSaleCount": 0, "ratingCount": 0, "createdAt": "2026-10-02T08:00:00Z" },
        "refType": "ORDER",
        "refId": 2,
        "isRead": false,
        "createdAt": "2026-10-12T13:20:41Z"
      },
      {
        "id": 1,
        "type": "PENALTY_EXPIRED",
        "title": "处罚已到期，账号已恢复",
        "body": "你的账号限制已于 2026-10-11T13:20:41Z 到期自动解除。",
        "refType": "PENALTY",
        "refId": 1,
        "isRead": true,
        "readAt": "2026-10-11T14:00:00Z",
        "createdAt": "2026-10-11T13:20:41Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "traceId": "7f9c1a2b3d4e5f60",
  "timestamp": "2026-10-12T13:21:00Z"
}
```

> **注意（已裁决）**：`body` / `refType` / `refId` / `readAt` / `actor` 为无值时，字段**一定被省略**（Jackson `NON_NULL`）。
> **不再存在「按某种策略决定」的余地**，见 [`data-conventions.md`](./data-conventions.md) §1（**OQ-22 已裁决**）。

### 7.2 角标响应（`GET /api/v1/notifications/unread-count`）

```json
{ "code": "OK", "message": "成功", "data": { "unreadCount": 3 }, "traceId": "7f9c1a2b3d4e5f60", "timestamp": "2026-10-12T13:21:00Z" }
```

### 7.3 标记全部已读（`POST /api/v1/notifications/read`，请求体 `{}` 或 `{"ids": null}`）

> 该接口 `Idempotency-Key` 为**可选**（`IdempotencyKeyOptional`）；成功响应返回**剩余未读数** `UnreadCountResponse`。

```json
{ "code": "OK", "message": "成功", "data": { "unreadCount": 0 }, "traceId": "7f9c1a2b3d4e5f60", "timestamp": "2026-10-12T13:21:05Z" }
```

### 7.4 通知设置（`GET /api/v1/users/me/notification-settings`）

```json
{ "code": "OK", "message": "成功", "data": { "inAppEnabled": true, "emailEnabled": false }, "traceId": "7f9c1a2b3d4e5f60", "timestamp": "2026-10-12T13:21:00Z" }
```

---

## 8. 实现检查清单（后端 / 前端）

| # | 检查项 | 责任方 |
| --- | --- | --- |
| 1 | `refType` 与 `refId` **成对**写入（违反 `ck_notifications_ref_pair` 会 500） | 后端 |
| 2 | `isRead` 与 `readAt` **成对**写入（违反 `ck_notifications_read_pair` 会 500） | 后端 |
| 3 | `title` ≤120、`body` ≤500（超出插入失败） | 后端 |
| 4 | 通知落库在**同一业务事务**内，且**不回滚**业务（异常只记日志） | 后端 |
| 5 | 幂等：重复请求**不重复发**（见 §3 幂等列） | 后端 |
| 6 | 强制通知（§4.2）**不受** `inAppEnabled` 影响 | 后端 |
| 7 | `markNotificationsRead` 的 `ids` 为空/null/不传 → **全部已读**（契约明文） | 后端 |
| 8 | `getUnreadNotificationCount` 在 0 时返回 `200` + `unreadCount: 0` | 后端 |
| 9 | 用户只能读写**自己的**通知（`recipient_id` 过滤） | 后端 |
| 10 | 按 `type` 分支渲染；**不匹配** `title`/`body` 文案 | 前端 |
| 11 | `refType` **字段缺失**（无值）时**不做跳转**；跳转目标 404 时降级提示。**禁止**用 `refType === null` 判定（恒为 `false`） | 前端 |
| 12 | 轮询遵循 §5.2（30 秒 / 隐藏暂停 / 429 退避 / 401 停轮询） | 前端 |
| 13 | 角标数用 `unreadCount`，不用本地列表长度推算 | 前端 |
| 14 | 强制通知的 UI 不提供「关闭」入口 | 前端 |
| 15 | **不得为 §2.1 的 `PICKUP_CONFIRMED` / `NEW_REVIEW` 预留类型成员或 UI 分支**（**OQ-35 已裁决**） | 前端 |
| 16 | **不得**为取货点确认、收到新评价写入通知记录（**OQ-35 已裁决**） | 后端 |
| 17 | 字段名**只能**使用 `body` / `refType` / `refId`；**禁止**出现 `content` / `relatedId`（**OQ-33 已裁决**） | 前后端 |

---

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **执行 P0 裁决并固化**：① **OQ-33 / CQ-06 已裁决** —— 通知 JSON **强制**使用 `body` / `refType` / `refId`，新增强制映射关系（`content`→`body`，`relatedId`→`refType`+`refId`），禁止前端定义 `content`/`relatedId`；② **OQ-35 / CQ-07 已裁决** —— 取货点确认（`PICKUP_CONFIRMED`）与收到新评价（`NEW_REVIEW`）**迭代二不实现**，登记为迭代五或未来版本需求，前后端**均禁止**为其写通知或预留 UI 分支；③ 示例 JSON 按 **OQ-22 裁决**改写为「`null` 字段被省略」形态；④ 检查清单由 14 项扩至 17 项 | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：字段名以契约原文为准（`body`/`refId`）、`notifications` 表结构与库级约束、25 个 `NotificationType` 取值、27 条触发时机矩阵（含 2 个契约缺口）、通知开关与强制通知清单、同步落库 + 前端轮询约定、跳转映射与示例 JSON、实现检查清单 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
