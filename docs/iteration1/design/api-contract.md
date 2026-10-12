# 迭代一 接口清单（API Contract）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 接口清单（API Contract） |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 接口契约负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 规范文件 | [openapi.yaml](./openapi.yaml)（OpenAPI 3.0.3，已通过 openapi-spec-validator 校验） |
| 基础路径 | `/api/v1` |
| 依据文档 | `../requirements.md`（54 FR）、`../permission-matrix.md`、`../state-machines.md`、`../rules.md`、`../out-of-scope.md`、`design/schema.md` |
| 冻结约束 | **2026-10-09 之后不允许新增接口**；变更流程见 [api-freeze.md](./api-freeze.md) §4 |

---

## 1. 总览

| 指标 | 数值 |
| --- | --- |
| 模块（tag）数 | 13 |
| **操作（endpoint）总数** | **83** |
| 公开（无需登录，`security: []`）操作 | 10 |
| 需登录操作（含管理员） | 73 |
| 仅管理员操作（`x-required-roles: [ADMIN]`） | 14 |
| 系统内部操作 | 4 |
| **必填 `Idempotency-Key` 的操作** | **16** |
| 可选 `Idempotency-Key` 的操作 | 16 |
| 组件 Schema | 99 |
| 复用参数 | 7 |
| 复用响应 | 9 |
| **错误码总数** | **93**（其中 72 个已在响应示例中引用；见 [error-codes.md](./error-codes.md) §2.1） |
| **FR 覆盖率** | **54 / 54 = 100%**（FR-101~FR-108、FR-201~FR-211、FR-301~FR-312、FR-401~FR-412、FR-501~FR-507、FR-601~FR-604） |

### 1.1 模块分布

| 模块 | 操作数 | 说明 |
| --- | --- | --- |
| 01-认证与账户 | 6 | 注册、登录、刷新（预留）、退出、当前用户、改密 |
| 02-个人资料 | 5 | 昵称/头像、常用交易区域、通知设置、公开主页 |
| 03-字典与区域 | 4 | 交易区域、取货点、分类、成色 |
| 04-商品 | 10 | 列表/详情/发布/编辑/上下架/交换意愿/占用查看/图片上传/举报 |
| 05-购物车 | 4 | 查看（按卖家分组）、加购、更新、移除 |
| 06-订单与付款 | 8 | 提交、列表、详情、模拟付款、取消、选点、交付、确认收货 |
| 07-评价 | 3 | 订单互评、评价查询、我收到的评价 |
| 08-交换包 | 5 | 创建、列表、详情、编辑、删除 |
| 09-交换提议 | 11 | 发起、列表、详情、接受、拒绝、反提案、取消、交换码、核销、确认、交换互评 |
| 10-举报 | 3 | 举报商品、我的举报列表、举报详情 |
| 11-管理员治理 | 17 | 用户检索/详情/封禁/处罚/解封/下架/恢复/申诉/举报/审计/字典；我的处罚与申诉 |
| 12-通知 | 3 | 列表、未读数、标记已读 |
| 13-系统定时任务 | 4 | 超时释放、交换过期、自动解封、评价窗口关闭（内部接口） |

---

## 2. 接口清单（逐条）

> 列含义：
> - **权限**：来自 `../permission-matrix.md` 的 7 类主体记号（公开 / 登录 / 本人 / 管理员 / 系统）
> - **幂等**：`必须` = 必填 `Idempotency-Key`；`可选` = 建议携带；`—` = 无写副作用或天然幂等
> - **状态转换**：引 `../state-machines.md` 的转换编号
> - **错误码**：主要错误码，完整定义见 [error-codes.md](./error-codes.md)

> **编号说明（重要）**：本节表格的 `#` 列为**本文档内部序号**（用于阅读与计数），
> 表格共覆盖 79 个操作；另有 4 个由任务书要求引入、需求基线无独立 FR 的操作
> （`refreshToken`、`getMyNotificationSettings`、`updateMyNotificationSettings`）
> 与图片相关能力在 §3.1 说明，合计 **83** 个操作。
> **跨文档引用一律使用 `operationId`**（唯一且与 `openapi.yaml` 一致），
> 不要使用序号跨文档比对（各文档序号独立编号）。

### 2.1 认证与账户（FR-101 ~ FR-104）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | FR-101 | POST | `/auth/register` | `registerUser` | 公开 | `RegisterRequest` | `CurrentUserResponse` | — | — | `AUTH_USERNAME_TAKEN`、`VALIDATION_INVALID_PARAMETER`、`COMMON_RATE_LIMITED` |
| 2 | FR-102 | POST | `/auth/login` | `login` | 公开 | `LoginRequest` | `AuthTokenResponse` | — | — | `AUTH_INVALID_CREDENTIALS`、`COMMON_RATE_LIMITED` |
| 3 | FR-103 | POST | `/auth/refresh` | `refreshToken` | 公开 | `RefreshTokenRequest` | `AuthTokenResponse` | — | — | `COMMON_NOT_IMPLEMENTED`（Q-07 未启用）、`AUTH_UNAUTHENTICATED` |
| 4 | FR-104 | POST | `/auth/logout` | `logout` | 登录 | — | `ApiResponse` | 天然幂等 | — | `AUTH_UNAUTHENTICATED` |
| 5 | FR-103 | GET | `/auth/me` | `getCurrentUser` | 登录 | — | `CurrentUserResponse` | — | — | `AUTH_UNAUTHENTICATED`、`AUTH_TOKEN_EXPIRED` |
| 6 | 2.10 | PUT | `/auth/password` | `changePassword` | 本人 | `ChangePasswordRequest` | `ApiResponse` | — | — | `AUTH_INVALID_CREDENTIALS`、`VALIDATION_INVALID_PARAMETER` |

> **FR-103 说明**：登录态保持通过 `bearerAuth` + `GET /auth/me` 实现；7 天有效期（假设 A-06）。
> **FR-104 说明**：退出为客户端丢弃令牌；服务端撤销能力取决于 Q-07（当前假设无状态 JWT）。

