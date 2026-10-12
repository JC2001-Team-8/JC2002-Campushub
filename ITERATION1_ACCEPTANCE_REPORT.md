# 迭代一 验收报告（ITERATION1 ACCEPTANCE REPORT）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 验收报告 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 验收人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 项目 | 校园/社区闲置交易平台 |
| 技术栈 | Java 11 + Spring Boot 2.7.18 + PostgreSQL 15 |
| 验收对象 | 迭代一全部交付物（需求、数据库、接口契约、工程骨架、接口骨架、设计文档、交付材料） |
| 验收方式 | **只读检查**：磁盘文件核查 + 哈希计算 + 运行时 HTTP 只读请求 + 数据库只读查询 |
| 验收基准时刻 | **2026-10-12 23:49:21 CST** |
| 结论 | ✅ **7 个验收项全部符合**（含 1 项"符合计划"的说明项）；无不符合项 |

> **声明**：本次验收**未修改任何代码、数据库迁移脚本、接口契约或需求文档**。
> 未执行任何会改变项目状态的命令（未运行 `mvn clean`、未执行 `docker compose down`、
> 未重启容器）。全部检查均为只读。证明见 §5「未修改证明」。

---

## 1. 验收概述

### 1.1 迭代一的目标

迭代一的目标是**把"要做什么"和"做成什么样"彻底定下来，并交付一个可直接开始编码的工程与接口骨架**。
具体为：确认服务范围（2–3 个校区及周边社区）、明确做与不做、交付关系图与规则与权限说明、
冻结接口清单（10/9 后不新增）、交付可启动的工程骨架、产出进度更新 1 材料。

### 1.2 验收范围

依据迭代一原始需求的核心点，本次验收覆盖 7 个方面：

| # | 验收项 | 对应的原始要求 |
| --- | --- | --- |
| A1 | 需求说明书与不做清单 | 明确做与不做；不做清单 ≥11 项且含 4 项核心 |
| A2 | 可点击原型 | 交付可点击原型 |
| A3 | 设计文档 | 商品/订单/交换关系图、交易与交换规则、各接口权限说明 |
| A4 | 接口清单与冻结 | 接口清单；10/9 后不许新增 |
| A5 | 工程骨架 | 能启动的工程骨架 |
| A6 | 进度更新 1 材料 | 10/12 进度更新 1 材料 |
| A7 | 服务范围确认 | 确认服务 2–3 个校区及周边社区 |

### 1.3 验收方法

| 手段 | 说明 |
| --- | --- |
| 文件核查 | 逐份读取交付文档，统计条目数与关键内容 |
| 哈希计算 | 对契约、迁移脚本、需求文档计算 SHA-256，与冻结声明比对 |
| 运行时只读请求 | `curl` 请求已运行的容器端点（GET/HEAD） |
| 数据库只读查询 | `SELECT` 查询 `flyway_schema_history` 与 `information_schema` |
| 交叉验证 | 契约 operationId ↔ Controller 方法双向比对 |

---

## 2. 验收清单

### A1 需求说明书与不做清单

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 需求说明书存在且完整 | `docs/iteration1/requirements.md` | ✅ 符合 | 含定位与边界、服务范围、7 类角色、功能范围、错误场景、追溯关系 |
| 功能需求条目化 | 同上 | ✅ 符合 | **54 条 FR**（FR-101~108、201~211、301~312、401~412、501~507、601~604），无重号 |
| 不做清单存在 | `docs/iteration1/out-of-scope.md` | ✅ 符合 | — |
| **不做清单 ≥11 项** | 同上 | ✅ 符合 | **实测 18 项**（OOS-01 ~ OOS-18），超出要求 7 项 |
| 含**真实支付** | OOS-01 | ✅ 符合 | 标题标注「【核心不做】」 |
| 含**多人交换链** | OOS-02 | ✅ 符合 | 标题标注「【核心不做】」 |
| 含**现金补差价** | OOS-03 | ✅ 符合 | 标题标注「【核心不做】」 |
| 含**部分接受交换包** | OOS-04 | ✅ 符合 | 标题标注「【核心不做】」 |
| 每项含原因与替代方案 | 同上 | ✅ 符合 | 每项含「不做 / 为什么不做 / 本迭代替代方案 / 未来迭代」 |
| 权限矩阵 | `docs/iteration1/permission-matrix.md` | ✅ 符合 | 7 类主体 × 6 大业务域，150 行表格；含硬性边界 P-00-A/B/C |
| 状态机 | `docs/iteration1/state-machines.md` | ✅ 符合 | 6 大状态机 + 评价状态机，**70 个转换编号**（I/O/S/B/P/AP） |
| 业务规则 | `docs/iteration1/rules.md` | ✅ 符合 | **34 条规则**（R-01 ~ R-35，R-29 预留） |
| 术语与假设 | `docs/iteration1/glossary-assumptions-questions.md` | ✅ 符合 | 术语表 + **20 条假设 A-01~A-20** + 34 条待确认问题 |

