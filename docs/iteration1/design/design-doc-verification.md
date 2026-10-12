# 迭代一 设计文档补齐 验证记录

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 设计文档补齐 验证记录 |
| 版本 | v1.1 |
| 日期 | 2026-10-12 |
| 作者 | 系统设计文档负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 验证对象 | `docs/iteration1/design/SystemDesign.md`、`docs/iteration1/design/sequence-diagrams.md` |
| 校验快照 | `SystemDesign.md` = **63712** 字节 @22:53:52；`sequence-diagrams.md` = **77259** 字节 @22:56:09 |
| 校验方式 | 全部结果基于**磁盘实测**（脚本读取 + 数据库实查 + `openapi.yaml` 解析），非人工估计 |
| 结论 | ✅ **10 项验收标准全部通过**；交叉引用 13 项检查全部通过；登记 1 项上游真实缺陷（DDP-01）——**第 7 步已按授权修复并闭环** |

> **关于校验快照**：本报告编写期间两份被检文档仍在小幅修订（补齐交叉链接、修正错误码字面量）。
> 上表字节数为**最终定稿**版本。若后续再有修改，本报告的统计数字需同步重算。

---

## 1. 本步骤交付物

| 文件 | 类型 | 内容摘要 | 实测字节 |
| --- | --- | --- | --- |
| [`SystemDesign.md`](./SystemDesign.md) | 新建 | 统一系统设计文档：10 个一级章节（§0 阅读指引 / §1 概述 / §2 架构 / §3 领域模型 / §4 状态机与规则 / §5 权限 / §6 接口清单 / §7 不做清单 / §8 假设 / §9 附录）；内嵌 10 个 Mermaid 图 | 63712 |
| [`sequence-diagrams.md`](./sequence-diagrams.md) | 新建 | 关键流程时序图：12 个一级章节（§0 / §1~§7 主体 / §8 交叉索引 / §8.1 文件导览 / §9 渲染说明 / §10 变更记录）；9 个 `sequenceDiagram` | 77259 |
| [`README.md`](./README.md) | 更新 | 新增 §1.0.0「系统设计文档与时序图」索引（含图表统计），并给出建议阅读顺序：需求 → 数据库 → 接口契约 → 设计文档 → 时序图 | 23301 |
| [`design-doc-verification.md`](./design-doc-verification.md) | 新建 | 本文件 | — |

**未修改**（符合「不擅自修改上游文档」要求）：

| 文件 | 实测证据 |
| --- | --- |
| `docs/iteration1/design/openapi.yaml` | 仍 **83** 个 `operationId`；mtime `10-12 21:41`（本步骤未触碰） |
| `src/main/resources/db/migration/V1~V4` | mtime `10-12 21:18 ~ 21:41`（本步骤未触碰） |
| 需求基线 7 份 / 数据库设计 4 份 / 契约 6 份 | 本步骤只读取与引用 |

---

## 2. 统计

### 2.1 被检文档侧（实测）

| 统计项 | `SystemDesign.md` | `sequence-diagrams.md` |
| --- | --- | --- |
| 一级章节（`##`） | **10** | **12** |
| Mermaid 块总数 | **10** | **9** |
| 图表类型分布 | `flowchart` 4 / `erDiagram` 1 / `stateDiagram-v2` 5 | `sequenceDiagram` 9 |
| 内部相对链接 | 23（**全部可解析**） | 13（**全部可解析**） |
| 引用的 operationId | 45 | 39 |
| 引用的错误码 | 53（全部为 `error-codes.md` 已定义） | 30（全部已定义） |
| 引用的状态转换编号 | 51（全部合法） | 46（全部合法） |
| 引用的规则编号 | 27（全部合法） | 27（全部合法） |
| 引用 FR 编号 | 16 | **35**（P-01 优化后；见 §4.1.2） |
| 涉及表名 | **25 / 25**（与实库完全一致） | — |

### 2.2 覆盖的流程（实测 7 组 / 9 图）

