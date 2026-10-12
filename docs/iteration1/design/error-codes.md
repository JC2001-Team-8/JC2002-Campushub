# 迭代一 统一错误码表（Error Codes）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 统一错误码表 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 接口契约负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 规范文件 | [openapi.yaml](./openapi.yaml)（`components.schemas.ErrorResponse`、`components.responses.*`） |
| 依据文档 | `../requirements.md` §7（E-01~E-17）、`../rules.md` §9、`../permission-matrix.md` |
| 冻结约束 | 错误码亦属契约：**新增错误码需走冻结变更流程**（见 [api-freeze.md](./api-freeze.md) §4） |

---

## 1. 错误码命名规范

| 规则 | 说明 |
| --- | --- |
| 格式 | `DOMAIN_REASON`（大写下划线） |
| 域前缀 | `AUTH_` 认证授权 / `USER_` 用户与处罚 / `ITEM_` 商品 / `CART_` 购物车 / `ORDER_` 订单 / `SWAP_` 交换 / `REVIEW_` 评价 / `REPORT_` 举报 / `GOVERNANCE_` 治理 / `APPEAL_` 申诉 / `FILE_` 文件 / `VALIDATION_` 参数校验 / `COMMON_` 通用 |
| 唯一性 | 同一 `code` 只对应一种语义，**不随接口变化** |
| 稳定性 | `code` 是**机器可判定**的标识，前端**必须**依据 `code` 分支，**不得**依赖 `message` 文本 |
| 可读性 | `message` 为面向用户的中文提示，可随文案优化调整 |
| 细节 | 补充信息放 `details`（结构随 `code` 变化） |

### 1.1 HTTP 状态码语义分区

| HTTP | 语义 | 触发特征 |
| --- | --- | --- |
| 400 | 语法/格式错误 | 类型错误、长度越界、缺必填、非法枚举值、非法排序字段、缺治理原因 |
| 401 | 未认证 | 未携带令牌、令牌无效/过期、登录凭据错误 |
| 403 | 已认证但无权限 | 非本人、被处罚禁写、非管理员、管理员参与交易、目标为管理员 |
| 404 | 资源不存在 | 路由目标不存在（**不用于掩盖越权**） |
| 409 | 状态/并发冲突 | 占用冲突、终态保护、幂等冲突、乐观锁冲突、重复封禁、重复申诉 |
| 422 | 业务规则不满足 | 件数越界、跨区、不接受交换、未完成评价、部分接受、文件超限 |
| 429 | 限流 | 敏感接口（登录/注册） |
| 500 | 服务端错误 | 未预期异常 |
| 501 | 特性未启用 | `refreshToken`（Q-07 未启用） |
| 503 | 依赖不可用/维护 | 数据库/文件存储不可用 |

### 1.2 通用响应体

```json
{
  "code": "ITEM_ALREADY_RESERVED",
  "message": "商品已被其他订单占用",
  "details": { "itemId": 12, "currentStatus": "RESERVED" },
  "traceId": "7f9c1a2b3d4e5f60",
  "timestamp": "2026-10-12T13:20:41Z"
}
```

> **成功响应的 `code` 固定为 `OK`**，不使用 HTTP 200 + 业务错误码的混用模式；
> 业务失败一律使用非 2xx HTTP 状态码 + `ErrorResponse`。

---

## 2. 错误场景覆盖（E-01 ~ E-17）

> `../requirements.md` §7 定义的 17 类错误场景，逐条映射到错误码。

