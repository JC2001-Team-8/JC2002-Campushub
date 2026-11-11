# 细节定义与协议（补充版）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 细节定义与协议（补充版） |
| 版本 | **v1.0** |
| 日期 | 2026-11-11 |
| 读者 | 后端、前端、测试、接口契约负责人 |
| 定位 | 补齐现有摘要中「说了但没定义清楚」的细节；**只补细节，不改计数、不改归属、不改责任** |
| 冲突处理 | 与契约或 `ITERATION-PLAN` 冲突时，以契约与 `ITERATION-PLAN` 为准 |
| 关系 | 规则见 [`COLLABORATION-RULES.md`](./COLLABORATION-RULES.md)；执行见 [`EXECUTION-CHECKLIST.md`](./EXECUTION-CHECKLIST.md) |

> 本文件不裁决通知事件总线、不裁决 A-6~A-9、不新增错误码、不修改契约与迁移。

---

## 1. 契约测试 6 个维度定义

> **说明**：本定义为**建议**，需接口契约负责人确认后作为验收口径；不改变 `openapi.yaml` 的事实。

| # | 维度 | 比对内容 | 通过标准 | 失败级别示例 |
| --- | --- | --- | --- | --- |
| 1 | 路径与方法 | `paths` 的路径与方法集合 | 运行时 `/v3/api-docs` 的 83 个 operation 与契约**集合相等**；手工端点已 `@Hidden` | 缺路径 / 多路径 → BLOCKER |
| 2 | 请求 DTO | 字段名、类型、`required`、数组边界 | 逐字段一致；必填缺失或类型不符即失败 | 字段名不同 → MAJOR |
| 3 | 响应 DTO | 字段名、类型、`required`、可空性 | 逐字段一致；金额为十进制字符串、时间为 ISO 8601 UTC | 字段名不同 → MAJOR |
| 4 | 枚举取值 | 枚举集合与大小写 | 取值集合完全一致，不多不少 | 多值/少值 → MAJOR |
| 5 | 权限声明 | `security` / `x-required-roles` | 与契约声明一致；统计应为公开 10 / 需登录 55 / 仅管理员 14 / 系统内部 4 | 声明不符 → BLOCKER |
| 6 | 幂等声明 | 是否必填或可选 `Idempotency-Key` | 16 个必填、16 个可选的归属与契约一致 | 必填当可选 → BLOCKER |

**工具建议**：Dredd 或 Schemathesis 二者择一；契约测试只比 `schema`，不比 `example`（`example` 属弱冻结项）。

---

## 2. 通知事件总线机制（不裁决，只列候选）

### 2.1 已确定的事实

| 项 | 事实 |
| --- | --- |
| 表归属 | `notifications` 归切片 4（E-10） |
| 切片 3 职责 | 只**发布事件**，不写 `notifications` 表；事件必须幂等，重复执行不重复发通知 |
| 切片 4 职责 | 实现通知写入与 `NotificationService` 的三个查询/已读接口 |
| 事件范围 | 下单、付款、取消/超时释放、取货点确认、收到交换提议、被拒绝、反提案、交换谈成、被处罚、自动解封、收到新评价 |

### 2.2 候选机制

| 候选 | 做法 | 优点 | 约束/风险 | 是否需契约或迁移变更 |
| --- | --- | --- | --- | --- |
| A | Spring `ApplicationEventPublisher`（进程内事件） | 实现简单，不新增表；切片 3 只发事件，切片 4 监听写入 | 仅进程内；多实例部署时各实例只处理自身事件；事件丢失无补偿 | 否 |
| B | 数据库事件表（切片 3 写事件行，切片 4 消费） | 跨进程、可靠、可重放 | 需要新增表或复用现有表；跨片写表需登记例外；涉及迁移与契约边界 | **可能需 `V5+` 与 `api-freeze.md` §4 评估** |
| C | 切片 3 直接调用切片 4 的 `NotificationService` | 调用链直观 | 切片 3 闭合时切片 4 尚未开工，接口实现不存在；会形成反向依赖 | 否，但**时序不可行** |

### 2.3 结论状态

**不裁决。** 由**切片 3 与切片 4 负责人共同裁决**，并在裁决后补充到交接材料；若选择 B，必须先走 `api-freeze.md` §4 与迁移审批，不得先建表后补流程。

---

## 3. 切片 1 幂等验证方式

| 项 | 内容 |
| --- | --- |
| 事实 | `updateMyProfile` **不是**必填幂等接口，也**不在**可选幂等接口清单中（`idempotency-and-concurrency.md` 未列） |
| 建议 | 切片 1 用**单元测试或切面测试**验证幂等切面通路（抢占 → 处理 → 写回 → 回放；同键异体 409），**不用 HTTP 契约验收**把 `updateMyProfile` 当幂等接口 |
| 后续 | 切片 2 的 `createItem` 是必填幂等接口，届时用**真实 HTTP** 验证同键重放与同键异体 |
| 依据 | [`../docs/iteration1/design/idempotency-and-concurrency.md`](../docs/iteration1/design/idempotency-and-concurrency.md) 第 3/4 章 |

