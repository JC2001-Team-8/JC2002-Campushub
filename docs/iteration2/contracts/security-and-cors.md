# 迭代二 广义契约 ③：安全与跨域（Security & CORS）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 ③：安全与跨域 |
| 契约编号 | GC-03 |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— P0 项（CQ-04 / OQ-40）**已裁决并固化**；未裁决项以【待确认】标注 |
| 上游依据 | `docs/iteration1/design/api-permission-mapping.md`、`docs/iteration1/permission-matrix.md`、`docs/iteration1/design/error-codes.md`、`docs/iteration1/design/openapi.yaml`、`docs/iteration1/rules.md`（R-01/R-33/R-34）、`docs/iteration1/design/api-open-questions.md`（AQ-01/AQ-02/AQ-16） |
| 适用迭代 | 迭代二及后续 |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
>
> **定位**：定义前后端在**安全边界**上的分工与约定：怎么认证、谁能调什么、跨域怎么配、哪些字段不能出现在响应里、以及治理动作的审计要求。
>
> **重要前提（如实登记）**：迭代一交付的 `SecurityConfig` 处于**骨架放开模式**（`anyRequest().permitAll()`，仅公开路径 `permitAll`，其余权限收敛规则以 TODO 注释保留）。**迭代二必须先收紧权限**，否则本文档 §3、§4 的边界在运行时不被强制。此为**实现任务**，不是契约变更；契约侧（`openapi.yaml` 的 `x-required-roles` 与 `api-permission-mapping.md`）**已冻结且无需修改**。

---

## 1. 认证方式（Authentication）

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 方案 | **无状态 Bearer JWT** | `bearerAuth` |
| 请求头 | `Authorization: Bearer <accessToken>` | `openapi.yaml` |
| 令牌载荷承诺 | 至少含用户 ID（`sub`）、角色（`role`）、签发时间、到期时间 | 实现细节 |
| **有效期** | **7 天**；`expiresIn = 604800`（秒），`expiresAt` 为绝对到期时间 | A-06 / R-01 |
| 令牌类型 | `tokenType` 恒为 `"Bearer"` | `AuthTokenResponse` |
| 签发时机 | `login` 成功；**`registerUser` 成功亦直接签发令牌（注册即登录）** —— **OQ-27 已裁决**（详见 [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) §2.1.1）。**过渡期前端须双兼容**：响应含 `accessToken` 则直接登录，否则回退调用 `login`（**CQ-11**） | — |
| 过期/无效 | `401 AUTH_TOKEN_EXPIRED`（过期）/ `401 AUTH_UNAUTHENTICATED`（未带或无效） | E-01 |
| 凭据错误 | `401 AUTH_INVALID_CREDENTIALS`，**不区分账号是否存在**（防枚举） | R-01 |
| 退出 | `logout` **不使令牌失效**（无状态 JWT，服务端不维护黑名单）；前端**必须**自行清除本地令牌 | FR-104 |
| 存储位置 | **建议**：`accessToken` 存内存（Pinia/状态管理）+ 可选 `localStorage`（页面刷新后恢复）；**禁止**把令牌写入 URL、日志、`document.cookie` 明文（见 §7） | — |
| 令牌续期 | **不启用**：`refreshToken` 默认返回 **`501 COMMON_NOT_IMPLEMENTED`**（AQ-02 / Q-07），`AuthTokenResponse.refreshToken` 为 `null` | AQ-02 |
| 过期前端策略 | 收到 `401 AUTH_TOKEN_EXPIRED` / `AUTH_UNAUTHENTICATED` → **清空本地令牌 + 跳登录页 + 记住来源页**（`error-codes.md` §5 约定 2） | — |
| `AUTH_INVALID_CREDENTIALS` 例外 | 属**登录表单错误**，**不清空令牌**，仅提示；**保留已填用户名** | `error-codes.md` §3.3 |

### 1.1 内部接口凭证（`X-Internal-Token`）

