# 迭代一 后端工程骨架 验证记录

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 后端工程骨架 验证记录 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 后端工程负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 验证对象 | `pom.xml`、`Dockerfile`、`Dockerfile.build`、`.dockerignore`、`docker-compose.yml`、`src/main/**` |
| 验证环境 | macOS + Docker Desktop 29.8.1 / Compose v5.5.1 / **JDK Temurin 11.0.32.1** / Maven 3.9（容器）/ **PostgreSQL 15.19** |
| 结论 | ✅ **验收标准 1~10 全部通过** |

---

## 1. 验证环境与端口说明

| 项 | 说明 |
| --- | --- |
| JDK（宿主机） | Temurin 11.0.32.1（`/usr/libexec/java_home -v 11`） |
| Maven | 宿主机**未安装**；使用容器 `maven:3.9-eclipse-temurin-11` 执行构建 |
| PostgreSQL | `postgres:15`（实测 15.19） |
| 交付的 compose 端口 | **5432**（数据库，任务默认值）、**8080**（应用） |
| 本次实际验证端口 | 数据库 **55432**（原因见下）、应用 **8080** |

> **⚠️ 端口冲突（实际遇到）**
> 验证机上 `5432` 已被一个**与本项目无关**的既有容器占用
> （`campushub-poc-postgres`）。为**不影响他人数据**，验证时用环境变量覆盖：
>
> ```bash
> POSTGRES_HOST_PORT=55432 APP_HOST_PORT=8080 docker compose up -d
> ```
>
> **交付文件中的默认映射仍是 5432**（符合任务要求），且
> `app` 容器内部始终通过服务名 `postgres:5432` 访问数据库，**不受该覆盖影响**。
> 用户机器上 5432 空闲时无需任何改动。

---

## 2. 验收标准逐项结果

| # | 验收标准 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | `mvn clean package -DskipTests` 成功，无编译错误 | ✅ | §3，`BUILD SUCCESS`，23 个源文件编译通过 |
| 2 | `docker compose up -d --build` 成功启动 postgres + app | ✅ | §5，`up` 退出码 0，耗时 5 秒 |
| 3 | Flyway 自动执行 V1~V4，`flyway_schema_history` 4 条成功 | ✅ | §6，4 条记录 `success = t` |
| 4 | `curl /actuator/health` 返回 `{"status":"UP"}` | ✅ | §7.1 |
| 5 | `curl /api/v1/health` 返回统一响应包络 | ✅ | §7.2 |
| 6 | Swagger UI 可访问，显示 HealthController | ✅ | §7.4，页面 200，api-docs 含 2 个健康端点 |
| 7 | 含分层结构、统一响应、全局异常、Security/JWT 骨架、Flyway 配置 | ✅ | §8 |
| 8 | 未实现业务逻辑、未生成 83 个 Controller、未改 V1~V4 | ✅ | §9 |
| 9 | `README-backend.md` 含启动、验证、常见问题 | ✅ | §8 |
| 10 | 完成报告含启动日志、健康检查、Flyway 记录、Swagger 结果、文件列表、假设、待确认、自检 | ✅ | 本文档 + 完成报告 |

---

## 3. Maven 构建结果