---

## 4. 验收项错误码唯一化清单

> 原则：同一场景只写**一个**错误码与一个 HTTP 状态；以 `error-codes.md` 为准。
> 「旧写法」指早期路线材料中出现的「400/409」「400/403」「A/B」等不确定写法；本表只做唯一化，不新增码。

| # | 场景 | 旧写法（若出现） | **唯一值** | 依据 |
| --- | --- | --- | --- | --- |
| 1 | 下单/加购遇到不可购买商品（已售出/已下架/违规下架） | 400 / 409 | **422 `ITEM_NOT_PURCHASABLE`** | `error-codes.md` |
| 2 | 购物车条目不可结算 | — | **422 `CART_ITEM_NOT_PURCHASABLE`** | 同上 |
| 3 | 跨区域下单或发起交换 | 400 / 409 | **422 `TRADE_ZONE_MISMATCH`**（E-17） | 同上 |
| 4 | 商品区域不在卖家已勾选区域 | 400 `TRADE_ZONE_NOT_SELECTED` | **422 `ITEM_TRADE_ZONE_NOT_SELECTED`** | 同上 |
| 5 | 买家未勾选该交易区域 | — | **422 `TRADE_ZONE_NOT_SELECTED`** | 同上 |
| 6 | 用户尚未勾选任何常用区域 | 400 `USER_TRADE_ZONE_REQUIRED` | **422 `USER_TRADE_ZONE_REQUIRED`** | 同上 |
| 7 | 取货点不属于订单区域 | 400 | **422 `PICKUP_POINT_ZONE_MISMATCH`** | 同上 |
| 8 | 管理员不可处罚管理员 | 400 / 403 | **403 `GOVERNANCE_CANNOT_PUNISH_ADMIN`** | 同上 |
| 9 | 商品已被订单占用 | 409 对应错误码 | **409 `ITEM_ALREADY_RESERVED`** | 同上 |
| 10 | 商品已被交换占用（订单侧） | 409 对应错误码 | **409 `ITEM_RESERVED_BY_SWAP`** | 同上 |
| 11 | 商品已被交换占用（交换侧） | 409 `SWAP_ITEM_OCCUPIED` / `ITEM_ALREADY_RESERVED` | **409 `SWAP_ITEM_OCCUPIED`**（发起/接受交换时） | 同上 |
| 12 | 交换提议部分接受 | — | **422 `SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED`** | 同上 |
| 13 | 重复接受交换提议 | 409 | **409 `SWAP_PROPOSAL_ALREADY_ACCEPTED`** | 同上 |
| 14 | 交换码未签发 | — | **409 `SWAP_CODE_NOT_ISSUED`** | 同上 |
| 15 | 交换码重复核销 | 409 | **409 `SWAP_CODE_ALREADY_VERIFIED`** | 同上 |
| 16 | 未核销就确认交换 | — | **409 `SWAP_CODE_NOT_VERIFIED`** | 同上 |
| 17 | 目标商品不接受交换 | — | **422 `SWAP_TARGET_NOT_ACCEPTING`** | 同上 |
| 18 | 对自己发起交换 | — | **422 `SWAP_SELF_PROPOSAL_NOT_ALLOWED`** | 同上 |
| 19 | 占用态/终态改为接受交换 | 409 | **409 `ITEM_STATUS_NOT_SWAPPABLE`** | 同上 |
| 20 | 占用态修改关键字段 / 下架 | 409 | **409 `ITEM_LOCKED_BY_OCCUPATION`** | 同上 |
| 21 | 终态商品编辑 | 409 | **409 `ITEM_STATUS_NOT_EDITABLE`** | 同上 |
| 22 | 文件超过 5 MB | 413 / 422 | **422 `FILE_TOO_LARGE`**（A-10 已裁决） | 同上 |
| 23 | 文件类型不允许 | — | **422 `FILE_TYPE_NOT_ALLOWED`** | 同上 |
| 24 | 上传空文件 | 400 | **400 `FILE_EMPTY`** | 同上 |
| 25 | 治理动作缺原因 | 400（别名混用） | **400 `VALIDATION_REASON_REQUIRED`**（`GOVERNANCE_REASON_REQUIRED` 为已登记别名，实现统一用前者） | 同上 |
| 26 | 重复封禁生效中用户 | 409 | **409 `GOVERNANCE_ACTIVE_PENALTY_EXISTS`** | 同上 |
| 27 | 重复举报同一商品 | 409 / 200 | **200 `REPORT_ALREADY_EXISTS`**（按成功处理，返回既有记录） | 同上 |
| 28 | 举报自己的商品 | — | **422 `REPORT_OWN_ITEM_NOT_ALLOWED`** | 同上 |
| 29 | 重复处理举报 | 409 | **409 `REPORT_ALREADY_HANDLED`** | 同上 |
| 30 | 未完成交易就评价 | 409 | **409 `REVIEW_TRANSACTION_NOT_COMPLETED`** | 同上 |
| 31 | 评价窗口已关闭 | 409 | **409 `REVIEW_WINDOW_CLOSED`** | 同上 |
| 32 | 重复评价 | 409 | **409 `REVIEW_ALREADY_SUBMITTED`** | 同上 |
| 33 | 评分越界 | 400 | **400 `REVIEW_RATING_OUT_OF_RANGE`** | 同上 |
| 34 | 一个处罚重复申诉 | 409 | **409 `APPEAL_ALREADY_EXISTS`** | 同上 |
| 35 | 处罚非生效中仍申诉 | 409 | **409 `APPEAL_PENALTY_NOT_ACTIVE`** | 同上 |
| 36 | 重复处理申诉 | 409 | **409 `APPEAL_ALREADY_RESOLVED`** | 同上 |
| 37 | 用户名/邮箱重名 | 409 | **409 `AUTH_USERNAME_TAKEN`** | 同上 |
| 38 | 未登录 / 令牌失效 | 401 | **401 `AUTH_UNAUTHENTICATED`** | 同上 |
| 39 | 他人资源 | 404 或 403 | **403 `AUTH_NOT_RESOURCE_OWNER`**（不用 404） | 同上 |
| 40 | 非管理员调用治理接口 | 403 | **403 `AUTH_ADMIN_REQUIRED`** | 同上 |
| 41 | 被处罚用户执行交易写操作 | 403 | **403 `USER_RESTRICTED_CANNOT_WRITE`**（含 `restrictedUntil`） | 同上 |
| 42 | 幂等键复用但请求体不同 | 409 | **409 `COMMON_IDEMPOTENCY_KEY_REUSED`** | 同上 |
| 43 | 乐观锁版本冲突 | 409 | **409 `COMMON_VERSION_CONFLICT`** | 同上 |
| 44 | 排序字段不在白名单 | 400 | **400 `COMMON_INVALID_SORT_FIELD`** | 同上 |

