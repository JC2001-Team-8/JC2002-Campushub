# 迭代二 广义契约 ①：默认值与边界（Defaults & Boundaries）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 ①：默认值与边界 |
| 契约编号 | GC-01 |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— P0 项（OQ-01 / OQ-27 / OQ-41）**已裁决并固化**；未裁决项以【待确认】标注 |
| 上游依据 | `docs/iteration1/design/openapi.yaml`（冻结候选）、`docs/iteration1/rules.md`、`docs/iteration1/design/error-codes.md`、`docs/iteration1/design/schema.md`、`docs/iteration1/design/api-open-questions.md`、`docs/iteration1/glossary-assumptions-questions.md` |
| 适用迭代 | 迭代二及后续（`openapi.yaml` 冻结后的实现与联调阶段） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
>
> **定位**：`openapi.yaml` 定义「形状」（路径 / 方法 / DTO / 枚举 / 错误码）。本文档补齐**形状之外**的行为语义：
> 字段**不传 / 传 null / 传空数组 / 传 0 / 超限**时，后端**必须**怎么做，前端**可以**依赖什么。
>
> **冲突处理**：本文档与 `openapi.yaml` 冲突时**以 `openapi.yaml` 为准**；本文档与 `rules.md` / `state-machines.md` 冲突时，**只登记到 [`open-questions.md`](./open-questions.md)**，不修改上游文档。

---

## 0. 阅读与判定约定

### 0.1 四种「空」的区分（全文复用）

| 记号 | 含义 | 判定依据 |
| --- | --- | --- |
| **U** | **未传**（键不存在于 JSON / 查询参数缺省） | 请求体无该键 |
| **N** | **显式 `null`** | 键存在且值为 `null`（JSON `null`） |
| **∅** | **空集合** | `[]`（数组）/ `{}`（对象）/ `""`（字符串） |
| **0** | **零值** | 数值 `0`、`false`、枚举的第一项 |

### 0.2 三条总原则

| # | 原则 | 说明 |
| --- | --- | --- |
| P1 | **可空字段区分「未传」与「显式 null」** | 未传 = **保持原值不变**（PATCH 语义）；显式 `null` = **写入 NULL**（清空）。仅对 `OpenAPI` 中 `nullable: true` 且**非 required** 的字段生效 |
| P2 | **必填字段给 null 一律 400** | `nullable: false` 或 `required: [...]` 中列出的字段，收到 `null` 或未传均返回 `400 VALIDATION_INVALID_PARAMETER`，`details.field` 指向字段 |
| P3 | **不可在运行时新造错误码** | 所有失败必须使用 `error-codes.md` 中已登记的 93 个错误码之一；不得临时发明新码 |

### 0.3 边界判定的统一顺序（与 `api-permission-mapping.md` §1.2 一致）

```text
① 认证（401）→ ② 权限/归属（403）→ ③ 资源存在性（404）
→ ④ 请求体格式校验（400）→ ⑤ 语义/业务规则校验（422）→ ⑥ 状态/并发冲突（409）→ ⑦ 服务端（5xx）
```

> **要点**：**请求体格式校验（400）早于语义/业务规则校验（422）**，**语义校验（422）早于状态冲突（409）**。
> 例：交换包传 0 件且用户已被处罚 → 先判 403（被处罚），再做 422 件数校验（见 §2.7）。

### 0.4 **400 与 422 的划分 —— 已裁决（OQ-01 / CQ-09）**

> **裁决（2026-10-13）**：**统一为 422 Unprocessable Entity**（语义错误，语法正确）。

> **当请求体结构正确（如 JSON 语法没问题），但语义违反约束（如 `imageUrls` 为空数组，违反 `minItems=1`），后端统一返回 422。**
> **400 仅用于请求体格式错误或 JSON 解析失败。**

| HTTP | **唯一适用场景** | 典型例子 |
| --- | --- | --- |
| **400 Bad Request** | **请求体格式错误 / JSON 解析失败 / 类型不可转换 / 字段缺失或为 `null` / 字符串长度越界 / 非法枚举值 / 非法排序字段** | `{"estimatedPrice": "abc"}`（类型错误）、`{"title": null}`（必填为 null）、`{"status": "NOT_A_STATUS"}`（非法枚举）、`sort=rating`（白名单外） |
| **422 Unprocessable Entity** | **JSON 语法与类型均正确，但语义违反契约约束** | `imageUrls: []`（违反 `minItems=1`）、`itemIds: []`（件数越界）、`tradeZoneIds: []`（未勾选区域）、跨区域交易、目标商品不接受交换、件数超上限 |

**统一规则（停止使用"视情况而定"的写法）**

| # | 规则 |
| --- | --- |
| R1 | **数组长度违反 `minItems` / `maxItems` → 一律 422**（**不因"写在 OpenAPI 里"而降级为 400**） |
| R2 | 请求体**无法解析为 JSON**、**字段类型错误**、**必填字段缺失或为 `null`** → **400** |
| R3 | 字符串**长度**越界（`minLength` / `maxLength`）→ **400**（属格式层约束） |
| R4 | **枚举取值非法** → **400** |
| R5 | 语义层面的"业务规则不满足"（含 R1）→ **422** |
| R6 | **错误码取自 `error-codes.md` 已登记的 93 个之一**；无专属 422 码时复用 **`VALIDATION_INVALID_PARAMETER`**（见下"实现差异"） |

> **必须先读：错误码只使用已登记的名称**
>
> 本裁决文书中出现的「`VALIDATION_ERROR`」是**后端 Java 枚举常量名**，**不是线上错误码**。
> 线上错误码为 **`VALIDATION_INVALID_PARAMETER`**（`error-codes.md` §3.2 已登记；实测 `ErrorCode.VALIDATION_ERROR("VALIDATION_INVALID_PARAMETER", ...)`）。
> **前后端一律以 `VALIDATION_INVALID_PARAMETER` 作为 `code` 分支依据**，**禁止**使用 `VALIDATION_ERROR`。