```text
[INFO] --- maven-compiler-plugin:3.10.1:compile (default-compile) ---
[INFO] Compiling 23 source files to /workspace/target/classes
[INFO] --- maven-jar-plugin:3.2.2:jar (default-jar) ---
[INFO] Building jar: /workspace/target/campus-trade.jar
[INFO] --- spring-boot-maven-plugin:2.7.18:repackage (repackage) ---
[INFO] Replacing main artifact with repackaged archive
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

| 指标 | 值 |
| --- | --- |
| 源文件数 | 23（含 5 个 `package-info.java`） |
| 产物 | `target/campus-trade.jar`（约 50.2 MB，fat jar） |
| 契约资源打入 | ✅ `BOOT-INF/classes/openapi/openapi.yaml`（264 KB） |
| 首次构建耗时 | 约 7 分 40 秒（含下载全部依赖） |
| 增量构建耗时 | **22.8 秒**（依赖已缓存） |

---

## 4. 构建提速（重要改进）

初次采用「容器内多阶段构建」时，镜像构建**耗时约 38 分钟**且最终失败，原因是：

1. 每次构建都是全新容器，需**重新下载全部 Maven 依赖**；
2. 构建上下文含 `.mvnrepo`（105 MB），共 **155 MB** 需发送给 daemon；
3. macOS 下 Docker 跨卷 I/O 慢，放大上述问题。

### 4.1 优化措施

| 措施 | 效果 |
| --- | --- |
| 默认 `Dockerfile` 改为**运行镜像**（仅 `COPY target/campus-trade.jar`，不做 Maven 构建） | 省去容器内依赖下载 |
| 新增 `.dockerignore`（排除 `.mvnrepo`、`.git`、日志等；**保留 `target/`**） | 构建上下文 **155 MB → 49.6 MB** |
| 保留 `Dockerfile.build` 作为 CI 方案（pom 先 COPY + `dependency:go-offline` 依赖层缓存） | pom 未改时命中缓存 |
| 修复 `FROM ${RUNNER_IMAGE}` 空值缺陷 | 见 §4.3 |

### 4.2 实测耗时对比

| 方案 | 镜像构建耗时 |
| --- | --- |
| 容器内多阶段构建（初版） | **约 38 分钟**（且因下述缺陷失败） |
| **宿主机打包 + 运行镜像（当前默认）** | **9 秒** |
| 全栈 `docker compose up -d`（镜像已就绪） | **5 秒** |
| app 容器从启动到 healthy | **约 10 秒**（含 Flyway V1~V4 与 Tomcat 启动） |

### 4.3 期间修复的构建缺陷

| # | 缺陷 | 报错 | 原因 | 修复 |
| --- | --- | --- | --- | --- |
| 1 | `RUNNER_IMAGE` 空值 | `base name (${RUNNER_IMAGE}) should not be blank` | Dockerfile 用 `FROM ${RUNNER_IMAGE}`，而 `docker-compose.yml` 的 `build.args.RUNNER_IMAGE` 在变量未设置时被解析为空字符串，**空值覆盖了 Dockerfile 的 ARG 默认值** | 改为直接写死 `FROM maven:3.9-eclipse-temurin-11`，并移除 compose 中的 `build.args`；换镜像只需改该行 |
| 2 | `.dockerignore` 误排除 `target/` | `COPY failed: ... stat target/campus-trade.jar: file does not exist` | 运行镜像需要 `COPY target/campus-trade.jar`，与「排除 target」矛盾 | 从 `.dockerignore` 移除 `target/`，并加注释说明原因 |

---

## 5. Docker Compose 启动结果

### 5.1 `docker compose ps`

```text
NAME              IMAGE                         COMMAND                  SERVICE    STATUS                    PORTS
jc2001-app        campus-trade:1.0.0-SNAPSHOT   "sh -c 'exec java $J…"   app        Up (healthy)              0.0.0.0:8080->8080/tcp
jc2001-postgres   postgres:15                   "docker-entrypoint.s…"   postgres   Up (healthy)              0.0.0.0:55432->5432/tcp
```

> 数据库宿主机端口显示 55432 是因为本次验证用 `POSTGRES_HOST_PORT=55432` 覆盖；
> 交付默认值为 5432。

### 5.2 启动顺序验证

```text
Container jc2001-postgres Starting
Container jc2001-postgres Started
Container jc2001-postgres Waiting     ← depends_on: service_healthy 生效
Container jc2001-postgres Healthy
Container jc2001-app Starting
Container jc2001-app Started
```

`depends_on: postgres: condition: service_healthy` 有效：app 在数据库健康后才启动，
避免 Flyway 因连接失败中断。

---

## 6. Flyway 迁移结果（应用启动时自动执行）

### 6.1 app 容器日志

```text
2026-10-12 22:33:45.413 [main] INFO  o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "1 - init schema"
2026-10-12 22:33:45.693 [main] INFO  o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "2 - seed reference data"
2026-10-12 22:33:45.707 [main] INFO  o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "3 - seed development data"
2026-10-12 22:33:45.751 [main] INFO  o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "4 - fix review window index"
2026-10-12 22:33:45.762 [main] INFO  o.f.core.internal.command.DbMigrate - Successfully applied 4 migrations to schema "public", now at version v4 (execution time 00:00.408s)
2026-10-12 22:33:46.917 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http) with context path ''
2026-10-12 22:33:46.929 [main] INFO  c.j.c.CampusTradeApplication - Started CampusTradeApplication in 3.7 seconds
```

### 6.2 `flyway_schema_history` 查询

```sql
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