### 2.2 个人资料（FR-105 ~ FR-108）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 7 | FR-105, FR-106 | PUT | `/users/me/profile` | `updateMyProfile` | 本人（处罚中禁止） | `UpdateProfileRequest` | `CurrentUserResponse` | — | — | `USER_RESTRICTED_CANNOT_WRITE`、`VALIDATION_INVALID_PARAMETER` |
| 8 | FR-107 | PUT | `/users/me/trade-zones` | `setMyTradeZones` | 本人（处罚中允许） | `UserTradeZonesRequest` | `CurrentUserResponse` | — | — | `VALIDATION_INVALID_PARAMETER`、`TRADE_ZONE_NOT_FOUND` |
| 9 | AQ-01 | GET | `/users/me/notification-settings` | `getMyNotificationSettings` | 登录 | — | `NotificationSettings` | — | — | `AUTH_UNAUTHENTICATED` |
| 10 | AQ-01 | PUT | `/users/me/notification-settings` | `updateMyNotificationSettings` | 登录 | `NotificationSettings` | `NotificationSettings` | — | — | `VALIDATION_INVALID_PARAMETER` |
| 11 | FR-108 | GET | `/users/{userId}/public-profile` | `getPublicProfile` | 公开 | — | `UserProfile` | — | — | `USER_NOT_FOUND` |

> **无联系方式/住址/定位字段**（GEO-02 / OOS-05）。
> **AQ-01**：通知设置由任务书要求引入，需求基线无独立 FR，已登记待确认。

### 2.3 字典与区域（R-15 / R-06 / FR-206 / FR-207）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 12 | R-15 / FR-209 | GET | `/trade-zones` | `listTradeZones` | 公开 | — | `TradeZone[]` | — | — |
| 13 | R-06 / FR-309 | GET | `/trade-zones/{tradeZoneId}/pickup-points` | `listPickupPointsByZone` | 公开 | — | `PickupPoint[]` | — | `TRADE_ZONE_NOT_FOUND` |
| 14 | FR-206 | GET | `/categories` | `listCategories` | 公开 | — | `Category[]` | — | — |
| 15 | FR-207 | GET | `/item-conditions` | `listItemConditions` | 公开 | — | `ItemCondition[]` | — | — |

> **【待确认 Q-01/Q-02/Q-03】**：交易区域与取货点当前为**占位数据**（`CAMPUS_A`、`COMMUNITY_A1`、`PICKUP_LIBRARY_A` 等），名称含「【待确认】」。
> 替换真实数据**不改变路径与 DTO 结构**（见 §6 可替换性保证）。

### 2.4 商品（FR-201 ~ FR-211）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 16 | FR-201（浏览） | GET | `/items` | `listItems` | 公开 | — | `PageResult<ItemSummary>` | — | 仅返回 `IN_SALE` | `COMMON_INVALID_SORT_FIELD` |
| 17 | FR-201 | POST | `/items` | `createItem` | 本人（处罚中禁止） | `CreateItemRequest` | `ItemDetail` | **必须** | （无）→ `IN_SALE`（I-01） | `USER_RESTRICTED_CANNOT_WRITE`、`TRADE_ZONE_NOT_SELECTED`、`VALIDATION_INVALID_PARAMETER` |
| 18 | FR-202 | GET | `/items/mine` | `listMyItems` | 本人 | — | `PageResult<ItemSummary>` | — | — | `AUTH_UNAUTHENTICATED` |
| 19 | FR-210 | GET | `/items/{itemId}` | `getItemDetail` | 公开 | — | `ItemDetail` | — | — | `ITEM_NOT_FOUND` |
| 20 | FR-202 | PUT | `/items/{itemId}` | `updateItem` | 本人（处罚中禁止） | `UpdateItemRequest` | `ItemDetail` | 可选 | `IN_SALE`→`IN_SALE` / `OFF_SHELF`→`OFF_SHELF`（I-03/I-04） | `AUTH_NOT_RESOURCE_OWNER`、`ITEM_LOCKED_BY_OCCUPATION`、`ITEM_STATUS_NOT_EDITABLE`、`COMMON_VERSION_CONFLICT` |
| 21 | FR-203 | POST | `/items/{itemId}/off-shelf` | `takeItemOffShelf` | 本人 | — | `ItemDetail` | 可选 | `IN_SALE`→`OFF_SHELF`（I-05） | `ITEM_LOCKED_BY_OCCUPATION` |
| 22 | FR-204 | POST | `/items/{itemId}/relist` | `relistItem` | 本人（处罚中禁止） | — | `ItemDetail` | 可选 | `OFF_SHELF`→`IN_SALE`（I-06） | `ITEM_LOCKED_BY_OCCUPATION`、`ITEM_BANNED_CANNOT_RELIST` |
| 23 | FR-401 | PUT | `/items/{itemId}/swap-preference` | `updateItemSwapPreference` | 本人（处罚中禁止） | `UpdateSwapPreferenceRequest` | `ItemDetail` | 可选 | 仅改 `swap_preference`（R-20） | `ITEM_STATUS_NOT_SWAPPABLE` |
| 24 | 3.11 | GET | `/items/{itemId}/occupation` | `getItemOccupation` | 本人/管理员 | — | 内联对象 | — | — | `AUTH_NOT_RESOURCE_OWNER`、`ITEM_NOT_FOUND` |
| 25 | FR-205 | POST | `/files/images` | `uploadImage` | 登录 | `multipart/form-data` | `ItemImageUploadResponse` | — | — | `FILE_TOO_LARGE`、`FILE_TYPE_NOT_ALLOWED` |
| 26 | FR-211 | POST | `/items/{itemId}/reports` | `reportItem` | 登录（处罚中允许） | `CreateReportRequest` | `Report` | 可选 | — | `REPORT_ALREADY_EXISTS`（幂等 200）、`REPORT_OWN_ITEM_NOT_ALLOWED` |

> **FR-206/207** 由字典接口 14/15 提供；**FR-208**（估价）为 DTO 字段 `estimatedPrice`（DecimalString）+ 免责声明 `priceDisclaimer`；
> **FR-209**（交易区域）为 `createItem.tradeZoneId`。
> **搜索**（FR-03/页面）由 16 的 `keyword` 查询参数承载；**不支持距离筛选**（GEO-04 / OOS-06）。

