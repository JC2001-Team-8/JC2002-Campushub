# 迭代三（切片 3）后端验收报告

| 项目 | 内容 |
| --- | --- |
| 切片 | 切片 3：购买与交换（3A 购买 / 3B 交换） |
| 分支 | `feat/slice3` |
| 交付人 | zcb0219（迭代三后端负责人 / 切片 3 后端主办） |
| 日期 | 2026-11-29 |
| 契约 | `docs/iteration1/design/openapi.yaml`（83 operationId，未修改） |
| 数据库 | PostgreSQL 15，Flyway V1~V5（V1~V4 未修改） |

---

## 1. 交付范围

切片 3 共 **30 个 operationId**：

| 模块 | operationId | 数量 |
| --- | --- | --- |
| Cart | `getCart` `addCartItem` `updateCartItem` `removeCartItem` | 4 |
| Order | `submitOrder` `listOrders` `getOrderDetail` `mockPayOrder` `cancelOrder` `selectPickupPoint` `markOrderDelivered` `confirmOrderReceipt` | 8 |
| Swap | `createSwapBundle` `listMySwapBundles` `getSwapBundle` `updateSwapBundle` `deleteSwapBundle` `createSwapProposal` `listSwapProposals` `getSwapProposal` `acceptSwapProposal` `rejectSwapProposal` `counterSwapProposal` `cancelSwapProposal` `getSwapCode` `verifySwapCode` `confirmSwap` `createSwapReview` | 16 |
| Internal | `runOrderTimeoutReleaseTask` `runSwapProposalExpireTask` | 2 |
| **合计** | | **30** |

> `runPenaltyAutoUnbanTask` / `runReviewWindowCloseTask` 属**切片 4**，本片未实现（文件中标注 `TODO(slice4)`）。

## 2. 对 2→3 交接文档 §5「验收检查表」的逐项结论

| # | 验收项 | 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 30 个 operationId 全部接入持久化服务，不返回占位异常 | ✅ | 切片 3 的 4 个 Controller / 4 个 ServiceImpl 中 `throw new UnsupportedOperationException` 为 0（切片 4 的 2 个除外） |
| 2 | 3A 单独演示「购物车→下单占用→模拟付款/取消/超时释放→送达→确认收货」 | ✅ | `Iteration3AcceptanceTest#purchaseFlowEndToEnd`、`#cancelOrderReleasesOccupation`、`#timeoutReleaseTask` |
| 3 | 并发请求验证同一商品不能同时进入两个有效订单/交换占用；付款与超时释放不能双重推进 | ✅ | `#concurrentSubmitOnlyOneWins`：5 线程 → 1×201 / 4×409，`item_occupations` 进行中记录恰好 1 条；付款与超时均用条件更新（`status='PENDING_PAYMENT' AND now()<expires_at` / `now()>=expires_at`）互斥 |
| 4 | 16 个带幂等要求的操作逐个验证 | ⚠️ 部分 | 已覆盖 `createItem`/`submitOrder`/`mockPayOrder`/`acceptSwapProposal`/`counterSwapProposal`/`createSwapReview`/`createSwapBundle`/`createSwapProposal` 的重放与同键异体；其余接口为「天然幂等」（覆盖式更新/条件更新），未逐条写重放用例 |
| 5 | 越权 403、未登录 401、缺失资源 404、状态冲突 409/422 | ✅ | `#authAndValidation` + 手工探针 `probe-edge.ps1`（20 项） |
| 6 | 取货点区域匹配、交换包数量边界、整包接受、唯一一次反提案、交换码一次性、72h 释放 | ✅ | `#purchaseFlowEndToEnd`（选点）、`#swapBundleRules`（1–5 件）、`#swapFlowEndToEnd`（整包接受/一次性核销）、`#swapCounterOnlyOnce`、`#swapExpireTask` |
| 7 | 新增 Flyway 迁移可在干净 PostgreSQL 顺序执行；保留 V1–V4 | ✅ | 本地空库与 CI（`postgres:15` service）各跑一次 V1→V5；V1~V4 的 `git diff` 为空 |
| 8 | 完成 3A 验收后再开始 3B；验收结果更新到本文件及进度材料 | ✅ | 提交顺序 `815d27d`(3A) → `61af342`(3B)，3A 验收通过后才开发 3B |

## 3. 交接清单 10 项（`HANDOVER-ITERATION1-TO-2.md` §6.2）

| # | 检查项 | 负责人 | 本片结论 |
| --- | --- | --- | --- |
| 1 | 本地全量构建通过 | 后端主办 | ✅ `mvn -B clean package` → BUILD SUCCESS |
| 2 | 应用可启动、健康检查通过 | 后端主办 | ✅ `/actuator/health` = 200（本地 + CI） |
| 3 | 交付标准逐项验证并留痕 | 后端主办 + 前端主办 | ✅ 后端部分见本文件 §2；**前端联调未开始** |
| 4 | 没有遗留的未通过测试或编译错误 | 后端主办 | ✅ `Tests run: 10, Failures: 0, Errors: 0`（本地 + CI） |
| 5 | 契约未被修改 | 接口契约负责人 | ✅ `openapi.yaml` LF-SHA256 前 16 位 = `36bd7a03a184734c`；V1~V4 未改 |
| 6 | 未触碰归属范围之外的文件 | 下一片负责人 | ✅ 改动集合 = 切片 3 的 Controller/ServiceImpl + `ItemService` 领域入口 + 公共错误码/异常处理 + CI/测试；未触碰切片 4 的 Admin/Report/Review/Notification 实现 |
| 7 | 提交信息规范、无 TODO 残留 | 后端主办 | ✅ 提交格式 `feat(slice3A|slice3B|...): ...`；本片 Controller 中「TODO 下一步：实现业务逻辑」已清零 |
| 8 | 已合并 main | 后端负责人 | ❌ **未完成**：分支已推送，PR 待评审后合并 |
| 9 | 下一片负责人已完成代码审查 | 下一片负责人 | ⬜ 待他人执行 |
| 10 | 口头交接已完成 | 后端主办 + 下一片负责人 | ⬜ 待他人执行 |

> 第 8、9、10 项依赖评审人与下一片负责人，非本片单方面可完成。

## 4. 证据

| 项 | 结果 |
| --- | --- |
| CI | run `46168889153` → `success`（JDK 11 + PostgreSQL 15） |
| 测试 | `Tests run: 10, Failures: 0, Errors: 0` |
| 手工探针 | `probe-3a`(26) / `probe-3b`(20) / `probe-3b2`(14) / `probe-edge`(20) 全通过 + `probe-race` 并发通过 |
| 构建产物 | `target/campus-trade.jar`（48 MB） |

## 5. 已知遗留

1. **未合并 main**（第 8 项）：待评审。
2. **前端联调未开始**：倒计时、取货点选择、交换码 UI 依赖前端主办。
3. **幂等用例未覆盖全部 16 个接口**（见 §2 第 4 项）。
4. **管理员路径、`RESTRICTED` 用户、评价窗口关闭、部分接受请求体**等分支无自动化用例。

## 6. 本片对 A-9 的裁决（后端负责人职责）

A-9：`.mvnrepo/` 已被 git 跟踪，`.gitignore` 未含 `.mvnrepo/`、`__pycache__/`、`.env`。

**裁决：本片不做破坏性清理**，理由：从索引移除 `.mvnrepo/` 会产生上千文件的删除提交，影响所有分支的合并；
本片仅补充 `.gitignore` 的 `data/`（本地上传目录）。`.mvnrepo/` 的清理建议由交付负责人统一决策后单独一次提交执行。
