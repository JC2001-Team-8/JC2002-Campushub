# 迭代一 后端工程骨架 使用说明（README-backend）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 后端工程骨架 使用说明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 后端工程负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 技术栈 | Java 11 + Spring Boot 2.7.18 + PostgreSQL 15（固定） |
| 工程坐标 | `com.jc2001:campus-trade:1.0.0-SNAPSHOT` |
| 上游依据 | `docs/iteration1/design/api-freeze.md`（接口契约冻结候选） |

> **本步骤范围**：只搭**工程骨架** —— 可编译、可启动、可连 PostgreSQL 15、
> Flyway 自动执行 V1~V4、`/actuator/health` 返回 UP、Swagger UI 可访问。
> **不含**业务逻辑、**不含** 83 个业务 Controller、**不含**前端。

---

## 1. 环境要求

| 组件 | 版本 | 校验命令 | 说明 |
| --- | --- | --- | --- |
| JDK | **11** | `java -version` | 必须 11（Java 8 与 17 均不保证可用） |
| Maven | 3.6+ | `mvn -version` | 可选：无本地 Maven 时用 `./mvnw` 或容器构建（见 §3.3） |
| Docker | 20.10+ | `docker --version` | 用于 `docker compose` 方式启动 |
| Docker Compose | v2 | `docker compose version` | 使用 `docker compose`（非 `docker-compose`） |
| PostgreSQL | 15 | 由 compose 提供 | 无需本地安装 |

> **端口占用**：默认使用宿主机 `5432`（数据库）与 `8080`（应用）。
> 若被占用，见 §6.1 与 §6.2 的覆盖方式。

---

## 2. 目录结构

```text
.
├── pom.xml                      # Maven 构建描述（Java 11 / Spring Boot 2.7.18）
├── Dockerfile                   # 【默认】运行镜像：仅复制 target/*.jar，不做 Maven 构建（秒级）
├── Dockerfile.build             # 【可选】容器内完整构建（多阶段 + 依赖层缓存），供 CI 使用
├── .dockerignore                # 排除 .mvnrepo/target 无关项，构建上下文 155MB → 50MB
├── docker-compose.yml           # PostgreSQL 15 + app
├── README-backend.md            # 本文件
├── README-db.md                 # 数据库说明（前序步骤产出）
├── db/verify_schema.sql         # 数据库验证脚本（只读可重复执行）
├── docs/iteration1/             # 需求基线 / 数据库设计 / 接口契约
│   └── design/openapi.yaml      # 已冻结契约（83 operationId），构建时打入 jar
├── tools/generate_api_skeleton.py  # 【一次性】由契约生成 Controller/DTO/Service 骨架
└── src/main/
    ├── java/com/jc2001/campustrade/
    │   ├── CampusTradeApplication.java     # 入口（@SpringBootApplication + @ConfigurationPropertiesScan）
    │   ├── common/                         # 统一响应与异常
    │   │   ├── ApiResponse.java            # code/message/data/traceId/timestamp
    │   │   ├── PageResult.java             # page/size/total/items（+ 契约别名）
    │   │   ├── ErrorResponse.java          # code/message/details/traceId/timestamp
    │   │   ├── ErrorCode.java              # 7 个通用错误码（其余下一步补齐）
    │   │   ├── GlobalExceptionHandler.java # @RestControllerAdvice 全局异常
    │   │   ├── ApiErrorController.java     # /error 出口（统一容器级错误格式）
    │   │   └── TraceIdFilter.java          # X-Trace-Id 贯穿响应体/响应头/MDC
    │   ├── config/
    │   │   ├── SecurityConfig.java         # ⚠️ 骨架放开模式，含收紧 TODO
    │   │   ├── OpenApiConfig.java          # 运行时文档元信息 + 两个安全方案
    │   │   ├── JacksonConfig.java          # ISO 8601 UTC、non_null
    │   │   └── AppProperties.java          # app.* 强类型绑定（占位配置集中处）
    │   ├── security/
    │   │   ├── JwtTokenProvider.java       # 生成/解析 JWT（HS256，7 天）
    │   │   ├── JwtAuthenticationFilter.java# Bearer 解析骨架
    │   │   ├── AuthenticatedUser.java      # UserDetails 骨架
    │   │   └── UserDetailsServiceImpl.java # ⚠️ 骨架：未接入用户仓储
    │   ├── controller/                     # 12 个契约 Controller（83 operationId）+ 2 个手工自检
    │   │   ├── AuthController.java          # 01-认证与账户（6）
    │   │   ├── UserController.java          # 02-个人资料（5）
    │   │   ├── DictionaryController.java    # 03-字典与区域（4）
    │   │   ├── ItemController.java          # 04-商品（10）
    │   │   ├── CartController.java          # 05-购物车（4）
    │   │   ├── OrderController.java         # 06-订单与付款（8）
    │   │   ├── ReviewController.java        # 07-评价（3）
    │   │   ├── SwapController.java          # 08-交换包 + 09-交换提议（16）
    │   │   ├── ReportController.java        # 10-举报（3）
    │   │   ├── AdminController.java         # 11-管理员治理（17）
    │   │   ├── NotificationController.java  # 12-通知（3）
    │   │   ├── InternalController.java      # 13-系统定时任务（4，内部）
    │   │   ├── HealthController.java        # 【手工】/api/v1/health、/health/ready
    │   │   └── FrozenSpecController.java    # 【手工】/openapi/openapi.yaml（冻结契约）
    │   ├── service/        # 12 个 Service 接口（方法签名同 Controller）
    │   ├── service/impl/   # 12 个空实现（全部抛 UnsupportedOperationException）
    │   ├── repository/     # 占位包（下一步按 schema.md 生成）
    │   ├── entity/         # 占位包
    │   └── dto/
    │       ├── request/    # 33 个请求 DTO
    │       └── response/   # 66 个响应 DTO/枚举（含 22 个枚举）
    └── resources/
        ├── application.yml       # 主配置
        ├── application-dev.yml   # 开发环境（DEBUG 日志、Swagger 可试调用）
        └── db/migration/         # Flyway：V1~V4（⚠️ 不得修改已执行脚本）
```