> **【实现差异登记 · IMP-01（P1，非本次 P0 裁决范围）】**
> 裁决要求"语义违反约束 → 422"，但**当前运行时** `GlobalExceptionHandler` 把参数校验异常统一映射为 `ErrorCode.VALIDATION_ERROR` → **HTTP 400**（`ErrorCode` 中**没有任何** `HttpStatus.UNPROCESSABLE_ENTITY` 映射）。
> 因此**在本条裁决落地前**，前端必须同时容错 **400** 与 **422**（按 `code` 分支，不按 HTTP 状态码硬编码）。
> **消除该差异属于后端实现任务**（需新增 422 映射，不属本次"只改 Markdown"的范围），已登记到 [`open-questions.md`](./open-questions.md) **IMP-01**。

---

## 1. 全局参数：分页 / 排序 / 时间

| 参数 | 位置 | U（未传） | N（null） | ∅ / 0 | 超限 | 契约依据 |
| --- | --- | --- | --- | --- | --- | --- |
| `page` | query | **默认 0**（首页） | 按 U 处理视为 0 | `0` 合法 = 首页 | 负数 → `400 VALIDATION_INVALID_PARAMETER`（`minimum: 0`） | `PageParam` |
| `size` | query | **默认 20** | 按 U 处理视为 20 | `0` → `400`（`minimum: 1`） | `size > 100` → **`400 VALIDATION_PAGE_SIZE_EXCEEDED`**（**不静默截断为 100**） | `SizeParam`、`error-codes.md` §3.2 |
| `sort` | query | 采用该资源**默认排序**（见下表） | 视为 U | `[]` → 采用默认排序 | 非白名单字段 → **`400 COMMON_INVALID_SORT_FIELD`** | `SortParam` |
| `X-Trace-Id` | header | 服务端生成 | 视为 U | — | 长度无契约约束（建议 ≤64） | `api-open-questions.md` AQ-16 |

### 1.1 分页越界（`page` 超出总页数）

| 场景 | 后端行为 | 前端可依赖 |
| --- | --- | --- |
| `page >= totalPages` 且 `totalElements > 0` | 返回 **200 `OK`**，`data.content = []`，`page` 原样回显，`first=false`、`last=true` | **不得**把空页当错误；应回退到最后一页或回到第 0 页 |
| `totalElements = 0` | 返回 **200 `OK`**，`content=[]`，`totalPages=0`，`first=true`、`last=true` | 空状态由 `totalElements === 0` 判定，**不要**用 `content.length === 0` 单独判定 |
| `page` 为 `null` 或 `""` | 按未传处理 → `page=0` | — |

> **【待确认】**：`totalPages = 0` 时 `last` 应为 `true`（本文档建议值）。Spring Data `Page` 的 `isLast()` 在空结果时返回 `true`，与建议一致。

### 1.2 排序白名单与默认方向（逐资源）

| 资源 | 允许的 `sort` 字段 | 默认排序 | 契约依据 |
| --- | --- | --- | --- |
| 商品列表 `listItems` | `published_at`、`estimated_price` | `published_at,desc` | `SortParam`、`api-open-questions.md` AQ-09 |
| 我的商品 `listMyItems` | 同上 | `published_at,desc` | 同上 |
| 订单 `listOrders` | `created_at` | `created_at,desc` | 同上 |
| 交换提议 `listSwapProposals` | `created_at` | `created_at,desc` | 同上 |
| 通知 `listMyNotifications` | `created_at` | `created_at,desc` | 同上 |
| 举报 / 处罚 / 审计（管理员） | **未定义白名单** | 建议 `created_at,desc` | 【待确认】见 [`open-questions.md`](./open-questions.md) OQ-05 |

> **多级排序**：`sort` 可重复传入（`explode: true`），形如 `sort=estimated_price,asc&sort=published_at,desc`。
> **方向缺省**：仅传 `sort=published_at`（无 `,方向`）时，按该字段**默认方向**处理（上表）；`asc`/`desc` **大小写敏感为小写**，传 `ASC` 返回 `400 COMMON_INVALID_SORT_FIELD`。

### 1.3 时间与倒计时

| 字段 | 约定 | 契约依据 |
| --- | --- | --- |
| 所有时间字段 | **ISO 8601 UTC**，形如 `2026-10-12T13:20:41Z` | `openapi.yaml` 全局约定 / R-19 |
| `expiresAt` / `deadline` / `endsAt` | **绝对时间**，服务端计算 | R-19、AD-05 |
| `remainingSeconds` | 服务端计算的剩余秒数；**到期瞬间为 0** | R-19、AD-05 |
| 边界判定 | **`now >= expiresAt` 即视为已超时/已过期**（订单 R-07、提议 R-25、处罚 R-31 统一） | rules.md R-07/R-25/R-31 |
| 前端倒计时 | **必须**由 `remainingSeconds` 递减或 `expiresAt` 本地推算；**禁止**信任前端时间 | R-19 |
| `expiresAt` 已过但状态未变 | 返回**当前真实状态**，`remainingSeconds` 返回 `0`（**不得**返回负数） | 【待确认】见 OQ-06 |

---

## 2. 逐接口的字段默认值与边界

> 只列**关键字段**。未列出的字段遵循 §0.2 三条总原则。
> **「U 时的行为」一列中的「→ 存储 X」为本文档给出的后端实现约定；标【待确认】者属 `openapi.yaml` 未定义项。**