| installed_rank | version | description | success |
| --- | --- | --- | --- |
| 1 | 1 | init schema | t |
| 2 | 2 | seed reference data | t |
| 3 | 3 | seed development data | t |
| 4 | 4 | fix review window index | t |

### 6.3 迁移结果核对

| 检查项 | 期望 | 实测 | 结果 |
| --- | --- | --- | --- |
| 迁移条数 | 4 | 4 | ✅ |
| 全部成功 | 是 | 全 `t` | ✅ |
| 业务表数量 | 25 | 25 | ✅ |
| 种子数据 | users=4, items=9, trade_zones=7 | 一致 | ✅ |

---

## 7. 端点验证结果

### 7.1 `GET /actuator/health` → 200

```json
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP"},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"}},"groups":["db-readiness","liveness","readiness"]}
```

`db` 组件为 UP，说明**数据库连通且迁移完成**。

### 7.2 `GET /api/v1/health` → 200（统一响应包络）

```json
{"code":"OK","message":"服务正常","data":"UP","traceId":"7debf9cc660b4c86","timestamp":"2026-10-12T14:33:56.357504Z"}
```

字段与契约 `ApiResponse`（code / message / data / traceId / timestamp）一致；
`timestamp` 为 ISO 8601 UTC（尾缀 `Z`）。

### 7.3 `GET /api/v1/health/ready` → 200

```json
{"code":"OK","message":"就绪检查完成","data":{"iteration":"iteration-1","apiVersion":"1.0.0-frozen-candidate","database":"UP","flyway":{"appliedCount":4,"currentVersion":"4","expectedCount":4},"status":"UP"},"traceId":"e746516bb40f4179","timestamp":"2026-10-12T14:33:56.378975Z"}
```

### 7.4 Swagger UI 与契约文档

| 检查 | 实测 |
| --- | --- |
| `GET /swagger-ui.html` | `302` → `/swagger-ui/index.html` |
| `GET /swagger-ui/index.html` | `200`，`<title>Swagger UI</title>` |
| `GET /openapi/openapi.yaml` | `200`，**264148 bytes**，`application/yaml;charset=UTF-8` |
| 契约内容一致性 | ✅ 与 `docs/iteration1/design/openapi.yaml` **逐字节相同** |
| 契约内容核对 | ✅ openapi 3.0.3、version `1.0.0-frozen-candidate`、**73 paths / 83 operations** |
| `GET /v3/api-docs` | `200`，含 `/openapi/openapi.yaml`、`/api/v1/health`、`/api/v1/health/ready` |
| 安全方案 | ✅ `bearerAuth`（HTTP bearer JWT）、`internalToken`（`X-Internal-Token`） |

> **Swagger UI 展示内容说明**：`springdoc.swagger-ui.url` 指向 `/openapi/openapi.yaml`，
> 因此打开 Swagger UI 看到的是**已冻结的完整迭代一契约（83 个操作）**，
> 而非骨架自动生成的少量端点。任务验收要求「Swagger UI 可访问，显示 HealthController」——
> HealthController 的两个端点同时出现在 `/v3/api-docs`（运行时文档）中，
> 并可通过将 `springdoc.swagger-ui.url` 置空切换为默认展示。

### 7.5 统一错误响应格式

| 场景 | 响应 | HTTP |
| --- | --- | --- |
| 未知路由 `GET /api/v1/not-exists` | `{"code":"COMMON_RESOURCE_NOT_FOUND","message":"请求的接口不存在","traceId":"...","timestamp":"..."}` | 404 |
| 方法不支持 `POST /api/v1/health` | `{"code":"VALIDATION_INVALID_PARAMETER","message":"请求方法不被支持",...}` | 400 |

**改进点**：初版未处理时，未映射路由会落到 Spring Boot Whitelabel 错误结构
（`timestamp/status/error/path`），与契约的 `ErrorResponse` 不一致。
为此新增 `ApiErrorController`（实现 `ErrorController`）接管 `/error`，
将容器级错误统一改写为契约格式。