---

## 3. 本地启动

### 3.1 方式一：Docker Compose（推荐，含数据库）

> **⚠️ 前置：必须先在宿主机打包**。默认的 `Dockerfile` 是**运行镜像**——
> 只把 `target/campus-trade.jar` 复制进镜像，**不在容器内执行 Maven**。
> 这样做是因为容器内全新构建需重新下载全部依赖，实测耗时约 **38 分钟**；
> 改为复制产物后，镜像构建实测 **9 秒**（详见 §7.4）。

```bash
# 0) 宿主机打包（本机已有 ~/.m2 缓存时通常几十秒）
mvn clean package -DskipTests

# 1) 构建并启动 postgres + app
docker compose up -d --build

# 2) 查看状态（等 app 变为 healthy）
docker compose ps

# 3) 查看应用日志（可见 Flyway 执行 V1~V4）
docker compose logs -f app
```

**无本地 Maven 时**先借助容器完成打包（依赖缓存到工作区 `.mvnrepo`，仅首次慢）：

```bash
docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 \
  -w /workspace maven:3.9-eclipse-temurin-11 \
  mvn -B -DskipTests clean package
```

**若希望「容器内一键构建全部」**（CI / 无本地 Java 环境），改用 `Dockerfile.build`：

```bash
docker build -f Dockerfile.build -t campus-trade:full .
# 或临时用一份指向该文件的最小 compose 覆盖
```

启动后：

- 应用：<http://localhost:8080>
- 健康检查：<http://localhost:8080/actuator/health>
- Swagger UI：<http://localhost:8080/swagger-ui.html>

### 3.2 方式二：本地 Maven 运行（数据库需已就绪）

```bash
# 1) 只启动数据库
docker compose up -d postgres

# 2) 本地运行应用（使用 dev profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或先打包再运行：

```bash
mvn clean package -DskipTests
java -jar target/campus-trade.jar --spring.profiles.active=dev
```

### 3.3 无本地 Maven 时的构建方式

`pom.xml` 未附带 Maven Wrapper（本环境无法联网拉取 wrapper 分发包）。
无本地 Maven 时用容器构建：

```bash
docker run --rm -v "$PWD":/workspace -w /workspace \
  maven:3.9-eclipse-temurin-11 \
  mvn -B -DskipTests clean package
