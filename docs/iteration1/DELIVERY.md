# 迭代一 交付说明（DELIVERY）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 交付说明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 交付负责人（待填写姓名） |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** |
| 迭代 | 迭代一：需求基线 / 数据库设计 / 接口契约 / 工程骨架 / 接口骨架 / 设计文档 |
| 契约版本 | `1.0.0-frozen-candidate`（OpenAPI 3.0.3，73 paths / **83 operations** / 99 schemas） |
| 打包产物 | `iteration1-delivery.zip`（由 `scripts/package-iteration1.sh` 生成；**≈ 712 KB / 205 个文件**，精确值以 `wc -c iteration1-delivery.zip` 当场实测为准） |
| 配套材料 | [`progress-update-1.md`](./progress-update-1.md)（进度更新 1）、[`design/final-delivery-report.md`](./design/final-delivery-report.md)（最终交付报告） |

> **一句话结论**：迭代一的**需求、设计、数据库、接口契约与工程骨架全部完成并验证通过**；
> **业务逻辑尚未实现**（83 个 operationId 的方法体统一抛 `UnsupportedOperationException`）。
> 团队可**直接依据冻结契约开始编码**。

---

## 1. 交付物清单

### 1.1 需求文档（`docs/iteration1/`，8 份）

| 文件 | 内容 | 规模要点 |
| --- | --- | --- |
| [`requirements.md`](./requirements.md) | 需求说明书 | **54 条 FR**、7 类角色、7 条约束 C-01~C-07 |
| [`out-of-scope.md`](./out-of-scope.md) | 不做清单 | **18 项**（含 4 项核心：OOS-01~OOS-04） |
| [`permission-matrix.md`](./permission-matrix.md) | 权限矩阵 | 7 主体 × 6 权限域 |
| [`state-machines.md`](./state-machines.md) | 状态机 | 6 大状态机 + 评价状态机，**70 个转换编号** |
| [`rules.md`](./rules.md) | 交易与交换规则 | **34 条**（R-01~R-35） |
| [`pages-and-flows.md`](./pages-and-flows.md) | 页面与流程 | 22 个页面 + 4 条关键流程 |
| [`glossary-assumptions-questions.md`](./glossary-assumptions-questions.md) | 术语、假设、待确认问题 | 术语表 + **20 条假设 A-01~A-20** + 34 条问题 |
| [`README.md`](./README.md) | 迭代一文档索引 | 阅读顺序与产出说明 |

### 1.2 数据库设计与迁移（`docs/iteration1/design/` + `src/main/resources/db/migration/`）

| 交付物 | 内容 | 规模要点 |
| --- | --- | --- |
| [`design/er-diagram.md`](./design/er-diagram.md) | ER 图 | 4 张 Mermaid 图（总览 + 占用 / 交换 / 治理详图）+ 基数汇总 |
| [`design/schema.md`](./design/schema.md) | 字段字典 | **25 张业务表 / 324 列** |
| [`design/db-verification.md`](./design/db-verification.md) | 数据库验证记录 | 迁移验证 + **19 项负向测试** |
| [`design/open-questions.md`](./design/open-questions.md) | 数据库待确认问题 | **25 个 DQ** + 19 项设计决策 DD |
| `V1__init_schema.sql` | 建表 | 25 张业务表、26 个枚举、324 条列注释、43 个命名 CHECK |
| `V2__seed_reference_data.sql` | 参照数据 | 7 交易区域 / 13 取货点 / 6 分类 / 5 成色 |
| `V3__seed_development_data.sql` | 开发种子数据 | 4 用户 / 9 商品 / 4 订单 / 2 交换 / 2 处罚 / 1 申诉 / 5 通知 |
| `V4__fix_review_window_index.sql` | DQ-27 修复 | 重建 `idx_orders_review_window`（排除终态） |
| `db/verify_schema.sql` | 结构与一致性校验脚本 | 25 项只读检查 |