| 项 | 约定 |
| --- | --- |
| 适用范围 | **仅** `/internal/**` 4 个定时任务接口（`x-required-roles: [SYSTEM]`、`x-internal: true`） |
| 凭证类型 | `apiKey`，请求头 `X-Internal-Token`（**不是用户 JWT**） |
| 与用户 JWT 的关系 | 携带用户令牌调用 `/internal/**` **一律拒绝**；持有有效内部令牌视为 `ROLE_SYSTEM` |
| 前端红线 | **绝对禁止**把 `X-Internal-Token` 下发到浏览器端、前端产物、`.env` 前端文件或日志中 |
| 网关要求 | 生产环境**必须**在网关/反向代理层禁止外部访问 `/internal/` 前缀（GW-01/GW-02）；外部请求返回 404 或 403 且不暴露前缀存在性 |

---

## 2. 权限模型（Authorization Model）

### 2.1 七类主体

| 主体 | 判定 | 说明 |
| --- | --- | --- |
| 访客（GUEST） | 未携带令牌 | 仅只读浏览 |
| 注册用户（USER） | `role=USER` 且 `status=ACTIVE` | 所有个人能力的基础 |
| 卖家 / 买家 | **关系角色**（由资源归属 / 交易参与方推导） | **不是注册身份** |
| 管理员（ADMIN） | `role=ADMIN` | **不参与交易**（P-00-A） |
| 被处罚用户（RESTRICTED） | `status=RESTRICTED` | 交易写操作被禁，但可登录、浏览、申诉 |
| 系统（SYSTEM） | 有效 `X-Internal-Token` | 定时任务，无人工入口 |

### 2.2 逐条统计（引用 `api-permission-mapping.md` §3.8）

| 口径 | 数量 | 备注 |
| --- | --- | --- |
| 公开（`security: []`） | **10** | `registerUser`、`login`、`refreshToken`、`getPublicProfile`、`listTradeZones`、`listPickupPointsByZone`、`listCategories`、`listItemConditions`、`listItems`、`getItemDetail` |
| 需登录（JWT，含管理员） | **73** | — |
| 仅管理员（`x-required-roles: [ADMIN]`） | **14** | 全部为 `admin*` 前缀操作 |
| 系统内部（`x-required-roles: [SYSTEM]`） | **4** | `/internal/tasks/**` |
| **业务操作合计** | **83** | 恒等式：`公开 10 + 需登录 73 = 83`；`仅管理员 14 ⊆ 需登录 73` |

> **口径提示**：`api-contract.md` §1 曾出现「仅管理员 15」的口径（14 个 `admin*` + `getSwapBundle` 的 `[USER, ADMIN]` 只读旁路）。
> `api-permission-mapping.md` §5.1 已说明**以 `openapi.yaml` 为准 = 14**。本文档采用**口径 A（14）**。

#### 2.2.1 **契约层与注解层双口径 —— 已裁决（CQ-04）**

> **裁决（2026-10-13）**：**两者都正确，属于不同抽象层级的计数，不构成矛盾。**

> **契约层面统计为 10 公开 / 73 需登录 / 14 管理员 / 4 系统；
> 注解层面统计为 10 `permitAll` / 55 `isAuthenticated` / 14 `hasRole('ADMIN')` / 4 `hasRole('SYSTEM')`。
> 差异原因在于部分接口通过路径匹配而非 `@PreAuthorize` 实现认证，两者均正确，不构成矛盾。**

| 抽象层级 | 载体 | 公开/免认证 | 需登录 | 仅管理员 | 系统 |
| --- | --- | --- | --- | --- | --- |
| **契约层**（**权威**） | `openapi.yaml` 的 `security` + `x-required-roles`；`api-permission-mapping.md` §3.8 | **10** | **73** | **14** | **4** |
| **注解层**（实现视角） | Controller 的 `@PreAuthorize` / `SecurityConfig` 路径规则；`handover` §3.3 | **10** | **55** | **14** | **4** |

**差异根因（必须理解，否则会误判为缺陷）**

