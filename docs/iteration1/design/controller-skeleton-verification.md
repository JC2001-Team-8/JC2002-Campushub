# 迭代一 接口骨架（83 operationId）验证记录

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 接口骨架（83 operationId）验证记录 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 后端接口骨架负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 验证对象 | `controller/`、`dto/request/`、`dto/response/`、`service/`、`service/impl/`、`tools/generate_api_skeleton.py` |
| 契约真源 | `docs/iteration1/design/openapi.yaml`（73 paths / 83 operations / 99 schemas，`1.0.0-frozen-candidate`） |
| 验证环境 | macOS + Docker Desktop 29.8.1 / JDK Temurin 11.0.32.1 / Maven 3.9（容器）/ PostgreSQL 15.19 |
| 结论 | ✅ **验收标准 1~12 全部通过**；83/83 operationId 与契约完全 1:1 |

---

## 1. 生成方式与产出统计

### 1.1 为什么用生成器而非手写

契约已**冻结**（83 operationId / 99 schemas）。手写必然产生拼写、路径、类型或
operationId 的偏差，而验收要求 1:1。因此编写一次性生成器
`tools/generate_api_skeleton.py`（**只读取**契约，绝不修改契约与迁移脚本），
从 `openapi.yaml` 生成全部骨架。

| 项 | 说明 |
| --- | --- |
| 生成器路径 | `tools/generate_api_skeleton.py`（一次性工具，属构建期辅助，非运行时代码） |
| 输入 | `docs/iteration1/design/openapi.yaml`（只读） |
| 幂等性 | 可重复执行，覆盖式生成（**手工维护文件受保留清单保护**，见 §1.3） |
| 对账清单 | 生成时同步输出 `target/api-skeleton-manifest.txt`（operationId / 方法 / 路径 / Controller） |

### 1.2 产出统计

| 类别 | 数量 | 说明 |
| --- | --- | --- |
| **Controller（契约生成）** | **12** | 覆盖 83 个 operationId |
| Controller（手工维护） | 2 | `HealthController`、`FrozenSpecController`（非契约端点） |
| **DTO / request** | **33** | 请求体 Schema |
| **DTO / response** | **66** | 响应与共享类型（含 **22 个枚举**） |
| **Service 接口** | **12** | 按域拆分 |
| **Service 实现** | **12** | 全部方法抛 `UnsupportedOperationException` |
| Java 源文件总数 | 158 | 含 5 个 `package-info.java` |
| 契约 schemas 覆盖 | **99 / 99** | 77 个 DTO 类 + 22 个枚举 = 99 |

**Controller 与 Service 的 12 个模块划分**（契约 tag 是分组，一个分组可对应多个类）：

| Controller | 契约 tag | operations | Service |
| --- | --- | --- | --- |
| `AuthController` | 01-认证与账户 | 6 | `AuthService` |
| `UserController` | 02-个人资料 | 5 | `UserService` |
| `DictionaryController` | 03-字典与区域 | 4 | `DictionaryService` |
| `ItemController` | 04-商品 | 10 | `ItemService` |
| `CartController` | 05-购物车 | 4 | `CartService` |
| `OrderController` | 06-订单与付款 | 8 | `OrderService` |
| `ReviewController` | 07-评价 | 3 | `ReviewService` |
| `SwapController` | 08-交换包 + 09-交换提议 | 16 | `SwapService` |
| `ReportController` | 10-举报 | 3 | `ReportService` |
| `AdminController` | 11-管理员治理 | 17 | `AdminService` |
| `NotificationController` | 12-通知 | 3 | `NotificationService` |
| `InternalController` | 13-系统定时任务 | 4 | `InternalTaskService` |
| **合计** | 13 个 tag | **83** | 12 个 Service |

> **说明**：契约的 tag `08-交换包` 与 `09-交换提议` 合并为一个 `SwapController`
> （同属交换域，`/swap-bundles/**` 与 `/swap-proposals/**`），
> `@Tag` 注解与 operationId 保持契约原值不变。

### 1.3 生成器保留清单（重要修复）

