# 迭代二 广义契约 P0 裁决报告（Decision Log）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 P0 裁决报告 |
| 契约编号 | GC-DL |
| 版本 | v1.0 |
| 日期 | 2026-10-13 |
| 作者 | **技术契约裁决人（待填写姓名）** |
| 状态 | **已裁决并固化（DECIDED）** |
| 裁决范围 | 上一轮发现的 **9 条 P0 阻塞问题**：OQ-01、OQ-22、OQ-23、OQ-27、OQ-33、OQ-35、OQ-37、OQ-40、OQ-41；及关联矛盾 CQ-01、CQ-04、CQ-05、CQ-06、CQ-07、CQ-09 |
| 裁决原则 | **让文档描述运行时代码的实际行为，而不是让代码迁就文档**。**未修改任何 Java 代码、配置、迁移脚本或 `openapi.yaml`** |
| 落地文档 | GC-01 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md)（v1.1）、GC-02 [`data-conventions.md`](./data-conventions.md)（v1.1）、GC-03 [`security-and-cors.md`](./security-and-cors.md)（v1.1）、GC-04 [`notification-events.md`](./notification-events.md)（v1.1）、GC-05 [`mock-and-contract-testing.md`](./mock-and-contract-testing.md)（v1.1）、GC-99 [`open-questions.md`](./open-questions.md)（v1.1） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**

---

## 0. 裁决总览

| 序号 | 编号 | 裁决结论（一句话） | 类型 | 是否需变更契约 | 落地文档 |
| --- | --- | --- | --- | --- | --- |
| 1 | **OQ-22 / CQ-01** | **服从运行时 Jackson `NON_NULL`**：`null` 字段从响应中省略；前端按可选字段（`?`）容错 | **服从现状** | 否 | GC-02 §1 |
| 2 | **OQ-23 / CQ-05** | **22 个命名枚举 = 21 个业务字典 + 1 个系统级枚举（`UserRole`）** | 口径固定 | 否 | GC-02 §2.1 |
| 3 | **OQ-33 / CQ-06** | **以 `openapi.yaml` 为唯一真源**：`body` / `refType` / `refId` | 契约优先 | 否 | GC-04 §0 |
| 4 | **OQ-35 / CQ-07** | 两个缺口通知**迭代二不实现**，登记为**迭代五或未来版本需求** | 范围收缩 | 否 | GC-04 §2.1 / §3.1 |
| 5 | **OQ-01 / CQ-09** | 语义违反约束（含数组违反 `minItems`）**统一 422**，错误码 `VALIDATION_INVALID_PARAMETER` | 语义统一 | **是**（类型 D） | GC-01 §0.4 |
| 6 | **OQ-27** | **注册即登录**：注册成功即签发令牌 + 用户信息 | 体验优化 | **是**（类型 C） | GC-01 §2.1.1 |
| 7 | **OQ-37** | 工具链指定：`openapi-typescript` / `orval` / `Prism` / `Dredd` 或 `Schemathesis` | 工具指定 | 否 | GC-05 §1.2 / §3.1 / §4.1 |
| 8 | **OQ-40** | 联调使用 **V3 种子占位账号**（4 个），不新增账号 | 数据复用 | 否 | GC-05 §6.2 / GC-03 §6.4 |
| 9 | **OQ-41** | **迭代二暂不新增 `V5+` 迁移**，继续使用占位数据 | 范围收缩 | 否 | GC-01 §4 |
| — | **CQ-04** | 契约层 10/73/14/4 与注解层 10/55/14/4 **均正确、不构成矛盾** | 口径澄清 | 否 | GC-03 §2.2.1 |

**结论：9 条 P0 全部裁决完毕，迭代二可无歧义开工。**

---

## 1. 逐条裁决

### 裁决 1 —— OQ-22 / CQ-01：空值策略 **服从运行时 `NON_NULL`**