| 场景 | 语义 | 主错误码 | HTTP | 可重试 | 接口示例 |
| --- | --- | --- | --- | --- | --- |
| E-01 | 未登录 | `AUTH_UNAUTHENTICATED` / `AUTH_TOKEN_EXPIRED` | 401 | 登录后重试 | 全部需登录接口 |
| E-02 | 非本人资源 | `AUTH_NOT_RESOURCE_OWNER` | **403**（不用 404） | 否 | `updateItem`、`confirmOrderReceipt` |
| E-03 | 被处罚用户禁写 | `USER_RESTRICTED_CANNOT_WRITE`（含 `restrictedUntil`） | 403 | 解封后重试 | `submitOrder`、`createItem` |
| E-04 | 商品已被占用 | `ITEM_ALREADY_RESERVED` / `ITEM_RESERVED_BY_SWAP` | 409 | 否 | `submitOrder`、`addCartItem` |
| E-05 | 订单已超时/已取消后付款 | `ORDER_TIMEOUT_RELEASED` / `ORDER_ALREADY_CANCELLED` | 409 | 否 | `mockPayOrder` |
| E-06 | 重复提交（幂等命中） | 无错误码 —— **返回首次结果**（`OK`，200/201） | 200/201 | **是（安全重试）** | `submitOrder`、`mockPayOrder` |
| E-07 | 交换包件数越界 | `SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE` | 422 | 修正后重试 | `createSwapBundle`、`updateSwapBundle` |
| E-08 | 目标商品不接受交换 | `SWAP_TARGET_NOT_ACCEPTING` | 422 | 否 | `createSwapProposal` |
| E-09 | 提议已终结仍操作 | `SWAP_PROPOSAL_ALREADY_FINALIZED` / `SWAP_PROPOSAL_ALREADY_ACCEPTED` / `SWAP_PROPOSAL_COUNTERED` | 409 | 否 | `acceptSwapProposal`、`rejectSwapProposal` |
| E-10 | 商品被其他交换占用 | `SWAP_ITEM_OCCUPIED` | 409 | 否 | `createSwapProposal`、`acceptSwapProposal` |
| E-11 | 交换中商品被下架 → 整笔作废 | `SWAP_PROPOSAL_VOIDED`（响应含 `terminationReason=VOID_ITEM_BANNED`） | 409 | 否 | `getSwapProposal`（读取态）、`acceptSwapProposal` |
| E-12 | 交换码重复核销 | `SWAP_CODE_ALREADY_VERIFIED` | 409 | 否 | `verifySwapCode` |
| E-13 | 重复封禁已在处罚中的用户 | `GOVERNANCE_ACTIVE_PENALTY_EXISTS` | 409 | 否 | `adminBanUser` |
| E-14 | 重复举报同一商品 | `REPORT_ALREADY_EXISTS`（**幂等返回 200 + 既有记录**） | 200 | 否 | `reportItem` |
| E-15 | 对未完成订单评价 | `REVIEW_TRANSACTION_NOT_COMPLETED` | 409 | 完成后再评 | `createOrderReview`、`createSwapReview` |
| E-16 | 幂等键复用但请求体不同 | `COMMON_IDEMPOTENCY_KEY_REUSED` | 409 | 换新键 | 全部幂等接口 |
| E-17 | 跨区域交易 | `TRADE_ZONE_MISMATCH` | 422 | 否 | `submitOrder`、`createSwapProposal` |

**覆盖结论：E-01 ~ E-17 全部 17 类场景均有明确错误码或明确的「无错误码」语义（E-06 为幂等成功回放）。**

### 2.1 错误码规模统计

| 域前缀 | 错误码数 | 说明 |
| --- | --- | --- |
| `AUTH_` | 7 | 认证与授权 |
| `USER_` | 3 | 用户与处罚状态 |
| `ITEM_` | 10 | 商品 |
| `TRADE_ZONE_` | 3 | 交易区域（含跨区 E-17） |
| `PICKUP_POINT_` | 2 | 公共取货点 |
| `CART_` | 3 | 购物车 |
| `ORDER_` | 11 | 订单与付款 |
| `SWAP_` | 21 | 交换（最复杂域，含整包接受/整笔作废/交换码） |
| `REVIEW_` | 4 | 评价 |
| `REPORT_` | 4 | 举报 |
| `GOVERNANCE_` | 5 | 治理处置 |
| `APPEAL_` | 4 | 申诉 |
| `FILE_` | 4 | 文件上传 |
| `VALIDATION_` | 4 | 参数校验 |
| `COMMON_` | 8 | 通用（幂等/并发/限流） |
| **合计** | **93** | 其中 **72 个**已在 `openapi.yaml` 的响应示例中实际引用；其余 21 个为**已定义保留码**（同域其他资源不存在/状态不匹配等场景，实现时按此表返回，避免临时发明新码） |