> 本表未覆盖的错误码以 `error-codes.md` 的 93 个为准；**表中没有的码不得自造**。
> 若实现中发现 `error-codes.md` 未明确的场景，标「待接口契约负责人确认」，不自行发明。

---

## 5. Q-04 管理员账号来源

| 项 | 内容 |
| --- | --- |
| 事实 | `V3__seed_development_data.sql` 创建 `admin@example.com`（占位密码）；生产环境禁止执行 `V3` |
| 影响 | 管理员账号来源未确认会影响治理接口的初始化与鉴权方案 |
| 建议 | **切片 4 开工前**由后端负责人确认：预置账号 / 数据库直插 / 注册后提权 / 配置文件 四选一 |
| 约束 | 不修改 `V3__seed_development_data.sql`；如确需调整初始化方式，按迁移规则新增 `V5+` 并走审批 |
| 状态 | **待确认**（不写成已确认） |

---

## 6. `FILE_TOO_LARGE` 统一 422

已由 **A-10** 裁决：以 `error-codes.md` 与 `openapi.yaml` 为准，统一 **422**。
本文件只登记，不再展开；实现与测试不得写 413。

---

## 7. 其他待确认项

| 编号 | 事项 | 事实 | 影响 | 依据 |
| --- | --- | --- | --- | --- |
| Q-01 | 最终服务哪 2~3 个校区及周边社区（确切名称） | 占位 `CAMPUS_A` 等 | 阻塞区域数据预置；只替换数据值，不改契约 | [`../docs/iteration1/glossary-assumptions-questions.md`](../docs/iteration1/glossary-assumptions-questions.md) |
| Q-02 | 预设交易区域清单与编码规则 | 占位编码 | 阻塞区域字典与校验 | 同上 |
| Q-03 | 公共取货点清单与命名 | 占位 `PICKUP_LIBRARY_A` 等 | 阻塞取货点字典与选点流程 | 同上 |
| Q-33 | PF-23~PF-26（通知列表、举报页、我的申诉、搜索结果页）是否纳入页面基线 | 暂列可选项，不计入 22 个基线页面 | `/notifications` 是否排期 | 同上 |
| Q-34 | 搜索是否作为独立页面；搜索维度 | 假设关键词 + 分类 + 区域，可内嵌首页 | `/search` 是否排期 | 同上 |

**替换占位数据**属 `api-freeze.md` 第 6 章**类型 G**，不属契约变更；但若涉及已应用的 `V2`，应新增 `V5+` 更新数据，不直接改 `V2`。

---

## 8. 变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-11-11 | 交接包补充代理 | 首版：契约测试 6 维度定义（建议，需确认）、通知事件总线 3 个候选（不裁决）、切片 1 幂等验证方式、44 行错误码唯一化清单、Q-04 管理员账号来源、`FILE_TOO_LARGE` 422 登记、Q-01~Q-03/Q-33/Q-34 待确认项 | 生效 |