### 1.3 接口契约（`docs/iteration1/design/`，7 份）

| 文件 | 内容 | 规模要点 |
| --- | --- | --- |
| [`design/openapi.yaml`](./design/openapi.yaml) | **OpenAPI 3.0.3 契约（唯一真源）** | 73 paths / **83 operations** / 99 schemas / 13 tags |
| [`design/api-contract.md`](./design/api-contract.md) | 接口清单与 FR 映射 | 83 个接口逐条 + **FR 54/54 = 100%** |
| [`design/api-permission-mapping.md`](./design/api-permission-mapping.md) | 权限映射 | 7 主体 × 6 域；公开 10 / 需登录 73 / 仅管理员 14 / 内部 4；56 条越权测试 |
| [`design/error-codes.md`](./design/error-codes.md) | 错误码 | **93 个** `DOMAIN_REASON`（15 个域前缀）+ E-01~E-17 |
| [`design/idempotency-and-concurrency.md`](./design/idempotency-and-concurrency.md) | 幂等与并发 | 16 必填 + 16 可选幂等接口；IDEM-A/B/C；12 张 `version` 表；7 个部分唯一索引 |
| [`design/api-freeze.md`](./design/api-freeze.md) | 冻结声明与变更流程 | **10/9 后不新增**；变更需 3 人审批 |
| [`design/api-open-questions.md`](./design/api-open-questions.md) | 契约待确认问题 | **18 条 AQ**（4 条 P0）+ 20 项契约决策 AD |

### 1.4 工程骨架（仓库根目录）

| 交付物 | 内容 |
| --- | --- |
| `pom.xml` | Spring Boot 2.7.18 / Java 11；web、security、data-jpa、validation、actuator、flyway、postgresql、springdoc 1.7.0、jjwt 0.11.5；`finalName=campus-trade` |
| `Dockerfile` | **默认运行镜像**（复制 jar，不容器内构建）；非 root（uid 1500）；`ENTRYPOINT java $JAVA_OPTS -jar` |
| `Dockerfile.build` | 可选容器内完整构建（含依赖层缓存），供 CI |
| `docker-compose.yml` | `postgres:15` + `app`；健康检查与 `depends_on: service_healthy`；卷 `jc2001_pgdata`、`campus_trade_uploads` |
| `.dockerignore` | 排除 `.mvnrepo/`、`.git/`、日志、`data/` 等（**不排除 `target/`**） |
| `README-backend.md` | 启动方式（3 种）、验证步骤、配置说明、常见问题 |
| `README-db.md` | 数据库设计与启动说明 |
| [`design/backend-skeleton-verification.md`](./design/backend-skeleton-verification.md) | 工程骨架验证记录（含构建提速 38min → 9s） |

### 1.5 接口骨架（`src/main/java/com/jc2001/campustrade/`）

| 交付物 | 规模 | 说明 |
| --- | --- | --- |
| `controller/` | **12 个契约 Controller + 2 个自检 Controller** | 覆盖 **83 个 operationId**，1:1 对齐 |
| `dto/request/` | **33 个** | 请求 DTO，含 Bean Validation 注解 |
| `dto/response/` | **66 个**（含 **22 个枚举**） | 响应 DTO 与共享类型 |
| `service/` + `service/impl/` | **12 + 12 个** | 接口就位；实现**统一抛 `UnsupportedOperationException`** |
| `common/` | ApiResponse / PageResult / ErrorResponse / ErrorCode / GlobalExceptionHandler / ApiErrorController / TraceIdFilter | 统一响应与异常 |
| `config/` | SecurityConfig / OpenApiConfig / JacksonConfig / AppProperties | 配置层 |
| `security/` | JwtTokenProvider / JwtAuthenticationFilter / AuthenticatedUser / UserDetailsServiceImpl | JWT 认证 |
| `repository/`、`entity/` | 占位包 | 下一步按 `schema.md` 生成 |
| [`design/controller-skeleton-verification.md`](./design/controller-skeleton-verification.md) | 验证记录 | **83/83 对账、16+16 幂等、权限 10/55/14/4** |