```

> 首次构建需下载依赖，耗时较长（本环境实测约 7~8 分钟）。
> 如需复用依赖缓存，追加
> `-v "$PWD/.mvnrepo":/root/.m2`（该目录已加入 `.gitignore`）。

---

## 4. 验证步骤

```bash
# 1) 健康检查（Spring Boot 标准）
curl http://localhost:8080/actuator/health
# 期望：{"status":"UP", ...}

# 2) 业务侧健康检查（契约格式统一响应）
curl http://localhost:8080/api/v1/health
# 期望：{"code":"OK","message":"服务正常","data":"UP","traceId":"...","timestamp":"..."}

# 3) 就绪检查（含数据库 + Flyway）
curl http://localhost:8080/api/v1/health/ready
# 期望：data.database=UP，data.flyway.appliedCount=4，data.flyway.currentVersion=4

# 4) Swagger UI（浏览器打开）
open http://localhost:8080/swagger-ui.html

# 5) 冻结契约（Swagger UI 默认加载此地址）
curl -o /dev/null -w "%{http_code} %{size_download}\n" http://localhost:8080/openapi/openapi.yaml

# 6) 运行时就绪信息
curl http://localhost:8080/v3/api-docs
```

### 4.1 验证数据库与迁移

```bash
# 连接数据库
psql -h localhost -p 5432 -U campus_trade -d campus_trade
# 密码：campus_trade

# 或用容器内的 psql
docker exec -it jc2001-postgres psql -U campus_trade -d campus_trade
```

```sql
-- 迁移记录：应显示 V1~V4 且 success = t
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

-- 业务表数量：应为 25
SELECT count(*) FROM information_schema.tables
WHERE table_schema='public' AND table_name<>'flyway_schema_history';

-- 种子数据抽查
SELECT count(*) FROM users;        -- 4
SELECT count(*) FROM items;        -- 9
SELECT count(*) FROM trade_zones;  -- 7
```

### 4.1.1 验证 Swagger UI 显示全部 83 个操作

第五步已按契约生成 83 个 operationId 的接口骨架，Swagger UI **默认展示运行时文档**
（即代码生成的 83 个操作），并可切换到「迭代一契约（冻结候选）」原文对照。

```bash
# ① 统计运行时文档中的操作数量 —— 应为 83
curl -s http://localhost:8080/v3/api-docs   | python3 -c "import json,sys; d=json.load(sys.stdin); \
      print('operations =', sum(1 for p,i in d['paths'].items() \
        for m in i if m in ('get','post','put','patch','delete')))"
# 期望输出：operations = 83

# ② 打开 Swagger UI（浏览器）—— 左栏应列出 12 个 tag 分组、共 83 个操作
open http://localhost:8080/swagger-ui.html

# ③ 确认 Swagger UI 加载的文档入口
curl -s http://localhost:8080/v3/api-docs/swagger-config \
  | python3 -c "import json,sys; print([u['name'] for u in json.load(sys.stdin)['urls']])"
# 期望：['运行时文档（代码生成，83 个操作）', '迭代一契约（冻结候选 1.0.0-frozen-candidate）']

# ④ 冻结契约原文（供 Swagger UI 对照，字节级等同源文件）
curl -o /dev/null -w "%{http_code} %{size_download}\n" http://localhost:8080/openapi/openapi.yaml
# 期望：200 264148
```

**说明**：

- 两个自检端点（`/api/v1/health*`、`/openapi/openapi.yaml`）已用 `@Hidden` 从文档隐藏，
  因此文档操作数**恰好为 83**，与契约一致。
- 业务方法当前统一抛 `UnsupportedOperationException`，故在 Swagger UI 中「Try it out」
  会返回 500（这是**预期行为**，表示骨架已就位但业务未实现）。
- 需登录的接口会先经 `@PreAuthorize` 拦截，未携带令牌时返回 **403/401**（权限注解已生效）。
- 操作总数的完整对账见
  `docs/iteration1/design/controller-skeleton-verification.md` §2。

### 4.2 验证统一错误响应格式

```bash
# 未知路由 → 契约格式 404（而非 Whitelabel 结构）
curl http://localhost:8080/api/v1/not-exists
# 期望：{"code":"COMMON_RESOURCE_NOT_FOUND","message":"请求的接口不存在","traceId":"...","timestamp":"..."}