| # | 根因 |
| --- | --- |
| 1 | `55` 与 `73` 的差额（**18 个操作**）在于：部分接口的认证**由路径匹配规则**（`SecurityConfig` / `PUBLIC_ENDPOINTS` / 网关）或**本人校验**（Service 层归属判断，如 `AUTH_NOT_RESOURCE_OWNER`）承担，**未**使用 `isAuthenticated` 表达式 |
| 2 | 两个层级统计的**对象不同**：契约层统计"**契约要求**"，注解层统计"**当前骨架写法**" |
| 3 | **不构成矛盾**：`14` 管理员与 `4` 系统两个数字**完全一致**；`10` 公开也**完全一致**，仅"需登录"的**表达方式**不同 |

> **实现约束**：迭代二收紧权限时，**必须以契约层（10/73/14/4）为对齐目标**，逐条落实 `api-permission-mapping.md` §2（7 主体 × 6 域）与 §1.2 判定顺序；**不得**以"注解层已有 55 个 `isAuthenticated`"为由认为权限已完备（`SecurityConfig` 当前仍为**骨架放开模式** `anyRequest().permitAll()`）。

### 2.3 判定顺序（**不可交换**）

```text
① 端点是否公开（security: []）→ 是则放行
② 是否携带有效令牌                        → 否则 401
③ 管理员交易拦截（P-00-A）                → 否则 403 AUTH_FORBIDDEN_TRADING_AS_ADMIN
④ 被处罚用户禁写（R-33 白名单）           → 否则 403 USER_RESTRICTED_CANNOT_WRITE（带 restrictedUntil）
⑤ 角色校验（ADMIN / SYSTEM）              → 否则 403 AUTH_ADMIN_REQUIRED
⑥ 归属 / 参与方校验（P-00-B）             → 否则 403 AUTH_NOT_RESOURCE_OWNER
⑦ 资源存在性                              → 否则 404
⑧ 状态 / 业务规则 / 并发                  → 409 / 422 / 400
```

> **为什么顺序固定**：顺序错误会产生三类安全问题 —— ① 用 404 泄露资源存在性；② 处罚用户通过归属校验后写入；③ 管理员绕过交易限制。原文见 `api-permission-mapping.md` §1.2。

### 2.4 三条硬性边界

| 编号 | 边界 | 约定 |
| --- | --- | --- |
| **P-00-A** | **管理员不参与交易** | `ROLE_ADMIN` 对任何发布/编辑商品、加购物车、下单、付款、交付、确认收货、评价、创建交换包、发起/接受/拒绝/反提案/核销/确认交换**一律拒绝**，`403 AUTH_FORBIDDEN_TRADING_AS_ADMIN`。管理员对交易资源仅保留**治理只读**（`getItemDetail`、`getOrderDetail`、`getSwapBundle`、`getSwapProposal`、`getItemOccupation`）+ 治理写操作 |
| **P-00-B** | **越权返回 403，不用 404** | 非本人资源一律 `403 AUTH_NOT_RESOURCE_OWNER`（商品域别名 `403 ITEM_NOT_OWN_SELLER`），**不得**用 404 掩盖越权 |
| **P-00-C** | **被处罚用户必须能登录** | 否则无法申诉；`login` 对其放行，响应 `status=RESTRICTED` + `restrictedUntil` |

### 2.5 数据范围（Row-level Scope）

| 主体 | 可见 / 可写范围 |
| --- | --- |
| 访客 | 仅 `IN_SALE` 商品、字典、公开用户资料（`getPublicProfile`） |
| 注册用户 | **本人**资源（自有商品、本人购物车、参与的订单/交换/举报/申诉/通知） |
| 卖家 | 自有商品上的读写；参与订单上的交付操作 |
| 买家 | 本人订单上的付款/取消/选点/确认收货；参与交换上的操作 |
| 管理员 | **治理只读**（订单/交换/商品/处罚/举报/申诉/审计）+ 治理写操作；**不得**经 `/orders`、`/swap-proposals` 等用户侧集合接口浏览全量数据 |
| 被处罚用户 | 仅 `permission-matrix.md` §8 白名单（登录、浏览、查看自己的订单/交换/处罚、提交申诉、勾选交易区域、举报、下架自有商品、改密码、通知相关） |
| 系统 | 仅定时任务范围；**不得**修改用户资料、删除数据、绕过状态机、跳过审计 |

### 2.6 归属校验的具体含义（逐类）

