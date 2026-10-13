# 迭代二 广义契约（Generalized Contracts）索引

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代二 广义契约 索引（README） |
| 版本 | **v1.1（2026-10-13 执行 P0 裁决后）** |
| 日期 | 2026-10-13 |
| 作者 | 技术契约负责人（待填写姓名）；**P0 裁决：技术契约裁决人（待填写姓名）** |
| 状态 | **冻结候选（FROZEN-CANDIDATE）** —— 9 条 P0 已裁决并固化 |
| 范围 | `docs/iteration2/contracts/`（**8 份文档**：GC-00 ~ GC-05 + GC-99 + GC-DL） |
| 上游依据 | `docs/iteration1/design/openapi.yaml`（冻结候选）、`docs/iteration1/design/api-freeze.md`、`docs/iteration1/`（需求与设计基线） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**

---

## 1. 为什么需要「广义契约」

迭代一已完成三件事：

| 已冻结 | 载体 | 规模 |
| --- | --- | --- |
| 接口**形状** | `docs/iteration1/design/openapi.yaml` | 73 paths / **83** operationId / **99** schema / **93** 错误码 |
| 业务**语义** | `rules.md`（34 条）、`state-machines.md`（6 大状态机）、`error-codes.md`、`idempotency-and-concurrency.md`、`permission-matrix.md`、`api-permission-mapping.md`、`schema.md`、`SystemDesign.md`、`sequence-diagrams.md` | — |
| 交付**冻结声明** | `docs/iteration1/FREEZE.md`、`design/api-freeze.md` | 冻结时刻 2026-10-09（新增接口）/ 2026-10-12（交付物） |

**但「形状之外」仍存在缺口** —— 这些缺口正是前后端并行开发时最容易产生分歧、返工与联调失败的地方：

| # | 缺口 | 典型问题 | 由哪份广义契约补齐 |
| --- | --- | --- | --- |
| 1 | **默认值与边界** | 不传 / 空值 / 空数组 / 0 / 超限时后端到底怎么做？ | **GC-01** |
| 2 | **数据语义** | 字段是省略还是返回 `null`？金额怎么传？枚举有哪些取值？ | **GC-02** |
| 3 | **安全与跨域** | 认证怎么做？CORS 允许什么？哪些字段不能返回？ | **GC-03** |
| 4 | **通知事件** | 什么事件发通知？通知长什么样？同步还是异步？ | **GC-04** |
| 5 | **Mock 与契约测试** | 前端怎么在没有后端时开发？怎么证明没跑偏？ | **GC-05** |

> 广义契约 = **接口形状之外、但直接影响前后端能否并行且不返工**的全部行为约定。

---

## 2. 文档清单与阅读顺序

### 2.1 清单

| 编号 | 文件 | 主题 | 页数量级 | 状态 |
| --- | --- | --- | --- | --- |
| **GC-00** | [`README.md`](./README.md)（本文件） | 索引、阅读顺序、更新机制、权威顺序 | 2 | **冻结候选（v1.1）** |
| **GC-01** | [`defaults-and-boundaries.md`](./defaults-and-boundaries.md) | 默认值、空值、空数组、0、超限时的后端行为（逐接口） | 3 | 草稿（待确认） |
| **GC-02** | [`data-conventions.md`](./data-conventions.md) | 空值策略、枚举字典、金额、时间、ID、分页排序、文件上传 | 3 | 草稿（待确认） |
| **GC-03** | [`security-and-cors.md`](./security-and-cors.md) | 认证、权限模型、数据范围、CORS、CSRF、脱敏、限流、审计 | 3 | 草稿（待确认） |
| **GC-04** | [`notification-events.md`](./notification-events.md) | 通知结构、25 个类型、触发矩阵、开关、同步/异步、前端轮询 | 3 | 草稿（待确认） |
| **GC-05** | [`mock-and-contract-testing.md`](./mock-and-contract-testing.md) | 工具链、Mock 覆盖、契约测试 6 维度、变更流程、联调计划与验收 | 3 | 草稿（待确认） |
| **GC-99** | [`open-questions.md`](./open-questions.md) | 本目录**全部**待确认问题（OQ）与登记的矛盾（CQ） | — | **冻结候选**（9 条 P0 已裁决；P1/P2 待裁决） |
| **GC-DL** | [`decision-log.md`](./decision-log.md) | **P0 裁决报告**：9 条 P0 的裁决结论、依据、落地与待跟进事项 | 2–3 | **已裁决并固化（DECIDED）** |

### 2.2 建议阅读顺序