生成器初版会**无条件删除** `controller/`、`service/`、`service/impl/` 下的所有 `.java`，
导致手工维护的 `HealthController` 与 `FrozenSpecController` 被删除（实测发生）。
现已加入保留清单，两个手工文件不会被删除或覆盖：

```python
KEEP = {
    'controller/HealthController.java',
    'controller/FrozenSpecController.java',
}
```

---

## 2. operationId 对账（1:1）

### 2.1 对账方法

对 `/v3/api-docs`（运行时文档）与 `openapi.yaml`（契约）分别提取
`(operationId, HTTP方法, 路径)` 三元组，路径统一补齐 `/api/v1` 前缀后逐项比对。

### 2.2 对账结果

```text
====================================================================
operationId 对账（规范化 /api/v1 前缀后）
====================================================================
  契约 = 83   运行时 = 83
  缺失（契约有、运行时无）  : 0
  多余（运行时、契约无）    : 0
  HTTP 方法不一致           : 0
  路径不一致                : 0
  结论: ✅ 83/83 完全 1:1（无缺失、无多余、无重命名）
```

### 2.3 按 tag 分布核对

| tag | 契约 | 运行时 | 一致 |
| --- | --- | --- | --- |
| 01-认证与账户 | 6 | 6 | ✅ |
| 02-个人资料 | 5 | 5 | ✅ |
| 03-字典与区域 | 4 | 4 | ✅ |
| 04-商品 | 10 | 10 | ✅ |
| 05-购物车 | 4 | 4 | ✅ |
| 06-订单与付款 | 8 | 8 | ✅ |
| 07-评价 | 3 | 3 | ✅ |
| 08-交换包 | 16\* | 16\* | ✅ |
| 10-举报 | 3 | 3 | ✅ |
| 11-管理员治理 | 17 | 17 | ✅ |
| 12-通知 | 3 | 3 | ✅ |
| 13-系统定时任务 | 4 | 4 | ✅ |
| **合计** | **83** | **83** | ✅ |

> \* 契约 tags 中 `08-交换包` 与 `09-交换提议` 合并计入 `SwapController`，
> 上表按契约 tag 聚合展示。

---

## 3. 权限注解核对

由契约 `x-required-roles` 扩展推导 `@PreAuthorize` 表达式，逐项统计：

| `@PreAuthorize` 表达式 | 数量 | 契约对应角色 | 说明 |
| --- | --- | --- | --- |
| `permitAll()` | **10** | `GUEST`（`security: []`） | 公开接口，另加 `@SecurityRequirements` 显式声明空安全需求 |
| `isAuthenticated()` | **55** | `USER` / `USER,ADMIN` / `USER,RESTRICTED` | 需登录；「本人」约束在 Service 层做归属校验（E-02 返回 403） |
| `hasRole('ADMIN')` | **14** | `ADMIN` | 仅管理员（全部 `admin*` 操作） |
| `hasRole('SYSTEM')` | **4** | `SYSTEM` | `/internal/**` 定时任务，另加 `@SecurityRequirement(name="internalToken")` |
| **合计** | **83** | | 与契约 `x-required-roles` 分布一致 |

安全方案标注：

| 标注 | 数量 |
| --- | --- |
| `@SecurityRequirements`（公开，输出 `security: []`） | 10 |
| `@SecurityRequirement(name = "bearerAuth")` | 69 |
| `@SecurityRequirement(name = "internalToken")` | 4 |

### 3.1 与 `api-permission-mapping.md` 的一致性

| 权限映射要求 | 落地 |
| --- | --- |
| 公开 10 个接口 | ✅ `permitAll()` + `@SecurityRequirements` |
| 需登录 73 个 | ✅ 55 `isAuthenticated()` + 14 `hasRole('ADMIN')` + 4 `hasRole('SYSTEM')` |
| 仅管理员 14 个 | ✅ 全部 `admin*` 操作均为 `hasRole('ADMIN')` |
| 系统内部 4 个 | ✅ `/internal/**` 为 `hasRole('SYSTEM')` + `internalToken` 安全方案 |
| **P-00-A 管理员不参与交易** | ⚠️ 骨架阶段由 `@PreAuthorize` 仅做「需登录」判断；**下一步必须在 Service 层显式拒绝 `ROLE_ADMIN`**（已在 `service/` 的 `package-info` 与 Service 接口 Javadoc 中标注 TODO） |
| **E-02 越权返回 403（不用 404）** | ⚠️ 归属校验属业务逻辑，下一步在 Service 层实现（已标注） |
| **E-03 被处罚用户禁写** | ⚠️ 同上；契约 `x-forbidden-for-restricted` 已在方法 Javadoc 中生成标注 |