| # | 时序图 | 章节 | 覆盖 operationId 数 | 状态转换编号 |
| --- | --- | --- | --- | --- |
| 1 | 购买主线（浏览 → 互评） | §1 | 11 | `O-01` `I-07` `O-02` `O-05` `O-06` `O-07` `I-10` `O-10` |
| 2 | 交换主线（发起 → 整包接受 → 交换码 → 确认，含拒绝/反提案/取消分支） | §2 | 11 | `S-01`~`S-05` `S-08`~`S-10` `I-11`~`I-13` |
| 3 | 交换**整笔作废**（R-24 / R-35） | §3 | 4 | `S-07` `S-13` `I-12` `I-14` `I-15` |
| 4 | 治理主线（举报 → 下架 → 封禁 → 申诉 → 解封） | §4 | 13 | `P-01`~`P-07` `P-09` `AP-01`~`AP-03` `I-14`~`I-16` |
| 5 | **并发抢购**（两人抢最后一件，仅一人成功） | §5 | 1 | `I-07` `O-01` `O-12` |
| 6 | **超时释放**（订单 30 分钟未付款 → 定时任务释放 → 通知双方） | §6 | 4 | `O-01` `O-02` `O-04` `I-07`~`I-09` |
| 7 | 定时任务：72h 过期 / 到期自动解封 / 评价窗口关闭（含终态保护） | §7 | 4 | `S-06` `P-04` `P-05` `P-07` `P-09` `AP-04` |

> 任务要求「至少 5 个」——实际覆盖 **7 组流程 / 9 个图**，超出要求。

### 2.3 基准侧（上游文档）实测读数

| 基准项 | 实测值 | 来源 |
| --- | --- | --- |
| 业务表 | **25** 张 | `information_schema`（实库） |
| 列总数 | **324** 列 | `information_schema`（实库） |
| 枚举类型 | 26（含 21 个业务枚举） | `pg_enum`（实库） |
| 部分唯一索引 | **7** 个 | `pg_indexes WHERE indexdef ILIKE '%UNIQUE%WHERE%'` |
| 契约 paths / operations | **73 / 83** | `openapi.yaml` |
| 契约 schemas | 99 | `openapi.yaml` |
| 错误码 | **93** 个 | `error-codes.md` |
| FR 总数 | **54** 条 | `requirements.md` |
| 不做清单 | **18** 项（含 4 项核心） | `out-of-scope.md` |
| 状态机转换编号 | **70** 个（`I-`/`O-`/`S-`/`B-`/`P-`/`AP-`） | `state-machines.md` |
| 规则 | **34** 条（R-01~R-35） | `rules.md` |
| Controller（契约生成） | **12** 个 | `src/main/java/.../controller/` |
| DTO | **99** 个（request 33 + response 66） | `src/main/java/.../dto/` |
| Service 接口 / 实现 | **12 / 12** | `src/main/java/.../service/` |
| Flyway 迁移 | **V1~V4**，4 条 `success = t` | `flyway_schema_history`（实库） |

---

## 3. 交叉引用一致性检查

### 3.1 检查结果总览