### 2.1 认证与账户（tag 01）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `registerUser` | `email` | 否 | → 存储 **`NULL`**（`users.email` 允许空，Q-06 假设不验证） | → `NULL` | `""` → 视为未传 → `NULL` | `>254` 字符 → `400` |
| `registerUser` | `username` | ✅ | `400 VALIDATION_INVALID_PARAMETER` | `400` | `""` → `400`（`minLength: 3`） | `>32` 或含非 `[A-Za-z0-9_]` → `400` |
| `registerUser` | `password` | ✅ | `400` | `400` | `""` → `400` | `<8` 或 `>64` → `400` |
| `registerUser` | `nickname` | ✅ | `400` | `400` | `""` → `400` | `>20` → `400` |
| `login` | 全部 | ✅ | `400` | `400` | `""` → `401 AUTH_INVALID_CREDENTIALS`（**不做空值特异性提示**，防枚举） | 超长 → `400` |
| `refreshToken` | `refreshToken` | ✅ | **`501 COMMON_NOT_IMPLEMENTED`**（AQ-02：未启用优先于参数校验） | `501` | `501` | `501` |
| `changePassword` | `oldPassword` / `newPassword` | ✅ | `400` | `400` | `""` → `400` | `newPassword <8` 或 `>64` → `400`；`oldPassword` 错 → `401 AUTH_INVALID_CREDENTIALS` |
| `logout` | 无请求体 | — | 幂等成功 `200` | — | — | — |

> **【待确认】`refreshToken` 的校验顺序**：本文档建议**先返回 501**（特性未启用，AQ-02），不因缺 `refreshToken` 先返回 400。见 OQ-07（**P1，未裁决**）。

#### 2.1.1 **`registerUser` 成功后的认证行为 —— 已裁决（OQ-27）**

> **裁决（2026-10-13）**：**注册成功后直接签发令牌（注册即登录 / 自动登录）**，以提升前端体验。
>
> **注册成功后，响应体中包含令牌与用户信息，用户无需再次调用 `login` 接口。**
> **该行为与 `login` 接口保持一致，前端应直接存储令牌并跳转首页。**

| 项 | **裁决后的约定** |
| --- | --- |
| 成功状态码 | **`201 Created`**（与 `openapi.yaml` 一致，不变） |
| 响应体 | `ApiResponse.data` **同时包含**：① 认证令牌；② 用户信息 |
| 令牌字段 | 与 `login` **完全同构**：`tokenType`（恒 `"Bearer"`）、`accessToken`、**`expiresIn`（`604800`）**、`expiresAt`；`refreshToken` 不返回（AQ-02 未启用） |
| 用户信息字段 | 与 `login` / `getCurrentUser` 同构，至少含 `id`、`username`、`nickname`、`role`、`status`、`tradeZones` |
| 前端行为 | **直接存储 `accessToken` → 跳转首页**；**不得**在注册成功后再调用 `login`（避免重复签发令牌） |
| 失败分支 | 用户名/邮箱重复 → `409 AUTH_USERNAME_TAKEN`；参数不合法 → `400`；其余不变 |

> **⚠️ 契约落点差异（登记为 CQ-11，**必须知情**）**
>
> **现状**：`openapi.yaml` 的 `registerUser` **当前冻结定义**为
> `201` + `data: CurrentUserResponse`，其 description 明文写着「注册成功后**不自动签发令牌**，需调用 `/auth/login`」（**不含令牌**）。
>
> 因此本裁决**与已冻结契约的 `registerUser` 响应形状不一致**。由于本次任务**禁止修改 `openapi.yaml`**，本裁决的落地方式为：
>
> | # | 落地路径 | 说明 |
> | --- | --- | --- |
> | 1 | **契约侧**：需按 `api-freeze.md` §4 流程提交变更（响应 `data` 由 `CurrentUserResponse` 扩展为「令牌 + 用户信息」，属**类型 C 向后兼容扩充**），并登记变更台账 | **本次不执行**（修改 `openapi.yaml` 被明令禁止） |
> | 2 | **实现侧**：后端待实现时**必须**同时返回令牌与用户信息；在契约变更通过前，**不得**擅自新增响应字段 | 迭代二实现任务 |
> | 3 | **前端侧（过渡期容错，强制）**：**必须**兼容两种响应——**若响应中存在 `accessToken` → 直接登录**；**若不存在 → 回退调用 `login`**。前端**禁止**硬依赖令牌存在 | **迭代二立即生效** |
>
> **裁决效力说明**：**"注册即登录"是本项目的最终裁决目标**；在上述契约变更完成前，前端按第 3 条容错实现即可正常联调，**不阻塞迭代二开工**。

### 2.2 个人资料与设置（tag 02）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `updateMyProfile` | `nickname` | 否 | **保持原昵称不变** | `400`（`nickname` 在 DB 为 `NOT NULL`） | `""` → `400`（`minLength: 1`） | `>20` → `400` |
| `updateMyProfile` | `avatarUrl` | 否 | **保持原头像不变** | **清空头像**（→ `NULL`） | `""` → 视为清空 → `NULL` | `>255` → `400` |
| `updateMyProfile` | 两个字段都 U | — | **空操作**：返回 `200 OK` + 当前资料（**不报 400**） | — | — | — |
| `setMyTradeZones` | `tradeZoneIds` | ✅ | `400` | `400` | `[]` → **`422 VALIDATION_INVALID_PARAMETER`**（违反 `minItems: 1`，按 §0.4 R1 **一律 422**） | `>5` → **`422 VALIDATION_INVALID_PARAMETER`**（R-15 上限）；含不存在/停用 ID → `404 TRADE_ZONE_NOT_FOUND` |
| `getMyNotificationSettings` | 无入参 | — | `200`，`inAppEnabled` / `emailEnabled` 均有值 | — | — | — |
| `updateMyNotificationSettings` | `inAppEnabled` | ✅ | `400` | `400` | `false` **合法** = 关闭通知 | — |
| `updateMyNotificationSettings` | `emailEnabled` | 否 | → **`false`**（迭代一未实现投递，恒 `false`） | → `false` | `true` → **被静默归一为 `false`**（契约注明恒为 false） | — |

