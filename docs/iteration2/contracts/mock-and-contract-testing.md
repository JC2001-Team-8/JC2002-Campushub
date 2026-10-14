# 迭代二 广义契约 ⑤：Mock 与契约测试（Mock & Contract Testing）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 ⑤：Mock 与契约测试 |
| 契约编号 | GC-05 |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— P0 项（OQ-37 / OQ-40）**已裁决并固化**；未裁决项以【待确认】标注 |
| 上游依据 | `docs/iteration1/design/openapi.yaml`（冻结候选，83 操作 / 99 schema / 93 错误码）、`docs/iteration1/design/api-freeze.md`、`docs/iteration1/design/controller-skeleton-verification.md`、`docs/iteration1/design/error-codes.md`、`ITERATION1_TO_ITERATION2_HANDOVER.md` §4.4 |
| 适用迭代 | 迭代二及后续（前后端并行开发期） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
>
> **定位**：定义「前端在没有后端时怎么开发、有后端后怎么验证没跑偏」。
> 核心原则：**`openapi.yaml` 是唯一真源**；Mock 由它生成，契约测试拿它比对；**任何差异必须登记，不得私自改契约**。

---

## 1. 唯一真源与工具链

### 1.1 真源与派生关系

```text
docs/iteration1/design/openapi.yaml   ← 唯一真源（已冻结，禁止修改）
        │
        ├──(代码生成)──► 前端 TS 类型（types）
        ├──(代码生成)──► 前端 API Client（含请求/响应类型、路径、方法）
        ├──(Mock 生成)─► Mock Server（正常 / 边界 / 错误场景）
        └──(比对目标)──► 契约测试（后端 /v3/api-docs ↔ openapi.yaml）
```

### 1.2 工具链 —— **已裁决（OQ-37）：工具已指定，不锁定版本**

> **裁决（2026-10-13）**：**指定具体工具，不锁定具体版本。**

> **前端类型生成使用 `openapi-typescript`，API Client 使用 `orval`；
> Mock Server 使用 `Prism`（基于 `openapi.yaml` 启动，支持正常/边界/错误场景）；
> 契约测试使用 `Dredd` 或 `Schemathesis`（对比运行时 `/v3/api-docs` 与 `openapi.yaml`）。**

| 用途 | **已裁决的工具** | 说明 |
| --- | --- | --- |
| **TS 类型生成** | **`openapi-typescript`** | 只生成类型，**不生成运行时代码**；产物提交，或在 `prebuild` 生成 |
| **API Client 生成** | **`orval`** | 可同时产出 TanStack Query hooks 与 MSW mock；与 Vue 集成需少量适配 |
| **Mock Server** | **`Prism`**（`@stoplight/prism-cli`） | `prism mock openapi.yaml`，**基于 `openapi.yaml` 启动**，支持**正常 / 边界 / 错误**三类场景；可 `--errors` 模拟 4xx/5xx |
| **契约测试** | **`Dredd`** **或** **`Schemathesis`**（二者择一即可） | 对比**运行时 `/v3/api-docs`** 与 **`openapi.yaml`**；`Dredd` 偏"契约即测试用例"，`Schemathesis` 偏"基于 schema 的属性测试" |
| **OpenAPI 校验**（辅助） | `openapi-spec-validator` 或 `@apidevtools/swagger-parser` | 迭代一已使用 `openapi-spec-validator`；**属辅助校验，非契约测试主体** |

> **版本策略（明确）**：**不锁定具体版本**。但**必须**把所选工具的实际版本写入前端 `package.json` / 后端 CI 依赖文件并**提交锁文件**，避免生成结果漂移。
> **禁止**在生成流程中「顺手修改」生成产物中的类型（除注释与格式化外）；如类型与需求不符，**改契约或登记问题**，不改产物。
>
> **口径澄清**：本节**取代** v1.0 中"`MSW` 为主 + 自研脚本比对"的**推荐性**表述。
> `MSW` **仍可用于**组件测试与离线开发（**可选，不作为裁决指定项**）；**契约测试的权威执行方式为 `Dredd` 或 `Schemathesis`**。
> **说明**：`Prism` 依赖 `openapi.yaml` 的 `example`（弱冻结项），故 §4.2 明确规定**契约测试只比对 `schema`，不比对 `example`**，二者分工不冲突。

### 1.3 前端仓库结构建议（【待确认 OQ-38】仓库位置）

