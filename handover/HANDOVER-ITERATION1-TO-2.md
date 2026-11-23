# 迭代一 → 迭代二 交接文档（Markdown 现行版）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 → 迭代二 交接文档 |
| 版本 | **v2.0** |
| 日期 | 2026-11-11 |
| 读者 | 迭代二及后续全体组员（4 后端 + 3 前端）、验收人、代码审查人 |
| 状态 | 生效（本版为现行版） |
| 技术栈 | Java 11 + Spring Boot 2.7.18 + PostgreSQL 15 |
| 契约版本 | OpenAPI 3.0.3，`1.0.0-frozen-candidate`（83 个 operationId，已冻结） |
| 替代关系 | 本版**替代**根目录旧版 `ITERATION1_TO_ITERATION2_HANDOVER.md` 与 `handover/` 下原 PDF；旧版与 PDF 已由用户发小组项目群备份，本版为唯一现行版 |
| 配套路线 | [`ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md)（切片、归属、operationId 分配唯一口径） |
| 目录说明 | [`PROJECT-STRUCTURE.md`](./PROJECT-STRUCTURE.md)（生成时点实际目录结构） |

> 本文档只描述事实与规则，不修改任何代码、契约、迁移或冻结文档。
> 所有计数与哈希均来自生成时点的只读实测，见第 2、3 章。

---

## 1. 迭代一完成状态

### 1.1 一句话结论

迭代一把「要做什么、做成什么样」定死，并交付了可直接开始编码的工程与接口骨架；**业务逻辑一行未实现**，这属迭代一预期。

### 1.2 代码事实（实测）

| 项 | 实测值 | 说明 |
| --- | --- | --- |
| Java 源文件 | **158** | `src/main/java` 下全部 `.java` |
| 契约 Controller | **12** | Auth、User、Dictionary、Item、Cart、Order、Swap、Internal、Admin、Report、Review、Notification |
| 手工 Controller | **2** | `HealthController`、`FrozenSpecController`，均已 `@Hidden` |
| operationId | **83** | 与 `openapi.yaml` 一一对应 |
| 路径 × 方法映射 | **86** | 83 契约 + 3 手工（`/health`、`/health/ready`、`/openapi`） |
| DTO | **33 Request + 66 Response = 99** | `dto/request`、`dto/response` |
| Service 接口 / 实现 | **12 / 12** | 方法签名与 Controller 对齐 |
| Service 实现状态 | **全部抛 `UnsupportedOperationException`** | 285 处 / 36 个文件（含 Controller 中的占位抛错） |
| entity / repository | **仅 `package-info.java`** | 0 实体、0 仓储 |
| `ErrorCode` | **7 个通用码** | 93 个业务码未落地 |
| `SecurityConfig` | **`anyRequest().permitAll()`** | 骨架放开模式，切片 1 必办收紧 |
| `@PreAuthorize` 分布 | permitAll 10 / isAuthenticated 55 / ADMIN 14 / SYSTEM 4 | 合计 83，与契约声明一致 |
| JWT 过滤器 | 解析异常时按匿名继续 | 受保护接口的 401 语义需在切片 1 复核 |
| 同名包络类 | `common.ApiResponse/ErrorResponse/PageResult` 与 `dto.response` 同名三对并存 | Controller 只使用 `common.*` |

### 1.3 数据库与文档事实

| 项 | 实测值 |
| --- | --- |
| 迁移脚本 | `V1`~`V4` 共 4 个，已应用且只读 |
| 业务表 / 列 | 25 张 / 324 列（`../docs/iteration1/design/schema.md`） |
| 契约文档 | `openapi.yaml`（83 operationId / 73 paths）、`api-contract.md`、`error-codes.md`、`api-freeze.md`、`api-permission-mapping.md`、`idempotency-and-concurrency.md`、`schema.md`、`rules.md`、`state-machines.md`、`SystemDesign.md` 等 |
| 冻结文档 | `../docs/iteration1/FREEZE.md` 已登记 10 个本步产出文件与 5 个契约/迁移文件哈希 |
| 交付归档 | `../iteration1-delivery.zip`（冻结归档，未解压、未修改） |

**明确属迭代一预期、不算缺陷**：Service 全空实现、entity/repository 为空、`ErrorCode` 仅通用码、`SecurityConfig` 放开、业务接口调用返回 500（需鉴权接口返回 401/403）。

---

## 2. 契约冻结说明

### 2.1 冻结基线（SHA-256 前 16 位）

| 文件 | SHA-256 前 16 |
| --- | --- |
| `../docs/iteration1/design/openapi.yaml` | `36bd7a03a184734c` |
| `../src/main/resources/db/migration/V1__init_schema.sql` | `620ad94bb6cf96b2` |
| `../src/main/resources/db/migration/V2__seed_reference_data.sql` | `8a5f22d53904761a` |
| `../src/main/resources/db/migration/V3__seed_development_data.sql` | `ab3c9c105aed2eea` |
| `../src/main/resources/db/migration/V4__fix_review_window_index.sql` | `cbd952e089a2d0af` |
| `../iteration1-delivery.zip` | `85997ba15b4a71c8e743db5b015bcedcd45f4957e1da383a99f636a045302145` |

### 2.2 冻结规则

| 规则 | 内容 |
| --- | --- |
| 接口契约 | `openapi.yaml` 是唯一真源；`2026-10-09` 之后**不允许新增接口** |
| 契约变更 | 必须走 `../docs/iteration1/design/api-freeze.md` **§4** 流程（申请 → 影响面 → 类型判定 → 审批 → 升版 → 台账 → 同步实现） |
| 迁移冻结 | `V1`~`V4` 只读（含注释、空白、换行）；结构变更一律新增 `V5+` |
| 占位数据替换 | 替换 `CAMPUS_A`、`PICKUP_LIBRARY_A` 等值属 `api-freeze.md` 第 6 章**类型 G，不属契约变更**，登记即可 |
| 错误码 | 只使用 `error-codes.md` 已登记的 93 个 `DOMAIN_REASON` 错误码，禁止自造 |

---

## 3. 已知代码缺陷（只登记，未修复）

| 编号 | 缺陷 | 证据 | 建议归属 |
| --- | --- | --- | --- |
| CD-1 | `ItemController.uploadImage` 方法无参数，缺契约必填 multipart `file` 与 `purpose`；83 个操作中仅此 1 个缺必填请求体参数 | `../src/main/java/com/jc2001/campustrade/controller/ItemController.java`（`uploadImage()`）；`openapi.yaml` `/files/images` | **切片 2 开工前修复** |
| CD-2 | `common` 与 `dto.response` 存在 `ApiResponse` / `ErrorResponse` / `PageResult` 三对同名类；Controller 只用 `common.*` | 两个包下同名文件 | 整合阶段统一收口，避免第二真源 |
| CD-3 | JWT 过滤器在解析异常时清空上下文并按匿名继续 | `security/JwtAuthenticationFilter.java` 的 `catch` 分支 | 切片 1 收紧安全时复核，确保受保护接口返回 401 |

> 以上均未修复，**不属于迭代一验收缺陷遗漏之外的业务实现偏离**；迭代一仍处于完成状态。

---

## 4. 切片划分与归属

> 唯一口径：`openapi.yaml` 的 83 个 operationId + Controller 文件归属。
> 逐条分配见 [`ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md) 第 6 章。

