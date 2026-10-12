# 迭代一 接口契约冻结声明（API Freeze）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 接口契约冻结声明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 接口契约负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 冻结对象 | [openapi.yaml](./openapi.yaml)（OpenAPI 3.0.3）+ 本目录 6 份配套契约文档 |
| 规范版本号 | `1.0.0-frozen-candidate` |
| 生效范围 | 后端工程骨架、Controller 骨架、前端联调、服务端测试的**唯一接口依据** |

---

## 1. 冻结声明

### 1.1 声明正文

> 本目录下的 `openapi.yaml`（OpenAPI 3.0.3，`info.version = 1.0.0-frozen-candidate`）及配套文档，
> 构成 **迭代一接口契约的冻结候选版本**。
>
> 自本声明发布起：
>
> 1. **接口路径、HTTP 方法、operationId、请求/响应 DTO 结构、枚举取值、错误码、幂等要求、并发语义**
>    视为**已冻结**，不得单方面变更。
> 2. 后端工程骨架、Controller 骨架、前端联调与服务端测试**必须**以本契约的唯一依据实现；
>    实现与契约不一致时，**以契约为准**，或按 §4 流程修订契约。
> 3. **2026-10-09 之后不允许新增接口**（迭代一路线硬性要求）。新增接口需走 §4 变更流程并留存记录。
> 4. 通过评审后，文档状态由 `冻结候选` 升级为 `已冻结（FROZEN）`，`info.version` 升级为 `1.0.0`。

### 1.2 冻结范围

| 冻结内容 | 载体 | 冻结强度 |
| --- | --- | --- |
| 路径、方法、operationId | `openapi.yaml` `paths` | **强冻结**（新增/改名/改路径均需变更流程） |
| 请求/响应 DTO 字段与类型 | `openapi.yaml` `components.schemas` | **强冻结**（新增可选字段需变更流程；删除/改类型禁止） |
| 枚举取值 | `openapi.yaml` 枚举 schema | **强冻结**（必须与 `V1__init_schema.sql` 一致） |
| 统一包络与分页结构 | `ApiResponse` / `ErrorResponse` / `PageResult` | **强冻结** |
| 错误码 | [error-codes.md](./error-codes.md) | **强冻结** |
| 权限要求 | `x-required-roles` + [api-permission-mapping.md](./api-permission-mapping.md) | **强冻结** |
| 幂等要求与并发语义 | [idempotency-and-concurrency.md](./idempotency-and-concurrency.md) | **强冻结** |
| FR 映射 | [api-contract.md](./api-contract.md) §3 | 强冻结（54/54 覆盖） |
| `description` 文案 | `openapi.yaml` | **弱冻结**（措辞可优化，语义不得变） |
| **占位数据的具体值** | `TradeZone.code/name`、`PickupPoint.code/name` | **不冻结**（Q-01/Q-02/Q-03 确认后替换，见 §6） |

### 1.3 契约权威顺序

当文档之间出现分歧时，按以下优先级判定：

```text
1. openapi.yaml            （机器可读，最终权威）
2. api-contract.md         （接口清单与 FR 映射）
3. error-codes.md          （错误码语义）
4. api-permission-mapping.md（权限边界）
5. idempotency-and-concurrency.md（幂等与并发细节）
6. api-open-questions.md   （假设与待确认清单）
```

> 若发现 `openapi.yaml` 与其余文档不一致，**必须**在冻结评审时修正，不得带着分歧进入实现阶段。

---

## 2. 冻结基线核对表

