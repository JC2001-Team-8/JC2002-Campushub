# 迭代一 → 迭代二 交接文档

### 校园/社区闲置交易平台 · 交付交接说明

| 项目 | 内容 |
| --- | --- |
| 项目名称 | 校园/社区闲置交易平台（Campus / Community Idle-Goods Trading Platform） |
| 文档名称 | 迭代一 → 迭代二 交接文档 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 编写人 | 迭代一交付负责人（待填写姓名） |
| 读者 | **迭代二组员（假设未参与迭代一）** |
| 状态 | 草稿（待确认） |
| 技术栈 | Java 11 + Spring Boot 2.7.18 + PostgreSQL 15 |
| 契约版本 | OpenAPI 3.0.3，`1.0.0-frozen-candidate`（83 操作，已冻结） |

> **本文档自包含**。若你刚接手本项目且从未看过迭代一文档，**只读这一份即可开始工作**。
> 需要细节时，按 §7 的「关键文档索引」顺序深入。
>
> **交付形态**：正式交付物是 **`ITERATION1_TO_ITERATION2_HANDOVER.pdf`**（14 页，约 1.1 MB，可直接阅读）。
> 本 Markdown 文件是**该 PDF 的源文件**，保留以便后续维护。两者内容一致；
> 若需要重新生成 PDF：
> `python3 tools/md2pdf.py ITERATION1_TO_ITERATION2_HANDOVER.md ITERATION1_TO_ITERATION2_HANDOVER.pdf "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"`

---

## 1. 项目概述与迭代一范围

### 1.1 项目在做什么

平台面向**高校校区及周边社区**的师生与居民，解决「闲置物品在熟人半径内难以低成本、
低信任门槛流转」的问题：卖家把闲置挂出，按**预设交易区域 + 公共取货点**完成线下当面交付；
买家可**直接购买**，也可用自己的闲置**以物换物**。

平台**不读定位、不填住址、不做地图、不做物流**，用「固定取货点 + 交换码 / 交付确认」
替代复杂的地理与信任基础设施。

### 1.2 迭代一做了什么（一句话）

> 迭代一把「**要做什么**」和「**做成什么样**」彻底定下来，并交付了一个
> **可直接开始编码的工程与接口骨架** —— 但**业务逻辑一行都没实现**。

### 1.3 迭代一的交付边界

| 类别 | 交付了 | 没交付 |
| --- | --- | --- |
| 需求 | 需求说明书（54 条 FR）、不做清单（18 项）、权限矩阵、状态机、规则、页面流程、术语与假设 | 用户调研、竞品分析 |
| 数据库 | ER 图、字段字典（25 表 / 324 列）、迁移脚本 V1~V4（已在 PG 15 实测） | 业务查询、索引调优 |
| 接口 | OpenAPI 3.0.3 契约（**83 操作 / 99 schema / 93 错误码**）+ 6 份契约文档（**已冻结**） | 接口的业务实现 |
| 工程 | 可编译、可启动的 Spring Boot 骨架；Docker Compose 一键起栈 | 生产级部署方案 |
| 接口骨架 | 12 个 Controller 覆盖 83 个 operationId；99 个 DTO；12 个 Service **空实现** | **业务逻辑、Repository、Entity** |
| 设计 | 系统设计文档、9 个时序图、4 份验证记录 | 前端实现、可点击原型 |
| 交付材料 | DELIVERY.md、FREEZE.md、进度更新 1、验收报告、交付包 | — |

### 1.4 迭代一最关键的三条结论

1. **接口契约已冻结**：2026-10-09 之后**不允许新增接口**。83 个 operationId 是后续所有工作的基准。
2. **业务逻辑完全未实现**：83 个 Controller 方法与 83 个 Service 方法**统一抛
   `UnsupportedOperationException`**。调用业务接口会返回 500（需鉴权的接口返回 401/403）。
3. **数据库结构已定型**：25 张业务表由 Flyway `V1~V4` 管理，**V1~V4 不可修改**，
   表结构变更必须新增 `V5+`。

---

## 2. 文件结构总览

### 2.1 项目根目录

```
jc2001.backend/
├── README-backend.md              ← 【先读】后端启动、验证、常见问题
├── README-db.md                   ← 数据库设计与启动说明
├── pom.xml                        ← Maven 构建（Spring Boot 2.7.18 / Java 11）
├── Dockerfile                     ← 默认【运行镜像】：只复制 jar，不在容器内构建
├── Dockerfile.build               ← 可选【构建镜像】：容器内完整 Maven 构建（CI 用）
├── docker-compose.yml             ← 一键起 postgres + app（含健康检查）
├── .dockerignore                  ← 构建上下文排除规则
├── ITERATION1_ACCEPTANCE_REPORT.md ← 迭代一验收报告（本任务 A 产出）
├── ITERATION1_TO_ITERATION2_HANDOVER.pdf ← 本文档的 PDF 版（本任务 B 产出）
├── iteration1-delivery.zip        ← 迭代一交付包（712 KB / 205 文件）
├── docs/                          ← 全部文档（见 §2.2）
├── src/                           ← 源码（见 §2.3）
├── scripts/
│   └── package-iteration1.sh      ← 交付打包脚本
└── tools/
    ├── generate_api_skeleton.py   ← 【重要】契约→骨架生成器（可重复再生成）
    └── md2pdf.py                  ← Markdown→PDF 生成器（本次交接文档所用）
```

### 2.2 文档目录 `docs/iteration1/`