| 切片 | 名称 | 时间 | 表数 | operationId（Controller 口径） | Controller |
| --- | --- | --- | --- | --- | --- |
| 切片 1 | 账户与共享内核 | 10/13 ~ 10/20 | 7 | **15** | Auth 6 + User 5 + Dictionary 4 |
| 切片 2 | 商品与检索 | 10/21 ~ 11/2 | 3 | **10** | Item 10 |
| 切片 3 | 购买与交换 | 11/3 ~ 11/20 | 9 | **30** | Cart 4 + Order 8 + Swap 16 + Internal 2 |
| 切片 4 | 治理、评价与整合交付 | 11/21 ~ 12/10 | 6 | **28** | Admin 17 + Report 3 + Review 3 + Notification 3 + Internal 2 |
| **合计** | — | 10/13 ~ 12/10 | **25** | **83** | 12 个契约 Controller |

**说明**

- 切片 3 含 `runOrderTimeoutReleaseTask`（3A）与 `runSwapProposalExpireTask`（3B）；切片 4 含 `runPenaltyAutoUnbanTask` 与 `runReviewWindowCloseTask`。
- `createSwapReview` 位于 `SwapController`，按文件归属计入**切片 3**（不重复计入切片 4）。
- 举报的 3 个 operationId（`reportItem`、`listMyReports`、`getMyReport`）与 `reports` 表同属**切片 4**，**不在迭代二（切片 1 + 切片 2）范围内**。
- 迭代二（切片 1 + 切片 2）合计 **25 个 operationId = 15 + 10**。