---

## 3. 完整错误码清单

### 3.1 通用（`COMMON_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `COMMON_INVALID_SORT_FIELD` | 400 | 排序字段不在白名单 | 传 `sort` 为不支持的字段（如 `rating`、`distance`） | OOS-09 | 重置为默认排序并提示 |
| `COMMON_IDEMPOTENCY_KEY_REUSED` | 409 | 同一幂等键但请求体不同 | E-16 | R-17 | 生成**新的** `Idempotency-Key` 后重试 |
| `COMMON_VERSION_CONFLICT` | 409 | 乐观锁版本不匹配 | `version` 与库中不一致；详情含 `currentVersion`/`currentStatus` | R-18 | 重新拉取详情，提示用户数据已更新后重试 |
| `COMMON_RATE_LIMITED` | 429 | 触发限流 | 登录/注册高频调用；响应头 `Retry-After` | — | 按 `retryAfterSeconds` 倒计时后重试 |
| `COMMON_INTERNAL_ERROR` | 500 | 服务端未预期异常 | 任意 | — | 展示通用错误 + `traceId`，允许重试 |
| `COMMON_SERVICE_UNAVAILABLE` | 503 | 依赖不可用/维护中 | 数据库/文件存储不可用 | OOS-17 | 稍后重试，展示维护提示 |
| `COMMON_NOT_IMPLEMENTED` | 501 | 特性未启用 | `refreshToken`（Q-07 未启用） | Q-07 | 回退到「7 天令牌 + 重新登录」策略 |
| `COMMON_RESOURCE_NOT_FOUND` | 404 | 通用资源不存在 | 兜底（优先使用具体域的 `*_NOT_FOUND`） | — | 返回上一页 |

### 3.2 参数校验（`VALIDATION_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `VALIDATION_INVALID_PARAMETER` | 400 | 参数校验失败 | 类型错误、长度越界、枚举非法、缺必填；`details` 为字段级列表 | R-10 | **按 `details[].field` 定位表单字段**逐项提示 |
| `VALIDATION_REASON_REQUIRED` | 400 | **治理操作缺少原因** | 下架/封禁/解封/恢复/申诉处理/举报处理未填 `reason`/`note` | **R-34** | 聚焦原因输入框，提示「必须填写原因」 |
| `VALIDATION_PAGE_SIZE_EXCEEDED` | 400 | 分页 `size` 超上限 | `size > 100` | — | 重置为 20 后重试 |
| `VALIDATION_DATE_RANGE_INVALID` | 400 | 时间范围非法 | `from > to`（处罚记录查询） | — | 修正时间范围 |

### 3.3 认证与授权（`AUTH_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `AUTH_UNAUTHENTICATED` | 401 | 未登录或登录态失效 | E-01：未带令牌/令牌无效 | FR-103 / R-01 | **清空本地令牌**，跳转登录页并记住来源 |
| `AUTH_TOKEN_EXPIRED` | 401 | 令牌已过期 | 超过 7 天（A-06） | FR-103 / R-01 | 清空令牌跳登录；若启用刷新则先尝试 `refreshToken` |
| `AUTH_INVALID_CREDENTIALS` | 401 | 凭据错误 | 登录密码错误、改密的原密码错误。**不区分账号是否存在**（防枚举） | FR-102 | 统一提示「用户名或密码错误」，**不清空已填用户名** |
| `AUTH_USERNAME_TAKEN` | 409 | 用户名或邮箱已被占用 | 注册时重名（`citext` 大小写不敏感） | FR-101 | 提示并聚焦用户名输入框，建议加后缀 |
| `AUTH_NOT_RESOURCE_OWNER` | **403** | 非本人资源 | **E-02**：操作他人商品/订单/交换/交换包 | R-02 / P-00-B | 提示无权限；**返回上一页**（不引导重试） |
| `AUTH_ADMIN_REQUIRED` | 403 | 需要管理员权限 | 非管理员调用 `/admin/**` | permission-matrix §6 | 提示无权限；隐藏管理入口 |
| `AUTH_FORBIDDEN_TRADING_AS_ADMIN` | 403 | 管理员不可参与交易 | **P-00-A**：管理员下单/发布/发起交换 | P-00-A | 提示「管理员账号不参与交易」 |