### 2.5 购物车（FR-301 ~ FR-302）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 27 | FR-302 | GET | `/cart` | `getCart` | 本人 | — | `CartView` | — | `AUTH_UNAUTHENTICATED` |
| 28 | FR-301 | POST | `/cart/items` | `addCartItem` | 本人（处罚中禁止） | `AddCartItemRequest` | `CartItem` | 可选 | `CART_OWN_ITEM_NOT_ALLOWED`、`ITEM_ALREADY_RESERVED`、`ITEM_NOT_PURCHASABLE` |
| 29 | 4.3 | PATCH | `/cart/items/{cartItemId}` | `updateCartItem` | 本人（处罚中禁止） | `UpdateCartItemRequest` | `CartItem` | — | `AUTH_NOT_RESOURCE_OWNER`、`CART_ITEM_NOT_FOUND` |
| 30 | 4.3 | DELETE | `/cart/items/{cartItemId}` | `removeCartItem` | 本人（处罚中禁止） | — | 204 | 天然幂等 | `AUTH_NOT_RESOURCE_OWNER` |

> **FR-301**：同一商品不重复加入（幂等返回既有条目，HTTP 200）；不可加自己发布的商品。
> **FR-302**：`CartView.groups` 按卖家分组，即提交订单时的拆单依据（R-05 / C-02）。

### 2.6 订单与付款（FR-303 ~ FR-311）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 31 | FR-303, FR-304 | POST | `/orders` | `submitOrder` | 本人（处罚中禁止） | `SubmitOrderRequest` | `SubmitOrderResponse` | **必须** | （无）→`PENDING_PAYMENT`（O-01）；商品 `IN_SALE`→`RESERVED`（I-07） | `ITEM_ALREADY_RESERVED`、`ITEM_RESERVED_BY_SWAP`、`TRADE_ZONE_MISMATCH`、`TRADE_ZONE_NOT_SELECTED`、`COMMON_IDEMPOTENCY_KEY_REUSED` |
| 32 | 4.5 | GET | `/orders` | `listOrders` | 本人 | — | `PageResult<OrderSummary>` | — | — | `AUTH_UNAUTHENTICATED` |
| 33 | FR-305 | GET | `/orders/{orderId}` | `getOrderDetail` | 参与方/管理员 | — | `OrderDetail` | — | — | `AUTH_NOT_RESOURCE_OWNER`、`ORDER_NOT_FOUND` |
| 34 | FR-306 | POST | `/orders/{orderId}/mock-pay` | `mockPayOrder` | 买家本人（处罚中禁止） | `MockPayRequest` | `PaymentResult` | **必须** | `PENDING_PAYMENT`→`PAID`（O-02） | `ORDER_TIMEOUT_RELEASED`、`ORDER_STATUS_NOT_PAYABLE`、`ORDER_ALREADY_CANCELLED` |
| 35 | FR-308 | POST | `/orders/{orderId}/cancel` | `cancelOrder` | 买家本人（处罚中禁止） | `CancelOrderRequest` | `OrderDetail` | 可选 | `PENDING_PAYMENT`→`CANCELLED`（O-03）；商品→`IN_SALE`（I-08） | `ORDER_CANNOT_CANCEL_AFTER_PAID`、`ORDER_ALREADY_CANCELLED` |
| 36 | FR-309 | PUT | `/orders/{orderId}/pickup-point` | `selectPickupPoint` | 买家本人（处罚中禁止） | `SelectPickupPointRequest` | `OrderDetail` | 可选 | `PAID`→`PAID`（O-05） | `PICKUP_POINT_ZONE_MISMATCH`、`ORDER_NOT_PAID_FOR_PICKUP_SELECTION`、`ORDER_ALREADY_DELIVERED` |
| 37 | FR-310 | POST | `/orders/{orderId}/deliver` | `markOrderDelivered` | 卖家本人 | — | `OrderDetail` | 可选 | `PAID`→`DELIVERED`（O-06） | `ORDER_PICKUP_POINT_NOT_SELECTED`、`ORDER_STATUS_NOT_DELIVERABLE` |
| 38 | FR-311 | POST | `/orders/{orderId}/confirm-receipt` | `confirmOrderReceipt` | 买家本人 | — | `OrderDetail` | 可选 | `DELIVERED`→`COMPLETED`（O-07）；商品→`SOLD`（I-10） | `ORDER_STATUS_NOT_DELIVERED` |

> **FR-305**：倒计时由 `OrderDetail.expiresAt` + `remainingSeconds` 提供，**服务端计算**（R-19），前端禁止自行推算。
> **FR-307**（超时取消）与 **FR-601** 由内部接口 80 实现，见 §2.13。
> **FR-306**：`mockNotice` 固定为「模拟付款，未产生真实扣款」（OOS-01）。

### 2.7 评价（FR-312）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 39 | FR-312 | POST | `/orders/{orderId}/reviews` | `createOrderReview` | 交易参与方（处罚中禁止） | `CreateReviewRequest` | `Review` | **必须** | `OPEN`→`BUYER_REVIEWED`/`SELLER_REVIEWED`→`BOTH_REVIEWED` | `REVIEW_TRANSACTION_NOT_COMPLETED`、`REVIEW_WINDOW_CLOSED`、`REVIEW_ALREADY_SUBMITTED` |
| 40 | 4.14 | GET | `/orders/{orderId}/reviews` | `listOrderReviews` | 交易参与方 | — | `ReviewStatusResponse` | — | — | `AUTH_NOT_RESOURCE_OWNER` |
| 41 | 4.14 | GET | `/users/me/reviews` | `listMyReceivedReviews` | 本人 | — | `PageResult<Review>` | — | — | `AUTH_UNAUTHENTICATED` |
| 71 | FR-312 | POST | `/swap-proposals/{proposalId}/reviews` | `createSwapReview` | 交换双方（处罚中禁止） | `CreateReviewRequest` | `Review` | **必须** | 同评价状态机 | `REVIEW_TRANSACTION_NOT_COMPLETED`、`REVIEW_ALREADY_SUBMITTED` |

> **明确不做**：评价标签、信用徽章、评价参与排序（OOS-09）。
> 对方评价仅在「双方均已评价」或「窗口关闭」后可见（避免互相偷看）。

