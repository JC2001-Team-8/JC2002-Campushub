# 迭代路线重塑：垂直切片规划（Markdown 现行版）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代路线重塑：垂直切片规划 |
| 版本 | **v2.0** |
| 日期 | 2026-11-11 |
| 读者 | 迭代二及后续全体组员（4 后端 + 3 前端）、验收人 |
| 状态 | **生效**（本文件为现行版） |
| 唯一真源 | `../docs/iteration1/design/openapi.yaml`（83 个 operationId）、`../docs/iteration1/design/schema.md`（25 张表 / 324 列）、`../src/main/resources/db/migration/V1~V4` |
| 约束文档 | `../docs/iteration1/rules.md`、`../docs/iteration1/state-machines.md`、`../docs/iteration1/design/error-codes.md`、`../docs/iteration1/design/api-freeze.md`、`../docs/iteration1/design/idempotency-and-concurrency.md` |
| 版本关系 | 根目录三份路线附件为历史参考；本文件入仓于 `handover/`，为**唯一现行版** |

> 本文件不修改任何已有项目文件；只做规划、分工与口径统一。
> 凡本文件与 `openapi.yaml` / `schema.md` / `V1~V4` 冲突，一律以契约与迁移脚本为准。

---

## 1. 一页读完

1. **不按层切，按业务域切。** 四个切片 = 账户与共享内核 → 商品与检索 → 购买与交换 → 治理、评价与整合交付。
2. **串行接力。** 同一时刻只有一个切片在写代码；切片闭合（编译通过 + 自测通过 + 合并主干 + 交接清单逐项打勾）后才交棒。
3. **双人责任制。** 每片至少 1 名后端主办 + 1 名后端协办 + 1 名前端主办；协办可跨片复用，但不得一人包干整片。
4. **契约冻结。** 83 个 operationId 不得私自增减；`V1~V4` 只读；任何变更走 `api-freeze.md` §4。
5. **三维边界锁定。** 数据库表归属 + Java 包路径归属 + 前端路由前缀归属，三者同时落在本片范围内才可开工。

---

## 2. 背景与约束

### 2.1 迭代一完成状态（摘要）

| 项 | 值 |
| --- | --- |
| Java 源文件 | 158 |
| 契约 Controller | 12（Auth、User、Dictionary、Item、Cart、Order、Swap、Internal、Admin、Report、Review、Notification） |
| 手工 Controller | 2（Health、FrozenSpec，已 `@Hidden`） |
| operationId | **83** |
| DTO | 33 Request + 66 Response = 99 |
| Service 接口 / 实现 | 12 / 12，全部抛 `UnsupportedOperationException` |
| entity / repository | 仅 `package-info.java`，0 实体、0 仓储 |
| 业务表 / 列 | 25 张 / 324 列 |
| `info.version` | `1.0.0-frozen-candidate` |

**业务逻辑未实现、Service 全空、entity/repository 为空，均属迭代一预期。**

### 2.2 三条硬约束

| 编号 | 约束 | 具体含义 | 违反的后果 |
| --- | --- | --- | --- |
| C-1 | **契约冻结** | `openapi.yaml` 的 83 个 operationId、路径、方法、DTO、枚举、错误码、权限声明一律不得自行修改；变更必须走 `api-freeze.md` §4 | 前后端同时返工；契约测试出现 BLOCKER/MAJOR |
| C-2 | **`V1~V4` 迁移不可改** | 已应用脚本只读；结构变更一律新增 `V5+` | Flyway checksum 校验失败，全组无法启动 |
| C-3 | **GitHub 贡献可见** | 每人用自己的账号提交；提交信息可追溯；禁止代提交、禁止一次性大提交 | 无法证明个人贡献；评审与回溯失效 |

### 2.3 为什么重塑为垂直切片

原始迭代路线按「用户可见的功能阶段」划分，只说了「这一阶段用户能用什么」，没有规定「谁写哪些文件、什么时候交接、怎么防止两个人改同一个文件」。对 7 人、经验不足、重度依赖 AI 的团队，这种描述会退化成水平分层：所有人同时开工，A 写 Entity、B 写 Repository、C 写 Service、D 写 Controller，前端另起一套。

本规划保留原路线的时间骨架与业务主题，**改变阶段内部的组织方式**：按业务域纵向切，片内所有层由同一批人负责，片与片串行接力。

---

## 3. 方法论

### 3.1 水平分层的五条失效路径

| # | 失效路径 | 在本项目中的表现 |
| --- | --- | --- |
| 1 | 契约漂移 | Controller 按自己的理解写字段，前端按契约生成类型，联调时才发现不一致 |
| 2 | AI 上下文碎片化 | 让 AI 写「所有 Repository」需同时理解 25 张表 324 列，超限后开始编字段名 |
| 3 | 端到端反馈链过长 | 「注册能成功吗」要等所有层完成才能验证 |
| 4 | 合并冲突 | 多人同时改 `SecurityConfig`、`ErrorCode`、`ApiResponse`、`GlobalExceptionHandler` |
| 5 | 责任分散 | 出问题时「每层都对」，没人对「这条业务链路能跑通」负责 |

