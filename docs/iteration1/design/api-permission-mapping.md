# 迭代一 接口权限映射

| 项目 | 内容 |
| --- | --- |
| 标题 | 迭代一 接口权限映射 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 接口契约负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 依据文档 | [`../permission-matrix.md`](../permission-matrix.md)、[`../requirements.md`](../requirements.md)、[`openapi.yaml`](./openapi.yaml) |
| 规范文件 | [`openapi.yaml`](./openapi.yaml)（每个操作用 `x-required-roles` 扩展声明权限） |
| 适用范围 | 迭代一全部 **83** 个业务操作（13 个模块）；公开 **10** 个、需登录 **73** 个 |
| 用途 | 权限实现与安全测试的**唯一逐条依据**：每行给出「接口 → 7 类主体 → 判定结果」 |

> **强约束**：本文档**不得**与 [`openapi.yaml`](./openapi.yaml) 冲突。若两者不一致，**以 `openapi.yaml` 为唯一真源**，并立即提变更单修正本文档（见 §9、§10）。
>
> **待确认标记**：文中 `【待确认】` 处均引用问题编号（`Q-01`~`Q-34` 需求问题、`DQ-01`~`DQ-25` 数据库问题、`A-01`~`A-20` 假设），出处见 [`../glossary-assumptions-questions.md`](../glossary-assumptions-questions.md) 与 [`open-questions.md`](./open-questions.md)。

---

## 1. 权限记号与判定规则

### 1.1 记号（与 [`../permission-matrix.md`](../permission-matrix.md) §1.1 保持一致）

| 记号 | 含义 | 判定语义 |
| --- | --- | --- |
| ✅ | 允许 | 公开可达，或该主体在满足其它前置条件时允许 |
| ❌ | 禁止 | 该主体**一律**禁止（即使已登录、即使通过归属校验） |
| 🔒 | 需登录 | 必须携带有效 `bearerAuth`（JWT），否则 `401 AUTH_UNAUTHENTICATED` / `401 AUTH_TOKEN_EXPIRED` |
| 👤 | 需本人 | 必须通过**资源归属 / 交易参与方**校验，失败返回 `403 AUTH_NOT_RESOURCE_OWNER`（**不是 404**） |
| 🛡️ | 仅管理员 | 必须 `ROLE_ADMIN`，否则 `403 AUTH_ADMIN_REQUIRED` |
| ⛔ | 被处罚用户禁止 | `ROLE_RESTRICTED` 调用写操作时返回 `403 USER_RESTRICTED_CANNOT_WRITE`（含 `details.restrictedUntil`、`penaltyId`） |
| ⏱️ | 系统任务 | 仅由定时任务经 `/internal/**` 调用，无人工 HTTP 入口 |
| — | 不适用 | 该主体语义上不涉及此操作（非禁止声明，仅表示无角色意义） |

**复合写法**：单元格可组合多个记号，表示**同时满足**才放行。例如 `🔒👤` = 必须登录 **且** 必须是本人；`🔒🛡️` = 必须登录 **且** 必须是管理员；`🔒⛔` = 必须登录，且被处罚用户禁止；`✅ 公开` = 无需登录。

**列的含义（7 类主体）**：

| 列 | 角色 | 定义 |
| --- | --- | --- |
| 访客 | `ROLE_GUEST` | 未登录用户，只读 |
| 注册用户 | `ROLE_USER` | 已登录的普通用户（未落到具体交易身份时的通用能力） |
| 卖家 | `ROLE_SELLER` | 注册用户**在其自有商品 / 其作为卖方的订单**上的关系角色（非独立账号） |
| 买家 | `ROLE_BUYER` | 注册用户**在其作为买方的订单 / 其发起的交换**上的关系角色（非独立账号） |
| 管理员 | `ROLE_ADMIN` | 治理角色，**不参与交易** |
| 被处罚用户 | `ROLE_RESTRICTED` | 处罚生效中的用户，交易写操作被禁（白名单见 §7） |
| 系统 | `ROLE_SYSTEM` | 内置定时任务（`ROLE_SYSTEM` 服务账号 / 内网凭证） |

### 1.2 判定顺序（实现必须按此顺序短路）

| 顺序 | 判定 | 失败结果 | 说明 |
| --- | --- | --- | --- |
| 1 | 端点是否需要登录（`security` 是否为空） | `401 AUTH_UNAUTHENTICATED` | `security: []` 的 **10** 个公开端点直接跳到第 5 步 |
| 2 | 令牌是否有效且未过期 | `401 AUTH_TOKEN_EXPIRED` | 7 天有效期（假设 `A-06` / `R-01`） |
| 3 | **P-00-A** 管理员交易拦截 | `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN` | 管理员调用交易写接口时**先于**归属校验触发 |
| 4 | 角色是否在 `x-required-roles` 内 | `403 AUTH_ADMIN_REQUIRED`（`/admin/**`）或 `403 USER_RESTRICTED_CANNOT_WRITE`（处罚拦截） | 处罚拦截**先于**归属校验 |
| 5 | **P-00-B** 归属 / 参与方校验 | `403 AUTH_NOT_RESOURCE_OWNER` | **刻意不使用 404**，避免探测他人资源是否存在 |
| 6 | 业务前置条件（状态机、区域、占用、幂等） | `409` / `422` | 见 [`error-codes.md`](./error-codes.md) E-04~E-17 |

> **顺序不可交换**：顺序错误会产生「用 404 泄露资源存在性」「处罚用户通过归属校验后写入」「管理员绕过交易限制」三类安全问题。

### 1.3 三条硬性边界（优先级高于任何矩阵单元格）

| 编号 | 边界 | 内容 | 契约后果 |
| --- | --- | --- | --- |
| **P-00-A** | **管理员不参与交易** | `ROLE_ADMIN` 对任何**发布/编辑商品、加购物车、下单、付款、发货、确认收货、评价、创建交换包、发起/接受/拒绝/反提案/核销/确认交换**一律 ❌，即使其账号技术可登录 | 交易写接口对管理员返回 `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN`；管理员对交易资源仅保留**治理只读**（见 §2.5 说明）与治理写操作 |
| **P-00-B** | **本人原则** | 涉及「自己的资源」的操作**必须**校验资源归属（商品卖家、订单买卖双方、交换包包主、提议双方、举报发起人、处罚对象、评价归属）；**仅靠登录态不足** | 归属校验失败统一返回 **`403 AUTH_NOT_RESOURCE_OWNER`**，**不返回 404**，避免信息泄露与语义混淆。唯一可辨析的例外：资源确实不存在时返回 `404 *_NOT_FOUND`；但**当请求者不是归属人时，一律先返回 403**，不得以 404 掩盖 |
| **P-00-C** | **处罚不越权** | 被处罚用户**仍可**：登录、退出、携带令牌、浏览商品与详情、搜索、查看自己的资料、查看自己的处罚与解封时间、提交申诉、查看自己的历史订单与交换（只读）、下架自己的商品、举报、上传图片、查看/修改常用交易区域、修改密码、查看通知、标记通知已读、查看自己的通知设置 | 处罚**只**禁止交易类写入（白名单见 §7）；拦截必须返回专用错误码并携带解封时间 |

> **P-00-C 的取证例外**：`uploadImage` 对被处罚用户开放，用于举报举证；但该图片若用于发布商品，会在 `createItem` 处被 `403 USER_RESTRICTED_CANNOT_WRITE` 拦截（契约已显式说明）。

### 1.4 两个高频易错点

| 编号 | 易错点 | 正确定义 |
| --- | --- | --- |
| PM-01 | 「被处罚用户」不是独立账号类型 | 它**仍是** `ROLE_USER`，只是附加 `status=RESTRICTED` + `restrictedUntil`；因此 `x-required-roles` 中出现 `RESTRICTED` 的端点表示「被处罚用户可访问」，**未出现**表示被拦截（返回 `403 USER_RESTRICTED_CANNOT_WRITE`） |
| PM-02 | 卖家 / 买家不是可注册身份 | 二者是**关系角色**：同一账号在 A 商品上是卖家、在 B 订单上是买家；矩阵中的「卖家 / 买家」列表示**通过归属校验后的关系角色视角**，未通过归属校验时该列为 ❌ |

---

## 2. 7 主体 × 6 域 权限映射

> **编号说明（重要）**：下表 `operationId #N` 中的 `N` 为**本文档内部序号**，与
> [`api-contract.md`](./api-contract.md) 的表格序号**不通用**。跨文档引用请使用 `operationId`。


> **6 大域与 OpenAPI tag 的映射**（模块编号与 [`api-contract.md`](./api-contract.md) §1.1 一致）：

| 域 | 覆盖 tag | 操作数 |
| --- | --- | --- |
| §2 账户与个人资料 | 01-认证与账户、02-个人资料、12-通知 | 6 + 5 + 3 = 14 |
| §3 商品 | 03-字典与区域、04-商品、05-购物车、10-举报 | 4 + 10 + 4 + 3 = 21 |
| §4 购买 | 06-订单与付款、07-评价 | 8 + 3 = 11 |
| §5 交换 | 08-交换包、09-交换提议 | 5 + 11 = 16 |
| §6 管理员治理 | 11-管理员治理 | 17 |
| §7 系统定时任务 | 13-系统定时任务 | 4 |
| **合计** | — | **83** |

> **编号列**采用「模块号.序号」形式，序号沿用 [`api-contract.md`](./api-contract.md) 操作序号（`#01`~`#83`）。

### 2.1 §2 账户与个人资料（tag 01 / 02 / 12）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `registerUser` #01 | POST /auth/register | ✅ 公开 | — | — | — | — | — | — | 无需登录；用户名唯一（`AUTH_USERNAME_TAKEN` 409）；**即使携带过期令牌也必须放行** |
| `login` #02 | POST /auth/login | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ **允许登录** | — | P-00-C：被处罚用户**必须能登录**（否则无法申诉）；成功后响应 `status=RESTRICTED` + `restrictedUntil` |
| `refreshToken` #03 | POST /auth/refresh | ✅ 公开 | — | — | — | — | — | — | **未启用**：返回 `501 COMMON_NOT_IMPLEMENTED`（Q-07）；`x-feature-flag: refresh-token-enabled` |
| `logout` #04 | POST /auth/logout | ❌ | 🔒 | 🔒 | 🔒 | 🔒 | 🔒 | — | 幂等：重复退出返回成功态 |
| `getCurrentUser` #05 | GET /auth/me | ❌ | 🔒 | 🔒 | 🔒 | 🔒 | 🔒 | — | 前端据此判断「是否登录 / 是否被处罚 / 是否管理员」 |
| `changePassword` #06 | PUT /auth/password | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | — | 本人=账号自身；需校验原密码（`AUTH_INVALID_CREDENTIALS` 401）；**处罚期间允许**（非交易写入） |
| `updateMyProfile` #07 | PUT /users/me/profile | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ⛔（**昵称/头像字段**） | — | 本人=账号自身；被处罚用户修改**昵称与头像**返回 `403 USER_RESTRICTED_CANNOT_WRITE`（假设 `A-13` / Q-12）**【待确认 Q-12】** |
| `setMyTradeZones` #08 | PUT /users/me/trade-zones | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 允许 | — | 1–5 个区域；**处罚期间允许**（非交易写入，契约 `x-forbidden-for-restricted: false` 未标注） |
| `getMyNotificationSettings` #09 | GET /users/me/notification-settings | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | — | 本人=账号自身 |
| `updateMyNotificationSettings` #10 | PUT /users/me/notification-settings | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | — | 处罚期间允许（非交易写入）；管理员同为自身设置，**不得**修改他人设置 |
| `getPublicProfile` #11 | GET /users/{userId}/public-profile | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 仅昵称、头像、在售数量、常用区域，**无联系方式**；任何主体可查**任意**用户（非本人原则的**显式例外**） |
| `listMyNotifications` #12 | GET /notifications | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | — | 本人=收件人；越权访问他人通知应视为不可达（仅本人集合有入口） |
| `getUnreadNotificationCount` #13 | GET /notifications/unread-count | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | — | 同上 |
| `markNotificationsRead` #14 | POST /notifications/read | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 | 🔒👤 ✅ 允许 | — | 只能标记**自己的**通知；传入他人通知 ID 必须被忽略或返回 `403 AUTH_NOT_RESOURCE_OWNER`（实现取其一，需测试固化为无副作用） |