**前端路由前缀（摘要）**

| 切片 | 路由前缀 |
| --- | --- |
| 切片 1 | `/register`、`/login`、`/me`、`/me/profile`；全局布局与鉴权守卫 |
| 切片 2 | `/`、`/search`、`/items`、`/items/:id`、`/items/new`、`/items/:id/swap-setting`、`/me/items` |
| 切片 3 | `/cart`、`/checkout`、`/orders`、`/orders/:id`、`/orders/:id/pay-result`、`/orders/:id/pickup-point`、`/me/swap-bundles`、`/items/:id/propose-swap`、`/swaps/:id`、`/swaps/:id/swap-code` |
| 切片 4 | `/items/:id/report`、`/me/reports`、`/me/reviews`、`/me/penalties`、`/me/appeals`、`/notifications`、`/orders/:id/reviews`、`/admin`、`/admin/users`、`/admin/items`、`/admin/penalties`、`/admin/reports`、`/admin/audit-logs` |

完整表归属、受限写字段与例外登记见路线文件附录 A。

---

## 5. 双人责任制

| 切片 | 后端主办 | 后端协办 | 前端主办 |
| --- | --- | --- | --- |
| 切片 1：账户与共享内核 | ———— | ———— | ———— |
| 切片 2：商品与检索 | ———— | ————（可复用切片 1 协办） | ———— |
| 切片 3：购买与交换（3A） | ———— | ———— | ————、———— |
| 切片 3：购买与交换（3B） | ———— | ———— | ————、———— |
| 切片 4：治理、评价与整合交付 | ———— | ———— | ————、———— |

**规则**

1. 每片至少 **1 名后端主办 + 1 名后端协办 + 1 名前端主办**；协办可跨片复用，但不可由一人包干整片。
2. 主办负责主流程；协办负责评审与错误分支实现；前端主办负责本片页面与联调。
3. 姓名与 GitHub 账号留 `————` 占位，由团队开工前填写；**禁止共用账号、禁止代提交**。
4. 固定角色：接口契约负责人 1、后端负责人 1、验收人 1、前端负责人 1，可由后端或前端成员兼任。

---

## 6. 串行接力与交接清单

### 6.1 串行接力

- 同一时刻**只有一个切片在写代码**；切片闭合后才交棒给下一片。
- **禁止跳片**（例如为赶进度先做切片 4 的管理员页面）。
- **禁止带病交棒**：未通过的测试、未解决的编译错误不得留给下一片。

### 6.2 交接清单（10 项，逐项打勾才可交棒）