| # | 检查项 | 期望 | 实测 | 结果 |
| --- | --- | --- | --- | --- |
| C-01 | **表名一致性** | 仅出现 25 张业务表中的表名 | `SystemDesign.md` 提及 **25** 个表名，**全部**属于实库 25 张业务表；无非法表名 | ✅ |
| C-02 | **状态枚举一致性** | 仅出现契约/实库定义的枚举取值 | 两份文档出现的 `SNAKE_CASE` 标识符经逐条比对，**未发现非法状态值**；被正则误报的 14 项（如 `SWAP_ITEM_OCCUPIED`、`ORDER_TIMEOUT_RELEASED`）实为**错误码**或**通知类型**，非状态值 | ✅ |
| C-03 | **错误码一致性** | 仅出现 `error-codes.md` 已定义的 93 个码 | 两份文档引用的错误码**全部**在 `error-codes.md` 中已定义；**0 个编造码**（过程中曾发现 7 个并已修正，见 §4.2） | ✅ |
| C-04 | **operationId 一致性** | 引用的 operationId 必须存在于契约 83 个之中 | `SystemDesign.md` 引用 45 个、`sequence-diagrams.md` 引用 39 个，**全部**存在于契约 83 个之中；0 个多余、0 个拼写错误 | ✅ |
| C-05 | **FR 编号一致性** | 引用的 FR 必须存在于 `requirements.md` 的 54 条 | `SystemDesign.md` 引用 16 个 FR，**全部**已登记；无未登记 FR | ✅ |
| C-06 | **状态转换编号一致性** | 引用的编号必须存在于 `state-machines.md` 的 70 个 | 两份文档共引用 51 + 46 个转换编号，**全部**合法；仅 `SystemDesign.md` 的 `P-00` 为**权限边界编号**（P-00-A/B/C），非状态转换，属命名空间巧合 | ✅ |
| C-07 | **规则编号一致性** | 引用的 R-xx 必须存在于 `rules.md` 的 34 条 | 两份文档共引用 27 条，**全部**合法 | ✅ |
| C-08 | **不做清单一致性** | 与 `out-of-scope.md` 的 18 项逐一对应 | `SystemDesign.md` §7 列出 **18** 项，编号集合与 `out-of-scope.md` **完全一致**；**4 项核心**（OOS-01/02/03/04）均加粗标注且名称正确 | ✅ |
| C-09 | **权限一致性** | 与 `api-permission-mapping.md` §3.8 及运行时 `/v3/api-docs` 一致 | `SystemDesign.md` §5.2 给出 公开 **10** / 需登录 **73** / 仅管理员 **14** / 系统内部 **4**，与上游一致；恒等式 10+73=83 成立 | ✅ |
| C-10 | **Mermaid 语法可渲染性** | 所有块语法正确 | 两份文档共 **19** 个 Mermaid 块：`sequenceDiagram` 的 `alt/else/opt/loop/par/end` **全部配平**；所有行双引号**成对**；图表类型均为 Mermaid 支持类型 | ✅ |
| C-11 | **内部链接可解析** | 相对链接全部指向存在文件 | 共 36 个相对链接（23 + 13），**全部可解析**（过程中修正 1 处层级错误的 `README-backend.md` 链接，见 §4.2） | ✅ |
| C-12 | **迁移脚本未修改** | V1~V4 mtime 早于本步骤 | V1 `21:23`、V2 `21:18`、V3 `21:20`、V4 `21:41`，均为第 2~3 步产出时间 | ✅ |
| C-13 | **OpenAPI 契约未修改** | 仍为 73 paths / 83 operations | `operationId` 计数 = **83**；mtime `21:41`（本步骤未触碰） | ✅ |

**结论：13 项交叉引用检查全部通过。**

### 3.2 时序图与接口/状态的对应（逐图可追溯）

每图前的说明表均给出「参与角色 / 覆盖 operationId / 状态转换编号 / 关键规则 / 主要错误码」五项，
且 `sequence-diagrams.md` §8 提供汇总交叉索引。抽查示例：

| 时序图步骤（示例） | 对应 operationId | 状态转换 | 规则 | 错误码 |
| --- | --- | --- | --- | --- |
| 提交订单（下单即占用） | `submitOrder` | `O-01` + `I-07` | R-16 / R-05 / R-18 | `ITEM_ALREADY_RESERVED` |
| 模拟付款 | `mockPayOrder` | `O-02` | R-08 / R-07 | `ORDER_TIMEOUT_RELEASED` |
| 选择取货点 | `selectPickupPoint` | `O-05` | R-06 / R-15 | `PICKUP_POINT_ZONE_MISMATCH` |
| 卖家交付 | `markOrderDelivered` | `O-06` | R-06 | `ORDER_PICKUP_POINT_NOT_SELECTED` |
| 买家确认收货 | `confirmOrderReceipt` | `O-07` + `I-10` | R-09 | `ORDER_STATUS_NOT_DELIVERED` |
| 整包接受 | `acceptSwapProposal` | `S-02` | R-22 / R-23 | `SWAP_PROPOSAL_ALREADY_ACCEPTED` |
| 核销交换码 | `verifySwapCode` | `S-08` | R-26 | `SWAP_CODE_ALREADY_VERIFIED` |
| 双方确认 | `confirmSwap` | `S-09` / `S-10` + `I-13` | R-27 | `SWAP_CODE_NOT_VERIFIED` |
| 整笔作废 | 系统联动 | `S-07` / `S-13` | R-24 / R-35 | `SWAP_PROPOSAL_VOIDED` |
| 封禁用户 | `adminBanUser` | `P-01` | R-30 / R-34 | `GOVERNANCE_ACTIVE_PENALTY_EXISTS` |
| 处理申诉 | `adminResolveAppeal` | `AP-02` / `AP-03` | R-32 / R-34 | `APPEAL_ALREADY_RESOLVED` |
| 超时释放 | `runOrderTimeoutReleaseTask` | `O-04` + `I-08` | R-07 / R-16 | `ORDER_TIMEOUT_RELEASED` |