| # | 核对项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | OpenAPI 3.0.3 可被工具解析 | ✅ | `openapi-spec-validator` 校验通过（见 [db-verification.md](./db-verification.md) 同级的自检报告 / §7） |
| 2 | 无 YAML 语法错误 | ✅ | `yaml.safe_load` 通过 |
| 3 | 无悬空 `$ref` | ✅ | 110 个去重 `$ref` 全部可解析 |
| 4 | operationId 唯一 | ✅ | 83 个 operationId，0 重复、0 缺失 |
| 5 | 每个操作有 summary/description/tags/responses/x-required-roles | ✅ | 完整性检查 0 缺失 |
| 6 | 迭代一全部功能均有接口 | ✅ | 13 模块 83 操作；账户/商品/购买/交换/治理/评价/举报/通知/定时任务全覆盖 |
| 7 | DTO 不暴露敏感字段 | ✅ | 无 `password_hash`/`deleted_at`/`created_by`/`updated_by` |
| 8 | 枚举与数据库一致 | ✅ | **21/21** 枚举与 `V1__init_schema.sql` 逐字一致 |
| 9 | 错误码覆盖 17 类错误场景 | ✅ | E-01~E-17 全量映射（见 error-codes.md §2） |
| 10 | 权限覆盖 7 主体 × 6 域 | ✅ | api-permission-mapping.md |
| 11 | 幂等要求已声明 | ✅ | 16 必填 + 16 可选（见 idempotency-and-concurrency.md §3/§4） |
| 12 | 并发语义已声明 | ✅ | `x-locking` 扩展 + idempotency-and-concurrency.md §6/§8 |
| 13 | FR 覆盖率 | ✅ | **54 / 54 = 100%**（api-contract.md §3） |
| 14 | 占位数据标注【待确认】 | ✅ | 18 处【待确认】，集中在区域/取货点 |
| 15 | P0/DQ 假设全部登记 | ✅ | api-open-questions.md §2/§3 |
| 16 | 未写后端/前端代码 | ✅ | 仅 Markdown + YAML |

---

## 3. `2026-10-09 后不新增接口` 约束

### 3.1 约束内容

| 项 | 规则 |
| --- | --- |
| 冻结时间点 | **2026-10-09**（迭代一路线给定） |
| 允许的操作 | ① 修正 `description` 文案；② 补充 `example`；③ 补充**可选**响应字段（向后兼容）；④ 修正错误 |
| **禁止**的操作 | ① **新增任何接口**；② 删除接口；③ 修改路径/方法/operationId；④ 删除或重命名 DTO 字段；⑤ 收窄枚举取值；⑥ 变更必填性（可选→必填）；⑦ 变更错误码语义 |
| 例外 | 仅当出现 **P0 级阻塞**（如安全漏洞、数据正确性缺陷）时，走 §4 流程，且须记录在 §5 变更台账 |

### 3.2 为什么严格禁止新增

| 理由 | 说明 |
| --- | --- |
| 工程骨架已按 83 个操作生成 Controller 骨架 | 新增接口意味着骨架返工 |
| 前端联调已按契约接入 | 新增接口破坏联调基线 |
| 服务端测试用例已按契约编写 | 新增接口使测试覆盖出现空洞 |
| 契约漂移是迭代延期的主因之一 | 路线明确要求「10/9 后不许新增」 |

### 3.3 覆盖确认

| 模块 | 操作数 | 是否可覆盖迭代一全部需求 |
| --- | --- | --- |
| 01-认证与账户 | 6 | ✅ FR-101~FR-104 |
| 02-个人资料 | 5 | ✅ FR-105~FR-108 |
| 03-字典与区域 | 4 | ✅ R-15 / R-06 / FR-206 / FR-207 |
| 04-商品 | 10 | ✅ FR-201~FR-211 |
| 05-购物车 | 4 | ✅ FR-301~FR-302 |
| 06-订单与付款 | 8 | ✅ FR-303~FR-311 |
| 07-评价 | 3 | ✅ FR-312 |
| 08-交换包 | 5 | ✅ FR-402~FR-403 |
| 09-交换提议 | 11 | ✅ FR-404~FR-412 |
| 10-举报 | 3 | ✅ FR-211 |
| 11-管理员治理 | 17 | ✅ FR-501~FR-505 |
| 12-通知 | 3 | ✅ FR-507 |
| 13-系统定时任务 | 4 | ✅ FR-601~FR-604 |
| **合计** | **83** | **54/54 FR 覆盖，无缺口** |

> 结论：**迭代一全部功能在冻结前已无遗漏**，因此「不新增」约束是可执行的，不存在已知未覆盖需求。

---

## 4. 变更流程

### 4.1 流程步骤