| 目录 | 内容 | 是否提交 |
| --- | --- | --- |
| `src/api/generated/` | `openapi-typescript` 生成的 `types.ts` | ✅ 提交（保证 CI 可复现） |
| `src/api/client/` | `orval` 生成的 client / hooks | ✅ 提交 |
| `src/mocks/handlers/` | MSW handlers（按 tag 分文件，如 `items.ts`、`orders.ts`） | ✅ 提交 |
| `openapi/openapi.yaml` | **从后端仓库同步的契约副本** | ✅ 提交（记录同步来源的 commit） |
| `scripts/sync-openapi.mjs` | 同步脚本（复制 + 校验 + 打印哈希） | ✅ 提交 |

> **同步约束**：前端契约副本**必须**附带**来源 commit 哈希**（写入 `openapi/README.md`），
> 且**禁止**在前端副本上做任何修改。若需修改 → 走后端**变更流程**（§5）。

---

## 2. 从契约生成前端类型与 Client

### 2.1 生成命令（示例，具体脚本名待定）

```bash
# 1) 校验契约合法性
npx @apidevtools/swagger-parser validate openapi/openapi.yaml

# 2) 生成 TS 类型
npx openapi-typescript openapi/openapi.yaml -o src/api/generated/types.ts

# 3) 生成 API Client / hooks
npx orval --config orval.config.ts
```

### 2.2 生成约定

| # | 约定 |
| --- | --- |
| 1 | **每次契约同步后必须重新生成**，并把生成产物一并提交（便于 review 差异） |
| 2 | 生成产物**顶部自动加注释**（来源文件 + 版本 + 生成时间 + 来源 commit） |
| 3 | **禁止手改**生成产物；如需本地适配 → 在 `src/api/adapters/` 写**包装层** |
| 4 | 类型命名以 `components.schemas` 的 schema 名为准（`ItemDetail`、`OrderSummary`…），**不重命名** |
| 5 | 枚举 → TS **字符串字面量联合类型**（而非 TS `enum`），便于 `switch` 收窄 |
| 6 | `nullable: true` 的字段 → `T \| null`；非 `required` 字段 → 可选（**与空值策略联动**，见 [`data-conventions.md`](./data-conventions.md) §1 / OQ-22） |
| 7 | 金额字段（`DecimalString`）→ 生成结果应为 `string`，**禁止**在适配层转 `number` |
| 8 | `int64` → `number`；**前端不得对 ID 做算术**（[`data-conventions.md`](./data-conventions.md) §5） |

### 2.3 `operationId` 命名与前端调用

| 项 | 约定 |
| --- | --- |
| 前端方法名 | 建议直接采用 `operationId`（如 `listItems`、`createItem`），**不另起别名** |
| 覆盖范围 | 迭代二实现的 **28 个** `operationId`（账户 6 + 个人资料 5 + 字典 4 + 商品 10 + 举报 3 = 28，见 `handover` §5.2）**必须有**生成的方法；其余 55 个也生成（可暂不调用） |
| 路径前缀 | `servers[0].url = http://localhost:8080/api/v1` → 生成的 client `baseURL` 须可配置（**唯一允许改动的运行时配置**） |
| 类型校验 | 每个调用的响应类型**必须**来自生成类型，**禁止**手写 `any` 或重复定义 DTO |

---

## 3. Mock Server 约定

### 3.1 Mock 形态（**以 Prism 为裁决指定项**）

| 形态 | 工具 | 适用场景 | 数据来源 | 裁决地位 |
| --- | --- | --- | --- | --- |
| **A. 独立 Mock 进程** | **`Prism`** | **前端独立开发、演示、联调前验证**；**正常 / 边界 / 错误**三类场景 | `openapi.yaml` 的 `example` + `examples`；无 `example` 的字段由 Prism 按类型生成 | ✅ **OQ-37 裁决指定** |
| **B. 浏览器内 Mock** | `MSW` | 组件测试、单测、离线开发（**可选**） | 手写 handlers + 契约类型约束；数据来自 fixtures | ➖ 可选，非裁决指定项 |

> **OQ-37 裁决后的口径**：**Mock Server 使用 `Prism`**（基于 `openapi.yaml` 启动）。
> `MSW` **不取消**，但**降级为可选**，仅用于组件测试/单测场景；**不纳入**契约测试链路。
> **OQ-39（是否同时维护两套 Mock）为 P1，仍未裁决**；在裁决前，**以 `Prism` 为唯一必需的 Mock 形态**，`MSW` 按需自建、不阻塞交付。

### 3.2 覆盖要求（**必须**，非可选）