### 3.4 用户与处罚（`USER_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `USER_NOT_FOUND` | 404 | 用户不存在 | 查询不存在用户 ID | FR-501 | 返回列表页 |
| `USER_RESTRICTED_CANNOT_WRITE` | **403** | **被处罚用户禁止该写操作** | **E-03**；`details.restrictedUntil` + `penaltyId` | FR-503 / R-33 | **展示处罚提示条 + 解封时间 + 申诉入口**；禁用交易类按钮 |
| `USER_TRADE_ZONE_REQUIRED` | 422 | 尚未勾选常用交易区域 | 发布商品前未勾选任何区域 | FR-107 / R-15 | 引导跳转「编辑资料 → 勾选交易区域」 |

### 3.5 商品（`ITEM_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `ITEM_NOT_FOUND` | 404 | 商品不存在或已删除 | 详情/编辑不存在的商品 | FR-202 | 返回列表 |
| `ITEM_ALREADY_RESERVED` | **409** | 商品已被**订单**占用 | **E-04**；`details.currentStatus=RESERVED` | FR-304 / R-16 | 刷新商品状态；禁用下单/加购按钮 |
| `ITEM_RESERVED_BY_SWAP` | 409 | 商品已被**交换**占用 | E-10 的订单侧表述；`currentStatus=SWAPPING` | R-23 | 同上，提示「该商品正在交换中」 |
| `ITEM_NOT_PURCHASABLE` | 422 | 商品不可购买 | 已售出/已下架/违规下架 | FR-301 / R-04 | 从购物车移除或灰显 |
| `ITEM_LOCKED_BY_OCCUPATION` | 409 | 占用态禁止该操作 | **R-11**：占用态改关键字段；有占用时下架/重新上架 | R-11 / FR-202~204 | 禁用编辑入口；提示「交易进行中，暂不可修改」 |
| `ITEM_STATUS_NOT_EDITABLE` | 409 | 终态禁止编辑 | `SOLD` / `BANNED` 商品编辑（I-17） | R-11 / I-17 | 隐藏编辑按钮 |
| `ITEM_BANNED_CANNOT_RELIST` | 409 | 违规下架不可自行上架 | **I-16**：`BANNED` 商品调 `relistItem` | FR-204 / I-16 | 提示「违规下架需联系管理员」，**不提供**重新上架按钮 |
| `ITEM_STATUS_NOT_SWAPPABLE` | 409 | 该状态不可设为接受交换 | **R-20**：占用态/终态改为 `ACCEPTING` | FR-401 / R-20 | 禁用交换开关并说明原因 |
| `ITEM_NOT_OWN_SELLER` | 403 | 非商品卖家 | 编辑他人商品（同 `AUTH_NOT_RESOURCE_OWNER` 的商品域别名） | P-00-B | 同 E-02 |
| `ITEM_TRADE_ZONE_NOT_SELECTED` | 422 | 所选交易区域不在已勾选范围内 | **R-15**：`tradeZoneId` ∉ 用户已勾选区域 | FR-209 / R-15 | 提示并引导勾选该区域 |