```
docs/iteration1/
├── README.md                              ← 迭代一文档索引与建议阅读顺序
├── DELIVERY.md                            ← 【必读】交付说明：清单/验收/环境/启动/已知限制
├── FREEZE.md                              ← 【必读】交付冻结声明 + 全部文件 SHA-256
├── progress-update-1.md                   ← 进度更新 1 材料（Summary/Challenges/Next Steps）
│
├── requirements.md                        ← 需求说明书（54 条 FR）
├── out-of-scope.md                        ← 不做清单（18 项，含 4 项核心）
├── permission-matrix.md                   ← 权限矩阵（7 主体 × 6 域）
├── state-machines.md                      ← 状态机（70 个转换编号）
├── rules.md                               ← 业务规则（34 条，R-01~R-35）
├── pages-and-flows.md                     ← 页面与流程（45 条页面 + 4 条流程）
├── glossary-assumptions-questions.md      ← 术语 + 20 条假设 + 34 条待确认问题
│
└── design/
    ├── openapi.yaml                       ← ★★ 接口契约【唯一真源】【已冻结】
    ├── api-contract.md                    ← 83 个接口清单 + FR 覆盖矩阵（54/54）
    ├── api-permission-mapping.md          ← 各接口权限说明（逐条 endpoint）
    ├── error-codes.md                     ← 93 个错误码 + E-01~E-17 错误场景
    ├── idempotency-and-concurrency.md     ← 幂等与并发（16 必填 + 16 可选幂等接口）
    ├── api-freeze.md                      ← 冻结声明与变更流程（10/9 后不新增）
    ├── api-open-questions.md              ← 18 条契约待确认（AQ）+ 20 项契约决策
    ├── er-diagram.md                      ← 关系图（4 个 Mermaid ER 图）★ 设计文档要求
    ├── schema.md                          ← 字段字典（25 表 / 324 列）
    ├── open-questions.md                  ← 数据库待确认（DQ）
    ├── SystemDesign.md                    ← 【必读】统一系统设计文档（10 章 / 10 图）
    ├── sequence-diagrams.md               ← 关键流程时序图（9 个图 / 7 组流程）
    │
    ├── db-verification.md                 ← 验证记录：数据库迁移
    ├── backend-skeleton-verification.md   ← 验证记录：工程骨架
    ├── controller-skeleton-verification.md ← 验证记录：接口骨架（83/83 对账）
    ├── design-doc-verification.md         ← 验证记录：设计文档
    ├── independent-verification.md        ← 独立验证报告（41/41 主张确认）
    ├── final-delivery-report.md           ← 迭代一最终交付报告
    └── README.md                          ← 设计目录索引
```

### 2.3 源码目录 `src/`

```
src/main/java/com/jc2001/campustrade/
├── CampusTradeApplication.java     ← 启动类
├── common/                         ← 统一响应与异常【已实现，迭代二直接用】
│   ├── ApiResponse.java            ← 成功响应包络 code/message/data/traceId/timestamp
│   ├── ErrorResponse.java          ← 失败响应包络
│   ├── PageResult.java             ← 分页结果
│   ├── ErrorCode.java              ← 错误码枚举【⚠️ 目前仅 7 个通用码，需补 93 个】
│   ├── GlobalExceptionHandler.java ← 全局异常处理
│   ├── ApiErrorController.java     ← 容器级错误改写
│   └── TraceIdFilter.java          ← traceId 生成与透传
├── config/                         ← 配置【已实现】
│   ├── SecurityConfig.java         ← ⚠️ 目前 anyRequest().permitAll()，迭代二需收紧
│   ├── OpenApiConfig.java          ← 运行时 OpenAPI 文档
│   ├── JacksonConfig.java          ← JSON 序列化（ISO 8601 UTC 等）
│   └── AppProperties.java          ← app.* 自定义配置
├── security/                       ← JWT 认证骨架【部分实现】
│   ├── JwtTokenProvider.java       ← HS256，7 天有效期
│   ├── JwtAuthenticationFilter.java ← ⚠️ 令牌无效时静默放行为匿名
│   ├── AuthenticatedUser.java      ← UserDetails 实现
│   └── UserDetailsServiceImpl.java ← ⚠️ 恒抛 UsernameNotFoundException（无 Repository）
├── controller/                     ← 14 个 Controller【骨架就位，方法体抛异常】
│   ├── AuthController.java         ← 认证与账户（6 个 operationId）★ 迭代二
│   ├── UserController.java         ← 个人资料（5 个）★ 迭代二
│   ├── DictionaryController.java   ← 字典与区域（4 个）★ 迭代二
│   ├── ItemController.java         ← 商品（10 个，含图片上传）★ 迭代二
│   ├── ReportController.java       ← 举报（3 个）★ 迭代二
│   ├── CartController.java         ← 购物车（4 个）
│   ├── OrderController.java        ← 订单与付款（8 个）
│   ├── ReviewController.java       ← 评价（3 个）
│   ├── SwapController.java         ← 交换包 + 交换提议（16 个）
│   ├── AdminController.java        ← 管理员治理（17 个）
│   ├── NotificationController.java ← 通知（3 个）
│   ├── InternalController.java     ← 系统定时任务（4 个，内部）
│   ├── HealthController.java       ← 【手工维护】/api/v1/health
│   └── FrozenSpecController.java   ← 【手工维护】/openapi/openapi.yaml
├── dto/
│   ├── request/   （33 个 Request DTO）
│   └── response/  （66 个 Response DTO，含 22 个枚举）
├── service/       （12 个 Service 接口）
├── service/impl/  （12 个空实现，全部抛 UnsupportedOperationException）
├── repository/    ← 【占位包，需迭代二创建】
└── entity/        ← 【占位包，需迭代二创建】

src/main/resources/
├── application.yml                 ← 主配置（端口/数据源/Flyway/springdoc/app.*）
├── application-dev.yml             ← dev profile（DEBUG 日志、SQL 打印）
└── db/migration/
    ├── V1__init_schema.sql         ← 25 张业务表【不可修改】
    ├── V2__seed_reference_data.sql ← 参照数据（7 区域/13 取货点/6 分类/5 成色）【不可修改】
    ├── V3__seed_development_data.sql ← 开发种子数据【不可修改，禁止用于生产】
    └── V4__fix_review_window_index.sql ← DQ-27 修复【不可修改】
```

### 2.4 各目录职责速查