> **`setMyTradeZones` 空数组的状态码 —— 已裁决（OQ-01 / CQ-09）**：**`[]` 返回 `422 VALIDATION_INVALID_PARAMETER`**。
> 依据 §0.4 R1（数组违反 `minItems` **一律 422**）与 R6（无专属 422 码时复用 `VALIDATION_INVALID_PARAMETER`）。
> `USER_TRADE_ZONE_REQUIRED`（422）保留其「引导用户去勾选交易区域」的业务用途，但**本次 `[]` 的统一错误码为 `VALIDATION_INVALID_PARAMETER`**。
>
> **`setMyTradeZones` 重复勾选**：`tradeZoneIds` 内出现重复 ID → **去重后处理**（不报错），结果集为去重后的集合。

### 2.3 商品（tag 04）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `createItem` | `description` | 否 | → **存储 `NULL`**（`items.description` 允许空）**【待确认】见 OQ-02** | → `NULL` | `""` → **存储 `""`**（**不归一为 `NULL`**；DB 允许空串） | `>1000` → `400` |
| `createItem` | `imageUrls` | ✅ | `400` | `400` | **`[]` → `422 VALIDATION_INVALID_PARAMETER`**（违反 `minItems: 1`，按 §0.4 R1 **一律 422**） | `>9` → **`422 FILE_COUNT_EXCEEDED`**（`error-codes.md` §3.13）；元素 `>255` 字符 → `400` |
| `createItem` | `swapPreference` | 否 | → **DB 默认 `NOT_ACCEPTING`**（`schema.md` `items.swap_preference` DEFAULT） | → `NOT_ACCEPTING` | — | 非法枚举值 → `400` |
| `createItem` | `estimatedPrice` | ✅ | `400` | `400` | `"0"` / `"0.00"` **合法**（`>= 0`） | 违反 `DecimalString` pattern → `400`；`< 0` → `400`（`ck_items_price_nonneg`） |
| `createItem` | `title` | ✅ | `400` | `400` | `""` → `400` | `>60` → `400` |
| `createItem` | `tradeZoneId` | ✅ | `400` | `400` | `0` / 不存在 → `404 TRADE_ZONE_NOT_FOUND`；不在本人已勾选集合 → `422 ITEM_TRADE_ZONE_NOT_SELECTED` | — |
| `updateItem` | 全字段 | 否 | **部分更新语义**：仅更新出现的字段 | 见下 | — | — |
| `updateItem` | `description` | 否 | 保持原值 | **清空 → `NULL`** | `""` → 存储 `""` | `>1000` → `400` |
| `updateItem` | `imageUrls` | 否 | **保持原图片不变** | `400`（数组字段不接受 null） | **`[]` → `422 VALIDATION_INVALID_PARAMETER`**（全量替换后必须 ≥1 张，R-12；按 §0.4 R1 **一律 422**） | `>9` → `422 FILE_COUNT_EXCEEDED` |
| `updateItem` | `version` | ✅（契约描述为必填） | `400`（**建议**）/ 见 OQ-03 | `400` | `0` 合法 | 不匹配 → `409 COMMON_VERSION_CONFLICT` |
| `updateItem` | 占用态关键字段（`title`/`estimatedPrice`/`categoryId`/`conditionId`/`tradeZoneId`） | 否 | 允许不改 | — | — | **占用态下出现任一关键字段 → `409 ITEM_LOCKED_BY_OCCUPATION`**（R-11；**即使值与当前相同也拒绝**，见 OQ-04） |
| `takeItemOffShelf` / `relistItem` | 无请求体（或仅 `reason`） | — | 幂等：重复下架/上架返回成功态 | — | — | 有占用 → `409 ITEM_LOCKED_BY_OCCUPATION`；`BANNED` 重新上架 → `409 ITEM_BANNED_CANNOT_RELIST` |
| `updateItemSwapPreference` | `swapPreference` | ✅ | `400` | `400` | — | 占用态/终态改为 `ACCEPTING` → `409 ITEM_STATUS_NOT_SWAPPABLE` |
| `updateItemSwapPreference` | `version` | 否 | 不做版本校验（仅状态前置条件） | 同 U | — | 传入且不匹配 → `409 COMMON_VERSION_CONFLICT` |
| `uploadImage` | `file` | ✅ | `400 VALIDATION_INVALID_PARAMETER` | `400` | 空文件 → `400 FILE_EMPTY` | `>5MB` → `422 FILE_TOO_LARGE`；非 JPG/PNG/WebP → `422 FILE_TYPE_NOT_ALLOWED` |
| `listItems` | `keyword` | 否 | 不做关键词过滤 | 视为 U | `""` → **不做过滤**（不报错） | `>60` → `400` |
| `listItems` | `minPrice` / `maxPrice` | 否 | 不做价格过滤 | 视为 U | `"0"` 合法 | 违反 `DecimalString` → `400`；**`minPrice > maxPrice` → 返回空列表（200）**，见 OQ-08 |
| `listItems` | `categoryId` / `conditionId` / `tradeZoneId` / `sellerId` | 否 | 不做该维度过滤 | 视为 U | 不存在的 ID → **返回空列表 200**（**不返回 404**，字典类过滤参数无存在性语义） | — |
| `listItems` | `swapPreference` | 否 | 不做过滤（**返回两种意愿的商品**） | 视为 U | — | 非法枚举 → `400` |

> **`imageUrls` 语义总表（AQ-05 的落地）**