| 资源 | 「本人」的确切含义 | 反例 |
| --- | --- | --- |
| 商品 | `item.sellerId == token.userId` | 用户 A 编辑/下架用户 B 的商品 |
| 订单 | `buyerId`（付款/取消/选点/确认收货）或 `sellerId`（交付）**或**参与方（读取） | 第三方读取/操作他人订单 |
| 交换包 | `ownerId == token.userId` | 第三方读取/编辑/删除 |
| 交换提议 | 发起方（取消）或目标方（接受/拒绝/反提案）或**双方**（读取交换码/核销/确认） | 发起方自接受、第三方核销 |
| 举报 | `reporterId == token.userId` **或**管理员 | 他人读取该举报 |
| 申诉 | `appellantId == token.userId` | 他人查看/提交 |
| 通知 | `recipientId == token.userId` | 读取/标记他人通知 |

> **完整 45 行清单**见 `api-permission-mapping.md` §4（「必须本人的接口清单」），本文档不重复。

---

## 3. 跨域（CORS）

### 3.1 当前**实际**配置（迭代一骨架，如实登记）

| 项 | `application.yml` 默认 | `application-dev.yml` 覆盖 | 代码默认值 |
| --- | --- | --- | --- |
| `enabled` | `true` | `true` | `true` |
| `allowed-origins` | `http://localhost:5173,http://localhost:3000` | `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173` | `http://localhost:5173` |
| `allowed-methods` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | 同默认 | 同默认 |
| `allowed-headers` | **`*`（通配）** | 同默认 | `*` |
| `allow-credentials` | **`false`** | 同默认 | `false` |
| `max-age` | `3600` | 同默认 | `3600` |
| 暴露的响应头 | — | — | **`X-Trace-Id`**（`SecurityConfig.corsConfigurationSource()` 显式 `addExposedHeader`） |
| 环境变量 | `APP_CORS_ENABLED`、`APP_CORS_ALLOWED_ORIGINS` | — | — |

### 3.2 本文档的**约定值**（前端可依赖）

| 项 | 约定 | 说明 |
| --- | --- | --- |
| 允许的来源（开发） | `http://localhost:5173`（Vite 默认）、`http://127.0.0.1:5173`、`http://localhost:3000` | 与 `application-dev.yml` 一致 |
| 允许的来源（生产/预发） | **【待确认 OQ-28】**：正式前端域名（迭代一未交付生产部署，OOS-17） | **禁止**在生产使用 `*` 通配来源 |
| 允许的方法 | `GET`、`POST`、`PUT`、`PATCH`、`DELETE`、`OPTIONS` | 与契约的 HTTP 方法集合一致（**注意含 `PATCH`**，购物车条目更新用 `PATCH`） |
| **允许的请求头（约定白名单）** | `Authorization`、`Content-Type`、**`Idempotency-Key`**、`X-Trace-Id`、`X-Internal-Token`（仅内部环境） | 见 §3.3 |
| 允许携带凭证 | **`false`**（不使用 Cookie；令牌走 `Authorization` 头） | 与无状态 JWT 一致 |
| 暴露的响应头 | **`X-Trace-Id`**；建议追加 **`Retry-After`**（限流时前端需要读取）→ **【待确认 OQ-29】** | `X-Trace-Id` 已实现 |
| 预检缓存 | `Access-Control-Max-Age: 3600`（1 小时） | 减少 OPTIONS 往返 |
| 预检放行 | `OPTIONS /**` 必须 `permitAll`（不得要求认证） | 骨架已实现 |

### 3.3 允许请求头：约定值与现状的差异（**重点**）

| 项 | 现状 | 本文档约定 | 处置 |
| --- | --- | --- | --- |
| `allowed-headers` | **`*`**（`application.yml` / `AppProperties`） | **显式白名单**：`Authorization, Content-Type, Idempotency-Key, X-Trace-Id` | **【待确认 OQ-30】**：是否把 `*` 收窄为白名单 |

> **说明**：当前 `*` 通配**功能上可用**（含 `Idempotency-Key`），但**安全性偏松**。
> 收窄为白名单属 `application.yml` 的**配置变更**（**不在本次任务范围内**），本文档仅**登记差异与建议**。
> **前端不受影响**：两种情况都允许发送 `Authorization` / `Content-Type` / `Idempotency-Key`。