> **说明**：`updateMyProfile` 的处罚拦截是**字段级**的（仅昵称/头像）；其余资料字段是否放开【待确认 Q-12】。若 Q-12 结论为「从宽」，则本行 `⛔` 需同步删除，且 [`openapi.yaml`](./openapi.yaml) 的 `x-forbidden-for-restricted: true` 必须一并修改。

### 2.2 §3 商品（tag 03 字典 / 04 商品 / 05 购物车 / 10 举报）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `listTradeZones` #15 | GET /trade-zones | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 字典只读；区域清单条目**占位数据**【待确认 Q-01、Q-02】 |
| `listPickupPointsByZone` #16 | GET /trade-zones/{tradeZoneId}/pickup-points | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 取货点清单**占位数据**【待确认 Q-03】 |
| `listCategories` #17 | GET /categories | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 分类由管理员预置，用户不可自定义（Q-21）**【待确认 Q-21】** |
| `listItemConditions` #18 | GET /item-conditions | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 成色字典只读 |
| `listItems` #19 | GET /items | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 仅展示「在售」；下架/售出/违规下架默认不展示；关键词+分类+区域过滤（OOS-09） |
| `createItem` #20 | POST /items | ❌ | 🔒 ⛔ | 🔒👤 ⛔ | 🔒 ⛔ | ❌（**P-00-A**） | 🔒 ⛔ | — | 幂等**必须**（`Idempotency-Key`）；发布后即成为该商品卖家；需已勾选交易区域（`USER_TRADE_ZONE_REQUIRED` 422）；管理员返回 `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN` |
| `listMyItems` #21 | GET /items/mine | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=商品卖家；被处罚用户**可查看**自己的商品（含下架态） |
| `getItemDetail` #22 | GET /items/{itemId} | ✅ 公开 | ✅ | ✅ | ✅ | ✅ | ✅ | — | 展示图片、成色、估价、区域、卖家交换状态；**不含卖家联系方式** |
| `updateItem` #23 | PUT /items/{itemId} | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | ❌（**P-00-A**） | 🔒 ⛔ | — | 本人=商品卖家；非卖家 → `403 AUTH_NOT_RESOURCE_OWNER`（商品域别名 `ITEM_NOT_OWN_SELLER`）；占用/终态 → `409 ITEM_LOCKED_BY_OCCUPATION` / `ITEM_STATUS_NOT_EDITABLE` |
| `takeItemOffShelf` #24 | POST /items/{itemId}/off-shelf | ❌ | 🔒👤 | 🔒👤 | ❌ | ❌ | 🔒👤 ✅ **允许** | — | 「减损」操作，被处罚用户**允许**（契约 `x-forbidden-for-restricted: false`；权限矩阵 §8 记为 Q-11 可选项）**【待确认 Q-11】**；有进行中占用时 `409 ITEM_LOCKED_BY_OCCUPATION`；幂等 |
| `relistItem` #25 | POST /items/{itemId}/relist | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | ❌ | 🔒 ⛔ | — | 本人=商品卖家；被处罚用户**禁止重新上架**；`BANNED`（违规下架）不可自行上架 → `409 ITEM_BANNED_CANNOT_RELIST` |
| `updateItemSwapPreference` #26 | PUT /items/{itemId}/swap-preference | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | ❌ | 🔒 ⛔ | — | 本人=商品卖家；占用态/终态设为「接受交换」→ `409 ITEM_STATUS_NOT_SWAPPABLE` |
| `getItemOccupation` #27 | GET /items/{itemId}/occupation | ❌ | ❌ | 🔒👤 | ❌ | 🔒🛡️ **只读** | 🔒👤 ✅ 只读（限自有商品） | — | **本人或管理员**；卖家看自有商品占用，管理员为治理只读（唯一允许管理员读取交易占用的场景之一，不违反 P-00-A——不产生交易副作用） |
| `uploadImage` #28 | POST /files/images | ❌ | 🔒 | 🔒 | 🔒 | 🔒 | 🔒 ✅ **允许** | — | **P-00-C 取证例外**：被处罚用户允许上传（用于举报举证）；用于发布商品时由 `createItem` 拦截；JPG/PNG/WebP、≤5MB（假设 `A-05`） |
| `getCart` #29 | GET /cart | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=购物车所有者；按卖家分组；**处罚期间允许查看** |
| `addCartItem` #30 | POST /cart/items | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 本人=购物车所有者；不可加自己发布的商品；同一商品不重复；占用 → `409 ITEM_ALREADY_RESERVED` / `ITEM_RESERVED_BY_SWAP` |
| `updateCartItem` #31 | PATCH /cart/items/{cartItemId} | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 本人=该购物车条目所属用户；条目属于他人 → `403 AUTH_NOT_RESOURCE_OWNER`；数量固定 1（假设 `A-16`） |
| `removeCartItem` #32 | DELETE /cart/items/{cartItemId} | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 同上；契约标注 `x-forbidden-for-restricted: true`（**注意**：它属写操作，被处罚用户禁止；与「只读查看购物车」不同） |
| `reportItem` #33 | POST /items/{itemId}/reports | ❌ | 🔒 | 🔒 | 🔒 | ❌ | 🔒 ✅ **允许** | — | 必须登录；被处罚用户**可举报**（治理相关）；`reason` 必填；重复举报幂等返回 200 + 既有记录（E-14） |
| `listMyReports` #34 | GET /users/me/reports | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ | — | 本人=举报发起人 |
| `getMyReport` #35 | GET /reports/{reportId} | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒🛡️ | 🔒👤 ✅ | — | **举报人或管理员**；非举报人非管理员 → `403 AUTH_NOT_RESOURCE_OWNER` |

### 2.3 §4 购买（tag 06 / 07）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `submitOrder` #36 | POST /orders | ❌ | 🔒 ⛔ | ❌（不可买自有商品） | 🔒👤 ⛔ | ❌（**P-00-A**） | 🔒 ⛔ | — | 幂等**必须**；同一卖家一单；占用发生在下单时（R-16）；跨区域 `422 TRADE_ZONE_MISMATCH`；管理员 → `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN`；被处罚 → `403 USER_RESTRICTED_CANNOT_WRITE` |
| `listOrders` #37 | GET /orders | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=订单参与方；管理员**不得**经此接口浏览全量订单（治理走 `/admin/**`）；只返回请求者参与的订单 |
| `getOrderDetail` #38 | GET /orders/{orderId} | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒🛡️ **只读** | 🔒👤 ✅ 只读 | — | **参与方或管理员（只读）**；非参与方且非管理员 → `403 AUTH_NOT_RESOURCE_OWNER`（**不得返回 404**） |
| `mockPayOrder` #39 | POST /orders/{orderId}/mock-pay | ❌ | ❌ | ❌ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 幂等**必须**；**仅买家本人**；仅「待付款」可付；超时后 `409 ORDER_TIMEOUT_RELEASED`；卖家/第三方调用 → `403` |
| `cancelOrder` #40 | POST /orders/{orderId}/cancel | ❌ | ❌ | ❌ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | **仅买家本人**；本迭代仅「待付款」阶段允许自助取消（假设；已付款是否可取消见 Q-08）**【待确认 Q-08】** |
| `selectPickupPoint` #41 | PUT /orders/{orderId}/pickup-point | ❌ | ❌ | ❌ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | **仅买家本人**；取货点必须属于订单交易区域；付款后选择，交付后禁止修改 |
| `markOrderDelivered` #42 | POST /orders/{orderId}/deliver | ❌ | ❌ | 🔒👤 | ❌ | ❌ | 🔒👤 ✅ **收尾允许**（假设 `A-14`） | — | **仅该订单卖家**；需已选定取货点；**假设 A-14**：处罚前已存在订单的收尾操作默认**允许**（否则交易永久卡死）**【待确认 Q-13】** |
| `confirmOrderReceipt` #43 | POST /orders/{orderId}/confirm-receipt | ❌ | ❌ | ❌ | 🔒👤 | ❌ | 🔒👤 ✅ **收尾允许**（假设 `A-14`） | — | **仅该订单买家**；幂等；同样受假设 `A-14` 保护**【待确认 Q-13】** |
| `createOrderReview` #44 | POST /orders/{orderId}/reviews | ❌ | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 幂等**必须**；本人=交易参与方（买卖双方各一次）；前置：订单「已完成」，否则 `409 REVIEW_TRANSACTION_NOT_COMPLETED` |
| `listOrderReviews` #45 | GET /orders/{orderId}/reviews | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=交易参与方；非参与方 → `403 AUTH_NOT_RESOURCE_OWNER` |
| `listMyReceivedReviews` #46 | GET /users/me/reviews | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ | — | 本人=被评价人（仅自己收到的评价） |

> **A-14 的边界（必须逐字实现）**：仅 `#42 markOrderDelivered` 与 `#43 confirmOrderReceipt` 两个**收尾**操作对被处罚用户放行，且**仅针对处罚生效前已存在**的进行中订单。处罚生效**后**新产生的订单本就不可能存在（下单已被 ⛔ 拦截）。`createOrderReview`（#44）**不在**允许范围（契约 `x-forbidden-for-restricted: true`）**【待确认 Q-13】**。