### 3.6 交易区域与取货点（`TRADE_ZONE_` / `PICKUP_POINT_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `TRADE_ZONE_MISMATCH` | **422** | **跨区域交易** | **E-17**：商品区域 ≠ 订单/交换区域 | **R-15 / OOS-07** | 提示「不支持跨区交易」；引导切换区域筛选 |
| `TRADE_ZONE_NOT_FOUND` | 404 | 交易区域不存在或已停用 | 传入无效区域 ID | FR-107 | 重新拉取区域列表 |
| `TRADE_ZONE_NOT_SELECTED` | 422 | 买家未勾选该交易区域 | 下单时区域 ∉ 买家已勾选区域 | R-15 | 引导勾选 |
| `PICKUP_POINT_NOT_FOUND` | 404 | 取货点不存在或已停用 | 传入无效取货点 ID | FR-309 | 重新拉取取货点列表 |
| `PICKUP_POINT_ZONE_MISMATCH` | **422** | 取货点不属于订单交易区域 | **R-06**：取货点区域 ≠ 订单区域 | FR-309 / R-06 | 仅展示同区域取货点，重新选择 |

### 3.7 购物车（`CART_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `CART_ITEM_NOT_FOUND` | 404 | 购物车条目不存在 | 删除/修改已移除的条目 | — | 刷新购物车（删除场景可静默成功） |
| `CART_OWN_ITEM_NOT_ALLOWED` | **422** | 不可加入自己发布的商品 | **R-04** | 隐藏自家商品的加购按钮 |
| `CART_ITEM_NOT_PURCHASABLE` | 422 | 条目不可结算 | 商品被占用/售出/下架 | FR-302 / R-04 | 灰显条目 + 展示 `unavailableReason`，禁止勾选 |

### 3.8 订单（`ORDER_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `ORDER_NOT_FOUND` | 404 | 订单不存在 | 查询不存在订单 | FR-311 | 返回订单列表 |
| `ORDER_TIMEOUT_RELEASED` | **409** | **订单已超时取消且商品已释放** | **E-05**；「付款 vs 超时」并发时付款方失败 | **R-07** | 展示「订单已超时」；**停止倒计时**；隐藏付款按钮 |
| `ORDER_ALREADY_CANCELLED` | 409 | 订单已取消 | 重复取消/取消后付款 | O-03 | 刷新为终态视图 |
| `ORDER_STATUS_NOT_PAYABLE` | 409 | 当前状态不可付款 | 非 `PENDING_PAYMENT` 时付款 | O-02 | 刷新订单状态 |
| `ORDER_CANNOT_CANCEL_AFTER_PAID` | **409** | 已付款不可自助取消 | **E-05 相关**；Q-08 假设不允许 | **Q-08 / A-14** | 提示需联系管理员；**隐藏取消按钮**（已付款后） |
| `ORDER_STATUS_NOT_DELIVERABLE` | 409 | 当前状态不可标记交付 | 非 `PAID` 时交付 | O-06 | 刷新状态 |
| `ORDER_PICKUP_POINT_NOT_SELECTED` | **409** | 未选取货点不可交付 | **R-06 / O-06** 前置条件 | FR-310 / R-06 | 提示卖家等待买家选点 |
| `ORDER_NOT_PAID_FOR_PICKUP_SELECTION` | 409 | 未付款不可选取货点 | A-18 / Q-25 假设「付款后选点」 | FR-309 / A-18 | 先引导付款 |
| `ORDER_ALREADY_DELIVERED` | 409 | 已交付不可改取货点 | `DELIVERED` 后修改 | O-05 | 隐藏修改入口 |
| `ORDER_STATUS_NOT_DELIVERED` | 409 | 未交付不可确认收货 | 非 `DELIVERED` 时确认 | O-07 | 刷新状态 |
| `ORDER_ITEM_SELLER_MISMATCH` | 422 | 订单明细跨卖家 | 违反 **C-02**（一单一卖家） | FR-303 / C-02 | 重新按卖家分组下单 |