| 场景 | `createItem` | `updateItem` |
| --- | --- | --- |
| 数组顺序 | **下标即 `sort_order`**，第 1 张（下标 0）为封面 | 同左 |
| `[]` | `400`（必须 ≥1 张） | `400`（全量替换后必须 ≥1 张） |
| 重复 URL | **接受**；去重后落库（`uq_item_images_sort` 只约束 `(item_id, sort_order)`） | 同左 |
| 未上传过的 URL | **接受**（不做存在性校验 —— 契约未定义）；见 OQ-09 | 同左 |
| 图片数量上限 | 每个商品 1–9 张（跨次累积也须 ≤9） | 同左 |

### 2.4 购物车（tag 05）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `addCartItem` | `itemId` | ✅ | `400` | `400` | 不存在 → `404 ITEM_NOT_FOUND` | 自家商品 → `422 CART_OWN_ITEM_NOT_ALLOWED`；已占用 → `422 ITEM_NOT_PURCHASABLE` |
| `addCartItem` | 重复添加同一商品 | — | **IDEM-A**：返回 `200` + **既有条目**（**不产生重复条目、不报 409**） | — | — | — |
| `updateCartItem` | `version` | 否 | 不做校验（该接口仅确认条目有效性） | 同 U | — | 不匹配 → `409 COMMON_VERSION_CONFLICT` |
| `updateCartItem` | 请求体为空 `{}` | 否 | `200`，返回条目当前状态 | — | — | **不接受数量变更**：出现 `quantity` 且 ≠1 → `400`（见 OQ-10） |
| `removeCartItem` | — | — | 幂等：删除不存在的条目 **②** | — | — | — |

> **②【待确认】删除不存在的条目**：`error-codes.md` 建议「删除场景可静默成功」，
> 但 `CART_ITEM_NOT_FOUND`（404）已登记。本文档建议：**返回 `200 OK`（幂等）**，见 OQ-11。

### 2.5 订单与付款（tag 06）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `submitOrder` | `cartItemIds` | ✅ | `400` | `400` | `[]` → **`422 VALIDATION_INVALID_PARAMETER`**（违反 `minItems: 1`，按 §0.4 R1 **一律 422**） | 含不存在的条目 → `404 CART_ITEM_NOT_FOUND`；含不可结算条目 → `422 CART_ITEM_NOT_PURCHASABLE` |
| `submitOrder` | `cartItemIds` 重复 ID | — | **去重后处理**（不报错） | — | — | — |
| `submitOrder` | `tradeZoneId` | ✅ | `400` | `400` | 不存在 → `404 TRADE_ZONE_NOT_FOUND`；不在已勾选 → `422 TRADE_ZONE_NOT_SELECTED`；与商品不同区 → `422 TRADE_ZONE_MISMATCH` | — |
| `submitOrder` | `note` | 否 | → 存储 `NULL` | → `NULL` | `""` → 存储 `""`**【待确认】见 OQ-02** | `>255` → `400` |
| `submitOrder` | 跨卖家 | — | **自动拆单**，返回 `orders[]` 多笔 + `split=true`（R-05） | — | — | 任一卖家商品失效 → **整批失败**（假设 A-17，**不产生部分下单**） |
| `mockPayOrder` | `confirm` | 否 | → **`true`**（`default: true`） | → `true` | `false` → 见 OQ-12 | — |
| `cancelOrder` | `reason` | 否 | → 存储 `NULL`（`cancel_note`） | → `NULL` | `""` → 存储 `""` | `>200` → `400` |
| `cancelOrder` | `version` | 否 | 不做版本校验（用 `WHERE status='PENDING_PAYMENT'` 条件更新） | 同 U | — | 非 `PENDING_PAYMENT` → `409 ORDER_ALREADY_CANCELLED` / `ORDER_CANNOT_CANCEL_AFTER_PAID` |
| `selectPickupPoint` | `pickupPointId` | ✅ | `400` | `400` | 不存在 → `404 PICKUP_POINT_NOT_FOUND` | 跨区域 → `422 PICKUP_POINT_ZONE_MISMATCH`；未付款 → `409 ORDER_NOT_PAID_FOR_PICKUP_SELECTION`；已交付 → `409 ORDER_ALREADY_DELIVERED` |
| `markOrderDelivered` | 无请求体 | — | 未选取货点 → `409 ORDER_PICKUP_POINT_NOT_SELECTED` | — | — | 非 `PAID` → `409 ORDER_STATUS_NOT_DELIVERABLE` |
| `confirmOrderReceipt` | 无请求体 | — | 非 `DELIVERED` → `409 ORDER_STATUS_NOT_DELIVERED` | — | — | 重复确认 → **IDEM-A `200`** |

> **`SubmitOrderResponse.orders` 为空的情况不存在**：契约 `minItems: 1`。若无可下单条目，返回错误而非空数组。

### 2.6 评价（tag 07）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `createOrderReview` / `createSwapReview` | `rating` | ✅ | `400` | `400` | `0` → `400 REVIEW_RATING_OUT_OF_RANGE`（`minimum: 1`） | `>5` → `400 REVIEW_RATING_OUT_OF_RANGE` |
| 同上 | `content` | 否 | → 存储 `NULL` | → `NULL` | `""` → 存储 `""` | `>500` → `400` |
| 同上 | 同一方第二次提交 | — | `409 REVIEW_ALREADY_SUBMITTED`（**IDEM-B**，不回放） | — | — | — |

> **注意**：评价是 **IDEM-B**（重复明确失败），与下单/付款的 **IDEM-A**（回放首次结果）**不同**，前端不可统一按「静默成功」处理。