### 2.4 §5 交换（tag 08 / 09）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `listMySwapBundles` #47 | GET /swap-bundles | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=交换包所有者 |
| `createSwapBundle` #48 | POST /swap-bundles | ❌ | 🔒 ⛔ | 🔒 ⛔ | 🔒 ⛔ | ❌（**P-00-A**） | 🔒 ⛔ | — | 幂等**必须**；1–5 件自有且未被占用的商品，否则 `422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`；管理员 → `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN` |
| `getSwapBundle` #49 | GET /swap-bundles/{bundleId} | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒🛡️ **只读** | 🔒👤 ✅ 只读 | — | **包主或管理员**；非包主且非管理员 → `403 AUTH_NOT_RESOURCE_OWNER` |
| `updateSwapBundle` #50 | PUT /swap-bundles/{bundleId} | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | ❌ | 🔒 ⛔ | — | 本人=包主；仅「草稿/已打包」且无进行中提议时可改 |
| `deleteSwapBundle` #51 | DELETE /swap-bundles/{bundleId} | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | ❌ | 🔒 ⛔ | — | 本人=包主；无进行中提议时可删 |
| `listSwapProposals` #52 | GET /swap-proposals | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ 只读 | — | 本人=**参与方**（仅返回请求者参与的提议）；管理员不得经此接口浏览全量 |
| `createSwapProposal` #53 | POST /swap-proposals | ❌ | 🔒 ⛔ | 🔒 ⛔ | 🔒 ⛔ | ❌（**P-00-A**） | 🔒 ⛔ | — | 幂等**必须**；目标商品须接受交换且同区域，否则 `422 SWAP_TARGET_NOT_ACCEPTING` / `TRADE_ZONE_MISMATCH`；目标被占用 → `409 SWAP_ITEM_OCCUPIED` |
| `getSwapProposal` #54 | GET /swap-proposals/{proposalId} | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | 🔒🛡️ **只读** | 🔒👤 ✅ 只读 | — | **提议双方或管理员（只读）**；第三方 → `403 AUTH_NOT_RESOURCE_OWNER` |
| `acceptSwapProposal` #55 | POST /swap-proposals/{proposalId}/accept | ❌ | ❌ | ❌ | 🔒👤 ⛔（**仅目标方**） | ❌ | 🔒 ⛔ | — | 幂等**必须**；**仅目标方**（发起方自接受 → `403`）；并发下只允许一方成功（R-18/R-31）；终态后 `409 SWAP_PROPOSAL_ALREADY_FINALIZED` |
| `rejectSwapProposal` #56 | POST /swap-proposals/{proposalId}/reject | ❌ | ❌ | ❌ | 🔒👤（**仅目标方**） | ❌ | 🔒 ⛔ | — | **仅目标方**；幂等 |
| `counterSwapProposal` #57 | POST /swap-proposals/{proposalId}/counter | ❌ | ❌ | ❌ | 🔒👤 ⛔（**仅目标方**） | ❌ | 🔒 ⛔ | — | 幂等**必须**；**仅目标方**；**仅一轮**反提案（假设 `A-08`）；已反提案 → `409 SWAP_PROPOSAL_COUNTERED` |
| `cancelSwapProposal` #58 | POST /swap-proposals/{proposalId}/cancel | ❌ | 🔒👤（**仅发起方**） | 🔒👤（**仅发起方**） | 🔒👤（**仅发起方**） | ❌ | 🔒 ⛔ | — | **仅发起方**；仅「待处理」状态；幂等；目标方调用 → `403` |
| `getSwapCode` #59 | GET /swap-proposals/{proposalId}/swap-code | ❌ | ❌ | 🔒👤（**仅双方**） | 🔒👤（**仅双方**） | ❌ | 🔒👤 **允许**（只读） | — | **仅提议双方**；第三方（含**其他已登录用户**）→ `403 AUTH_NOT_RESOURCE_OWNER`；被处罚用户仍可读取（收尾所需，与 `A-14` 同源，契约未标 `x-forbidden-for-restricted`）**【待确认 Q-13】** |
| `verifySwapCode` #60 | POST /swap-proposals/{proposalId}/swap-code/verify | ❌ | ❌ | 🔒👤 ⛔（**仅双方**） | 🔒👤 ⛔（**仅双方**） | ❌ | 🔒 ⛔ | — | **一次性**：重复核销 → `409 SWAP_CODE_ALREADY_VERIFIED`；第三方 → `403` |
| `confirmSwap` #61 | POST /swap-proposals/{proposalId}/confirm | ❌ | ❌ | 🔒👤 ⛔（**仅双方**） | 🔒👤 ⛔（**仅双方**） | ❌ | 🔒 ⛔ | — | 双方各确认一次，两次齐备才「交换完成」；第三方 → `403` |
| `createSwapReview` #62 | POST /swap-proposals/{proposalId}/reviews | ❌ | ❌ | 🔒👤 ⛔ | 🔒👤 ⛔ | ❌ | 🔒 ⛔ | — | 幂等**必须**；本人=交换双方；前置：交换「已完成」 |

> **交换域的管理员说明**：仅 `#49`、`#54` 允许管理员**只读**（治理取证）；`#55`~`#61` 等一切交换**写**操作对管理员一律 ❌（P-00-A）。
>
> **注意 `getSwapCode`（#59）**：它对被处罚用户**未**标注 `x-forbidden-for-restricted`，属于 `A-14` 的连带允许项（无交换码则无法完成线下核销与收尾）**【待确认 Q-13】**。

### 2.5 §6 管理员治理（tag 11，17 个操作 = 14 个仅管理员 + 3 个用户侧本人接口）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `adminSearchUsers` #63 | GET /admin/users | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 仅管理员；非管理员 → `403 AUTH_ADMIN_REQUIRED`；分页 |
| `adminGetUserDetail` #64 | GET /admin/users/{userId} | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 仅管理员；用户详情 + 交易概览 |
| `adminBanUser` #65 | POST /admin/users/{userId}/ban | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；`reason` **必填**（`VALIDATION_REASON_REQUIRED` 400）；默认封禁 3 天（假设 `A-03`）；已在处罚中 → `409 GOVERNANCE_ACTIVE_PENALTY_EXISTS`（假设拒绝，Q-10）**【待确认 Q-09、Q-10】** |
| `adminListUserPenalties` #66 | GET /admin/users/{userId}/penalties | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 仅管理员；用户侧等价接口为 `listMyPenalties`（#77） |
| `adminListPenalties` #67 | GET /admin/penalties | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 全量处罚记录；时间范围非法 → `VALIDATION_DATE_RANGE_INVALID` 400 |
| `adminLiftPenalty` #68 | POST /admin/penalties/{penaltyId}/lift | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；`reason` 必填；与自动解封并发时只允许一方成功（R-31） |
| `adminTakedownItem` #69 | POST /admin/items/{itemId}/takedown | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；`reason` **必填**；商品 → 违规下架（`BANNED`）；记审计 |
| `adminRestoreItem` #70 | POST /admin/items/{itemId}/restore | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；`reason` 必填；恢复后回到「在售」或「已下架」 |
| `adminListAppeals` #71 | GET /admin/appeals | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 仅管理员；申诉列表 |
| `adminResolveAppeal` #72 | POST /admin/appeals/{appealId}/resolve | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；处理意见必填；通过即解封（与 `adminLiftPenalty` 并发互斥） |
| `adminListReports` #73 | GET /admin/reports | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 仅管理员；举报列表 |
| `adminHandleReport` #74 | POST /admin/reports/{reportId}/handle | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 幂等**必须**；处理结论必填 |
| `adminListAuditLogs` #75 | GET /admin/audit-logs | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 只读；审计日志 |
| `adminListDictionary` #76 | GET /admin/dictionaries/{dictionaryType} | ❌ | ❌ | ❌ | ❌ | 🔒🛡️ | ❌ | — | 迭代一以只读查询为主（Q-21）**【待确认 Q-21】** |
| `listMyPenalties` #77 | GET /users/me/penalties | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ **允许** | — | **本人**=处罚对象；被处罚用户**必须**能看待自己的处罚与解封时间（P-00-C）；管理员不得经此接口读取他人处罚 |
| `createAppeal` #78 | POST /penalties/{penaltyId}/appeals | ❌ | 🔒👤 ⛔★ | 🔒👤 ⛔★ | 🔒👤 ⛔★ | ❌ | 🔒👤 ✅ **允许** | — | 幂等**必须**；**处罚对象本人**；仅针对**生效中**处罚；非本人 → `403 AUTH_NOT_RESOURCE_OWNER`；★此处「⛔」**不适用**——申诉是 P-00-C 明确允许的能力 |
| `listMyAppeals` #79 | GET /appeals/mine | ❌ | 🔒👤 | 🔒👤 | 🔒👤 | ❌ | 🔒👤 ✅ | — | 本人=申诉提交人；`createAppeal` 后可立即查看进度 |

> **管理员唯一允许的「交易资源读取」**：`getItemOccupation`（#27）、`getOrderDetail`（#38）、`getSwapBundle`（#49）、`getSwapProposal`（#54）、`getMyReport`（#35）与管理员治理域的只读接口。这些均**不产生交易写入**，与 P-00-A 不冲突；不得据此扩展出管理员下单/付款/交换能力。
>
> **管理员账号来源【待确认 Q-04】**：预置账号 / 数据库直插 / 注册后提权 / 配置文件四种方式尚未确定，直接影响 `/admin/**` 的鉴权初始化与本文 §5 的可测试性。

### 2.6 §7 系统定时任务（tag 13，4 个内部操作）

| 操作 | 方法与路径 | 访客 | 注册用户 | 卖家 | 买家 | 管理员 | 被处罚用户 | 系统 | 判定与约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `runOrderTimeoutReleaseTask` #80 | POST /internal/tasks/order-timeout-release | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ⏱️🔒 **允许** | 幂等（IDEM-B）：条件更新 `WHERE status='PENDING_PAYMENT'`；**已付款订单绝不被超时取消**；建议每分钟 |
| `runSwapProposalExpireTask` #81 | POST /internal/tasks/swap-proposal-expire | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ⏱️🔒 **允许** | 幂等；释放全部占用且只释放一次；建议每分钟 |
| `runPenaltyAutoUnbanTask` #82 | POST /internal/tasks/**penalty-auto-unban** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ⏱️🔒 **允许** | 幂等：同一处罚只解封一次；与管理员手动解封/申诉通过并发时只允许一方成功（R-31）；建议每分钟 |
| `runReviewWindowCloseTask` #83 | POST /internal/tasks/review-window-close | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ⏱️🔒 **允许** | 关闭未评价入口；重复执行无副作用 |

> **路径提醒（易错）**：`runPenaltyAutoUnbanTask` 的规范路径是 **`/internal/tasks/penalty-auto-unban`**，**不是** `/internal/tasks/penalty-unban`。任何实现、网关规则或测试脚本使用后者即为**契约缺陷**。
>
> 系统任务**不得**：修改用户资料、删除数据、绕过状态机转换、跳过审计（permission-matrix §7）。

---

## 3. 必须登录的接口清单

> 共 **73** 个需登录操作（83 − **10** 公开）。分组口径与 §2 的 6 大域一致。列「处罚用户可用」直接对应契约的 `x-forbidden-for-restricted`。