### 2.8 交换包（FR-402 ~ FR-403）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 42 | 5.4 | GET | `/swap-bundles` | `listMySwapBundles` | 本人（处罚中只读） | — | `PageResult<SwapBundleSummary>` | — | — | `AUTH_UNAUTHENTICATED` |
| 43 | FR-402 | POST | `/swap-bundles` | `createSwapBundle` | 本人（处罚中禁止） | `CreateSwapBundleRequest` | `SwapBundleDetail` | **必须** | （无）→`DRAFT`/`PACKED`（B-01） | `SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`（E-07）、`SWAP_BUNDLE_ITEM_NOT_OWNED`、`SWAP_ITEM_IN_ACTIVE_BUNDLE`、`ITEM_ALREADY_RESERVED` |
| 44 | 5.4 | GET | `/swap-bundles/{bundleId}` | `getSwapBundle` | 包主/管理员 | — | `SwapBundleDetail` | — | — | `AUTH_NOT_RESOURCE_OWNER`、`SWAP_BUNDLE_NOT_FOUND` |
| 45 | 5.3 | PUT | `/swap-bundles/{bundleId}` | `updateSwapBundle` | 包主（处罚中禁止） | `UpdateSwapBundleRequest` | `SwapBundleDetail` | 可选 | `PACKED`/`RELEASED`→`DRAFT`（B-08） | `SWAP_BUNDLE_LOCKED`（SB-04）、`SWAP_BUNDLE_STATUS_NOT_EDITABLE`、`COMMON_VERSION_CONFLICT` |
| 46 | 5.5 | DELETE | `/swap-bundles/{bundleId}` | `deleteSwapBundle` | 包主（处罚中禁止） | — | 204 | 天然幂等 | `PACKED`→软删除（B-11） | `SWAP_BUNDLE_LOCKED` |

> **FR-403**：`SwapBundleDetail.estimatedTotal` 为估价合计，**仅展示**；响应含 `priceDisclaimer` 明示**不做等价判定、不支持现金补差价**（OOS-03）。
> **件数 1–5** 在 `CreateSwapBundleRequest.itemIds` 用 `minItems/maxItems` 声明，服务端仍二次校验并返回 422（E-07）。

### 2.9 交换提议（FR-404 ~ FR-412）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 47 | 5.7 | GET | `/swap-proposals` | `listSwapProposals` | 参与方（处罚中只读） | — | `PageResult<SwapProposalSummary>` | — | — | `AUTH_UNAUTHENTICATED` |
| 48 | FR-404, FR-411 | POST | `/swap-proposals` | `createSwapProposal` | 本人（处罚中禁止） | `CreateSwapProposalRequest` | `SwapProposalDetail` | **必须** | （无）→`PENDING`（S-01）；双方商品 `IN_SALE`→`SWAPPING`（I-11） | `SWAP_ITEM_OCCUPIED`（E-10）、`ITEM_ALREADY_RESERVED`、`SWAP_TARGET_NOT_ACCEPTING`（E-08）、`TRADE_ZONE_MISMATCH`（E-17）、`SWAP_SELF_PROPOSAL_NOT_ALLOWED` |
| 49 | 5.7 | GET | `/swap-proposals/{proposalId}` | `getSwapProposal` | 参与方/管理员 | — | `SwapProposalDetail` | — | — | `AUTH_NOT_RESOURCE_OWNER`、`SWAP_PROPOSAL_NOT_FOUND` |
| 50 | FR-405 | POST | `/swap-proposals/{proposalId}/accept` | `acceptSwapProposal` | **仅目标方**（处罚中禁止） | — | `SwapProposalDetail` | **必须**（IDEM-B） | `PENDING`→`ACCEPTED`（S-02）；生成交换码（C-05） | `SWAP_PROPOSAL_ALREADY_ACCEPTED`、`SWAP_PROPOSAL_ALREADY_FINALIZED`（E-09）、`SWAP_ITEM_OCCUPIED`、`SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED`（OOS-04） |
| 51 | FR-406 | POST | `/swap-proposals/{proposalId}/reject` | `rejectSwapProposal` | **仅目标方** | `RejectSwapProposalRequest` | `SwapProposalDetail` | 可选 | `PENDING`→`REJECTED`（S-03）；商品→`IN_SALE`（I-12） | `SWAP_PROPOSAL_ALREADY_FINALIZED` |
| 52 | FR-407 | POST | `/swap-proposals/{proposalId}/counter` | `counterSwapProposal` | **仅目标方**（处罚中禁止） | `CounterSwapProposalRequest` | `SwapProposalDetail`（新提议） | **必须**（IDEM-B） | `PENDING`→`COUNTERED`（S-04）+ 新建 `PENDING`（`counterDepth=1`） | `SWAP_COUNTER_NOT_ALLOWED`（R-28 仅一轮）、`SWAP_PROPOSAL_ALREADY_FINALIZED` |
| 53 | 5.16 | POST | `/swap-proposals/{proposalId}/cancel` | `cancelSwapProposal` | **仅发起方**（处罚中禁止） | — | `SwapProposalDetail` | 可选 | `PENDING`→`CANCELLED`（S-05） | `AUTH_NOT_RESOURCE_OWNER`、`SWAP_PROPOSAL_ALREADY_FINALIZED` |
| 54 | FR-409 | GET | `/swap-proposals/{proposalId}/swap-code` | `getSwapCode` | 仅双方 | — | `SwapCode` | — | — | `SWAP_CODE_NOT_ISSUED`、`AUTH_NOT_RESOURCE_OWNER` |
| 55 | FR-409 | POST | `/swap-proposals/{proposalId}/swap-code/verify` | `verifySwapCode` | 仅双方（处罚中禁止） | `VerifySwapCodeRequest` | `SwapCode` | 可选（IDEM-B） | 交换码 `ISSUED`→`VERIFIED`（S-08） | `SWAP_CODE_ALREADY_VERIFIED`（E-12）、`SWAP_CODE_INVALID`、`SWAP_CODE_NOT_ISSUED` |
| 56 | FR-410 | POST | `/swap-proposals/{proposalId}/confirm` | `confirmSwap` | 仅双方（处罚中禁止） | `ConfirmSwapRequest` | `SwapProposalDetail` | 可选 | `ACCEPTED`→（1/2 确认）→`COMPLETED`（S-09/S-10）；商品→`SOLD`（I-13） | `SWAP_CODE_NOT_VERIFIED` |

> **FR-405 关键**：**整包接受（all-or-nothing）**，接口**不接受物品子集参数**（OOS-04）。
> 若请求体携带物品列表 → `SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED`。
> **FR-408**（72 小时过期）由内部接口 81 实现。
> **FR-412**（整笔作废）由「商品被下架/被占用」触发的系统联动实现，见 `adminTakedownItem`（R-35）与规则 R-24。

