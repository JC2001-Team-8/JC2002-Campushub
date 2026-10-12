# 迭代一 领域模型与数据库设计 说明（Design README）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 领域模型与数据库设计 说明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 数据库与领域模型设计负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 目标库 | PostgreSQL 15 |
| 技术栈 | Java 11 + Spring Boot 2.7.18 + Flyway + PostgreSQL 15 |
| 上游依据 | `docs/iteration1/`（需求基线与范围冻结文档包） |

---

## 1. 本目录包含什么

| 文件 | 内容 | 读者 |
| --- | --- | --- |
| `README.md`（本文件） | 需求→表映射、设计决策索引、未覆盖项说明、如何使用本设计 | 所有人 |
| `er-diagram.md` | Mermaid ER 图：总览图 + 占用/交换/治理三张详图 + 基数汇总 | 后端、数据库、评审 |
| `schema.md` | **25 张表的完整字段字典**（324 列，含类型/约束/默认值/说明），由迁移后数据库导出 | 后端、接口契约设计 |
| `db-verification.md` | 迁移验证记录：实测输出、19 项负向测试、4 个已修缺陷 | 测试、评审 |
| `open-questions.md` | 25 个待确认问题（DQ）+ 19 项设计决策（DD）+ 有意不做清单 | 需求方、评审 |
| `SystemDesign.md` | **统一系统设计文档**（第 6 步产出） | 所有人 |
| `sequence-diagrams.md` | 关键流程时序图（第 6 步产出） | 后端、测试 |

### 1.0 接口契约（第 3 步产出，**冻结候选**）

| 文件 | 内容 | 读者 |
| --- | --- | --- |
| `openapi.yaml` | **OpenAPI 3.0.3 规范**：83 个操作、13 模块、99 个 Schema，已通过 `openapi-spec-validator` 校验 | 后端、前端、测试 |
| `api-contract.md` | 接口清单：逐条 endpoint、**54/54 FR 映射**、不做的接口清单、全局约定 | 所有人 |
| `api-permission-mapping.md` | 7 主体 × 6 域权限映射、本人/管理员/系统边界、越权测试要点 | 后端、测试 |
| `error-codes.md` | 统一错误码表：93 个 `DOMAIN_REASON` 错误码，覆盖 E-01~E-17 与 R-16/R-17/R-18/R-23/R-24/R-35 | 前后端 |
| `idempotency-and-concurrency.md` | 幂等键与条件更新双层防护、32 个幂等接口、并发场景逐一说明 | 后端、测试 |
| `api-freeze.md` | **冻结声明**、10/9 后不新增接口约束、变更流程与台账、P0/DQ 假设汇总 | 所有人 |
| `api-open-questions.md` | 18 条 AQ 待确认（4 条 P0）+ 20 项契约设计决策（AD）+ 假设落点 | 需求方、评审 |

### 1.0.0 系统设计文档与时序图（第 6 步产出，**设计内容统一入口**）

| 文件 | 内容 | 读者 |
| --- | --- | --- |
| [`SystemDesign.md`](./SystemDesign.md) | **迭代一系统设计文档**（10 个一级章节 / **10 个 Mermaid 图**：4 flowchart + 1 erDiagram + 5 stateDiagram）：整合架构（分层 + 部署拓扑 + 选型理由）、领域模型（ER + 实体 + 占用模型）、状态机与规则（6 状态机 + R-16~R-35 + 幂等并发）、权限设计（7 角色 + 矩阵摘要 + P-00-A/B/C）、接口清单（13 模块 83 操作 + 54 FR + 冻结声明 + 93 错误码）、不做清单 18 项、假设与待确认问题、附录（文件索引 + 术语表 + 设计要点速查） | 所有人（评审首选） |
| [`sequence-diagrams.md`](./sequence-diagrams.md) | 关键流程时序图（**9 个 `sequenceDiagram`**，覆盖 **7 组流程**：购买主线、交换主线、整笔作废、治理主线、并发抢购、超时释放、§7 定时任务）：每图前置「参与角色 / 覆盖 operationId / 状态转换 / 关键规则 / 错误码」五项说明；含 §8 交叉索引、§9 渲染说明、待确认清单 | 后端、测试、评审 |
| [`design-doc-verification.md`](./design-doc-verification.md) | 本步验证记录：图表统计、交叉引用一致性检查（表名 / 状态枚举 / 错误码 / operationId / FR 五项）、上游矛盾登记（DDP）、未覆盖项、验收自检 | 评审 |
| [`final-delivery-report.md`](./final-delivery-report.md) | **迭代一最终交付报告**（第 7 步产出）：交付总览与规模统计、7 步验收结果、DDP-01 与 P-01 修复证明、未覆盖项与迭代二计划、**12 项最终一致性检查**、打包输出、10 项验收自检 | 所有人（交付验收首选） |
| [`independent-verification.md`](./independent-verification.md) | **独立验证报告**（由未参与交付的验证人编撰）：对 DDP-01 / P-01 / 接口对齐 / 迁移 / 运行时端点 / 一致性 / 打包 / 无新代码 / 文档自述数值共 9 组主张的**对抗式复核**。结论：**0 项被推翻**，2 项保留（均为非交付缺陷，见报告 §0.3） | 评审、验收方 |