### 3.1 §2 账户与个人资料（13 个）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 01 | `logout` | POST /auth/logout | ✅ |
| 02 | `getCurrentUser` | GET /auth/me | ✅ |
| 03 | `changePassword` | PUT /auth/password | ✅ |
| 04 | `updateMyProfile` | PUT /users/me/profile | ⛔（仅昵称/头像字段，假设 `A-13`）**【待确认 Q-12】** |
| 05 | `setMyTradeZones` | PUT /users/me/trade-zones | ✅ |
| 06 | `getMyNotificationSettings` | GET /users/me/notification-settings | ✅ |
| 07 | `updateMyNotificationSettings` | PUT /users/me/notification-settings | ✅ |
| 08 | `listMyNotifications` | GET /notifications | ✅ |
| 09 | `getUnreadNotificationCount` | GET /notifications/unread-count | ✅ |
| 10 | `markNotificationsRead` | POST /notifications/read | ✅ |
| 11 | `listMyPenalties` | GET /users/me/penalties | ✅ **（P-00-C 关键）** |
| 12 | `createAppeal` | POST /penalties/{penaltyId}/appeals | ✅ **（P-00-C 关键）** |
| 13 | `listMyAppeals` | GET /appeals/mine | ✅ |

> 本域 = 「01-认证与账户」3 个（`logout`、`getCurrentUser`、`changePassword`；另 3 个为公开）+「02-个人资料」4 个 +「12-通知」3 个 + 治理域本人接口 3 个（`listMyPenalties`、`createAppeal`、`listMyAppeals`）= **13** 个。

### 3.2 §3 商品（12 个）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 15 | `createItem` | POST /items | ⛔ |
| 16 | `listMyItems` | GET /items/mine | ✅ |
| 17 | `updateItem` | PUT /items/{itemId} | ⛔ |
| 18 | `takeItemOffShelf` | POST /items/{itemId}/off-shelf | ✅ **（减损操作）**【待确认 Q-11】 |
| 19 | `relistItem` | POST /items/{itemId}/relist | ⛔ |
| 20 | `updateItemSwapPreference` | PUT /items/{itemId}/swap-preference | ⛔ |
| 21 | `getItemOccupation` | GET /items/{itemId}/occupation | ✅ |
| 22 | `uploadImage` | POST /files/images | ✅ **（取证例外）** |
| 23 | `getCart` | GET /cart | ✅ |
| 24 | `addCartItem` | POST /cart/items | ⛔ |
| 25 | `updateCartItem` | PATCH /cart/items/{cartItemId} | ⛔ |
| 26 | `removeCartItem` | DELETE /cart/items/{cartItemId} | ⛔ |

> 本域 = 「04-商品」的 8 个需登录操作（`createItem`、`listMyItems`、`updateItem`、`takeItemOffShelf`、`relistItem`、`updateItemSwapPreference`、`getItemOccupation`、`uploadImage`）+ 「05-购物车」4 个 = **12** 个。其余商品的 5 个操作（`listItems`、`getItemDetail` 与字典 3 个）为公开。举报 2 个见 §3.6。

### 3.3 §4 购买（11 个）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 27 | `submitOrder` | POST /orders | ⛔ |
| 28 | `listOrders` | GET /orders | ✅（只读） |
| 29 | `getOrderDetail` | GET /orders/{orderId} | ✅（只读） |
| 30 | `mockPayOrder` | POST /orders/{orderId}/mock-pay | ⛔ |
| 31 | `cancelOrder` | POST /orders/{orderId}/cancel | ⛔ |
| 32 | `selectPickupPoint` | PUT /orders/{orderId}/pickup-point | ⛔ |
| 33 | `markOrderDelivered` | POST /orders/{orderId}/deliver | ✅ **（收尾，假设 `A-14`）【待确认 Q-13】** |
| 34 | `confirmOrderReceipt` | POST /orders/{orderId}/confirm-receipt | ✅ **（收尾，假设 `A-14`）【待确认 Q-13】** |
| 35 | `createOrderReview` | POST /orders/{orderId}/reviews | ⛔ |
| 36 | `listOrderReviews` | GET /orders/{orderId}/reviews | ✅（只读） |
| 37 | `listMyReceivedReviews` | GET /users/me/reviews | ✅ |

> 本域 = 「06-订单与付款」8 个 + 「07-评价」3 个 = **11** 个，全部需登录（购买域无公开操作）。

### 3.4 §5 交换（16 个，全部需登录）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 38 | `listMySwapBundles` | GET /swap-bundles | ✅（只读） |
| 39 | `createSwapBundle` | POST /swap-bundles | ⛔ |
| 40 | `getSwapBundle` | GET /swap-bundles/{bundleId} | ✅（只读） |
| 41 | `updateSwapBundle` | PUT /swap-bundles/{bundleId} | ⛔ |
| 42 | `deleteSwapBundle` | DELETE /swap-bundles/{bundleId} | ⛔ |
| 43 | `listSwapProposals` | GET /swap-proposals | ✅（只读） |
| 44 | `createSwapProposal` | POST /swap-proposals | ⛔ |
| 45 | `getSwapProposal` | GET /swap-proposals/{proposalId} | ✅（只读） |
| 46 | `acceptSwapProposal` | POST /swap-proposals/{proposalId}/accept | ⛔ |
| 47 | `rejectSwapProposal` | POST /swap-proposals/{proposalId}/reject | ⛔ |
| 48 | `counterSwapProposal` | POST /swap-proposals/{proposalId}/counter | ⛔ |
| 49 | `cancelSwapProposal` | POST /swap-proposals/{proposalId}/cancel | ⛔ |
| 50 | `getSwapCode` | GET /swap-proposals/{proposalId}/swap-code | ✅（收尾所需）**【待确认 Q-13】** |
| 51 | `verifySwapCode` | POST /swap-proposals/{proposalId}/swap-code/verify | ⛔ |
| 52 | `confirmSwap` | POST /swap-proposals/{proposalId}/confirm | ⛔ |
| 53 | `createSwapReview` | POST /swap-proposals/{proposalId}/reviews | ⛔ |

### 3.5 §6 管理员治理（17 个；其中 14 个 `admin*`，3 个本人接口已在 §3.1 列出）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 54 | `adminSearchUsers` | GET /admin/users | ❌（非管理员一律 403） |
| 55 | `adminGetUserDetail` | GET /admin/users/{userId} | ❌ |
| 56 | `adminBanUser` | POST /admin/users/{userId}/ban | ❌ |
| 57 | `adminListUserPenalties` | GET /admin/users/{userId}/penalties | ❌ |
| 58 | `adminListPenalties` | GET /admin/penalties | ❌ |
| 59 | `adminLiftPenalty` | POST /admin/penalties/{penaltyId}/lift | ❌ |
| 60 | `adminTakedownItem` | POST /admin/items/{itemId}/takedown | ❌ |
| 61 | `adminRestoreItem` | POST /admin/items/{itemId}/restore | ❌ |
| 62 | `adminListAppeals` | GET /admin/appeals | ❌ |
| 63 | `adminResolveAppeal` | POST /admin/appeals/{appealId}/resolve | ❌ |
| 64 | `adminListReports` | GET /admin/reports | ❌ |
| 65 | `adminHandleReport` | POST /admin/reports/{reportId}/handle | ❌ |
| 66 | `adminListAuditLogs` | GET /admin/audit-logs | ❌ |
| 67 | `adminListDictionary` | GET /admin/dictionaries/{dictionaryType} | ❌ |
| 68 | `listMyPenalties` | GET /users/me/penalties | ✅ **（已在 §3.1 编号 11 列出）** |
| 69 | `createAppeal` | POST /penalties/{penaltyId}/appeals | ✅ **（已在 §3.1 编号 12 列出）** |
| 70 | `listMyAppeals` | GET /appeals/mine | ✅ **（已在 §3.1 编号 13 列出）** |

> 本域 17 个操作全部需登录；其中 14 个为 `admin*`（`x-required-roles: [ADMIN]`），3 个为用户侧本人接口（`x-required-roles: [USER, RESTRICTED]`，上文以「已在 §3.1 列出」注明，避免重复计数）。

### 3.6 举报域需登录操作（2 个）

| # | operationId | 方法与路径 | 处罚用户可用 |
| --- | --- | --- | --- |
| 71 | `listMyReports` | GET /users/me/reports | ✅ |
| 72 | `getMyReport` | GET /reports/{reportId} | ✅ |

### 3.7 §7 系统内部操作（4 个，不使用用户登录态）

| # | operationId | 方法与路径 | 认证方式 |
| --- | --- | --- | --- |
| 73 | `runOrderTimeoutReleaseTask` | POST /internal/tasks/order-timeout-release | `X-Internal-Token`（**非** JWT） |
| 74 | `runSwapProposalExpireTask` | POST /internal/tasks/swap-proposal-expire | 同上 |
| 75 | `runPenaltyAutoUnbanTask` | POST /internal/tasks/penalty-auto-unban | 同上 |
| 76 | `runReviewWindowCloseTask` | POST /internal/tasks/review-window-close | 同上 |

> 上表编号 73~76 为**展示序号**（按内部鉴权而非 JWT 归类）；实际「需登录（JWT）」为 **73 个 = 83 − 10**，其中管理员 14 个、系统 4 个（系统不计入 JWT 登录，故下文计数表分开列示）。

### 3.8 计数校验（与 [`api-contract.md`](./api-contract.md) §1 总览逐项一致）

> 口径以 **operation 级 `security: []`** 为准（路径级与操作级重复声明只计一次操作）。
> 判定方式：遍历 83 个操作，某操作的 `security` 为空数组即视为**公开**。

| 口径 | 本文档核出数量 | `api-contract.md` 总览 | 一致性 |
| --- | --- | --- | --- |
| **公开（无需登录）** | **10** —— `registerUser`、`login`、`refreshToken`、`getPublicProfile`、`listTradeZones`、`listPickupPointsByZone`、`listCategories`、`listItemConditions`、`listItems`、`getItemDetail` | **10** | ✅ 一致 |
| **需登录（JWT，含管理员）** | **73** | **73** | ✅ 一致 |
| **仅管理员**（`x-required-roles: [ADMIN]`） | **14**（全部为 `admin*` 前缀） | **14** | ✅ 一致 |
| **管理员可访问的治理操作**（含 `[USER, ADMIN]` 的 `getSwapBundle`、`getItemDetail` 等只读旁路） | 15 | 不单列 | 口径不同，非差异 |
| **系统内部**（`x-required-roles: [SYSTEM]`） | **4** | **4** | ✅ 一致 |
| **业务操作合计** | **83** | **83** | ✅ 一致 |

**校验恒等式**：`公开 10 + 需登录 73 = 83` ✅；`仅管理员 14 ⊆ 需登录 73` ✅。