### 2.10 举报（FR-211 查询侧）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 57 | FR-211 | GET | `/users/me/reports` | `listMyReports` | 本人（处罚中允许） | — | `PageResult<Report>` | — | `AUTH_UNAUTHENTICATED` |
| 58 | 3.13 | GET | `/reports/{reportId}` | `getMyReport` | 举报人本人/管理员 | — | `Report` | — | `AUTH_NOT_RESOURCE_OWNER`、`REPORT_NOT_FOUND` |

> 举报**提交**接口为 #26 `reportItem`（`POST /items/{itemId}/reports`）。

### 2.11 管理员治理（FR-501 ~ FR-505）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 状态转换 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 59 | FR-501 | GET | `/admin/users` | `adminSearchUsers` | 仅管理员 | — | `PageResult<AdminUserSummary>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 60 | 6.2 | GET | `/admin/users/{userId}` | `adminGetUserDetail` | 仅管理员 | — | `AdminUserDetail` | — | — | `AUTH_ADMIN_REQUIRED`、`USER_NOT_FOUND` |
| 61 | FR-503 | POST | `/admin/users/{userId}/ban` | `adminBanUser` | 仅管理员 | `BanUserRequest` | `Penalty` | **必须** | （无）→`penalties.ACTIVE`（P-01）；用户→`RESTRICTED` | `VALIDATION_REASON_REQUIRED`（R-34）、`GOVERNANCE_ACTIVE_PENALTY_EXISTS`（E-13）、`GOVERNANCE_CANNOT_PUNISH_ADMIN` |
| 62 | FR-504 | GET | `/admin/users/{userId}/penalties` | `adminListUserPenalties` | 仅管理员 | — | `PageResult<Penalty>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 63 | FR-504 | GET | `/admin/penalties` | `adminListPenalties` | 仅管理员 | — | `PageResult<Penalty>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 64 | 6.8 | POST | `/admin/penalties/{penaltyId}/lift` | `adminLiftPenalty` | 仅管理员 | `LiftPenaltyRequest` | `Penalty` | **必须** | `ACTIVE`/`APPEALING`→`LIFTED`（P-07） | `VALIDATION_REASON_REQUIRED`、`GOVERNANCE_PENALTY_ALREADY_RESOLVED` |
| 65 | FR-502 | POST | `/admin/items/{itemId}/takedown` | `adminTakedownItem` | 仅管理员 | `TakedownItemRequest` | `TakedownItemResponse` | **必须** | →`BANNED`（I-14/I-15）+ **联动作废**（R-35） | `VALIDATION_REASON_REQUIRED`、`ITEM_NOT_FOUND` |
| 66 | 6.4 | POST | `/admin/items/{itemId}/restore` | `adminRestoreItem` | 仅管理员 | `RestoreItemRequest` | `ItemDetail` | **必须** | `BANNED`→`IN_SALE`/`OFF_SHELF`（I-16） | `VALIDATION_REASON_REQUIRED`、`ITEM_LOCKED_BY_OCCUPATION` |
| 67 | FR-505 | GET | `/admin/appeals` | `adminListAppeals` | 仅管理员 | — | `PageResult<Appeal>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 68 | FR-505 | POST | `/admin/appeals/{appealId}/resolve` | `adminResolveAppeal` | 仅管理员 | `ResolveAppealRequest` | `Appeal` | **必须** | `SUBMITTED`→`APPROVED`/`REJECTED`（AP-02/AP-03） | `VALIDATION_REASON_REQUIRED`、`APPEAL_ALREADY_RESOLVED` |
| 69 | 6.9 | GET | `/admin/reports` | `adminListReports` | 仅管理员 | — | `PageResult<Report>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 70 | 6.9 | POST | `/admin/reports/{reportId}/handle` | `adminHandleReport` | 仅管理员 | `HandleReportRequest` | `Report` | **必须** | `PENDING`→`HANDLED`/`DISMISSED` | `VALIDATION_REASON_REQUIRED`、`REPORT_ALREADY_HANDLED` |
| 72 | 6.10 | GET | `/admin/audit-logs` | `adminListAuditLogs` | 仅管理员 | — | `PageResult<AuditLog>` | — | — | `AUTH_ADMIN_REQUIRED` |
| 73 | 6.11 | GET | `/admin/dictionaries/{dictionaryType}` | `adminListDictionary` | 仅管理员 | — | `Object[]` | — | — | `AUTH_ADMIN_REQUIRED` |
| 74 | 2.11 | GET | `/users/me/penalties` | `listMyPenalties` | 本人（**处罚中允许**） | — | `PageResult<Penalty>` | — | — | `AUTH_UNAUTHENTICATED` |
| 75 | 2.12 | POST | `/penalties/{penaltyId}/appeals` | `createAppeal` | 被处罚者本人 | `CreateAppealRequest` | `Appeal` | **必须** | （无）→`SUBMITTED`（AP-01）；`penalties`→`APPEALING`（P-03） | `APPEAL_ALREADY_EXISTS`（AP-01）、`APPEAL_PENALTY_NOT_ACTIVE` |
| 76 | 2.12 | GET | `/appeals/mine` | `listMyAppeals` | 本人（**处罚中允许**） | — | `PageResult<Appeal>` | — | — | `AUTH_UNAUTHENTICATED` |

> **R-32**：处罚与申诉记录**永不物理删除**，仅状态变化。
> **R-34**：所有治理接口的 `reason` / `note` **必填**，缺失返回 `VALIDATION_REASON_REQUIRED`。
> **P-00-A**：**管理员不参与交易**——上述接口均不含下单/发布/交换能力。

### 2.12 通知（FR-507）

| # | FR | 方法 | 路径 | operationId | 权限 | 请求 DTO | 响应 DTO | 幂等 | 主要错误码 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 77 | FR-507 | GET | `/notifications` | `listMyNotifications` | 本人（处罚中允许） | — | `PageResult<Notification>` | — | `AUTH_UNAUTHENTICATED` |
| 78 | FR-507 | GET | `/notifications/unread-count` | `getUnreadNotificationCount` | 本人（处罚中允许） | — | `UnreadCountResponse` | — | `AUTH_UNAUTHENTICATED` |
| 79 | FR-507 | POST | `/notifications/read` | `markNotificationsRead` | 本人（处罚中允许） | `MarkNotificationsReadRequest` | `UnreadCountResponse` | 可选 | `AUTH_UNAUTHENTICATED` |

> **【待确认 Q-05】** 通知渠道是否仅站内；**【待确认 Q-33】** 通知列表页是否纳入迭代一页面基线。
> 接口先行冻结以支撑治理通知（FR-507）。