| 场景类别 | 覆盖要求 | 示例 |
| --- | --- | --- |
| **正常场景** | 每个被前端调用的 `operationId` **至少 1 条** | `createItem` → 201 + `ItemDetail` |
| **空数据场景** | 每个列表接口 **至少 1 条**空结果 | `listItems` → `content: []`、`totalElements: 0` |
| **边界场景** | 每个关键边界值 **至少各 1 条** | `size=100`（最大）、`size=101`（→ 400）、`imageUrls` 恰好 1 张 / 9 张、`targetItemIds` 恰好 1 件 |
| **错误场景** | **每个前端需要分支处理的 `code` 至少 1 条** | `401 AUTH_TOKEN_EXPIRED`、`403 USER_RESTRICTED_CANNOT_WRITE`（带 `restrictedUntil`）、`409 ITEM_ALREADY_RESERVED`（带 `currentStatus`）、`409 COMMON_VERSION_CONFLICT`（带 `currentVersion`）、`422 TRADE_ZONE_MISMATCH`、`429 COMMON_RATE_LIMITED`（带 `Retry-After`）、`500 COMMON_INTERNAL_ERROR` |
| **幂等场景** | 同 `Idempotency-Key` 重复提交 **至少 1 条** | `submitOrder` 第二次返回**同一订单**（**E-06 回放，不是错误**） |
| **延迟/超时** | **至少 1 条**慢响应（如 3s） | 验证前端 loading / 超时提示 |
| **通知场景** | 25 个 `NotificationType` **各 1 条** | 验证前端 `type` 分支与跳转（[`notification-events.md`](./notification-events.md) §2） |

### 3.3 Mock 数据必须遵守的契约约束

| # | 约束 | 依据 |
| --- | --- | --- |
| 1 | 响应包络为 `ApiResponse`（`code`/`message`/`data`/`traceId`/`timestamp`）；成功 `code` 恒为 `"OK"` | AD-01 |
| 2 | 失败响应为 `ErrorResponse`（`code`/`message`/`details`/`traceId`/`timestamp`），**HTTP 状态必须非 2xx** | AD-01 |
| 3 | 枚举值**必须**来自 [`data-conventions.md`](./data-conventions.md) §2 的字典 | AD-09 |
| 4 | 金额**必须**为字符串 `"120.00"` | AD-04 |
| 5 | 时间**必须**为 ISO 8601 UTC（`...Z`） | AD-05 |
| 6 | `PageResult` 的 7 个字段**必须全部存在** | `PageResult.required` |
| 7 | **禁止**在 Mock 中出现 `password_hash` / `deleted_at` / `created_by` / `updated_by` | AD-08 |
| 8 | `traceId` 在响应头 `X-Trace-Id` 与响应体中**保持一致** | AQ-16 |
| 9 | 枚举与错误码的**累计覆盖清单**需在 Mock 目录留一份 `coverage.md`（记录哪些 code/type 已被 Mock 覆盖） | 本文档 §3.2 |

### 3.4 Mock 的局限（必须知情）

| # | 局限 | 后果 | 缓解 |
| --- | --- | --- | --- |
| 1 | Mock **不执行业务规则** | 「已占用商品不可下单」只有状态校验，Mock 不保证 | Mock handler 内**手工模拟**状态机（用内存状态），或尽早切真实后端 |
| 2 | Mock **不保证跨接口一致性** | 下单后商品状态不会自动变 | MSW handler 共享一份**内存 store**，在 handler 里改状态 |
| 3 | Mock **不校验并发** | 并发抢购无法在 Mock 层验证 | 并发验证只在真实后端做（§6 验收标准） |
| 4 | Mock 的 `example` 可能**与契约漂移** | Prism 基于 `example`，`example` 是弱冻结项 | 契约测试只比 `schema`，不比 `example`（§4.2） |
| 5 | Mock **没有真实鉴权** | 前端可能漏带 `Authorization` 也能通过 | Mock 必须**校验 `Authorization` 存在性**，未带 → 401 |

---

## 4. 契约测试（Contract Testing）

### 4.1 目标与会话前提