### 3.9 交换（`SWAP_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `SWAP_BUNDLE_NOT_FOUND` | 404 | 交换包不存在 | 查询/编辑不存在的包 | FR-402 | 返回列表 |
| `SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE` | **422** | **交换包件数越界** | **E-07**：件数 <1 或 >5；`details.currentCount/min/max` | **FR-402 / SB-01** | 提示 1–5 件；禁用超额添加 |
| `SWAP_BUNDLE_ITEM_NOT_OWNED` | 422 | 包内物品不属于本人 | SB-02 | FR-402 / SB-02 | 刷新可选物品列表 |
| `SWAP_ITEM_IN_ACTIVE_BUNDLE` | **409** | 物品已在其他进行中交换包 | **SB-03** | R-21 / SB-03 | 提示该物品已被占用 |
| `SWAP_BUNDLE_LOCKED` | **409** | 交换包被进行中提议引用，不可编辑/删除 | **SB-04** | R-21 / SB-04 | **禁用编辑入口**并说明「交换进行中」 |
| `SWAP_BUNDLE_STATUS_NOT_EDITABLE` | 409 | 交换包终态不可编辑 | `COMPLETED` | B-12 | 隐藏编辑 |
| `SWAP_PROPOSAL_NOT_FOUND` | 404 | 交换提议不存在 | 查询不存在提议 | FR-404 | 返回列表 |
| `SWAP_ITEM_OCCUPIED` | **409** | **商品被其他进行中交换占用** | **E-10**；`details.occupiedBy=SWAP` | **R-23** | 刷新商品状态；提示已被占用 |
| `SWAP_TARGET_NOT_ACCEPTING` | **422** | 目标商品不接受交换 | **E-08** | FR-404 / R-20 | 隐藏/禁用「发起交换」按钮 |
| `SWAP_SELF_PROPOSAL_NOT_ALLOWED` | 422 | 不可与自己交换 | S-01 `ck_swap_proposals_parties` | FR-404 | 不可选自己的商品 |
| `SWAP_PROPOSAL_ALREADY_ACCEPTED` | **409** | 提议已被接受 | **E-09**（IDEM-B）；`details.swapCodeId` | **R-22 / S-02** | 跳转交换码页；**不重复生成交换码** |
| `SWAP_PROPOSAL_ALREADY_FINALIZED` | **409** | 提议已终结 | **E-09**：已拒绝/过期/取消/完成 | 状态机 §4.2 | 展示终态与 `terminationReason`；隐藏操作按钮 |
| `SWAP_PROPOSAL_COUNTERED` | 409 | 提议已被反提案 | S-04 后原提议状态 | FR-407 | 跳转到**新提议**（`parentProposalId`） |
| `SWAP_COUNTER_NOT_ALLOWED` | **409** | **反提案仅允许一轮** | **R-28 / A-10**：`counterDepth=1` 再反提案 | **FR-407 / R-28** | **隐藏反提案按钮**并提示「仅可反提案一次」 |
| `SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED` | **422** | **不支持部分接受** | **OOS-04**：accept 请求携带物品子集 | **FR-405 / OOS-04** | 前端**不得**提供物品级勾选（整包接受） |
| `SWAP_PROPOSAL_VOIDED` | **409** | 提议已**整笔作废** | **E-11 / R-24**；`details.terminationReason=VOID_ITEM_*` | **R-24 / R-35** | 展示作废原因（哪件被占/下架）；提示可重新发起 |
| `SWAP_CODE_NOT_ISSUED` | 409 | 交换码尚未生成 | 提议非 `ACCEPTED` | FR-409 | 等待对方接受 |
| `SWAP_CODE_INVALID` | 422 | 交换码错误 | 输入的码与记录不匹配 | FR-409 | 允许重新输入（不清空历史） |
| `SWAP_CODE_ALREADY_VERIFIED` | **409** | **交换码已核销（一次性）** | **E-12**；`details.verifiedAt` | **R-26 / C-05** | 提示已失效；进入双方确认流程 |
| `SWAP_CODE_NOT_VERIFIED` | 409 | 未核销交换码不可确认 | R-27 前置条件 | FR-410 / R-27 | 先核销交换码 |
| `SWAP_CONFIRM_PARTIAL` | 200 | 已记录单方确认（非错误） | 仅一方确认，`processed=1/2` | R-27 | 正常返回；展示「等待对方确认」 |