> **建议阅读顺序**：需求基线 → 数据库设计 → 接口契约 → **SystemDesign.md** → **sequence-diagrams.md**。
> 详见 `SystemDesign.md` §0.1。

### 1.0.1 接口骨架（第 5 步产出，按契约生成）

| 文件 | 内容 | 读者 |
| --- | --- | --- |
| `controller-skeleton-verification.md` | 接口骨架验证记录：**83/83 operationId 对账**、权限与幂等核对、DTO 一致性、编译/启动/Swagger 验证、9 个已修缺陷、未实现清单 | 后端、测试、评审 |
| `../../../../src/main/java/com/jc2001/campustrade/controller/` | 12 个 Controller（83 个 operationId）+ 2 个手工自检 Controller | 后端 |
| `../../../../src/main/java/com/jc2001/campustrade/dto/` | 99 个 DTO（request 33 / response 66，含 22 个枚举） | 后端 |
| `../../../../tools/generate_api_skeleton.py` | 一次性生成器（只读契约；可重复再生成，保证 1:1） | 后端 |

> 关键结论：运行时 `/v3/api-docs` 的 operationId 与 `openapi.yaml` **完全 1:1**
> （缺失 0 / 多余 0 / 方法或路径不一致 0）；Swagger UI 默认展示这 83 个操作。
> 所有方法体抛 `UnsupportedOperationException`，**业务逻辑未实现**（下一步）。

> **交付冻结**：本迭代交付物自 **2026-10-12 23:12:32 CST** 起冻结，校验码见 [`../FREEZE.md`](../FREEZE.md)。

### 1.1 关联的实现文件（本步骤产出）

| 文件 | 作用 |
| --- | --- |
| `docker-compose.yml` | 本地 PostgreSQL 15（**仅数据库**，用于迁移验证） |
| `README-db.md` | 如何启动、迁移、连接查询 |
| `db/verify_schema.sql` | 25 项只读验证查询（可重复执行） |
| `src/main/resources/db/migration/V1__init_schema.sql` | 全部表、枚举、约束、索引、注释（+324 条列注释） |
| `src/main/resources/db/migration/V2__seed_reference_data.sql` | 预设字典：区域、取货点、分类、成色（含占位 TODO） |
| `src/main/resources/db/migration/V3__seed_development_data.sql` | 开发种子：管理员、示例用户/商品/订单/交换/处罚（含占位 TODO） |
| `src/main/resources/db/migration/V4__fix_review_window_index.sql` | 修正 FR-604 评价窗口扫描的部分索引条件（排除终态 `BOTH_REVIEWED`，DQ-27） |

---

## 2. 需求 → 表 映射（可追溯性）

### 2.1 按功能需求（FR）映射