| 项 | 约定 |
| --- | --- |
| 目标 | 证明**运行中的后端**与 `openapi.yaml` 的对外契约**一致** |
| **执行工具（OQ-37 裁决指定）** | **`Dredd`** **或** **`Schemathesis`**，二者**择一**即可。<br>· `Dredd`：以 `openapi.yaml` 为用例源，逐 endpoint 实际调用并与 schema 断言比对；<br>· `Schemathesis`：基于 schema 的属性测试（可自动生成边界值），覆盖更广、报告更细 |
| 前提 | 后端可启动（Docker Compose 或本机运行），Flyway V1~V4 成功 |
| 比对两端 | **A 端**：后端运行时文档 `GET /v3/api-docs`（springdoc 输出）<br>**B 端**：`docs/iteration1/design/openapi.yaml`（冻结契约） |
| 触发时机 | ① 迭代二每个功能合并前；② 每次 `operationId` / DTO 变更后；③ 发版前 |
| 产物 | 《契约一致性报告》：差异清单 + 判定（契约错 / 实现错 / 弱冻结项）+ 处置结论 |

> **工具选择补充说明**：`Dredd` / `Schemathesis` 直接消费 `openapi.yaml` 并对运行时发请求，**不需要自研对比脚本**（v1.0 中"自研 `ajv` 脚本"的表述已由 OQ-37 裁决取代）。
> 若团队希望保留 **§4.2 的 6 维度静态比对**（`openapi.yaml` ↔ `/v3/api-docs` 的**文档级**差异），可在 `Dredd`/`Schemathesis` 之外**另行**开发辅助脚本；**该脚本为可选项**，不再作为契约测试的必需项。

### 4.2 比对维度（**6 项，逐项机器可比**）

| # | 维度 | 比对方法 | 差异判定 |
| --- | --- | --- | --- |
| 1 | **operationId 集合** | 两端 operationId 排序后做集合运算 | 数量应为 **83**（[`security-and-cors.md`](./security-and-cors.md) §2.2）；缺失/多余 → **必须**登记 |
| 2 | **路径 + 方法** | 归一化（去尾斜杠、参数名占位符统一）后逐条比对 | 不一致 → **必须**登记 |
| 3 | **请求/响应 DTO 字段与类型** | 递归比对 `components.schemas`（字段名、类型、`required`、`enum`、`format`、长度/范围约束） | 差异 → **必须**登记；`required` 收窄、类型变更 → **破坏性** |
| 4 | **枚举取值** | 逐枚举取值集合比对（应 **22 个命名枚举 + 7 组内联**） | 取值漂移 → **必须**登记（AD-09：与 DB 枚举 21/21 一致） |
| 5 | **错误码** | 收集 `/v3/api-docs` 中响应 `example`/`schema` 出现的 `code`，与 `error-codes.md` 的 **93 个**比对 | 出现**未登记**的错误码 → **必须**登记 |
| 6 | **权限声明** | 比对 `x-required-roles` 与 `security`（应 **10 公开 / 73 需登录 / 14 ADMIN / 4 SYSTEM**） | 不一致 → **必须**登记 |

### 4.3 明确**不**比对的内容（避免误报）

| 项 | 原因 |
| --- | --- |
| `info.version`（`1.0.0-frozen-candidate` vs 运行时） | 弱冻结项，允许不同 |
| `description` / `summary` 文案 | **弱冻结**（措辞可优化，语义不得变） |
| `example` / `examples` 内容 | 弱冻结；`examples` 全量补齐属 **AQ-17**，可在冻结后补 |
| `servers` 列表 | 环境相关 |
| 占位数据的具体值（`CAMPUS_A` 等） | **不冻结**（Q-01/Q-02/Q-03 确认后替换）；见 §5 |
| 字段顺序 | JSON 顺序无语义 |

### 4.4 契约测试执行与报告约定

> **OQ-37 裁决后的定位**：§4.2 的 **6 个维度**是**验收标准**（要检查什么）；`Dredd` / `Schemathesis` 是**执行手段**（用什么检查）。

| # | 约定 |
| --- | --- |
| 1 | 契约测试**只读**两端（`openapi.yaml` 与 `/v3/api-docs`），**禁止**自动「修复」任何一端 |
| 2 | 输出**机器可读**（JSON，如 `Dredd` 的 `--reporter=json` / `Schemathesis` 的 JUnit）**+ 人类可读**（Markdown 表）两份报告 |
| 3 | 差异按**严重度**分级：`BLOCKER`（路径/方法/操作缺失）、`MAJOR`（DTO 类型、`required`、枚举）、`MINOR`（文案、示例、描述） |
| 4 | 任何 `BLOCKER` / `MAJOR` → **CI 失败**，且**必须**登记后才可豁免 |
| 5 | 报告**归档**到 `docs/iteration2/contracts/reports/`（含日期、工具名与版本、后端 commit 哈希） |
| 6 | **禁止**把「契约测试的输出」直接当成「契约的更新」；修正契约必须走 §5 流程 |
| 7 | `Dredd` / `Schemathesis` 需要**可运行的后端 + 有效令牌**；测试账号与令牌获取方式见 §6.2（**OQ-40 已裁决：V3 种子账号**；注意 **IMP-02** 的占位哈希阻塞项） |
| 8 | 因 §4.3 已将 `example` 排除在比对之外，运行 `Prism` 与运行契约测试**互不干扰**，可分两条流水线执行 |