| 步骤 | 责任人 | 动作 | 产出 |
| --- | --- | --- | --- |
| 1 | 提出方 | 填写**变更申请**：变更类型、涉及 operationId/字段、原因、不做的后果 | 变更申请记录 |
| 2 | 提出方 | 评估**影响面**：受影响的后端骨架文件、前端页面、测试用例、数据库结构 | 影响清单 |
| 3 | 接口契约负责人 | 判定变更类型（§4.2）与是否属 P0 例外 | 判定结论 |
| 4 | 需求方 + 接口负责人 | **审批**（§4.3） | 审批意见 |
| 5 | 接口契约负责人 | 更新 `openapi.yaml` + 配套文档，**提升 `info.version`** | 新版本契约 |
| 6 | 接口契约负责人 | 登记**变更台账**（§5） | 台账记录 |
| 7 | 后端 / 前端 | 同步实现与测试 | 实现与用例更新 |

### 4.2 变更类型与要求

| 类型 | 示例 | 是否允许 | 额外要求 |
| --- | --- | --- | --- |
| **A. 文案修订** | 修正 `description` 错别字 | ✅ 允许 | 无需审批，仅登记 |
| **B. 补充示例** | 增加 `example` | ✅ 允许 | 无需审批，仅登记 |
| **C. 向后兼容扩充** | 新增**可选**响应字段 | ⚠️ 需接口负责人审批 | 不得影响既有解析；须同步 `api-contract.md` |
| **D. 缺陷修正** | 修正错误码语义、修正 DTO 类型错误 | ⚠️ 需审批 | 须评估是否已实现 |
| **E. 破坏性变更** | 删字段、改类型、可选→必填、改路径 | ❌ **禁止**（迭代一内） | 若必须，属 P0 例外，需 §4.3 双人审批 |
| **F. 新增接口** | 新增 endpoint | ❌ **禁止**（2026-10-09 后） | 同上；须证明为 P0 阻塞且无法用既有接口承载 |
| **G. 替换占位数据值** | `CAMPUS_A` → 真实编码 | ✅ 允许 | **不属变更**，见 §6 |

### 4.3 审批要求

| 变更类型 | 审批人 | 记录要求 |
| --- | --- | --- |
| A / B | 接口契约负责人（单人） | 变更台账 |
| C / D | 接口契约负责人 + 后端负责人 | 变更台账 + 影响清单 |
| E / F（P0 例外） | 接口契约负责人 + 需求方 + 后端负责人（**三人**） | 变更台账 + 影响清单 + **书面理由** |

---

### 4.4 本次冻结前修正的一个契约缺陷（记录备查）

| 项 | 内容 |
| --- | --- |
| 编号 | **DQ-27** |
| 缺陷 | FR-604 `runReviewWindowCloseTask` 的扫描条件原为 `review_status <> 'WINDOW_CLOSED'`，会把终态 `BOTH_REVIEWED`（双方已评）纳入并把订单**回退**为 `WINDOW_CLOSED`，违反评价状态机 R-05 的 **IDEM-C 终态保护** |
| 发现方式 | 接口契约阶段对 `openapi.yaml` 与 `state-machines.md`、数据库部分索引做交叉复核时发现 |
| 修正 | ① `openapi.yaml`：扫描条件与 `x-locking` 改为 `review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED') AND now() > review_deadline`；② 数据库：新增迁移 `V4__fix_review_window_index.sql` 重建 `idx_orders_review_window` |
| 影响面 | 无接口路径/DTO/枚举变化（**不属冻结变更**）；仅修正实现语义与索引条件。已在空库验证 V1~V4 顺序执行成功 |
| 登记 | `open-questions.md` DQ-27、`db-verification.md` §3.1.1 |

## 5. 变更台账（迭代一）

| # | 日期 | 类型 | 变更内容 | 涉及 operationId/字段 | 影响面 | 审批 | 版本 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| — | 2026-10-12 | 初始基线 | 首次冻结候选：83 操作 / 13 模块 / 99 schema | 全部 | — | 待评审 | 1.0.0-frozen-candidate |

> **后续所有变更必须在本表追加行**，不得就地修改历史记录。

---

## 6. 占位数据替换（不是契约变更）

**【待确认 Q-01 / Q-02 / Q-03】** 的真实数据落地时，遵循以下规则，**无需走 §4 变更流程**：