### A2 可点击原型

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 交付可点击原型 | — | ⚠️ **符合计划（未实现）** | **本迭代未实现前端原型**。`pages-and-flows.md` 第 12 行已明确声明：「本文档**只列页面与流程步骤**，不含 UI 设计稿、不含前端代码、不含可点击原型（迭代一不实现前端）」。页面与流程已完整定义（45 条页面条目），原型**留待后续迭代**（迭代六「前端与联调」） |

> **验收判断**：本迭代的实际计划是「只搭骨架和接口清单，业务未开始」，
> 原型未实现**符合本迭代计划**，不构成不符合项。
> 但需注意：**原始任务书字面要求"交付可点击原型"**，此项属**范围调整**而非交付缺陷，
> 建议在进度更新材料或评审时明确说明该调整已获认可。

### A3 设计文档

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| **商品/订单/交换关系图** | `docs/iteration1/design/er-diagram.md` | ✅ 符合 | **4 个 Mermaid ER 图**：① 核心交易链路总览 ② 占用关系详图（C-01/R-16/R-23）③ 交换关系详图（R-22/R-24/R-28）④ 治理关系详图（R-30/R-32/R-35）；另有基数汇总 |
| 关系图（汇总视图） | `docs/iteration1/design/SystemDesign.md` §3.1 | ✅ 符合 | 内嵌核心交易链路 ER 总览图（1 个 `erDiagram`） |
| 字段字典 | `docs/iteration1/design/schema.md` | ✅ 符合 | **25 张表 / 324 列**完整字段字典 |
| **交易与交换规则** | `docs/iteration1/rules.md` | ✅ 符合 | 34 条规则，覆盖库存占用（R-16）、幂等（R-17）、并发（R-18）、整包接受（R-22）、同时占用互斥（R-23）、整笔作废（R-24）、交换码一次性（R-26）、自动解封（R-31）等 |
| 规则摘要（设计视角） | `SystemDesign.md` §4.6 | ✅ 符合 | R-16 ~ R-35 逐条摘要 + 承载机制 |
| **各接口权限说明** | `docs/iteration1/design/api-permission-mapping.md` | ✅ 符合 | **503 行表格**；7 主体 × 6 域逐条 endpoint；公开 10 / 需登录 73 / 仅管理员 14 / 系统内部 4；56 条越权测试要点 |
| 权限说明（需求侧） | `docs/iteration1/permission-matrix.md` | ✅ 符合 | 与接口侧映射互相印证 |
| 权限说明（设计侧） | `SystemDesign.md` §5 | ✅ 符合 | 7 角色定义 + 矩阵摘要 + P-00-A/B/C 硬性边界 |
| 状态机文档 | `state-machines.md` + `SystemDesign.md` §4 | ✅ 符合 | 6 个 `stateDiagram` 图 + 转换编号体系 |
| 关键流程时序图 | `docs/iteration1/design/sequence-diagrams.md` | ✅ 符合 | **9 个 `sequenceDiagram`**，覆盖 7 组流程；每图含「参与角色/operationId/**覆盖 FR**/状态转换/规则/错误码」六项 |
| 统一系统设计文档 | `docs/iteration1/design/SystemDesign.md` | ✅ 符合 | 10 章 / **10 个 Mermaid 图**（4 flowchart + 1 erDiagram + 5 stateDiagram） |