# 方法不支持 → 400
curl -X POST http://localhost:8080/api/v1/health
```

### 4.3 验证 traceId 贯穿

```bash
# 响应头与响应体中的 traceId 应一致；也可由调用方指定
curl -D - -o /dev/null -H 'X-Trace-Id: my-trace-0001' http://localhost:8080/api/v1/health | grep -i x-trace-id
curl -H 'X-Trace-Id: my-trace-0001' http://localhost:8080/api/v1/health
```

---

## 5. 配置说明

### 5.1 环境变量（生产必须覆盖的项）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/campus_trade` | 数据库连接串（compose 中覆盖为 `postgres:5432`） |
| `SPRING_DATASOURCE_USERNAME` | `campus_trade` | 数据库用户 |
| `SPRING_DATASOURCE_PASSWORD` | `campus_trade` | 数据库密码 |
| `SPRING_PROFILES_ACTIVE` | （空） | 建议 `dev` |
| `SERVER_PORT` | `8080` | 应用端口 |
| `FLYWAY_ENABLED` | `true` | 是否自动执行迁移 |
| `APP_JWT_SECRET` | ⚠️ 开发占位串 | **生产必须注入 ≥32 字节随机密钥**（HS256 要求） |
| `APP_JWT_EXPIRATION_SECONDS` | `604800` | 令牌有效期 7 天（假设 A-06） |
| `APP_INTERNAL_TOKEN` | ⚠️ 开发占位串 | 内部接口凭证（`/internal/**`，AD-18） |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | 前端联调来源 |
| `APP_STORAGE_LOCAL_DIR` | `./data/uploads` | 图片本地存储目录（OOS-18） |
| `POSTGRES_HOST_PORT` | `5432` | compose 中 postgres 的宿主机端口 |
| `APP_HOST_PORT` | `8080` | compose 中 app 的宿主机端口 |
| `RUNNER_IMAGE` | `maven:3.9-eclipse-temurin-11` | Dockerfile 运行阶段基础镜像（见 §6.5） |

### 5.2 占位配置集中位置

**所有占位配置集中在 `application.yml` 的 `app.*` 段**，并由
`config/AppProperties.java` 强类型绑定（`@ConfigurationProperties(prefix = "app")`）。
代码中不散落隐式默认值。

### 5.3 关键配置行为

| 配置 | 值 | 原因 |
| --- | --- | --- |
| `spring.jpa.hibernate.ddl-auto` | `validate` | 表结构**只由 Flyway 管理**，Hibernate 不得改动 |
| `spring.jpa.open-in-view` | `false` | 避免视图层持有 Session（配合条件更新风格，R-18） |
| `spring.flyway.validate-on-migrate` | `true` | 脚本被改动时**立即失败**而非静默继续 |
| `management.endpoints.web.exposure.include` | `health,info` | 只暴露必要端点 |
| `springdoc.swagger-ui.url` | `/openapi/openapi.yaml` | Swagger UI 默认展示**已冻结契约**而非骨架端点 |

---

## 6. 常见问题

### 6.1 端口 5432 被占用

**症状**

```text
Error response from daemon: ... Bind for 0.0.0.0:5432 failed: port is already allocated
```

**原因**：宿主机已有 PostgreSQL 或其他容器占用 5432。

**解法**（任选）

```bash
# 先确认占用者
lsof -nP -iTCP:5432 -sTCP:LISTEN
docker ps --format '{{.Names}}: {{.Ports}}'

# 解法 A（推荐）：用环境变量改宿主机端口，不改文件
POSTGRES_HOST_PORT=55432 docker compose up -d --build
#   注意：app 容器内部始终用 postgres:5432，不受影响；
#   但你的本地 psql 需改用 -p 55432

# 解法 B：停掉占用 5432 的无关容器
docker stop <容器名>
```