### 4.5 迭代一已知的、**不要误报**的差异

| 项 | 现状 | 处置 |
| --- | --- | --- |
| 「仅管理员 14 vs 15」口径 | `api-contract.md` §1 曾写 15；`openapi.yaml` 实为 **14** | **以 `openapi.yaml` 为准**（已在 `api-permission-mapping.md` §5.1 说明）；**不属差异** |
| `SecurityConfig` 权限放开 | 骨架为 `permitAll`，与契约的权限声明**运行时不一致** | 属**实现待补**（迭代二任务）；契约测试比对的是**注解/声明**，不是运行时行为 |
| `springdoc` 输出的 schema 命名 | 可能与 `openapi.yaml` 的 schema 名不同（如 `ItemDetail` vs 内部类名） | **归一化后比对字段结构**；命名差异记为 `MINOR` |

---

## 5. 变更流程（接口冻结后）

> **依据**：`docs/iteration1/design/api-freeze.md` §4~§5。**冻结时刻：2026-10-09**（之后不允许新增接口）。

### 5.1 变更类型（与 `api-freeze.md` §4.2 一致）

| 类型 | 示例 | 允许 | 审批 |
| --- | --- | --- | --- |
| **A. 文案修订** | 修正 `description` 错别字 | ✅ | 接口负责人（单人） |
| **B. 补充示例** | 增加 `example` | ✅ | 接口负责人（单人） |
| **C. 向后兼容扩充** | 新增**可选**响应字段 / 新增枚举值 | ⚠️ | 接口负责人 **+** 后端负责人 |
| **D. 缺陷修正** | 修正错误码语义、DTO 类型错误 | ⚠️ | 接口负责人 **+** 后端负责人 |
| **E. 破坏性变更** | 删字段、改类型、可选→必填、改路径 | ❌ 禁止 | **三人审批**（P0 例外） |
| **F. 新增接口** | 新增 endpoint | ❌ 禁止（2026-10-09 后） | **三人审批**（P0 例外） |
| **G. 替换占位数据值** | `CAMPUS_A` → 真实编码 | ✅ **不属变更** | 无需审批，仅登记 |

### 5.2 三人审批的含义（E / F 类）

| 审批人 | 角色 |
| --- | --- |
| 1 | **接口契约负责人** |
| 2 | **需求方**（产品或需求负责人） |
| 3 | **后端负责人** |

> **记录要求**：变更台账 + **影响清单** + **书面理由**；并**提升 `openapi.yaml` 的 `info.version`**。

### 5.3 变更流程（7 步，与 `api-freeze.md` §4.1 一致）

```text
1. 提出方填写变更申请（类型 / 涉及 operationId 或字段 / 原因 / 不做的后果）
2. 评估影响面（后端文件、前端页面、测试用例、数据库结构）
3. 接口契约负责人判定变更类型与是否 P0 例外
4. 需求方 + 后端负责人审批（E/F 类须三人）
5. 更新 openapi.yaml + 配套文档，并提升 info.version
6. 登记变更台账
7. 后端 / 前端同步实现与测试
```

### 5.4 **禁止事项（红线）**

| # | 禁止 |
| --- | --- |
| 1 | **禁止**绕过流程直接改 `openapi.yaml` |
| 2 | **禁止**只改实现不改契约（或反之）导致两端漂移 |
| 3 | **禁止**在前端契约副本上直接改字段/枚举 |
| 4 | **禁止**在 Mock 里「造」契约中不存在的字段或错误码（会让前端按错误契约开发） |
| 5 | **禁止**把契约测试的差异「静默忽略」—— 差异**必须**登记到 [`open-questions.md`](./open-questions.md) 或变更台账 |
| 6 | **禁止**用 404 掩盖越权、用 500 掩盖并发冲突等**语义等价但不合规**的实现（E-02 / R-18） |

### 5.5 本迭代契约补全过程中发现的**候选**变更项（**未提交审批**）

> 以下项来自本目录 5 份契约的【待确认】清单。**它们只是候选**，是否走变更流程由团队裁决。