| 需求编号段 | 需求域 | 主要表 | 关键约束/规则 |
| --- | --- | --- | --- |
| FR-101 ~ FR-104 | 注册、登录、登录态、退出 | `users` | `uq_users_username` / `uq_users_email`（citext 大小写不敏感）；R-01 令牌有效期由应用层 |
| FR-105 ~ FR-106 | 昵称、头像 | `users` | `ck_users_nickname_len`（1–20，A-04/A-09）；头像为单列 `avatar_url` |
| FR-107 | 勾选常用交易区域 | `user_trade_zones` + `trade_zones` | `uq_user_trade_zones`；R-15 区域只能来自预设字典 |
| FR-108 | 个人主页 | `users` + `items`（聚合查询） | 无联系方式字段（刻意不建） |
| FR-201 ~ FR-204 | 发布/编辑/下架/重新上架商品 | `items` | `ck_items_published_when_on_sale`；`ck_items_banned_trace`（违规下架必留痕） |
| FR-205 | 图片 1–9 张 | `item_images` | `uq_item_images_sort`；`ck_item_images_size`（≤5MB） |
| FR-206 / FR-207 | 分类 / 成色 | `categories` / `item_conditions` | 预设字典（Q-21：仅管理员维护） |
| FR-208 | 估价 | `items.estimated_price` | `ck_items_price_nonneg`；R-13 仅展示，另见 `*_snapshot` |
| FR-209 | 交易区域 | `items.trade_zone_id` | R-15：必须 ∈ 卖家已勾选区域（对账查询 §13） |
| FR-210 | 交换状态展示 | `items.swap_preference` | `swap_preference` 枚举（ACCEPTING/NOT_ACCEPTING） |
| FR-211 | 举报 | `reports` | `uq_reports_pending_reporter_item`（C-06） |
| FR-301 ~ FR-302 | 购物车 / 按卖家分组 | `cart_items` | `uq_cart_items_user_item`（R-04）；分组为查询行为 |
| FR-303 | 提交订单 | `orders` + `order_items` | **C-02 一单一卖家**（`orders.seller_id`）；`uq_order_items_order_item` |
| FR-304 | 库存预留 | **`item_occupations`** + `items.status` | **R-16**：下单即占用；`uq_item_occ_active_item` |
| FR-305 | 付款倒计时 | `orders.expires_at` | **R-07 / A-01**（30 分钟）；`idx_orders_status_expires` 支撑扫描 |
| FR-306 | 模拟付款 | `payments` | **R-08 / OOS-01**；`uq_payments_order_succeeded`（一单一成功记录） |
| FR-307 | 超时取消 | `orders.status` + `cancel_reason` | **R-07**：`PAYMENT_TIMEOUT`；释放占用 → 商品回 `IN_SALE` |
| FR-308 | 主动取消 | `orders.cancel_reason` | `BUYER_CANCELLED`（Q-08：已付款不可自助取消） |
| FR-309 | 选择公共取货点 | `orders.pickup_point_id` | **R-06**；`ck_orders_pickup_pair`；同区域校验见对账 §13 |
| FR-310 | 卖家交付 | `orders.delivered_at` / `DELIVERED` | `ck_orders_delivered` |
| FR-311 | 确认收货 | `orders.completed_at` / `COMPLETED` | `ck_orders_completed`；商品转 `SOLD` |
| FR-312 | 互评前置条件 | `orders.review_status` + `reviews` | **R-14**；`orders.review_deadline`（A-07 7 天）；唯一索引限每方一次 |
| FR-401 | 设置交换状态 | `items.swap_preference` | R-20 |
| FR-402 | 创建交换包（1–5 件） | `swap_bundles` + `swap_bundle_items` | **SB-01/SB-02/SB-03**；`uq_swap_bundle_items_active` |
| FR-403 | 交换包估价合计 | `swap_bundles.estimated_total` | R-13 仅展示；**OOS-03 无补差价字段** |
| FR-404 | 发起交换 | `swap_proposals` + `swap_proposal_items` | **C-04**；`ck_swap_proposals_parties`（不可自我交换）；R-15 同区域 |
| FR-405 | 整包接受 | `swap_proposals.status='ACCEPTED'` | **R-22 无部分接受**；`ck_swap_proposals_accepted_trace` |
| FR-406 | 拒绝 | `status='REJECTED'` + `termination_reason` | `ck_swap_proposals_termination_trace` |
| FR-407 | 反提案 | `parent_proposal_id` + `counter_depth` | **R-28 仅一轮**：`ck_swap_proposals_counter_depth` |
| FR-408 | 72 小时过期 | `swap_proposals.expires_at` | **R-25 / A-02**；部分索引支撑扫描 |
| FR-409 | 交换码 | `swap_codes` | **R-26 / C-05**：`uq_swap_codes_proposal` + `uq_swap_codes_code` + `ck_swap_codes_verified_trace` |
| FR-410 | 双方确认 | `proposer_confirmed_at` / `target_confirmed_at` | **R-27**：`ck_swap_proposals_completed_trace` |
| FR-411 | 同时占用 | `item_occupations` | **R-23**：`uq_item_occ_active_item` 统一互斥 |
| FR-412 | 整笔作废 | `status='CANCELLED'` + `termination_reason` | **R-24**：`VOID_ITEM_*` 枚举 |
| FR-501 | 搜索用户 | `users` + 索引 | 仅管理员（权限层） |
| FR-502 | 下架违规商品 | `items.status='BANNED'` + `penalties` | `ck_items_banned_trace`；**R-35 联动** |
| FR-503 | 封禁三天 | `penalties` + `users.status` | **R-30 / A-03**；`uq_penalties_one_active`；`ck_users_restricted_until` |
| FR-504 | 查看处罚记录 | `penalties` | **R-32 永不删除** |
| FR-505 | 处理申诉 | `appeals` | `uq_appeals_penalty`（AP-01）；`ck_appeals_resolution_trace` |
| FR-506 | 自动解封 | `penalties.status='EXPIRED'` + `users.status` | **R-31**；`idx_penalties_active_ends` 支撑扫描 |
| FR-507 | 通知 | `notifications` | Q-05 待确认渠道 |
| FR-601 | 订单超时取消 | `orders` | 部分索引 + 应用层幂等 |
| FR-602 | 交换提议过期 | `swap_proposals` | 部分索引 + 应用层幂等 |
| FR-603 | 封禁到期解封 | `penalties` / `users` | 部分索引 + 条件更新 |
| FR-604 | 评价窗口关闭 | `orders.review_deadline` | 部分索引 |