### 1.6 设计文档（`docs/iteration1/design/`）

| 文件 | 内容 | 规模 |
| --- | --- | --- |
| [`design/SystemDesign.md`](./design/SystemDesign.md) | **统一系统设计文档** | 10 章 / **10 张 Mermaid 图** |
| [`design/sequence-diagrams.md`](./design/sequence-diagrams.md) | 关键流程时序图（v1.1） | **9 个 `sequenceDiagram`** / 7 组流程 / 含 `覆盖 FR` 追溯 |
| [`design/README.md`](./design/README.md) | 设计目录索引 | 含建议阅读顺序 |

### 1.7 验证记录（4 份）

| 文件 | 验证对象 | 结论 |
| --- | --- | --- |
| [`design/db-verification.md`](./design/db-verification.md) | 数据库迁移 V1~V4 | ✅ 通过（含 19 项负向测试） |
| [`design/backend-skeleton-verification.md`](./design/backend-skeleton-verification.md) | 工程骨架 | ✅ 通过 |
| [`design/controller-skeleton-verification.md`](./design/controller-skeleton-verification.md) | 接口骨架 83 operationId | ✅ 通过（83/83 1:1） |
| [`design/design-doc-verification.md`](./design/design-doc-verification.md) | 设计文档补齐（v1.1） | ✅ 通过（13 项交叉引用检查全通过） |
| [`design/final-delivery-report.md`](./design/final-delivery-report.md) | **本步骤最终交付** | ✅ 通过（10 项验收自检） |

### 1.8 打包与脚本

| 交付物 | 内容 |
| --- | --- |
| `scripts/package-iteration1.sh` | 一键打包脚本，输出 `iteration1-delivery.zip` |
| `tools/generate_api_skeleton.py` | 契约 → 骨架生成器（**经交付负责人确认予以保留**：契约变更或迭代二补接口时可重复再生成，持续保证 83/83 的 1:1 对齐） |

---

## 2. 验收状态总览

| 步骤 | 交付内容 | 验收结果 | 关键证据 |
| --- | --- | --- | --- |
| **1. 需求基线** | 8 份需求文档 | ✅ 通过 | 54 FR / 18 不做项 / 7×6 权限矩阵 / 70 转换编号 / 34 规则 / 20 假设 |
| **2. 数据库设计** | ER 图、字段字典、V1~V4 | ✅ 通过 | 25 表 / 324 列 / 4 条迁移 `success=t` / Flyway `validate` 通过 |
| **3. 接口契约冻结** | OpenAPI 3.0.3 + 6 份契约文档 | ✅ 通过 | 73 paths / **83 operations** / 99 schemas / 93 错误码 / FR 54/54 |
| **4. 工程骨架** | pom / Docker / Compose / 公共层 | ✅ 通过 | 编译成功、启动成功、`/actuator/health` UP、Flyway 成功 |
| **5. 接口骨架** | 12 Controller / 99 DTO / 12 Service | ✅ 通过 | **83/83 operationId 1:1**；幂等 16+16；权限 10/55/14/4；`/v3/api-docs` = 83 |
| **6. 设计文档** | SystemDesign + 时序图 + 验证记录 | ✅ 通过 | 13 项交叉引用检查全通过；19 个 Mermaid 块语法 0 问题 |
| **7. 交付打包**（本步） | DELIVERY / 进度更新 / 打包脚本 / 修复 | ✅ 通过 | DDP-01 已修复、P-01 已闭环、10 项验收自检通过 |

**汇总**：7 个步骤**全部通过**；累计 **4 份验证记录**、**0 项未解决阻塞问题**。

### 2.1 本步骤修复项