> 骨架阶段**只做接口层权限声明**，不做归属校验与处罚拦截——这属业务逻辑，本步骤明确不实现。

---

## 4. 幂等占位核对

依据 `idempotency-and-concurrency.md`，幂等要求来自契约的共享参数 `$ref`：

| 项 | 契约 | 生成结果 | 一致 |
| --- | --- | --- | --- |
| `Idempotency-Key` **必填**（`required = true`） | 16 | **16** | ✅ |
| `Idempotency-Key` **可选**（`required = false`） | 16 | **16** | ✅ |

生成形态（示例，`adminBanUser`）：

```java
// 幂等键 Idempotency-Key 见方法参数（R-17）
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/admin/users/{userId}/ban")
public ResponseEntity<ApiResponse<Penalty>> adminBanUser(
        @Parameter(description = "用户 ID") @PathVariable("userId") Long userId,
        @Parameter(description = "**必填**幂等键（R-17）...")
        @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
        @RequestBody BanUserRequest body
) {
    throw new UnsupportedOperationException("TODO: implement adminBanUser");
}
```

> **实现要点（已在 Service Javadoc 标注）**：幂等键只是第一层防护（R-17）；
> 第二层是**条件更新/乐观锁**（R-18），**禁止「先查后改」**。
> 两部分须在下一步一并实现。

### 4.1 生成器修复过程中发现的问题

| # | 问题 | 影响 | 修复 |
| --- | --- | --- | --- |
| 1 | 共享参数 `components.parameters` 的 `$ref` 未解析 | 幂等参数**全部丢失**（实测 0 必填 / 0 可选） | 解析 `$ref` 到 `components.parameters` 定义 |
| 2 | HTTP 头名 `Idempotency-Key` 含 `-`，不能做 Java 变量名 | 生成非法标识符 | 加入 `safe_ident()`：`Idempotency-Key → idempotencyKey`，`itemId` 保持原样 |
| 3 | 行内 `// 注释` 位于参数逗号之前 | Java 语法错误 | 改为参数列表上方的独立注释行 |

---

## 5. DTO 与契约一致性

### 5.1 覆盖

| 项 | 数量 |
| --- | --- |
| 契约 `components.schemas` | 99 |
| 生成 DTO 类 | 77（request 33 + response 44） |
| 生成枚举类 | 22 |
| **合计覆盖** | **99 / 99** |

### 5.2 类型映射（按任务要求）

| OpenAPI | Java | 备注 |
| --- | --- | --- |
| `string` | `String` | `format: date-time` → `Instant`（ISO 8601 UTC / AD-05） |
| `integer int32` | `Integer` | |
| `integer int64` | `Long` | |
| `number` | `BigDecimal` | |
| **`DecimalString`（金额）** | **`String`** | 契约用 `DecimalString` 表达金额，**避免浮点误差**（R-13）；以 `String` 承载 |
| `boolean` | `Boolean` | |
| `array` | `List<T>` | |
| `object`（无属性） | `Map<String, Object>` | 契约中零散 object 响应 |
| `$ref` | 对应 DTO 类 | 按 request/response 分属不同包 |
| `allOf: [PageResult, {content}]` | `PageResult<T>` | 内层元素类型从 `content.items.$ref` 反推（见 §5.3） |

### 5.3 `PageResult<T>` 泛型推导（重要）

OpenAPI 3.0 **无泛型语法**，契约把分页表达为
`allOf: [ {$ref: PageResult}, {properties: {content: {items: {$ref: X}}}} ]`。
生成器初版据此退化为 `Object`（分类信息丢失）。已改为从 `content.items.$ref`
反推元素类型，**16 个分页接口全部正确**：