```text
① README.md（本文件）          —— 5 分钟了解全貌与权威顺序
② data-conventions.md          —— 先统一「数据长什么样」，是所有约定的公共基础
③ defaults-and-boundaries.md   —— 再统一「边界怎么处理」，逐接口对照实现
④ security-and-cors.md         —— 前后端分工的安全边界，联调前必须达成一致
⑤ notification-events.md       —— 副作用与前端轮询（涉及迭代二之外的模块，可后读）
⑥ mock-and-contract-testing.md —— 开工第一天就按此搭建工程，避免后期返工
⑦ open-questions.md            —— 开工前评审这份清单，逐条裁决
⑧ decision-log.md              —— 9 条 P0 的裁决结论与依据（开工前必读）
```

### 2.3 按角色的最小必读集

| 角色 | 必读 | 选读 |
| --- | --- | --- |
| **前端负责人** | GC-02、GC-01、GC-05、GC-03、GC-04 | GC-99 |
| **后端负责人** | GC-01、GC-02、GC-03、GC-04 | GC-05、GC-99 |
| **测试** | GC-05 §6.4、GC-01、GC-03 §2 | 全部 |
| **接口契约负责人** | 全部（尤其 GC-99） | — |
| **需求方 / 评审** | 本 README §1/§2/§4、**GC-DL**、GC-99 | GC-01 §5 |

---

## 3. 权威顺序（**冲突时按此判定**）

```text
1. docs/iteration1/design/openapi.yaml          ← 机器可读，最终权威（已冻结）
2. docs/iteration1/design/api-freeze.md         ← 冻结范围与变更流程
3. docs/iteration1/design/error-codes.md        ← 错误码语义
4. docs/iteration1/design/api-permission-mapping.md ← 权限边界
5. docs/iteration1/design/idempotency-and-concurrency.md ← 幂等与并发
6. docs/iteration1/（其余：rules / state-machines / schema / SystemDesign / sequence-diagrams）
7. docs/iteration2/contracts/（本目录 5 份广义契约 + OQ 清单 + **P0 裁决报告 decision-log.md**）
8. ITERATION1_TO_ITERATION2_HANDOVER.md         ← 交接说明（描述性，非契约）
```

| 规则 | 说明 |
| --- | --- |
| R1 | 本目录**不得**与 `openapi.yaml` 冲突；冲突时**以 `openapi.yaml` 为准**，并**登记到 [`open-questions.md`](./open-questions.md)** |
| R2 | 本目录**不得**与 `rules.md` / `state-machines.md` / `error-codes.md` / `schema.md` 冲突；**发现矛盾只登记，不修改上游文档** |
| R3 | 本目录**不得**新增字段、枚举、错误码、接口；如需新增 → 走 `api-freeze.md` §4 变更流程 |
| R4 | 标注【待确认】的条目**不是契约**，前端**不得**依赖其具体取值（须等裁决） |

---

## 4. 状态与门槛

| 状态 | 含义 | 可否据此开发 |
| --- | --- | --- |
| **草稿** | 内容已写完，未评审 | 可作为**参考**；【待确认】项不可依赖 |
| **待确认** | 已提交评审，等待回答 OQ | 同上 |
| **冻结候选** | 已评审，OQ 已裁决 | ✅ 可据此开发 |
| **已冻结** | 已并入契约基线 | ✅ 唯一依据 |

**当前状态（2026-10-13 执行 P0 裁决后）**

| 文档 | 裁决前 | **裁决后** |
| --- | --- | --- |
| GC-00 `README.md`（本文件） | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-01 `defaults-and-boundaries.md` | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-02 `data-conventions.md` | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-03 `security-and-cors.md` | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-04 `notification-events.md` | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-05 `mock-and-contract-testing.md` | 草稿（待确认） | **冻结候选（v1.1）** |
| GC-99 `open-questions.md` | 草稿（待确认） | **冻结候选（v1.1）**（9 条 P0 已裁决；P1/P2 待裁决） |
| GC-DL `decision-log.md` | — | **已裁决并固化（DECIDED）** |

> **P0 已清零**：9 条 P0（OQ-01 / OQ-22 / OQ-23 / OQ-27 / OQ-33 / OQ-35 / OQ-37 / OQ-40 / OQ-41）**全部裁决并固化**，迭代二可无歧义开工。
> **开工时前端须同时执行两条过渡期容错**：① **字段可选**（OQ-22，`null` 字段被省略）；② **注册响应双兼容**（OQ-27 / CQ-11）。

升级到「**已冻结**」的门槛：

| # | 门槛 | 判定 |
| --- | --- | --- |
| 1 | `open-questions.md` 中的 **P0 问题全部裁决** | ✅ **已达成**（见 GC-99 §1.1 与 GC-DL） |
| 2 | P1 问题已裁决或有明确的临时默认值 | 见 GC-99 §1.2 |
| 3 | 与上游 4 份权威文档的**交叉引用一致性检查通过** | 见 §5.2 |
| 4 | 前后端负责人**共同签署** | — |

---