| 目录 / 文件 | 作用 | 迭代二是否要动 |
| --- | --- | --- |
| `docs/iteration1/` | 需求基线（只读参考） | ❌ 不改（除登记新问题） |
| `docs/iteration1/design/openapi.yaml` | **接口契约唯一真源** | ❌ 不改（已冻结） |
| `src/main/resources/db/migration/V1~V4` | 数据库结构 | ❌ **不改**；新增表用 `V5+` |
| `src/main/java/.../entity/` | JPA 实体（占位） | ✅ **要创建** |
| `src/main/java/.../repository/` | JPA 仓储（占位） | ✅ **要创建** |
| `src/main/java/.../service/impl/` | 业务逻辑（空实现） | ✅ **要填充** |
| `src/main/java/.../controller/` | 接口层（已就位） | ⚠️ 基本不改（除非修 bug） |
| `src/main/java/.../dto/` | 传输对象（已就位） | ⚠️ 不改（契约已冻结） |
| `src/main/java/.../common/ErrorCode.java` | 错误码枚举 | ✅ **要补 93 个码** |
| `src/main/java/.../config/SecurityConfig.java` | 权限配置 | ✅ **要收紧** |
| `tools/generate_api_skeleton.py` | 契约→骨架生成器 | 🔧 契约变更时才用 |
| `scripts/package-iteration1.sh` | 交付打包 | 🔧 归档时用 |

---

## 3. 目前实现了什么

### 3.1 数据库（**已实测**）

| 项 | 数值 | 验证方式 |
| --- | --- | --- |
| 业务表 | **25 张** | `information_schema.tables`（实库查询） |
| 列总数 | **324 列** | `information_schema.columns` |
| 枚举类型 | 26 个（含 21 个业务枚举） | `pg_enum` |
| 迁移脚本 | **V1~V4，4 条全部成功** | `flyway_schema_history.success = t` |
| 参照 / 种子数据 | 7 交易区域 / 13 取货点 / 6 分类 / 5 成色；4 用户 / 9 商品 / 4 订单 / 2 交换 / 2 处罚 / 1 申诉 / 5 通知 | V2 / V3 已执行 |
| 部分唯一索引 | **7 个** | `pg_indexes WHERE indexdef ILIKE '%UNIQUE%WHERE%'` |

**25 张表清单**：

```
users                  user_trade_zones        trade_zones          pickup_points
categories             item_conditions         items                item_images
item_occupations       cart_items              orders               order_items
payments               swap_bundles            swap_bundle_items    swap_proposals
swap_proposal_items    swap_codes              reviews              reports
penalties              appeals                 notifications        idempotency_records
audit_logs
```

**核心设计要点（迭代二必须理解）**：

- `items.status` 是**占用的投影**，不是真值来源。真值是 `item_occupations` 台账。
- `item_occupations` 上有一个**部分唯一索引** `uq_item_occ_active_item`
  （`ON item_occupations(item_id) WHERE released_at IS NULL`）：
  **同一商品同时最多一条未释放占用** —— 由数据库强制，不依赖应用层约定。
- 所有表都有 `created_at / updated_at / created_by / updated_by`；
  12 张表有 `version` 列（乐观锁）。
- **处罚、申诉、订单、交换记录永不物理删除**，外键一律 `ON DELETE RESTRICT`。

### 3.2 接口契约（**已冻结**）

| 项 | 数值 |
| --- | --- |
| 规范版本 | OpenAPI 3.0.3 |
| paths | **73** |
| **operations** | **83**（operationId 唯一） |
| schemas | **99** |
| tags（模块） | 13 |
| 错误码 | **93** 个 `DOMAIN_REASON` |
| FR 覆盖 | **54 / 54 = 100%** |
| 基础路径 | `/api/v1` |
| 认证 | `Bearer JWT`（`bearerAuth`）；内部接口 `X-Internal-Token`（`internalToken`） |
| 版本 | `1.0.0-frozen-candidate` |

**统一约定**：

| 项 | 约定 |
| --- | --- |
| 成功响应 | `ApiResponse`：`code`（固定 `OK`）/ `message` / `data` / `traceId` / `timestamp` |
| 失败响应 | `ErrorResponse`：`code`（`DOMAIN_REASON`）/ `message` / `details` / `traceId` / `timestamp` |
| 分页 | 请求 `page`（**0 起**）/ `size`（1–100，默认 20）/ `sort`（白名单）；响应 `PageResult` |
| 幂等 | `Idempotency-Key` 请求头：**16 个必填 + 16 个可选** |
| 时间 | ISO 8601 UTC；倒计时由服务端计算并返回 `expiresAt` + `remainingSeconds` |
| 金额 | `DecimalString`（十进制字符串，2 位小数，单位元）—— **不用 number，避免浮点误差** |
| 越权 | 返回 **403**，不用 404 掩盖（E-02） |
| 追踪 | 响应头 `X-Trace-Id` 与响应体 `traceId` 一致 |

### 3.3 后端骨架

| 项 | 数值 | 状态 |
| --- | --- | --- |
| Controller（契约生成） | **12 个**，覆盖 **83 个 operationId** | ✅ 与契约 **1:1**（缺失 0 / 多余 0） |
| Controller（手工维护） | 2 个（Health / FrozenSpec） | ✅ 已 `@Hidden` 排除出文档 |
| Request DTO | **33 个** | ✅ 含 Bean Validation 注解 |
| Response DTO | **66 个**（含 **22 个枚举**） | ✅ 与 99 个 schema 全覆盖 |
| Service 接口 / 实现 | **12 / 12 个** | ⚠️ 实现**全部抛异常** |
| Java 源文件总数 | 158 | ✅ 编译通过（`mvn clean package` BUILD SUCCESS） |
| 权限注解 | 10 `permitAll` / 55 `isAuthenticated` / 14 `hasRole('ADMIN')` / 4 `hasRole('SYSTEM')` | ✅ 与契约一致 |
| 幂等参数 | 16 必填 + 16 可选 | ✅ 与契约一致 |

### 3.4 设计文档