> **结论**：设计文档要求的三要素（**关系图、交易与交换规则、接口权限说明**）**全部齐全且超出要求**
> （额外交付了系统设计文档与时序图）。

### A4 接口清单与冻结

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 接口清单存在 | `docs/iteration1/design/openapi.yaml` | ✅ 符合 | OpenAPI 3.0.3，`info.version = 1.0.0-frozen-candidate` |
| **接口数量** | 同上 | ✅ 符合 | **实测 83 个 operationId**（唯一性校验通过：83 = 83，无重号） |
| 接口规模 | 同上 | ✅ 符合 | **73 paths / 99 schemas / 13 tags** |
| 接口说明文档 | `docs/iteration1/design/api-contract.md` | ✅ 符合 | 83 个接口逐条清单 + **FR 覆盖矩阵 54/54 = 100%** |
| 错误码体系 | `docs/iteration1/design/error-codes.md` | ✅ 符合 | **93 个** `DOMAIN_REASON` 错误码（15 个域前缀）+ E-01~E-17 错误场景 |
| 幂等与并发说明 | `docs/iteration1/design/idempotency-and-concurrency.md` | ✅ 符合 | 16 必填 + 16 可选幂等接口；IDEM-A/B/C；12 张 `version` 表；7 个部分唯一索引 |
| **10/9 后不许新增** | `docs/iteration1/design/api-freeze.md` | ✅ 符合 | 明确声明「**2026-10-09 之后不允许新增接口**」（文档中 6 处强调）；含分级变更流程（3 人审批）与变更台账 |
| 冻结状态可核验 | `FREEZE.md` + 运行时 | ✅ 符合 | 契约 SHA-256 `36bd7a03a184734c…` 记入 `FREEZE.md`；运行时 `/v3/api-docs` 实测 **83 operations**，与契约 1:1 |
| 契约与代码对齐 | Controller ↔ openapi.yaml | ✅ 符合 | 12 个 Controller 共 **83 个方法**，双向差集为空（缺失 0 / 多余 0） |

### A5 工程骨架

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 工程可编译 | `pom.xml` + `src/` | ✅ 符合 | 158 个 Java 源文件；`mvn clean package` 成功（第 5 步验证记录：BUILD SUCCESS） |
| 工程可启动 | `CampusTradeApplication` | ✅ 符合 | 启动耗时约 3.2s（第 4/5 步验证记录） |
| **容器化一键启动** | `docker-compose.yml` | ✅ 符合 | 实测 `jc2001-app` 与 `jc2001-postgres` 均 **Up (healthy)** |
| **Flyway 迁移成功** | `src/main/resources/db/migration/V1~V4` | ✅ 符合 | 实库 `flyway_schema_history` **4 条记录，`success = t` 全部为真** |
| 迁移未修改 | 同上 | ✅ 符合 | SHA-256 与 `FREEZE.md` 记录一致；mtime 为 21:18~21:41（第 2 步产出时间） |
| 数据库结构落地 | PostgreSQL 15 | ✅ 符合 | 实库 **25 张业务表**（不含 `flyway_schema_history`） |
| **Swagger UI 可访问** | springdoc-openapi 1.7.0 | ✅ 符合 | `/swagger-ui.html` → HTTP **302** → `/swagger-ui/index.html`（200） |
| 运行时文档 | `/v3/api-docs` | ✅ 符合 | 实测 **83 operations**，与契约完全一致 |
| 冻结契约可访问 | `FrozenSpecController` | ✅ 符合 | `/openapi/openapi.yaml` → HTTP 200，**264,148 字节** |
| 健康检查 | `/actuator/health` | ✅ 符合 | HTTP 200 |
| 契约格式健康检查 | `/api/v1/health` | ✅ 符合 | HTTP 200，统一包络 `{"code":"OK","data":"UP",...}` |
| 接口骨架 | 12 Controller / 99 DTO / 12 Service | ✅ 符合 | 33 Request DTO + 66 Response DTO（含 22 枚举）；Service 为空实现 |