| 候选 | 类型 | 说明 |
| --- | --- | --- |
| 新增 `ORDER_PICKUP_POINT_SELECTED` 枚举 | **C（扩充枚举）** | 见 [`notification-events.md`](./notification-events.md) §3.1 G-1 |
| 新增 `REVIEW_RECEIVED` 枚举 | **C（扩充枚举）** | 见同上 G-2 |
| `setMyTradeZones` 空数组的状态码统一（400→422） | **D（缺陷修正）** | 见 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) OQ-01 |
| 「始终返回 null 字段」（改 Jackson 配置） | **非契约变更**（配置变更，**不在本任务范围**） | 见 [`data-conventions.md`](./data-conventions.md) OQ-22 |
| CORS `allowed-headers` 由 `*` 收窄为白名单 | **非契约变更**（配置变更） | 见 [`security-and-cors.md`](./security-and-cors.md) OQ-30 |

---

## 6. 联调计划

### 6.1 从 Mock 切到真实后端：**只改 `baseURL`**

| 项 | 约定 |
| --- | --- |
| 切换方式 | **仅**修改环境变量 / 配置项 `VITE_API_BASE_URL`（或生成的 client `baseURL`） |
| 禁止 | **禁止**为「切后端」而修改任何组件、store、类型、handler 的业务逻辑 |
| Mock 开关 | `VITE_USE_MOCK=true/false`；**生产构建必须 `false`**（并在构建脚本中断言） |
| 三套环境 | `mock`（MSW）→ `local`（`http://localhost:8080/api/v1`）→ `dev`（预发域名，**【待确认 OQ-28】**） |
| 验证 | 切换后**只跑** §6.4 的验收标准；若出现差异 → **登记**，不静默改代码迁就 |

### 6.2 测试账号 —— **已裁决（OQ-40）：使用 V3 种子占位账号，不新增账号**

> **裁决（2026-10-13）**：**使用 `V3__seed_development_data.sql` 中的占位账号，不新增账号。**
>
> **联调测试账号暂使用 V3 种子数据中的 `admin@example.com`（管理员）和 3 个普通用户账号。**
> **密码为占位密码，待 Q-04 确认后再替换为真实账号体系。V3 脚本禁止在生产环境执行。**

**V3 种子账号的实际清单（已核对脚本，**共 4 个账号**）**

| 用户名 | 邮箱 | 角色 / 状态 | 昵称 | 用途 |
| --- | --- | --- | --- | --- |
| **`admin`** | **`admin@example.com`** | `ADMIN` / `ACTIVE` | 平台管理员（开发占位） | 治理操作：下架、封禁、解封、处理举报与申诉、查审计 |
| `alice` | `alice@example.com` | `USER` / `ACTIVE` | 爱丽丝（示例卖家） | 发布商品、作为卖家 |
| `bob` | `bob@example.com` | `USER` / `ACTIVE` | 鲍勃（示例买家） | 作为买家/交换对手方 |
| `carol` | `carol@example.com` | `USER` / `ACTIVE` | 卡罗尔（示例用户） | 第三方（验证 `403 AUTH_NOT_RESOURCE_OWNER` 越权） |

| 项 | **裁决后的约定** |
| --- | --- |
| 账号总数 | **1 个管理员 + 3 个普通用户 = 4 个**（**不新增账号**） |
| 被处罚用户 | **不预置**，由 `admin` 现场封禁 `carol` 产生（可同时验证封禁流程与 `403 USER_RESTRICTED_CANNOT_WRITE` + `restrictedUntil`） |
| 密码 | **占位密码**；**待 Q-04 确认后再替换为真实账号体系** |
| 环境限制 | **V3 脚本禁止在生产环境执行**（脚本头部已明文声明） |
| 数据种子 | 继续使用 V1~V4（**不新增 V5+**，见 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §4 / OQ-41） |

> **⚠️ 必须知情的两个事实（已核对 `V3__seed_development_data.sql`，**阻塞项已登记**）**
>
> | # | 事实 | 影响 | 结论 |
> | --- | --- | --- | --- |
> | 1 | V3 实际为 **1 个管理员 + 3 个普通用户**（`admin` / `alice` / `bob` / `carol`），**不是**"4 个普通用户账号" | 第三方越权用例仍可用（3 个普通用户足够：1 卖家 + 1 买家 + 1 第三方） | **不阻塞**，按上表执行 |
> | 2 | V3 的 `password_hash` 是**格式不合法的占位 bcrypt 串**（`$2a$10$PLACEHOLDERHASH…`，实测 **62 字符**，合法 bcrypt 为 **60 字符**），**无法匹配任何明文口令**；文件头注释说明"真实哈希在后端工程骨架步骤由应用初始化逻辑写入或重置" | **这 4 个账号当前无法登录**，联调无法开始 | ⚠️ **阻塞 P2 联调**。需后端在迭代二补齐「**把占位哈希重置为真实 bcrypt**」的初始化能力（或提供一次性重置脚本），**属于后端实现任务**，**不属本次"只改 Markdown"的范围**。已登记为 [`open-questions.md`](./open-questions.md) **IMP-02** |