> **本环境实测**：搭建时宿主机 5432 已被无关容器
> `campushub-poc-postgres` 占用，为不影响他人数据，验证改用
> `POSTGRES_HOST_PORT=55432`（详见
> `docs/iteration1/design/backend-skeleton-verification.md` §1）。

### 6.2 端口 8080 被占用

```bash
APP_HOST_PORT=8081 docker compose up -d
# 或本地运行时：
java -jar target/campus-trade.jar --server.port=8081
```

### 6.3 Flyway checksum 不一致

**症状**

```text
Migration checksum mismatch for migration version 1
FlywayValidateException: Validate failed: Migrations have failed validation
```

**原因**：修改了**已执行过**的迁移脚本（`V1`~`V4`）。

**解法**

- **正确做法**：把变更写成新增迁移 `V5__xxx.sql`，**不要**改已执行的脚本。
- 若只是本地验证库弄脏，可清库重来：

```bash
docker compose down -v      # ⚠️ 删除数据卷，数据全部丢失
docker compose up -d --build
```

- 若生产库已执行且确实需要修复元数据，使用 `flyway repair`
  （务必先确认原因，见 `docs/iteration1/design/db-verification.md` §5.2）。

> **项目约定**：`V1`~`V4` 已冻结。任何 schema 变更必须新增 `V5+`。

### 6.4 换过数据库名/密码后连不上（数据卷残留）

**症状**：`FATAL: role "campus_trade" does not exist` 或
`FATAL: database "campus_trade" does not exist`。

**原因**：`POSTGRES_DB/USER/PASSWORD` **仅在数据卷为空时**生效。
若此前用旧配置（如 v1.0 的 `jc2001`）初始化过数据卷，卷内仍是旧角色。

**解法**

```bash
docker compose down -v      # ⚠️ 删除数据卷后重新初始化
docker compose up -d --build
```

### 6.5 构建很慢 / 反复下载 Maven 依赖

**症状**：`docker compose up -d --build` 卡在 `mvn dependency:go-offline` 或
`Downloading from central`，耗时数十分钟。

**原因**：容器内构建时每次都是**全新容器**，`~/.m2` 不跨构建保留，
所有依赖都要重新下载；叠加 macOS 下 Docker 跨卷 I/O 慢。

**解法（本项目已默认采用）**：改用「宿主机打包 + 容器仅作运行环境」，
即默认 `Dockerfile` 只执行 `COPY target/campus-trade.jar`。
实测构建时间 **38 分钟 → 9 秒**。

若必须容器内构建，请用 `Dockerfile.build`（已实现 pom 先 COPY +
`dependency:go-offline` 的**依赖层缓存**），并配合 `.dockerignore`
排除 `.mvnrepo`（构建上下文 155MB → 50MB）。

### 6.5.1 镜像构建无法拉取基础镜像

**症状**

```text
failed to resolve source metadata for docker.io/library/eclipse-temurin:11-jre
```

**原因**：Dockerfile 运行阶段默认使用 `maven:3.9-eclipse-temurin-11`
（本环境无法访问 Docker Hub，改用本地已有镜像）。若你的环境可访问 Docker Hub，
建议换成更小的纯 JRE 镜像：

```bash
docker compose build --build-arg RUNNER_IMAGE=eclipse-temurin:11-jre
# 或直接修改 Dockerfile 中 RUNNER_IMAGE 的默认值
```

两者都是 Temurin 11，**替换不影响任何构建/运行行为**，仅镜像体积不同。

### 6.6 `app.jwt.secret 长度不足` 启动失败

**症状**

```text
IllegalStateException: app.jwt.secret 长度不足：HS256 要求至少 32 字节
```

**原因**：HS256 要求密钥 ≥ 256 bit。这是**有意的快速失败**，
避免带着弱密钥启动到生产。

**解法**：注入足够长的随机密钥。