> **说明（消除历史歧义）**：早期版本曾把「公开操作」误计为 11，原因是把
> `openapi.yaml` 中**路径级与操作级重复出现的 `security: []`** 各计一次。
> 正确口径是**按操作计数**，即 **10 个公开操作**。`api-contract.md` §1 与本表已统一为 10 / 73。

## 4. 必须本人的接口清单

> 「本人」= 资源归属 / 交易参与方校验。**校验失败一律 `403 AUTH_NOT_RESOURCE_OWNER`，不得返回 404**（P-00-B）。商品域的等价别名：`ITEM_NOT_OWN_SELLER`（同为 403）。

| operationId | 方法与路径 | 「本人」的确切含义 | 反例（非本人）| 主要错误码 |
| --- | --- | --- | --- | --- |
| `changePassword` | PUT /auth/password | **账号自身**（令牌所属 `userId`） | 传他人 `userId` 或试图用他人令牌修改 | `403 AUTH_NOT_RESOURCE_OWNER` / `401 AUTH_INVALID_CREDENTIALS` |
| `updateMyProfile` | PUT /users/me/profile | **账号自身**（路径为 `/users/me`，无跨用户入口） | 无（接口本身不接受他人 ID） | `403 USER_RESTRICTED_CANNOT_WRITE`（昵称/头像） |
| `setMyTradeZones` | PUT /users/me/trade-zones | **账号自身** | 无 | `422 USER_TRADE_ZONE_REQUIRED` |
| `getMyNotificationSettings` | GET /users/me/notification-settings | **账号自身**（通知收件人） | 无 | `401` |
| `updateMyNotificationSettings` | PUT /users/me/notification-settings | **账号自身**（通知收件人） | 无 | `401` |
| `listMyItems` | GET /items/mine | **商品卖家** | 无（集合天然按卖家过滤） | `401` |
| `updateItem` | PUT /items/{itemId} | **商品卖家**（`item.sellerId == 令牌 userId`） | 用户 A 编辑用户 B 的商品 | `403 AUTH_NOT_RESOURCE_OWNER` / `403 ITEM_NOT_OWN_SELLER` |
| `takeItemOffShelf` | POST /items/{itemId}/off-shelf | **商品卖家** | 下架他人商品 | `403 AUTH_NOT_RESOURCE_OWNER` / `409 ITEM_LOCKED_BY_OCCUPATION` |
| `relistItem` | POST /items/{itemId}/relist | **商品卖家** | 上架他人商品 | `403 AUTH_NOT_RESOURCE_OWNER` / `409 ITEM_BANNED_CANNOT_RELIST` |
| `updateItemSwapPreference` | PUT /items/{itemId}/swap-preference | **商品卖家** | 改他人商品的交换意愿 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `getItemOccupation` | GET /items/{itemId}/occupation | **商品卖家**；或 **管理员（治理只读）** | 第三方买家读取他人商品占用 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `getCart` | GET /cart | **购物车所有者** | 无（集合天然按所有者过滤） | `401` |
| `addCartItem` | POST /cart/items | **购物车所有者**（且不得加自己的商品） | 加自己的商品 | `422 ITEM_NOT_PURCHASABLE` / `403 AUTH_NOT_RESOURCE_OWNER` |
| `updateCartItem` | PATCH /cart/items/{cartItemId} | **购物车条目的所有者**（`cartItem.userId == 令牌 userId`） | 修改他人购物车条目 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `removeCartItem` | DELETE /cart/items/{cartItemId} | **购物车条目的所有者** | 删除他人购物车条目 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `listOrders` | GET /orders | **订单参与方**（`buyerId` 或 `sellerId` 命中） | 无（集合按参与方过滤） | `401` |
| `getOrderDetail` | GET /orders/{orderId} | **订单参与方**；或 **管理员（只读）** | 非参与方查看他人订单 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `mockPayOrder` | POST /orders/{orderId}/mock-pay | **订单买家**（`buyerId == 令牌 userId`） | 卖家或第三方代付 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `cancelOrder` | POST /orders/{orderId}/cancel | **订单买家** | 卖家代取消 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `selectPickupPoint` | PUT /orders/{orderId}/pickup-point | **订单买家** | 卖家选点 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `markOrderDelivered` | POST /orders/{orderId}/deliver | **订单卖家** | 买家自行标记交付 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `confirmOrderReceipt` | POST /orders/{orderId}/confirm-receipt | **订单买家** | 卖家自行确认收货 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `createOrderReview` | POST /orders/{orderId}/reviews | **交易参与方**（买卖双方各一次，且订单「已完成」） | 第三方评价该订单 | `403 AUTH_NOT_RESOURCE_OWNER` / `409 REVIEW_TRANSACTION_NOT_COMPLETED` |
| `listOrderReviews` | GET /orders/{orderId}/reviews | **交易参与方** | 第三方读取订单评价 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `listMyReceivedReviews` | GET /users/me/reviews | **被评价人（账号自身）** | 无 | `401` |
| `listMySwapBundles` | GET /swap-bundles | **交换包所有者** | 无 | `401` |
| `createSwapBundle` | POST /swap-bundles | **包主（创建者）**，且包内商品均属本人 | 加入他人商品 | `403 AUTH_NOT_RESOURCE_OWNER` / `422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE` |
| `getSwapBundle` | GET /swap-bundles/{bundleId} | **交换包所有者**；或 **管理员（只读）** | 第三方读取他人交换包 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `updateSwapBundle` | PUT /swap-bundles/{bundleId} | **交换包所有者（包主）** | 第三方编辑他人交换包 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `deleteSwapBundle` | DELETE /swap-bundles/{bundleId} | **交换包所有者（包主）** | 第三方删除他人交换包 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `listSwapProposals` | GET /swap-proposals | **提议参与方**（发起方或目标方） | 无（集合按参与方过滤） | `401` |
| `createSwapProposal` | POST /swap-proposals | **发起方（提议人）**，且包与目标商品均属本人/本人有权 | 用他人交换包发起 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `getSwapProposal` | GET /swap-proposals/{proposalId} | **提议双方**；或 **管理员（只读）** | 第三方读取提议 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `acceptSwapProposal` | POST /swap-proposals/{proposalId}/accept | **目标方（targetUserId）** | 发起方自接受、第三方接受 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `rejectSwapProposal` | POST /swap-proposals/{proposalId}/reject | **目标方** | 发起方自拒绝 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `counterSwapProposal` | POST /swap-proposals/{proposalId}/counter | **目标方** | 发起方反提案 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `cancelSwapProposal` | POST /swap-proposals/{proposalId}/cancel | **发起方（提议人）** | 目标方取消 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `getSwapCode` | GET /swap-proposals/{proposalId}/swap-code | **提议双方**（严格两人） | 第三方（含其他登录用户）读取交换码 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `verifySwapCode` | POST /swap-proposals/{proposalId}/swap-code/verify | **提议双方** | 第三方核销 | `403 AUTH_NOT_RESOURCE_OWNER` / `409 SWAP_CODE_ALREADY_VERIFIED` |
| `confirmSwap` | POST /swap-proposals/{proposalId}/confirm | **提议双方**（各确认一次） | 第三方确认 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `createSwapReview` | POST /swap-proposals/{proposalId}/reviews | **交换双方** | 第三方评价该交换 | `403 AUTH_NOT_RESOURCE_OWNER` / `409 REVIEW_TRANSACTION_NOT_COMPLETED` |
| `listMyReports` | GET /users/me/reports | **举报发起人（账号自身）** | 无 | `401` |
| `getMyReport` | GET /reports/{reportId} | **举报发起人**；或 **管理员** | 他人读取该举报 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `listMyPenalties` | GET /users/me/penalties | **处罚对象（账号自身）** | 无（`/users/me`） | `401` |
| `createAppeal` | POST /penalties/{penaltyId}/appeals | **处罚对象本人**（`penalty.userId == 令牌 userId`） | 用户 A 替他人生成申诉 | `403 AUTH_NOT_RESOURCE_OWNER` |
| `listMyAppeals` | GET /appeals/mine | **申诉提交人（账号自身）** | 无 | `401` |
| `listMyNotifications` | GET /notifications | **通知收件人（账号自身）** | 无 | `401` |
| `getUnreadNotificationCount` | GET /notifications/unread-count | **通知收件人（账号自身）** | 无 | `401` |
| `markNotificationsRead` | POST /notifications/read | **通知收件人**（仅能标记自己的通知 ID） | 用他人通知 ID 标记 | `403 AUTH_NOT_RESOURCE_OWNER`或静默忽略 |

> **合计**：本清单 **50** 个操作带「本人 / 参与方」语义。其中 4 个（`getItemOccupation`、`getOrderDetail`、`getSwapBundle`、`getSwapProposal`）与 1 个（`getMyReport`）为「本人**或管理员（只读）**」；其余均为严格本人。
>
> **主键级越权是必测项**：所有以 `{cartItemId}`、`{orderId}`、`{bundleId}`、`{proposalId}`、`{reportId}`、`{penaltyId}`、`{itemId}` 为路径参数的接口，**必须**在服务端由「路径 ID → 资源 → 归属比对」推导权限，**禁止**信任任何客户端传入的 `userId` / `ownerId` 字段。

---

## 5. 仅管理员接口清单

> **核心清单共 14 个**，即 `x-required-roles: [ADMIN]` 且路径前缀 `/admin/` 的 `admin*` 操作。非管理员（含被处罚用户、卖家、买家）调用一律 `403 AUTH_ADMIN_REQUIRED`；未登录调用 `401 AUTH_UNAUTHENTICATED`。

| # | operationId | 方法与路径 | 幂等 | 必填 `reason` |
| --- | --- | --- | --- | --- |
| 1 | `adminSearchUsers` | GET /admin/users | — | 否 |
| 2 | `adminGetUserDetail` | GET /admin/users/{userId} | — | 否 |
| 3 | `adminBanUser` | POST /admin/users/{userId}/ban | **必须** | ✅ 是 |
| 4 | `adminListUserPenalties` | GET /admin/users/{userId}/penalties | — | 否 |
| 5 | `adminListPenalties` | GET /admin/penalties | — | 否 |
| 6 | `adminLiftPenalty` | POST /admin/penalties/{penaltyId}/lift | **必须** | ✅ 是 |
| 7 | `adminTakedownItem` | POST /admin/items/{itemId}/takedown | **必须** | ✅ 是 |
| 8 | `adminRestoreItem` | POST /admin/items/{itemId}/restore | **必须** | ✅ 是 |
| 9 | `adminListAppeals` | GET /admin/appeals | — | 否 |
| 10 | `adminResolveAppeal` | POST /admin/appeals/{appealId}/resolve | **必须** | ✅ 是（处理意见） |
| 11 | `adminListReports` | GET /admin/reports | — | 否 |
| 12 | `adminHandleReport` | POST /admin/reports/{reportId}/handle | **必须** | ✅ 是（处理结论） |
| 13 | `adminListAuditLogs` | GET /admin/audit-logs | — | 否 |
| 14 | `adminListDictionary` | GET /admin/dictionaries/{dictionaryType} | — | 否 |