### 7.6 traceId 贯穿

```bash
curl -D - -o /dev/null -H 'X-Trace-Id: my-trace-0001' http://localhost:8080/api/v1/health
# → X-Trace-Id: my-trace-0001
```

响应头与响应体 `traceId` 一致；调用方可指定，未指定时服务端生成 16 位十六进制值。

---

## 8. 工程结构与配置核对

### 8.1 分层结构

| 包 | 内容 | 状态 |
| --- | --- | --- |
| `com.jc2001.campustrade` | `CampusTradeApplication` | ✅ |
| `common` | `ApiResponse`、`PageResult`、`ErrorResponse`、`ErrorCode`、`GlobalExceptionHandler`、`ApiErrorController`、`TraceIdFilter` | ✅ |
| `config` | `SecurityConfig`、`OpenApiConfig`、`JacksonConfig`、`AppProperties` | ✅ |
| `security` | `JwtTokenProvider`、`JwtAuthenticationFilter`、`AuthenticatedUser`、`UserDetailsServiceImpl` | ✅ |
| `controller` | `HealthController`、`FrozenSpecController` | ✅ 仅工程自检，**无业务 Controller** |
| `service` / `service.impl` / `repository` / `entity` / `dto` | 占位包，`package-info.java` 写明下一步约束 | ✅ 空实现 |

### 8.2 配置核对（`application.yml`）

| 配置 | 要求 | 实测值 | 结果 |
| --- | --- | --- | --- |
| `server.port` | 8080 | 8080 | ✅ |
| 数据源 URL | `jdbc:postgresql://localhost:5432/campus_trade` | 同（compose 覆盖为 `postgres:5432`） | ✅ |
| 用户名/密码 | 环境变量注入，默认 `campus_trade` | 同 | ✅ |
| `jpa.hibernate.ddl-auto` | `validate` | `validate` | ✅ |
| `jpa.open-in-view` | `false` | `false` | ✅ |
| Flyway enabled | 是 | `true` | ✅ |
| Flyway locations | `classpath:db/migration` | 同 | ✅ |
| Flyway validate-on-migrate | —— | `true`（脚本被改动即失败） | ✅ |
| Actuator 暴露 | health、info | `health,info` | ✅ |
| springdoc swagger-ui path | `/swagger-ui.html` | 同 | ✅ |
| springdoc api-docs path | `/v3/api-docs` | 同 | ✅ |
| 日志级别 | root INFO，项目 DEBUG（dev） | 主配置 INFO / dev profile DEBUG | ✅ |
| `app.jwt.secret` | 占位 | `CHANGE_ME_...`（可被 `APP_JWT_SECRET` 覆盖） | ✅ |
| `app.jwt.expiration` | 占位 | `604800`（7 天，假设 A-06） | ✅ |
| `app.internal.token` | 占位 | `CHANGE_ME_...`（可被 `APP_INTERNAL_TOKEN` 覆盖） | ✅ |

**占位配置集中性**：全部 `app.*` 占位项集中于 `application.yml` 的 `app:` 段，
并由 `AppProperties`（`@ConfigurationProperties(prefix="app")`）强类型绑定，
代码中无散落的隐式默认值。

### 8.3 Security 骨架状态

| 项 | 状态 |
| --- | --- |
| CSRF | 已禁用（前后端分离） |
| Session | `STATELESS` |
| 白名单 | `/actuator/health*`、`/v3/api-docs*`、`/swagger-ui*`、`/openapi/**`、`/api/v1/auth/{register,login,refresh}`、`/api/v1/health*`、`/static/**` |
| 其余请求 | `permitAll`（**骨架放开模式**） |
| 启动告警 | ✅ 日志输出醒目 WARNING 提示需收紧 |
| TODO 标注 | ✅ `SecurityConfig` 内含收敛规则注释块 |
| 401/403 响应 | ✅ 使用契约 `ErrorResponse` 结构（`AuthenticationEntryPoint` / `AccessDeniedHandler`） |
| 密码编码器 | ✅ BCrypt |

---

## 9. 明确未做（与范围一致）