### A6 进度更新 1 材料

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 材料存在 | `docs/iteration1/progress-update-1.md` | ✅ 符合 | 版本 v1.0，日期 2026-10-12 |
| 含**已完成工作摘要** | 同上 §1 | ✅ 符合 | 「1. Summary of Work Done」——按 7 类产出组织（需求/数据库/契约/工程/接口/设计/本步） |
| 含**遇到的挑战与解决** | 同上 §2 | ✅ 符合 | 「2. Challenges Faced and Solutions」——**5 个真实挑战**（范围蔓延、构建提速 250×、并发占用数据库级保证、83 接口一致性、跨文档一致性） |
| 含**下一步计划** | 同上 §3 | ✅ 符合 | 「3. Next Steps」——迭代二三批次 + 后续迭代展望 + P0 待确认事项 |
| 有索引可发现 | `docs/iteration1/README.md` | ✅ 符合 | 索引第 10 行收录，阅读顺序新增「交付评审 / 验收」一行 |

### A7 服务范围确认

| 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- |
| 确认服务 2–3 个校区及周边社区 | `requirements.md` §2.1 | ⚠️ **部分符合** | 服务**模式**已确认（2–3 个校区 + 周边社区、预设交易区域、6 类公共取货点、不读定位/不填住址/不做地图），但**具体校区名称仍未确认**（**Q-01/ Q-02 / Q-03**，P0 待确认） |
| 当前处理方式 | 占位数据 | ⚠️ | 使用 `CAMPUS_A`、`COMMUNITY_A1`、`PICKUP_LIBRARY_A` 等占位值；**替换不改路径与 DTO 结构**，属纯数据替换 |
| 交付材料中已登记 | `DELIVERY.md` §4.1 + `progress-update-1.md` §3.3 | ✅ 符合 | 已作为 P0 待确认项登记，并说明「不阻塞迭代二启动」 |

---

## 3. 重点结论

### 3.1 迭代一是否覆盖原始业务

**结论：✅ 覆盖**。迭代一原始需求的 7 个核心点中，**6 项完全符合，1 项符合计划（原型）**。

| 原始要求 | 判定 |
| --- | --- |
| 确认服务 2–3 个校区及周边社区，明确做与不做 | ⚠️ 部分符合（**服务模式已定，具体校区名称待确认 Q-01~Q-03**） |
| 交付可点击原型 | ⚠️ 符合计划（本迭代只搭骨架与接口清单，**原型留待后续**，已在 `pages-and-flows.md` 声明） |
| 设计文档：商品/订单/交换关系图、交易与交换规则、各接口权限说明 | ✅ 符合（且超出要求，额外交付系统设计文档与时序图） |
| 接口清单，10/9 后不许新增 | ✅ 符合（83 operationId，冻结声明明确） |
| 能启动的工程骨架 | ✅ 符合（容器健康、Flyway 4 条成功、Swagger UI 可访问） |
| 10/12 进度更新 1 材料 | ✅ 符合（三大节齐备） |
| 不做清单 ≥11 项，含 4 项核心 | ✅ 符合（18 项，4 项核心全覆盖） |

### 3.2 不符合项

**无不符项（0 项）。**

### 3.3 部分符合项与建议

| # | 项 | 现状 | 建议 |
| --- | --- | --- | --- |
| 1 | **具体校区/交易区域/取货点名称未确认**（Q-01/Q-02/Q-03） | 使用占位数据 `CAMPUS_A` 等 | ① 尽快向需求方确认；② 确认后**只替换数据**（`V2__seed_reference_data.sql` 的种子值），**不改契约、不改路径、不改 DTO**；③ 因 V2 已应用于开发库，替换需新增 `V5+` 迁移或仅改种子脚本后重建库 |
| 2 | **可点击原型未交付** | `pages-and-flows.md` 已列 45 条页面与 4 条流程，但无前端代码 | ① 若课程/评审要求必须有原型，建议明确"迭代一以骨架与契约为交付边界"并取得认可；② 否则在迭代六（前端与联调）补做 |
| 3 | **管理员账号来源未确认**（Q-04） | 种子创建 `admin@example.com`（占位密码） | 生产部署前确认；**注意 `V3__seed_development_data.sql` 禁止在生产执行** |