### 3.2 垂直切片的七条机制

| 机制 | 落点 |
| --- | --- |
| 限界上下文 | 四个切片 = 账户内核 / 商品检索 / 购买交换 / 治理评价 |
| 功能纵切 | 切片 2 交付「能发布、能搜到、能看清」 |
| 贯通骨架 | 切片 1 强制交付「注册 → 登录 → 看自己主页」 |
| 全栈功能归属 | 同片的表、后端包、前端路由由同批人掌握 |
| 串行接力 | 闭合后交棒，跨片并行冲突降为 0 |
| 主干开发 | 小步提交，主干每天可编译、可启动 |
| 上下文收敛 | 单片最多 9 张表、30 个 operationId，按子阶段再拆 |

### 3.3 纠正「先建全部地基」

> 「先把所有 Entity、所有 Repository、所有公共组件建好，再一人负责一个模块。」

这是水平分层换了一层皮：它需要同时理解全部 324 列与跨表约束；地基与业务之间没有可验证的中间态；「一人一个模块」若仍是技术层，冲突并未消失；并且会推迟第一个可演示版本。

**正确做法：** 地基本身就是切片 1 的交付物，而且只建切片 1 与后续切片真的会用到的那部分（7 张表 + 幂等基础设施）。不做「理论上将来可能用到」的抽象。

---

## 4. 四个切片

### 4.1 切片 1：账户与共享内核（10/13 ~ 10/20）

| 维度 | 内容 |
| --- | --- |
| 独占表（7） | `users`、`user_trade_zones`、`trade_zones`、`pickup_points`、`categories`、`item_conditions`、`idempotency_records` |
| 只读表 | 无（本片不依赖其他切片数据） |
| Controller / operationId | `AuthController` 6 + `UserController` 5 + `DictionaryController` 4 = **15** |
| 前端路由前缀 | `/register`、`/login`、`/me`、`/me/profile`；全局布局与鉴权守卫（`/` 占位） |
| 交付标准摘要 | 15 个接口真实实现；注册 → 登录 → `/auth/me` 端到端；用户名唯一且大小写不敏感；密码只存摘要；昵称 1~20；未登录 401；他人资源 403（不用 404）；常用区域 1~5；4 个字典只返回启用项并按 `sort_order` 升序；`refreshToken` 返回 501；幂等与乐观锁通路可用；`SecurityConfig` 真正收紧 |
| 交接物 | 15 个 operationId 实现；7 个 Entity + 7 个 Repository；幂等切面 + 乐观锁模板；公共实体冻结清单；前端布局/守卫/请求封装/错误提示；回归自测记录；冒烟脚本；契约测试报告 |

### 4.2 切片 2：商品与检索（10/21 ~ 11/2）

| 维度 | 内容 |
| --- | --- |
| 独占表（3） | `items`、`item_images`、`item_occupations` |
| 只读表 | `users`、`trade_zones`、`user_trade_zones`、`categories`、`item_conditions` |
| 受限写 | `idempotency_records`（例外 E-01，只经切片 1 的幂等切面） |
| Controller / operationId | `ItemController` = **10** |
| 前端路由前缀 | `/`、`/search`、`/items`、`/items/:id`、`/items/new`、`/items/:id/swap-setting`、`/me/items` |
| 交付标准摘要 | 发布必填校验与字段级 `details`；标题 1~60；图片 1~9 张、单张 ≤ 5 MB、JPG/PNG/WebP、首图为封面；`listItems` 只返回 `IN_SALE`；非法排序字段 400；非本人编辑/下架 403；占用态禁改关键字段 409；终态保护；`createItem` 幂等；并发编辑 409；`uploadImage` 需登录；占用查询仅本人/管理员 |
| 交接物 | 10 个 operationId 实现；3 个 Entity + 3 个 Repository；图片存储与限制说明；「占用 / 释放」公开方法说明（供切片 3 调用）；前端 6 个页面；回归自测记录；进度更新 2 材料 |

> 注意：`uploadImage` 的现有 Controller 签名缺 multipart `file`、`purpose`（CD-1），**必须在切片 2 开工前修复**。

### 4.3 切片 3：购买与交换（11/3 ~ 11/20）

> 内部两个子阶段串行：**3A 购买（11/3 ~ 11/6 验收）** → **3B 交换（11/7 ~ 11/20 验收）**。3A 闭合后 3B 才开工。