---

## 4. 上游文档矛盾与待确认问题

### 4.1 新增矛盾登记（DDP）

| 编号 | 问题 | 实测证据 | 影响 | 处置建议 |
| --- | --- | --- | --- | --- |
| **DDP-01** ✅ **已修复** | `state-machines.md` **第 189 行** `S-07`（整笔作废）把规则交叉引用写成「**规则 R-22** 的落地」，但 `rules.md` 中 **R-22 = 整包接受、不可部分接受**，**R-24 = 整笔作废** | `state-machines.md:189` 原文含「规则 R-22 的落地」；`rules.md:274` 标题为「R-22 整包接受」、`rules.md:294` 标题为「R-24 整笔作废」 | 仅**文档交叉引用**错误；不影响数据库、契约与代码（`SystemDesign.md` 与 `sequence-diagrams.md` **均正确**使用 R-24，**未传播**该错误） | ✅ **已按授权修正**为 R-24（详见 §4.1.1）。修改仅限该处引用，未改动任何规则语义 |

**复核澄清（避免误报）**：`error-codes.md` 中 `SWAP_PROPOSAL_ALREADY_ACCEPTED` 行标注的「**R-22 / S-02**」
**是正确的**——该错误码语义为「已整包接受 → 重复接受失败」，归属 **R-22（整包接受）** 与 `S-02`。
**无需修改**。此前生成的 `sequence-diagrams.md` 初稿曾把两者一并报为缺陷，已在本步骤复核纠正。

#### 4.1.1 DDP-01 修复记录（第 7 步执行，用户已授权）

**修改文件**：`docs/iteration1/state-machines.md`（**仅此一处，仅此一行**）

**修改前后对比**（第 189 行）：

```diff
- | S-07 | PENDING | 任一商品被其他占用/下架/违规下架 | CANCELLED（原因=整笔作废） | 系统联动 | 触发条件出现 | **整笔作废**：释放全部占用；通知双方；写原因（是「任一件被占则整笔作废」规则 R-22 的落地） | IDEM-B（只作废一次） |
+ | S-07 | PENDING | 任一商品被其他占用/下架/违规下架 | CANCELLED（原因=整笔作废） | 系统联动 | 触发条件出现 | **整笔作废**：释放全部占用；通知双方；写原因（是「任一件被占则整笔作废」规则 R-24 的落地） | IDEM-B（只作废一次） |
```

| 修复项 | 值 |
| --- | --- |
| 唯一变更 | 规则引用 `R-22` → **`R-24`** |
| 变更字符数 | 1 处（表格单元格内 1 个编号） |
| 语义影响 | **无**。整笔作废的语义、触发条件、动作、幂等要求均未改动，仅修正指向的规则编号 |
| 是否改动 `rules.md` | **未改动**（R-22 仍为「整包接受」、R-24 仍为「整笔作废」，定义本身正确） |
| 是否改动其他文件 | **未改动**（`git`-less 环境下以 `find -newermt` 验证：本步仅触碰 `state-machines.md` 与文档类文件） |
| 残留核查 | `grep -rn 'S-07' docs/iteration1/ \| grep 'R-22'` 仅命中**本缺陷记录本身**（§4.1 与本表），无其他误引用 |
| 交叉验证 | `rules.md:274` =「R-22 整包接受、不可部分接受」；`rules.md:294` =「R-24 整笔作废」→ 修正后引用**一致** |

> **结论**：DDP-01 已闭环。迭代一交付版本中 `state-machines.md` 的 S-07 正确引用 **R-24**。

#### 4.1.2 P-01 追溯优化记录（第 7 步执行）

**修改文件**：`docs/iteration1/design/sequence-diagrams.md`（版本 v1.0 → **v1.1**）