| 文件 | 内容 |
| --- | --- |
| `SystemDesign.md` | 10 章 / **10 个 Mermaid 图**：架构、部署拓扑、领域模型、状态机、规则、权限、接口清单、不做清单、假设 |
| `sequence-diagrams.md` | **9 个时序图 / 7 组流程**：购买主线、交换主线、整笔作废、治理主线、并发抢购、超时释放、定时任务 |
| `er-diagram.md` | 4 个关系图（总览 / 占用 / 交换 / 治理） |

### 3.5 部署与运行

| 项 | 状态 |
| --- | --- |
| Docker Compose 一键起栈 | ✅ 实测 `jc2001-app` 与 `jc2001-postgres` 均 **Up (healthy)** |
| Flyway 迁移 | ✅ 启动时自动执行 V1~V4 |
| Swagger UI | ✅ `/swagger-ui.html` → 302 → 可访问 |
| 运行时接口文档 | ✅ `/v3/api-docs` 实测 **83 operations** |
| 冻结契约原文 | ✅ `/openapi/openapi.yaml`（HTTP 200，264,148 字节） |
| 健康检查 | ✅ `/actuator/health` 200；`/api/v1/health` 200 |

---

## 4. 如何启动与验证

### 4.1 环境要求

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| **JDK** | **11**（必需，不支持 8 / 17+） | 编译与运行 |
| **Maven** | 3.6+ | 构建（**非必需**：可用 `Dockerfile.build` 在容器内构建） |
| **Docker** | 20.10+ | 容器化运行（推荐） |
| **Docker Compose** | v2（`docker compose`） | 一键起栈 |
| PostgreSQL | **15** | 可由 Compose 提供，**无需本机安装** |

> 应用本身**不需要**本机安装 Maven 或 PostgreSQL —— `docker compose up` 即可起完整栈。

### 4.2 启动方式一：Docker Compose（推荐）

```bash
# ① 打包（首次必须；容器镜像只复制产物，不在容器内构建）
docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-11 mvn -B -DskipTests clean package
#   若本机已装 Maven：直接
#   mvn clean package -DskipTests

# ② 起栈（默认 app 8080 / postgres 5432）
DOCKER_BUILDKIT=0 docker compose -p jc2001 up -d --build

# ③ 若宿主机 5432 已被占用（本项目搭建时即遇到），改用：
POSTGRES_HOST_PORT=55432 APP_HOST_PORT=8080 DOCKER_BUILDKIT=0 docker compose -p jc2001 up -d --build

# ④ 等待健康
docker compose -p jc2001 ps      # 期望 app 与 postgres 均 (healthy)
```

> **关于 `DOCKER_BUILDKIT=0`**：本环境 `docker buildx` 存在沙箱限制，
> 需用传统 builder。若你的环境正常，可省略该变量。

### 4.3 启动方式二：本机运行

```bash
mvn clean package -DskipTests
java -jar target/campus-trade.jar --spring.profiles.active=dev
# 或开发时热重载：
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

需要本机或可访问的 PostgreSQL 15，默认连接 `jdbc:postgresql://localhost:5432/campus_trade`
（可用 `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` 覆盖）。

### 4.4 验证清单（6 项，应全部通过）

```bash
# ① Spring Boot 标准健康检查
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

# ⑥ 冻结契约原文
curl -s -o /dev/null -w "%{http_code} %{size_download}\n" http://localhost:8080/openapi/openapi.yaml
# 期望 200 264148
```

### 4.5 数据库连接与验证

| 项 | 值 |
| --- | --- |
| 数据库名 | `campus_trade` |
| 用户名 / 密码 | `campus_trade` / `campus_trade`（**开发默认值**，生产必须改） |
| 容器内地址 | `postgres:5432`（服务名解析） |
| 宿主机端口 | **5432**（默认）；本项目验证环境因端口被占用用 `55432` |
| 数据卷 | `jc2001_pgdata` |

```bash
# 验证 Flyway 迁移记录（应为 4 条且全部成功）
docker exec jc2001-postgres psql -U campus_trade -d campus_trade \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# 结构一致性校验（25 项只读检查）
docker exec -i jc2001-postgres psql -U campus_trade -d campus_trade < db/verify_schema.sql

# 直接进入数据库
docker exec -it jc2001-postgres psql -U campus_trade -d campus_trade
```

> ⚠️ **`POSTGRES_*` 环境变量仅在数据卷为空时生效**（容器首次初始化）。
> 若数据卷已存在，改密码需按 `README-backend.md` §6.4 处理。

---

## 5. 迭代二目标与交接要点

### 5.1 迭代二目标

> **账户、商品与基础检索** —— 让平台**第一次真正跑起来**：
> 能注册登录、能带图发布闲置、别人能搜到并看清细节。

具体包括：

| # | 功能点 | 涉及 operationId |
| --- | --- | --- |
| 1 | 注册 / 登录 / 退出 / 登录态保持 | `registerUser`、`login`、`logout`、`getCurrentUser`、`refreshToken`（预留 501） |
| 2 | 个人主页：改昵称、换头像、勾选常用交易区域 | `updateMyProfile`、`setMyTradeZones`（`UserTradeZonesRequest`）、`getPublicProfile` |
| 3 | 发布物品（标题、描述、分类、成色、心理估价、可售数量、最多 9 张图） | `createItem`、`uploadImage` |
| 4 | 我的物品：修改 / 下架 / 重新上架 | `listMyItems`、`updateItem`、`takeItemOffShelf`、`relistItem` |
| 5 | 访客按分类浏览、关键词搜索、限定区域 | `listItems`、`listCategories`、`listItemConditions`、`listTradeZones` |
| 6 | 详情页：多图轮播、卖家头像、区域、剩余数量、公开交换状态 | `getItemDetail` |
| 7 | 举报违规商品 | `reportItem`、`listMyReports`、`getMyReport` |
| 8 | 权限红线：除发布者外任何人不能修改/下架；上传图片必须登录 | `@PreAuthorize` + Service 层归属校验 |

### 5.2 迭代一已预留但未实现的部分

以下接口**已在 `openapi.yaml` 中定义并冻结**，Controller 方法**已就位**，
但 **Service 实现是空的**（抛 `UnsupportedOperationException`）。迭代二只需填充 Service。