### 2.7 交换包（tag 08）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `createSwapBundle` | `itemIds` | ✅ | `400` | `400` | `[]` → **`422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`**（E-07 定为 422；与 §0.4 R1 一致） | `>5` → `422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`；含他人商品 → `422 SWAP_BUNDLE_ITEM_NOT_OWNED` |
| `createSwapBundle` | `itemIds` 重复 ID | — | **去重后处理**（不报错） | — | — | 商品已在其他进行中包 → `409 SWAP_ITEM_IN_ACTIVE_BUNDLE` |
| `createSwapBundle` | `title` | 否 | → 存储 `NULL` | → `NULL` | `""` → 存储 `""` | `>60` → `400` |
| `createSwapBundle` | `tradeZoneId` | 否 | → **取默认区域（见 OQ-13）** | 同 U | 不存在 → `404 TRADE_ZONE_NOT_FOUND` | — |
| `updateSwapBundle` | `itemIds` | 否 | 保持原组成 | `400` | `[]` → **`422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`**（不允许清空，R-21） | `>5` → `422` |
| `updateSwapBundle` | `title` | 否 | 保持原值 | **清空 → `NULL`** | `""` → 存储 `""` | `>60` → `400` |
| `deleteSwapBundle` | — | — | 被进行中提议引用 → `409 SWAP_BUNDLE_LOCKED`；终态 → `409 SWAP_BUNDLE_STATUS_NOT_EDITABLE` | — | — | — |

### 2.8 交换提议（tag 09）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `createSwapProposal` | `targetItemIds` | ✅ | `400` | `400` | `[]` → **`422 VALIDATION_INVALID_PARAMETER`**（违反 `minItems: 1`，按 §0.4 R1 **一律 422**） | 跨卖家 → **`422`**（C-04：必须同属一个目标用户），见 OQ-14；任一被占用 → `409 SWAP_ITEM_OCCUPIED` / `ITEM_ALREADY_RESERVED` |
| `createSwapProposal` | `proposerBundleId` | ✅ | `400` | `400` | 不存在 → `404 SWAP_BUNDLE_NOT_FOUND` | 非本人包 → `403 AUTH_NOT_RESOURCE_OWNER` |
| `createSwapProposal` | `tradeZoneId` | ✅ | `400` | `400` | — | 与目标商品不同区 → `422 TRADE_ZONE_MISMATCH` |
| `createSwapProposal` | `message` | 否 | → 存储 `NULL` | → `NULL` | `""` → 存储 `""` | `>500` → `400` |
| `createSwapProposal` | 与自己的商品交换 | — | `422 SWAP_SELF_PROPOSAL_NOT_ALLOWED` | — | — | — |
| `acceptSwapProposal` | 请求体 | — | **无请求体**。契约 `accept` **不接受物品子集参数**（OOS-04 / R-22） | — | — | 请求体出现物品列表 → `422 SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED` |
| `rejectSwapProposal` | `reason` | 否 | → 存储 `NULL` | → `NULL` | `""` → 存储 `""` | `>200` → `400` |
| `counterSwapProposal` | `targetBundleId` / `targetItemIds` | ✅ | `400` | `400` | `targetItemIds: []` → **`422 VALIDATION_INVALID_PARAMETER`**（按 §0.4 R1） | `counterDepth=1` 时再反提案 → `409 SWAP_COUNTER_NOT_ALLOWED` |
| `counterSwapProposal` | `message` | 否 | → `NULL` | → `NULL` | `""` → `""` | `>500` → `400` |
| `verifySwapCode` | `code` | ✅ | `400` | `400` | `""` → `422 SWAP_CODE_INVALID` | `>16` → `400`；**大小写**：建议**不敏感**（统一转大写比较），见 OQ-15；已核销 → `409 SWAP_CODE_ALREADY_VERIFIED` |
| `confirmSwap` | `confirm` | ✅ | `400` | `400` | — | **必须为 `true`**；传 `false` → `400 VALIDATION_INVALID_PARAMETER`，见 OQ-16 |
| `confirmSwap` | `remark` | 否 | → `NULL` | → `NULL` | `""` → `""` | `>200` → `400` |
| `cancelSwapProposal` | 请求体 | — | 仅发起方可取消；非 `PENDING` → `409 SWAP_PROPOSAL_ALREADY_FINALIZED` | — | — | — |

### 2.9 举报（tag 10）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `reportItem` | `reason` | ✅ | `400` | `400` | — | 非法枚举 → `400` |
| `reportItem` | `detail` | 否 | → `NULL` | → `NULL` | `""` → `""` | `>500` → `400` |
| `reportItem` | 同一用户重复举报同一商品（已有未处理举报） | — | **`200` + 既有记录**（`REPORT_ALREADY_EXISTS`，**按成功处理**，C-06） | — | — | 举报自己的商品 → `422 REPORT_OWN_ITEM_NOT_ALLOWED` |
| `reportItem` | 重复举报时 `detail` 不同 | — | **不更新既有记录**（保持首次内容），见 OQ-17 | — | — | — |

### 2.10 管理员治理与申诉（tag 11）