### 3.4 前端 CORS 相关约定

| # | 约定 |
| --- | --- |
| 1 | **必须**在跨域请求中携带 `Authorization` 头（认证接口除外）；**不需要** `credentials: 'include'`（不使用 Cookie） |
| 2 | 发送 `Authorization` 或 `Idempotency-Key` 会触发 **预检（OPTIONS）**；前端必须能容忍预检失败并给出可读提示 |
| 3 | 读取 `traceId` **优先从响应体**（`ErrorResponse.traceId`）取；需要时再从响应头 `X-Trace-Id` 取（已在暴露白名单中） |
| 4 | **禁止**在前端把 `baseURL` 写成通配或依赖代理绕过 CORS；开发环境推荐 **Vite `server.proxy`** 或直连 `http://localhost:8080/api/v1` 二者之一，**联调前统一约定**（见 [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) §6） |
| 5 | 若使用 Vite 代理，**浏览器侧不发跨域请求**，CORS 配置不生效；此时应确认后端 CORS 配置**仅用于直连场景** |
| 6 | 生产环境**必须**使用 HTTPS；令牌**不得**经明文 HTTP 传输 |

---

## 4. CSRF（跨站请求伪造）

| 项 | 约定 | 理由 |
| --- | --- | --- |
| 是否需要 CSRF Token | **不需要** | ① 认证走 `Authorization` 头而非 Cookie，浏览器不会自动附带；② 服务端为**无状态**（`SessionCreationPolicy.STATELESS`），不创建 `HttpSession`；③ `SecurityConfig` 已 `csrf().disable()` |
| 前端动作 | **无需**携带 CSRF Token，**无需**读取 CSRF Cookie | — |
| 前提条件（必须保持） | **禁止**改为基于 Cookie 的会话认证；若未来引入 Cookie，则**必须**同时启用 CSRF 防护（走契约变更流程） | — |
| 其他 CSRF 缓解 | 严格 CORS 来源白名单（§3.2，禁止通配）；`Content-Type: application/json` 强制（表单类型跨域请求会被预检拦截） | — |

---

## 5. 脱敏字段清单（Data Masking）

### 5.1 **绝不允许**出现在任何响应中的字段

| 字段 | 来源 | 理由 | 依据 |
| --- | --- | --- | --- |
| `password_hash` | `users.password_hash` | 口令摘要泄露风险 | AD-08 |
| `deleted_at` | `items.deleted_at`、`swap_bundles.deleted_at` 等 | 软删除属内部实现，暴露会泄露删除行为 | AD-08 |
| `created_by` | 各表审计字段 | 内部审计字段 | AD-08 |
| `updated_by` | 各表审计字段 | 内部审计字段 | AD-08 |
| `request_hash` | `idempotency_records.request_hash` | 幂等内部实现 | AD-08 |
| `internal_token` / `X-Internal-Token` | 配置 | 内部凭证 | `api-permission-mapping.md` §6.1 |

> **注意**：`created_at` / `updated_at` **不在**脱敏清单中 —— 它们以业务时间字段（`createdAt` / `updatedAt`）形式**正常返回**（如 `ItemDetail.updatedAt`）。

### 5.2 需**按权限**限制的字段

| 字段 | 限制 | 说明 |
| --- | --- | --- |
| `ItemDetail.bannedReason` | 仅 `status=BANNED` **且有权限查看时**返回 | R-34 留痕；对普通访客/买家应返回 `null` |
| `AuditLog.*` | **仅管理员**可读 | R-34 可见性 |
| `Penalty.*` 全量 | 全量处罚记录**仅管理员**；用户仅可见**自己的**记录 | R-32 可见性 |
| `SwapCode.code` | **仅提议双方**可见；**禁止**出现在任何公开列表接口 | R-26 安全要求 |
| `Report.reporter` | 举报人是**管理员可见**；被举报方**不返回**举报人身份 | 治理需要 |
| `AuthTokenResponse.accessToken` | 仅返回给**令牌所有者本人**；**禁止**写入日志 | — |
| 联系方式类字段 | **全库无此字段**（`UserProfile` 不含手机号/微信号/住址/定位） | AQ-12 / OOS-08 |