| 编号 | 类型 | 内容 | 状态 |
| --- | --- | --- | --- |
| **DDP-01** | 上游文档引用错误 | `state-machines.md:189` 的 S-07（整笔作废）误引 `R-22`，应为 `R-24` | ✅ **已修复**（经用户授权，仅改 1 处引用，规则语义未变） |
| **P-01** | 追溯链优化 | `sequence-diagrams.md` 未直接引用 FR，需两步跳转 | ✅ **已闭环**（9 张图补 `覆盖 FR` 行、§8 补 `覆盖 FR` 列，引用 35 个真实 FR） |

---

## 3. 接口冻结声明

> **本迭代接口契约为冻结候选版本 `1.0.0-frozen-candidate`。**
> **2026-10-09 之后不允许新增接口。**

| 项 | 状态 |
| --- | --- |
| 冻结的 paths / operations | 73 / **83** |
| 冻结的 schemas | 99 |
| 冻结的错误码 | 93 |
| 允许的变更 | 修正 `description` 文案、补充 `example`、补充**可选**响应字段（向后兼容）、修正错误 |
| 禁止的变更 | 新增/删除接口；修改路径 / HTTP 方法 / operationId；删除或重命名 DTO 字段；收窄枚举取值；变更必填性；变更错误码语义 |
| 变更流程 | 见 [`design/api-freeze.md`](./design/api-freeze.md) §4（影响面评估 + 审批 + 台账登记） |
| 唯一例外 | 占位数据（校区 / 社区 / 取货点）的**具体值**替换不属契约变更（不改路径与 DTO 结构） |

**运行时一致性**：`/v3/api-docs` 的 operationId 与契约**完全 1:1**（缺失 0 / 多余 0 / 路径与方法不一致 0）。
验证方法与证据见 [`design/controller-skeleton-verification.md`](./design/controller-skeleton-verification.md) §2。

---

## 4. 假设与待确认问题

### 4.1 P0 待确认（不阻塞交付，但需在迭代二前确认）

| 编号 | 来源 | 问题 | 当前处理策略 |
| --- | --- | --- | --- |
| **Q-01 / Q-02 / Q-03** | 需求 | 服务哪 2–3 个校区及周边社区 / 交易区域清单与编码 / 公共取货点清单与命名 | ⚠️ **仍使用占位数据**（`CAMPUS_A`、`COMMUNITY_A1`、`PICKUP_LIBRARY_A` 等）。**替换不改变路径与 DTO 结构**，属纯数据替换 |
| **Q-04** | 需求 | 管理员账号来源 | 种子创建 `admin@example.com`（占位密码）。**V3 禁止在生产执行** |
| **DQ-01** | 数据库 | 是否启用商品「草稿」态 | **不启用**（发布即 `IN_SALE`）。若启用属契约破坏性变更 |
| **DQ-05** | 数据库 | 封禁能否延长 / 覆盖 / 调时长 | **固定 72 小时且拒绝重复封禁**。放宽属破坏性变更 |
| **AQ-01** | 契约 | 通知开关是否纳入迭代一 | **纳入**（仅站内渠道）。撤销需删除 2 个操作 |
| **AQ-02** | 契约 | `refreshToken` 是否启用 | 保留路径，**默认返回 501**。启用仅切开关 |
| **AQ-03** | 契约 | 字典是否需在线维护写接口 | **仅只读**；写接口需走契约变更流程 |
| **AQ-04** | 契约 | 同用户多条不同类型生效处罚是否允许 | 由 `uq_penalties_one_active` 与 Service 校验决定 |

### 4.2 P1 / P2 待确认（已登记，不阻塞）