```bash
export APP_JWT_SECRET="$(openssl rand -base64 48)"
java -jar target/campus-trade.jar
```

### 6.7 应用启动早于数据库就绪

compose 中 `app` 已声明 `depends_on: postgres: condition: service_healthy`，
正常情况下不会发生。若本地手工运行时遇到连接失败，等待数据库就绪后重启应用即可。

### 6.8 健康检查显示 `DEGRADED`

访问 `/api/v1/health/ready` 查看具体原因：

- `database: DOWN` → 数据库不可达（检查连接串/网络/容器状态）
- `flyway.appliedCount` 小于 4 → 迁移未完成（查看应用日志中的 Flyway 输出）

### 6.9 Flyway 提示 PostgreSQL 15 未测试

**症状**

```text
WARN o.f.c.i.database.base.Database - Flyway upgrade recommended: PostgreSQL 15.19
is newer than this version of Flyway and support has not been tested.
The latest supported version of PostgreSQL is 14.
```

**这是提示，不是错误**：迁移实际执行成功（`Successfully applied 4 migrations`）。
原因见 §7.1，**请勿据此升级 Flyway 版本**。

---

## 7. 已知事项与后续步骤

### 7.1 Flyway 版本与 PostgreSQL 15 的说明（重要决策）

当前使用 Spring Boot 2.7.18 **托管**的 Flyway 8.5.13，因此会有「PostgreSQL 15 未测试」提示。

**为什么不升级到 Flyway 9/10**：

| 版本 | Java 要求 | 与 Spring Boot 2.7 的兼容性 |
| --- | --- | --- |
| 8.5.13（当前） | Java 8+ | ✅ Boot 2.7 官方托管；`FlywayAutoConfiguration` 绑定 Flyway 8 的 `Configuration` API |
| 9.x | Java 8+ | ❌ Flyway 9 移除了 `Configuration` 类并重构 `FluentConfiguration`，Boot 2.7 的自动配置会因**找不到类/方法签名不匹配**而启动失败 |
| 10.x | **Java 17** | ❌ 与「Java 11 固定」直接冲突；且 PostgreSQL 支持被拆到独立模块 `flyway-database-postgresql` |

**结论**：保持 8.5.13。理由：
1. 迁移**实际执行成功**（V1~V4 全部成功，25 张表创建完成），提示仅为版本白名单未更新；
2. V1~V4 只使用**幂等 DDL**（`CREATE TABLE` / `CREATE INDEX` / `CREATE TYPE`），不依赖 PG15 新特性，兼容风险极低；
3. 换用非 Boot 托管版本属于**跨版本混搭**，会带来真实且更高的启动失败风险。

> 若未来升级到 Spring Boot 3.x（Java 17），届时可一并升级到 Flyway 10.x，
> 并按 Flyway 10 的模块化要求额外引入 `flyway-database-postgresql`。

### 7.2 安全配置当前为「骨架放开模式」（必须收紧）

`SecurityConfig` 目前除公开路径外**全部 `permitAll`**，以便联调。启动日志会输出醒目告警。

**下一步必须**按 `docs/iteration1/design/api-permission-mapping.md` 收紧：

| 路径 | 目标规则 |
| --- | --- |
| `/api/v1/auth/register`、`/auth/login`、`/auth/refresh` | `permitAll`（契约 `security: []`） |
| `/api/v1/trade-zones/**`、`/categories`、`/item-conditions` | `permitAll`（公开字典） |
| `GET /api/v1/items`、`GET /api/v1/items/{id}` | `permitAll`（访客可浏览） |
| `GET /api/v1/users/{id}/public-profile` | `permitAll`（公开资料） |
| `/api/v1/admin/**` | `hasRole('ADMIN')` |
| `/api/v1/internal/**` | 内部凭证校验；网关层禁止外部访问（AD-18） |
| 其余 `/api/v1/**` | `authenticated()` |

同时补齐三条硬性约束：

- **越权返回 403，不用 404**（E-02 / AD-02）
- **被处罚用户（RESTRICTED）禁止交易类写操作** → 403 + `restrictedUntil`（E-03）
- **管理员不参与交易**（P-00-A）→ 交易类接口显式拒绝 `ROLE_ADMIN`