### 5.1 「15」的口径说明（消除 api-contract 与本文档的计数差）

[`api-contract.md`](./api-contract.md) §1 总览写「仅管理员操作 **15**」，而 [`openapi.yaml`](./openapi.yaml) 中 `x-required-roles: [ADMIN]` 实际为 **14** 处。两种口径的差额来源如下：

| 口径 | 数量 | 组成 | 说明 |
| --- | --- | --- | --- |
| A（严格） | **14** | `x-required-roles: [ADMIN]` 的操作 | 与 `openapi.yaml` 逐字一致 |
| B（管理员可访问的治理操作） | **15** | 14 个 `admin*` + `getSwapBundle`（`x-required-roles: [USER, ADMIN]`，管理员只读） | 总览可能采用此口径 |

> **【待确认】**：需在冻结评审时确定采用口径 A 还是 B，并同步 [`api-contract.md`](./api-contract.md) §1 总览。**本文档 §2.4 已把 `getSwapBundle` 记为「包主或管理员只读」**，与口径 B 的解释相容；但 §3.8 计数表以口径 A 为准。**以 `openapi.yaml` 为准**。

---

## 6. 系统内部接口清单

> 共 **4** 个，全部位于 `/internal/**`，`x-required-roles: [SYSTEM]` 且标注 `x-internal: true`。

| # | operationId | 方法与路径 | `security` | 幂等语义 | 调度（建议） |
| --- | --- | --- | --- | --- | --- |
| 1 | `runOrderTimeoutReleaseTask` | POST **/internal/tasks/order-timeout-release** | `internalToken: []` | IDEM-B（条件更新） | 每分钟 |
| 2 | `runSwapProposalExpireTask` | POST **/internal/tasks/swap-proposal-expire** | `internalToken: []` | IDEM-B（条件更新） | 每分钟 |
| 3 | `runPenaltyAutoUnbanTask` | POST **/internal/tasks/penalty-auto-unban** | `internalToken: []` | IDEM-B（条件更新） | 每分钟 |
| 4 | `runReviewWindowCloseTask` | POST **/internal/tasks/review-window-close** | `internalToken: []` | 重复执行无副作用 | 每小时 |

### 6.1 `X-Internal-Token` 与 `ROLE_SYSTEM`

| 项 | 约定 |
| --- | --- |
| 安全方案 | `internalToken`（`type: apiKey`、`in: header`、`name: X-Internal-Token`） |
| 与用户 JWT 的关系 | **不是**用户 JWT：内部接口**不使用** `bearerAuth`；携带用户令牌调用 `/internal/**` 一律拒绝 |
| 等价身份 | 持有有效 `X-Internal-Token` 的调用方被视为 `ROLE_SYSTEM` 服务账号 |
| 凭证来源 | 由部署环境注入（环境变量），**不得**下发到浏览器端，**不得**写入前端产物或日志 |
| 鉴权失败 | `401`（未携带或凭证无效）；**不得**降级为 `ROLE_ADMIN` 或 `ROLE_USER` |
| 审计 | 系统任务同样需要写审计/通知，但**不得**修改用户资料、删除数据、绕过状态机、跳过审计 |

### 6.2 网关要求（强制）

| 编号 | 要求 | 说明 |
| --- | --- | --- |
| GW-01 | 生产环境**必须**在网关/反向代理层**禁止外部访问** `/internal/` 前缀 | [`openapi.yaml`](./openapi.yaml) `x-internal-security-note` 明确要求 |
| GW-02 | 内部路由**仅**允许内网 / 服务网格 / 定时任务调度器来源 | 外部请求应返回 404 或 403，且**不得**暴露该前缀存在性 |
| GW-03 | 迭代一**不提供**该前缀的公开文档入口 | 对外 Swagger/API 文档需过滤 `/internal/**` 与 `x-internal: true` 的操作 |
| GW-04 | `/internal/**` 不得出现在前端任何可调用清单中 | 前端 SDK 生成时应做 tag/`x-internal` 过滤 |
| GW-05 | 人工触发**不提供**公开入口 | permission-matrix §9 PERM-07；如需运维手动触发，必须走独立管理通道并登记 |

---

## 7. 被处罚用户（`ROLE_RESTRICTED`）能力白名单

> 白名单 = **允许**；白名单之外的一切**交易类写入** = **禁止**，返回 `403 USER_RESTRICTED_CANNOT_WRITE`，响应 `details` 必须含 `restrictedUntil`（及 `penaltyId`），前端展示处罚提示条 + 解封时间 + 申诉入口。

### 7.1 允许（白名单）

| # | 能力 | 对应 operationId | 备注 |
| --- | --- | --- | --- |
| R-01 | 登录 / 退出 | `login`、`logout` | 必须能登录才能申诉；重复退出幂等 |
| R-02 | 携带令牌访问 / 获取当前用户 | `getCurrentUser` | 响应含 `status=RESTRICTED` 与 `restrictedUntil` |
| R-03 | 浏览商品列表与详情 / 搜索 | `listItems`、`getItemDetail` | 只读；公开端点本身即允许 |
| R-04 | 查看自己的个人资料 | `getPublicProfile`（自视）、`listMyItems` | 只读 |
| R-05 | 查看自己的处罚与解封时间 | `listMyPenalties` | **P-00-C 关键**：用户须知道何时解封 |
| R-06 | 提交申诉 / 查看自己的申诉 | `createAppeal`、`listMyAppeals` | 幂等**必须**；仅针对**生效中**处罚（Q-16/Q-17）**【待确认】** |
| R-07 | 查看自己的历史订单（只读） | `listOrders`、`getOrderDetail` | 不产生新写入 |
| R-08 | 查看自己的交换（只读） | `listMySwapBundles`、`getSwapBundle`、`listSwapProposals`、`getSwapProposal`、`getSwapCode` | 不产生新写入；`getSwapCode` 为 `A-14` 收尾所需**【待确认 Q-13】** |
| R-09 | 下架自己的商品 | `takeItemOffShelf` | 属「减损」操作，允许（契约 `x-forbidden-for-restricted: false`）**【待确认 Q-11】** |
| R-10 | 举报 | `reportItem` | 治理相关；`reason` 必填；重复举报幂等 |
| R-11 | 上传图片 | `uploadImage` | **取证例外**：用于举报举证；用于发布商品时由 `createItem` 拦截 |
| R-12 | 查看 / 修改常用交易区域 | `setMyTradeZones` | 非交易写入 |
| R-13 | 修改密码 | `changePassword` | 需校验原密码 |
| R-14 | 查看通知 / 标记已读 / 查看与修改通知设置 | `listMyNotifications`、`getUnreadNotificationCount`、`markNotificationsRead`、`getMyNotificationSettings`、`updateMyNotificationSettings` | 非交易写入 |

### 7.2 禁止（白名单之外）

| # | 能力 | 对应 operationId | 错误码 |
| --- | --- | --- | --- |
| D-01 | 发布商品 | `createItem` | `403 USER_RESTRICTED_CANNOT_WRITE` |
| D-02 | 编辑商品 | `updateItem` | 同上 |
| D-03 | 重新上架 | `relistItem` | 同上 |
| D-04 | 修改昵称与头像 | `updateMyProfile`（字段级，**假设 `A-13`**） | 同上**【待确认 Q-12】** |
| D-05 | 修改商品交换意愿 | `updateItemSwapPreference` | 同上 |
| D-06 | 加购物车 / 修改 / 移除条目 | `addCartItem`、`updateCartItem`、`removeCartItem` | 同上（`getCart` **允许**，仅读） |
| D-07 | 下单 | `submitOrder` | 同上 |
| D-08 | 付款 | `mockPayOrder` | 同上 |
| D-09 | 取消订单 / 选择取货点 | `cancelOrder`、`selectPickupPoint` | 同上 |
| D-10 | 创建交换包 / 编辑 / 删除 | `createSwapBundle`、`updateSwapBundle`、`deleteSwapBundle` | 同上 |
| D-11 | 发起交换 | `createSwapProposal` | 同上 |
| D-12 | 接受 / 拒绝 / 反提案 | `acceptSwapProposal`、`rejectSwapProposal`、`counterSwapProposal` | 同上 |
| D-13 | 取消交换提议 | `cancelSwapProposal` | 同上 |
| D-14 | 核销交换码 | `verifySwapCode` | 同上 |
| D-15 | 确认交换 | `confirmSwap` | 同上 |
| D-16 | 评价（订单 / 交换） | `createOrderReview`、`createSwapReview` | 同上 |
| D-17 | 卖家交付 / 买家确认收货 | `markOrderDelivered`、`confirmOrderReceipt` | **例外见 §7.3（假设 `A-14`）** |

### 7.3 **假设 A-14（必须显式实现，标为【待确认 Q-13】）**

> **假设 A-14**：处罚前**已存在**的进行中订单/交换，其「**收尾操作**」默认**允许**执行，否则交易将**永久卡死**（商品被占用、双方无法交付/确认、订单无法终结）。

| operationId | 方法与路径 | 默认判定 | 适用条件 | 若 Q-13 结论为「禁止」的后果 |
| --- | --- | --- | --- | --- |
| `markOrderDelivered` #42 | POST /orders/{orderId}/deliver | ✅ **允许** | 仅**处罚生效前**已存在的进行中订单；需已选定取货点 | 卖家无法交付 → 订单永久停留「已付款」；商品占用无法释放 |
| `confirmOrderReceipt` #38→#43 | POST /orders/{orderId}/confirm-receipt | ✅ **允许** | 同上 | 买家无法确认收货 → 订单永久不完成；卖家收不到交易终结 |
| `getSwapCode` #59 | GET /swap-proposals/{proposalId}/swap-code | ✅ **允许（连带）** | 仅提议双方；用于线下核销 | 双方无法取得交换码 → 交换无法核销 |
| `verifySwapCode` #60 | POST /swap-proposals/{proposalId}/swap-code/verify | ⛔ **禁止**（契约标注 `x-forbidden-for-restricted: true`） | — | **与 A-14 存在张力**：若已存在交换需要收尾，该点需一并裁决**【待确认 Q-13】** |
| `confirmSwap` #61 | POST /swap-proposals/{proposalId}/confirm | ⛔ **禁止**（契约标注 `x-forbidden-for-restricted: true`） | — | 同上，**必须与 Q-13 一并裁决** |
| `rejectSwapProposal` #56 | POST /swap-proposals/{proposalId}/reject | ⛔ **禁止** | — | 目标方无法拒绝在途提议 → 占用可能被超时任务释放（`runSwapProposalExpireTask`） |