> **红线**：**禁止**把生产/真实用户数据用于联调；联调**只使用** V3 种子账号。
> **管理员账号来源（Q-04，P0，仍未答复）**：V3 采用的是「**种子预置账号**」方案（`admin@example.com`）；**Q-04 确认后**若改为配置文件/提权等其它方案，本表需同步更新。
> **注意**：Q-04 属**需求侧 P0**，**不在本次 9 条 P0 裁决范围内**，仍然开放。

### 6.3 种子数据需求（联调前需具备）

| # | 数据 | 最少数量 | 用途 |
| --- | --- | --- | --- |
| 1 | 交易区域（`CAMPUS_A` 等占位） | ≥ 2 个 | 跨区拒绝（`422 TRADE_ZONE_MISMATCH`）验证 |
| 2 | 每个区域下的公共取货点 | 每区域 ≥ 2 个 | 取货点跨区（`422 PICKUP_POINT_ZONE_MISMATCH`）验证 |
| 3 | 分类（`categories`） | ≥ 3 个 | 商品发布与筛选 |
| 4 | 成色（`item_conditions`） | 5 个 | 商品发布与筛选 |
| 5 | 用户的常用交易区域勾选（`user_trade_zones`） | 每个测试用户 1–5 个 | 发布商品前置条件 |
| 6 | 在售商品（含图片） | ≥ 6 件，分属 ≥ 2 个卖家 | 列表/详情/下单/交换 |
| 7 | 进行中订单（各状态） | 每个订单状态 ≥ 1 条 | 状态分支与错误码验证 |
| 8 | 进行中交换提议（含各状态） | 每个提议状态 ≥ 1 条 | 交换分支与错误码验证 |
| 9 | 通知（覆盖 25 种 `type`） | 每种 ≥ 1 条 | 通知 UI 分支与跳转 |
| 10 | 生效中处罚 + 申诉 | ≥ 1 条 | 处罚白名单与申诉流程 |

> **种子迁移需求 —— 已裁决（OQ-41）**：**迭代二暂不新增 `V5__seed_iteration2_*.sql` 种子迁移**，继续使用 V1~V4。
> **上表所需的联调数据由真实接口操作产生**（更贴近真实、同时验证接口），而非预置种子。
> **约束**：**禁止修改 V1~V4**（已哈希冻结）；真实占位数据替换待 Q-01~Q-03 确认后走**新增** `V5__update_placeholder_zones.sql` 之类的新迁移。详见 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §4。

### 6.4 联调验收标准（前后端共同签署）

| # | 验收项 | 通过标准 | 依据 |
| --- | --- | --- | --- |
| 1 | **契约一致** | 契约测试 6 个维度**零 `BLOCKER` / 零 `MAJOR`**；operationId = 83 | §4.2 |
| 2 | **正常主流程** | 注册 → 登录 → 改资料 → 勾选区域 → 上传图片 → 发布商品 → 列表可见 → 详情正确 → 编辑/下架/重新上架 → 举报 | `handover` §5.1 |
| 3 | **错误码分支** | 前端对 `error-codes.md` §5 的 11 条约定**逐条实现**；关键 `code` 有 E2E 用例 | `error-codes.md` §5 |
| 4 | **权限边界** | `api-permission-mapping.md` §8 的 6 组越权用例**全部通过**（非本人 403、管理员交易 403、被处罚禁写 403 + `restrictedUntil`、未登录 401、令牌过期 401） | §8 |
| 5 | **幂等** | 必填 `Idempotency-Key` 的 16 个接口：**同键重复提交返回首次结果**（E-06 按成功处理）；**同键异请求体** → 409 | R-17 |
| 6 | **空值/边界** | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) 中**已确认**的边界逐条验证（分页、`[]`、`null`、超限） | GC-01 |
| 7 | **数据格式** | 金额为字符串 2 位小数、时间为 ISO 8601 UTC、枚举取值合法、`traceId` 头体一致 | GC-02 |
| 8 | **CORS** | 从 `http://localhost:5173` 直连 **无 CORS 报错**；预检通过；`Idempotency-Key` 可发送；`X-Trace-Id` 可读 | GC-03 §3 |
| 9 | **脱敏** | 响应中**不出现** `password_hash` / `deleted_at` / `created_by` / `updated_by` | GC-03 §5 |
| 10 | **通知** | 触发矩阵中已确认的每个事件**产生正确 `type` 与 `refId`**；角标数正确；标记已读幂等 | GC-04 §3 |
| 11 | **越权不泄露** | 非本人资源返回 **403 而非 404** | P-00-B |
| 12 | **性能基线**（【待确认 OQ-42】） | 列表接口 P95 **< 500ms**（本地、种子数据规模）；图片上传 ≤5MB 可完成 | — |