**账户与认证（6 个）**

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `registerUser` | POST | `/api/v1/auth/register` |
| `login` | POST | `/api/v1/auth/login` |
| `refreshToken` | POST | `/api/v1/auth/refresh`（**约定返回 501**，AQ-02） |
| `logout` | POST | `/api/v1/auth/logout` |
| `getCurrentUser` | GET | `/api/v1/auth/me` |
| `changePassword` | PUT | `/api/v1/auth/password` |

**个人资料（5 个）**

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `updateMyProfile` | PUT | `/api/v1/users/me/profile` |
| `setMyTradeZones` | PUT | `/api/v1/users/me/trade-zones` |
| `getMyNotificationSettings` | GET | `/api/v1/users/me/notification-settings` |
| `updateMyNotificationSettings` | PUT | `/api/v1/users/me/notification-settings` |
| `getPublicProfile` | GET | `/api/v1/users/{userId}/public-profile` |

**字典与区域（4 个，只读）**

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `listTradeZones` | GET | `/api/v1/trade-zones` |
| `listPickupPointsByZone` | GET | `/api/v1/trade-zones/{tradeZoneId}/pickup-points` |
| `listCategories` | GET | `/api/v1/categories` |
| `listItemConditions` | GET | `/api/v1/item-conditions` |

**商品（10 个）**

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `listItems` | GET | `/api/v1/items` |
| `createItem` | POST | `/api/v1/items` |
| `listMyItems` | GET | `/api/v1/items/mine` |
| `getItemDetail` | GET | `/api/v1/items/{itemId}` |
| `updateItem` | PUT | `/api/v1/items/{itemId}` |
| `takeItemOffShelf` | POST | `/api/v1/items/{itemId}/off-shelf` |
| `relistItem` | POST | `/api/v1/items/{itemId}/relist` |
| `updateItemSwapPreference` | PUT | `/api/v1/items/{itemId}/swap-preference` |
| `getItemOccupation` | GET | `/api/v1/items/{itemId}/occupation` |
| `uploadImage` | POST | `/api/v1/files/images` |

**举报（3 个）**

| operationId | 方法 | 路径 |
| --- | --- | --- |
| `reportItem` | POST | `/api/v1/items/{itemId}/reports` |
| `listMyReports` | GET | `/api/v1/users/me/reports` |
| `getMyReport` | GET | `/api/v1/reports/{reportId}` |

**合计 28 个 operationId**（占 83 个的 1/3），全部已冻结、Controller 已就位、Service 待实现。

**关键 DTO 字段（已冻结，直接使用）**

`CreateItemRequest`（`required` 标 `*`）：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `title` * | string | maxLength = 60 |
| `description` | string | maxLength = 1000 |
| `categoryId` * | int64 | 取自 `listCategories` |
| `conditionId` * | int64 | 取自 `listItemConditions` |
| `tradeZoneId` * | int64 | 取自用户已勾选区域；商品所属区域 |
| `estimatedPrice` * | DecimalString | 字符串金额，2 位小数（R-13 平台不定价） |
| `imageUrls` * | string[] | **minItems=1, maxItems=9** |
| `swapPreference` | enum | `ACCEPTING` / `NOT_ACCEPTING` |

`UserTradeZonesRequest`：`tradeZoneIds`（必填，**minItems=1, maxItems=5**）

`UpdateProfileRequest`：`nickname`（maxLength=20）、`avatarUrl`（maxLength=255）

`ItemImageUploadResponse`：`imageUrl` *、`contentType` *、`fileSize` *

**图片存储约束（迭代一已定义）**

| 项 | 约束 | 强制位置 |
| --- | --- | --- |
| 数量 | 每商品 **1–9 张** | 契约 `minItems=1, maxItems=9` |
| 单张大小 | **≤ 5 MB**（5242880 字节） | DB `ck_item_images_size` + Spring `max-file-size: 5MB` |
| 格式 | **JPG / PNG / WebP** | `FILE_TYPE_NOT_ALLOWED`（422） |
| 存储 | **服务器本地目录**，数据库只记路径 | `item_images.image_url`；`items` 不含二进制 |
| 排序 | `item_images.sort_order`（有唯一约束 `uq_item_images_sort`） | 表已就位 |
| 封面 | 约定 **`sort_order` 最小者为首图** | ⚠️ 无 `is_cover` 字段，见 §5.5 差异 #3 |

### 5.3 建议实现顺序

按依赖关系，建议这样推进（**不要跳步**）：

```
第 1 步：地基（无依赖，必须先做）
  1.1 创建 entity 包 —— 按 schema.md 把 25 张表映射为 JPA 实体
  1.2 创建 repository 包 —— Spring Data JPA 仓储
  1.3 补全 common/ErrorCode.java —— 按 error-codes.md 逐字补齐 93 个业务码
  1.4 收紧 config/SecurityConfig.java —— 按 api-permission-mapping.md 逐条放行
  1.5 实现 security/UserDetailsServiceImpl.java —— 接上 users 表
      （目前恒抛 UsernameNotFoundException，导致无法真正认证）

第 2 步：账户与字典（依赖第 1 步）
  2.1 AuthService：registerUser → login → getCurrentUser → logout → changePassword
  2.2 UserService：updateMyProfile → setMyTradeZones → getPublicProfile → 通知设置
  2.3 DictionaryService：4 个只读字典接口（数据已在 V2 种子中）

第 3 步：商品发布（依赖第 2 步的登录态）
  3.1 FileService：uploadImage（本地目录 + 大小/格式校验 + 返回 imageUrl）
  3.2 ItemService：createItem（含 imageUrls 落库到 item_images + sort_order）
  3.3 ItemService：listMyItems → updateItem → takeItemOffShelf → relistItem

第 4 步：检索与详情（依赖第 3 步有数据）
  4.1 ItemService：listItems（分类筛选 / 关键词搜索 / 区域限定 / 分页）
  4.2 ItemService：getItemDetail（多图 + 卖家公开信息 + 区域 + 交换状态）

第 5 步：举报（最后做）
  5.1 ReportService：reportItem → listMyReports → getMyReport
```