| 优化项 | 内容 |
| --- | --- |
| 每图说明表 | 9 张图的说明表各新增 **`覆盖 FR`** 行（原为「参与角色 / 覆盖 operationId / 状态转换 / 关键规则 / 错误码」五项，现为六项） |
| §8 交叉索引表 | 新增 **`覆盖 FR`** 列（表头由 5 列变 6 列，9 行数据同步补齐） |
| FR 派生口径 | 全部取自 `api-contract.md` §3「FR 覆盖矩阵」与 §2「接口清单」的 `#NN → operationId` 对照；**未自造任何 FR 编号** |
| 覆盖 FR 总数 | 文档共引用 **35 个不同 FR**（FR-209/210/211、301~307、309~312、401~412、501~506、601~604），**全部**存在于 `requirements.md` 的 54 条之中 |
| 追溯链变化 | 由「时序图 → operationId → （跳转 api-contract §3）→ FR」**两步**，变为「时序图 → FR」**一步直达** |
| Mermaid 影响 | 9 个 `sequenceDiagram` 块**未改动**，语法校验仍 0 问题 |

**结论**：P-01 已闭环。

### 4.2 本步骤发现并已修正的问题（不属上游缺陷）

| # | 问题 | 影响 | 处置 |
| --- | --- | --- | --- |
| 1 | `sequence-diagrams.md` 初稿使用了 **7 个 `error-codes.md` 中不存在的错误码**（`APPEAL_NOT_ALLOWED`、`VALIDATION_REQUIRED_FIELD_MISSING`、`GOVERNANCE_USER_RESTRICTED`、`AUTH_INTERNAL_TOKEN_INVALID`、`ORDER_STATUS_INVALID`、`ORDER_ALREADY_COMPLETED`、`ITEM_STATUS_NOT_RESTORABLE`） | 违反「错误码可追溯」，前端会收到契约外的码 | 全部替换为契约内真实码：`APPEAL_PENALTY_NOT_ACTIVE`、`VALIDATION_INVALID_PARAMETER`、`USER_RESTRICTED_CANNOT_WRITE`、`AUTH_UNAUTHENTICATED`、`ORDER_STATUS_NOT_PAYABLE`、`ORDER_ALREADY_CANCELLED`、`ITEM_BANNED_CANNOT_RELIST`；另 `SWAP_PROPOSAL_RECEIVED` → `SWAP_PROPOSED`（该处本为**通知类型**，非错误码） |
| 2 | `SystemDesign.md` 中「部分唯一索引」的**成员列举有误**：把 `uq_swap_codes_proposal`、`uq_idempotency_user_key` 列为部分索引（实为**完整唯一约束**），并遗漏 `uq_reviews_order_reviewer`、`uq_reviews_swap_reviewer` | 数量「7」正确，但成员错误，会误导实现 | 已按实库更正为 7 个真实部分索引 + 另附「完整唯一约束」对照表，并显式说明与上游 `idempotency-and-concurrency.md` §7 的口径差异 |
| 3 | `SystemDesign.md` 一处相对链接层级错误（`../../../../README-backend.md` 比实际多一层） | 链接失效，评审无法跳转 | 修正为 `../../../README-backend.md` |
| 4 | `SystemDesign.md` 混淆「Java 枚举常量名」与「线上错误码」（写成 `VALIDATION_ERROR` 等） | 读者会以为线上返回 `VALIDATION_ERROR` | 已区分并列出两套名称的对应关系 |
| 5 | `sequence-diagrams.md` 曾声明「`SystemDesign.md` 尚不存在」（写作时确实尚未落盘） | 陈述过时 | 已更新为路径说明；并补 §8.1 文件导览（13 个可点击链接） |
| 6 | `sequence-diagrams.md` 出现重复的 `## 9. Mermaid 渲染说明` 章节 | 章节编号冲突 | 已删除重复块并把「相关文件导览」重编号为 §8.1 置于 §9 之前 |

### 4.3 继承自上游、影响设计文档的待确认问题