| 维度 | 内容 |
| --- | --- |
| 独占表（9） | `cart_items`、`orders`、`order_items`、`payments`、`swap_bundles`、`swap_bundle_items`、`swap_proposals`、`swap_proposal_items`、`swap_codes` |
| 受限写 | `items`（`status`、`version`，E-02）、`item_occupations`（新增 + `released_at` / `release_reason`，E-04）、`idempotency_records`（E-01） |
| 只读表 | `users`、`trade_zones`、`pickup_points`、`user_trade_zones` |
| Controller / operationId | `CartController` 4 + `OrderController` 8 + `SwapController` 16 + `InternalController` 2 = **30** |
| 前端路由前缀 | `/cart`、`/checkout`、`/orders`、`/orders/:id`、`/orders/:id/pay-result`、`/orders/:id/pickup-point`、`/me/swap-bundles`、`/items/:id/propose-swap`、`/swaps/:id`、`/swaps/:id/swap-code` |
| 交付标准摘要 | 下单即占用；30 分钟倒计时由服务端给 `expiresAt` + `remainingSeconds`；并发不卖超；付款/超时互斥；取货点同区域；交换包 1~5 件；整包接受；反提案仅一轮；一次性交换码；72 小时过期释放；整笔作废；订单与交换占用互斥；16 个必填幂等接口在本片的 7 个逐一校验 |
| 交接物 | 30 个 operationId 实现；9 个 Entity + 9 个 Repository；占用/释放统一入口说明；并发与整笔作废验证记录；业务事件清单（供切片 4 写通知）；前端 11 个页面；回归自测记录 |

### 4.4 切片 4：治理、评价与整合交付（11/21 ~ 12/10）

| 维度 | 内容 |
| --- | --- |
| 独占表（6） | `reviews`、`reports`、`penalties`、`appeals`、`notifications`、`audit_logs` |
| 受限写 | `items`（`status`、`banned_reason`、`banned_at`、`banned_by`、`version`，E-03）、`item_occupations`（释放，E-04）、`orders`（取消字段，E-05）、`swap_proposals`（作废字段，E-06）、`users`（`status`、`restricted_until`、`version`，E-07）、`idempotency_records`（E-01） |
| 只读表 | `order_items`、`swap_proposal_items`、`trade_zones`、`pickup_points`、`categories`、`item_conditions` |
| Controller / operationId | `AdminController` 17 + `ReportController` 3 + `ReviewController` 3 + `NotificationController` 3 + `InternalController` 2 = **28** |
| 前端路由前缀 | `/items/:id/report`、`/me/reports`、`/me/reviews`、`/me/penalties`、`/me/appeals`、`/notifications`、`/orders/:id/reviews`、`/admin`、`/admin/users`、`/admin/items`、`/admin/penalties`、`/admin/reports`、`/admin/audit-logs` |
| 交付标准摘要 | 非管理员访问治理接口 403；治理动作原因必填；封禁默认 72 小时；重复封禁 409；管理员不可被封禁；处罚中禁写白名单；自动解封幂等；申诉唯一；下架联动（取消订单 / 整笔作废 / 释放占用 / 通知）IDEM-B；恢复前无占用冲突；处罚/申诉/订单/交换永不物理删除；审计完整；举报去重；互评窗口与可见性；评价窗口关闭不回退终态；通知幂等；一键启动包在干净电脑跑通两条主线；材料齐备 |
| 交接物 | 28 个 operationId 实现；6 个 Entity + 6 个 Repository；通知事件清单与幂等约定；审计写入约定；违规下架联动说明；前端 13 个页面；回归自测记录；一键启动部署包；测试报告；用户手册；技术报告；演示材料；进度更新 3/4 材料 |

---

## 5. 唯一 operationId 分配表（83 个）

> 口径：`openapi.yaml` 83 个 operationId + Controller 文件归属。
> 合计：切片 1 = 15、切片 2 = 10、切片 3 = 30、切片 4 = 28，**15 + 10 + 30 + 28 = 83**。