### 2.13 系统定时任务（FR-601 ~ FR-604）—— **内部接口**

| # | FR | 方法 | 路径 | operationId | 权限 | 响应 DTO | 幂等 | 状态转换 | 建议频率 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 80 | FR-601, FR-307 | POST | `/internal/tasks/order-timeout-release` | `runOrderTimeoutReleaseTask` | **系统** | `ScheduledTaskResult` | IDEM-B | `PENDING_PAYMENT`→`TIMEOUT_CANCELLED`（O-04）；商品→`IN_SALE` | 每分钟 |
| 81 | FR-602, FR-408 | POST | `/internal/tasks/swap-proposal-expire` | `runSwapProposalExpireTask` | **系统** | `ScheduledTaskResult` | IDEM-B | `PENDING`→`EXPIRED`（S-06） | 每分钟 |
| 82 | FR-603, FR-506 | POST | `/internal/tasks/penalty-auto-unban` | `runPenaltyAutoUnbanTask` | **系统** | `ScheduledTaskResult` | IDEM-B | `ACTIVE`/`APPEALING`→`EXPIRED`（P-04） | 每分钟 |
| 83 | FR-604 | POST | `/internal/tasks/review-window-close` | `runReviewWindowCloseTask` | **系统** | `ScheduledTaskResult` | IDEM-B | `review_status`→`WINDOW_CLOSED` | 每小时 |

> **`/internal/**` 仅供系统定时任务调用**：使用 `X-Internal-Token`（`internalToken`）而非用户 JWT；
> 生产环境应在网关层禁止外部访问该前缀。详见 [api-permission-mapping.md](./api-permission-mapping.md) §6。

---

## 3. FR 覆盖矩阵（54/54）

> 逐条核对。凡 FR 无独立 endpoint 的，给出承载位置与理由。

| FR | 需求 | 承载接口 | 覆盖 |
| --- | --- | --- | --- |
| FR-101 | 用户注册 | #1 `registerUser` | ✅ |
| FR-102 | 用户登录 | #2 `login` | ✅ |
| FR-103 | 登录态保持 | `bearerAuth` + #5 `getCurrentUser`（+ #3 `refreshToken` 预留） | ✅ |
| FR-104 | 退出登录 | #4 `logout` | ✅ |
| FR-105 | 修改昵称 | #7 `updateMyProfile` | ✅ |
| FR-106 | 上传头像 | #25 `uploadImage` + #7 `updateMyProfile` | ✅ |
| FR-107 | 勾选常用交易区域 | #8 `setMyTradeZones`（+ #12 字典） | ✅ |
| FR-108 | 查看个人主页 | #11 `getPublicProfile` | ✅ |
| FR-201 | 发布商品 | #17 `createItem` | ✅ |
| FR-202 | 编辑商品 | #20 `updateItem`（+ #18 `listMyItems`） | ✅ |
| FR-203 | 下架商品 | #21 `takeItemOffShelf` | ✅ |
| FR-204 | 重新上架 | #22 `relistItem` | ✅ |
| FR-205 | 图片限制 1–9 张 | #25 `uploadImage` + `CreateItemRequest.imageUrls`（`minItems/maxItems`） | ✅ |
| FR-206 | 分类 | #14 `listCategories` + `categoryId` 字段 | ✅ |
| FR-207 | 成色 | #15 `listItemConditions` + `conditionId` 字段 | ✅ |
| FR-208 | 估价 | `estimatedPrice`（`DecimalString`）+ `priceDisclaimer`（R-13） | ✅ |
| FR-209 | 交易区域 | `tradeZoneId` 字段 + `TRADE_ZONE_NOT_SELECTED` 校验 | ✅ |
| FR-210 | 交换状态展示 | #19 `getItemDetail` 的 `swapDisplayStatus` + #23 `updateItemSwapPreference` | ✅ |
| FR-211 | 举报商品 | #26 `reportItem` | ✅ |
| FR-301 | 加入购物车 | #28 `addCartItem` | ✅ |
| FR-302 | 购物车按卖家分组 | #27 `getCart`（`CartView.groups`） | ✅ |
| FR-303 | 提交订单（一单一卖家、拆单） | #31 `submitOrder`（`SubmitOrderResponse.split`） | ✅ |
| FR-304 | 库存预留 | #31 `submitOrder`（下单即占用，R-16） | ✅ |
| FR-305 | 付款倒计时 | #33 `getOrderDetail`（`expiresAt` + `remainingSeconds`） | ✅ |
| FR-306 | 模拟付款 | #34 `mockPayOrder` | ✅ |
| FR-307 | 超时取消 | #80 `runOrderTimeoutReleaseTask`（内部） | ✅ |
| FR-308 | 主动取消 | #35 `cancelOrder` | ✅ |
| FR-309 | 选择公共取货点 | #36 `selectPickupPoint`（+ #13 取货点字典） | ✅ |
| FR-310 | 卖家交付 | #37 `markOrderDelivered` | ✅ |
| FR-311 | 买家确认收货 | #38 `confirmOrderReceipt` | ✅ |
| FR-312 | 互评前置条件 | #39 `createOrderReview`、#40 `listOrderReviews`、#71 `createSwapReview` | ✅ |
| FR-401 | 设置交换状态 | #23 `updateItemSwapPreference` | ✅ |
| FR-402 | 创建交换包 1–5 件 | #43 `createSwapBundle` | ✅ |
| FR-403 | 交换包估价合计 | #44 `getSwapBundle`（`estimatedTotal`，仅展示 OOS-03） | ✅ |
| FR-404 | 发起交换 | #48 `createSwapProposal` | ✅ |
| FR-405 | 整包接受 | #50 `acceptSwapProposal`（**无 item 级参数**，OOS-04） | ✅ |
| FR-406 | 拒绝 | #51 `rejectSwapProposal` | ✅ |
| FR-407 | 反提案（仅一轮） | #52 `counterSwapProposal` + `counterDepth` 校验 | ✅ |
| FR-408 | 72 小时过期 | #81 `runSwapProposalExpireTask`（内部） | ✅ |
| FR-409 | 交换码 | #54 `getSwapCode` + #55 `verifySwapCode` | ✅ |
| FR-410 | 双方确认 | #56 `confirmSwap`（`proposerConfirmedAt`/`targetConfirmedAt`） | ✅ |
| FR-411 | 同时占用 | #48 `createSwapProposal` 的占用语义 + `ITEM_ALREADY_RESERVED`/`SWAP_ITEM_OCCUPIED` | ✅ |
| FR-412 | 整笔作废 | 系统联动：`adminTakedownItem`（#65，R-35）+ `terminationReason=VOID_*` 暴露于 #49/#50 | ✅ |
| FR-501 | 搜索用户 | #59 `adminSearchUsers` | ✅ |
| FR-502 | 下架违规商品 | #65 `adminTakedownItem` | ✅ |
| FR-503 | 封禁三天 | #61 `adminBanUser` | ✅ |
| FR-504 | 查看处罚记录 | #62 `adminListUserPenalties`、#63 `adminListPenalties`、#74 `listMyPenalties` | ✅ |
| FR-505 | 处理申诉 | #67 `adminListAppeals`、#68 `adminResolveAppeal`、#75 `createAppeal`、#76 `listMyAppeals` | ✅ |
| FR-506 | 自动解封 | #82 `runPenaltyAutoUnbanTask`（内部） | ✅ |
| FR-507 | 通知 | #77 `listMyNotifications`、#78 `getUnreadNotificationCount`、#79 `markNotificationsRead` | ✅ |
| FR-601 | 订单超时取消 | #80 `runOrderTimeoutReleaseTask` | ✅ |
| FR-602 | 交换提议过期 | #81 `runSwapProposalExpireTask` | ✅ |
| FR-603 | 封禁到期自动解封 | #82 `runPenaltyAutoUnbanTask` | ✅ |
| FR-604 | 评价窗口关闭 | #83 `runReviewWindowCloseTask` | ✅ |