| 编号 | 来源 | 问题 | 当前默认（已在设计中生效） |
| --- | --- | --- | --- |
| **Q-01 / Q-02 / Q-03** | 需求 | 校区 / 交易区域 / 公共取货点的真实名称与清单 | 占位 `CAMPUS_A`、`COMMUNITY_A1`、`PICKUP_LIBRARY_A` 等；**替换不改路径与 DTO 结构** |
| **Q-04** | 需求 | 管理员账号来源 | 种子创建 `admin@example.com`（占位密码，禁止生产执行 V3） |
| **Q-05** | 需求 | 通知渠道是否仅站内 | 仅站内（`emailEnabled` 恒 false） |
| **Q-07** | 需求 | 是否需要 refresh token | 契约保留 `POST /auth/refresh`，默认返回 **501** |
| **Q-08** | 需求 | 已付款订单能否自助取消 | **不可**，需管理员介入（无退款能力） |
| **Q-13** | 需求 | 处罚期间「收尾操作」是否允许 | **允许**（假设 A-14），否则交易永久卡死 |
| **DQ-01** | 数据库 | 是否启用商品草稿态 | **不启用**，发布即 `IN_SALE` |
| **DQ-05** | 数据库 | 封禁能否延长 / 覆盖 | **固定 72 小时**且拒绝重复封禁 |
| **DQ-26** | 数据库 | `ACCEPTED` 阶段被下架时 `swap_codes` 是否置 `EXPIRED` | 当前**安全**（核销要求提议为 `ACCEPTED`），建议明确 |
| **DQ-27** | 数据库 | FR-604 扫描条件误纳终态 `BOTH_REVIEWED` | ✅ **已闭环**：`V4__fix_review_window_index.sql` + 契约同步修正 |
| **DQ-29 ~ DQ-32** | 本步提出 | ① `termination_reason`/`cancel_reason`/`lift_source` 枚举字面量未在 `schema.md` 明文列出；② 占用建立后卖家自行下架的语义；③ `ROLE_RESTRICTED`（角色）与 `RESTRICTED`（状态）措辞口径；④ `/internal/tasks/*` 调度周期与单批上限未规定 | 均**不阻塞**评审；建议在冻结前统一登记 |
| **AQ-01 ~ AQ-04** | 契约 | 通知开关纳入 / refresh 启用 / 字典是否需写接口 / 多类型处罚并存 | 已在契约中给出默认（见 `api-open-questions.md`） |

> **口径澄清（非缺陷）**：`state-machines.md` 用 `ROLE_RESTRICTED`/`ROLE_USER`（**角色名**），
> 而 `schema.md` 与实库用 `user_status = ACTIVE,RESTRICTED`（**状态值**）、`user_role = USER,ADMIN`（**角色值**）。
> 二者属「角色 vs 状态」两个层面，**不构成冲突**；建议在文档中显式说明措辞对应关系（已登记 DQ-31）。

---

## 5. 未覆盖内容与待补充项

| # | 未覆盖项 | 原因 / 归属 | 计划 |
| --- | --- | --- | --- |
| 1 | **业务逻辑实现** | 本步骤只做文档整合，明确不写代码 | 下一步按 `api-contract.md` 逐条实现 |
| 2 | **93 个业务错误码的代码落地** | `ErrorCode` 枚举当前仅 7 个通用码 | 下一步按 `error-codes.md` 逐字补齐 |
| 3 | **Service 层归属校验与处罚拦截**（P-00-A/B/C、E-02、E-03） | 接口层已用 `@PreAuthorize` 声明；归属校验属业务逻辑 | 下一步在 Service 层实现 |
| 4 | **`PageResult` 字段名与契约统一** | 现为 `page/size/total/items` + 契约别名 | 下一步统一为 `content/totalElements` |
| 5 | **Security 权限收紧** | 当前为骨架放开模式（`anyRequest().permitAll()`） | 下一步按 `api-permission-mapping.md` 收紧 |
| 6 | **前端实现** | 迭代一明确不做（`out-of-scope.md`） | 迭代二 |
| 7 | **生产级部署 / 监控 / 备份** | OOS-17 明确不做 | 迭代 N |
| 8 | **`SystemDesign.md` 未内嵌完整 ER 图**（只内嵌总览图，另 3 张详图在 `er-diagram.md`） | 避免文档过长与两处重复维护；已给出跳转链接 | 保持现状 |

### 5.1 精确性说明

| 编号 | 说明 |
| --- | --- |
| **P-01** ✅ **已闭环** | ~~`sequence-diagrams.md` 未直接引用 FR 编号（0 处），追溯需经 `api-contract.md` §3 两步跳转。~~ **第 7 步已完成优化**：9 张时序图说明表各新增 `覆盖 FR` 行、§8 交叉索引新增 `覆盖 FR` 列，共引用 **35 个 FR**（全部真实存在），追溯链变为**一步直达**。详见 §4.1.2。 |
| **P-02** | `SystemDesign.md` 只**内嵌 1 张 ER 总览图**，另外 3 张详图（占用 / 交换 / 治理）与基数汇总在 `er-diagram.md`。文档已给出明确跳转，避免重复维护两处 ER 图导致不一致。 |
| **P-03** | 本报告统计的「错误码引用数」（53 / 30）已对同一码的多次出现去重；`SystemDesign.md` 另有若干**错误码域前缀**（如 `AUTH_`、`SWAP_`）用于说明命名规范，不属错误码引用，已从统计中排除。 |