**每一步都要做的事**：

- 写 Service 实现，替换 `UnsupportedOperationException`
- 用 `error-codes.md` 的码返回业务错误（**不要自造码**）
- 遵守 `idempotency-and-concurrency.md` 的幂等与条件更新要求
- 归属校验失败返回 **403**（不是 404）
- 更新或新增验证记录

### 5.4 必须遵守的约束（**硬性红线**）

| # | 约束 | 说明 |
| --- | --- | --- |
| 1 | **不新增未登记的接口** | 2026-10-09 后禁止新增。确需变更走 `api-freeze.md` §4 流程（影响面评估 + 3 人审批 + 台账登记） |
| 2 | **沿用已冻结的 DTO 与错误码** | 不改 DTO 字段名/类型/必填性；错误码从 `error-codes.md` 里取，**禁止自造** |
| 3 | **`V1~V4` 不可修改** | Flyway 有 checksum 校验，改任何一个字符都会导致启动失败 |
| 4 | **新增表用 `V5+`** | 需要新表或改表结构时，新建 `V5__xxx.sql` |
| 5 | **`ddl-auto=validate`** | Hibernate **不得**改表结构；实体字段与表不一致会启动失败 |
| 6 | **禁止「先查后改」** | 状态转换用条件更新或乐观锁；受影响行数 0 → 409（R-18） |
| 7 | **占用与投影必须同一事务** | 写 `item_occupations` 与 `items.status` 必须在同一事务（DD-01） |
| 8 | **唯一索引冲突映射为 409** | SQLState `23505` → 对应域的错误码，不是 500 |
| 9 | **越权返回 403** | 不用 404 掩盖（E-02） |
| 10 | **治理动作必填原因且留痕** | `reason` 必填 + 写 `audit_logs`（R-34） |
| 11 | **管理员不参与交易** | `ROLE_ADMIN` 不得下单/发布/发起交换（P-00-A） |
| 12 | **金额用 `DecimalString`** | 字符串承载，不用浮点（R-13） |

### 5.5 迭代一至迭代二之间的**已知差异（需先确认）**

以下 3 项是**迭代二任务描述与迭代一已冻结设计之间的不一致**。
**迭代一未擅自修改**（契约已冻结），此处如实登记，**请先与导师/团队确认再动手**：

| # | 差异 | 迭代一（已冻结） | 迭代二描述 | 影响与建议 |
| --- | --- | --- | --- | --- |
| **1** | **交易区域勾选数量** | `requirements.md` FR-107 与契约 `UserTradeZonesRequest.tradeZoneIds` 均为 **1–5 个**（`minItems=1, maxItems=5`） | 要求 **1–3 个** | ⚠️ **收窄上限属契约"破坏性"变更**（`api-freeze.md` 禁止"收窄枚举/变更必填性"类变更）。**建议**：① 确认是否真的要从 5 收到 3；② 若确认，需走契约变更流程并同步改 `openapi.yaml`（但本迭代契约已冻结，改它属越界）；③ **最省事做法：接口仍接受 1–5，仅前端限制勾选 3 个**，不改契约 |
| **2** | **"可售数量"字段** | `items` 表**没有任何数量/库存字段**；`rules.md` Q-23 与假设 A-16 明确「闲置交易多为单件，**购物车数量固定为 1**」，DB 有 `CHECK (quantity = 1)` | 要求商品有 **"可售数量"**，详情页显示 **"剩余数量"** | ⚠️ **数据库层不支持多数量库存**（一物一件模型）。**建议**：① 确认"可售数量"是否指"同一商品可卖多件"；② 若是，属**模型级变更**，需新增 `V5+` 迁移加 `items.quantity`/`stock` 并改契约（破坏性）；③ 若只是"显示 1 件"，则无需改动，`getItemDetail` 直接返回固定 1 |
| **3** | **图片拖动排序 / 第一张封面** | `item_images` 有 `sort_order`（+ 唯一约束），但**契约没有暴露排序入参**（`CreateItemRequest.imageUrls` 只是字符串数组，按数组顺序落 `sort_order`）；**没有 `is_cover` 字段** | 要求"可拖动排序，第一张封面" | ⚠️ **部分支持**：数组顺序 = `sort_order`，"第一张"即 `sort_order` 最小者。**建议**：① 前端把拖动后的顺序按数组位置提交即可，**无需改契约**；② "封面"用约定（最小 `sort_order`）而非独立字段，若必须显式标记，需加 `V5+` 迁移 + 契约变更 |

> **另注**：迭代二描述中的「心理估价」对应契约的 `estimatedPrice`（`DecimalString`）；
> 「可售数量」在迭代一设计中**不存在**，是最需要澄清的一项。

### 5.6 待确认的 P0 问题（**建议开工前先问**）

| 编号 | 问题 | 当前默认（可按默认实现，但需标注） |
| --- | --- | --- |
| **Q-01** | 最终服务哪 2–3 个校区？ | 占位 `CAMPUS_A` / `CAMPUS_B` / `CAMPUS_C` |
| **Q-02** | 预设交易区域清单与编码规则？ | 占位 `CAMPUS_A`、`COMMUNITY_A1` 等 |
| **Q-03** | 公共取货点清单与命名？ | 占位 `PICKUP_LIBRARY_A` 等 13 个 |
| **Q-04** | 管理员账号来源？ | 种子创建 `admin@example.com`（占位密码） |
| **DQ-01** | 是否启用商品「草稿」态？ | **不启用**（发布即 `IN_SALE`）。若启用属破坏性变更 |
| **DQ-05** | 封禁能否延长 / 覆盖 / 调时长？ | **固定 72 小时且拒绝重复封禁** |
| **AQ-01** | 通知开关是否纳入？ | **纳入**（仅站内渠道） |
| **AQ-02** | `refreshToken` 是否启用？ | 保留路径，**默认返回 501** |
| **AQ-03** | 字典是否需在线维护写接口？ | **仅只读** |
| **AQ-04** | 同用户多条不同类型生效处罚是否允许？ | 由 `uq_penalties_one_active` 决定 |