### 3.10 评价（`REVIEW_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `REVIEW_TRANSACTION_NOT_COMPLETED` | **409** | 交易未完成不可评价 | **E-15**；`details.currentStatus` | **FR-312 / R-14** | 未完成时**不显示**评价入口 |
| `REVIEW_WINDOW_CLOSED` | 409 | 互评窗口已关闭 | 超过 7 天（A-07） | FR-312 / R-14 | 隐藏评价入口，展示「评价期已结束」 |
| `REVIEW_ALREADY_SUBMITTED` | 409 | 该方已评价过 | IDEM-B（每方一次） | R-14 | 展示已提交的评价（只读）；**不支持修改** |
| `REVIEW_RATING_OUT_OF_RANGE` | 400 | 评分越界 | `rating` ∉ [1,5] | FR-312 | 校验输入 |

### 3.11 举报（`REPORT_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `REPORT_NOT_FOUND` | 404 | 举报记录不存在 | 查询不存在举报 | FR-211 | 返回列表 |
| `REPORT_ALREADY_EXISTS` | **200** | **该商品已有未处理举报（幂等）** | **E-14 / C-06**；返回既有记录 | **R-17 / C-06** | 提示「你已举报过该商品」；**视为成功**，不报错 |
| `REPORT_OWN_ITEM_NOT_ALLOWED` | 422 | 不可举报自己的商品 | — | FR-211 | 隐藏自家商品的举报入口 |
| `REPORT_ALREADY_HANDLED` | 409 | 举报已处理 | 重复处理 | permission-matrix 6.9 | 刷新为已处理视图 |

### 3.12 治理与申诉（`GOVERNANCE_` / `APPEAL_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `GOVERNANCE_ACTIVE_PENALTY_EXISTS` | **409** | **该用户已有生效中封禁** | **E-13**；Q-10 假设拒绝重复封禁；`details.activePenaltyId`/`restrictedUntil` | **R-30 / E-13** | 禁用封禁按钮，展示现有处罚与到期时间 |
| `GOVERNANCE_CANNOT_PUNISH_ADMIN` | 403 | 不可处罚管理员 | 目标 `role=ADMIN` | R-30 | 隐藏封禁入口 |
| `GOVERNANCE_PENALTY_ALREADY_RESOLVED` | 409 | 处罚已解封/已过期 | 重复解封；与自动解封并发 | R-31 | 刷新处罚状态 |
| `GOVERNANCE_PENALTY_NOT_FOUND` | 404 | 处罚记录不存在 | 无效 `penaltyId` | FR-504 | 返回列表 |
| `GOVERNANCE_REASON_REQUIRED` | 400 | 治理动作缺原因 | 等同 `VALIDATION_REASON_REQUIRED`（治理域别名） | R-34 | 同 `VALIDATION_REASON_REQUIRED` |
| `APPEAL_ALREADY_EXISTS` | **409** | **该处罚已有申诉** | **AP-01**：一个处罚最多一条申诉 | **R-32 / AP-01** | 跳转已有申诉详情 |
| `APPEAL_PENALTY_NOT_ACTIVE` | 409 | 处罚非生效中，不可申诉 | 已解封/已过期（Q-17 假设不允许） | R-32 | 隐藏申诉入口 |
| `APPEAL_ALREADY_RESOLVED` | 409 | 申诉已处理 | 重复处理申诉 | AP-06 | 刷新为已处理视图 |
| `APPEAL_NOT_FOUND` | 404 | 申诉不存在 | 无效 `appealId` | FR-505 | 返回列表 |

### 3.13 文件（`FILE_`）