| # | 检查项 | 判定标准 | 负责人 |
| --- | --- | --- | --- |
| 1 | 本地全量构建通过 | `mvn clean package -DskipTests` 输出 BUILD SUCCESS | 后端主办 |
| 2 | 应用可启动、健康检查通过 | `/actuator/health` 返回 200；`docker compose ps` 显示 Up (healthy) | 后端主办 |
| 3 | 交付标准逐项验证并留痕 | 本片验收项逐项有结论，失败项为 0 | 后端主办 + 前端主办 |
| 4 | 没有遗留的未通过测试或编译错误 | 测试全绿；编译无错误 | 后端主办 |
| 5 | 契约未被修改 | `openapi.yaml` SHA-256 前 16 位仍为 `36bd7a03a184734c` | 接口契约负责人 |
| 6 | 未触碰归属范围之外的文件 | `git diff --stat` 的文件集合 ⊆ 开工前贴出的清单 | 下一片负责人 |
| 7 | 提交信息规范、无 TODO 残留 | 提交信息符合 `feat(切片名): 具体内容`；`grep -rn "TODO"` 无本片遗留项 | 后端主办 |
| 8 | 已合并 main | 合并后主干可启动、可跑通上一片的冒烟脚本 | 后端负责人 |
| 9 | 下一片负责人已完成代码审查 | 6 项合规检查逐项通过（越界、契约一致、条件更新、错误码、测试、跨片依赖） | 下一片负责人 |
| 10 | 口头交接已完成 | 下一片负责人能复述：本片交付了什么、遗留了什么、哪些是受限写 | 后端主办 + 下一片负责人 |

> 未勾选项必须给出结论：① 在本片内解决后重走清单；② 把对应 operationId 移出本片并登记（写明原因与影响）。**不允许“带病交棒 + 口头保证下片补”。**

---

## 7. Git 与提交规范

| 项 | 规范 |
| --- | --- |
| 账号 | 每人用自己的 GitHub 账号提交，贡献可追溯；禁止共用账号、禁止代提交 |
| 分支 | `feat/slice<N>-<简短描述>`（如 `feat/slice1-account-core`） |
| 合并 | Pull Request + 至少 1 人评审；评审通过后由后端负责人合并 |
| 提交信息 | `feat(切片名): 具体内容`，写清“哪个 operationId / 哪条规则”，例如 `feat(切片2): 实现 createItem 与 uploadImage（R-10/R-12/R-03）` |
| 频率 | 每天至少提交一次可编译状态；禁止一次性提交上千行 |
| 禁止 | `git push --force`、`git reset --hard`、`git checkout --` 丢弃他人改动、提交 `target/` / `.mvnrepo/` / `.DS_Store` / IDE 配置 |

> 当前仓库已初始化为 git 仓库并含 `.gitignore`。首次提交前请补齐 `.mvnrepo/`、`__pycache__/`、`.env` 的忽略项（本次未修改 `.gitignore`）。

---

## 8. 迭代二启动前置条件

| # | 前置条件 | 责任方 |
| --- | --- | --- |
| 1 | 切片 1 的后端主办、后端协办、前端主办完成认领 | 后端负责人 / 前端负责人 |
| 2 | **CD-1（`uploadImage` 缺 multipart 参数）在切片 2 开工前修复**并补用例 | 切片 2 归属人 |
| 3 | A-6（`DELIVERY.md` 是否解冻）由交付负责人裁决 | 交付负责人 |
| 4 | A-7（迭代二广义契约 README 权威顺序）由接口契约负责人裁决 | 接口契约负责人 |
| 5 | A-9（`.mvnrepo/` 已入库与 `.gitignore` 缺项）由后端负责人裁决 | 后端负责人 |
| 6 | 根目录三份路线附件与 `handover/` 版本不一致时，**以 `handover/` 版本为准** | 全体 |

---

## 9. 已裁决事项（本版唯一口径）

| 编号 | 原矛盾 | 裁决结果 | 依据 |
| --- | --- | --- | --- |
| A-1 | 三份路线基准不在仓库 | 路线文件入仓为 `handover/ITERATION-PLAN-VERTICAL-SLICES.md`；相对路径从 `handover/` 解析（`../docs/...`）；根目录附件版本为历史参考，本版为唯一现行版 | 本次任务授权；`openapi.yaml` / `schema.md` / `V1~V4` 为唯一真源 |
| A-2 | 切片 3/4 的 operationId 计数与 `InternalController` 的 2/2 分工不自洽 | 以 `openapi.yaml` 83 个 operationId + Controller 文件归属为唯一口径：`InternalController` 4 个方法中 `runOrderTimeoutReleaseTask`、`runSwapProposalExpireTask` 归切片 3，`runPenaltyAutoUnbanTask`、`runReviewWindowCloseTask` 归切片 4；`createSwapReview`（`SwapController`）归切片 3 | 83 = 15 + 10 + 30 + 28 |
| A-3 | 切片 4 的 operationId 数存在多种写法 | 统一为 **28**，不再保留其他写法 | 同上，按 Controller 口径 |
| A-4 | 切片 2 是否配协办 | 按双人责任制：切片 2 配 1 主办 + 1 协办（可复用切片 1 协办）+ 1 前端主办 | 双人责任制红线 |
| A-5 | 切片 1 表数 6 vs 7 | 切片 1 独占 **7** 张表，含 `user_trade_zones` | `schema.md` 与 `V1`~`V4` |
| A-10 | `FILE_TOO_LARGE` 413 vs 422 | **422**；文档与实现均按 `error-codes.md` 与 `openapi.yaml` | `error-codes.md` |

