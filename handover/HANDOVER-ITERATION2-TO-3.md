# 迭代二 → 迭代三后端交接

> 项目：CampusHub 校园/社区闲置交易平台  
> 依据：`handover/ITERATION-PLAN-VERTICAL-SLICES.md`、冻结契约 `docs/iteration1/design/openapi.yaml`（83 个 operationId）、状态机与协作规则  
> 阅读对象：迭代三后端开发者  
> 状态：开发交接草稿；请在本分支 PR 审查通过并完成联调后更新“验证状态”。

## 1. 迭代三目标与边界

迭代三交付切片 3 的 30 个 operationId，按顺序分成 3A 购买（先验收）和 3B 交换（3A 闭合后再开始）。本迭代拥有 9 张表：`cart_items`、`orders`、`order_items`、`payments`、`swap_bundles`、`swap_bundle_items`、`swap_proposals`、`swap_proposal_items`、`swap_codes`。

允许通过切片二提供的领域入口占用/释放商品，并通过切片一提供的幂等服务写入 `idempotency_records`。`items.status/version` 与 `item_occupations` 受协作规则 E-02/E-04 限制：不得从订单或交换代码绕过切片二领域入口直接改写。`users`、`trade_zones`、`pickup_points`、`user_trade_zones` 只读。通知由迭代四负责；本片仅发布业务事件。

## 2. 开发顺序与接口清单

### 3A：购买（先完成并验收）

- `CartController`：`getCart`、`addCartItem`、`updateCartItem`、`removeCartItem`
- `OrderController`：`submitOrder`、`listOrders`、`getOrderDetail`、`mockPayOrder`、`cancelOrder`、`selectPickupPoint`、`markOrderDelivered`、`confirmOrderReceipt`
- `InternalController`：`runOrderTimeoutReleaseTask`

关键规则：下单即占用；订单生成服务端 `expiresAt` 与 `remainingSeconds`；订单商品及估价须快照；并发下单不可超卖；付款和超时释放必须互斥；取货点必须属于订单交易区域；买家/卖家权限分别校验；付款使用项目定义的模拟支付流程；下单超时释放为幂等操作。冻结契约标记为必需幂等的接口须逐项核对 `Idempotency-Key`，不可只依赖前端防重。

### 3B：交换（3A 验收后开始）

- `SwapController`：`createSwapBundle`、`listMySwapBundles`、`getSwapBundle`、`updateSwapBundle`、`deleteSwapBundle`、`createSwapProposal`、`listSwapProposals`、`getSwapProposal`、`acceptSwapProposal`、`rejectSwapProposal`、`counterSwapProposal`、`cancelSwapProposal`、`getSwapCode`、`verifySwapCode`、`confirmSwap`、`createSwapReview`
- `InternalController`：`runSwapProposalExpireTask`

关键规则：交换包 1–5 件；整包接受，不支持部分接受；反提案仅允许一轮；交换提议与订单占用互斥；交换码一次性验证；72 小时过期后释放占用；整笔提议作废；状态推进、商品占用与释放放在同一事务中。所有双方角色检查和状态前置条件按冻结状态机实现。

## 3. 迭代二提供的后端依赖

预计可复用的依赖：

1. `ItemService` / 商品领域入口：须明确公开 `occupyForOrder`、`occupyForSwap`、`releaseOccupation`（或等效受控 API），并在事务中使用版本条件更新；迭代三不直接更新 `items`、`item_occupations`。
2. `IdempotencyService`：同一用户、键、接口与请求体重试应回放首次响应；请求体不同须返回 `409 COMMON_IDEMPOTENCY_KEY_REUSED`。
3. 认证用户信息及管理员角色；交易参与方不得以 ADMIN 身份交易。
4. 商品详情/占用查询：`getItemOccupation` 的占用空态由迭代二负责；迭代三应补充并约定订单/交换占用的展示关联信息，避免两个切片同时改同一 Controller。

开发开始前请在 PR 审查中确认这些接口的实际方法签名与事务边界。若尚未合并或未通过集成验证，先在迭代二 PR 的审查线程确认，不要自行直接写共享表。

## 4. 冻结契约与数据约定

- `openapi.yaml` 为请求、响应、权限、错误码及 operationId 的权威来源。不要直接改冻结文件或修改 V1–V4；如确需变更，先按 API freeze 流程获得团队确认，再新增迁移 V5+。
- 错误码必须逐字使用 `docs/iteration1/design/error-codes.md` 中的值；不要新造错误码。
- 金额按 `DecimalString` / PostgreSQL `NUMERIC(12,2)` 处理，不得用 `double` 计算。
- 状态转换参照 `docs/iteration1/design/state-machines.md`；占用互斥以 `item_occupations` 的活动记录与数据库唯一约束为最终防线。
- 订单超时和交换过期任务必须可重复执行；任务本身应支持内部调用鉴权，外部客户端不能访问 `/api/v1/internal/**`。
- 默认排序只接受 API 描述的白名单字段；分页从 0 开始，`size` 1–100。

## 5. 验收检查表

- [ ] 30 个 operationId 全部接入持久化服务，不返回占位异常。
- [ ] 3A 先单独演示“购物车 → 下单占用 → 模拟付款/取消/超时释放 → 送达 → 确认收货”完整路径。
- [ ] 通过并发请求验证同一商品不能同时进入两个有效订单/交换占用，付款与超时释放不能双重推进。
- [ ] 对 16 个带幂等要求的操作逐个做“首次提交、相同请求重试、不同请求复用键”验证。
- [ ] 验证不同用户越权访问为 403；未登录请求为 401；缺失资源为 404；状态冲突按契约返回 409/422。
- [ ] 验证取货点区域匹配、交换包数量边界、整包接受、唯一一次反提案、交换码一次性及 72 小时释放。
- [ ] 检查新增 Flyway 迁移可在干净 PostgreSQL 数据库顺序执行；保留原有 V1–V4。
- [ ] 完成 3A 验收后，才开始 3B；所有验收结果、已知缺陷和 API 依赖更新到本文件及进度材料。

## 6. 当前交接状态

当前后端分支包含迭代二账户/共享内核与商品切片的实现工作，Java 11 + Spring Boot Maven 打包编译已通过。仓库当前没有 `src/test` 测试用例；尚未连接 PostgreSQL/Docker，数据库迁移、并发、上传静态资源访问和端到端流程未能在本地运行验证。请将这些项目视为待验收项，不能据此认为运行验收已经完成。

本文件是交接草稿，不表示迭代三功能已经实现或通过验收。迭代二功能 PR 合并后，请将对应 PR 编号、合并 SHA、验证命令及结果补到本节，再由迭代三开发者确认依赖接口。