| 编号段 | 来源 | 主要内容 |
| --- | --- | --- |
| Q-05 ~ Q-34 | 需求 | 通知渠道、refresh token 细节、已付款取消、处罚期收尾操作、交换双方长期不确认等 |
| DQ-02 ~ DQ-27 | 数据库 | 软删除策略、命名规范、`ACCEPTED` 阶段下架的 `swap_codes` 处置、FR-604 扫描条件（**DQ-27 已闭环**） |
| AQ-05 ~ AQ-18 | 契约 | 分页默认值、错误码粒度、图片上传细节、限流策略等 |

> 完整清单见 [`glossary-assumptions-questions.md`](./glossary-assumptions-questions.md)、
> [`design/open-questions.md`](./design/open-questions.md)、
> [`design/api-open-questions.md`](./design/api-open-questions.md)。

### 4.3 本步骤新登记

| 编号 | 内容 | 状态 |
| --- | --- | --- |
| DQ-29 | `termination_reason` / `cancel_reason` / `lift_source` 的枚举字面量未在 `schema.md` 明文列出（实际已在 `V1` 中以 PostgreSQL ENUM 落地并实测） | 待补文档说明 |
| DQ-30 | 占用建立之后卖家自行下架的语义（拒绝 vs 整笔作废） | 待确认 |
| DQ-31 | `state-machines.md` 用 `ROLE_RESTRICTED`/`ROLE_USER`（角色名），`schema.md` 用 `RESTRICTED`/`ACTIVE`（状态值）—— 属「角色 vs 状态」两层面，**不冲突**，建议统一措辞 | 待统一措辞 |
| DQ-32 | `POST /api/v1/internal/tasks/*` 的调度周期与单批处理上限未在契约中规定 | 待确认（不影响契约） |

---

## 5. 环境要求

| 组件 | 版本要求 | 用途 | 验证版本 |
| --- | --- | --- | --- |
| **JDK** | **11**（必需，不支持 8 / 17+） | 编译与运行 | Temurin 11.0.32.1 |
| **Maven** | 3.6+ | 构建（**非必需**：可用 `Dockerfile.build` 在容器内构建） | 3.9 |
| **Docker** | 20.10+ | 容器化运行 | 29.8.1 |
| **Docker Compose** | v2（`docker compose`） | 一键起栈 | v2 |
| **PostgreSQL** | **15** | 数据库（可由 Compose 提供，无需本机安装） | 15.19 |
| 磁盘 | ≥ 2 GB 可用 | 镜像 + 卷 + 构建产物 | — |
| 网络 | 首次需可访问镜像仓库与 Maven 中央仓 | 拉取基础镜像与依赖 | — |

> **说明**：应用本身**不需要**本机安装 Maven 或 PostgreSQL —— `docker compose up` 即可起完整栈。
> 若要本机构建，需要 JDK 11 + Maven。

---

## 6. 启动与验证步骤

完整说明见 [`README-backend.md`](../../README-backend.md)。以下为**快速验证路径**。

### 6.1 方式一：Docker Compose（推荐）

```bash
# ① 打包（首次必须；容器镜像只复制产物，不在容器内构建）
docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-11 mvn -B -DskipTests clean package
#   若本机已装 Maven：mvn clean package -DskipTests

# ② 起栈（默认 8080 / 5432；端口被占用时可覆盖）
DOCKER_BUILDKIT=0 docker compose -p jc2001 up -d --build

# 若宿主机 5432 已被占用，改用：
# POSTGRES_HOST_PORT=55432 APP_HOST_PORT=8080 DOCKER_BUILDKIT=0 docker compose -p jc2001 up -d --build

# ③ 等待健康
docker compose -p jc2001 ps          # 期望 app 与 postgres 均 (healthy)
```

### 6.2 方式二：本机运行（需 JDK 11 + 可访问的 PostgreSQL）

```bash
mvn clean package -DskipTests
java -jar target/campus-trade.jar --spring.profiles.active=dev
```

### 6.3 验证清单（6 项，全部应通过）