### 5.3 前端约定

| # | 约定 |
| --- | --- |
| 1 | **禁止**在前端请求任何脱敏字段（不存在的字段请求也无意义） |
| 2 | **禁止**在前端 `localStorage` / `sessionStorage` / Pinia 持久化中保存 `accessToken` 以外的敏感数据；`accessToken` 也必须可随时清除 |
| 3 | **禁止**把完整响应体（含 `traceId`、令牌）打印到生产控制台 |
| 4 | 日志上报/埋点**必须**剔除 `accessToken`、`Authorization` 头、完整用户输入口令 |

---

## 6. 限流与审计

### 6.1 限流（Rate Limiting）

| 项 | 约定 | 状态 |
| --- | --- | --- |
| 是否实现 | **建议迭代二暂不实现**（无网关、单实例、无真实流量压力） | **【待确认 OQ-31】** |
| 契约预留 | 错误码 `COMMON_RATE_LIMITED`（**429**）+ 响应头 `Retry-After` **已在契约中定义** | 契约已冻结 |
| 重点接口（若实现） | `login`、`registerUser` 等敏感接口优先 | `openapi.yaml` 全局约定句 |
| 前端要求 | **必须**预留 429 处理分支：读取 `Retry-After`（秒）或 `details.retryAfterSeconds`，**倒计时后重试**，**不得**立即无限重试 | `error-codes.md` §3.1 |
| 若启用 | 需追加暴露 `Retry-After` 响应头（见 §3.2 / OQ-29） | — |

### 6.2 审计日志（Audit Log）—— **必须实现，不可省略**

| 项 | 约定 | 依据 |
| --- | --- | --- |
| 必须写审计的治理动作 | **下架商品**（`adminTakedownItem`）、**恢复商品**（`adminRestoreItem`）、**封禁**（`adminBanUser`）、**解封**（`adminLiftPenalty`）、**处理申诉**（`adminResolveAppeal`）、**处理举报**（`adminHandleReport`）；**建议**同时记录 `adminSearchUsers` | **R-34** |
| **`reason` 必填** | 上述治理接口的 `reason` / `note` **为必填**；缺失或仅空白 → `400 VALIDATION_REASON_REQUIRED` | R-34 |
| 审计记录内容 | `actorId`（系统任务为 `null`）、`actorRole`、`action`、`targetType`、`targetId`、`reason`、`beforeState`、`afterState`、`createdAt` | `AuditLog` |
| 写入语义 | **追加写（append-only），不可修改、不可删除** | `AuditLog` |
| 幂等 | 重复请求必须被**幂等键**或**状态校验**拦截，**不得**产生重复审计噪音 | R-34 |
| 权限 | 审计日志**仅管理员**可读（`adminListAuditLogs`） | R-34 |
| 系统任务 | 定时任务同样需写审计/通知，但**不得**修改用户资料、删除数据、绕过状态机、跳过审计 | `api-permission-mapping.md` §6.1 |
| 导出 | **不做**审计导出（Q-32 / AQ-13） | — |

### 6.3 其他安全约定

| 项 | 约定 |
| --- | --- |
| 口令存储 | **BCrypt**（`SecurityConfig.passwordEncoder()`）；**禁止**明文或可逆存储 |
| 口令强度 | 至少 8 位；强度细则与前端校验强度 **【待确认 OQ-32】** |
| 账号枚举 | 登录失败统一 `401 AUTH_INVALID_CREDENTIALS`；注册重名返回 `409 AUTH_USERNAME_TAKEN`（**重名必然暴露，属可接受**） |
| 越权测试 | 迭代二**必须**按 `api-permission-mapping.md` §8 的 6 组用例做越权测试（归属类、交换角色、管理员边界、被处罚用户、认证与令牌、通用断言） |
| 敏感操作二次确认 | 治理动作（封禁/下架）前端**必须**二次确认并强制填写原因 |

### 6.4 联调测试账号 —— **已裁决（OQ-40）**