### 7.4 构建方案决策：为什么默认不在容器内构建

| 方案 | 实现 | 实测耗时 | 结论 |
| --- | --- | --- | --- |
| 容器内多阶段构建（初版） | 容器内 `mvn dependency:go-offline` + `mvn package` | **约 38 分钟** | ❌ 每次全新下载依赖，不可接受 |
| **宿主机打包 + 复制产物（当前默认）** | `Dockerfile`：仅 `COPY target/campus-trade.jar` | **9 秒** | ✅ 采用 |
| 容器内构建 + 依赖层缓存（可选） | `Dockerfile.build`：先 COPY pom.xml 再 `go-offline` | 首次慢、pom 未改时命中缓存 | ✅ 保留给 CI |

配套优化：新增 `.dockerignore`，把构建上下文从 **155MB 降到 50MB**
（排除 `.mvnrepo` 105MB、`.git`、日志等；`target/` 必须保留，
因为其中 50MB 的 fat jar 正是要复制进镜像的产物）。

另外修复了一处真实缺陷：初版用 `FROM ${RUNNER_IMAGE}` + Compose `build.args` 传值，
当变量为空时 Docker 报 `base name (${RUNNER_IMAGE}) should not be blank` 导致构建失败。
现已改为直接写死 `FROM`，避免空值风险。

### 7.3 本步骤明确不做

| 不做项 | 说明 |
| --- | --- |
| ~~83 个业务 Controller~~ | ✅ **第五步已完成生成**（见 `controller-skeleton-verification.md`） |
| 业务逻辑实现 | 83 个方法体统一抛 `UnsupportedOperationException`，下一步实现 |
| 业务 Service / Repository / Entity | `service`/`repository`/`entity`/`dto` 为占位包 |
| 93 个业务错误码 | `ErrorCode` 当前只有 7 个通用码，其余按 `error-codes.md` 补齐 |
| 前端代码 | 不在本步骤范围 |
| 修改 `V1`~`V4` 迁移脚本 | **禁止**（Flyway checksum） |

---

## 8. 相关文档

| 文档 | 用途 |
| --- | --- |
| `docs/iteration1/design/api-freeze.md` | 接口契约冻结声明与变更流程 |
| `docs/iteration1/design/api-contract.md` | 83 个接口清单与 54 FR 映射 |
| `docs/iteration1/design/api-permission-mapping.md` | 权限收紧依据 |
| `docs/iteration1/design/error-codes.md` | 93 个错误码（`ErrorCode` 补齐依据） |
| `docs/iteration1/design/idempotency-and-concurrency.md` | 幂等与并发实现约束 |
| `docs/iteration1/design/schema.md` | 25 张表字段字典（实体生成依据） |
| `docs/iteration1/design/backend-skeleton-verification.md` | 本步骤验证记录（实测输出） |
| `README-db.md` | 数据库启动、迁移、查询 |
| `db/verify_schema.sql` | 数据库对账脚本（只读可重复执行） |

---

## 9. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 后端工程负责人 | 初稿：环境要求、目录结构、三种启动方式、验证步骤、配置说明、9 项常见问题、Flyway 版本决策、安全收紧清单 | 草稿（待确认） |
| v1.1 | 2026-10-12 | 后端工程负责人 | 构建提速：默认 Dockerfile 改为「复制宿主机 jar」的运行镜像（38 分钟 → 9 秒）；新增 Dockerfile.build（容器内构建，含依赖层缓存）与 .dockerignore（上下文 155MB → 50MB）；修复 RUNNER_IMAGE 空值导致构建失败的缺陷；补充 §6.5 与 §7.4 | 草稿（待确认） |
| v1.2 | 2026-10-12 | 后端接口骨架负责人 | 第五步：按冻结契约生成 83 个 operationId 的 Controller/DTO/Service 骨架（12 Controller + 99 DTO + 12 Service）；Swagger UI 默认展示 83 个操作；新增 §4.1.1 验证方法 | 草稿（待确认） |