> **裁决要求**：`A-14` 目前**仅覆盖订单收尾两个操作**（`markOrderDelivered`、`confirmOrderReceipt`），这是「交易不卡死」与「处罚有威慑」之间的折中。**交换域（#59~#61）的处罚行为存在规则张力，必须在接口冻结前由 Q-13 给出唯一结论**，并同步修改 [`openapi.yaml`](./openapi.yaml) 的 `x-forbidden-for-restricted` 与本文档 §7.1 / §7.2。
>
> **实现约束**：判定「处罚前已存在」必须依据**订单/交换的创建时间早于处罚生效时间**（服务端时间，`Asia/Shanghai` 展示 / UTC 存储，假设 `A-20`），**不得**依据客户端传入时间。

---

## 8. 越权测试要点

> 每一条均为**负向用例**：期望结果为**拒绝**。测试必须校验 **HTTP 状态码 + 业务错误码** 双重断言，并额外断言「**不返回 404 以免泄露资源存在性**」且「**不产生任何写副作用**」。

### 8.1 归属类越权（P-00-B）

| # | 用例 | 构造 | 期望 |
| --- | --- | --- | --- |
| T-01 | 用户 A 编辑用户 B 的商品 | A 的令牌 + B 商品的 `itemId` 调 `PUT /items/{itemId}` | `403 AUTH_NOT_RESOURCE_OWNER`（或商品域别名 `403 ITEM_NOT_OWN_SELLER`）；B 的商品字段**零变更** |
| T-02 | 用户 A 下架 / 重新上架 / 改交换意愿 B 的商品 | 同上三个接口 | 均 `403 AUTH_NOT_RESOURCE_OWNER`（`relistItem` 亦不得因处罚判断先于归属而返回 409） |
| T-03 | 用户 A 查看 B 商品的占用情况 | A 调 `GET /items/{itemId}/occupation` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-04 | 用户 A 修改 / 删除 B 的购物车条目 | A 调 `PATCH`/`DELETE /cart/items/{cartItemId}`（B 的条目 ID） | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-05 | 非参与方查看订单 | 用户 C（既非买家也非卖家）调 `GET /orders/{orderId}` | `403 AUTH_NOT_RESOURCE_OWNER`；**不得**返回 404 |
| T-06 | 非参与方读取订单评价 | 用户 C 调 `GET /orders/{orderId}/reviews` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-07 | 卖家代替买家付款 | 卖家令牌调 `POST /orders/{orderId}/mock-pay` | `403 AUTH_NOT_RESOURCE_OWNER`；订单状态不变 |
| T-08 | 买家自行标记「已交付」 | 买家令牌调 `POST /orders/{orderId}/deliver` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-09 | 卖家自行「确认收货」 | 卖家令牌调 `POST /orders/{orderId}/confirm-receipt` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-10 | 非订单参与方评价 | 用户 C 调 `POST /orders/{orderId}/reviews` | `403 AUTH_NOT_RESOURCE_OWNER`（先于 `409 REVIEW_TRANSACTION_NOT_COMPLETED`） |
| T-11 | 第三方编辑 / 删除他人交换包 | 用户 C 调 `PUT`/`DELETE /swap-bundles/{bundleId}` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-12 | 第三方读取他人交换包 / 交换提议 | 用户 C 调 `GET /swap-bundles/{bundleId}`、`GET /swap-proposals/{proposalId}` | 均 `403 AUTH_NOT_RESOURCE_OWNER` |
| T-13 | 在他人交换包中加入自有商品 | A 用 B 的 `bundleId` 调 `PUT /swap-bundles/{bundleId}` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-14 | 用他人交换包发起提议 | A 用 B 的 `bundleId` 调 `POST /swap-proposals` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-15 | 非举报人读取他人举报 | 用户 A 调 `GET /reports/{reportId}`（B 的举报） | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-16 | 用户 A 替用户 B 提交申诉 | A 的令牌 + B 的 `penaltyId` 调 `POST /penalties/{penaltyId}/appeals` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-17 | 标记他人通知为已读 | A 调 `POST /notifications/read` 携带 B 的通知 ID | `403 AUTH_NOT_RESOURCE_OWNER` **或**静默忽略（择一实现，但必须**不改变** B 的通知状态） |

### 8.2 交换域角色越权（仅目标方 / 仅发起方 / 仅双方）

| # | 用例 | 构造 | 期望 |
| --- | --- | --- | --- |
| T-18 | **发起方**调用 `acceptSwapProposal` | 提议发起方令牌调 `POST /swap-proposals/{proposalId}/accept` | `403 AUTH_NOT_RESOURCE_OWNER`（**仅目标方**） |
| T-19 | **第三方**调用 `acceptSwapProposal` | 无关用户令牌 | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-20 | 发起方调用 `rejectSwapProposal` / `counterSwapProposal` | 发起方令牌 | 均 `403 AUTH_NOT_RESOURCE_OWNER` |
| T-21 | **目标方**调用 `cancelSwapProposal` | 目标方令牌 | `403 AUTH_NOT_RESOURCE_OWNER`（**仅发起方**） |
| T-22 | **非提议双方读取交换码** | 无关登录用户调 `GET /swap-proposals/{proposalId}/swap-code` | `403 AUTH_NOT_RESOURCE_OWNER`；响应体**不得**包含任何交换码片段 |
| T-23 | 第三方核销交换码 | 无关用户调 `POST /swap-proposals/{proposalId}/swap-code/verify` | `403 AUTH_NOT_RESOURCE_OWNER`；**不得**消耗核销次数（交换码仍有效） |
| T-24 | 第三方确认交换 | 无关用户调 `POST /swap-proposals/{proposalId}/confirm` | `403 AUTH_NOT_RESOURCE_OWNER`；双方确认计数不变 |
| T-25 | 第三方评价交换 | 无关用户调 `POST /swap-proposals/{proposalId}/reviews` | `403 AUTH_NOT_RESOURCE_OWNER` |
| T-26 | 已终结提议重复接受 | 目标方在提议已接受后再调 `accept` | `409 SWAP_PROPOSAL_ALREADY_FINALIZED`（幂等命中按契约返回首次结果，**不得**产生第二次占用） |

### 8.3 管理员边界（P-00-A）

| # | 用例 | 构造 | 期望 |
| --- | --- | --- | --- |
| T-27 | 非管理员调用 `/admin/**` | 普通用户调 `GET /admin/users`、`POST /admin/users/{userId}/ban` 等 | `403 AUTH_ADMIN_REQUIRED`；**不产生**任何治理写副作用 |
| T-28 | 被处罚用户调用 `/admin/**` | `ROLE_RESTRICTED` 令牌 | `403 AUTH_ADMIN_REQUIRED`（管理员判定优先，**不得**返回 `USER_RESTRICTED_CANNOT_WRITE` 混淆语义） |
| T-29 | **管理员下单** | `ROLE_ADMIN` 调 `POST /orders` | `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN`（P-00-A）；**不占用**商品 |
| T-30 | 管理员发布 / 编辑商品 | `POST /items`、`PUT /items/{itemId}` | `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN` |
| T-31 | 管理员加购物车 / 付款 / 确认收货 | `POST /cart/items`、`mock-pay`、`confirm-receipt` | 均 `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN`（管理员**不得**以「治理」名义写交易） |
| T-32 | 管理员创建交换包 / 发起交换 / 接受 / 确认 | `POST /swap-bundles`、`POST /swap-proposals`、`accept`、`confirmSwap` | 均 `403 AUTH_FORBIDDEN_TRADING_AS_ADMIN` |
| T-33 | 管理员越权改他人资料 | 管理员调 `PUT /users/me/profile` 试图作用于他人 | 接口仅作用于自身；**不得**存在管理员改他人昵称/头像的路径 |
| T-34 | 管理员经用户侧接口读全量 | 管理员调 `GET /orders`、`GET /swap-proposals` 期望全量 | 仅返回其**自身参与**的集合（管理员无参与）；治理需走 `/admin/**` |
| T-35 | 管理员治理写操作缺 `reason` | `POST /admin/items/{itemId}/takedown` 不带 `reason` | `400 VALIDATION_REASON_REQUIRED` |

### 8.4 被处罚用户（`ROLE_RESTRICTED`）

| # | 用例 | 构造 | 期望 |
| --- | --- | --- | --- |
| T-36 | **被处罚用户下单** | `ROLE_RESTRICTED` 调 `POST /orders` | `403 USER_RESTRICTED_CANNOT_WRITE`，`details.restrictedUntil` 非空 |
| T-37 | 被处罚用户发布 / 编辑 / 重新上架 / 改交换意愿商品 | `POST /items`、`PUT /items/{itemId}`、`relist`、`swap-preference` | 均 `403 USER_RESTRICTED_CANNOT_WRITE` |
| T-38 | 被处罚用户加购物车 / 付款 / 取消 / 选点 | `addCartItem`、`mock-pay`、`cancel`、`pickup-point` | 均 `403 USER_RESTRICTED_CANNOT_WRITE` |
| T-39 | 被处罚用户创建交换包 / 发起 / 接受 / 拒绝 / 反提案 / 取消 | 对应 6 个接口 | 均 `403 USER_RESTRICTED_CANNOT_WRITE` |
| T-40 | 被处罚用户核销交换码 / 确认交换 / 评价 | `verifySwapCode`、`confirmSwap`、`createOrderReview`、`createSwapReview` | 均 `403 USER_RESTRICTED_CANNOT_WRITE` |
| T-41 | 被处罚用户改昵称 / 头像 | `PUT /users/me/profile` 携带昵称或头像字段 | `403 USER_RESTRICTED_CANNOT_WRITE`（假设 `A-13`）**【待确认 Q-12】** |
| T-42 | **被处罚用户仍可登录** | `POST /auth/login` | `200`；响应 `status=RESTRICTED` + `restrictedUntil`（**不得**返回 403） |
| T-43 | **被处罚用户仍可浏览与查看自己的处罚** | `GET /items`、`GET /items/{itemId}`、`GET /users/me/penalties` | 均 `200`；处罚记录含解封时间（P-00-C） |
| T-44 | **被处罚用户仍可申诉** | `POST /penalties/{penaltyId}/appeals`（自己的生效处罚） | `201`/`200`；幂等 |
| T-45 | **被处罚用户仍可举报 / 上传图片 / 改区域 / 改密码 / 查看通知** | `reportItem`、`uploadImage`、`setMyTradeZones`、`changePassword`、`notifications` 三接口 | 均成功（白名单 R-10~R-14） |
| T-46 | **被处罚用户仍可只读查看历史订单与交换** | `listOrders`、`getOrderDetail`、`listMySwapBundles`、`getSwapProposal` | 均 `200`，且**不产生**任何状态推进 |
| T-47 | **A-14 收尾操作允许** | 处罚**前**已存在的进行中订单：卖家调 `deliver`、买家调 `confirm-receipt` | 均成功**【待确认 Q-13】** |
| T-48 | A-14 边界——处罚前不存在的订单 | 若存在处罚**后**由系统/管理员产生的订单，被处罚用户调 `confirm-receipt` | 按 Q-13 结论裁决；**默认**仍以「处罚前已存在」为唯一放行条件**【待确认 Q-13】** |
| T-49 | 处罚拦截优先于业务前置条件 | 被处罚用户对「不存在」的商品调 `PUT /items/{itemId}` | 按 §1.2 顺序：先 `403 USER_RESTRICTED_CANNOT_WRITE`（或先 401/403），**不得**先返回 `404 ITEM_NOT_FOUND` 泄露存在性 |