### 2.2 按约束（C）与规则（R）映射

| 约束/规则 | 语义 | 数据库落地 | 是否数据库级强制 |
| --- | --- | --- | --- |
| **C-01** | 一物同时最多一个进行中占用 | `uq_item_occ_active_item` | ✅ **是** |
| **C-02** | 一单一卖家 | `orders.seller_id` 单列 | ⚠️ 跨表，应用层 + 对账 §13 |
| **C-03** | 交换包 1–5 件 | 无（聚合数量） | ⚠️ 应用层 + 对账 §13 |
| **C-04** | 提议目标商品 ≥1 且同属目标方 | 无（聚合归属） | ⚠️ 应用层 + 对账 §13 |
| **C-05** | 一提议最多一个交换码 | `uq_swap_codes_proposal` | ✅ **是** |
| **C-06** | 同一用户对同一商品最多 1 条未处理举报 | `uq_reports_pending_reporter_item` | ✅ **是** |
| **C-07** | 被处罚用户不得交易写入 | 无 | ⚠️ 应用层权限拦截（C-07 本身即会话级） |
| **R-16** | 下单即占用、释放幂等 | `item_occupations` + `released_at` + `version` | ✅ 部分（互斥强制，投影需应用层） |
| **R-17** | 幂等键 | `idempotency_records` + `uq_idempotency_user_key` | ✅ **是** |
| **R-18** | 条件更新/乐观锁 | 12 张表 `version` | ✅ 字段就位（用法在应用层） |
| **R-19** | 服务端时间 | `timestamptz` + `expires_at` 绝对时间 | ✅ 类型就位 |
| **R-22** | 整包接受、不可部分接受 | 无 item 级接受字段 | ✅ **结构上不可能** |
| **R-23** | 订单占用与交换占用互斥 | `uq_item_occ_active_item` | ✅ **是** |
| **R-24** | 整笔作废 | `termination_reason` 枚举 | ⚠️ 枚举就位，触发在应用层 |
| **R-25** | 72 小时过期 | `expires_at` + 部分索引 | ✅ 字段就位 |
| **R-26** | 交换码一次性 | `uq_swap_codes_code` + `ck_swap_codes_verified_trace` | ✅ 部分（一次性核销靠 `version` 条件更新） |
| **R-27** | 双方确认齐备才完成 | `ck_swap_proposals_completed_trace` | ✅ **是** |
| **R-28** | 反提案仅一轮 | `ck_swap_proposals_counter_depth`（0..1） | ✅ **是** |
| **R-30** | 封禁默认三天、同时最多一条生效 | `ck_penalties_period` + `uq_penalties_one_active` | ✅ 部分（72h 由应用层计算） |
| **R-31** | 到期自动解封幂等 | `version` + `idx_penalties_active_ends` | ⚠️ 应用层 + 字段就位 |
| **R-32** | 处罚/申诉记录永不删除 | 无 `deleted_at`，外键 `ON DELETE RESTRICT` | ✅ **是** |
| **R-33** | 处罚期间禁止交易写入 | 无 | ⚠️ 应用层拦截 |
| **R-34** | 治理动作 must 填原因并留痕 | `ck_penalties_reason_not_blank`、`ck_items_banned_trace`、`audit_logs` | ✅ 部分（`audit_logs.reason` 需应用层强制） |
| **R-35** | 违规下架联动取消/作废 | `cancel_reason='ADMIN_TAKEDOWN'` + `termination_reason='VOID_ITEM_BANNED'` | ⚠️ 枚举就位，联动在应用层 |