| 项 | 内容 |
| --- | --- |
| **裁决** | **服从运行时现状。** 由于后端全局配置了 Jackson `NON_NULL`（`application.yml` `spring.jackson.default-property-inclusion: non_null` + `JacksonConfig` `Include.NON_NULL`），**当字段值为 `null` 时，该字段会从 JSON 响应中省略**。**前端 TypeScript 类型应将相应字段标记为可选（`?`），并针对字段缺失做容错处理。** |
| **依据** | ① **不修改 Java 代码/配置**（本次红线）；② 修改全局序列化策略会波及全部 99 个 schema 的已实现响应，需全量回归，收益与成本不匹配；③ 前端类型配置为"可选 + `required` 字段恒有值"后，省略与返回 `null` 在调用方**完全等价**；④ 可减少约 20–30% 响应体积 |
| **强制补偿约定（由"建议"升为"必须"）** | **C1** 所有 `required` 字段永不为空值；**C2** 前端类型标记为 `field?: T`（**不是** `T \| null`），**禁止** `field!`；**C3** 读取前必须判空（可选链 `?.` / `??`）；**C4** 判定"无数据"**必须**用 `field === undefined`，**禁止** `field === null`（恒为 `false`）；**C5** 空集合 `[]` **必须返回**（当前配置为 `NON_NULL` 而**非** `NON_EMPTY`），`PageResult.content` 空页时必须为 `[]` |
| **落地** | [`data-conventions.md`](./data-conventions.md) §1（重写为裁决结论 + 5 条强制约定）；[`notification-events.md`](./notification-events.md) §7 示例 JSON 改为**无 `null` 值**形态（`actor` / `readAt` 缺失即表示无值）；[`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §3 |
| **影响面** | 全部 99 个 schema 的序列化行为；前端类型定义与所有解构/取值代码 |
| **是否需变更契约** | **否**（`openapi.yaml` 的 `nullable: true` 只声明"值可为 null"，与"字段是否省略"不冲突） |
| **状态** | ✅ **已裁决并固化** |

### 裁决 2 —— OQ-23 / CQ-05：枚举口径 **22 = 21 业务字典 + 1 系统级**

| 项 | 内容 |
| --- | --- |
| **裁决** | **保留 `openapi.yaml` 中 22 个命名枚举的现状**，定义为「**21 个业务字典 + 1 个系统级枚举（`UserRole`）**」。**共 22 个命名枚举，其中 21 个为业务字典（在 `glossary` 中定义），1 个为系统级枚举（`UserRole`，用于权限校验）。** |
| **计数恒等式** | `22 个命名枚举 = 21 个业务字典枚举 + 1 个系统级枚举（UserRole）`；另含 **7 组内联枚举**（I-1~I-7，见 GC-02 §2.10） |
| **依据** | ① 保留 `openapi.yaml` 现状即保留契约原样，**零变更成本**；② 历史两个数字**均正确**，只是口径不同：`AD-09` 的「21/21」指**21 个业务字典**与库级枚举一致；`controller-skeleton-verification.md` 的「22」指**命名枚举 Schema 总数**；③ 明确"系统级 vs 业务字典"的区分，对前端常量文件组织有实际指导意义 |
| **落地** | [`data-conventions.md`](./data-conventions.md) §2.1（口径唯一化 + 计数恒等式 + 历史数字处置）、§2.2 标题改为「1 个业务 + 1 个系统级」 |
| **影响面** | 前端枚举常量文件组织、文档统计口径、验收自检 |
| **是否需变更契约** | **否**（纯口径说明，不改 `openapi.yaml`） |
| **状态** | ✅ **已裁决并固化** |

### 裁决 3 —— OQ-33 / CQ-06：通知字段名 **以契约原文为准**

| 项 | 内容 |
| --- | --- |
| **裁决** | **以 `openapi.yaml` 为唯一真源。** 通知 JSON 结构**强制**使用 **`body`**、**`refType`**、**`refId`**。<br>**任务书中的字段名与契约不一致，前端必须以后者为准。字段映射关系为：`content` → `body`；`relatedId` → `refType` + `refId`。** |
| **映射表** | `content` → **`body`**（一个字段）；`relatedId` → **`refType` + `refId`**（**一个字段拆成两个**，两者必须**成对**出现或成对缺失，库级约束 `ck_notifications_ref_pair`） |
| **依据** | ① `body` / `refType` / `refId` **本就是契约原文**，契约侧零变更；② 改字段名属 **`api-freeze.md` §4.2 类型 E 破坏性变更**，**明令禁止**；③ 字段名只是命名差异，语义完全一致，**由任务书/文档口径对齐契约**是最低成本路径 |
| **禁止** | 前端**禁止**定义 `content` / `relatedId` 类型或读取这两个字段（将永远得到 `undefined`） |
| **落地** | [`notification-events.md`](./notification-events.md) §0（重写为裁决结论 + 强制映射表 + 注意事项原文）、§8 检查清单新增第 17 条 |
| **影响面** | 通知相关前端组件与类型；任务书/文档表述 |
| **是否需变更契约** | **否**（由任务书口径对齐契约） |
| **状态** | ✅ **已裁决并固化** |

### 裁决 4 —— OQ-35 / CQ-07：两个缺口通知 **迭代二不实现**

| 项 | 内容 |
| --- | --- |
| **裁决** | **暂不加入 `openapi.yaml`**，作为「**迭代五或未来版本需求**」登记。**迭代二不实现。**<br>**【未来迭代待定】取货点确认（`PICKUP_CONFIRMED`）、收到新评价（`NEW_REVIEW`）。这两个事件在契约中没有对应枚举，迭代二暂不实现，待迭代五评价系统完善后统一定义。** |
| **迭代二行为** | 两个事件**均不发通知**：<br>· **取货点确认** → 卖家通过订单详情可见取货点；`markOrderDelivered` 的前置条件 `ORDER_PICKUP_POINT_NOT_SELECTED`（409）会强制其关注；<br>· **收到新评价** → 被评价人可在订单/交换详情查看；已有 `REVIEW_WINDOW_CLOSED` 作为窗口收敛通知 |
| **依据** | ① 新增枚举值属 **类型 C**，需审批 + 台账 + 提版本，成本高于收益；② 两个事件都有可替代的信息获取路径；③ 迭代二范围（账户/商品/检索）不涉及评价系统完善 |
| **强制约束** | ① **后端禁止**为这两类事件写 `notifications` 记录（`notification_type` 为库级枚举，写入未定义值会**插入失败**，可能导致业务事务异常）；② **前端禁止**预留类型成员或 UI 分支；③ **禁止**用"临时枚举值"代替（如借用 `REVIEW_WINDOW_OPENED` 表达 `NEW_REVIEW`）；④ 未来启用**必须**走 `api-freeze.md` §4 流程（类型 C） |
| **占位类型名的地位** | `PICKUP_CONFIRMED` / `NEW_REVIEW` 是**未来占位名，不是契约枚举**，**当前禁止使用** |
| **落地** | [`notification-events.md`](./notification-events.md) §2.1（未来迭代登记 + 4 条强制约束）、§3.1（已裁决摘要）、§8 检查清单新增第 15/16 条 |
| **影响面** | 通知 UI 分支、后端通知落库点 |
| **是否需变更契约** | **否**（本次**不**新增枚举；未来启用时才需变更） |
| **状态** | ✅ **已裁决并固化** |

### 裁决 5 —— OQ-01 / CQ-09：数组违反 `minItems` **统一返回 422**

| 项 | 内容 |
| --- | --- |
| **裁决** | **统一为 422 Unprocessable Entity**（语义错误，语法正确）。<br>**当请求体结构正确（如 JSON 语法没问题），但语义违反约束（如 `imageUrls` 为空数组，违反 `minItems=1`），后端统一返回 422。400 仅用于请求体格式错误或 JSON 解析失败。** |
| **错误码（重要更正）** | 使用 **`VALIDATION_INVALID_PARAMETER`**（`error-codes.md` §3.2 已登记）。<br>**裁决文书中出现的「`VALIDATION_ERROR`」是后端 Java 枚举常量名，不是线上错误码** —— 实测 `ErrorCode.VALIDATION_ERROR("VALIDATION_INVALID_PARAMETER", HttpStatus.BAD_REQUEST, ...)`，**线上 `code` 为 `VALIDATION_INVALID_PARAMETER`**。前后端**一律按 `VALIDATION_INVALID_PARAMETER` 分支**，禁止使用 `VALIDATION_ERROR`。 |
| **统一规则** | **R1** 数组长度违反 `minItems`/`maxItems` → **一律 422**；**R2** JSON 无法解析/类型错误/必填缺失或为 `null` → 400；**R3** 字符串长度越界 → 400；**R4** 非法枚举 → 400；**R5** 语义层业务规则不满足（含 R1）→ 422；**R6** 错误码取自已登记的 93 个之一，无专属 422 码时复用 `VALIDATION_INVALID_PARAMETER` |
| **依据** | ① 消除"同一个 `[]` 在不同接口返回 400/422"的不一致；② 422 语义（"语法正确、语义不满足"）与本场景**完全吻合**；③ 已有 422 错误码（`SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE` 等）证明团队已按 422 设计该类场景，本裁决把**未覆盖的接口统一过来** |
| **落地** | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §0.4（新增「400 与 422 的划分」+ R1~R6 + 错误码澄清）、§2.2 / §2.3 / §2.5 / §2.7 / §2.8 全部数组边界行逐条改为 422 |
| **覆盖的字段** | `createItem.imageUrls`、`updateItem.imageUrls`、`setMyTradeZones.tradeZoneIds`、`submitOrder.cartItemIds`、`createSwapBundle.itemIds`、`updateSwapBundle.itemIds`、`createSwapProposal.targetItemIds`、`counterSwapProposal.targetItemIds` |
| **⚠️ 残留实现差异（IMP-01）** | 当前运行时 `GlobalExceptionHandler` 把参数校验统一映射为 `VALIDATION_INVALID_PARAMETER` + **HTTP 400**（`ErrorCode` 中**不存在** `HttpStatus.UNPROCESSABLE_ENTITY` 映射，实测 0 处）。**消除该差异属于后端实现任务**（需为语义校验新增 422 映射），**不属本次"只改 Markdown"的范围**。**在落地前，前端必须同时容错 400 与 422，并按 `code` 分支，不得按 HTTP 状态码硬编码。** |
| **是否需变更契约** | **是**（错误码语义层，`api-freeze.md` §4.2 **类型 D 缺陷修正**）；**本次不修改 `openapi.yaml`** |
| **状态** | ✅ **已裁决并固化**（实现侧 IMP-01 待后端跟进） |

### 裁决 6 —— OQ-27：**注册即登录**

| 项 | 内容 |
| --- | --- |
| **裁决** | **注册成功后直接签发令牌（自动登录）**，以提升前端体验。<br>**注册成功后，响应体中包含令牌与用户信息，用户无需再次调用 `login` 接口。该行为与 `login` 接口保持一致，前端应直接存储令牌并跳转首页。** |
| **令牌字段** | 与 `login` **完全同构**：`tokenType`（恒 `"Bearer"`）、`accessToken`、**`expiresIn`（`604800`）**、`expiresAt`；`refreshToken` **不返回**（AQ-02 未启用） |
| **用户信息字段** | 与 `login` / `getCurrentUser` 同构，至少含 `id`、`username`、`nickname`、`role`、`status`、`tradeZones` |
| **成功状态码** | **`201 Created`**（与 `openapi.yaml` 一致，不变） |
| **依据** | ① 提升前端体验（注册后无需二次请求）；② 与 `login` 行为一致，前端可复用同一套令牌存储逻辑；③ 减少一次网络往返与一个失败点 |
| **⚠️ 契约落点差异（CQ-11，**必须知情**）** | `openapi.yaml` 的 `registerUser` **当前冻结定义**为 `201` + `data: CurrentUserResponse`，其 description **明文写着「注册成功后不自动签发令牌，需调用 `/auth/login`」**（**不含令牌**）。**本裁决与该响应形状不一致。** 由于本次任务**禁止修改 `openapi.yaml`**，落地路径为三轨：<br>① **契约侧**：需按 `api-freeze.md` §4 提交变更（`data` 由 `CurrentUserResponse` 扩展为「令牌 + 用户信息」，属 **类型 C 向后兼容扩充**）并登记台账 —— **本次不执行**；<br>② **实现侧**：后端待实现时**必须**同时返回令牌与用户信息；契约变更通过前**不得**擅自新增响应字段；<br>③ **前端侧（过渡期强制容错）**：**若响应中存在 `accessToken` → 直接登录；若不存在 → 回退调用 `login`。** 前端**禁止**硬依赖令牌存在。 |
| **阻塞性** | **不阻塞**迭代二开工（前端按第 ③ 条容错即可联调）；但**契约变更必须在迭代二内完成**，否则契约与实现长期漂移 |
| **落地** | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §2.1.1（新增，含三轨落地与过渡期容错） |
| **是否需变更契约** | **是**（类型 C，需接口负责人 + 后端负责人审批 + 台账登记）；**本次不修改 `openapi.yaml`** |
| **状态** | ✅ **已裁决并固化**（契约变更与实现待跟进） |

### 裁决 7 —— OQ-37：工具链**已指定，不锁定版本**

| 项 | 内容 |
| --- | --- |
| **裁决** | **前端类型生成使用 `openapi-typescript`，API Client 使用 `orval`；Mock Server 使用 `Prism`（基于 `openapi.yaml` 启动，支持正常/边界/错误场景）；契约测试使用 `Dredd` 或 `Schemathesis`（对比运行时 `/v3/api-docs` 与 `openapi.yaml`）。** |
| **工具清单** | 类型 → **`openapi-typescript`**；Client → **`orval`**；Mock → **`Prism`**（**裁决指定项**）；契约测试 → **`Dredd`** **或** **`Schemathesis`**（二者择一）；辅助校验 → `openapi-spec-validator` / `@apidevtools/swagger-parser` |
| **版本策略** | **不锁定具体版本**；但工具的实际版本**必须**写入 `package.json` / CI 依赖文件并**提交锁文件**，避免生成结果漂移 |
| **口径变更** | v1.0 的「`MSW` 为主 + 自研 `ajv` 脚本比对」推荐被**取代**：`MSW` **降级为可选**（仅组件测试/单测，不纳入契约测试链路）；**契约测试的权威执行方式为 `Dredd` / `Schemathesis`**；自研静态比对脚本（GC-05 §4.2 六维度）**为可选辅助** |
| **依据** | ① `Prism` 直接消费 `openapi.yaml`，**与唯一真源零漂移**；② `Dredd` / `Schemathesis` 直接对运行时发请求并按 schema 断言，**不需要自研胶水代码**；③ 只指定工具不锁版本，兼顾可复现与升级灵活性 |
| **落地** | [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) §1.2（工具链表 + 版本策略 + 口径澄清）、§3.1（Mock 形态：Prism 为裁决指定项）、§4.1（执行工具）、§4.4（执行与报告约定） |
| **影响面** | 前端工程脚手架、CI 流水线、Mock 维护成本 |
| **是否需变更契约** | **否**（工具链约定，不涉及 `openapi.yaml`） |
| **状态** | ✅ **已裁决并固化** |

### 裁决 8 —— OQ-40：联调使用 **V3 种子占位账号**

| 项 | 内容 |
| --- | --- |
| **裁决** | **使用 `V3__seed_development_data.sql` 中的占位账号，不新增账号。**<br>**联调测试账号暂使用 V3 种子数据中的 `admin@example.com`（管理员）和 3 个普通用户账号。密码为占位密码，待 Q-04 确认后再替换为真实账号体系。V3 脚本禁止在生产环境执行。** |
| **实际账号清单（已核对脚本，共 4 个）** | `admin` / **`admin@example.com`**（`ADMIN`）；`alice` / `alice@example.com`；`bob` / `bob@example.com`；`carol` / `carol@example.com` |
| **被处罚用户** | **不预置**：由 `admin` 现场封禁 `carol` 产生（可同时验证封禁流程与 `403 USER_RESTRICTED_CANNOT_WRITE` + `restrictedUntil`） |
| **依据** | ① 复用既有种子数据，**零新增成本**；② 3 个普通用户足以覆盖"卖家 / 买家 / 第三方越权"三类用例；③ 不新增迁移（与 OQ-41 一致）；④ V3 已明文声明"仅用于本地开发与联调，禁止用于生产" |
| **事实更正 ①** | V3 实际为 **1 个管理员 + 3 个普通用户**（`admin` / `alice` / `bob` / `carol`），**不是**"4 个普通用户账号"。**不阻塞**（3 个普通用户满足全部越权用例） |
| **⚠️ 事实更正 ②（IMP-02，**阻塞 P2 联调**）** | V3 的 `password_hash` 是**格式不合法的占位 bcrypt 串**（`$2a$10$PLACEHOLDERHASH…`，实测 **62 字符**，合法 bcrypt 为 **60 字符**），**无法匹配任何明文口令**。V3 文件头注释称"真实哈希在后端工程骨架步骤由应用初始化逻辑写入或重置"，但**该初始化逻辑当前未实现** → **这 4 个账号当前均无法登录，P2 联调无法开始**。<br>**处置（三选一，均属后端实现任务，且不得修改 V1~V4）**：① 实现"占位哈希重置"初始化能力；② 提供一次性重置脚本；③ 由 Q-04 确定真实账号体系后替换 |
| **落地** | [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) §6.2（完整清单 + 两项事实更正）、[`security-and-cors.md`](./security-and-cors.md) §6.4 |
| **环境红线** | **V3 脚本禁止在生产环境执行**；**禁止**把生产/真实用户数据用于联调 |
| **是否需变更契约** | **否**（测试数据约定） |
| **状态** | ✅ **已裁决并固化**（**不影响 P0 开工**；P2 联调前须解决 IMP-02） |

### 裁决 9 —— OQ-41：**迭代二暂不新增 `V5+` 迁移**

| 项 | 内容 |
| --- | --- |
| **裁决** | **迭代二暂不新增 `V5+` 迁移**，继续使用 V1~V4。<br>**迭代二开发期间，继续使用 `CAMPUS_A`、`PICKUP_LIBRARY_A` 等占位数据。待 Q-01~Q-03 确认后，再新增 `V5__update_placeholder_zones.sql` 之类的迁移进行替换。在此之前，禁止直接修改 V1~V4。** |
| **迁移约束** | 只保留 **V1~V4**；V1~V4 的 SHA-256 必须与 `docs/iteration1/FREEZE.md` §1.2 一致；**禁止修改 V1~V4**；**禁止**在执行过的迁移上追加内容 |
| **真实数据替换路径** | 待 **Q-01/Q-02/Q-03** 确认后，**新增** `V5__update_placeholder_zones.sql` 之类的**新迁移**做**数据替换**（**不是**修改 V1~V4）；替换时**不得改变字段名、类型、路径与 DTO 结构** |
| **前端约束** | **禁止**硬编码 `CAMPUS_A` 等占位编码或"校区 A"等占位名称；**必须**从 `GET /trade-zones`、`GET /trade-zones/{id}/pickup-points` 动态获取 |
| **依据** | ① 占位数据替换属 `api-freeze.md` §4.2 **类型 G（不属变更）**，可延后；② 避免在执行过的迁移链上引入"修改历史"的反模式；③ 避免种子数据与实现漂移；④ 新增迁移属数据库变更流程，应待需求确认后再做 |
| **落地** | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §4（新增「占位数据与迁移约束」） |
| **是否需变更契约** | **否**（本次不新增迁移；未来替换占位数据属类型 G） |
| **状态** | ✅ **已裁决并固化** |

### 补充裁决 —— CQ-04：权限计数**双口径均正确**

| 项 | 内容 |
| --- | --- |
| **裁决** | **两者都正确，属于不同抽象层级的计数。**<br>**契约层面统计为 10 公开 / 73 需登录 / 14 管理员 / 4 系统；注解层面统计为 10 `permitAll` / 55 `isAuthenticated` / 14 `hasRole('ADMIN')` / 4 `hasRole('SYSTEM')`。差异原因在于部分接口通过路径匹配而非 `@PreAuthorize` 实现认证，两者均正确，不构成矛盾。** |
| **差异根因** | ① `55` 与 `73` 的差额（**18 个操作**）在于部分接口的认证**由路径匹配规则**（`SecurityConfig` / `PUBLIC_ENDPOINTS` / 网关）或**本人校验**（Service 层归属判断，如 `AUTH_NOT_RESOURCE_OWNER`）承担，**未**使用 `isAuthenticated` 表达式；② 两个层级统计的**对象不同**：契约层统计"契约要求"，注解层统计"当前骨架写法"；③ **`14` 管理员与 `4` 系统完全一致，`10` 公开也完全一致** |
| **实现约束** | 迭代二收紧权限时**必须以契约层（10/73/14/4）为对齐目标**，逐条落实 `api-permission-mapping.md` §2 与 §1.2 判定顺序；**不得**以"注解层已有 55 个 `isAuthenticated`"为由认为权限已完备（`SecurityConfig` 当前仍为骨架放开模式 `anyRequest().permitAll()`） |
| **落地** | [`security-and-cors.md`](./security-and-cors.md) §2.2.1（新增「契约层与注解层双口径」+ 差异根因表 + 实现约束） |
| **是否需变更契约** | **否** |
| **状态** | ✅ **已裁决并固化** |

---

## 2. 裁决过程中**新发现**的问题（已登记，未在本轮裁决）

| 编号 | 类型 | 内容 | 影响 | 状态 |
| --- | --- | --- | --- | --- |
| **CQ-11** | 矛盾（**P0 级**） | `openapi.yaml` 的 `registerUser` 响应（`CurrentUserResponse`，**不含令牌**）与 **OQ-27「注册即登录」裁决**不一致 | 契约与实现将长期漂移 | ⏳ **未闭环**：需走 `api-freeze.md` §4（**类型 C**）；过渡期前端双兼容 |
| **CQ-12** | 命名混淆（P2） | 裁决文书的「`VALIDATION_ERROR`」是**后端 Java 枚举常量名**，**不是线上错误码**（线上为 `VALIDATION_INVALID_PARAMETER`） | 前端若误用会拿不到匹配值 | ✅ **已闭环**：文档侧已澄清（GC-01 §0.4） |
| **IMP-01** | 实现阻塞（P1） | `GlobalExceptionHandler` 把参数校验统一映射为 **HTTP 400**；`ErrorCode` 中**不存在** `HttpStatus.UNPROCESSABLE_ENTITY` 映射（实测 0 处），与 OQ-01「语义 422」裁决冲突 | 前端若按 HTTP 状态码分支会误判 | ⏳ **未闭环**：后端需为语义校验新增 422 映射；**在此之前前端必须同时容错 400 与 422** |
| **IMP-02** | 实现阻塞（**P2，阻塞联调**） | V3 种子账号的占位 bcrypt 哈希**非法**（62 字符，合法为 60），**无法匹配任何口令** → `admin`/`alice`/`bob`/`carol` **当前均无法登录** | **P2 联调无法开始** | ⏳ **未闭环**：后端需实现"占位哈希重置"或提供重置脚本（**不得修改 V1~V4**） |

---

## 3. 裁决执行清单（本次实际改动）

| # | 文件 | 版本 | 改动 |
| --- | --- | --- | --- |
| 1 | [`data-conventions.md`](./data-conventions.md) | v1.0 → **v1.1** | §1 空值策略重写为**裁决结论 + 5 条强制约定**（原「建议」语气全部取消）；§2.1 枚举口径唯一化为 **22 = 21 + `UserRole`**；§2.2 标题调整；版本记录 + 状态升级 |
| 2 | [`notification-events.md`](./notification-events.md) | v1.0 → **v1.1** | §0 字段名裁决 + **强制映射表**；**新增 §2.1** 「未来迭代待定」两个占位通知 + 4 条强制约束；§3.1 改为已裁决摘要；§7 示例 JSON 改为**省略 `null`** 形态；§8 检查清单 14 → **17 项** |
| 3 | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) | v1.0 → **v1.1** | **新增 §0.4** 「400 与 422 的划分」（R1~R6 + 错误码澄清）；§2.2/§2.3/§2.5/§2.7/§2.8 全部数组边界行改为 **422**；**新增 §2.1.1** 「注册即登录」+ 三轨落地；**新增 §4** 「占位数据与迁移约束」；§3 按 `NON_NULL` 改写；§5 待确认索引移除已裁决项 |
| 4 | [`security-and-cors.md`](./security-and-cors.md) | v1.0 → **v1.1** | **新增 §2.2.1** 「契约层与注解层双口径」；**新增 §6.4** 「联调测试账号」 |
| 5 | [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) | v1.0 → **v1.1** | §1.2 工具链表重写（`Prism` / `Dredd` / `Schemathesis` 指定，`MSW` 降级）；§3.1 Mock 形态改写；§4.1 增加执行工具；§4.4 重写执行与报告约定；§6.2 重写为已裁决账号清单 + 两项事实更正 |
| 6 | [`open-questions.md`](./open-questions.md) | v1.0 → **v1.1** | §0.1 数量总览（已裁决/未裁决）；§1.1 九条 P0 全改为「✅ 已裁决」；§2 CQ 标记裁决 + **新增 CQ-11 / CQ-12**；**新增 §2.2** IMP 实现阻塞项；§4 裁决记录填入 9 条；§5 收敛路径改为进度表 |
| 7 | [`decision-log.md`](./decision-log.md) | **新增** | 本文件 |

**未改动（红线遵守）**：`src/**`（含 `application*.yml`）、`src/main/resources/db/migration/V1~V4`、`docs/iteration1/design/openapi.yaml`、`docker-compose.yml`、`Dockerfile`、`pom.xml`、`docs/iteration1/**`。

---

## 4. 待跟进事项（**不阻塞迭代二开工**）

| # | 事项 | 类型 | 负责方 | 优先级 |
| --- | --- | --- | --- | --- |
| 1 | `registerUser` 响应扩展为「令牌 + 用户信息」（**CQ-11**） | 契约变更（类型 C，需 2 人审批 + 台账） | 接口契约负责人 + 后端负责人 | **高**（迭代二内完成） |
| 2 | 语义校验统一 422（**OQ-01** 的契约侧登记 + **IMP-01** 的实现侧映射） | 契约变更（类型 D）+ 后端实现 | 接口契约负责人 + 后端负责人 | 中 |
| 3 | V3 占位哈希重置，使 4 个种子账号可登录（**IMP-02**） | 后端实现（**不得修改 V1~V4**） | 后端负责人 | **高（阻塞 P2 联调）** |
| 4 | 裁决 §1.2 的 **20 条 P1** | 逐条裁决 | 各功能负责人 | 中（功能开工前） |
| 5 | 裁决 §1.3 的 **13 条 P2** | 逐条裁决 | 各功能负责人 | 低（可与联调并行） |
| 6 | Q-01~Q-04（**需求侧 P0，仍未答复**） | 需求确认 | 需求方 | **高**（阻塞占位数据替换与管理员账号体系） |

---

## 5. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.0** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | 初版：9 条 P0 裁决（OQ-01 / OQ-22 / OQ-23 / OQ-27 / OQ-33 / OQ-35 / OQ-37 / OQ-40 / OQ-41）+ 补充裁决 CQ-04；每条含裁决结论、依据、落地文档、影响面、是否需变更契约；新发现 CQ-11 / CQ-12 / IMP-01 / IMP-02；裁决执行清单；待跟进事项 | **已裁决并固化（DECIDED）** |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