### 8.5 认证与令牌

| # | 用例 | 构造 | 期望 |
| --- | --- | --- | --- |
| T-50 | 未登录调用需登录接口 | 无 `Authorization` 头调 `GET /orders` | `401 AUTH_UNAUTHENTICATED` |
| T-51 | 过期令牌 | 伪造/过期 7 天令牌调 `GET /auth/me` | `401 AUTH_TOKEN_EXPIRED` |
| T-52 | 携带用户令牌调用内部接口 | `Authorization: Bearer <JWT>` 调 `POST /internal/tasks/order-timeout-release` | 拒绝（`401`）；**不得**按 `ROLE_SYSTEM` 放行 |
| T-53 | 无 `X-Internal-Token` 调用内部接口 | 不带头部调 4 个 `/internal/**` | 均 `401` |
| T-54 | 外部直连 `/internal/**` | 经网关从公网访问 | 网关层拒绝（GW-01/GW-02）；**不得**到达业务服务 |
| T-55 | 携带过期令牌调用公开接口 | `registerUser` / `login` / `listItems` 等 **10** 个公开接口 + 过期令牌 | **必须成功**（公开端点不得因令牌失效而 401） |
| T-56 | 管理员调用公开接口 | `ROLE_ADMIN` 调 `getPublicProfile` / `listItems` | 成功（公开 + 治理只读不冲突） |

### 8.6 越权测试的通用断言（每条用例都要覆盖）

| 断言 | 内容 |
| --- | --- |
| A1 | HTTP 状态码与业务 `code` 同时匹配预期 |
| A2 | 拒绝场景**不得**返回 `404`（避免存在性泄露）；资源确实不存在时才返回 404，且**仅对有权访问者** |
| A3 | 拒绝场景**零写副作用**：再查一次数据库/详情，状态、版本号、占用计数、通知计数均不变 |
| A4 | 审计：治理类拒绝**不得**写入成功审计；成功治理写操作**必须**写审计 |
| A5 | 幂等：所有 `Idempotency-Key` **必须**的接口，重复提交返回首次结果（E-06），且**不产生第二次占用/扣减** |
| A6 | 并发：`acceptSwapProposal`、`adminLiftPenalty` vs `runPenaltyAutoUnbanTask`、订单占用等并发场景，只允许一方成功（R-18/R-31） |

---

## 9. 权限与 OpenAPI 的对应关系

> **唯一真源原则**：[`openapi.yaml`](./openapi.yaml) 是接口权限的**唯一真源**（single source of truth）。本文档是它的**可读化映射**；实现（后端注解/中间件、前端按钮可见性、测试用例）必须以 `openapi.yaml` 为准。任何不一致都视为**缺陷**，需按 §10 走变更流程。

### 9.1 `x-required-roles` 取值表

| 取值 | 含义 | 允许的主体 | 与本文记号的关系 |
| --- | --- | --- | --- |
| `GUEST` | 访客（公开） | 未登录用户；**必须**配合 `security: []` | ✅ 公开 |
| `USER` | 登录的普通用户 | `ROLE_USER`（含未受罚的买卖双方）；**不含** `ROLE_ADMIN`（P-00-A 拦截） | 🔒 / 🔒👤 / 🔒⛔ |
| `ADMIN` | 仅管理员 | `ROLE_ADMIN` | 🔒🛡️ |
| `RESTRICTED` | 被处罚用户亦可用 | `ROLE_RESTRICTED`（该端点在白名单内） | ✅（白名单项） |
| `SYSTEM` | 系统内部 | `ROLE_SYSTEM`（`X-Internal-Token`） | ⏱️ |

> 取值可并列（如 `[USER, ADMIN, RESTRICTED]`、`[GUEST, USER, ADMIN, RESTRICTED]`、`[USER, RESTRICTED]`、`[USER, ADMIN]`），表示**任一命中即可**。
>
> **易错点**：`x-required-roles: [USER]` 表示**管理员被排除**（P-00-A），而不是「USER 及更高权限」。管理员**不继承** USER 能力。

### 9.2 其它相关扩展与约定

| 扩展 / 字段 | 语义 | 权限含义 |
| --- | --- | --- |
| `security: []` | 该操作显式声明**无需认证** | **公开**（对应 `GUEST`）；共 11 处 |
| `security: [{bearerAuth: []}]`（全局默认） | 需 JWT | 需登录 |
| `security: [{internalToken: []}]` | 需 `X-Internal-Token` | 系统内部；**不使用** `bearerAuth` |
| `x-internal: true` | 标记**内部接口** | 仅 `/internal/**`，共 4 个；对外必须屏蔽（GW-01~GW-04） |
| `x-internal-security-note` | 文档级内部接口安全说明 | 网关禁止外部访问该前缀 |
| `x-forbidden-for-restricted: true` | 该操作**被处罚用户禁止** | 共 24 个；实现须在归属校验**之前**拦截 |
| `x-forbidden-for-restricted: false` | 该操作**被处罚用户允许** | 共 3 个：`takeItemOffShelf`、`markOrderDelivered`、`confirmOrderReceipt`（后两者＝假设 `A-14`） |
| `x-idempotency` / `x-idempotency-semantics` | 幂等要求（`IDEM-A`/`IDEM-B`） | 非权限字段，但**安全测试必测**（防重复占用） |
| `x-feature-flag` | 特性开关 | `refreshToken` 的 `refresh-token-enabled` → `501` |

### 9.3 映射一致性检查表（冻结评审必须逐项通过）

| # | 检查 | 通过标准 |
| --- | --- | --- |
| C-01 | 操作数一致 | `openapi.yaml` 中 `operationId` 出现 **83** 次；`x-required-roles` 出现 **83** 次（一一对应，无遗漏） |
| C-02 | 公开数一致 | `security: []` 去重后覆盖 **10** 个公开操作（`registerUser`、`login`、`refreshToken`、`getPublicProfile`、`listTradeZones`、`listPickupPointsByZone`、`listCategories`、`listItemConditions`、`listItems`、`getItemDetail`）；原始出现 11 次含 `/auth/login` 的重复声明，须消除 |
| C-03 | 管理员数一致 | `x-required-roles: [ADMIN]` 为 **14**；若总览写「15」，须按 §5.1 明确采用口径 A（14）或 B（14 + `getSwapBundle` = 15）**【待确认】** |
| C-04 | 系统数一致 | `[SYSTEM]` = **4**，且均有 `x-internal: true` |
| C-05 | 处罚例外数一致 | `x-forbidden-for-restricted: false` 恰为 **3**：`takeItemOffShelf`、`markOrderDelivered`、`confirmOrderReceipt` |
| C-06 | 处罚拦截数一致 | `x-forbidden-for-restricted: true` = **24**，与本文 §7.2 逐条对齐 |
| C-07 | 路径拼写 | `runPenaltyAutoUnbanTask` 路径为 `/internal/tasks/penalty-auto-unban`（**不是** `penalty-unban`） |
| C-08 | 本人语义标注 | §4 清单中每个 `👤` 行均在 `openapi.yaml` 的 description 中有「仅本人 / 仅目标方 / 仅发起方 / 参与方」明文说明 |
| C-09 | 403 不降级为 404 | 所有 `👤` 端点的错误响应中**必须**包含 403（`AUTH_NOT_RESOURCE_OWNER`），**不得**仅声明 404 |
| C-10 | 哨兵一致性 | 同一 operationId 在本文档、`api-contract.md`、`error-codes.md` 中引用的错误码名称完全一致 |

---

## 10. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 接口契约负责人（待填写姓名） | 初稿：7 主体 × 6 域逐接口权限映射（83 操作）；定义权限记号与三条硬性边界（P-00-A/B/C）；输出必须登录 / 必须本人 / 仅管理员 / 系统内部四份清单；登记被处罚用户白名单与假设 `A-13`、`A-14`；给出 56 条越权测试用例与 OpenAPI 一致性检查表 | **冻结候选（FROZEN-CANDIDATE）** |

### 10.1 本文档遗留的【待确认】项（冻结前必须闭环）

| 编号 | 待确认内容 | 影响章节 | 关联问题 | 建议动作 |
| --- | --- | --- | --- | --- |
| UC-01 | 被处罚用户是否禁止修改昵称/头像 | §2.1、§7.2 D-04、§8.4 T-41 | `Q-12` / 假设 `A-13` | 若从宽，需改 `openapi.yaml` 的 `x-forbidden-for-restricted` 与本文 §2.1/§7 |
| UC-02 | 处罚前已存在订单/交换的收尾操作是否允许 | §2.3、§2.4、§7.3、§8.4 T-47/T-48 | `Q-13` / 假设 `A-14` | **P1，阻塞冻结**；需裁决 `verifySwapCode`/`confirmSwap`/`getSwapCode` 三个交换域端点 |
| UC-03 | 被处罚用户是否允许下架自己的商品 | §2.2 `takeItemOffShelf`、§7.1 R-09 | `Q-11` | 契约当前为**允许**（`false`），需追认 |
| UC-04 | 「仅管理员操作」计数 14 vs 15 的口径选择 | §5.1、§9.3 C-03 | 文档一致性 | 按 §5.1 口径 A 或 B 统一，并同步 `api-contract.md` 总览 |
| UC-05 | 公开操作计数 10 vs 11 的口径（`/auth/login` 重复 `security: []`） | §3.8、§9.3 C-02 | 文档一致性 | 去重后统一为 **10**，并修正 `api-contract.md` 总览 |
| UC-06 | 已付款订单是否允许买家自助取消（影响 `cancelOrder` 权限边界） | §2.3 `cancelOrder` | `Q-08` | P1；若允许，需补状态机与权限说明 |
| UC-07 | 管理员账号来源（影响 `/admin/**` 鉴权初始化） | §2.5、§5 | `Q-04` | **P0**；决定管理员角色如何被授予 |
| UC-08 | 重复封禁已在处罚中的用户（拒绝/延长/覆盖） | §2.5 `adminBanUser`、§8.3 | `Q-10` | P1；影响 `409 GOVERNANCE_ACTIVE_PENALTY_EXISTS` 语义 |
| UC-09 | 申诉时效（生效期内可申诉 / 解封后不可） | §7.1 R-06 | `Q-16`、`Q-17` | 影响 `createAppeal` 的前置校验 |
| UC-10 | 交易区域 / 取货点 / 校区占位数据 | §2.2 字典三接口 | `Q-01`~`Q-03` | P0；决定公开字典接口的返回内容 |
| UC-11 | 业务时区与时间比较口径（用于「处罚前已存在」判定） | §7.3 | `A-20` | 统一为服务端 UTC 比较，展示用 `Asia/Shanghai` |