```bash
# ① 健康检查（Spring Boot 标准）
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
# 期望 200

# ② 契约格式健康检查（统一响应包络）
curl -s http://localhost:8080/api/v1/health
# 期望 {"code":"OK","message":"服务正常","data":"UP",...}

# ③ 就绪检查（数据库 + Flyway）
curl -s http://localhost:8080/api/v1/health/ready
# 期望 database=UP、flyway.appliedCount=4、currentVersion=4、status=UP

# ④ 运行时文档操作数量（关键验收项）
curl -s http://localhost:8080/v3/api-docs | python3 -c \
  "import json,sys;d=json.load(sys.stdin);print('operations =',sum(1 for p,i in d['paths'].items() for m in i if m in ('get','post','put','patch','delete')))"
# 期望 operations = 83

# ⑤ Swagger UI
curl -s -o /dev/null -w "%{http_code} -> %{redirect_url}\n" http://localhost:8080/swagger-ui.html
# 期望 302 -> /swagger-ui/index.html

# ⑥ 冻结契约原文（字节应与 docs/iteration1/design/openapi.yaml 一致）
curl -s -o /dev/null -w "%{http_code} %{size_download}\n" http://localhost:8080/openapi/openapi.yaml
# 期望 200 264148
```

### 6.4 数据库验证（可选）

```bash
# 迁移记录应为 4 条且全部成功
docker exec jc2001-postgres psql -U campus_trade -d campus_trade \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# 结构一致性校验（25 项只读检查）
docker exec -i jc2001-postgres psql -U campus_trade -d campus_trade < db/verify_schema.sql
```

### 6.5 打包交付物

```bash
bash scripts/package-iteration1.sh
# 输出：iteration1-delivery.zip
```

---

## 7. 已知限制

| # | 限制 | 说明 | 影响 |
| --- | --- | --- | --- |
| 1 | **未实现业务逻辑** | 83 个 Controller 方法与 83 个 Service 实现方法**统一抛 `UnsupportedOperationException`**；`repository/`、`entity/` 为占位包 | 系统**不能完成一笔真实交易**；调用业务接口返回 500 |
| 2 | **占位数据未替换** | 交易区域 / 取货点 / 分类 / 成色 / 用户均为占位值（`CAMPUS_A` 等） | 需在 Q-01~Q-04 确认后替换（**不改契约**） |
| 3 | **`ErrorCode` 仅 7 个通用码** | 93 个业务错误码尚未落到枚举 | 业务错误目前只能返回通用码；下一步须按 `error-codes.md` 逐字补齐 |
| 4 | **权限未收紧** | `SecurityConfig` 仍为 `anyRequest().permitAll()` + TODO；接口层 `@PreAuthorize` 已就位 | 归属校验与处罚拦截须在 Service 层实现 |
| 5 | **无真实支付 / 退款 / 物流** | 模拟付款打标；不接任何真实资金（OOS-01 / OOS-14 / OOS-16） | 属迭代一明确不做项 |
| 6 | **无前端** | 迭代一仅交付后端与契约；Swagger UI 可作临时联调界面 | 前端属迭代二 |
| 7 | **通知开关默认关闭** | `emailEnabled` 恒 `false`；仅站内通知（AQ-01 / Q-05） | 如需邮件/短信渠道须扩展 |
| 8 | **`refreshToken` 返回 501** | 契约保留路径但未启用（AQ-02 / Q-07） | 启用仅需切开关，不改契约 |
| 9 | **无生产级部署方案** | 无 K8s / 监控 / 备份 / 限流（OOS-17） | 当前为开发验证级部署 |
| 10 | **容器镜像偏大（~954 MB）** | 运行镜像基于 `maven:3.9-eclipse-temurin-11`（因构建期 Docker Hub 不可达，无 JRE 基础镜像可用） | 生产可换 `eclipse-temurin:11-jre` 显著瘦身（已在 `README-backend.md` 说明） |
| 11 | **开发种子数据不可用于生产** | `V3__seed_development_data.sql` 含占位账号与密码 | **生产环境禁止执行 V3** |
| 12 | **Flyway 8.5.13 对 PG 15 有 untested 警告** | Boot 2.7 的 `FlywayAutoConfiguration` 绑定 Flyway 8 API（9.x 已移除），Flyway 10 需 Java 17 | 迁移实际成功，不影响交付；已在验证记录说明 |

