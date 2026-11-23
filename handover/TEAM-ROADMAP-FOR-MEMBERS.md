# 团队路线说明（组员通俗版）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 团队路线说明（组员通俗版） |
| 版本 | **v1.0** |
| 日期 | 2026-11-11 |
| 读者 | 4 名后端 + 3 名前端 |
| 定位 | 通俗版路线：每个步骤做什么、前端跟到什么程度 |
| 与现有文件的关系 | 规划权威见 [`ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md)；本文件**不改变切片划分与计数**；协作规则见 [`COLLABORATION-RULES.md`](./COLLABORATION-RULES.md) |

> **冲突处理**：与契约或 `ITERATION-PLAN-VERTICAL-SLICES.md` 冲突时，以契约与 `ITERATION-PLAN` 为准；本文件不改变切片划分与计数。

> 本文所有表名、operationId、路由都来自已冻结资产与 `ITERATION-PLAN`，没有新造。

---

## 1. 整体节奏

- **串行接力**：同一时刻只有一个切片在写代码；这一片做到「编译通过 + 自测通过 + 合并主干 + 交接清单逐项打勾」后，下一片才开始。
- **双人责任制**：每片至少 1 名后端主办 + 1 名后端协办 + 1 名前端主办；协办可跨片复用，但不得一人包干整片。
- **契约冻结**：83 个 operationId 不改；`V1~V4` 不改；变更走 `api-freeze.md` §4。

---

## 2. 四个步骤概览

| 步骤 | 时间 | 后端做什么 | 前端做什么 | 交付标志 |
| --- | --- | --- | --- | --- |
| 步骤 1：账户与共享内核 | 10/13 ~ 10/20 | 注册、登录、退出、改密、当前用户、改资料、常用交易区域、通知设置、4 个字典接口；建立 7 张表（含 `user_trade_zones`）与幂等、乐观锁公共组件（共 15 个 operationId） | 注册页、登录页、个人主页、编辑资料页；全局布局 + 鉴权守卫 + 请求封装 + 统一错误提示 | 注册 → 登录 → 看自己主页端到端跑通；6 个公共实体冻结 |
| 步骤 2：商品与检索 | 10/21 ~ 11/2 | 商品列表与搜索、发布、详情、编辑、上下架、重新上架、交换意愿、占用查询、图片上传；建立占用台账（共 10 个 operationId） | 首页、搜索结果页、商品详情页、发布商品页、我的物品页、交换状态设置页 | 带图发布 → 搜索到 → 看清详情 → 我的物品可改可下架 |
| 步骤 3：购买与交换 | 11/3 ~ 11/20 | 3A 购买：购物车、下单即占用、30 分钟倒计时、模拟付款、取消、选取货点、交付、确认收货、超时释放任务；3B 交换：交换包、发起交换、接受/拒绝/反提案/取消、交换码、双方确认、72 小时过期任务（共 30 个 operationId） | 3A：购物车、提交订单、订单详情、付款结果、选择取货点；3B：交换包编辑、发起交换、交换提议详情、交换码页 | 购买主线与交换主线各端到端跑通；并发抢购不卖超、整笔作废验证通过 |
| 步骤 4：治理、评价与整合交付 | 11/21 ~ 12/10 | 举报提交与查询、管理员治理、违规下架与恢复、处罚与申诉、订单与交换互评、站内通知、自动解封与评价窗口关闭任务（共 28 个 operationId） | 举报入口、我的举报、通知列表、我收到的评价、订单互评、我的处罚、我的申诉，以及 5 个管理员页面 | 治理闭环可用；一键启动包在干净电脑跑通两条主线；测试报告、用户手册、技术报告、演示材料齐备 |
| **合计** | **10/13 ~ 12/10** | **83 个 operationId** | **22 个基线页面 + 4 个补充页面（其中 `/search`、`/notifications` 待确认）** | **25 张表全部落位，无遗漏无重复** |

> 表落位：**7 / 3 / 9 / 6 = 25**。完整表名见 `ITERATION-PLAN` 第 6 章。

---

## 3. 每个步骤详细说明

### 3.1 步骤 1：账户与共享内核（10/13 ~ 10/20）

**目标**：把后面三个步骤都要用的公共地基一次建好并冻结，同时让平台第一次真正跑起来。

**后端主力做什么**

1. 用户账户：`registerUser`、`login`、`refreshToken`（按约定返回 501）、`logout`、`getCurrentUser`、`changePassword`；密码只存摘要。
2. 个人资料：`updateMyProfile`（昵称 1~20、头像）、`getPublicProfile`。
3. 常用交易区域：`setMyTradeZones`，必须 1~5 个，只能从预设区域勾选。
4. 通知设置：`getMyNotificationSettings`、`updateMyNotificationSettings`。
5. 四个字典接口（只读）：`listTradeZones`、`listPickupPointsByZone`、`listCategories`、`listItemConditions`，只返回启用项并按 `sort_order` 升序。
6. 建 **7 张表**的 Entity 与 Repository：`users`、`user_trade_zones`、`trade_zones`、`pickup_points`、`categories`、`item_conditions`、`idempotency_records`。
7. 公共组件：幂等键处理（抢占 → 处理 → 写回 → 回放）与乐观锁模板（`WHERE id = ? AND version = ?`，影响 0 行返回 409）。
8. 把 `SecurityConfig` 从「全部放行」真正收紧，未登录返回 401。

**前端主力做什么**

1. 全局骨架：布局、导航、路由表、鉴权守卫（未登录访问 `/me` 跳 `/login`）。
2. 请求封装：统一注入 `Authorization: Bearer <JWT>`；解析 `code`/`message`/`data`/`traceId`/`timestamp`。
3. 统一错误提示：按错误码展示；按 `details[].field` 定位表单字段。
4. 注册页 `/register`、登录页 `/login`。
5. 个人主页 `/me`、编辑资料页 `/me/profile`（昵称、头像、常用区域；通知设置作为页内分区）。

**前端跟进到什么程度**

1. 注册、登录、退出、登录态保持四条流程在 Mock 上可走通。
2. 表单校验与契约一致：昵称长度、常用区域 1~5、必填项缺一即报错。
3. 错误分支可见：401 跳登录、403 提示无权限、400 定位字段。
4. 切真实后端只改 `baseURL` 一处。
5. 每个页面在真实后端手工走一遍。

**交付标志**：注册 → 登录 → 看自己主页端到端跑通；6 个公共实体宣布冻结。

---

### 3.2 步骤 2：商品与检索（10/21 ~ 11/2）

**目标**：让平台「有东西可看」：能带图发布、能搜索分类浏览、能看清详情、能管理自己的物品。

**后端主力做什么**

1. 商品列表与搜索：`listItems`，支持分类、区域、关键词、分页排序；**只返回 `IN_SALE`**；非法排序字段返回 `COMMON_INVALID_SORT_FIELD`。
2. 发布商品：`createItem`，必填缺一返回 `VALIDATION_INVALID_PARAMETER` 并给字段级 `details`；标题 1~60；区域必须属于卖家已勾选区域。
3. 商品详情：`getItemDetail`，含多图、卖家头像、区域、公开交换状态。
4. 我的物品：`listMyItems`、`updateItem`、`takeItemOffShelf`、`relistItem`，状态机 `IN_SALE` ↔ `OFF_SHELF` 与编辑限制要做对。
5. 交换意愿：`updateItemSwapPreference`（`ACCEPTING` / `NOT_ACCEPTING`），占用态或终态不能改成接受交换。
6. 占用查询：`getItemOccupation`，只有卖家本人或管理员能看，其他人 403。
7. 图片上传：`uploadImage`，JPG/PNG/WebP、单张 ≤ 5 MB、每商品 1~9 张、`sort_order` 最小者为封面；本地目录存储，数据库只记路径。
8. 建 `items`、`item_images`、`item_occupations` 三张表的 Entity 与 Repository，并对外提供「占用 / 释放」方法给步骤 3 调用。
9. 权限红线：除发布者外不能改或下架；上传图片必须登录。

> **开工前必办**：现有 `uploadImage` 签名缺 multipart `file`、`purpose`（CD-1），必须在切片 2 开工前修复。

**前端主力做什么**

1. 首页 `/`：分类浏览、区域筛选、列表分页。
2. 搜索结果页 `/search`：关键词搜索（**待确认 Q-34**，见第 6 章）。
3. 商品详情页 `/items/:id`：多图轮播、卖家头像、区域、成色、估价（显示「用户自填估价，仅供参考」）。
4. 发布商品页 `/items/new`：标题、描述、分类、成色、估价、1~9 张图上传与拖动排序、第一张作封面。
5. 我的物品页 `/me/items`：按在售/已预订/已售出/已下架分标签。
6. 交换状态设置页 `/items/:id/swap-setting`。
7. 图片格式、大小、数量三类限制在前端给出明确提示。

**前端跟进到什么程度**

1. 发布 → 首页搜到 → 详情看清，在 Mock 上先走通。
2. 三类图片失败（超大、格式不对、数量越界）有明确文案。
3. 越权要有反馈：他人账号改商品提示「无权操作」。
4. 列表分页、排序、筛选参数与 `listItems` 完全一致，不自造参数名。
5. 切真实后端手工走一遍发布与编辑，确认图片真的存下、封面真的是第一张。

**交付标志**：带图发布 → 搜索到 → 看清详情 → 我的物品可改可下架；占用台账的「占用 / 释放」方法交接给步骤 3。

---

### 3.3 步骤 3：购买与交换（11/3 ~ 11/20）

> 内部两个子阶段串行：**3A 购买（11/3 ~ 11/6 验收，13 个 operationId）** → **3B 交换（11/7 ~ 11/20 验收，17 个 operationId）**。3A 闭合后 3B 才开工。

**目标**：购买主线从看中到拿到手且不卖超；交换主线以物换物、谈崩也干净，任何一件拿不出来就整笔作废。

**后端主力做什么（3A 购买，13 个）**

1. 购物车：`getCart`（按卖家分组）、`addCartItem`（不可加自己的商品，同商品不重复）、`updateCartItem`、`removeCartItem`。
2. 提交订单：`submitOrder`，一单一卖家、自动拆单；**下单即占用**（商品 → `RESERVED`）。
3. 付款倒计时：`getOrderDetail` 返回 `expiresAt` 与 `remainingSeconds`，默认 30 分钟，**服务端算**。
4. 模拟付款：`mockPayOrder`，只打标不涉真实资金，固定提示「模拟付款，未产生真实扣款」。
5. 取消与释放：`cancelOrder`（仅待付款可取消），取消后商品立刻回到在售；已付款不能取消。
6. 超时释放：`runOrderTimeoutReleaseTask` 每分钟扫描 `PENDING_PAYMENT` 且已过期订单，改 `TIMEOUT_CANCELLED` 并释放商品；已付款订单**绝不**被取消。
7. 取货点：`selectPickupPoint`（付款后选，必须同区域）、`markOrderDelivered`（卖家）、`confirmOrderReceipt`（买家，商品转 `SOLD`）。
8. 并发不卖超：状态转换走条件更新，并发下只允许一个成功。
9. 幂等：`submitOrder`、`mockPayOrder` 必须校验 `Idempotency-Key`。

**后端主力做什么（3B 交换，17 个）**

1. 交换包：`createSwapBundle`（1~5 件、同一主人）、`listMySwapBundles`、`getSwapBundle`、`updateSwapBundle`、`deleteSwapBundle`。
2. 发起交换：`createSwapProposal`，双方全部物品变 `SWAPPING`，启动 72 小时倒计时。
3. 回应：`acceptSwapProposal`（整包接受，不接受物品子集）、`rejectSwapProposal`、`counterSwapProposal`（反提案仅一轮）、`cancelSwapProposal`。
4. 交换码：接受后生成**一个**一次性交换码；`getSwapCode` 查看、`verifySwapCode` 核销（只能一次）。
5. 双方确认：`confirmSwap`，双方各确认一次后 `COMPLETED`，物品转 `SOLD`。
6. 过期释放：`runSwapProposalExpireTask`，未处理提议 72 小时后过期并释放占用。
7. 整笔作废：任一物品被占用/下架，整笔交换作废，双方物品全部原样放回。
8. 同时占用互斥：同一商品不能同时被订单与交换占用。

**前端主力做什么（3A）**

1. 购物车页 `/cart`：按卖家分组。
2. 提交订单确认页 `/checkout`。
3. 订单列表 `/orders` 与详情 `/orders/:id`：详情页显示服务端倒计时。
4. 付款结果页 `/orders/:id/pay-result`。
5. 选择公共取货点 `/orders/:id/pickup-point`：只能选本订单区域。
6. 商品详情页「加入购物车」按钮。
7. 卖家「已交付」、买家「确认收货」按钮（同一详情页两种视角）。

**前端主力做什么（3B）**

8. 交换包编辑页 `/me/swap-bundles`：1~5 件，页面算估价合计并标注「仅展示，不做等价判定」。
9. 发起交换页 `/items/:id/propose-swap`。
10. 交换提议详情页 `/swaps/:id`：接受/拒绝/反提案/取消/确认五个动作。
11. 交换码页 `/swaps/:id/swap-code`：展示与核销。

**前端跟进到什么程度**

1. 倒计时**必须**用服务端 `expiresAt` / `remainingSeconds`，不拿本机时间算。
2. 并发结果正确展示：失败方看到「已被预订」；409 时按响应最新状态刷新。
3. 重复点击安全：同一逻辑操作复用同一 `Idempotency-Key`。
4. 「不可部分接受」要体现在界面：只有整包接受，没有物品级勾选。
5. 两条主线都在真实后端各走通一遍。

**交付标志**：购买主线与交换主线各端到端跑通；并交两份验证记录：并发抢购不卖超、整笔作废与双方物品原样退回。

---

### 3.4 步骤 4：治理、评价与整合交付（11/21 ~ 12/10）

**目标**：加上「管秩序」与「看口碑」，然后整理成换台电脑也能一键启动的交付包。

**后端主力做什么**

1. 举报：`reportItem`（同一人对同一商品只保留一条待处理举报）、`listMyReports`、`getMyReport`。
2. 管理员治理：`adminSearchUsers`、`adminGetUserDetail`、`adminBanUser`（默认封禁三天）、`adminListUserPenalties`、`adminListPenalties`、`adminLiftPenalty`、`adminListAppeals`、`adminResolveAppeal`、`adminListReports`、`adminHandleReport`、`adminListAuditLogs`、`adminListDictionary`。
3. 违规下架与恢复：`adminTakedownItem`、`adminRestoreItem`；下架要联动取消订单、整笔作废交换、释放占用、通知双方；恢复前确认无占用冲突。
4. 处罚与申诉（用户侧）：`listMyPenalties`、`createAppeal`（一个处罚最多一条申诉）、`listMyAppeals`。
5. 评价：`createOrderReview`、`listOrderReviews`、`listMyReceivedReviews`、`createSwapReview`（完成后才能评，每人每单一次，7 天窗口，对方评价在双方都评完或窗口关闭后可见）。
6. 通知：`listMyNotifications`、`getUnreadNotificationCount`、`markNotificationsRead`，并实现全部业务事件的通知写入；通知写入必须幂等。
7. 定时任务：`runPenaltyAutoUnbanTask`（到期自动解封）、`runReviewWindowCloseTask`（只关闭进行中的评价，不回退双方已评完的订单）。
8. 治理红线：治理动作必须填原因、必须写审计、处罚与申诉记录永不删除、管理员不参与交易。
9. 整合交付：一键启动包（含示例数据）、测试报告、用户手册、技术报告、演示材料。

> **开工前**：Q-04 管理员账号来源需由后端负责人确认（见 `DEFINITIONS-AND-PROTOCOLS.md`），不修改 `V3` 种子脚本。

**前端主力做什么**

1. 举报入口 `/items/:id/report`（弹窗即可）与我的举报 `/me/reports`。
2. 通知列表 `/notifications`（含未读角标；**待确认 Q-33**，见第 6 章）。
3. 我收到的评价 `/me/reviews`；订单互评入口 `/orders/:id/reviews`。
4. 我的处罚 `/me/penalties`、我的申诉 `/me/appeals`。
5. 管理员后台首页 `/admin`。
6. 管理员用户搜索与详情 `/admin/users`。
7. 管理员商品审核与下架 `/admin/items`。
8. 管理员处罚记录与申诉处理 `/admin/penalties`。
9. 管理员举报处理 `/admin/reports` 与审计日志 `/admin/audit-logs`。
10. 治理页面原因输入框必填校验，提交前提示「必须填写原因」。

**前端跟进到什么程度**

1. 管理员页面在路由层拦截：非管理员访问 `/admin/**` 直接跳走，不靠隐藏菜单。
2. 处罚状态展示完整：处罚记录、解封时间、哪些操作被禁止。
3. 通知未读数全站可见并实时更新，点已读后角标减。
4. 每个页面在真实后端走一遍，含管理员五个页面。
5. 拿到一键启动包后，在**没有开发环境的电脑**上按手册走一遍购买与交换主线。

**交付标志**：治理闭环可用；一键启动包在干净电脑跑通两条主线；测试报告、用户手册、技术报告、演示材料齐备；进度更新 3/4 材料已提交。

---

## 4. 前端通用跟进原则

1. **不等后端。** 后端未实现时用 Prism 基于 `../docs/iteration1/design/openapi.yaml` 起 Mock；需要错误分支时开 `--errors`。
2. **按错误码分支。** 例如 401 `AUTH_UNAUTHENTICATED` 跳登录、403 `AUTH_NOT_RESOURCE_OWNER` 提示无权、409 `ITEM_ALREADY_RESERVED` 提示已被预订；不按 HTTP 状态或文案猜。
3. **不改契约。** 前端不修改 `openapi.yaml`；发现缺接口或字段不对，登记后走 `api-freeze.md` §4，不先改自己的类型文件。
4. **每页真实走一遍。** 每页在真实后端至少手工走一遍完整流程（含失败路径），不能只交「Mock 上能点」。
5. **参与联调日。** 每步第 4 天联调，当场切 `baseURL`、当场记录字段/类型差异，当天登记。

---

## 5. 角色表

| 步骤 | 后端主办 | 后端协办 | 前端主办 | 交付标志 |
| --- | --- | --- | --- | --- |
| 步骤 1 | ———— | ———— | ———— | 注册 → 登录 → 看自己主页跑通；6 个公共实体冻结 |
| 步骤 2 | ———— | ————（可复用步骤 1 协办） | ———— | 带图发布 → 搜索到 → 详情 → 我的物品可改可下架 |
| 步骤 3（3A 购买） | ———— | ———— | ————、———— | 购买主线跑通；并发不卖超 |
| 步骤 3（3B 交换） | ———— | ———— | ————、———— | 交换主线跑通；整笔作废验证通过 |
| 步骤 4 | ———— | ———— | ————、———— | 治理闭环；一键启动包跑通两条主线 |

**固定角色**：接口契约负责人 1、后端负责人 1、验收人 1、前端负责人 1，可由后端或前端成员兼任。姓名与 GitHub 账号由团队填写，禁止编造与共用。

---

## 6. 待确认页面标记

| 路由 | 页面 | 状态 | 若评审决定做 | 若评审决定不做 |
| --- | --- | --- | --- | --- |
| `/search` | 搜索结果页 | **待确认 Q-34**（是否作为独立页面、搜索维度） | 归**切片 2**，按 `listItems` 的 `keyword` 等参数实现 | 前端不排期；搜索能力内嵌首页 |
| `/notifications` | 通知列表 | **待确认 Q-33**（PF-23~PF-26 是否纳入页面基线） | 归**切片 4**，按通知 3 个 operationId 实现 | 前端不排期；保留接口实现 |

> Q-33 / Q-34 详见 [`../docs/iteration1/glossary-assumptions-questions.md`](../docs/iteration1/glossary-assumptions-questions.md)。
> 两项均为 P2，不阻塞切片 1/2/3 开工。

---

## 7. 疑问索引

| 疑问类型 | 看哪份文档 |
| --- | --- |
| 规则全文、红线、交接清单、Git、Q1~Q5 | [`./COLLABORATION-RULES.md`](./COLLABORATION-RULES.md) |
| 切片划分、83 个 operationId 分配、表落位、双人责任制 | [`./ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md) |
| 交接事实、已裁决/待裁决、代码缺陷 | [`./HANDOVER-ITERATION1-TO-2.md`](./HANDOVER-ITERATION1-TO-2.md) |
| 接口路径、字段、枚举 | [`../docs/iteration1/design/openapi.yaml`](../docs/iteration1/design/openapi.yaml) |
| 错误码含义与前端处理 | [`../docs/iteration1/design/error-codes.md`](../docs/iteration1/design/error-codes.md) |
| 业务规则 / 状态机 | [`../docs/iteration1/rules.md`](../docs/iteration1/rules.md)、[`../docs/iteration1/state-machines.md`](../docs/iteration1/state-machines.md) |
| 表结构 | [`../docs/iteration1/design/schema.md`](../docs/iteration1/design/schema.md) |
| 契约能否改、怎么改 | [`../docs/iteration1/design/api-freeze.md`](../docs/iteration1/design/api-freeze.md) |
| 执行步骤与审查清单 | [`./EXECUTION-CHECKLIST.md`](./EXECUTION-CHECKLIST.md) |
| 细节定义（契约测试维度、通知机制候选、错误码唯一化） | [`./DEFINITIONS-AND-PROTOCOLS.md`](./DEFINITIONS-AND-PROTOCOLS.md) |

---

## 变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-11-11 | 交接包补充代理 | 首版通俗版：四步概览与详细说明（后端/前端/跟进程度/交付标志）、前端通用原则、角色表（含切片 2 协办）、`/search` Q-34 与 `/notifications` Q-33 待确认标记、疑问索引 | 生效 |