> **建议**：**按契约默认值实现，并在代码注释/提交信息中标注 `【待确认 Q-xx】`**，
> 不要因为等确认而阻塞开发。

### 5.7 `refreshToken` 的特别说明

契约保留了 `POST /api/v1/auth/refresh`，但**约定暂不启用**（AQ-02 / Q-07）。
迭代二的"登录态保持"若只需 7 天有效期令牌（`expiresIn = 604800` 秒），
**可直接返回 501 `COMMON_NOT_IMPLEMENTED`**，等需求确认后再实现。

---

## 6. 提交 GitHub 注意事项

### 6.1 建议的 `.gitignore`

> 仓库当前**不是 git 仓库**（无 `.git`）。首次提交前请创建 `.gitignore`：

```gitignore
# ---- 构建产物 ----
target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar

# ---- 本地 Maven 缓存（本项目为加速构建而建的本地仓库）----
.mvnrepo/

# ---- IDE ----
.idea/
*.iml
*.ipr
*.iws
.vscode/
*.swp

# ---- 操作系统 ----
.DS_Store
Thumbs.db

# ---- 日志与运行时数据 ----
*.log
logs/
data/
uploads/

# ---- Python 缓存（tools/ 下有 Python 脚本）----
__pycache__/
*.pyc

# ---- 交付包（作为 Release 附件，不入库）----
iteration1-delivery.zip

# ---- 敏感配置（如有本地覆盖）----
application-local.yml
.env
```

### 6.2 需要提交的文件

```
✅ src/                        （全部源码骨架）
✅ pom.xml
✅ docker-compose.yml
✅ Dockerfile
✅ Dockerfile.build
✅ .dockerignore
✅ .gitignore                  （需新建）
✅ README-backend.md
✅ README-db.md
✅ docs/                       （全部文档，含 design/openapi.yaml）
✅ db/verify_schema.sql
✅ scripts/package-iteration1.sh
✅ tools/generate_api_skeleton.py   （可选，但建议保留）
✅ tools/md2pdf.py                  （可选）
✅ ITERATION1_ACCEPTANCE_REPORT.md
✅ ITERATION1_TO_ITERATION2_HANDOVER.pdf
```

### 6.3 **不需要**提交的文件

| 文件 | 原因 | 替代做法 |
| --- | --- | --- |
| `iteration1-delivery.zip`（712 KB） | 二进制归档，不宜入库 | **作为 GitHub Release 附件**上传 |
| `target/` | 构建产物，可重新生成 | 由 CI 或本地 `mvn package` 生成 |
| `.mvnrepo/` | 本地 Maven 缓存（约 105 MB） | 各人本地生成 |
| `data/`、`uploads/` | 运行时上传的文件与本地产物 | 运行时生成；生产应挂载卷 |
| 本地数据库数据卷 | 不在仓库内（Docker volume） | 由 Flyway 迁移重建 |
| `__pycache__/`、`.DS_Store`、`*.log` | 临时/系统文件 | 已列入 `.gitignore` |

### 6.4 分支建议

```bash
# 迭代二从 main 拉出功能分支
git checkout main
git pull
git checkout -b feature/iteration2-account-item

# 建议按 §5.3 的 5 个步骤分别提交，每次提交信息带上 operationId 或 FR 编号，例如：
#   feat(auth): implement registerUser / login (FR-101, FR-102)
#   feat(item): implement createItem with image upload (FR-201, FR-205)
#   feat(error): add 93 domain error codes from error-codes.md

# 完成后合并回 main
git checkout main
git merge --no-ff feature/iteration2-account-item
```

**提交前自检**：

```bash
# 确认没有把构建产物或缓存提交进去
git status --short
git ls-files | grep -E 'target/|\.mvnrepo/|\.DS_Store|__pycache__' && echo "❌ 有不该提交的文件" || echo "✅ 干净"
```

### 6.5 如果本机有 5432 端口占用

本项目的 `docker-compose.yml` 默认映射 `5432`。若你机器上 5432 已被占用（本项目搭建时就遇到了），
用环境变量覆盖，**不要改 `docker-compose.yml`**：

```bash
POSTGRES_HOST_PORT=55432 APP_HOST_PORT=8080 docker compose -p jc2001 up -d --build
```

---

## 7. 关键文档索引（先读这 5 份）

| 顺序 | 文件 | 为什么读它 | 预计时间 |
| --- | --- | --- | --- |
| **1** | `README-backend.md` | 先把环境跑起来，确认你能启动、能访问 Swagger UI | 15 分钟 |
| **2** | `docs/iteration1/design/openapi.yaml` | **接口契约唯一真源**。用 Swagger UI 打开看，或导入 Postman/Apifox。迭代二所有接口定义都在这里 | 30 分钟 |
| **3** | `docs/iteration1/design/SystemDesign.md` | 建立全局理解：架构、领域模型、**占用与释放模型**、状态机、规则、权限 | 40 分钟 |
| **4** | `docs/iteration1/design/api-contract.md` | 83 个接口的逐条清单 + FR 映射，配合 `error-codes.md` 一起看 | 30 分钟 |
| **5** | `docs/iteration1/design/er-diagram.md` + `schema.md` | 动手写 entity/repository 之前必须看懂表结构与关系 | 40 分钟 |

**之后按需查阅**：

- 要做权限 → `design/api-permission-mapping.md`（逐条 endpoint 权限）
- 要写错误处理 → `design/error-codes.md`（93 个码 + HTTP 状态语义）
- 要处理并发/幂等 → `design/idempotency-and-concurrency.md`
- 要理解业务规则 → `rules.md`（34 条）+ `state-machines.md`（70 个转换）
- 要确认能否改动 → `design/api-freeze.md`（冻结与变更流程）

---

## 8. 附录

### 8.1 常用命令速查

