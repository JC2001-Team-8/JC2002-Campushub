# 迭代二 广义契约 ②：数据约定（Data Conventions）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 ②：数据约定 |
| 契约编号 | GC-02 |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— P0 项（OQ-22 / OQ-23）**已裁决并固化**；未裁决项以【待确认】标注 |
| 上游依据 | `docs/iteration1/design/openapi.yaml`（冻结候选）、`docs/iteration1/design/schema.md`、`docs/iteration1/rules.md`、`docs/iteration1/state-machines.md`、`docs/iteration1/glossary-assumptions-questions.md`（A-05 / A-20） |
| 适用迭代 | 迭代二及后续 |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
>
> **定位**：定义「一条数据在 JSON 里长什么样」。`openapi.yaml` 只说字段类型，本文档说**序列化行为**：
> 空值怎么表达、枚举有哪些取值、金额怎么传、时间用什么格式、ID 多长、分页排序怎么约定、文件怎么传。
>
> **权威顺序**：`openapi.yaml` > 本文档 > 其它约定。冲突时**登记到 [`open-questions.md`](./open-questions.md)**，不修改上游。

---

## 1. 空值策略（Null Policy）—— **已裁决（OQ-22）**

> **裁决（2026-10-13）**：**服从运行时现状**。后端全局配置了 Jackson `NON_NULL`，**当字段值为 `null` 时，该字段会从 JSON 响应中省略**。
> 本条**不再讨论**、**不再标注【待确认】**；前端按 §1.3 的强制约定做容错。

### 1.1 裁决结论（唯一权威表述）

> **由于后端全局配置了 Jackson `NON_NULL`（`application.yml` `spring.jackson.default-property-inclusion: non_null` + `JacksonConfig` `Include.NON_NULL`），当字段值为 `null` 时，该字段会从 JSON 响应中省略。前端 TypeScript 类型应将相应字段标记为可选（`?`），并针对字段缺失做容错处理。**

| 层次 | **裁决后的唯一约定** | 状态 |
| --- | --- | --- |
| **序列化层** | **`null` 字段在 JSON 响应中被省略（字段不返回）**。前端**必须**把可能为 `null` 的字段视为「**可能不存在**」 | ✅ **已裁决** |
| **语义层** | **`null` 表达「无值」，`[]` 表达「空集合」，二者不混用**；但由于 `NON_NULL`，响应中**看不到 `null`**，只能看到「字段缺失」与「`[]`」两种形态 | ✅ **已裁决** |

### 1.2 裁决依据

| # | 依据 |
| --- | --- |
| 1 | **服从既有实现**：`application.yml` 与 `JacksonConfig` 已在迭代一交付并冻结，**修改它们属配置变更**，本次裁决明确「**服从运行时现状，不修改代码**」 |
| 2 | **改动成本**：改全局序列化策略会波及全部 99 个 schema 的已实现响应，需全量回归，收益与成本不匹配 |
| 3 | **工程上可完全补偿**：只要前端类型生成器配置为「可选」且 `required` 字段恒有值（§1.3），省略与返回 `null` 在调用方是**等价**的 |
| 4 | **传输体积**：省略 `null` 可减少约 20–30% 的响应体积 |

### 1.3 强制补偿约定（**取消全部"建议"语气，以下为必须执行**）

> 服从 `NON_NULL` 的前提是**同时**满足以下全部 5 条。任何一条不满足即视为契约缺陷，必须登记。