---

## 10. 已知待裁决事项（只登记事实，不给结论）

| 编号 | 事实 | 影响 |
| --- | --- | --- |
| A-6 | `docs/iteration1/DELIVERY.md` 名为交付说明，但哈希已登记在 `FREEZE.md` §1.1 | 是否解冻/修订由交付负责人裁决 |
| A-7 | `docs/iteration2/contracts/README.md` 的权威顺序未纳入三份路线基准；文件内“8 份”与“7 份”不一致 | 由接口契约负责人裁决 |
| A-8 | 原 `handover/` PDF 已由用户发项目群备份并按授权删除；是否再生成 PDF 未定 | 本任务只产出 Markdown，不生成 PDF |
| A-9 | `.mvnrepo/` 已被 git 跟踪；`.gitignore` 未含 `.mvnrepo/`、`__pycache__/`、`.env` | 清理属破坏性 git 操作，由后端负责人裁决 |

---

## 11. 已删除文件与冻结冲突登记

### 11.1 已删除文件

| 路径 | 类型 | SHA-256 | 是否被 `FREEZE.md` 登记 | 是否被 Markdown 引用 | 是否在 `iteration1-delivery.zip` 内 | 处理 |
| --- | --- | --- | --- | --- | --- | --- |
| `handover/HANDOVER-ITERATION1-TO-2.pdf` | PDF | `d3cfbb8b4716522639b4eae1e5bad3c7c3e7c01c671e7dfb2aac27692b0ec3e5` | 否（FREEZE §1.1 无 PDF 项） | 否（无 Markdown 以该文件名引用） | 否（zip 内无 PDF） | 已删除，由本 Markdown 替代 |
| `handover/PROJECT-STRUCTURE.pdf` | PDF | `c7114360df71eaf720eda812418ca233439a300e13b87231dfc914099efa54cf` | 否 | 否 | 否 | 已删除，由 `PROJECT-STRUCTURE.md` 替代 |

### 11.2 备份与冻结说明

- **备份位置**：上述两个 PDF 已由用户发到**小组项目群**备份；删除经用户明确授权。
- **冻结冲突**：经核对，`FREEZE.md` §1.1 的 10 个文件与 §1.2 的 5 个契约/迁移均**未包含**这两个 PDF，因此删除**不破坏 FREEZE 已登记的哈希**。
- 若后续发现 FREEZE 或其他冻结材料登记了这两个 PDF，需由交付负责人决定是更新 FREEZE 还是补登删除记录；**本版不修改 `FREEZE.md`**。

### 11.3 引用失效说明

- 本次扫描未发现任何 Markdown 以 `HANDOVER-ITERATION1-TO-2.pdf` 或 `PROJECT-STRUCTURE.pdf` 作为引用路径。
- 根目录旧交接文档中出现的 `ITERATION1_TO_ITERATION2_HANDOVER.pdf` 指向根目录，该文件本就不存在（历史遗留），与本次删除无关；根目录旧文档不在本次修改范围，可能产生死链。

### 11.4 保留文件

`handover/` 下生成前仅有上述两个 PDF；删除后存在一个 macOS 自动生成的 `.DS_Store`（非本次新建，已由 `.gitignore` 忽略），按约束保留、不删除、不修改。本次新增 3 个 Markdown：