```bash
# ============ 构建 ============
mvn clean package -DskipTests                # 本机构建
docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-11 mvn -B -DskipTests clean package   # 无本机 Maven 时

# ============ 起栈 / 停栈 ============
DOCKER_BUILDKIT=0 docker compose -p jc2001 up -d --build     # 起
docker compose -p jc2001 ps                                   # 看状态
docker compose -p jc2001 logs -f app                          # 看应用日志
docker compose -p jc2001 stop                                 # 停（保留容器与数据）
docker compose -p jc2001 down                                 # 删容器（保留数据卷）
docker compose -p jc2001 down -v                              # 删容器 + 数据卷（重建库）

# ============ 验证（只读）============
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/api/v1/health/ready
curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool | head -40
open http://localhost:8080/swagger-ui.html                    # macOS

# ============ 数据库 ============
docker exec -it jc2001-postgres psql -U campus_trade -d campus_trade
docker exec jc2001-postgres psql -U campus_trade -d campus_trade \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
docker exec jc2001-postgres psql -U campus_trade -d campus_trade -c "\dt"
docker exec -i jc2001-postgres psql -U campus_trade -d campus_trade < db/verify_schema.sql

# ============ 打包交付 ============
bash scripts/package-iteration1.sh              # 生成 iteration1-delivery.zip
bash scripts/package-iteration1.sh /tmp/out      # 生成到指定目录

# ============ 契约→骨架再生成（仅在契约变更后使用）============
python3 tools/generate_api_skeleton.py
```

### 8.2 端口说明

| 服务 | 容器内端口 | 宿主机默认端口 | 覆盖变量 | 说明 |
| --- | --- | --- | --- | --- |
| 应用（Spring Boot） | 8080 | **8080** | `APP_HOST_PORT` | `/api/v1/**`、`/swagger-ui.html`、`/actuator/health` |
| PostgreSQL | 5432 | **5432** | `POSTGRES_HOST_PORT` | 验证环境曾用 `55432` 规避占用 |

> 应用容器**内部**始终用 `postgres:5432`（服务名解析），
> 宿主机端口覆盖不影响容器间通信。

### 8.3 占位数据说明（**重要**）

以下数据是**占位值**，等 Q-01~Q-04 确认后替换：

| 类别 | 占位值示例 | 数量 | 所在位置 |
| --- | --- | --- | --- |
| 校区 | `CAMPUS_A`、`CAMPUS_B`、`CAMPUS_C` | 3 | `V2__seed_reference_data.sql` → `trade_zones` |
| 社区 | `COMMUNITY_A1` 等 | 4 | 同上 |
| 公共取货点 | `PICKUP_LIBRARY_A` 等（6 种类型） | 13 | 同上 → `pickup_points` |
| 分类 | 教材书籍 / 电子数码 / 生活用品 / 服饰 / 运动器材 / 其他 | 6 | 同上 → `categories` |
| 成色 | 全新 / 几乎全新 / 轻微使用痕迹 / 明显使用痕迹 / 有瑕疵 | 5 | 同上 → `item_conditions` |
| 开发用户 | `admin@example.com` 等（占位密码） | 4 | `V3__seed_development_data.sql` |

**替换规则**：

- ✅ **可以改**：参照数据的具体值（名称、编码）
- ❌ **不可以改**：`V1~V4` 已应用的迁移文件（Flyway checksum 会失败）
- ✅ **正确做法**：新增 `V5__update_placeholder_zones.sql` 之类的迁移来更新数据，
  或在开发库重建时直接修改 `V2` 后重建数据库（仅限未发布环境）
- ⚠️ **`V3` 含占位账号与密码，生产环境禁止执行**

### 8.4 迭代二开工检查清单

开工前请逐项确认：

- [ ] 本机能 `docker compose up` 起栈，`/actuator/health` 返回 200
- [ ] Swagger UI 能打开，看到 12 个 tag 分组、83 个操作
- [ ] 用 `psql` 连上数据库，`\dt` 看到 25 张表
- [ ] `flyway_schema_history` 有 4 条记录且全部 `success = t`
- [ ] 已读完 §7 的 5 份文档
- [ ] 已与团队确认 §5.5 的 **3 项差异**（交易区域 1–3 vs 1–5、可售数量、图片排序）
- [ ] 已知晓 §5.6 的 P0 待确认问题，并决定"按默认实现 + 标注待确认"
- [ ] 已创建 `.gitignore` 并确认 `git status` 干净
- [ ] 已从 `main` 拉出 `feature/iteration2-account-item` 分支

### 8.5 遇到问题时的排查顺序

| 现象 | 排查方向 |
| --- | --- |
| 应用起不来，日志报 Flyway checksum | 有人改了 `V1~V4`。用 `git diff` 检查；恢复原文件 |
| 应用起不来，日志报 schema validation failed | 实体字段与表不一致（`ddl-auto=validate`）。对照 `schema.md` |
| 5432 端口被占用 | 用 `POSTGRES_HOST_PORT=55432` 覆盖，**不要改 compose 文件** |
| 调用业务接口返回 500 | **预期行为**：Service 未实现，抛 `UnsupportedOperationException` |
| 调用需登录接口返回 401/403 | 需带 `Authorization: Bearer <token>`；令牌无效会被静默当匿名 |
| `/v3/api-docs` 操作数不是 83 | 有人改了 Controller 或契约。对照 `controller-skeleton-verification.md` |
| `docker build` 报 buildx 错误 | 加 `DOCKER_BUILDKIT=0` 用传统 builder |
| 容器一直 unhealthy | `docker compose logs app` 看具体错误；常见是数据库未就绪或密码不符 |

---

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 迭代一交付负责人 | 首版：项目概述与迭代一范围、文件结构总览（含逐目录职责表）、已实现内容（数据库/契约/骨架/设计/部署）、启动与验证步骤、迭代二目标与 28 个待实现 operationId、建议实现顺序、12 条硬性约束、**3 项已冻结设计与迭代二描述的差异**、P0 待确认问题、GitHub 提交注意事项（.gitignore/分支/提交内容）、关键文档索引、附录（命令速查/端口/占位数据/开工检查清单/排错表） | 草稿（待确认） |