### 6.5 联调节奏（建议）

| 阶段 | 前端 | 后端 | 出口条件 |
| --- | --- | --- | --- |
| **P0 契约同步** | 同步 `openapi.yaml` + 生成类型/Client | 提供 `/v3/api-docs` | 生成产物编译通过；契约测试零 BLOCKER |
| **P1 Mock 并行** | 用 MSW/Prism 完成全部页面与错误分支 | 实现迭代二 28 个 `operationId` | Mock 覆盖 §3.2 全部场景 |
| **P2 逐接口切真** | **只改 `baseURL`**，按接口逐个切换 | 逐个接口自测（含幂等与并发） | 每个接口「Mock 结果 == 真实结果」 |
| **P3 全链路联调** | 跑 §6.4 的 12 项 | 提供联调环境与种子数据 | 12 项全部通过并签署 |
| **P4 冻结回归** | 回归所有已切接口 | 重跑契约测试 | 契约测试零差异（或差异已登记） |

---

## 7. 反模式清单（**不要这样做**）

| # | 反模式 | 后果 | 正确做法 |
| --- | --- | --- | --- |
| 1 | 前端按 `message` 文本判断错误类型 | 文案一改就崩 | 按 `code` 分支（AD-03） |
| 2 | 手抄 DTO 类型到前端 | 与契约漂移，编译期发现不了 | 由 `openapi-typescript` 生成 |
| 3 | Mock 里造契约中不存在的字段 | 前端按错误契约开发 | 只按 §3.3 约束生成 |
| 4 | 为了联调通过而改契约 | 破坏冻结 | 登记差异 → 走 §5 流程 |
| 5 | 用 `any` 绕过类型错误 | 缺陷推迟到运行时 | 修类型或登记契约问题 |
| 6 | 契约测试差异「先忽略，回头再说」 | 差异累积，冻结失效 | 差异**必须**登记（§5.4 #5） |
| 7 | 只测正常路径 | 错误分支无人验证 | 按 §3.2 覆盖错误场景 |
| 8 | 把金额转 `number` 计算 | 浮点误差 | 字符串 + 定点库（GC-02 §3） |
| 9 | 只在前端隐藏按钮，不等后端拦截 | 越权漏洞 | 后端必须校验（GC-03 §2.3） |
| 10 | 用 Mock 验证并发 | 结论无效 | 并发只对真实后端验证 |

---

## 8. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **执行 P0 裁决并固化**：① **OQ-37 已裁决** —— 工具链指定为 `openapi-typescript`（类型）+ `orval`（Client）+ **`Prism`（Mock，指定项）** + **`Dredd` 或 `Schemathesis`（契约测试）**，不锁定版本；`MSW` 降级为可选项；§3.1 / §4.1 / §4.4 同步改写；② **OQ-40 已裁决** —— 联调使用 **V3 种子占位账号**（`admin@example.com` + 3 个普通用户，**共 4 个，不新增**），密码为占位密码待 Q-04 替换，V3 禁止在生产执行；**如实登记**「V3 实为 1 管理员 + 3 普通用户」与「占位 bcrypt 哈希非法（62 字符）导致当前无法登录」两项事实，阻塞项登记为 **IMP-02** | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：唯一真源与工具链（`openapi-typescript`/`orval`/`Prism`/`MSW`）、前端类型与 Client 生成约定、Mock 覆盖要求与 9 条契约约束、契约测试 6 个比对维度与不比对清单、`api-freeze.md` 变更流程与红线、5 项候选变更、联调计划（只改 `baseURL`）、测试账号与种子数据需求、12 项验收标准、联调节奏、10 条反模式 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