### 3.4 额外发现（不影响验收结论，供参考）

| # | 发现 | 说明 |
| --- | --- | --- |
| 1 | `error-codes.md` 中错误码统计为 **93 个** | 用正则粗筛会得到 95 个，其中 `PICKUP_POINT_` 与 `TRADE_ZONE_` 是**域前缀名**（非错误码），需排除。交付文档中声明的 93 个**准确** |
| 2 | 仓库**不是 git 仓库**（无 `.git`） | 因此「某文件仅改 1 行」这类**改动范围**无法独立测量，只能测最终态。已在 `FREEZE.md` §3 如实标注，并以 SHA-256 提供内容级校验 |
| 3 | 存在 2 个 `.DS_Store`（macOS 自动生成） | 其中一个位于 `src/`。**非本任务产物**，但建议加入 `.gitignore`（提交 GitHub 时排除），见交接文档 §6 |
| 4 | 接口骨架方法体全部抛 `UnsupportedOperationException` | 属**预期**（迭代一不实现业务逻辑）。调用业务接口返回 500（未登录的需鉴权接口返回 403/401） |

---

## 4. 逐项验收汇总表

| # | 需求项 | 交付物 | 符合性 | 证据 / 备注 |
| --- | --- | --- | --- | --- |
| A1 | 需求说明书与不做清单 | `requirements.md`、`out-of-scope.md`、`permission-matrix.md`、`state-machines.md`、`rules.md`、`glossary-assumptions-questions.md` | ✅ **符合** | 54 FR；不做清单 **18 项**（≥11）含 4 项核心；7×6 权限矩阵；70 转换编号；34 规则；20 假设 |
| A2 | 可点击原型 | `pages-and-flows.md`（仅页面与流程定义） | ⚠️ **符合计划（未实现）** | 45 条页面 + 4 条流程已定义；第 12 行明确声明不含可点击原型，留待后续迭代 |
| A3 | 设计文档（关系图 / 规则 / 权限） | `er-diagram.md`、`rules.md`、`api-permission-mapping.md`、`permission-matrix.md`、`SystemDesign.md`、`sequence-diagrams.md` | ✅ **符合**（超出要求） | 4 个 ER 图；34 条规则；503 行权限映射；另交付 10 章设计文档与 9 个时序图 |
| A4 | 接口清单与冻结 | `openapi.yaml`、`api-contract.md`、`error-codes.md`、`api-freeze.md` | ✅ **符合** | **83 operationId**；73 paths/99 schemas；93 错误码；FR 54/54；已声明 10/9 后不新增 |
| A5 | 工程骨架 | `pom.xml`、`Dockerfile`、`docker-compose.yml`、`src/` | ✅ **符合** | 容器双健康；Flyway **4 条全成功**；25 张表；Swagger UI 302→200；`/v3/api-docs` = 83 |
| A6 | 进度更新 1 材料 | `progress-update-1.md` | ✅ **符合** | 三大节齐备（Summary / Challenges / Next Steps）；5 个真实挑战 |
| A7 | 确认服务 2–3 个校区及周边社区 | `requirements.md` §2.1 | ⚠️ **部分符合** | 服务模式已定；**具体校区名称待确认**（Q-01~Q-03，有占位数据与替换方案） |

**统计**：✅ 符合 **5** 项 · ⚠️ 部分符合/符合计划 **2** 项 · ❌ 不符合 **0** 项。

---

## 5. 未修改证明

### 5.1 声明

本次验收**只执行只读操作**：文件读取、`shasum`、`curl`（GET）、`psql`（SELECT）、`find`、`stat`、`docker ps`。
**未修改任何代码、数据库迁移脚本、接口契约或需求文档**；
**未执行** `mvn clean`、`docker compose down`、`docker compose up`、数据库写入等状态变更命令。