| code | HTTP | 含义 | 触发场景 | 关联 FR/R | 前端建议处理 |
| --- | --- | --- | --- | --- | --- |
| `FILE_TOO_LARGE` | **422** | 文件超过 5MB | **A-05**；`details.maxBytes/actualBytes` | FR-205 / A-05 | 提示单张 ≤5MB，引导压缩 |
| `FILE_TYPE_NOT_ALLOWED` | **422** | 文件类型不允许 | 非 JPG/PNG/WebP（A-05） | FR-205 / A-05 | 提示支持格式 |
| `FILE_EMPTY` | 400 | 上传空文件 | 空 `file` 字段 | FR-205 | 重新选择文件 |
| `FILE_COUNT_EXCEEDED` | 422 | 图片数量超限 | 商品 >9 张（A-05） | FR-205 / A-05 | 提示最多 9 张 |

---

## 4. 关键规则对应的错误码（R-16 / R-17 / R-18 / R-23 / R-24 / R-35）

| 规则 | 语义 | 对应错误码 | 说明 |
| --- | --- | --- | --- |
| **R-16** | 下单即占用库存 | `ITEM_ALREADY_RESERVED`、`ITEM_RESERVED_BY_SWAP`、`ITEM_NOT_PURCHASABLE` | 占用冲突在**下单时**即暴露（不是付款时） |
| **R-17** | 幂等键 | `COMMON_IDEMPOTENCY_KEY_REUSED`；幂等命中**无错误码**（回放首次结果） | 同键同请求 → 200/201（E-06）；同键异请求 → 409（E-16） |
| **R-18** | 条件更新/乐观锁 | `COMMON_VERSION_CONFLICT`；状态冲突用各域 `*_STATUS_*` / `*_ALREADY_*` | 响应 `details.currentStatus`/`currentVersion` 供前端刷新 |
| **R-23** | 订单占用与交换占用互斥 | `SWAP_ITEM_OCCUPIED`（被交换占）、`ITEM_ALREADY_RESERVED`（被订单占） | 同一商品同时最多一个进行中占用（C-01） |
| **R-24** | 整笔作废 | `SWAP_PROPOSAL_VOIDED`（`terminationReason=VOID_ITEM_*`） | 任一件被占/下架 → **整笔**作废并释放全部占用 |
| **R-35** | 违规下架联动 | 下架接口返回 200 + `affectedOrders`/`voidedSwapProposals`；受影响方的后续操作遇到 `SWAP_PROPOSAL_VOIDED` / `ORDER_ALREADY_CANCELLED` | 联动取消订单（`cancel_reason=ADMIN_TAKEDOWN`）或作废交换 |

---

## 5. 前端处理约定（统一要求）

| # | 约定 |
| --- | --- |
| 1 | **必须依据 `code` 分支，不得匹配 `message` 文本**（文案会优化） |
| 2 | 401（`AUTH_*`）→ 清空本地令牌并跳登录，记住来源页；**唯一例外**：`AUTH_INVALID_CREDENTIALS` 属登录表单错误，**不清令牌** |
| 3 | 403 → 提示无权限并返回上一页；`USER_RESTRICTED_CANNOT_WRITE` 需展示 `restrictedUntil` 与申诉入口 |
| 4 | 409 且 `details.currentStatus` 存在 → **刷新数据后重试一次**，不无限重试 |
| 5 | 409 `COMMON_VERSION_CONFLICT` → 重新拉取详情，提示「数据已更新，请确认后重试」 |
| 6 | 409 `COMMON_IDEMPOTENCY_KEY_REUSED` → **生成新的幂等键**后重试 |
| 7 | 422 → 属业务规则，**不要自动重试**，展示原因引导用户修正 |
| 8 | 幂等命中（E-06）返回 200/201 **不是错误**，按成功处理并跳转对应资源 |
| 9 | `REPORT_ALREADY_EXISTS` 返回 200，按**成功**处理（幂等） |
| 10 | 500/503 → 展示通用错误 + `traceId`，提供「重试」与「复制错误码」操作 |
| 11 | 所有错误提示应同时保留 `traceId`（便于用户报障时定位） |

---

## 6. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 接口契约负责人 | 初稿：命名规范、HTTP 语义分区、E-01~E-17 全量映射、13 个域共 70+ 错误码、R-16/R-17/R-18/R-23/R-24/R-35 规则对应、前端处理约定 | **冻结候选** |