---

## 6. 验收标准自检

| # | 验收标准 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | `SystemDesign.md` 覆盖架构、领域模型、状态机与规则、权限、接口清单、不做清单、假设 | ✅ | 10 个一级章节：§1 概述范围、§2 架构（分层+拓扑+选型）、§3 领域模型（ER+实体+占用）、§4 状态机与规则（6 状态机+R-16~R-35+幂等并发）、§5 权限（7 角色+矩阵+P-00）、§6 接口清单（83 操作+54 FR+冻结声明+93 错误码）、§7 不做清单 18 项、§8 假设与待确认 |
| 2 | `sequence-diagrams.md` 至少 5 个时序图（购买/交换/治理/并发抢购/超时释放） | ✅ | **9 个 `sequenceDiagram` / 7 组流程**：§1 购买、§2 交换、§3 整笔作废、§4 治理、§5 并发抢购、§6 超时释放、§7 定时任务（3 图） |
| 3 | 所有图表与文字描述一致，无自相矛盾 | ✅ | 19 个 Mermaid 块语法校验通过（块开闭配平、引号成对）；状态转换编号、规则编号、错误码均已与上游逐条比对一致 |
| 4 | 交叉引用检查通过：表名、状态、错误码、operationId、FR 编号全部可追溯 | ✅ | §3.1 的 C-01~C-07 全部通过：表名 25/25、错误码 0 编造、operationId 0 多余、FR 0 未登记、转换编号 0 非法、规则 0 非法 |
| 5 | 权限说明与 `api-permission-mapping.md` 一致 | ✅ | 公开 10 / 需登录 73 / 仅管理员 14 / 系统内部 4，与上游 §3.8 及运行时 `/v3/api-docs` 一致（C-09） |
| 6 | 接口清单与 `api-contract.md` 一致，83 操作无遗漏 | ✅ | 契约 73 paths / 83 operations；13 模块统计与 `api-contract.md` §1 一致（C-13） |
| 7 | 不做清单与 `out-of-scope.md` 一致，至少 11 项且含 4 项核心 | ✅ | 18 项，编号集合完全一致，4 项核心（OOS-01/02/03/04）全覆盖（C-08） |
| 8 | 假设与待确认问题全部登记，不引入新的隐式假设 | ✅ | A-01~A-20、Q-01~Q-04 等、DQ-01~DQ-32、AQ-01~AQ-18、DDP-01 均已集中登记并给出落点；本步骤新增的 4 条（DQ-29~DQ-32）已显式登记 |
| 9 | 未写代码，未修改迁移脚本，未修改 OpenAPI 契约 | ✅ | 本步骤仅新增/修改 4 个 Markdown 文件；无 `.java`/`.vue` 新增；V1~V4 mtime 未变；`openapi.yaml` 仍 83 个 operationId（C-12 / C-13） |
| 10 | 完成报告包含文件路径、图表数量、流程覆盖、交叉引用检查结果、假设列表、验收自检结果 | ✅ | 本文件 §1（文件）、§2（图表与流程统计）、§3（交叉引用）、§4（矛盾与假设）、§5（未覆盖）、§6（自检） |

**通过 10 / 10。**

---

## 7. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 系统设计文档负责人 | 初稿：交付物清单与未修改证据、实测统计（含基准侧读数）、13 项交叉引用一致性检查、DDP-01 上游缺陷登记与复核澄清、本步骤已修正的 6 个问题、继承的待确认问题、未覆盖项与 3 条精确性说明、10 项验收自检 | 草稿（待确认） |
| v1.1 | 2026-10-12 | 交付负责人 | **第 7 步收尾更新**：DDP-01 已按授权修复（§4.1.1，`state-machines.md:189` R-22 → R-24）；P-01 已闭环（§4.1.2，时序图补 `覆盖 FR` 行/列，引用 35 个真实 FR） | 草稿（待确认） |

> **过程记录**：本文件曾存在一版基于「两份被检文档尚未落盘」中间态写成的草稿（该草稿本身无误，
> 但结论已过时）。已于定稿时按最终磁盘内容整体重写，避免留下与事实不符的结论。