**覆盖率：54 / 54 = 100%**，无「无法映射」的 FR。

### 3.1 无独立 FR 编号但被任务书要求的能力

| 能力 | 接口 | 登记 |
| --- | --- | --- |
| 刷新令牌 | #3 `refreshToken`（预留，默认未启用） | **AQ-02**（Q-07） |
| 通知开关 | #9/#10 `*MyNotificationSettings` | **AQ-01**（Q-05） |
| 图片排序/删除/封面规则 | 通过 `imageUrls` 数组**全量替换** + 数组顺序即 `sortOrder`（第 1 张为封面）实现，未单独开接口 | **AQ-05**（见 api-open-questions.md） |
| 信用徽章 / 评价标签 | **不做** | **AQ-06**（OOS-09） |
| 距离筛选 | **不做** | 见 §4 |

---

## 4. 明确不做的接口（与 `../out-of-scope.md` 对齐）

> 以下**不存在**对应 endpoint，属**刻意缺失**，评审时不应视为遗漏。

| 不做项 | OOS | 为什么没有接口 |
| --- | --- | --- |
| 真实支付 / 支付渠道选择 / 支付回调 | OOS-01 | 仅 `mock-pay`；无 `payment-channel`、无回调接口 |
| 退款 / 售后仲裁 | OOS-14 | 无退款接口；已付款不可自助取消（Q-08 假设） |
| 多人交换链 | OOS-02 | `createSwapProposal` 的 `targetItemIds` 必须同属**一个** target；无多方接口 |
| 现金补差价 | OOS-03 | 无任何金额支付/差额接口；仅 `estimatedTotal` 展示 |
| 部分接受交换包 | OOS-04 | `acceptSwapProposal` **不接受**物品列表；携带则返回 422 |
| 定位 / 住址 | OOS-05 | 无坐标/地址字段；`tradeZoneIds` 只能来自预设字典 |
| 地图 / 导航 / 距离 / 附近 | OOS-06、GEO-04 | 无地图/坐标/距离参数；`listItems` 仅支持 `tradeZoneId` 区域维度 |
| 跨区交易 | OOS-07 | 服务端强校验同区域 → `TRADE_ZONE_MISMATCH` |
| 站内聊天 / 私信 | OOS-08 | 无消息接口；仅结构化状态 + `notifications` |
| 推荐算法 / 个性化排序 | OOS-09 | `sort` 为**白名单固定字段**；越界 → `COMMON_INVALID_SORT_FIELD` |
| 复杂议价 / 砍价 | OOS-10 | 购买为一口价；交换靠交换包 + 一轮反提案 |
| 第三方登录 / 手机验证码 | OOS-11 | 仅账号密码；无 OAuth / 验证码接口 |
| 多管理员角色 | OOS-12 | 单一 `ROLE_ADMIN`，无角色管理接口 |
| 用户注销 / 数据导出 | OOS-13 | 无注销、无导出接口 |
| 发票 | OOS-15 | 无发票接口 |
| 物流 / 快递 | OOS-16 | 无运单/物流接口；仅公共取货点当面交付 |
| 对象存储 / CDN | OOS-18 | `uploadImage` 返回本地静态路径，无签名 URL / 直传接口 |
| 生产级运维接口 | OOS-17 | 无健康检查聚合/灰度/监控接口（由部署层提供） |

---

## 5. 全局约定摘要

| 项 | 约定 |
| --- | --- |
| 基础路径 | `/api/v1` |
| 认证 | `Authorization: Bearer <JWT>`；令牌 7 天（假设 A-06） |
| 成功响应 | `ApiResponse`：`code`（成功=`OK`）、`message`、`data`、`traceId`、`timestamp` |
| 失败响应 | `ErrorResponse`：`code`（`DOMAIN_REASON`）、`message`、`details`、`traceId`、`timestamp` |
| 分页请求 | `page`（0 起）、`size`（1–100，默认 20）、`sort`（`字段,方向`，白名单） |
| 分页响应 | `PageResult`：`content`、`page`、`size`、`totalElements`、`totalPages`、`first`、`last` |
| 幂等 | `Idempotency-Key` 头；16 个接口**必填**，16 个**可选**；见 [idempotency-and-concurrency.md](./idempotency-and-concurrency.md) |
| 并发 | 需要条件更新的接口在 description 中声明 `x-locking` 与状态前置条件（R-18） |
| 权限声明 | `x-required-roles` 扩展（取值：`GUEST`/`USER`/`ADMIN`/`RESTRICTED`/`SYSTEM`）+ description 双写 |
| 时间 | ISO 8601 UTC，如 `2026-10-12T13:20:41Z`；**服务端时间为准**（R-19） |
| 金额 | `DecimalString`（十进制字符串，2 位小数，单位元）——**不用 number，避免浮点误差** |
| 追踪 | 响应头 `X-Trace-Id` 与响应体 `traceId` 一致 |