| # | 强制约定 | 责任方 |
| --- | --- | --- |
| C1 | **所有 `required` 字段永远不得为 `null`**；后端必须保证 `required` 字段恒有值（否则字段会消失，直接违反 `openapi.yaml` 的 `required`） | 后端 |
| C2 | **前端 TS 类型必须把可为空的字段标记为可选（`?`）**，即 `field?: T`（**不是** `field: T \| null`）；**禁止**使用非空断言 `field!` 访问这些字段 | 前端 |
| C3 | **必须针对字段缺失做容错处理**：读取前判空（`if (obj.field)` / `obj.field ?? fallback` / 可选链 `obj.field?.x`），**不得**假设字段一定存在 | 前端 |
| C4 | 判定「无数据」**必须**用 `field === undefined` 或 `!('field' in obj)`；**禁止**用 `field === null` 作为判定条件（该判断永远为 `false`） | 前端 |
| C5 | 空集合（`[]`）**必须返回**，不得被省略 —— 当前配置为 `NON_NULL`（**非** `NON_EMPTY`），空数组/空对象**正常返回**；`PageResult.content` 在空页时**必须是 `[]`** | 后端 |

### 1.4 各契约文档的联动写法（**已同步**）

| 文档 | 写法 |
| --- | --- |
| 本文档 §1 | 裁决结论 + 5 条强制补偿约定 |
| [`notification-events.md`](./notification-events.md) §6.3 / §7 | 示例 JSON 中**不出现** `null` 字段（如 `actor`、`refType`、`body`、`readAt` 缺失即表示无值） |
| [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §3 | 「响应侧空值边界」按「字段缺失」表述 |
| [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) §2.2 / §4.2 | 类型生成为可选字段；契约测试**不比对运行时字段存在性** |

### 1.5 请求方向的空值语义

| 场景 | 约定 | 依据 |
| --- | --- | --- |
| 可选字段**未传** | **保持原值不变**（部分更新 / PATCH 语义） | `defaults-and-boundaries.md` §0.2 P1 |
| 可选字段**显式 `null`** | **写入 `NULL`（清空）** —— 仅限 `nullable: true` 的字段 | 同上 |
| 必填字段传 `null` 或未传 | `400 VALIDATION_INVALID_PARAMETER` | 同上 P2 |
| 数组字段传 `null` | `400`（数组字段不接受 `null`；清空语义用 `[]`，而 `[]` 是否合法由各字段的 `minItems` 决定） | 同上 |

---

## 2. 枚举字典（Enum Dictionary）—— 口径 **已裁决（OQ-23）**

### 2.1 总览与一致性声明

> **裁决（2026-10-13）**：**保留 `openapi.yaml` 中 22 个命名枚举的现状**，并定义口径为「**21 个业务字典 + 1 个系统级枚举（`UserRole`）**」。本条**不再标注【待确认】**。

| 项 | 值 |
| --- | --- |
| `openapi.yaml` 中的**命名枚举 Schema** | **共 22 个** |
| 其中 **业务字典枚举** | **21 个**（在 `glossary-assumptions-questions.md` §1.2 的术语/字典中定义，覆盖账户状态、区域、商品、订单、交换、评价、举报、治理、申诉、通知） |
| 其中 **系统级枚举** | **1 个**：`UserRole`（`USER` / `ADMIN`），仅用于**权限校验**，不属于业务字典 |
| 其他**内联枚举**（未命名 Schema，字面量集中定义） | **7 组**，见 §2.10 |
| 来源 | **逐字对齐** `V1__init_schema.sql`（AD-09：21 个业务字典与库级枚举类型逐一一致） |
| 前端约束 | **必须按字面量字符串比较**；**禁止**硬编码中文名做逻辑判断（中文名仅用于展示） |
| 未知值处理 | 收到未知枚举值时，前端**不得崩溃**：展示为「未知状态」并隐藏写操作按钮；后端对未知枚举入参返回 `400 VALIDATION_INVALID_PARAMETER` |

**计数恒等式（口径唯一）**

```text
22 个命名枚举 = 21 个业务字典枚举 + 1 个系统级枚举（UserRole）
```

> **对历史数字的处置**（**只在本目录说明，不修改上游**）：
> · `api-open-questions.md` AD-09 的「**21/21**」= **21 个业务字典**与 `V1__init_schema.sql` 的枚举类型逐一一致，**正确**；
> · `controller-skeleton-verification.md` 的「**22 个枚举**」= **22 个命名枚举 Schema**（含 `UserRole`），**同样正确**；
> · 两个数字**并不矛盾**，差异仅为「是否把系统级枚举 `UserRole` 计入业务字典」。详见 [`open-questions.md`](./open-questions.md) CQ-05（**已裁决：口径差异，非矛盾**）。

### 2.2 账户与用户（1 个业务 + 1 个系统级）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 1 | `UserRole` | `USER` | 普通注册用户 | `CurrentUserResponse.role`、`AdminUserSummary.role` |
| | | `ADMIN` | 管理员（**不参与交易**，P-00-A） | 同上 |
| 2 | `UserStatus` | `ACTIVE` | 账号正常 | `CurrentUserResponse.status`、`AdminUserSummary.status` |
| | | `RESTRICTED` | 处于生效中处罚（交易写操作被禁，R-33） | 同上；`restrictedUntil` 此时必有值 |

### 2.3 区域与字典（2）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 3 | `ZoneType` | `CAMPUS` | 校区 | `TradeZone.zoneType` |
| | | `COMMUNITY` | 周边社区 | 同上 |
| 4 | `PickupPointType` | `LIBRARY` | 图书馆门口 | `PickupPoint.pointType` |
| | | `CANTEEN` | 食堂门口 | 同上 |
| | | `TEACHING` | 教学楼大厅 | 同上 |
| | | `DORM` | 宿舍区门口 | 同上 |
| | | `GATE` | 校门口 | 同上 |
| | | `SQUARE` | 社区广场 | 同上 |

> **区域与取货点的具体条目为占位数据【待确认】**：`CAMPUS_A` / `COMMUNITY_A1` / `PICKUP_LIBRARY_A`（Q-01/Q-02/Q-03）。
> 占位数据替换时**不得改变字段名、类型与路径**。

### 2.4 商品（2）

| # | 枚举 | 取值 | 中文含义 | 终态？ | 用于字段 |
| --- | --- | --- | --- | --- | --- |
| 5 | `ItemStatus` | `DRAFT` | 草稿（**迭代一不使用**，发布即上架，Q-14） | 否 | `ItemSummary.status`、`ItemDetail.status`、`TakedownItemResponse.status` |
| | | `IN_SALE` | 在售 | 否 | 同上 |
| | | `RESERVED` | 已被**订单**占用 | 否（占用态） | 同上 |
| | | `SWAPPING` | 已被**交换**占用 | 否（占用态） | 同上 |
| | | `SOLD` | 已售出 / 交换完成 | **是** | 同上 |
| | | `OFF_SHELF` | 卖家主动下架 | 否 | 同上 |
| | | `BANNED` | 违规下架（**卖家不可自行重新上架**，I-16） | **是** | 同上 |
| 6 | `SwapPreference` | `ACCEPTING` | 接受交换 | — | `CreateItemRequest.swapPreference`、`ItemSummary.swapPreference`、`UpdateSwapPreferenceRequest.swapPreference`、`listItems.swapPreference` |
| | | `NOT_ACCEPTING` | 不接受交换（**DB 默认值**） | — | 同上 |

> **占用互斥**：`RESERVED` 与 `SWAPPING` 为互斥占用态（R-23），由部分唯一索引在库级保证（C-01）。

### 2.5 订单（2）

| # | 枚举 | 取值 | 中文含义 | 终态？ | 用于字段 |
| --- | --- | --- | --- | --- | --- |
| 7 | `OrderStatus` | `PENDING_PAYMENT` | 待付款（30 分钟倒计时中，A-01） | 否 | `OrderSummary.status`、`OrderDetail.status`、`PaymentResult.status` |
| | | `PAID` | 已付款 / 待取货 | 否 | 同上 |
| | | `DELIVERED` | 已交付待确认 | 否 | 同上 |
| | | `COMPLETED` | 已完成（买家确认收货） | **是** | 同上 |
| | | `CANCELLED` | 已取消（买家主动 / 管理员联动） | **是** | 同上 |
| | | `TIMEOUT_CANCELLED` | 超时取消（系统） | **是** | 同上 |
| 8 | `OrderCancelReason` | `BUYER_CANCELLED` | 买家主动取消（O-03） | — | `OrderDetail.cancelReason` |
| | | `PAYMENT_TIMEOUT` | 付款超时（O-04） | — | 同上 |
| | | `ADMIN_TAKEDOWN` | 管理员下架联动取消（O-11 / R-35） | — | 同上 |

> **本迭代无退款状态**（OOS-14），故枚举中**不含** `REFUNDING` / `REFUNDED`。

### 2.6 交换（6）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 9 | `SwapBundleStatus` | `DRAFT` | 草稿（**DB 默认值**） | `SwapBundleSummary.status`、`SwapBundleDetail.status` |
| | | `PACKED` | 已打包 | 同上 |
| | | `IN_SWAP` | 被进行中提议引用 | 同上 |
| | | `OCCUPIED` | 包内存在被占用物品导致不可用 | 同上 |
| | | `RELEASED` | 已释放（可复用） | 同上 |
| | | `COMPLETED` | 已完成 | 同上 |
| 10 | `BundleItemStatus` | `ACTIVE` | 生效（**仅 `ACTIVE` 参与唯一约束**） | `SwapBundleItem.itemStatus` |
| | | `RELEASED` | 已释放 | 同上 |
| | | `COMPLETED` | 已完成 | 同上 |
| 11 | `SwapProposalStatus` | `PENDING` | 待处理（72 小时倒计时中，A-02） | `SwapProposalSummary.status`、`SwapProposalDetail.status` |
| | | `ACCEPTED` | 已整包接受（生成一次性交换码） | 同上 |
| | | `REJECTED` | 已拒绝 | 同上 |
| | | `COUNTERED` | 已被反提案（**中间态**，仅一轮，R-28） | 同上 |
| | | `EXPIRED` | 已过期（72 小时） | 同上 |
| | | `CANCELLED` | 已取消（含**整笔作废**） | 同上 |
| | | `COMPLETED` | 交换完成（双方确认齐备） | 同上 |
| 12 | `SwapTerminationReason` | `TARGET_REJECTED` | 目标方拒绝（S-03） | `SwapProposalDetail.terminationReason` |
| | | `PROPOSER_CANCELLED` | 发起方取消（S-05） | 同上 |
| | | `EXPIRED_72H` | 72 小时超时（S-06） | 同上 |
| | | `VOID_ITEM_OCCUPIED` | **整笔作废**：任一件被其他占用（R-24 / S-07） | 同上 |
| | | `VOID_ITEM_OFF_SHELF` | **整笔作废**：任一件被卖家下架（R-24 / S-07） | 同上 |
| | | `VOID_ITEM_BANNED` | **整笔作废**：任一件被违规下架（R-24 / R-35 / S-13） | 同上 |
| | | `COUNTERED` | 已被反提案（S-04） | 同上 |
| 13 | `SwapCodeStatus` | `ISSUED` | 已签发（未核销） | `SwapCode.status` |
| | | `VERIFIED` | 已核销（**一次性，不可再次核销**，R-26） | 同上 |
| | | `EXPIRED` | 已失效 | 同上 |
| 14 | `ReviewRole` | `BUYER` | 评价人角色 = 买家 | `Review.reviewerRole` |
| | | `SELLER` | 评价人角色 = 卖家 | 同上 |

### 2.7 评价（1）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 15 | `ReviewStatus` | `OPEN` | 可评价（**DB 默认值**，互评窗口开启） | `ReviewStatusResponse.reviewStatus` |
| | | `BUYER_REVIEWED` | 买家已评 | 同上 |
| | | `SELLER_REVIEWED` | 卖家已评 | 同上 |
| | | `BOTH_REVIEWED` | 双方已评（**终态**） | 同上 |
| | | `WINDOW_CLOSED` | 窗口关闭（7 天超期，A-07；**不自动生成差评**） | 同上 |

### 2.8 举报（2）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 16 | `ReportStatus` | `PENDING` | 待处理 | `Report.status` |
| | | `HANDLED` | 已受理 | 同上 |
| | | `DISMISSED` | 已驳回 | 同上 |
| 17 | `ReportReason` | `FAKE_INFO` | 虚假信息 | `CreateReportRequest.reason`、`Report.reason` |
| | | `PROHIBITED_ITEM` | 违禁物品 | 同上 |
| | | `WRONG_CATEGORY` | 分类错误 | 同上 |
| | | `SUSPECTED_FRAUD` | 疑似诈骗 | 同上 |
| | | `OTHER` | 其他 | 同上 |

### 2.9 治理与申诉（4 + 通知 1）

| # | 枚举 | 取值 | 中文含义 | 用于字段 |
| --- | --- | --- | --- | --- |
| 18 | `PenaltyType` | `BAN_3D` | 封禁 3 天（固定 72 小时，A-03） | `Penalty.penaltyType` |
| | | `ITEM_TAKEDOWN` | 违规下架处置（仅举证，不影响交易能力） | 同上 |
| 19 | `PenaltyStatus` | `ACTIVE` | 生效中 | `Penalty.status` |
| | | `APPEALING` | 申诉中（**处罚仍生效**，不停表，P-03） | 同上 |
| | | `LIFTED` | 已解封 | 同上 |
| | | `EXPIRED` | 已过期（系统自动解封，R-31） | 同上 |
| 20 | `PenaltyLiftSource` | `SYSTEM_EXPIRY` | 到期自动解封（P-04） | `Penalty.liftSource` |
| | | `APPEAL_APPROVED` | 申诉通过解封（P-05） | 同上 |
| | | `ADMIN_MANUAL` | 管理员手动解封（P-07） | 同上 |
| 21 | `AppealStatus` | `SUBMITTED` | 已提交 | `Appeal.status` |
| | | `APPROVED` | 申诉通过（立即解封） | 同上 |
| | | `REJECTED` | 申诉驳回（处罚继续至 `endAt`） | 同上 |
| 22 | `NotificationType` | — | **25 个取值，见 [`notification-events.md`](./notification-events.md) §2** | `Notification.type`、`listMyNotifications.type` |

### 2.10 内联枚举（未命名 Schema，7 组）

> 这些枚举**同样属契约**，与命名枚举同等强度。前端**必须**按字面量处理。

| # | 出现位置 | 取值 | 含义 |
| --- | --- | --- | --- |
| I-1 | `ItemDetail.swapDisplayStatus` | `可交换` / `暂不交换` / `交换中` / `已交换` | **唯一的中文枚举**，由 `swapPreference` + `status` 推导，**非数据库字段**；前端**不得**据此做业务判断 |
| I-2 | `CartItem.quantity` | `1`（字面量） | 数量固定为 1（A-16） |
| I-3 | `CartItem.unavailableReason` | `RESERVED_BY_ORDER` / `RESERVED_BY_SWAP` / `SOLD` / `OFF_SHELF` / `BANNED` / `DELETED` / `null` | 不可结算原因，仅当 `purchasable=false` 时非空 |
| I-4 | `OrderSummary.role` / `OrderDetail.role` / `PaymentResult.channel` | `BUYER`/`SELLER`；`MOCK` | 关系角色（AD-10）；付款渠道恒为 `MOCK` |
| I-5 | `SwapProposalSummary.role` / `SwapProposalDetail.role` | `PROPOSER` / `TARGET` | 当前用户在该提议中的关系角色 |
| I-6 | `AuditLog.actorRole` / `AuditLog.targetType` | `ADMIN`/`SYSTEM`/`USER`/`null`；`ITEM`/`USER`/`PENALTY`/`APPEAL`/`REPORT`/`ORDER`/`SWAP_PROPOSAL` | 审计主体与对象类型；`actorId=null` 表示系统任务 |
| I-7 | `Notification.refType` / `adminListDictionary.dictionaryType` / `ScheduledTaskResult.task` / `uploadImage.purpose` / `TakedownItemRequest` 相关 | `ORDER`/`SWAP`/`ITEM`/`PENALTY`/`APPEAL`/`REPORT`/`null`；`CATEGORIES`/`ITEM_CONDITIONS`/`TRADE_ZONES`/`PICKUP_POINTS`；`ORDER_TIMEOUT_RELEASE`/`SWAP_PROPOSAL_EXPIRE`/`PENALTY_AUTO_UNBAN`/`REVIEW_WINDOW_CLOSE`；`ITEM_IMAGE`/`AVATAR` | 多态引用、字典类型、定时任务名、上传用途 |

---

## 3. 金额（Money）

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 类型 | **`DecimalString`**：JSON **字符串**，形如 `"120.00"` | AD-04 |
| 单位 | **人民币元（CNY）**；`currency` 字段固定返回 `"CNY"` | `ItemSummary.currency` |
| 精度 | **固定 2 位小数**；整数部分 1–10 位 | `pattern: ^\d{1,10}(\.\d{1,2})?$` |
| **禁止** | **禁止使用 JSON number**（浮点误差）；**禁止**前端用 `parseFloat` 后参与金额比较/累加展示以外的运算 | AD-04 |
| 取值范围 | `>= 0`；**无上限**（Q-22 假设无上限）；DB 为 `numeric(12,2)` → 实际上限 `9999999999.99` | `schema.md` `ck_items_price_nonneg` |
| 缺省小数位 | `"120"` / `"120.0"` **合规**（pattern 允许 1–2 位）；建议后端**统一归一为 2 位**（`"120.00"`）输出 | 【待确认 OQ-24】 |
| 展示免责 | 所有金额展示位**必须**同时展示免责声明（商品详情 `priceDisclaimer`、购物车 `priceDisclaimer`、提议 `priceDisclaimer`） | R-13 |
| 涉及字段 | `estimatedPrice`、`priceSnapshot`、`estimatedTotal`、`groupTotal`、`totalAmount`、`offerTotal`、`wantTotal`、`amount`、`minPrice`/`maxPrice`（query） | `openapi.yaml` |
| 快照 | 下单/发起交换时**必须快照**当时估价，商品后续编辑不影响历史订单/交换的展示值 | R-13 |
| 交换包合计 | **仅展示**；**不做等价判定、不做差价计算**（OOS-03） | R-13 |
| 前端建议 | 使用「字符串 + 定点库」（如 `dinero.js` / `decimal.js`）或「分为单位的整数」做运算，避免 `0.1 + 0.2` 类误差 | — |

---

## 4. 时间与跟踪标识（Time & Trace）

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 传输格式 | **ISO 8601，UTC，带 `Z` 后缀**，秒级精度：`2026-10-12T13:20:41Z` | 全局约定 / AD-05 |
| 时区 | **存储与传输 UTC**；业务时区 `Asia/Shanghai`（A-20）仅用于**展示**与业务日切 | A-20 / Q-28 |
| 服务端时间为准 | 所有过期/倒计时判定**不得**信任前端时间 | R-19 |
| 倒计时双字段 | 同时返回 `expiresAt`（绝对）与 `remainingSeconds`（服务端算） | AD-05 |
| 边界 | `now >= expiresAt` 即视为超时/过期 | R-07 / R-25 / R-31 |
| 缺失时间 | `nullable` 时间字段未发生时返回 `null`（如 `paidAt`、`deliveredAt`、`readAt`、`verifiedAt`） | 各 Schema |
| 前端建议 | 统一用 `new Date(iso)` 解析后**按本地时区展示**；**禁止**把 `Z` 剥掉当本地时间 | — |

### 4.1 `traceId`

| 项 | 约定 |
| --- | --- |
| 生成 | 请求头 `X-Trace-Id` 可**可选**传入；未传由**服务端生成**（AQ-16） |
| 一致性 | **响应头 `X-Trace-Id` 与响应体 `ApiResponse.traceId` / `ErrorResponse.traceId` 必须为同一值**，同一次响应内一致 |
| 格式 | 16 位小写十六进制字符串（如 `7f9c1a2b3d4e5f60`）——格式为**建议**，契约仅要求为字符串 |
| 前端要求 | **必须**在错误提示中保留 `traceId`（便于用户报障）；`X-Trace-Id` 已在 CORS 的 `Access-Control-Expose-Headers` 中暴露，前端可读取 |
| 日志 | 前端可在请求头回传上一次的 `X-Trace-Id` 以串联链路（**可选**，非契约要求） |

---

## 5. 标识符（ID）

| 项 | 类型 | 格式 | 说明 |
| --- | --- | --- | --- |
| 全部业务实体主键 | `integer` / `int64` | **十进制数字**（JSON number），如 `7` | `items.id`、`orders.id`、`users.id`、`swapProposals` 等 |
| JS 安全整数 | — | **`int64` 在 JS 中超过 `2^53-1` 会丢精度** | **【待确认 OQ-25】**：当前种子数据量小、ID 远小于 `2^53`，风险低；若未来 ID 超过 `9007199254740991`，需改为字符串传输 |
| 前端建议 | — | 一律按 `number` 处理；**禁止**对 ID 做算术、正则截断或字符串排序 | — |
| 业务单号 | `string` | `orderNo` 形如 `ORD-20261012-0002`；`proposalNo` 形如 `SWP-20261012-0001` | DB `varchar(32)`，**唯一**（`uq_orders_order_no` / `uq_swap_proposals_proposal_no`） |
| 单号不可解析 | — | 前端**不得**从单号反解日期/序号做业务判断，仅用于展示与搜索 | — |
| 交换码 | `string` | 最长 16 字符，示例 `SWP8A2F` | **格式与长度待确认 Q-30 / OQ-15**；**大小写**建议核销时不敏感 |
| 幂等键 | `string` | 长度 **8–64**；建议 **UUID v4** | `Idempotency-Key` 头（A-15）；`minLength: 8`、`maxLength: 64` |
| 内部令牌 | `string` | 请求头 `X-Internal-Token`（`apiKey`） | **仅服务端/定时任务使用，禁止出现在浏览器端**（`api-permission-mapping.md` §6.1） |
| UUID | — | 迭代一**不引入** UUID 主键（全为 `bigint IDENTITY`） | 任务描述中的「UUID」仅适用于**幂等键**，非实体 ID |

---

## 6. 分页与排序（Pagination & Sorting）

| 项 | 约定 |
| --- | --- |
| 请求参数 | `page`（默认 **0**，从 0 起）、`size`（默认 **20**）、`sort`（可重复，`字段,方向`） |
| `size` 范围 | **1–100**；`>100` → `400 VALIDATION_PAGE_SIZE_EXCEEDED`（**不静默截断**） |
| 响应结构 | `PageResult`：`content` / `page` / `size` / `totalElements` / `totalPages` / `first` / `last`（**7 个字段全部为 `required`**） |
| `content` 无数据 | **返回 `[]`**，不得为 `null` |
| `first` / `last` | 首页 `first=true`；空结果集 `first=true` 且 `last=true`，`totalPages=0` |
| `sort` 白名单 | 见 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §1.2；越界 → `400 COMMON_INVALID_SORT_FIELD` |
| 方向 | 小写 `asc` / `desc`；缺省方向按资源默认（多为 `desc`） |
| 超出页数 | 返回 `200` + `content: []`（**不是错误**） |
| 不做 | 不做游标分页（cursor）、不做个性化/推荐排序（OOS-09） |
| 前端建议 | 把分页状态同步到 URL query（便于刷新与分享）；列表切换筛选条件时 `page` **重置为 0** |

---

## 7. 文件上传（File Upload）

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 接口 | `POST /files/images`（`multipart/form-data`，字段名 **`file`**） | `uploadImage` |
| 允许格式 | **JPG / PNG / WebP**（`image/jpeg`、`image/png`、`image/webp`） | A-05 |
| 单张大小 | **≤ 5MB**（`5242880` 字节；DB `ck_item_images_size`） | A-05 |
| 商品图片数量 | **每个商品 1–9 张**（`minItems: 1`、`maxItems: 9`） | A-05 / R-12 |
| 头像数量 | **1 张** | A-05 |
| 上传与绑定分离 | 先 `POST /files/images` 取得 `imageUrl`，再在 `createItem`/`updateItem` 的 `imageUrls` 中引用 | `ItemImageUploadResponse` |
| 排序 | **`imageUrls` 数组下标即 `sort_order`**；**下标 0（第 1 张）为封面**（`sort_order=0`） | AQ-05 / R-12 |
| 更新语义 | `updateItem.imageUrls` 为**全量替换**（不是增量追加）；替换后仍须 **≥1 张** | `UpdateItemRequest` |
| 封面判定 | **最小 `sort_order` 者为封面**；**没有独立 `is_cover` 字段** | 交接文档 §5.5 / AQ-05 |
| 存储 | 服务端**本地目录**，通过静态前缀 `/static/uploads` 访问（**不使用对象存储/CDN**，OOS-18） | `app.storage` |
| 响应字段 | `imageUrl`（可直接用于 `imageUrls`）、`contentType`、`fileSize`（字节） | `ItemImageUploadResponse` |
| 错误码 | `422 FILE_TOO_LARGE`（`details.maxBytes`/`actualBytes`）、`422 FILE_TYPE_NOT_ALLOWED`、`400 FILE_EMPTY`、`422 FILE_COUNT_EXCEEDED` | `error-codes.md` §3.13 |
| 前端建议 | 上传前做**客户端预校验**（类型 + 大小 + 数量），避免无效请求；支持多选后**按选择顺序**提交；拖动排序后**按新顺序重排数组**再提交（无需新接口） | — |
| 【待确认】 | 上传接口的 `purpose`（`ITEM_IMAGE`/`AVATAR`，`default: ITEM_IMAGE`）语义与数量校验归属（上传时校验 vs 绑定时校验）→ **OQ-26** | — |

---

## 8. 通用传输约定速查

| 项 | 约定 |
| --- | --- |
| 基础路径 | `/api/v1` |
| 统一包络 | 所有非 204 响应使用 `ApiResponse`：`code` / `message` / `data` / `traceId` / `timestamp`；成功 `code` 恒为 `"OK"` |
| 失败包络 | `ErrorResponse`：`code` / `message` / `details` / `traceId` / `timestamp`；**业务失败一律用非 2xx HTTP 状态**（AD-01） |
| 错误码 | 格式 `DOMAIN_REASON`；**前端必须按 `code` 分支，禁止匹配 `message`**（AD-03） |
| 脱敏 | 响应中**禁止**出现 `password_hash`、`deleted_at`、`created_by`、`updated_by`、`request_hash`（AD-07 / AD-08） |
| 请求 DTO 与响应 DTO 分离 | 请求体**不得**复用数据库实体；请求侧**不得**可写只读字段（AD-07） |
| 内容类型 | 除 `multipart/form-data`（上传）外，一律 `application/json; charset=UTF-8` |
| 字符编码 | UTF-8；中文直接传输（**不**做 Unicode 转义） |

---

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **执行 P0 裁决并固化**：① **OQ-22 / CQ-01 已裁决** —— 空值策略**服从运行时 Jackson `NON_NULL`**（`null` 字段从响应中省略），前端类型标记为可选 `?` 并做字段缺失容错，5 条补偿约定由「建议」升为**强制**；② **OQ-23 / CQ-05 已裁决** —— 枚举口径固定为「**22 个命名枚举 = 21 个业务字典 + 1 个系统级枚举 `UserRole`**」，并说明历史 21/22 两个数字均正确 | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：空值策略（含 5 条补偿约定）、22 个命名枚举 + 7 组内联枚举字典、金额/时间/traceId/ID/分页/文件上传约定、通用传输速查、5 条待确认问题 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