| 项 | 核实方式 | 结果 |
| --- | --- | --- |
| 未实现业务逻辑 | `service`/`service.impl`/`repository`/`entity`/`dto` 仅含 `package-info.java` | ✅ |
| 未生成 83 个业务 Controller | `controller` 包仅 `HealthController`、`FrozenSpecController` | ✅ |
| 未写前端代码 | 无 `.vue`/`.ts`/`.js` 前端源文件 | ✅ |
| 未修改 V1~V4 迁移脚本 | `flyway validate` 通过、`flyway_schema_history` 无 checksum 冲突 | ✅ |
| 未新增迁移 | `db/migration` 仅 V1~V4 | ✅ |

---

## 10. 已知事项

| # | 事项 | 说明 |
| --- | --- | --- |
| 1 | **Flyway 提示 PostgreSQL 15 未测试** | `WARN ... PostgreSQL 15.19 is newer than this version of Flyway and support has not been tested.` 属**提示非错误**；迁移实际全部成功。保持 Boot 2.7 托管的 Flyway 8.5.13，理由见 `README-backend.md` §7.1（Flyway 9 移除 Boot 2.7 依赖的 `Configuration` API；Flyway 10 要求 Java 17，与固定 Java 11 冲突） |
| 2 | **Security 为骨架放开模式** | 除白名单外全部 `permitAll`，**下一步必须**按 `api-permission-mapping.md` 收紧 |
| 3 | **`UserDetailsServiceImpl` 始终抛 `UsernameNotFoundException`** | 有意为之：`entity`/`repository` 尚未生成，返回伪造用户会掩盖未实现状态 |
| 4 | **`ErrorCode` 仅 7 个通用码** | 契约中 93 个业务错误码在下一步按 `error-codes.md` 补齐 |
| 5 | **`PageResult` 字段名待统一** | 现为 `page/size/total/items` + 契约别名 `content/totalElements`；下一步统一为契约字段名 |
| 6 | **镜像体积 954 MB** | 因运行基础镜像使用 `maven:3.9-eclipse-temurin-11`（本环境无法访问 Docker Hub）。可访问 Docker Hub 时改为 `eclipse-temurin:11-jre` 可显著减小 |
| 7 | **未附 Maven Wrapper** | 本环境无法联网拉取 wrapper 分发包；无本地 Maven 时用容器构建（`README-backend.md` §3.3） |
| 8 | **buildx 受沙箱限制** | 本次构建使用 `DOCKER_BUILDKIT=0`（legacy builder）。正常开发机用默认 BuildKit 亦可 |

---

## 11. 复现步骤（完整）

```bash
# ── 1) 宿主机打包 ────────────────────────────────────────────
mvn clean package -DskipTests
#   无本地 Maven：
#   docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 \
#     -w /workspace maven:3.9-eclipse-temurin-11 mvn -B -DskipTests clean package

# ── 2) 构建并启动（5432 空闲时可直接执行）────────────────────
docker compose up -d --build
#   5432 被占用时（本次验证即此情况）：
#   POSTGRES_HOST_PORT=55432 docker compose up -d --build

# ── 3) 等待健康 ─────────────────────────────────────────────
docker compose ps          # 期望 postgres 与 app 均为 (healthy)

# ── 4) 验收端点 ─────────────────────────────────────────────
curl http://localhost:8080/actuator/health                # {"status":"UP",...}
curl http://localhost:8080/api/v1/health                  # 统一响应包络
curl http://localhost:8080/api/v1/health/ready            # flyway.appliedCount=4
curl -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html
curl -o /dev/null -w "%{http_code}\n" http://localhost:8080/openapi/openapi.yaml

# ── 5) 迁移记录 ─────────────────────────────────────────────
docker exec jc2001-postgres psql -U campus_trade -d campus_trade \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# ── 6) 清理 ─────────────────────────────────────────────────
docker compose down            # 保留数据卷
# docker compose down -v       # ⚠️ 同时删除数据卷（Flyway 需重新执行）
```

---

## 12. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 后端工程负责人 | 初稿：验收标准逐项结果、Maven 构建输出、构建提速对比（38 分钟→9 秒）与 2 个缺陷修复、Compose 启动与 Flyway 记录、端点验证、工程结构与配置核对、8 项已知事项、完整复现步骤 | 草稿（待确认） |