> **说明**：标 ⚠️ 的项**不是设计缺陷**，而是「跨表/跨聚合不变量」与「会话级权限」在关系数据库中的固有边界。
> 每一项都已提供**对账查询**（`db/verify_schema.sql` §11–§13）作为可重复执行的兜底检测。

### 2.3 按页面/流程映射（示例）

| 页面 | 主要表 |
| --- | --- |
| PF-01 首页/分类列表 | `items` + `categories` + `item_conditions` + `item_images` |
| PF-02 商品详情 | 同上 + `users`（卖家公开资料）+ `item_occupations`（占用态） |
| PF-06 编辑资料 | `users` + `user_trade_zones` + `trade_zones` |
| PF-07 发布商品 | `items` + `item_images` + 字典表 |
| PF-09/10 购物车与下单 | `cart_items` → `orders` + `order_items` + `item_occupations` |
| PF-11~13 订单/付款/取货点 | `orders` + `payments` + `pickup_points` |
| PF-14~18 交换全流程 | `swap_bundles` + `swap_bundle_items` + `swap_proposals` + `swap_proposal_items` + `swap_codes` |
| PF-19~22 管理后台 | `users` + `items` + `penalties` + `appeals` + `reports` + `audit_logs` |

---

## 3. 核心设计决策索引

> 完整清单见 `open-questions.md` §4（DD-01 ~ DD-19）。此处列出**最影响后续实现**的 8 条。

| 编号 | 决策 | 为什么 | 影响 |
| --- | --- | --- | --- |
| **DD-01** | 引入 `item_occupations` 作为占用真值台账，`items.status` 仅为投影 | C-01/R-23 需要数据库级排他保证；单靠 `items.status` 无法表达「被谁占用」 | 所有占用/释放必须**同事务**同时写台账与投影 |
| **DD-03** | 多态引用不建外键（`ref_id`、`notifications.ref_id`） | PostgreSQL 不支持多态外键 | 应用层保证引用有效；已有 `idx_item_occ_ref` 支撑查询 |
| **DD-04** | 幂等独立成表，键为 (`user_id`, `idempotency_key`) | R-17 需要回放首次响应 + E-16 需要请求体比对 | 实现建议 `INSERT ... ON CONFLICT` 抢占，再推进状态 |
| **DD-06** | 订单取消原因与交换终结原因独立枚举 | 状态机 O-03/O-04/O-11、S-03/S-05/S-06/S-07/S-13 是不同路径 | 接口契约需要 `reason` 参数 |
| **DD-09** | `swap_bundle_items.item_status` 区分进行中 | SB-03 只约束「进行中」，允许包释放后复用（DQ-13） | 部分唯一索引 `WHERE item_status='ACTIVE'` |
| **DD-11** | 互评状态放在 `orders.review_status` | 状态机 §8 把互评定义为**交易维度**而非单条评价 | 评价状态与 `reviews` 行数解耦 |
| **DD-16** | 12 张表加 `version` | R-18「乐观锁或条件更新」 | 所有状态转换必须 `WHERE version = ?` 或 `WHERE status = ?` |
| **DD-19** | `citext` 用于 username/email | 避免 `Alice`/`alice` 并存 | 依赖 `citext` 扩展（官方 PG15 镜像内置） |