> 治理类接口的 **`reason` / `note` 全部为必填**（R-34）。缺失统一返回 **`400 VALIDATION_REASON_REQUIRED`**（或治理域别名 `GOVERNANCE_REASON_REQUIRED`）。
> **`reason` 只含空白字符（如 `"   "`）→ 视为缺失 → `400`**（建议做 `trim` 后判空），见 OQ-18。

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `adminBanUser` | `reason` | ✅ | `400 VALIDATION_REASON_REQUIRED` | `400` | `""` → `400` | `>255` → `400` |
| `adminBanUser` | `durationHours` | 否（`readOnly: true`） | → **固定 72** | → 固定 72 | 传任意值（含 72）→ **忽略不报错**（R-30 / A-03） | — |
| `adminBanUser` | 目标已有生效处罚 | — | `409 GOVERNANCE_ACTIVE_PENALTY_EXISTS`（Q-10 假设拒绝重复封禁） | — | — | 目标为管理员 → `403 GOVERNANCE_CANNOT_PUNISH_ADMIN` |
| `adminTakedownItem` | `reason` | ✅ | `400 VALIDATION_REASON_REQUIRED` | `400` | `""` → `400` | `>255` → `400` |
| `adminTakedownItem` | `itemId`（请求体） | 否 | → 取**路径参数** | → 取路径参数 | 与路径不一致 → **以路径为准**（契约明文） | 商品不存在 → `404 ITEM_NOT_FOUND` |
| `adminRestoreItem` | `reason` | ✅ | `400 VALIDATION_REASON_REQUIRED` | `400` | `""` → `400` | `>255` → `400` |
| `adminRestoreItem` | `restoreTo` | 否 | → **`IN_SALE`**（`default: IN_SALE`） | → `IN_SALE` | — | 非法枚举 → `400`；存在占用冲突 → `409`（见 OQ-19） |
| `adminLiftPenalty` | `reason` | ✅ | `400 VALIDATION_REASON_REQUIRED` | `400` | `""` → `400` | `>255` → `400`；处罚已解封 → `409 GOVERNANCE_PENALTY_ALREADY_RESOLVED` |
| `adminHandleReport` | `status` | ✅ | `400` | `400` | — | 仅接受 `HANDLED` / `DISMISSED` | 
| `adminHandleReport` | `note` | ✅ | `400` | `400` | `""` → `400` | `>255` → `400`；举报已处理 → `409 REPORT_ALREADY_HANDLED` |
| `createAppeal` | `reason` | ✅ | `400` | `400` | `""` → `400` | `>1000` → `400`；处罚非 `ACTIVE` → `409 APPEAL_PENALTY_NOT_ACTIVE`；已有申诉 → `409 APPEAL_ALREADY_EXISTS` |
| `adminResolveAppeal` | `decision` / `note` | ✅ | `400` | `400` | `note: ""` → `400` | `note >1000` → `400`；已处理 → `409 APPEAL_ALREADY_RESOLVED` |

### 2.11 通知（tag 12）

| operationId | 字段 | 必填 | U（未传） | N（null） | ∅ / 0 | 超限 |
| --- | --- | --- | --- | --- | --- | --- |
| `listMyNotifications` | `unreadOnly` | 否 | → **`false`**（`default: false`，返回全部） | → `false` | `false` = 全部 | — |
| `listMyNotifications` | `type` | 否 | 不做类型过滤 | 视为 U | — | 非法枚举 → `400` |
| `markNotificationsRead` | `ids` | 否 | **全部标记已读** | **全部标记已读** | `[]` → **全部标记已读**（契约明文：为空表示全部） | 含不属于本人的 ID → **忽略该 ID**（不报错），见 OQ-20 |
| `getUnreadNotificationCount` | — | — | 返回 `0`（无未读）时**返回 200 + `unreadCount: 0`**（**不返回 404**） | — | — | — |

### 2.12 系统内部任务（tag 13）

| operationId | 字段 | 约定 |
| --- | --- | --- |
| `runOrderTimeoutReleaseTask` | 无请求体 | 返回 `ScheduledTaskResult`；重复执行 `processed` 递减、`skipped` 递增，**不得**重复释放/重复通知（IDEM-B） |
| `runSwapProposalExpireTask` | 无请求体 | 同上；**已 `ACCEPTED` 的提议永不参与过期**（R-25） |
| `runPenaltyAutoUnbanTask` | 无请求体 | 同上；`ACTIVE`/`APPEALING` 到期均解封；与管理员手动解封并发时只一方成功 |
| `runReviewWindowCloseTask` | 无请求体 | 扫描条件须为 `review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED')`（**不得**把 `BOTH_REVIEWED` 回退，DQ-27 / IDEM-C） |

---

## 3. 空集合与空值的响应侧边界

> **前提（OQ-22 已裁决）**：后端全局 Jackson `NON_NULL` → **`null` 字段在 JSON 中被省略（字段不返回）**。
> 下表"`data`"一列出现 `null` 时，**实际响应中该字段被省略**，前端按"字段缺失"判定。详见 [`data-conventions.md`](./data-conventions.md) §1。

| 接口族 | 无数据时的响应 | `content` | `data` |
| --- | --- | --- | --- |
| 分页列表（`listItems`/`listOrders`/`listMyNotifications`/`listMyReports`/…） | `200 OK` | `[]` | `PageResult` 完整字段（`totalElements: 0`） |
| 非分页集合（`listCategories`/`listTradeZones`/`listItemConditions`/`listPickupPointsByZone`） | `200 OK` | — | `[]`（**不得**返回空值） |
| 无业务数据的写操作（下架、标记已读、交付、确认收货、`logout`） | `200 OK` | — | **省略**（`EmptyData`；前端不得假设 `data === null`，应判字段缺失） |
| 详情类（`getItemDetail`/`getOrderDetail`/…） | 资源不存在 → `404` | — | — |

> **空格与空集合的裁决（与 OQ-21 / OQ-22 一致）**：
> · **列表无数据 → 返回空数组 `[]`**（前端可无条件 `.map()`；`[]` **不会**被 `NON_NULL` 省略，因为配置为 `NON_NULL` 而**非** `NON_EMPTY`）；
> · **`null` 字段 → 字段被省略**，前端用 `field === undefined` 判定，**禁止**用 `field === null`（恒为 `false`）。
> OQ-21 为 **P2（未裁决）**，本节给出的是**与运行时一致**的确定行为。

---

## 4. 占位数据与迁移约束 —— **已裁决（OQ-41）**

> **裁决（2026-10-13）**：**迭代二暂不新增 `V5+` 迁移**，继续使用 V1~V4。