> **裁决（2026-10-13）**：**使用 V3 种子数据中的占位账号，不新增账号。**

> **联调测试账号暂使用 V3 种子数据中的 `admin@example.com`（管理员）和 3 个普通用户账号。
> 密码为占位密码，待 Q-04 确认后再替换为真实账号体系。V3 脚本禁止在生产环境执行。**

| 用户名 | 邮箱 | 角色 | 安全用途 |
| --- | --- | --- | --- |
| **`admin`** | **`admin@example.com`** | `ADMIN` | 验证治理权限（`AUTH_ADMIN_REQUIRED` 反例）与治理动作审计 |
| `alice` | `alice@example.com` | `USER` | 资源所有者（卖家） |
| `bob` | `bob@example.com` | `USER` | 交易参与方（买家） |
| `carol` | `carol@example.com` | `USER` | **第三方**：验证 `403 AUTH_NOT_RESOURCE_OWNER`（**不得**返回 404） |

| 安全约束 | 约定 |
| --- | --- |
| 账号数量 | **1 管理员 + 3 普通用户 = 4 个**；**不新增账号** |
| 被处罚用户 | **不预置**：由 `admin` 现场封禁 `carol` 产生，用于验证 `403 USER_RESTRICTED_CANNOT_WRITE` + `restrictedUntil` |
| 密码 | **占位密码**；**Q-04 确认后替换为真实账号体系** |
| **环境红线** | **V3 脚本禁止在生产环境执行**（脚本头部已声明）；生产环境的账号初始化方案待 Q-04 |
| 数据红线 | **禁止**把生产/真实用户数据用于联调 |
| 详细清单与阻塞项 | 见 [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) §6.2（含 **IMP-02**：占位 bcrypt 哈希非法、当前账号无法登录） |

---

## 7. 前端安全清单（可直接作为 PR 检查项）

| # | 检查项 |
| --- | --- |
| 1 | 令牌只存内存 + 可选 `localStorage`；**不存** Cookie 明文、不写 URL |
| 2 | 401 → 清令牌跳登录并记住来源页（`AUTH_INVALID_CREDENTIALS` 除外） |
| 3 | 403 → 提示无权限并返回上一页；`USER_RESTRICTED_CANNOT_WRITE` 需展示 `restrictedUntil` + 申诉入口 |
| 4 | 所有写操作按契约携带 `Idempotency-Key`（必填的 16 个接口**必须**带；重试**复用同一键**） |
| 5 | 错误处理**按 `code` 分支**，不匹配 `message`；提示中保留 `traceId` |
| 6 | 不渲染未转义的用户内容（`title`、`description`、`content`、`detail`、`reason`、`remark`、`note`） |
| 7 | 不请求、不缓存、不展示 §5.1 的脱敏字段 |
| 8 | 管理员与被处罚用户的 UI 能力**按权限隐藏**，但**不得**只依赖前端隐藏（后端必须拦截） |
| 9 | 交换码、内部令牌**禁止**出现在日志、埋点、错误上报中 |
| 10 | 生产环境强制 HTTPS；不在前端产物中出现任何内部凭证 |

---

## 8. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **执行 P0 裁决并固化**：① **CQ-04 已裁决** —— 新增 §2.2.1「契约层与注解层双口径」，明确 **契约层 10/73/14/4 与注解层 10/55/14/4 均正确、属不同抽象层级、不构成矛盾**，并给出差异根因与"以契约层为对齐目标"的实现约束；② **OQ-40 已裁决** —— 新增 §6.4「联调测试账号」，使用 **V3 种子占位账号**（`admin@example.com` + `alice`/`bob`/`carol`，共 4 个，不新增），密码为占位密码待 Q-04 替换，**V3 禁止在生产执行** | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：Bearer JWT 认证（7 天 / 604800）、`refreshToken`=501、内部令牌边界、权限模型与 10/73/14/4 统计、P-00-A/B/C 三条硬边界、数据范围与归属语义、CORS 现状与约定（含请求头差异）、CSRF 结论、脱敏字段清单、限流建议、审计强制要求、前端安全清单、6 条待确认问题 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