---

## 4. 覆盖度自检：未覆盖的需求点与需后续补充的表

### 4.1 已覆盖（结论）

| 需求域 | 表 | 状态 |
| --- | --- | --- |
| 账户与个人资料 | `users`、`user_trade_zones` | ✅ 全覆盖（FR-101~FR-108） |
| 商品 | `items`、`item_images`、`categories`、`item_conditions` | ✅ 全覆盖（FR-201~FR-211） |
| 购买 | `cart_items`、`orders`、`order_items`、`payments`、`item_occupations`、`pickup_points` | ✅ 全覆盖（FR-301~FR-312） |
| 交换 | `items.swap_preference`、`swap_bundles`、`swap_bundle_items`、`swap_proposals`、`swap_proposal_items`、`swap_codes` | ✅ 全覆盖（FR-401~FR-412） |
| 管理员治理 | `penalties`、`appeals`、`reports`、`audit_logs`、`notifications` | ✅ 全覆盖（FR-501~FR-507） |
| 定时任务 | 复用 `orders` / `swap_proposals` / `penalties` / `orders.review_deadline` + 部分索引 | ✅ 全覆盖（FR-601~FR-604，无独立表，符合「任务即查询+条件更新」设计） |

### 4.2 未覆盖 / 有意留空的需求点

| # | 需求点 | 当前状态 | 处理 |
| --- | --- | --- | --- |
| 1 | Q-01/Q-02/Q-03 的真实校区/区域/取货点名称 | **占位数据**（`CAMPUS_A` 等 + 名称含「【待确认】」） | 确认后替换 `V2`，**不改表结构** |
| 2 | Q-04 管理员账号来源 | 假设种子创建 `admin@example.com`，密码占位 | 确认后调整；生产禁止执行 `V3` |
| 3 | Q-05 通知渠道（是否仅站内） | 仅建 `notifications`（站内） | 若需邮件/短信，需新增投递记录表（迭代二） |
| 4 | FR-108 个人主页「在售商品数」 | 由 `items` 聚合查询，**不建冗余计数字段** | 如性能需要，迭代二加物化视图 |
| 5 | FR-103 登录态保持（令牌） | **无表**：假设为无状态令牌（JWT 风格） | 若需服务端会话/刷新令牌，需新增 `user_tokens` 表（见 DQ/§4，属迭代二） |
| 6 | FR-104 退出登录使令牌失效 | 无表（无状态令牌无法服务端撤销） | **与 5 同源**；若要求「退出即失效」，必须新增令牌表 → 影响接口契约 |
| 7 | 密码重置 / 修改密码 | `users.password_hash` 就位，**无重置令牌表** | FR 未列「忘记密码」，故不建；若需要属新需求 |
| 8 | 图片文件的物理存储 | 仅存 `image_url` 字符串（OOS-18 本地存储） | 不做对象存储元数据表 |
| 9 | 软删除商品的回收站/恢复 | `items.deleted_at` 就位，**无回收站流程** | 无 FR 要求，暂不设计 |
| 10 | 用户注销（OOS-13） | 无表、无字段 | 明确不做 |

### 4.3 需要后续步骤补充的表（明确登记）

| # | 表 | 何时需要 | 触发条件 |
| --- | --- | --- | --- |
| 1 | `user_tokens`（或 `refresh_tokens`） | **可能需要**：若 Q-07 确认需要刷新令牌，或确认「退出登录必须服务端失效」 | DQ 见 §4.2 第 5/6 项 |
| 2 | `notification_deliveries`（外部渠道投递记录） | 迭代二 | 若 Q-05 确认需要邮件/短信 |
| 3 | `trade_zone_groups`（区域组） | 迭代 N | 若 OOS-07 的跨区交易被提上日程 |
| 4 | 支付流水 / 退款表 | 迭代 N | 接入真实支付（OOS-01 / OOS-14） |
| 5 | 交换链参与者表 | 迭代 N | 多人交换链（OOS-02） |