## 5. 更新机制

### 5.1 谁能改、怎么改

| 变更内容 | 提出方 | 审批 | 记录 |
| --- | --- | --- | --- |
| 修正错别字 / 补充示例 | 任何人 | 无 | 本文档 §6 版本记录 |
| 补充【待确认】项的建议默认值 | 任何人 | 接口契约负责人 | 版本记录 |
| 裁决一个 OQ（把【待确认】改为确定约定） | 接口契约负责人 | 需求方（P0）/ 接口负责人（P1/P2） | 版本记录 + **GC-99 状态更新** |
| 修改**已确定的约定**（语义变更） | 接口契约负责人 | 前后端负责人 | 版本记录 + 影响面评估 |
| 涉及 `openapi.yaml` 字段/枚举/接口 | 任何人 | **走 `api-freeze.md` §4 流程** | 变更台账（**不在本目录**） |

### 5.2 更新时的强制自检（**每次修改后必须跑**）

| # | 自检项 | 方法 |
| --- | --- | --- |
| 1 | 文档状态头（标题/版本/日期/作者/状态）齐全 | 人工 |
| 2 | 显式声明「不影响运行时代码与已有契约」 | `grep -c "不修改任何运行时代码"` |
| 3 | 引用的 `operationId` 均存在于 `openapi.yaml` | 脚本比对 |
| 4 | 引用的错误码均在 `error-codes.md` 中登记 | 脚本比对 |
| 5 | 引用的枚举取值均在 `openapi.yaml` 中 | 脚本比对 |
| 6 | 引用的规则编号（R-xx）/ 状态机编号（O-xx、S-xx）均存在 | 脚本比对 |
| 7 | 章节内的 OQ 编号在 GC-99 中**都有对应条目** | 双向 grep |
| 8 | **未修改** `src/`、`db/migration/`、`openapi.yaml`、`docker-compose.yml`、`pom.xml` | `git status --short` |

### 5.3 目录内的一致性维护约定

| 约定 | 说明 |
| --- | --- |
| **OQ 编号唯一且只增不改** | 已裁决的 OQ **不删除、不重用编号**，在 GC-99 中标记 `已裁决` 并写结论 |
| **CQ（矛盾）编号独立** | 上游文档之间的矛盾用 `CQ-xx`；**只登记，不修改上游** |
| **交叉引用用相对链接** | 目录内互引用用 `./xxx.md`，引用上游用 `../iteration1/...` |
| **不复制大段上游内容** | 引用上游时给链接 + 关键结论，避免双份维护导致漂移 |
| **表格优先** | 约定类内容一律用标准 Markdown 表格（便于评审逐行勾选） |

---

## 6. 本目录与迭代一的关系（**不修改清单**）

| 文件 / 目录 | 本目录是否修改 | 说明 |
| --- | --- | --- |
| `src/**`（全部 Java 文件） | ❌ **不修改** | 含 Controller / Service / DTO / Config |
| `src/main/resources/application*.yml` | ❌ **不修改** | 含 CORS、Jackson、分页配置 |
| `src/main/resources/db/migration/V1~V4` | ❌ **不修改** | 迁移脚本已哈希冻结 |
| `docs/iteration1/design/openapi.yaml` | ❌ **不修改** | 契约唯一真源（已冻结） |
| `docker-compose.yml` / `Dockerfile` / `Dockerfile.build` / `pom.xml` | ❌ **不修改** | 构建与部署 |
| `docs/iteration1/**`（全部既有文档） | ❌ **不修改** | 发现矛盾只登记到 GC-99 |
| `docs/iteration2/contracts/**` | ✅ **仅新增** | 本目录 7 份文档 |

---

## 7. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| **v1.1** | **2026-10-13** | **技术契约裁决人（待填写姓名）** | **同步 P0 裁决结果**：① 文档清单新增 **GC-DL [`decision-log.md`](./decision-log.md)**（P0 裁决报告）；② 阅读顺序新增第 ⑧ 步；③ 权威顺序第 7 条纳入裁决报告；④ **§4 状态门槛重写**为逐文档裁决前/后对照表，并明确 **9 条 P0 已清零**、升级「已冻结」门槛 #1 已达成；⑤ 新增两条**开工期前端过渡容错**提示 | **冻结候选（FROZEN-CANDIDATE）** |
| v1.0 | 2026-10-13 | 技术契约负责人（待填写姓名） | 初稿：广义契约缺口说明、7 份文档清单与阅读顺序（含按角色最小必读集）、权威顺序与 4 条规则、状态门槛、更新机制与 8 项强制自检、不修改清单 | 草稿（待确认） |

> **本文档基于迭代一冻结契约，用于迭代二及后续开发的前后端并行协调。本文档不修改任何运行时代码、数据库迁移脚本或已有接口契约。**