| operationId | 响应类型 |
| --- | --- |
| `listItems` | `ApiResponse<PageResult<ItemSummary>>` |
| `listMyItems` | `ApiResponse<PageResult<ItemSummary>>` |
| `listOrders` | `ApiResponse<PageResult<OrderSummary>>` |
| `listMyReports` | `ApiResponse<PageResult<Report>>` |
| `listMyReceivedReviews` | `ApiResponse<PageResult<Review>>` |
| `listMySwapBundles` | `ApiResponse<PageResult<SwapBundleSummary>>` |
| `listSwapProposals` | `ApiResponse<PageResult<SwapProposalSummary>>` |
| `listMyNotifications` | `ApiResponse<PageResult<Notification>>` |
| `listMyPenalties` / `listMyAppeals` | `ApiResponse<PageResult<Penalty>>` / `<Appeal>` |
| `adminSearchUsers` | `ApiResponse<PageResult<AdminUserSummary>>` |
| `adminListUserPenalties` / `adminListPenalties` | `ApiResponse<PageResult<Penalty>>` |
| `adminListAppeals` | `ApiResponse<PageResult<Appeal>>` |
| `adminListReports` | `ApiResponse<PageResult<Report>>` |
| `adminListAuditLogs` | `ApiResponse<PageResult<AuditLog>>` |

> 修正后退化为 `Object` 的方法数：**16 → 0**。

### 5.4 敏感字段

| 检查项 | 结果 |
| --- | --- |
| `password_hash` | ✅ 未出现在任何 DTO |
| `deleted_at` | ✅ 未出现 |
| `created_by` / `updated_by` | ✅ 未出现 |
| `request_hash`（幂等内部字段） | ✅ 未出现 |

### 5.5 校验注解

由契约约束生成：`@NotNull`（required）、`@Size(min/max)`（minLength/maxLength/minItems/maxItems）、
`@Min` / `@Max`（minimum/maximum）。字段级 `@Schema(description = ...)` 由契约 description 生成。

> **未使用 Lombok**：任务要求可用 Lombok，但本环境 `.m2` 未缓存 Lombok 且需联网拉取，
> 且注解处理会引入构建脆弱性。因此生成**显式 getter/setter/toString**——
> 零新增依赖、完全自包含。`pom.xml` 未新增 Lombok 依赖。

---

## 6. 编译与启动验证

### 6.1 编译

```text
[INFO] Compiling 158 source files to /workspace/target/classes
[INFO] Building jar: /workspace/target/campus-trade.jar
[INFO] BUILD SUCCESS
```

| 项 | 值 |
| --- | --- |
| 源文件数 | 158 |
| 编译结果 | ✅ BUILD SUCCESS |
| 增量构建耗时 | 约 23 秒 |

**编译期修复的生成器缺陷**（详见 §8）：
未转义的反斜杠/换行/双引号（`unclosed string literal`）、
`toString()` 字符串未闭合、参数行内注释位置、`safe_ident` 小写化 `itemId`、
缺失类级 `@RequestMapping("/api/v1")`。

### 6.2 启动

```text
2026-10-12 22:44:28.900 INFO  o.f.c.i.command.DbValidate - Successfully validated 4 migrations
2026-10-12 22:44:28.911 INFO  o.f.c.i.command.DbMigrate - Schema "public" is up to date. No migration necessary.
2026-10-12 22:44:30.xxx INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8090 (http)
2026-10-12 22:44:30.xxx INFO  c.j.c.CampusTradeApplication - Started CampusTradeApplication in 3.177 seconds
```

---

## 7. 端点验证结果

### 7.1 宿主机 JVM 路径（端口 8090）

| 检查 | 结果 |
| --- | --- |
| `GET /actuator/health` | ✅ HTTP 200 |
| `GET /api/v1/health` | ✅ HTTP 200，统一包络：`{"code":"OK","message":"服务正常","data":"UP","traceId":"...","timestamp":"..."}` |
| `GET /api/v1/health/ready` | ✅ `database=UP`，`flyway.appliedCount=4`，`currentVersion=4`，`status=UP` |
| `GET /v3/api-docs` | ✅ **operations=83**，paths=73，tags=12 |
| `GET /swagger-ui.html` | ✅ HTTP 302 → `/swagger-ui/index.html` |
| `GET /swagger-ui/index.html` | ✅ HTTP 200 |
| `GET /openapi/openapi.yaml` | ✅ HTTP 200，264148 bytes（与源文件逐字节一致） |
| 业务接口（如 `GET /api/v1/trade-zones`） | ✅ HTTP 500（`UnsupportedOperationException`，证明已映射且未实现业务） |