### 5.1 错误语义分区（HTTP 使用规范）

| HTTP | 语义 | 何时使用 |
| --- | --- | --- |
| 400 | 语法/格式错误 | 类型错误、长度越界、缺必填、非法枚举、非法排序字段 |
| 401 | 未认证 | 未带令牌、令牌无效/过期、登录凭据错误 |
| 403 | 已认证但无权限 | 非本人（E-02，**不用 404**）、被处罚禁写（E-03）、非管理员、管理员参与交易（P-00-A） |
| 404 | 资源不存在 | **仅**路由目标不存在；**不用于掩盖越权** |
| 409 | 状态/并发冲突 | 占用冲突（E-04/E-10）、终态保护（E-05/E-09）、幂等冲突（E-16）、乐观锁冲突 |
| 422 | 业务规则不满足 | 件数越界（E-07）、跨区（E-17）、不接受交换（E-08）、未完成评价（E-15）、部分接受 |
| 429 | 限流 | 登录/注册等敏感接口 |
| 500 | 服务端错误 | 未预期异常，携带 `traceId` |
| 503 | 依赖不可用/维护 | 数据库或文件存储不可用 |
| 501 | 特性未启用 | `refreshToken`（Q-07 未启用） |

### 5.2 越权返回约定（重要）

| 场景 | 返回 | 理由 |
| --- | --- | --- |
| 访问**他人**资源（编辑他人商品、确认他人订单等） | **403** `AUTH_NOT_RESOURCE_OWNER` | E-02 明确要求；避免用 404 掩盖越权造成语义混淆 |
| 访问**不存在**的资源 ID | **404** | 资源本身不存在 |
| 被处罚用户执行交易写操作 | **403** `USER_RESTRICTED_CANNOT_WRITE` + `restrictedUntil` | E-03 要求返回解封时间 |
| 管理员执行交易操作 | **403** | P-00-A（管理员不参与交易） |
| 非管理员调用治理接口 | **403** `AUTH_ADMIN_REQUIRED` | permission-matrix §6 |

---

## 6. 占位数据可替换性保证

**【待确认 Q-01/Q-02/Q-03】** 的真实数据落地时，遵循以下约束，确保**不破坏已冻结的契约**：

| 保证 | 说明 |
| --- | --- |
| 路径不变 | `/trade-zones`、`/trade-zones/{tradeZoneId}/pickup-points` 路径固定 |
| DTO 结构不变 | `TradeZone` / `PickupPoint` 字段名与类型固定 |
| 仅数据值变化 | `code`（如 `CAMPUS_A` → 真实编码）、`name`（去掉「【待确认】」）、`description` 变化 |
| 标识符语义 | `id` 为自增主键，前端**不得**硬编码具体 ID；`code` 可作为稳定业务标识 |
| 数量可增减 | 最终为 2–3 个校区及周边社区；**区域数量减少不影响契约** |
| 校验逻辑不变 | 「1–5 个常用区域」「同区域交易」「取货点属订单区域」三条校验保持不变（R-15 / R-06） |

---

## 7. P0 / DQ 假设汇总（在契约中的作用）

> 完整登记见 [api-open-questions.md](./api-open-questions.md) 与 `design/open-questions.md`。
> 以下假设**已在契约中生效**，若变更需走冻结变更流程。

| 编号 | 假设 | 契约落点 |
| --- | --- | --- |
| **Q-01/Q-02** | 校区/社区使用占位编码 `CAMPUS_A`/`COMMUNITY_A1` | `TradeZone.code`/`name`，标注【待确认】 |
| **Q-03** | 取货点使用占位编码 `PICKUP_LIBRARY_A` 等 | `PickupPoint.code`/`name`，标注【待确认】 |
| **Q-04** | 管理员由种子数据创建 `admin@example.com` | 不在 API 层暴露；登录走 #2 |
| **DQ-01** | 不使用草稿态，发布即 `IN_SALE` | #17 `createItem` 无状态参数 |
| **DQ-02** | 交易区域可停用（`is_active`） | #12 `listTradeZones.includeInactive`（仅管理员生效） |
| **DQ-03** | 多区域用户一次交易仅在同区域内 | #31/#48 的 `TRADE_ZONE_MISMATCH` |
| **DQ-04** | 取货点无开放时间字段 | `PickupPoint` 仅 `description` |
| **DQ-05** | 封禁固定 3 天、拒绝重复封禁 | #61 `adminBanUser` 无时长参数 + `GOVERNANCE_ACTIVE_PENALTY_EXISTS` |
| **DQ-06** | 审计日志不导出 | #72 仅查询，无导出接口 |
| **A-01** | 付款倒计时 30 分钟 | #33 `expiresAt`/`remainingSeconds` |
| **A-02** | 交换提议 72 小时 | #49 `expiresAt`/`remainingSeconds` |
| **A-03** | 封禁 72 小时 | #61（`durationHours` 只读固定 72） |
| **A-05** | 图片 ≤5MB、1–9 张 | #25 `FILE_TOO_LARGE` |
| **A-06** | 令牌 7 天 | #2 `AuthTokenResponse.expiresIn=604800` |
| **A-07** | 互评窗口 7 天 | #40 `reviewDeadline` |
| **A-10** | 反提案仅一轮 | #52 `SWAP_COUNTER_NOT_ALLOWED` |
| **A-13** | 处罚期间禁止改昵称/头像 | #7 `USER_RESTRICTED_CANNOT_WRITE` |
| **A-14** | 处罚期间允许已有订单收尾 | #37/#38 未标 `x-forbidden-for-restricted` |
| **A-15/A-08** | 幂等键前端生成 UUID，保留 24h | 16 个必填接口 |
| **A-16** | 购物车数量固定为 1 | `CartItem.quantity` 枚举 `[1]` |
| **A-17** | 拆单时某件失效 → 整批失败 | #31 失败语义（Q-24） |
| **A-18** | 付款后才可选取货点 | #36 `ORDER_NOT_PAID_FOR_PICKUP_SELECTION` |

---

## 8. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 接口契约负责人 | 初稿：83 个操作、13 模块、54 FR 100% 覆盖、不做的接口清单、全局约定、占位可替换性保证、P0/DQ 假设落点 | **冻结候选** |