| 项 | 规则 |
| --- | --- |
| 允许变更 | `TradeZone.code` / `TradeZone.name` / `PickupPoint.code` / `PickupPoint.name` / `PickupPoint.description` 的**具体值** |
| 禁止变更 | 路径（`/trade-zones` 等）、DTO 结构、字段名、类型、枚举 |
| 标识符 | `id` 为自增主键，前端**不得**硬编码；`code` 可作稳定业务标识 |
| 数量 | 最终为 2–3 个校区及周边社区；数量增减不影响契约 |
| 校验不变 | 「1–5 个常用区域」「同区域交易」「取货点属订单区域」三条校验保持不变（R-15 / R-06） |
| 数据来源 | 由数据库迁移（新增 `V4+`）落地，**不修改已执行的 `V1`~`V3`** |
| 文档同步 | 需同步去除 `openapi.yaml` 中对应 `description` 的「【待确认】」标注 |

---

## 7. 未确认 P0 / DQ 假设汇总

> 以下假设**已在冻结候选中生效**（否则无法完成契约）。若后续确认结果不同，需走 §4 流程修订。
> 完整清单见 [api-open-questions.md](./api-open-questions.md)。

### 7.1 P0：需求待确认项（Q-01 ~ Q-04）

| 编号 | 问题 | 默认假设（已在契约生效） | 契约落点 | 若变更的影响 |
| --- | --- | --- | --- | --- |
| **Q-01** | 最终服务哪 2–3 个校区及周边社区 | 占位：`CAMPUS_A/B/C` + `COMMUNITY_A1/A2/B1/C1` | `TradeZone.code/name`（【待确认】） | **仅数据替换**，不改结构（§6） |
| **Q-02** | 交易区域清单与编码规则 | 占位编码 `CAMPUS_*` / `COMMUNITY_*`，`code VARCHAR(32)` | `TradeZone` | 仅数据替换 |
| **Q-03** | 公共取货点清单与命名 | 占位：`PICKUP_LIBRARY_A` 等 13 个，6 种类型 | `PickupPoint` | 仅数据替换 |
| **Q-04** | 管理员账号来源 | 假设由种子数据创建 `admin@example.com`（占位密码，**禁止生产执行 V3**） | 不在 API 层；登录走 `login` | 若改为配置/运维创建，**API 无影响** |

### 7.2 P0：数据库设计待确认项（DQ-01 ~ DQ-06）

| 编号 | 问题 | 默认假设（已在契约生效） | 契约落点 | 若变更的影响 |
| --- | --- | --- | --- | --- |
| **DQ-01** | 是否启用商品「草稿」态 | **不启用**，发布即 `IN_SALE`（A-14） | `createItem` 无状态参数 | 若启用草稿，需**新增接口**（禁止）或加可选字段 → §4 类型 C |
| **DQ-02** | 交易区域能否停用 | 可停用（`is_active`），列表默认仅返回启用项 | `listTradeZones.includeInactive`（仅管理员生效） | 若不可停用，该参数退化为无效果（兼容） |
| **DQ-03** | 多区域用户一次交易是否可跨自己勾选的区域 | **不可跨**，强校验同一 `tradeZoneId` | `TRADE_ZONE_MISMATCH` | 若允许互通，需引入区域组 → 破坏性变更，§4 类型 E |
| **DQ-04** | 取货点是否需要开放时间 | **不需要**，仅 `description` 文字说明 | `PickupPoint` 无时段字段 | 若需要，新增可选字段 → §4 类型 C |
| **DQ-05** | 封禁能否延长/覆盖/调时长 | **固定 72 小时且拒绝重复封禁** | `adminBanUser`（`durationHours` 只读固定 72）+ `GOVERNANCE_ACTIVE_PENALTY_EXISTS` | 若允许延长，需移除该 409 语义 → §4 类型 E |
| **DQ-06** | 审计日志是否需导出 | **不需要** | `adminListAuditLogs` 仅查询 | 若需要导出，需新增接口（禁止）→ P0 例外 |

### 7.3 P1 关键假设（影响接口语义）