### 7.2 容器化路径（`docker compose`，端口 8080）

```text
NAME              STATUS                   PORTS
jc2001-app        Up (healthy)             0.0.0.0:8080->8080/tcp
jc2001-postgres   Up (healthy)             0.0.0.0:55432->5432/tcp
```

| 检查 | 结果 |
| --- | --- |
| `GET /actuator/health` | ✅ HTTP 200 |
| `GET /api/v1/health` | ✅ 统一包络 |
| `GET /api/v1/health/ready` | ✅ `db=UP flyway=4/4 status=UP` |
| `GET /v3/api-docs` | ✅ **operations=83**，paths=73 |
| `GET /swagger-ui.html` | ✅ HTTP 302 → index |
| `GET /openapi/openapi.yaml` | ✅ HTTP 200，264148 bytes |
| Swagger 文档列表 | ✅ `['运行时文档（代码生成，83 个操作）', '迭代一契约（冻结候选 1.0.0-frozen-candidate）']` |
| `GET /api/v1/trade-zones` | ✅ HTTP 500（未实现） |
| `GET /api/v1/orders` | ✅ **HTTP 403**（`@PreAuthorize("isAuthenticated()")` 先生效——证明权限注解已工作） |
| 镜像构建耗时 | ✅ **10 秒**（复制 jar，无 Maven） |

### 7.3 Flyway（未修改迁移）

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

| version | description | success |
| --- | --- | --- |
| 1 | init schema | t |
| 2 | seed reference data | t |
| 3 | seed development data | t |
| 4 | fix review window index | t |

迁移脚本 mtime（均早于本步骤，**未修改**）：

```text
10-12 21:18  V2__seed_reference_data.sql
10-12 21:20  V3__seed_development_data.sql
10-12 21:23  V1__init_schema.sql
10-12 21:41  V4__fix_review_window_index.sql
```

### 7.4 Swagger UI 显示 83 个操作

`application.yml` 配置了两个文档入口，**默认加载运行时文档**：

```yaml
springdoc:
  swagger-ui:
    urls:
      - name: "迭代一契约（冻结候选 1.0.0-frozen-candidate）"
        url: /openapi/openapi.yaml
      - name: "运行时文档（代码生成，83 个操作）"
        url: /v3/api-docs
    urls-primary-name: "运行时文档（代码生成，83 个操作）"
```

| 项 | 值 |
| --- | --- |
| 默认展示 | 运行时文档（**83 个操作**） |
| 可切换对照 | 已冻结契约原文（83 个操作） |
| 自检端点 | 已用 `@Hidden` 从文档隐藏（`HealthController`、`FrozenSpecController`），确保文档操作数**恰好 83** |
| 页面可达 | ✅ `/swagger-ui.html` → 302 → `/swagger-ui/index.html`（200） |

> **验证方式**：`curl -s http://localhost:8080/v3/api-docs | python3 -c "...统计 operations..."` → **83**。
> 该数值即 Swagger UI 左栏列出的操作总数（共 12 个 tag 分组）。

---

## 8. 生成过程中发现并修复的缺陷（共 7 个）