### 5.2 关键文件哈希（验收后复核，应与 §5.4 基线一致）

| 文件 | SHA-256（前 16 位） | 期望（`FREEZE.md` 记录） | 一致 |
| --- | --- | --- | --- |
| `docs/iteration1/design/openapi.yaml` | `36bd7a03a184734c` | `36bd7a03a184734c` | ✅ |
| `src/main/resources/db/migration/V1__init_schema.sql` | `620ad94bb6cf96b2` | `620ad94bb6cf96b2` | ✅ |
| `V2__seed_reference_data.sql` | `8a5f22d53904761a` | `8a5f22d53904761a` | ✅ |
| `V3__seed_development_data.sql` | `ab3c9c105aed2eea` | `ab3c9c105aed2eea` | ✅ |
| `V4__fix_review_window_index.sql` | `cbd952e089a2d0af` | `cbd952e089a2d0af` | ✅ |

> 以上 5 个文件的哈希在**验收前后完全一致**，且与 `docs/iteration1/FREEZE.md` 记录的冻结值一致
> → **契约与迁移脚本未被修改**。

### 5.3 代码未被修改

| 检查 | 结果 |
| --- | --- |
| `src/` 下最新 `.java` 文件 mtime | **2026-10-12 22:44**（本任务基准时刻为 23:49，早于 1 小时以上） |
| `src/` 下 Java 文件总数 | **158**（与第 5 步交付数量一致） |
| 本任务窗口（23:45 之后）被修改/创建的文件 | 仅 `tools/md2pdf.py`（本任务新建的 PDF 生成工具） |

### 5.4 验收前基线（2026-10-12 23:49:21 CST）

```
契约与迁移:
  36bd7a03a184734c  docs/iteration1/design/openapi.yaml
  620ad94bb6cf96b2  src/main/resources/db/migration/V1__init_schema.sql
  8a5f22d53904761a  src/main/resources/db/migration/V2__seed_reference_data.sql
  ab3c9c105aed2eea  src/main/resources/db/migration/V3__seed_development_data.sql
  cbd952e089a2d0af  src/main/resources/db/migration/V4__fix_review_window_index.sql
  5ae0dd22051d4765  pom.xml
  20c13f70184e9c63  docker-compose.yml
  f92c296d3ed4340d  Dockerfile
  56bf49d8b7447a92  Dockerfile.build

需求文档:
  c2b10fb0c23a46dc  docs/iteration1/requirements.md
  fdf85393bc82720f  docs/iteration1/out-of-scope.md
  f6de92b743dd6fc3  docs/iteration1/permission-matrix.md
  292cb597beaf6251  docs/iteration1/state-machines.md
  0901806d0fc4b172  docs/iteration1/rules.md
  d735d91a4cf5a9e6  docs/iteration1/pages-and-flows.md
  01ea1e18df048370  docs/iteration1/glossary-assumptions-questions.md
```

> **说明**：仓库不是 git 仓库，故无法提供 `git status` 输出。
> 上述 SHA-256 基线可作为等价的内容级证据：任何文件被修改都会导致哈希变化。

---

## 6. 验收结论

> **迭代一交付物符合原始业务要求，验收通过。**
>
> - ✅ **5 项完全符合**：需求与不做清单、设计文档、接口清单与冻结、工程骨架、进度更新 1 材料。
> - ⚠️ **2 项部分符合/符合计划**：可点击原型（本迭代计划内未实现，已在文档中声明）、
>   校区与区域的具体名称（服务模式已定，名称待 Q-01~Q-03 确认，有占位数据与替换方案）。
> - ❌ **0 项不符合**。
>
> 两项部分符合**均不构成交付缺陷**：其一属迭代计划内的范围安排，其二属待需求方确认的输入项，
> 且**替换占位数据不影响已冻结的接口契约**。

---

## 7. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 验收人 | 首版：7 项验收清单（含逐项证据）、重点结论（覆盖情况与部分符合项建议）、逐项汇总表、未修改证明（哈希基线 + mtime 检查）、验收结论 | 草稿（待确认） |