- `handover/HANDOVER-ITERATION1-TO-2.md`（本文件）
- `handover/ITERATION-PLAN-VERTICAL-SLICES.md`
- `handover/PROJECT-STRUCTURE.md`

---

## 12. 关键文档索引（路径从 `handover/` 解析）

| 文档 | 用途 |
| --- | --- |
| [`../docs/iteration1/design/openapi.yaml`](../docs/iteration1/design/openapi.yaml) | 接口契约唯一真源（83 operationId，已冻结） |
| [`../docs/iteration1/design/api-freeze.md`](../docs/iteration1/design/api-freeze.md) | 冻结范围与 §4 变更流程 |
| [`../docs/iteration1/design/api-contract.md`](../docs/iteration1/design/api-contract.md) | 83 个接口清单与 FR 映射 |
| [`../docs/iteration1/design/api-permission-mapping.md`](../docs/iteration1/design/api-permission-mapping.md) | 逐接口权限说明 |
| [`../docs/iteration1/design/error-codes.md`](../docs/iteration1/design/error-codes.md) | 93 个错误码与 E-01~E-17 |
| [`../docs/iteration1/design/idempotency-and-concurrency.md`](../docs/iteration1/design/idempotency-and-concurrency.md) | 幂等与并发（16 必填 + 16 可选） |
| [`../docs/iteration1/design/schema.md`](../docs/iteration1/design/schema.md) | 25 张表 / 324 列字段字典 |
| [`../docs/iteration1/rules.md`](../docs/iteration1/rules.md) | 业务规则（R-01~R-35） |
| [`../docs/iteration1/state-machines.md`](../docs/iteration1/state-machines.md) | 6 大状态机与转换编号 |
| [`../docs/iteration1/design/SystemDesign.md`](../docs/iteration1/design/SystemDesign.md) | 系统设计（架构、领域模型、权限） |
| [`../docs/iteration1/FREEZE.md`](../docs/iteration1/FREEZE.md) | 交付冻结声明与哈希 |
| [`../README-backend.md`](../README-backend.md) | 后端启动、验证、常见问题 |
| [`../README-db.md`](../README-db.md) | 数据库连接、迁移、查询 |
| [`./ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md) | 四切片、83 个 operationId 分配、25 张表落位、双人责任制 |
| [`./PROJECT-STRUCTURE.md`](./PROJECT-STRUCTURE.md) | 实际目录结构、已删除 PDF 登记 |
| [`./COLLABORATION-RULES.md`](./COLLABORATION-RULES.md) | 协作规则全文（12 条规则、6 条红线、10 项交接清单、Q1~Q5） |
| [`./TEAM-ROADMAP-FOR-MEMBERS.md`](./TEAM-ROADMAP-FOR-MEMBERS.md) | 团队路线通俗版（每步做什么、前端跟到什么程度） |
| [`./EXECUTION-CHECKLIST.md`](./EXECUTION-CHECKLIST.md) | 执行层检查清单（开工前 / PR / 闭合 / agent 规范） |
| [`./DEFINITIONS-AND-PROTOCOLS.md`](./DEFINITIONS-AND-PROTOCOLS.md) | 细节定义（契约测试 6 维度、通知机制候选、错误码唯一化、Q-04） |

---

## 13. 变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v2.0 | 2026-11-11 | 交接包生成代理 | 新建 Markdown 现行版，替代根目录旧版与 `handover/` 原 PDF；按裁决表消解 A-1/A-2/A-3/A-4/A-5/A-10：切片 operationId 统一为 **15 / 10 / 30 / 28 = 83**，切片 1 为 7 张表，切片 2 配协办，`FILE_TOO_LARGE` 统一 422；写入双人责任制、10 项交接清单、Git 规范；登记 CD-1~CD-3；登记已删除 PDF 的哈希与备份说明；A-6~A-9 仅登记 | 生效 |
| — | — | — | 根目录旧版 `ITERATION1_TO_ITERATION2_HANDOVER.md` 与 `handover/` 原 PDF 为历史版本，已由用户发群备份 | 历史 |