| # | 缺陷 | 现象 | 修复 |
| --- | --- | --- | --- |
| 1 | f-string 花括号未转义 | 生成器 `SyntaxError` | 改为显式字符串拼接 |
| 2 | 共享参数 `$ref` 未解析 | 幂等参数全部丢失（0/0，应 16/16） | 解析 `components.parameters` |
| 3 | HTTP 头名含 `-` 用作 Java 标识符 | 非法 Java 标识符 | 新增 `safe_ident()` |
| 4 | description 未转义换行/双引号/反斜杠 | 大量 `unclosed string literal` 编译失败 | 加固 `esc()`（先转义反斜杠，再引号，再换行/制表） |
| 5 | 参数行内 `//` 注释位于逗号前 | `illegal start of expression` | 改为参数列表上方独立注释 |
| 6 | `toString()` 生成字符串未闭合 | `unclosed string literal` | 重写该生成块 |
| 7 | **生成器无条件删除手工 controller** | `HealthController`/`FrozenSpecController` 被删，`/api/v1/health` 与 `/openapi/openapi.yaml` 404 | 加入 KEEP 保留清单；恢复两个文件 |
| 8 | **Controller 缺类级基础路径** | 接口注册到 `/items` 而非 `/api/v1/items`，全部 404 | 加 `@RequestMapping("/api/v1")` |
| 9 | `PageResult<T>` 元素类型退化为 `Object` | 16 个分页接口类型信息丢失 | 从 `content.items.$ref` 反推元素类型 |

> 缺陷 7、8、9 是**只有实际启动并调用接口才能发现**的问题——若只做编译检查会全部漏过。

---

## 9. 未实现业务清单（下一步待办）

### 9.1 全部 83 个 operationId 均未实现

所有 Controller 方法体统一为：

```java
throw new UnsupportedOperationException("TODO: implement <operationId>");
```

所有 Service 实现方法同样抛 `UnsupportedOperationException`；**未注入任何 Repository、
未写事务逻辑、未访问数据库**（已用脚本核验：实现类中真实 `Repository`/`EntityManager`/`@Autowired`
引用数 = **0**）。

### 9.2 实现时必须遵守的约束（已写入 Service Javadoc 与 `package-info`）

| # | 约束 | 依据 |
| --- | --- | --- |
| 1 | 状态转换使用**条件更新或乐观锁**，禁止「先查后改」；受影响行数 0 → 409 + `details.currentStatus` | R-18 |
| 2 | **占用发生在下单时**（R-16）：写 `item_occupations` 与投影 `items.status` 必须**同一事务** | R-16 / DD-01 |
| 3 | 交换为**整包接受**（R-22）与**整笔作废**（R-24），不存在部分占用 | R-22 / R-24 |
| 4 | 订单占用（RESERVED）与交换占用（SWAPPING）**互斥**，由部分唯一索引保证 | R-23 / C-01 |
| 5 | 归属校验失败返回 **403**，不返回 404 | E-02 / AD-02 |
| 6 | 被处罚用户交易类写操作返回 **403** + `restrictedUntil` | E-03 |
| 7 | **管理员不参与交易**（P-00-A）：交易类接口须显式拒绝 `ROLE_ADMIN` | P-00-A |
| 8 | 部分唯一索引冲突（SQLState **23505**）映射为对应域 **409**，而非 500 | idempotency 文档 §7 |
| 9 | 违规下架需**联动**取消订单 / 作废交换 | R-35 |
| 10 | 幂等键（R-17）只是第一层；必须叠加条件更新（R-18） | R-17 / R-18 |
| 11 | 治理类接口 `reason` **必填** | R-34 |

### 9.3 尚未补齐的契约要素

| 项 | 现状 | 待办 |
| --- | --- | --- |
| 业务错误码 | `ErrorCode` 仅 7 个通用码 | 按 `error-codes.md` 补齐 93 个 `DOMAIN_REASON` 码 |
| `PageResult` 字段名 | 现为 `page/size/total/items` + 契约别名 `content/totalElements` | 统一为契约字段名（见 `common/PageResult.java` 的待确认说明） |
| 金额序列化 | 用 `String` 承载 `DecimalString` | 如需强类型，注册自定义序列化器（R-13） |
| Security 收紧 | 骨架放开模式（`anyRequest().permitAll()`） | 按 `api-permission-mapping.md` 收紧（TODO 已在 `SecurityConfig` 标注） |

---

## 10. 假设与待确认问题

### 10.1 本次生成引入的处理决策