| 编号 | 假设 | 契约落点 |
| --- | --- | --- |
| **Q-05** | 通知仅站内渠道 | `NotificationSettings.emailEnabled` 恒 false |
| **Q-07** | 仅固定 7 天令牌，`refreshToken` **未启用**（调用返回 501） | `refreshToken`（路径与 DTO 已冻结，实现开关待定） |
| **Q-08** | 已付款订单**不可**自助取消 | `cancelOrder` → `ORDER_CANNOT_CANCEL_AFTER_PAID` |
| **Q-09** | 封禁时长固定不可调 | `adminBanUser` 无时长入参 |
| **Q-10** | 重复封禁**拒绝** | `GOVERNANCE_ACTIVE_PENALTY_EXISTS` |
| **Q-13** | 处罚前已有订单/交换的**收尾操作允许** | `markOrderDelivered` / `confirmOrderReceipt` 未标处罚禁止 |
| **Q-24** | 拆单时某件失效 → **整批失败** | `submitOrder` 失败语义（不产生部分下单） |
| **Q-27** | 幂等键前端生成 UUID，保留 24h | 16 个必填接口 |
| **Q-30** | 交换码格式（建议 6–8 位，仅手输不扫码） | `SwapCode.code`（`VARCHAR(16)`） |
| **Q-33** | 通知列表页是否纳入页面基线 | 接口已冻结，页面待定 |
| **Q-34** | 搜索是否独立页面/维度 | 内嵌于 `listItems`（无独立搜索接口） |

### 7.4 其余假设（A-01 ~ A-20）

| 编号 | 假设 | 契约落点 |
| --- | --- | --- |
| A-01 | 付款倒计时 30 分钟 | `OrderDetail.expiresAt` |
| A-02 | 交换提议 72 小时 | `SwapProposalDetail.expiresAt` |
| A-03 | 封禁 72 小时 | `Penalty.endsAt` |
| A-04 | 昵称不唯一 | `updateMyProfile.nickname` |
| A-05 | 图片 ≤5MB、JPG/PNG/WebP、商品 1–9 张 | `uploadImage` + `FILE_TOO_LARGE` |
| A-06 | 令牌 7 天 | `AuthTokenResponse.expiresIn = 604800` |
| A-07 | 互评窗口 7 天 | `ReviewStatusResponse.deadline` |
| A-08 | 幂等记录保留 24h | `idempotency_records.expires_at` |
| A-09 | 昵称 1–20 字符 | `nickname` `maxLength: 20` |
| A-10 | 反提案仅一轮 | `SWAP_COUNTER_NOT_ALLOWED` |
| A-11 | 处罚期内随时可申诉 | `createAppeal` |
| A-12 | 处罚到期后未处理申诉自动结案 | 【待确认 Q-18】，当前不暴露接口 |
| A-13 | 处罚期间禁止改昵称/头像 | `updateMyProfile` → `USER_RESTRICTED_CANNOT_WRITE` |
| A-14 | 处罚期间允许已有订单收尾 | `markOrderDelivered` / `confirmOrderReceipt` |
| A-15 | 幂等键前端生成 UUID v4 | 16 个必填接口 |
| A-16 | 购物车数量固定为 1 | `CartItem.quantity` 枚举 `[1]` |
| A-17 | 跨卖家拆单失败语义为整批失败 | `submitOrder` |
| A-18 | 付款后才可选取货点 | `ORDER_NOT_PAID_FOR_PICKUP_SELECTION` |
| A-19 | 模拟付款一键成功（无需验证码） | `MockPayRequest.confirm` 默认 true |
| A-20 | 业务时区 Asia/Shanghai，时间用 ISO 8601 UTC | 全部时间字段 |

---

## 8. 冻结后的版本管理

| 场景 | `info.version` | 文档状态 |
| --- | --- | --- |
| 当前（冻结候选） | `1.0.0-frozen-candidate` | 冻结候选 |
| 通过评审后 | `1.0.0` | 已冻结（FROZEN） |
| 类型 A/B（文案/示例） | `1.0.1`（补丁） | 已冻结 |
| 类型 C/D（兼容扩充/缺陷修正） | `1.1.0`（次版本） | 已冻结 |
| 类型 E/F（破坏性/新增，P0 例外） | `2.0.0`（主版本） | 已冻结 + 需重新冻结评审 |

> 迭代一内**目标是不出现 `2.0.0`**。若出现，说明冻结失效，需复盘。

---

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 接口契约负责人 | 初稿：冻结声明、冻结范围与权威顺序、16 项基线核对、10/9 不新增约束、变更流程与审批、变更台账、占位替换规则、P0/DQ 假设汇总、版本管理策略 | **冻结候选** |