> **结论**：除上表所列**明确不在迭代一范围**的内容外，`docs/iteration1/` 需求基线中**全部实体与关系均已落到表结构**。

---

## 5. 如何使用本设计

### 5.1 数据库设计者 / 后端开发者

1. 先读 `er-diagram.md` §1 建立整体印象，再读 §2（占用）与 §3（交换）——这两处是最容易实现错的。
2. 查字段时用 `schema.md`；它有全部 324 列的约束与说明。
3. **实现状态转换前必读**：`../state-machines.md`（允许的起始状态与目标状态）+ `../rules.md` §5（R-17/R-18 幂等与并发）。
4. 写任何状态更新语句时，必须使用条件更新，例如：

   ```sql
   -- 正确（条件更新，R-18）
   UPDATE orders SET status='PAID', paid_at=now(), version=version+1
   WHERE id=? AND status='PENDING_PAYMENT' AND version=?;
   -- 受影响行数 = 0 → 并发冲突，返回 409 + 当前状态

   -- 禁止（先查后改，存在竞态窗口）
   SELECT status FROM orders WHERE id=?;   -- 再判断
   UPDATE orders SET status='PAID' WHERE id=?;
   ```

5. 占用/释放必须在同一事务内同时维护 `item_occupations` 与 `items.status`（DD-01）。

### 5.2 接口契约设计者

- 每个接口的**允许起始状态**取自 `../state-machines.md`，**目标状态**必须落在本文的 ENUM 取值内。
- 需要幂等的接口（`../rules.md` R-17 列出的 13 个）必须接受 `Idempotency-Key`，对应 `idempotency_records`。
- 治理类接口的 `reason` 为**必填**（`audit_logs.reason` / `penalties.reason`）。
- 错误场景（E-01~E-17）与数据库约束的对应关系见 `../rules.md` §9 与 `db-verification.md` §5。

### 5.3 评审者

- 直接看 `db-verification.md` §3（实测结果）与 §5（19 项负向测试）——约束是否真的生效有证据。
- 看 `open-questions.md` §3（DQ 问题）与 §4（DD 决策）——所有「我们替你做的选择」都在这里，便于否决。

---

## 6. 变更规则（重要）

| 规则 | 说明 |
| --- | --- |
| **禁止修改已执行的迁移** | `V1`~`V3` 已在验证库执行，Flyway 会校验 checksum；修改会导致 `validate` 失败 |
| **任何变更新增 `V4+`** | 例如新增列、新增枚举值（`ALTER TYPE ... ADD VALUE`）、新增索引 |
| **占位数据替换** | Q-01/Q-02/Q-03 确认后，**优先**通过新增 `V4__replace_trade_zones.sql` 之类迁移替换，而不是改 `V2` |
| **枚举新增值** | PostgreSQL 15 支持 `ALTER TYPE ... ADD VALUE`（不可与使用该值的事务同批）；若需删除/重命名枚举值，成本高，建议改用字典表 |
| **注释修改** | 直接用 `COMMENT ON COLUMN`，新增迁移即可 |

---

## 7. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 数据库与领域模型设计负责人 | 初稿：目录导览、FR/C/R/页面四维映射、8 条核心决策索引、覆盖度自检（含 5 张后续补充表）、使用指南与变更规则 | 草稿（待确认） |
| v1.1 | 2026-10-12 | 后端接口骨架负责人 | 新增 §1.0.1：第 5 步接口骨架产出（83 operationId / 99 DTO / 12 Service）与验证记录索引 | 草稿（待确认） |
| v1.2 | 2026-10-12 | 系统设计文档负责人 | 新增 §1.0.0：第 6 步「设计文档补齐」产出（SystemDesign.md / sequence-diagrams.md / design-doc-verification.md）与建议阅读顺序 | 草稿（待确认） |
| v1.3 | 2026-10-12 | 交付负责人 | 新增 `final-delivery-report.md`（迭代一最终交付报告）索引；DDP-01 修复、P-01 闭环 | **冻结候选** |
| v1.4 | 2026-10-12 | 交付负责人 | 新增 `independent-verification.md`（独立验证报告）索引 | **冻结候选** |