| # | 决策 | 理由 |
| --- | --- | --- |
| 1 | **不使用 Lombok**，生成显式访问器 | `.m2` 未缓存 Lombok 且需联网；注解处理增加构建脆弱性。零新增依赖更稳 |
| 2 | DTO 分 `dto/request` 与 `dto/response`；**枚举与共享类型统一放 `response` 包** | 避免同名类型在两个包重复定义（如 `Category` 同时被请求与响应引用） |
| 3 | `PageResult` 放 `common` 包（非 `dto/response`） | 第四步已建立，保持包结构稳定（任务要求「不重复创建」） |
| 4 | Service 返回**原始领域类型**，由 Controller 包 `ApiResponse` | 响应包络属接口层职责，不下沉到 Service |
| 5 | `SwapController` 承载 tag 08+09 共 16 个操作 | 同属交换域；`@Tag` 与 operationId 仍为契约原值 |
| 6 | 自检端点加 `@Hidden` | 保证 `/v3/api-docs` 操作数**恰好 83**，符合验收标准 3/4 |
| 7 | 保留 `tools/generate_api_skeleton.py` 在工程内 | 契约变更时可**可重复再生成**，并作为 1:1 对齐的证据链 |

### 10.2 待确认问题（不阻塞本步骤）

| 编号 | 问题 | 当前处理 |
| --- | --- | --- |
| **AQ-01** | 通知开关是否纳入迭代一 | 已生成 `getMyNotificationSettings` / `updateMyNotificationSettings`（契约冻结即含） |
| **AQ-02** | `refreshToken` 是否启用 | 已生成 `POST /auth/refresh`，实现时返回 501（契约约定） |
| **AQ-03** | 字典是否需在线维护写接口 | 仅生成只读 `adminListDictionary`；写接口需走契约变更流程 |
| **AQ-04** | 同用户多条不同类型生效处罚是否允许 | 不影响骨架；实现时由 `uq_penalties_one_active` 与 Service 校验决定 |
| **Q-01/Q-02/Q-03** | 校区/社区/取货点真实数据 | 占位数据仍为 `CAMPUS_A` 等；**替换不改路径与 DTO 结构** |
| **Q-07** | 刷新令牌机制 | 契约保留 `refreshToken`，默认 501 |
| **Q-24** | 拆单失败语义（整批失败 / 部分成功） | 契约已冻结为默认「整批失败」（A-17）；实现时确认 |

---

## 11. 验收标准自检

| # | 验收标准 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | `mvn clean package -DskipTests` 成功 | ✅ | §6.1，BUILD SUCCESS（158 源文件） |
| 2 | 应用启动成功，`/actuator/health` 返回 UP | ✅ | §6.2、§7.1、§7.2 |
| 3 | `/v3/api-docs` 操作数量为 83 | ✅ | §7.1、§7.4，实测 **83**（宿主机与容器两路径） |
| 4 | operationId 与契约 1:1，无缺失/多余/重命名 | ✅ | §2，缺失 0 / 多余 0 / 不一致 0 |
| 5 | 每个方法有正确的 HTTP 方法、路径、请求/响应 DTO | ✅ | §2.2（方法/路径 0 不一致）；§5（DTO 99/99） |
| 6 | 权限注解与 `api-permission-mapping.md` 一致 | ✅ | §3（10/55/14/4 与契约 `x-required-roles` 一致） |
| 7 | 幂等占位 16 必填 + 16 可选 | ✅ | §4（**16 / 16**） |
| 8 | DTO 与 `components.schemas` 一致，不暴露敏感字段 | ✅ | §5（99/99；敏感字段 0） |
| 9 | Flyway V1~V4 未修改，容器内仍成功 | ✅ | §7.3（4 条 success；mtime 未变） |
| 10 | 未实现业务逻辑，Service 抛 `UnsupportedOperationException` | ✅ | §9.1（83 个方法 + 12 个实现全部抛异常；Repository 引用 0） |
| 11 | 未写前端代码，未修改迁移脚本 | ✅ | 无前端文件；迁移 mtime 未变 |
| 12 | 完成报告含各项统计与验证结果 | ✅ | 本文档 + 完成报告 |

---

## 12. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 后端接口骨架负责人 | 初稿：生成方式与产出统计、83/83 operationId 对账、权限与幂等核对、DTO 一致性、编译与启动、双路径端点验证、Flyway 未改核对、Swagger 展示 83 操作、9 个已修缺陷、未实现清单、假设与待确认、12 项验收自检 | 草稿（待确认） |