---

## 8. 交付包结构

```
iteration1-delivery.zip
├── README-backend.md              # 后端启动与验证
├── README-db.md                   # 数据库设计说明
├── pom.xml
├── Dockerfile                     # 默认运行镜像
├── Dockerfile.build               # 可选容器内构建
├── docker-compose.yml
├── .dockerignore
├── docs/
│   └── iteration1/
│       ├── README.md
│       ├── DELIVERY.md            # 本文件
│       ├── progress-update-1.md   # 进度更新 1
│       ├── requirements.md
│       ├── out-of-scope.md
│       ├── permission-matrix.md
│       ├── state-machines.md
│       ├── rules.md
│       ├── pages-and-flows.md
│       ├── glossary-assumptions-questions.md
│       └── design/
│           ├── README.md
│           ├── SystemDesign.md
│           ├── sequence-diagrams.md
│           ├── openapi.yaml       # 契约唯一真源
│           ├── api-contract.md
│           ├── api-permission-mapping.md
│           ├── error-codes.md
│           ├── idempotency-and-concurrency.md
│           ├── api-freeze.md
│           ├── api-open-questions.md
│           ├── er-diagram.md
│           ├── schema.md
│           ├── open-questions.md
│           ├── db-verification.md
│           ├── backend-skeleton-verification.md
│           ├── controller-skeleton-verification.md
│           ├── design-doc-verification.md
│           └── final-delivery-report.md
├── db/
│   └── verify_schema.sql
├── scripts/
│   └── package-iteration1.sh
├── src/                           # 工程与接口骨架（158 个 Java 文件）
└── tools/
    └── generate_api_skeleton.py   # 契约→骨架生成器（保留以便再生成）
```

**明确排除**：`target/`、`.mvnrepo/`、`.git/`、`.idea/`、`.vscode/`、`__pycache__/`、`*.log`、`data/`、`uploads/`、`iteration1-delivery.zip` 自身。

---

### 8.1 收尾清理决定（第 7 步）

| 对象 | 决定 | 理由 |
| --- | --- | --- |
| `tools/generate_api_skeleton.py` | **保留** | 任务书允许「明确保留供后续迭代复用」。契约变更或迭代二补齐接口时可**可重复再生成**骨架，是维持 83/83 一致性的关键工具；仅 40 KB |
| `tools/__pycache__/` | **已删除** | Python 运行缓存，属临时文件，不进入交付包 |
| 其他临时中间文件（`/tmp` 下的接口文档转储、日志、调试脚本） | **已删除** | 非交付物 |
| Docker 验证栈（`jc2001-app` + `jc2001-postgres`） | **保留运行** | 经交付负责人确认保留，便于评审随时访问 Swagger UI 复查（`http://localhost:8080/swagger-ui.html`）。交付物已打包，容器不进入归档 |
| 无关容器 `campushub-poc-postgres` | **未触碰** | 属其他项目，不在本次交付范围 |

> **注意**：`Dockerfile.build` 与 `.mvnrepo/` 的关系 —— `.mvnrepo/` 是本地 Maven 缓存，**已排除**在交付包外；
> 若使用 `Dockerfile.build` 在容器内构建，依赖会在容器内重新下载。

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 交付负责人 | 首版：交付物清单（8 类）、验收状态总览（7 步全通过）、接口冻结声明、假设与待确认问题（P0 8 项 + P1/P2 汇总）、环境要求、启动与验证步骤、已知限制 12 项、交付包结构 | **冻结候选（FROZEN-CANDIDATE）** |