| # | operationId | Controller | 业务域 | 切片 | 子阶段 | 是否复用前片 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `registerUser` | AuthController | 账户与认证 | 1 | — | 否 |
| 2 | `login` | AuthController | 账户与认证 | 1 | — | 否 |
| 3 | `refreshToken` | AuthController | 账户与认证 | 1 | — | 否 |
| 4 | `logout` | AuthController | 账户与认证 | 1 | — | 否 |
| 5 | `getCurrentUser` | AuthController | 账户与认证 | 1 | — | 否 |
| 6 | `changePassword` | AuthController | 账户与认证 | 1 | — | 否 |
| 7 | `updateMyProfile` | UserController | 个人资料 | 1 | — | 否 |
| 8 | `setMyTradeZones` | UserController | 个人资料 | 1 | — | 否 |
| 9 | `getMyNotificationSettings` | UserController | 个人资料 | 1 | — | 否 |
| 10 | `updateMyNotificationSettings` | UserController | 个人资料 | 1 | — | 否 |
| 11 | `getPublicProfile` | UserController | 个人资料 | 1 | — | 否 |
| 12 | `listTradeZones` | DictionaryController | 字典与区域 | 1 | — | 否 |
| 13 | `listPickupPointsByZone` | DictionaryController | 字典与区域 | 1 | — | 否 |
| 14 | `listCategories` | DictionaryController | 字典与区域 | 1 | — | 否 |
| 15 | `listItemConditions` | DictionaryController | 字典与区域 | 1 | — | 否 |
| 16 | `listItems` | ItemController | 商品与检索 | 2 | — | 否 |
| 17 | `createItem` | ItemController | 商品与检索 | 2 | — | 否 |
| 18 | `listMyItems` | ItemController | 商品与检索 | 2 | — | 否 |
| 19 | `getItemDetail` | ItemController | 商品与检索 | 2 | — | 否 |
| 20 | `updateItem` | ItemController | 商品与检索 | 2 | — | 否 |
| 21 | `takeItemOffShelf` | ItemController | 商品与检索 | 2 | — | 否 |
| 22 | `relistItem` | ItemController | 商品与检索 | 2 | — | 否 |
| 23 | `updateItemSwapPreference` | ItemController | 商品与检索 | 2 | — | 否 |
| 24 | `getItemOccupation` | ItemController | 商品与检索 | 2 | — | 否 |
| 25 | `uploadImage` | ItemController | 商品与检索 | 2 | — | 否 |
| 26 | `getCart` | CartController | 购物车 | 3 | 3A | 否 |
| 27 | `addCartItem` | CartController | 购物车 | 3 | 3A | 否 |
| 28 | `updateCartItem` | CartController | 购物车 | 3 | 3A | 否 |
| 29 | `removeCartItem` | CartController | 购物车 | 3 | 3A | 否 |
| 30 | `submitOrder` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 31 | `listOrders` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 32 | `getOrderDetail` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 33 | `mockPayOrder` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 34 | `cancelOrder` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 35 | `selectPickupPoint` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 36 | `markOrderDelivered` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 37 | `confirmOrderReceipt` | OrderController | 订单与付款 | 3 | 3A | 否 |
| 38 | `runOrderTimeoutReleaseTask` | InternalController | 系统定时任务 | 3 | 3A | 否 |
| 39 | `createSwapBundle` | SwapController | 交换 | 3 | 3B | 否 |
| 40 | `listMySwapBundles` | SwapController | 交换 | 3 | 3B | 否 |
| 41 | `getSwapBundle` | SwapController | 交换 | 3 | 3B | 否 |
| 42 | `updateSwapBundle` | SwapController | 交换 | 3 | 3B | 否 |
| 43 | `deleteSwapBundle` | SwapController | 交换 | 3 | 3B | 否 |
| 44 | `createSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 45 | `listSwapProposals` | SwapController | 交换 | 3 | 3B | 否 |
| 46 | `getSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 47 | `acceptSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 48 | `rejectSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 49 | `counterSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 50 | `cancelSwapProposal` | SwapController | 交换 | 3 | 3B | 否 |
| 51 | `getSwapCode` | SwapController | 交换 | 3 | 3B | 否 |
| 52 | `verifySwapCode` | SwapController | 交换 | 3 | 3B | 否 |
| 53 | `confirmSwap` | SwapController | 交换 | 3 | 3B | 否 |
| 54 | `createSwapReview` | SwapController | 交换 | 3 | 3B | 否 |
| 55 | `runSwapProposalExpireTask` | InternalController | 系统定时任务 | 3 | 3B | 否 |
| 56 | `adminSearchUsers` | AdminController | 管理员治理 | 4 | — | 否 |
| 57 | `adminGetUserDetail` | AdminController | 管理员治理 | 4 | — | 否 |
| 58 | `adminBanUser` | AdminController | 管理员治理 | 4 | — | 否 |
| 59 | `adminListUserPenalties` | AdminController | 管理员治理 | 4 | — | 否 |
| 60 | `adminListPenalties` | AdminController | 管理员治理 | 4 | — | 否 |
| 61 | `adminLiftPenalty` | AdminController | 管理员治理 | 4 | — | 否 |
| 62 | `adminTakedownItem` | AdminController | 管理员治理 | 4 | — | 否 |
| 63 | `adminRestoreItem` | AdminController | 管理员治理 | 4 | — | 否 |
| 64 | `adminListAppeals` | AdminController | 管理员治理 | 4 | — | 否 |
| 65 | `adminResolveAppeal` | AdminController | 管理员治理 | 4 | — | 否 |
| 66 | `adminListReports` | AdminController | 管理员治理 | 4 | — | 否 |
| 67 | `adminHandleReport` | AdminController | 管理员治理 | 4 | — | 否 |
| 68 | `adminListAuditLogs` | AdminController | 管理员治理 | 4 | — | 否 |
| 69 | `adminListDictionary` | AdminController | 管理员治理 | 4 | — | 否 |
| 70 | `listMyPenalties` | AdminController | 管理员治理 | 4 | — | 否 |
| 71 | `createAppeal` | AdminController | 管理员治理 | 4 | — | 否 |
| 72 | `listMyAppeals` | AdminController | 管理员治理 | 4 | — | 否 |
| 73 | `reportItem` | ReportController | 举报 | 4 | — | 否 |
| 74 | `listMyReports` | ReportController | 举报 | 4 | — | 否 |
| 75 | `getMyReport` | ReportController | 举报 | 4 | — | 否 |
| 76 | `createOrderReview` | ReviewController | 评价 | 4 | — | 否 |
| 77 | `listOrderReviews` | ReviewController | 评价 | 4 | — | 否 |
| 78 | `listMyReceivedReviews` | ReviewController | 评价 | 4 | — | 否 |
| 79 | `listMyNotifications` | NotificationController | 通知 | 4 | — | 否 |
| 80 | `getUnreadNotificationCount` | NotificationController | 通知 | 4 | — | 否 |
| 81 | `markNotificationsRead` | NotificationController | 通知 | 4 | — | 否 |
| 82 | `runPenaltyAutoUnbanTask` | InternalController | 系统定时任务 | 4 | — | 否 |
| 83 | `runReviewWindowCloseTask` | InternalController | 系统定时任务 | 4 | — | 否 |
| **合计** | **83 个** | 12 个 Controller | — | **15 / 10 / 30 / 28** | — | — |

**跨片复用说明（不体现为 operationId 复用）**

- 切片 3 调用切片 2 公开的「占用 / 释放」方法；不得直接写 `items` / `item_occupations`（E-02 / E-04）。
- 切片 4 与切片 3 共用 `InternalTaskService`，按 operationId 划分方法级归属（E-09）。
- 切片 3 只发布业务事件，通知写入由切片 4 实现（E-10）。
- `getItemOccupation` 的接口与空态由切片 2 负责，占用态返回内容由切片 3 补充（E-08，同一方法不得被两片同时修改）。

---

## 6. 表落位表（25 张）

| 切片 | 表数 | 表名 |
| --- | --- | --- |
| 切片 1 账户与共享内核 | **7** | `users`、`user_trade_zones`、`trade_zones`、`pickup_points`、`categories`、`item_conditions`、`idempotency_records` |
| 切片 2 商品与检索 | **3** | `items`、`item_images`、`item_occupations` |
| 切片 3 购买与交换 | **9** | `cart_items`、`orders`、`order_items`、`payments`、`swap_bundles`、`swap_bundle_items`、`swap_proposals`、`swap_proposal_items`、`swap_codes` |
| 切片 4 治理、评价与整合交付 | **6** | `reviews`、`reports`、`penalties`、`appeals`、`notifications`、`audit_logs` |
| **合计** | **25** | 无遗漏、无重复 |

---

## 7. 双人责任制

| 切片 | 后端主办 | 后端协办 | 前端主办 |
| --- | --- | --- | --- |
| 切片 1 | ———— | ———— | ———— |
| 切片 2 | ———— | ————（可复用切片 1 协办） | ———— |
| 切片 3（3A 购买） | ———— | ———— | ————、———— |
| 切片 3（3B 交换） | ———— | ———— | ————、———— |
| 切片 4 | ———— | ———— | ————、———— |

**固定角色**：接口契约负责人 1、后端负责人 1、验收人 1、前端负责人 1，可由后端或前端成员兼任。
**红线**：不得一人包干整片；协办必须真实参与评审与错误分支实现；姓名与 GitHub 账号由团队填写，禁止编造与共用。

---

## 8. 12 条协作规则（摘要）

> 完整内容见团队协作规则文件（附件版）；本节只做摘要，不另立版本。

| # | 规则 | 一句话 |
| --- | --- | --- |
| 1 | 唯一真源 | 接口只认 `openapi.yaml`；结构只认 `schema.md` 与 `V1~V4`；规则只认 `rules.md` / `state-machines.md`；错误码只认 `error-codes.md` |
| 2 | 三维边界锁定 | 表归属 + Java 包归属 + 前端路由前缀，三者同时落在本片才可开工 |
| 3 | 禁止越界 | 不改他片 Controller/Service/Repository/Entity；跨片只依赖对方 Service 接口，禁止依赖 `ServiceImpl` |
| 4 | 公共实体只读 | 6 个公共实体由切片 1 建立；其余切片只读（例外见 E-01~E-10） |
| 5 | 交接规则 | 切片闭合才交棒；禁止跳片、禁止带病交棒 |
| 6 | Git 规则 | 每人自己账号；`feat/slice<N>-<描述>`；PR + 1 人评审；`feat(切片名): 具体内容` |
| 7 | 契约变更 | 走 `api-freeze.md` §4；禁止私改、禁止新增 endpoint、禁止破坏性变更 |
| 8 | 数据库迁移 | `V1~V4` 只读；结构变更新增 `V5+`；新增前在空库顺序验证 |
| 9 | 错误处理 | 只用 `error-codes.md` 的 93 个码；失败用 `ErrorResponse`；禁止裸 500 |
| 10 | 越权规则 | 越权返回 403，不用 404 掩盖；Controller 与 Service 两层校验 |
| 11 | 幂等与并发 | 16 个必填幂等接口；状态转换用条件更新/乐观锁；占用与投影同事务 |
| 12 | 代码审查 | 每 PR 至少 1 人评审；交棒审查由下一片负责人做 6 项合规检查 |

---

## 9. 6 条红线（摘要）

| # | 红线 | 要点 |
| --- | --- | --- |
| 1 | 禁止一人包干一个迭代 | 每片至少 1 主办 + 1 协办 + 1 前端主办；90% 提交来自同一人即复核 |
| 2 | 禁止并行修改同一文件/模块 | 公共层在切片 1 定型；后续改动走「提需求 → 所有者改」 |
| 3 | 禁止未评审改契约或迁移 | 契约走 §4；迁移新增 `V5+`；哈希不一致且无台账 → 拒绝合并 |
| 4 | 禁止 AI 代码不跑本地测试就提交 | 至少编译 + 本片自测；字段/枚举/错误码要能 grep 到源头 |
| 5 | 禁止共用 GitHub 账号 | 每人自己账号；提交前 `git config user.name/email` 自检 |
| 6 | 禁止交棒时留未通过测试/编译错误 | 阻塞项必须本片解决，或把 operationId 移出并登记 |

---

## 10. 交接清单（10 项）

| # | 检查项 | 判定标准 | 负责人 |
| --- | --- | --- | --- |
| 1 | 本地全量构建通过 | `mvn clean package -DskipTests` → BUILD SUCCESS | 后端主办 |
| 2 | 应用可启动、健康检查通过 | `/actuator/health` 200；容器 Up (healthy) | 后端主办 |
| 3 | 交付标准逐项验证并留痕 | 本片验收项逐项有结论，失败项 0 | 后端主办 + 前端主办 |
| 4 | 无遗留未通过测试或编译错误 | 测试全绿、编译无错误 | 后端主办 |
| 5 | 契约未被修改 | `openapi.yaml` 前 16 位 `36bd7a03a184734c` | 接口契约负责人 |
| 6 | 未触碰归属外文件 | `git diff --stat` ⊆ 开工前清单 | 下一片负责人 |
| 7 | 提交信息规范、无 TODO 残留 | `feat(切片名): …`；无本片 TODO | 后端主办 |
| 8 | 已合并 main | 主干可启动、可跑上一片冒烟脚本 | 后端负责人 |
| 9 | 下一片负责人完成代码审查 | 6 项合规检查逐项通过 | 下一片负责人 |
| 10 | 口头交接完成 | 能复述交付物、遗留项、受限写 | 后端主办 + 下一片负责人 |

---

## 11. 常见问题（摘要）

| 编号 | 问题 | 处理要点 |
| --- | --- | --- |
| Q1 | 前人 Bug | 先分类「契约错 / 实现错」；实现错交归属片修；契约错走 `api-freeze.md` §4；禁止顺手改 |
| Q2 | 契约缺接口 | 先自查既有接口能否承载；确需新增按类型 F 三人审批；禁止先用后补 |
| Q3 | AI 代码不符契约 | 以契约改代码；把契约片段直接喂给 AI；逐项核对字段/类型/枚举/金额/时间 |
| Q4 | 本地跑不通 | 依次查端口 → 健康 → 迁移 checksum → 构建；禁止先删库 |
| Q5 | Git 冲突 | 停手判断同片/跨片；跨片即越界；合并后重跑构建与冒烟 |

---

## 附录 A 文件归属速查

> 口径：**独占** = 只有归属切片可写；**只读** = 其他切片只能 `SELECT`；**受限写** = 按列出字段条件更新，且必须经归属切片公开方法。

### A.1 切片 1：账户与共享内核

| 维度 | 归属 |
| --- | --- |
| 数据库表 | **独占（7）**：`users`、`user_trade_zones`、`trade_zones`、`pickup_points`、`categories`、`item_conditions`、`idempotency_records`（E-01） |
| Java 包路径 | `controller.AuthController`、`UserController`、`DictionaryController`；`service.AuthService`、`UserService`、`DictionaryService`（含 `service.impl`）；`repository.UserRepository`、`UserTradeZoneRepository`、`TradeZoneRepository`、`PickupPointRepository`、`CategoryRepository`、`ItemConditionRepository`、`IdempotencyRecordRepository`；`entity.User`、`UserTradeZone`、`TradeZone`、`PickupPoint`、`Category`、`ItemCondition`、`IdempotencyRecord` |
| Controller / operationId | 3 个 / **15** |
| 前端路由 | `/register`、`/login`、`/me`、`/me/profile`；全局布局与鉴权守卫 |

### A.2 切片 2：商品与检索

| 维度 | 归属 |
| --- | --- |
| 数据库表 | **独占（3）**：`items`、`item_images`、`item_occupations`；**只读**：`users`、`trade_zones`、`user_trade_zones`、`categories`、`item_conditions`；**受限写**：`idempotency_records`（E-01） |
| Java 包路径 | `controller.ItemController`；`service.ItemService`（含 `service.impl`）；`repository.ItemRepository`、`ItemImageRepository`、`ItemOccupationRepository`；`entity.Item`、`ItemImage`、`ItemOccupation` |
| Controller / operationId | 1 个 / **10** |
| 前端路由 | `/`、`/search`、`/items`、`/items/:id`、`/items/new`、`/items/:id/swap-setting`、`/me/items` |

### A.3 切片 3：购买与交换

| 维度 | 归属 |
| --- | --- |
| 数据库表 | **独占（9）**：`cart_items`、`orders`、`order_items`、`payments`、`swap_bundles`、`swap_bundle_items`、`swap_proposals`、`swap_proposal_items`、`swap_codes`；**受限写**：`items`（E-02）、`item_occupations`（E-04）、`idempotency_records`（E-01）；**只读**：`users`、`trade_zones`、`pickup_points`、`user_trade_zones` |
| Java 包路径 | `controller.CartController`、`OrderController`、`SwapController`、`InternalController`（2/4）；`service.CartService`、`OrderService`、`SwapService`、`InternalTaskService`（含 `service.impl`）；`repository.CartItemRepository`、`OrderRepository`、`OrderItemRepository`、`PaymentRepository`、`SwapBundleRepository`、`SwapBundleItemRepository`、`SwapProposalRepository`、`SwapProposalItemRepository`、`SwapCodeRepository`；`entity.CartItem`、`Order`、`OrderItem`、`Payment`、`SwapBundle`、`SwapBundleItem`、`SwapProposal`、`SwapProposalItem`、`SwapCode` |
| Controller / operationId | 4 个 / **30** |
| 前端路由 | `/cart`、`/checkout`、`/orders`、`/orders/:id`、`/orders/:id/pay-result`、`/orders/:id/pickup-point`、`/me/swap-bundles`、`/items/:id/propose-swap`、`/swaps/:id`、`/swaps/:id/swap-code` |

### A.4 切片 4：治理、评价与整合交付

| 维度 | 归属 |
| --- | --- |
| 数据库表 | **独占（6）**：`reviews`、`reports`、`penalties`、`appeals`、`notifications`、`audit_logs`；**受限写**：`items`（E-03）、`item_occupations`（释放，E-04）、`orders`（E-05）、`swap_proposals`（E-06）、`users`（E-07）、`idempotency_records`（E-01）；**只读**：`order_items`、`swap_proposal_items`、`trade_zones`、`pickup_points`、`categories`、`item_conditions` |
| Java 包路径 | `controller.AdminController`、`ReportController`、`ReviewController`、`NotificationController`、`InternalController`（2/4）；`service.AdminService`、`ReportService`、`ReviewService`、`NotificationService`、`InternalTaskService`（含 `service.impl`）；`repository.ReviewRepository`、`ReportRepository`、`PenaltyRepository`、`AppealRepository`、`NotificationRepository`、`AuditLogRepository`；`entity.Review`、`Report`、`Penalty`、`Appeal`、`Notification`、`AuditLog` |
| Controller / operationId | 5 个 / **28** |
| 前端路由 | `/items/:id/report`、`/me/reports`、`/me/reviews`、`/me/penalties`、`/me/appeals`、`/notifications`、`/orders/:id/reviews`、`/admin`、`/admin/users`、`/admin/items`、`/admin/penalties`、`/admin/reports`、`/admin/audit-logs` |

### A.5 公共实体与例外登记

**公共实体（6 个，切片 1 建立，自切片 2 起只读）**

| # | 实体 | 表 | 只读方 |
| --- | --- | --- | --- |
| 1 | User | `users` | 切片 2 / 3 / 4（切片 4 另有受限写：封禁与解封） |
| 2 | TradeZone | `trade_zones` | 切片 2 / 3 / 4 |
| 3 | Category | `categories` | 切片 2 / 3 / 4 |
| 4 | ItemCondition | `item_conditions` | 切片 2 / 3 / 4 |
| 5 | PickupPoint | `pickup_points` | 切片 3 / 4 |
| 6 | Notification | `notifications` | 切片 1 建表结构 / 切片 4 实现写入；切片 3 只发布事件 |

**例外登记（E-01 ~ E-10）**

| 编号 | 对象 | 归属 | 允许的其他切片操作 | 约束 |
| --- | --- | --- | --- | --- |
| E-01 | `idempotency_records` | 切片 1 | 切片 2/3/4 写入 | 只经切片 1 的幂等切面/拦截器；禁止各片自建写入 |
| E-02 | `items.status` / `version` | 切片 2 | 切片 3 条件更新（占用与释放） | 只经切片 2 公开的占用/释放方法 |
| E-03 | `items.status` / `banned_*` / `version` | 切片 2 | 切片 4 条件更新（下架与恢复） | 只经切片 2 公开方法；审计与联动归切片 4 |
| E-04 | `item_occupations` | 切片 2 | 切片 3 新增与释放；切片 4 释放 | 历史行的 `ref_type` / `ref_id` / `occupied_at` 不可改 |
| E-05 | `orders`（取消字段） | 切片 3 | 切片 4 条件更新（下架联动取消） | 只经切片 3 的取消订单方法 |
| E-06 | `swap_proposals`（作废字段） | 切片 3 | 切片 4 条件更新（整笔作废） | 只经切片 3 的整笔作废方法 |
| E-07 | `users`（`status` / `restricted_until` / `version`） | 切片 1 | 切片 4 条件更新（封禁与解封） | 只经切片 1 的用户状态方法；不改用户名/密码/昵称/角色 |
| E-08 | `ItemController.getItemOccupation` | 切片 2 | 切片 3 补充占用态返回内容 | 切片 2 负责接口与空态，切片 3 负责占用态内容；同一方法不得被两片同时改 |
| E-09 | `InternalController` 4 个 operationId | 切片 3（2 个）/ 切片 4（2 个） | 共用 `InternalTaskService` | 按 operationId 划分方法级归属；任务注册与调度在切片 4 收口 |
| E-10 | `notifications` 写入 | 切片 4 | 切片 3 只发布事件、不写表 | 切片 3 的事件必须幂等 |

---

## 附录 B 契约基线

| 项 | 值 |
| --- | --- |
| `openapi.yaml` SHA-256 前 16 | `36bd7a03a184734c` |
| `V1__init_schema.sql` | `620ad94bb6cf96b2` |
| `V2__seed_reference_data.sql` | `8a5f22d53904761a` |
| `V3__seed_development_data.sql` | `ab3c9c105aed2eea` |
| `V4__fix_review_window_index.sql` | `cbd952e089a2d0af` |
| `info.version` | `1.0.0-frozen-candidate` |
| 业务表 / 列 | 25 张 / 324 列 |
| 错误码 | 93 个 `DOMAIN_REASON`，15 个域前缀，E-01~E-17 |
| 必填幂等接口 / 可选幂等接口 | 16 / 16 |
| 权限声明 | 公开 10 / 需登录 55 / 仅管理员 14 / 系统内部 4 |
| 冻结时点 | 2026-10-09 后不允许新增接口 |
| 变更流程 | `../docs/iteration1/design/api-freeze.md` §4 |
| 迁移规则 | `V1~V4` 只读；新增 `V5+` |

> 注：文件大小与权限相关的具体限制以 `openapi.yaml` 与 `error-codes.md` 为准；`FILE_TOO_LARGE` 统一为 **422**（A-10）。

---

## 附录 C 已裁决与待裁决

**已裁决（唯一口径）**

| 编号 | 结论 |
| --- | --- |
| A-1 | 路线文件入仓 `handover/ITERATION-PLAN-VERTICAL-SLICES.md`；相对路径从 `handover/` 解析；根目录附件为历史参考 |
| A-2 | operationId 以 Controller 文件归属为唯一口径：15 / 10 / 30 / 28 = 83 |
| A-3 | 切片 4 统一 **28** |
| A-4 | 切片 2 配 1 主办 + 1 协办（可复用切片 1 协办）+ 1 前端主办 |
| A-5 | 切片 1 独占 **7** 张表（含 `user_trade_zones`） |
| A-10 | `FILE_TOO_LARGE` = **422** |

**已知待裁决（只登记）**

| 编号 | 事实 |
| --- | --- |
| A-6 | `docs/iteration1/DELIVERY.md` 是否解冻 |
| A-7 | `docs/iteration2/contracts/README.md` 权威顺序与份数计数 |
| A-8 | 是否重新生成 PDF（本任务不生成） |
| A-9 | `.mvnrepo/` 已入库与 `.gitignore` 缺项 |

---

## 变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v2.0 | 2026-11-11 | 交接包生成代理 | 入仓现行版：保留四切片、串行接力、双人责任制、契约冻结；按裁决表消解 A-1/A-2/A-3/A-4/A-5/A-10；新增 83 行唯一 operationId 分配表（15/10/30/28）、25 张表落位（7/3/9/6）、10 项交接清单、E-01~E-10；A-6~A-9 仅登记 | 生效 |