> **迭代二开发期间，继续使用 `CAMPUS_A`、`PICKUP_LIBRARY_A` 等占位数据。**
> **待 Q-01~Q-03 确认后，再新增 `V5__update_placeholder_zones.sql` 之类的迁移进行替换。**
> **在此之前，禁止直接修改 V1~V4。**

| 项 | **裁决后的约定** |
| --- | --- |
| 占位数据 | `TradeZone.code` = `CAMPUS_A` / `CAMPUS_B` / `CAMPUS_C` / `COMMUNITY_A1` …；`PickupPoint.code` = `PICKUP_LIBRARY_A` …；名称含「【待确认】」前缀。**全部标注为【待确认】** |
| 迁移文件 | **只保留 V1~V4**，**不新增 V5+**；V1~V4 的 SHA-256 必须与 `docs/iteration1/FREEZE.md` §1.2 一致 |
| 红线 | **禁止直接修改 V1~V4 中任何脚本**（本次任务再次确认）；**禁止**在执行过的迁移上追加内容 |
| 真实数据替换路径 | 待 **Q-01/Q-02/Q-03**（校区/区域/取货点清单）确认后，**新增** `V5__update_placeholder_zones.sql` 之类的**新迁移**做**数据替换**（不是修改 V1~V4）；替换时**不得改变字段名、类型、路径与 DTO 结构** |
| 前端约束 | **禁止**在前端硬编码 `CAMPUS_A` 等占位编码或「校区 A」等占位名称；**必须**从 `GET /trade-zones`、`GET /trade-zones/{id}/pickup-points` 动态获取 |
| 新增迁移的纪律（供未来使用） | 新增 `V5+` 时**必须**：① 由后端负责人按数据库变更流程决定；② 同步更新 `FREEZE.md` 的哈希清单；③ 不得重命名/修改既有迁移文件 |

---

## 5. 【待确认】汇总索引（仅列**未裁决**项）

> 本节仅列出**仍未裁决**的**编号索引**，完整描述、影响面与建议默认值见 [`open-questions.md`](./open-questions.md)。
> **已裁决的 P0 项（OQ-01 / OQ-22 / OQ-23 / OQ-27 / OQ-33 / OQ-35 / OQ-37 / OQ-40 / OQ-41）已从本表移除**，其结论已直接写入本文档正文。

| 编号 | 一句话问题 | 优先级 |
| --- | --- | --- |
| ~~OQ-01~~ | ~~`[]` 违反 `minItems` 时返回 400（形状）还是 422（业务）~~ → **已裁决：一律 422 `VALIDATION_INVALID_PARAMETER`**（见 §0.4） | ✅ 已裁决 |
| OQ-02 | 可选字符串传 `""` 时存空串还是归一为 `NULL` | P1 |
| OQ-03 | `updateItem.version` 是否严格必填 | P1 |
| OQ-04 | 占用态提交**与原值相同**的关键字段是否算违规 | P1 |
| OQ-05 | 管理员列表接口的 `sort` 白名单 | P1 |
| OQ-06 | 已过期但状态未推进时 `remainingSeconds` 是否返回 0 | P1 |
| OQ-07 | `refreshToken` 未启用时 501 与 400 的先后顺序 | P1 |
| OQ-08 | `minPrice > maxPrice` 返回空列表还是 400 | P1 |
| OQ-09 | `imageUrls` 是否校验 URL 已存在 | P1 |
| OQ-10 | `updateCartItem` 收到 `quantity` 字段如何处理 | P1 |
| OQ-11 | 删除不存在的购物车条目返回 200 还是 404 | P1 |
| OQ-12 | `mockPayOrder.confirm=false` 的语义 | P1 |
| OQ-13 | `createSwapBundle.tradeZoneId` 未传时的取值来源 | P1 |
| OQ-14 | `createSwapProposal.targetItemIds` 跨卖家的错误码 | P1 |
| OQ-15 | 交换码核销是否大小写不敏感 | P1 |
| OQ-16 | `confirmSwap.confirm=false` 是否报错 | P1 |
| OQ-17 | 重复举报时是否更新 `detail` | P1 |
| OQ-18 | 仅含空白的 `reason` 是否算缺失 | P1 |
| OQ-19 | `adminRestoreItem` 遇占用冲突的状态码与行为 | P2 |
| OQ-20 | `markNotificationsRead` 含他人通知 ID 的处理 | P2 |
| OQ-21 | 空列表返回 `[]` 还是 `null` | P2 |

---

## 5. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **执行 P0 裁决并固化**：① **OQ-01 / CQ-09 已裁决** —— 新增 §0.4「400 与 422 的划分」，**语义违反约束（含数组违反 `minItems`）一律 422**，错误码统一为 **`VALIDATION_INVALID_PARAMETER`**（并澄清 `VALIDATION_ERROR` 是 Java 常量名而非线上错误码），同步修正 §2.2 / §2.3 / §2.5 / §2.7 / §2.8 全部数组边界行；② **OQ-27 已裁决** —— 新增 §2.1.1「注册成功即签发令牌」，并**如实登记**该裁决与 `openapi.yaml` 现有 `registerUser` 响应的差异（CQ-11）及过渡期前端容错要求；③ **OQ-41 已裁决** —— 新增 §4「占位数据与迁移约束」，**迭代二不新增 V5+**、禁止修改 V1~V4、真实数据替换走新迁移；④ §3 响应侧空值边界按 **OQ-22** 裁决改写为「字段缺失」；⑤ §5 待确认索引移除已裁决项 | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：四种「空」的区分、三条总原则、判定顺序、分页/排序/时间边界、12 个模块逐接口默认值与边界表、响应侧空值边界、21 条待确认问题索引 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
