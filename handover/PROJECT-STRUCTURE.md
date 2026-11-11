# 项目结构说明（生成时点实际结构）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 项目结构说明 |
| 版本 | **v1.0** |
| 日期 | 2026-11-11 |
| 状态 | 生效（反映生成时点的实际目录结构） |
| 说明 | `handover/` 下 PDF 已由本目录 Markdown 替代；PDF 原件已发小组项目群备份 |

> 本文件只描述实际存在的目录与文件，不修改任何内容。
> 表与切片的完整对应见 [`ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md)。

---

## 1. 根目录结构

```text
jc2001.backend/
├── .git/                          # 版本库（不在本文件展开）
├── .idea/                         # 本机 IDE 配置（不应提交）
├── .mvnrepo/                      # 本地 Maven 依赖缓存（构建缓存，非源码）
├── db/
│   └── verify_schema.sql          # 数据库只读对账脚本
├── docs/
│   ├── iteration1/                # 迭代一需求、设计、契约、冻结与验收材料（只读）
│   └── iteration2/
│       └── contracts/             # 迭代二广义契约 8 份（冻结候选）
├── handover/                      # 本目录：交接包（7 个 Markdown）
├── scripts/
│   └── package-iteration1.sh      # 迭代一交付打包脚本
├── src/                           # 后端源码（见第 2 章）
├── target/                        # Maven 构建产物（构建缓存，非源码）
├── tools/
│   ├── generate_api_skeleton.py   # 契约 → 骨架生成器
│   └── md2pdf.py                  # Markdown → PDF 工具（本任务未使用）
├── .dockerignore
├── .gitignore
├── docker-compose.yml
├── Dockerfile                     # 运行镜像：只复制宿主机 jar
├── Dockerfile.build               # 可选：容器内完整构建
├── pom.xml
├── README-backend.md              # 后端启动、验证、常见问题
├── README-db.md                   # 数据库连接、迁移、查询
├── ITERATION1_TO_ITERATION2_HANDOVER.md   # 旧版交接文档，保留供历史查阅；现行版在 handover/
├── ITERATION1_ACCEPTANCE_REPORT.md        # 迭代一验收报告（只读）
└── iteration1-delivery.zip        # 迭代一冻结归档（不解压、不修改；内含文件属冻结资产）
```

**说明**

- `target/`、`.mvnrepo/` 是构建缓存/产物，**不是源码目录**。
- `iteration1-delivery.zip` 为冻结归档；本目录不引用其内部文件为可访问资源，也未解压。
- 根目录旧版 `ITERATION1_TO_ITERATION2_HANDOVER.md` 保留；其内容为历史版本，现行交接见 `handover/HANDOVER-ITERATION1-TO-2.md`。

---

## 2. `src/` 结构

```text
src/main/java/com/jc2001/campustrade/
├── CampusTradeApplication.java     # 启动类（1）
├── common/                         # 统一响应与异常（7 个文件）
├── config/                         # 配置（4 个文件）
├── security/                       # JWT 与认证（4 个文件）
├── controller/                     # 14 个文件 = 12 契约 Controller + Health + FrozenSpec
├── service/                        # 12 个 Service 接口 + 1 个 package-info
├── service/impl/                   # 12 个空实现 + 1 个 package-info
├── dto/request/                    # 33 个请求 DTO + 1 个 package-info
├── dto/response/                   # 66 个响应 DTO/枚举
├── entity/                         # 仅 package-info（0 实体）
└── repository/                     # 仅 package-info（0 仓储）

src/main/resources/
├── application.yml
├── application-dev.yml
└── db/migration/                   # V1 ~ V4（只读）
```

| 目录 | 文件数 | 内容 |
| --- | --- | --- |
| 根（`campustrade`） | 1 | `CampusTradeApplication.java` |
| `common` | 7 | `ApiResponse`、`ErrorResponse`、`PageResult`、`ErrorCode`、`GlobalExceptionHandler`、`ApiErrorController`、`TraceIdFilter` |
| `config` | 4 | `SecurityConfig`、`OpenApiConfig`、`JacksonConfig`、`AppProperties` |
| `security` | 4 | `JwtTokenProvider`、`JwtAuthenticationFilter`、`AuthenticatedUser`、`UserDetailsServiceImpl` |
| `controller` | 14 | 12 个契约 Controller + `HealthController` + `FrozenSpecController`（后两者 `@Hidden`） |
| `service` | 12 接口 + 1 package-info | `AuthService`、`UserService`、`DictionaryService`、`ItemService`、`CartService`、`OrderService`、`SwapService`、`InternalTaskService`、`AdminService`、`ReportService`、`ReviewService`、`NotificationService` |
| `service/impl` | 12 实现 + 1 package-info | 全部抛 `UnsupportedOperationException` |
| `dto/request` | 33 + 1 package-info | 请求 DTO |
| `dto/response` | 66 | 响应 DTO 与枚举 |
| `entity` | 1 | 仅 `package-info.java` |
| `repository` | 1 | 仅 `package-info.java` |

**已知代码事实**：Java 源文件共 **158**；契约 operationId **83**；`ErrorCode` 仅 **7** 个通用码；`SecurityConfig` 仍 `anyRequest().permitAll()`。

---

## 3. `docs/iteration1/` 结构（冻结，只读）

```text
docs/iteration1/
├── README.md
├── DELIVERY.md
├── FREEZE.md
├── progress-update-1.md
├── requirements.md
├── out-of-scope.md
├── permission-matrix.md
├── state-machines.md
├── rules.md
├── pages-and-flows.md
├── glossary-assumptions-questions.md
└── design/                        # 19 个文件
    ├── openapi.yaml               # 接口契约唯一真源（83 operationId）
    ├── api-contract.md
    ├── api-freeze.md
    ├── api-open-questions.md
    ├── api-permission-mapping.md
    ├── error-codes.md
    ├── idempotency-and-concurrency.md
    ├── schema.md                  # 25 张表 / 324 列
    ├── open-questions.md
    ├── SystemDesign.md
    ├── sequence-diagrams.md
    ├── er-diagram.md
    ├── db-verification.md
    ├── backend-skeleton-verification.md
    ├── controller-skeleton-verification.md
    ├── design-doc-verification.md
    ├── independent-verification.md
    ├── final-delivery-report.md
    └── README.md
```

- 顶层 **11 个 Markdown**，`design/` 下 **19 个文件**。
- **冻结，只读**：`FREEZE.md` 已登记本步产出文件与契约/迁移哈希；任何修改须由交付负责人裁决，并更新 `FREEZE.md`。

---

## 4. `docs/iteration2/` 结构（冻结候选）

```text
docs/iteration2/contracts/
├── README.md
├── data-conventions.md
├── decision-log.md
├── defaults-and-boundaries.md
├── mock-and-contract-testing.md
├── notification-events.md
├── open-questions.md
└── security-and-cors.md
```

- 共 **8 份 Markdown**。
- **冻结候选**：修改需接口契约负责人批准；本目录不修改 `openapi.yaml`、代码、迁移或 `docs/iteration1/**`。

---

## 5. `handover/` 结构（本任务生成后）

```text
handover/
├── .DS_Store                            # macOS 自动生成（保留，不修改）
├── HANDOVER-ITERATION1-TO-2.md          # 交接文档（现行版）
├── ITERATION-PLAN-VERTICAL-SLICES.md    # 垂直切片规划（现行版）
├── PROJECT-STRUCTURE.md                 # 本文件
├── COLLABORATION-RULES.md               # 协作规则全文
├── TEAM-ROADMAP-FOR-MEMBERS.md          # 团队路线通俗版
├── EXECUTION-CHECKLIST.md               # 执行层检查清单
└── DEFINITIONS-AND-PROTOCOLS.md         # 细节定义与协议
```

- 本目录共 **7 个 Markdown**：3 个交接事实文档 + 4 个补充文档。
- 生成前本目录仅有上述两个 PDF（见第 9 章登记）。
- `.DS_Store` 为 macOS 自动生成的非 PDF 文件，已由 `.gitignore` 忽略；按约束保留，不删除、不修改。

---

## 6. 迁移目录

```text
src/main/resources/db/migration/
├── V1__init_schema.sql                 # 25 张业务表、约束、索引、注释
├── V2__seed_reference_data.sql         # 区域/取货点/分类/成色
├── V3__seed_development_data.sql       # 开发种子（禁止用于生产）
└── V4__fix_review_window_index.sql     # FR-604 评价窗口索引修正
```

- **只读**：`V1`~`V4` 均不可修改（含注释、空白、换行）。
- 结构变更一律新增 `V5+`；新增前须在空库顺序验证 `V1` 到最新版全部成功。

---

## 7. 表与切片的对应

25 张表按切片 **7 / 3 / 9 / 6** 分布，合计 **25**。完整表名与受限写字段见
[`ITERATION-PLAN-VERTICAL-SLICES.md`](./ITERATION-PLAN-VERTICAL-SLICES.md) 第 6 章与附录 A；本文件不重复展开。

---

## 8. 目录与切片归属速查

| 目录 / 文件 | 归属 | 说明 |
| --- | --- | --- |
| `src/.../controller/AuthController`、`UserController`、`DictionaryController` | 切片 1 | 15 个 operationId |
| `src/.../service/AuthService`、`UserService`、`DictionaryService`（含 `impl`） | 切片 1 | 切片 1 填充实现 |
| `src/.../entity`、`repository` 中 User/TradeZone/Category/ItemCondition/PickupPoint/UserTradeZone/IdempotencyRecord | 切片 1 | 7 个实体与 7 个仓储 |
| `src/.../common/**`、`config/SecurityConfig` | 切片 1 定型 | 公共层；后续改动走「提需求 → 所有者改」 |
| `src/.../controller/ItemController` | 切片 2 | 10 个 operationId |
| `src/.../service/ItemService`（含 `impl`）、Item/ItemImage/ItemOccupation 相关仓储与实体 | 切片 2 | — |
| `src/.../controller/CartController`、`OrderController`、`SwapController` | 切片 3 | 4 + 8 + 16 = 28 个 operationId |
| `src/.../controller/InternalController` | 切片 3 / 切片 4 | 4 个 operationId，2/2 拆分 |
| `src/.../service/CartService`、`OrderService`、`SwapService`、`InternalTaskService`（含 `impl`） | 切片 3 / 切片 4 | `InternalTaskService` 两片共用 |
| `src/.../controller/AdminController`、`ReportController`、`ReviewController`、`NotificationController` | 切片 4 | 17 + 3 + 3 + 3 = 26 个 operationId |
| `docs/iteration1/**` | 只读 | 冻结资产 |
| `docs/iteration2/contracts/**` | 契约负责人 | 冻结候选；切片不得私改 |
| `src/main/resources/db/migration/V1~V4` | 只读 | 结构变更新增 `V5+` |
| `db/verify_schema.sql` | 只读 | 对账脚本 |
| `handover/**` | 文档维护人 | 本目录 7 个 Markdown（3 个交接事实 + 4 个补充文档） |
| `README-backend.md`、`README-db.md` | 文档维护人 | 不在任何切片的代码归属内 |
| 根目录旧版 `ITERATION1_TO_ITERATION2_HANDOVER.md` | 历史 | 保留供查阅；现行版在 `handover/` |
| `iteration1-delivery.zip` | 冻结归档 | 不解压、不修改 |

> 跨片调用只能依赖对方 Service 接口，禁止依赖 `ServiceImpl`；调用方不得直接写他片独占表（例外见 E-01~E-10）。

---

## 9. 已删除 PDF 登记

| 路径 | 类型 | SHA-256 | 删除原因 | 备份位置 | 是否被 `FREEZE.md` 登记 | 引用死链 |
| --- | --- | --- | --- | --- | --- | --- |
| `handover/HANDOVER-ITERATION1-TO-2.pdf` | PDF | `d3cfbb8b4716522639b4eae1e5bad3c7c3e7c01c671e7dfb2aac27692b0ec3e5` | MD 替代 PDF，便于 AI 读取、grep、diff、版本控制 | 小组项目群 | 否（FREEZE §1.1 无 PDF 项） | 无 Markdown 以该文件名引用 |
| `handover/PROJECT-STRUCTURE.pdf` | PDF | `c7114360df71eaf720eda812418ca233439a300e13b87231dfc914099efa54cf` | 同上 | 小组项目群 | 否 | 无 Markdown 以该文件名引用 |

**补充说明**

- 两个 PDF 均**不在** `iteration1-delivery.zip` 内（zip 内无 PDF），删除不影响冻结归档。
- 根目录旧交接文档中出现的 `ITERATION1_TO_ITERATION2_HANDOVER.pdf` 指向根目录，该文件本就不存在（历史遗留），与本次删除无关；引用方不在本次修改范围，可能产生死链。
- 若后续发现冻结材料登记了被删除的 PDF，由交付负责人决定更新 FREEZE 或补登删除记录；本文件不修改 `FREEZE.md`。

---

## 变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-11-11 | 交接包生成代理 | 首版：按生成时点实际结构列出根目录、`src/`、`docs/iteration1`、`docs/iteration2/contracts`、`handover/`、迁移目录；给出目录与切片归属速查；登记已删除的两个 PDF（路径、SHA-256、备份位置、FREEZE 情况、引用死链） | 生效 |
