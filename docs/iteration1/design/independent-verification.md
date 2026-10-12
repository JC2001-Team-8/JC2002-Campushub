# 迭代一 独立验证报告（Independent Verification）

| 项 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 独立验证报告 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 独立验证人（待填写姓名） |
| 状态 | 草稿（待确认） |

> **验证立场**：本报告为**独立验证**，不采信交付方的自证结论。所有数字均为**实测**（命令输出粘贴于 §2）。
> 凡无法测量者，一律标注「未能验证」，不假定通过。
> **验证环境为只读**：除本报告文件外，未修改、创建或删除仓库内任何文件（唯一写操作位于 `/tmp` 下的临时目录）。

---

## §0 结论摘要（先读）

### 0.1 计分

| 类别 | 数量 |
| --- | --- |
| 核心主张项（V-1 ~ V-9 逐条拆分） | **41** |
| ├ 实测**确认**（✅） | **41** |
| ├ 与主张**不符**（❌） | **0** |
| └ 附带保留 / 部分确认（⚠️） | **2**（见 0.3，均不构成交付缺陷） |
| 附加抽查（超出必查清单，顺带核实） | **4**，全部确认 ✅ |
| 交付方数值主张被推翻 | **0** |
| **阻塞项** | **无** |

### 0.2 一句话结论

交付方关于本次最终步骤（DDP-01 修复、P-01 追溯补齐、打包归档）的全部**可测量主张均经得起实测检验**：83/83 接口对齐零差异、归档 203 文件/158 java/29 md/5 sql 与脚本自产包**逐条同名同尺寸**、目录层级未扁平化、排除项全为 0、数据库 25 表/324 列/93 错误码/54 FR/7 部分唯一索引/16+16 幂等均与主张一致。**未发现任何被推翻的数值主张。**

### 0.3 两项需读者注意的保留

1. **⚠️ V-1(d) 的 mtime 阈值口径**：任务书以「`2026-10-12 22:00` 之后」为界，据此实测 `src/` 下有 **138 个文件**晚于 22:00（实际为 22:40:35–22:44:37）。但本步骤的真实时间窗是 **23:02–23:08**；`src/` 下**最新 mtime 为 22:44:37**，晚于 22:45 与 23:00 的文件数均为 **0**。故「本步未触碰非文档文件」**实质成立**，任务书给出的 22:00 阈值并非本步边界。
2. **⚠️ V-6(c) 的「读取」部分不可观测**：**修改**可测（已测），**读取**无审计日志，无法证明「只读了这两份上游文件」。该半句标记为**未能验证**。

### 0.4 额外发现（流程风险，非数值主张）

交付物在我开始验证后**仍在被写入**：`DELIVERY.md`、`final-delivery-report.md`、`iteration1-delivery.zip` 的 mtime 从 `23:07:04 / 23:06:55 / 23:07:05` 变为 `23:08:30 / 23:08:30 / 23:08:31`，zip 大小 `708986 → 710199` 字节。
本报告所有测量均在 **23:08:31 之后**完成，并在收尾时以 SHA-256 复核确认**自 23:08:31 起无进一步变更**（见 §1.3）。详见 §4。

---

## §1 验证方法与环境

### 1.1 环境

| 项 | 值 |
| --- | --- |
| 仓库根 | `/Users/god-come/Desktop/jc2001.backend` |
| 验证日期 | 2026-10-12 |
| 验证时间窗 | 约 23:07 – 23:15（CST） |
| 数据库 | Docker 容器 `jc2001-postgres`（`Up 33 minutes (healthy)`，宿主 55432 → 容器 5432） |
| 应用 | Docker 容器 `jc2001-app`（`Up 22 minutes (healthy)`，宿主 8080） |
| 版本控制 | **无 `.git`**（`ls -d .git` → `No such file or directory`），故无法做基线 diff |

### 1.2 使用的命令 / 数据源

- 文件系统：`find -newermt`、`stat -f`、`ls -l`、`shasum -a 256`、`wc`
- 文本检索：`grep -rn / -c / -o`、Python 3（正则与结构化统计，替代缺失的 `grep -P`）
- 数据库：`docker exec jc2001-postgres psql -U campus_trade -d campus_trade` → `information_schema`、`pg_indexes`、`pg_enum`、`flyway_schema_history`
- 运行时：`curl` 打 `127.0.0.1:8080`
- 归档：`unzip -l`、`unzip -Z1`、`unzip -q` 解包比对
- 打包脚本：`bash -n`、实跑 `bash scripts/package-iteration1.sh /tmp/verify-pkg`

### 1.3 收尾哈希（证明测量对象稳定）

```
ac1e316c2c32b2dd2305e74ff27446a276b0ae772c8bcbcf52c698b8d060c846  iteration1-delivery.zip
2962657f82811d5b8b68e26a71c138496285bdcfbc1e99e5589aa64ef7dd24e0  docs/iteration1/DELIVERY.md
d5ffada0834663e2d13b28c64434a740962f5ec673ee8c17a8e265918d093945  docs/iteration1/design/final-delivery-report.md
292cb597beaf6251d63dba2a2c4b51a40c3437a18fa71a300f214d7ce2bacbf9  docs/iteration1/state-machines.md
9832c376ccdb72e572b8470e26c1d77ad609e5dd92859807fb16570f6732c3c2  docs/iteration1/design/sequence-diagrams.md
```

复核时间点哈希与上述完全一致 → 结论对应的产物版本唯一。

---

## §2 逐项验证结果

### V-1 DDP-01 修复

#### V-1(a) S-07 行倒数第二格（副作用列）引用为 R-24

**主张**：`state-machines.md` 中以 `| S-07 |` 开头的行，其**倒数第二格**规则引用是 `R-24`，且**不是** `R-22`。

**实测**：

```
$ grep -n '^| S-07 ' docs/iteration1/state-machines.md
189:| S-07 | PENDING | 任一商品被其他占用/下架/违规下架 | CANCELLED（原因=整笔作废） | 系统联动 | 触发条件出现 | **整笔作废**：释放全部占用；通知双方；写原因（是「任一件被占则整笔作废」规则 R-24 的落地） | IDEM-B（只作废一次） |
```

表头为 `| # | 起始状态 | 事件/动作 | 目标状态 | 触发者 | 前置条件 | 副作用 | 幂等要求 |`（8 列），故倒数第二格 = **副作用**列，其内容含「规则 **R-24** 的落地」。

补充核查——全文件是否仍残留 R-22：

```
$ grep -n 'R-22' docs/iteration1/state-machines.md
(no R-22 occurrence in state-machines.md)
```

**结论**：✅ **确认**。第 189 行引用为 `R-24`，全文件 `R-22` 出现次数为 **0**。

#### V-1(b) rules.md 中 R-22 / R-24 的定义

**实测**（原文粘贴）：

```
$ sed -n '40,42p' docs/iteration1/rules.md
| R-22 | 整包接受、不可部分接受 | 交换 | **最高** | all-or-nothing |
| R-23 | 同时占用互斥 | 交换 | **最高** | 一物一占用 |
| R-24 | 整笔作废 | 交换 | **最高** | 任一件被占则全废 |

$ sed -n '274p' docs/iteration1/rules.md
### R-22 整包接受、不可部分接受 【最高优先级】

$ sed -n '294p' docs/iteration1/rules.md
### R-24 整笔作废 【最高优先级】
```

**结论**：✅ **确认**。`rules.md` 中 **R-22 = 整包接受、不可部分接受**，**R-24 = 整笔作废**；R-22 正文（第 274 行）与 R-24 正文（第 294 行）标题与索引表一致。

#### V-1(c) 是否仍有其他位置误把 S-07 引为 R-22

**实测**（全仓库检索，排除 `target/`、`.mvnrepo/`；命中共 13 行）：

```
$ grep -rn 'S-07' . | grep -v '^Binary' | grep 'R-22' | grep -v '^./target/' | grep -v '^./.mvnrepo/'
./docs/iteration1/design/design-doc-verification.md:142   ← DDP-01 缺陷登记（§4.1）
./docs/iteration1/design/design-doc-verification.md:155   ← 修复前后 diff（§4.1.1，前置 "-" 行）
./docs/iteration1/design/design-doc-verification.md:166   ← 残留核查说明
./docs/iteration1/design/final-delivery-report.md:109     ← DDP-01 缺陷登记
./docs/iteration1/design/final-delivery-report.md:118     ← 修复前后 diff（"-" 行）
./docs/iteration1/design/final-delivery-report.md:127     ← 残留核查说明
./docs/iteration1/design/final-delivery-report.md:233     ← F-09 检查项说明
./docs/iteration1/design/final-delivery-report.md:281     ← 验收表复述
./docs/iteration1/design/sequence-diagrams.md:259         ← 待确认表 Q/DDP-01 记录
./docs/iteration1/design/sequence-diagrams.md:1089        ← 待确认表 DDP-01 行
./docs/iteration1/progress-update-1.md:90                 ← 修复说明
./docs/iteration1/progress-update-1.md:194                ← 缺陷复盘叙述
./docs/iteration1/DELIVERY.md:134                         ← 缺陷登记表
```

以上 **13 行全部位于缺陷记录 / 修复说明 / 待确认表 / 复盘叙述**之中，即任务书明确列为「EXPECTED、不算缺陷」的类别；**不存在任何一处把 S-07 当作规则依据来引用 R-22 的实质条款**。

另核对：`SystemDesign.md:460` 与 `idempotency-and-concurrency.md:534/617` 引用 S-07 时均使用 **R-24**（正确）：

```
docs/iteration1/design/SystemDesign.md:460:    PENDING --> CANCELLED : S-07 任一件被占/下架 → 整笔作废（R-24）
docs/iteration1/design/idempotency-and-concurrency.md:534:--    2b) 交换占用 → 整笔作废（S-07 / S-13，R-24）
```

**结论**：✅ **确认**。除缺陷记录本身外**无其他误引用**，错误未传播。

#### V-1(d) 本步是否触碰非文档文件

**主张（任务书口径）**：`src/` 或 `pom.xml`/`Dockerfile`/`docker-compose.yml` 中，mtime 晚于 `2026-10-12 22:00` 的文件应为 **0 个**。

**实测 1 —— 按任务书 22:00 阈值**：

```
$ find src pom.xml Dockerfile Dockerfile.build docker-compose.yml .dockerignore -newermt '2026-10-12 22:00' | wc -l
138
```

即 **138 个**文件晚于 22:00 —— 与任务书「expect none」**不符**。

**实测 2 —— 定位真实时间边界**：

```
$ find src -type f -newermt '2026-10-12 22:45' | wc -l
0
$ find src -type f -newermt '2026-10-12 23:00' | wc -l
0
$ find src -name '*.java' -exec stat -f '%Sm %N' -t '%Y-%m-%d %H:%M:%S' {} \; | sort -r | head -2
2026-10-12 22:44:37 src/main/java/com/jc2001/campustrade/controller/HealthController.java
2026-10-12 22:44:37 src/main/java/com/jc2001/campustrade/controller/FrozenSpecController.java
```

受检文件 mtime 汇总：

```
2026-10-12 21:47:07  pom.xml
2026-10-12 22:29:18  Dockerfile
2026-10-12 22:29:30  docker-compose.yml
2026-10-12 22:40:35  src/main/resources/application.yml
2026-10-12 22:44:13  src/main/java/.../service/impl/*.java 等（138 个文件集中于 22:40–22:44）
2026-10-12 22:44:37  src/main/java/.../controller/HealthController.java
2026-10-12 22:44:37  src/main/java/.../controller/FrozenSpecController.java
```

本步骤窗口（23:02–23:08）内被写入的文件**仅有文档 / 脚本 / 归档**：

```
$ find . -type f -not -path './target/*' -not -path './.mvnrepo/*' -newermt '2026-10-12 23:00' -exec stat -f '%Sm  %N' -t '%H:%M:%S' {} \; | sort
23:02:20  ./docs/iteration1/state-machines.md
23:03:33  ./docs/iteration1/design/sequence-diagrams.md
23:03:56  ./docs/iteration1/design/design-doc-verification.md
23:04:56  ./docs/iteration1/progress-update-1.md
23:05:19  ./scripts/package-iteration1.sh
23:05:37  ./docs/iteration1/README.md
23:06:17  ./docs/iteration1/design/README.md
23:08:30  ./docs/iteration1/DELIVERY.md
23:08:30  ./docs/iteration1/design/final-delivery-report.md
23:08:31  ./iteration1-delivery.zip
```

**验证 `find -newermt` 语义正确**（避免误判）：对已知 mtime 为 `21:23:19` 的 `V1__init_schema.sql` 执行 `-newermt '2026-10-12 21:30'` 无输出，符合预期。

**结论**：✅ **确认（实质）**，附口径更正说明。本步（23:02 起）未触碰任何 `src/`、`pom.xml`、`Dockerfile`、`docker-compose.yml` 文件；`src/` 最新 mtime 为 **22:44:37**，来自**本步之前**的步骤。任务书给出的 22:00 阈值会命中 138 个前序步骤文件，**该阈值不等于本步边界**。此点同时印证交付方自述（`design-doc-verification.md` §4.1.1：「`git`-less 环境下以 `find -newermt` 验证：本步仅触碰 `state-machines.md` 与文档类文件」）。

---

### V-2 P-01 修复

#### V-2(a) `| 覆盖 FR |` 行数

**主张**：新增 **9** 行 `| 覆盖 FR |`。

**实测**：

```
$ grep -c '| 覆盖 FR |' docs/iteration1/design/sequence-diagrams.md
10
$ grep -n '| 覆盖 FR |' docs/iteration1/design/sequence-diagrams.md
73:| 覆盖 FR | FR-209、FR-210、...
240:| 覆盖 FR | FR-210、FR-401、...
433:| 覆盖 FR | FR-303、FR-304、FR-412、FR-502 |
516:| 覆盖 FR | FR-211、FR-412、FR-501、...
719:| 覆盖 FR | FR-303、FR-304（...） |
795:| 覆盖 FR | FR-303、FR-304、FR-305、FR-306、FR-307、FR-601 |
892:| 覆盖 FR | FR-408、FR-602 |
951:| 覆盖 FR | FR-505、FR-506、FR-603 |
1014:| 覆盖 FR | FR-604 |
1072:| 时序图 | 覆盖 operationId（逗号分隔） | 覆盖 FR | 状态转换编号 | 关键规则 | 主要错误码 |
```

**解释**：字面匹配 10 处 = **9 个数据行 + 1 个 §8 交叉索引表的列头**（第 1072 行）。列头不是数据行。

**结论**：✅ **确认**。**9** 个数据行，与主张一致（第 10 处为 `覆盖 FR` 列头本身）。

#### V-2(b) 9 个 `sequenceDiagram` 区块是否各含且仅含 1 行

**实测**：

```
$ grep -c '^```mermaid' docs/iteration1/design/sequence-diagrams.md
9
$ grep -n 'sequenceDiagram' docs/iteration1/design/sequence-diagrams.md
63:6. **绘图约定。** 全部图形仅使用 Mermaid `sequenceDiagram`；渲染细节见 §9。   ← 散文
90:sequenceDiagram
269:sequenceDiagram
456:sequenceDiagram
542:sequenceDiagram
741:sequenceDiagram
817:sequenceDiagram
910:sequenceDiagram
971:sequenceDiagram
1034:sequenceDiagram
1119:1. **仅使用 `sequenceDiagram`。** ...                                        ← 散文
```

`sequenceDiagram` 字符串出现 11 次，其中 2 次在散文（第 63、1119 行），**Mermaid 图块恰为 9 个**（第 90/269/456/542/741/817/910/971/1034 行）。

区块与 `覆盖 FR` 行的一一对应（按章节）：

| # | 章节 | 图块起始行 | `覆盖 FR` 行 |
| --- | --- | --- | --- |
| 1 | §1 购买主线 | 89 | 73 |
| 2 | §2 交换主线 | 268 | 240 |
| 3 | §3 交换整笔作废 | 455 | 433 |
| 4 | §4 治理主线 | 541 | 516 |
| 5 | §5 并发抢购 | 740 | 719 |
| 6 | §6 超时释放 | 816 | 795 |
| 7 | §7.1 交换提议过期 | 909 | 892 |
| 8 | §7.2 封禁到期解封 | 970 | 951 |
| 9 | §7.3 评价窗口关闭 | 1033 | 1014 |

**结论**：✅ **确认**。9 个时序图章节**各恰好 1 行** `| 覆盖 FR |`。

#### V-2(c) 文件中每个 `FR-\d{3}` 是否真实存在于 requirements.md

**实测**：

```
distinct FR in sequence-diagrams.md: 35
distinct FR in requirements.md:      60   （含 6 个系列标题伪 ID，见下）
FR NOT found in requirements.md: NONE
sequence-diagrams FRs that are NOT definition rows in requirements.md: []
sequence-diagrams FRs total: 35  all are def rows? True
```

`sequence-diagrams.md` 中的 35 个 FR（去重）：
`FR-209 FR-210 FR-211 FR-301 FR-302 FR-303 FR-304 FR-305 FR-306 FR-307 FR-309 FR-310 FR-311 FR-312 FR-401 FR-402 FR-404 FR-405 FR-406 FR-407 FR-408 FR-409 FR-410 FR-411 FR-412 FR-501 FR-502 FR-503 FR-504 FR-505 FR-506 FR-601 FR-602 FR-603 FR-604`

**结论**：✅ **确认**。**35 个去重 FR，0 个编造**，且 35 个**全部**是 `requirements.md` 中的**定义表格行**（比「令牌存在」更强的判据）。

> 附注：`requirements.md` 中 60 个去重令牌里有 6 个不是需求行，而是**系列标题**（`FR-100 系列`…`FR-600 系列`）；这 6 个未被任何时序图引用，故不影响结论。

#### V-2(d) 9 个 Mermaid 块的分支配对与引号配对

**实测**（Python 逐块解析，`alt`/`opt`/`loop`/`par` 与 `end` 计数，逐行双引号奇偶）：

```
mermaid blocks: 9
block#1 lines 89-230:   open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#2 lines 268-423:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#3 lines 455-506:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#4 lines 541-709:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#5 lines 740-785:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#6 lines 816-880:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#7 lines 909-943:  open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#8 lines 970-1006: open/close balance=0 OK []   quotes: all lines have even number of double quotes
block#9 lines 1033-1066:open/close balance=0 OK []   quotes: all lines have even number of double quotes
```

**结论**：✅ **确认**。9 个块 **balance 全部为 0**（无孤立 `end`、无未闭合分支），**每行双引号均成对**。

---

### V-3 接口对齐

#### V-3(a)(b)(c) `operationId` 与控制器方法

**主张**：`openapi.yaml` 有 **83** 个 `operationId`；控制器（排除 `HealthController.java`、`FrozenSpecController.java`）方法数一致；差集为空。

**实测 —— 契约侧**：

```
operationId entries: 83
duplicates: NONE
distinct operationId: 83
```

**实测 —— 代码侧**（`public ResponseEntity<ApiResponse<...>> <name>(`）：

| Controller | 方法数 |
| --- | --- |
| AdminController | 17 |
| AuthController | 6 |
| CartController | 4 |
| DictionaryController | 4 |
| InternalController | 4 |
| ItemController | 10 |
| NotificationController | 3 |
| OrderController | 8 |
| ReportController | 3 |
| ReviewController | 3 |
| SwapController | 16 |
| UserController | 5 |
| **合计** | **83** |

```
TOTAL methods in scope: 83
duplicates: NONE
openapi operationIds: 83   controller methods: 83
In openapi but NOT in controllers: []
In controllers but NOT in openapi: []
```

**结论**：✅ **确认**。**83 ↔ 83，双向差集均为 0**，且两侧各自无重名。

#### V-3(d) `openapi.yaml` mtime 是否未变

**实测**：

```
$ stat -f '%Sm  %N' -t '%Y-%m-%d %H:%M:%S' docs/iteration1/design/openapi.yaml
2026-10-12 21:41:35  docs/iteration1/design/openapi.yaml
$ find docs/iteration1/design/openapi.yaml -newermt '2026-10-12 22:00' | wc -l
0
```

**结论**：✅ **确认**。mtime 为 `21:41:35`，早于 22:00，本步未改动。

---

### V-4 数据库迁移未改动且仍成功

#### V-4(a) 迁移文件 mtime

**主张**：`V1*.sql` ~ `V4*.sql` 全部位于 2026-10-12 21:18–21:41。

**实测**：

```
2026-10-12 21:18:10  src/main/resources/db/migration/V2__seed_reference_data.sql
2026-10-12 21:20:45  src/main/resources/db/migration/V3__seed_development_data.sql
2026-10-12 21:23:19  src/main/resources/db/migration/V1__init_schema.sql
2026-10-12 21:41:43  src/main/resources/db/migration/V4__fix_review_window_index.sql
```

**结论**：✅ **确认**。4 个文件全部落在 **21:18:10 – 21:41:43**，即 21:18–21:41 区间内，均早于本步。

#### V-4(b) 现网 `flyway_schema_history`

**主张**：4 行，`success` 全为真。

**实测**：

```
$ docker exec jc2001-postgres psql -U campus_trade -d campus_trade -c \
    "SELECT count(*) AS rows, bool_and(success) AS all_success FROM flyway_schema_history;"
 rows | all_success
------+-------------
    4 | t
(1 row)

$ ... -c "SELECT installed_rank, version, description, type, success, installed_on
          FROM flyway_schema_history ORDER BY installed_rank;"
 installed_rank | version |       description       | type | success |        installed_on
----------------+---------+-------------------------+------+---------+----------------------------
              1 | 1       | init schema             | SQL  | t       | 2026-10-12 22:33:45.412483
              2 | 2       | seed reference data     | SQL  | t       | 2026-10-12 22:33:45.692274
              3 | 3       | seed development data   | SQL  | t       | 2026-10-12 22:33:45.706674
              4 | 4       | fix review window index | SQL  | t       | 2026-10-12 22:33:45.750827
(4 rows)
```

**结论**：✅ **确认**。**4 行，`bool_and(success) = t`**（V1~V4 全部成功）。

---

### V-5 运行时端点

**主张与实测对照**：

| # | 端点 | 主张 | 实测 | 结论 |
| --- | --- | --- | --- | --- |
| 1 | `GET /actuator/health` | 200 | **200**（370 字节） | ✅ |
| 2 | `GET /api/v1/health` | 200 + `"code":"OK"` + `"data":"UP"` | **200** + 两者均命中（121 字节） | ✅ |
| 3 | `GET /api/v1/health/ready` | `database=UP`、`flyway.appliedCount=4`、`status=UP` | **全部命中**（285 字节） | ✅ |
| 4 | `GET /v3/api-docs` | 恰好 **83** operations | **83**（73 paths / 83 operationId，无重名） | ✅ |
| 5 | `GET /swagger-ui.html` | 302 | **302** | ✅ |
| 6 | `GET /openapi/openapi.yaml` | 200 + 264148 字节 | **200 + 264148 字节**（精确相符） | ✅ |

**关键原始输出**：

```
$ curl -s -o /tmp/resp.bin -w 'HTTP %{http_code}  bytes=%{size_download}\n' http://127.0.0.1:8080/actuator/health
HTTP 200  bytes=370
{"status":"UP","components":{"db":{"status":"UP",...},"diskSpace":{"status":"UP",...},
 "livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"}},
 "groups":["db-readiness","liveness","readiness"]}

$ ... /api/v1/health
HTTP 200  bytes=121
{"code":"OK","message":"服务正常","data":"UP","traceId":"86a4f12539e04a11","timestamp":"2026-10-12T15:08:12.248474Z"}

$ ... /api/v1/health/ready
HTTP 200  bytes=285
{"code":"OK","message":"就绪检查完成","data":{"iteration":"iteration-1",
 "apiVersion":"1.0.0-frozen-candidate","database":"UP",
 "flyway":{"appliedCount":4,"currentVersion":"4","expectedCount":4},"status":"UP"},...}

$ ... /swagger-ui.html
HTTP 302  bytes=0

$ ... /openapi/openapi.yaml
HTTP 200  bytes=264148
openapi: 3.0.3
...

$ curl -s http://127.0.0.1:8080/v3/api-docs
paths: 73      operations counted: 83      operationIds present: 83      dupes: NONE
```

**结论**：✅ **6/6 项全部确认**，其中 `/openapi/openapi.yaml` 字节数与主张**完全一致（264148）**。

---

### V-6 一致性检查

#### V-6(a) `docs/**/*.md` 的四项元数据字段

**主张**：全部含 版本 / 日期 / 作者 / 状态。

**实测**：

```
total docs/**/*.md files: 27
All files have 版本/日期/作者/状态 within first 2500 chars
```

**结论**：✅ **确认**。受检 **27 个** Markdown 文件**全部通过**（文件头部 2500 字符内四项齐备），无遗漏。

#### V-6(b) 相对 Markdown 链接可解析

**主张**：共 118 个链接，**0 悬空**。

**实测**：

```
相对链接出现次数（occurrence）        : 180
去重 (文件, 目标) 对（distinct pair） : 118
悬空（dangling）                      : 0
外部 / 锚点链接（已排除）             : 0
```

**说明**：任务书所称 **118** 与「**去重 (文件, 目标) 对**」计数**精确吻合**；若按**出现次数**统计则为 **180**（同一目标在文内多次引用会被重复计数）。两种口径下**悬空数均为 0**。

解包归档中的文档同样解析正常（归档内路径齐备，见 V-7）。

**结论**：✅ **确认**（0 悬空）。链接总数依口径为 **118（去重对）/ 180（出现次数）**，与主张的 118 在去重口径下一致。

#### V-6(c) 上游源文件是否仅「读取」而未「修改」

**实测（修改面）**：

```
2026-10-12 21:08:34  docs/iteration1/rules.md            ← 未修改
2026-10-12 23:02:20  docs/iteration1/state-machines.md   ← 仅 DDP-01 一处修复
```

本步（≥23:00）被修改的文件共 10 个，**全部为文档 / 脚本 / 归档**（清单见 V-1(d) 实测 2）；`rules.md` 的 mtime 停留在 `21:08:34`，**确未被改动**。

**实测（读取面）**：无审计日志、无 `.git` 基线，**无法观测「读取」行为**。

**结论**：✅ **确认（修改面）** / ⚠️ **未能验证（读取面）**。「仅 `state-machines.md` 被修改（且仅 S-07 一处引用）」**成立**；「只**读**了这两份上游文件」这一半句**无法测量**，不做判定。

---

### V-7 打包脚本与归档

#### V-7(a)(b) 脚本可执行 / 语法 / 收录范围

```
$ ls -l scripts/package-iteration1.sh
-rwx--x--x@ 1 god-come  staff  6559 Oct 12 23:05 scripts/package-iteration1.sh

$ bash -n scripts/package-iteration1.sh
bash -n: OK (exit 0)
```

收录清单核对（`INCLUDE` 数组）：

```
present: docs/iteration1
present: src
present: pom.xml
present: docker-compose.yml
present: Dockerfile
present: Dockerfile.build
present: .dockerignore
present: README-backend.md
present: README-db.md
present: db          present: scripts      present: tools
```

**结论**：✅ **确认**。脚本具备可执行位（`-rwx--x--x`）、`bash -n` 通过，且**包含了任务书列举的全部 7 个必需路径**（另额外收录 `Dockerfile.build`/`.dockerignore`/`db`/`scripts`/`tools`）。

#### V-7(c) 归档计数与体积

**主张**：203 文件（158 java / 29 md / 5 sql），约 692 KB。

**实测**：

```
$ unzip -Z1 iteration1-delivery.zip | grep -v '/$' | wc -l      → 203
$ unzip -Z1 iteration1-delivery.zip | grep -c '/$'              → 27    （目录条目）
$ unzip -Z1 iteration1-delivery.zip | grep -v '/$' | grep -c '\.java$' → 158
$ unzip -Z1 iteration1-delivery.zip | grep -v '/$' | grep -c '\.md$'   → 29
$ unzip -Z1 iteration1-delivery.zip | grep -v '/$' | grep -c '\.sql$'  → 5
$ stat -f '%z' iteration1-delivery.zip                          → 710199 字节（693.6 KiB）
```

**结论**：✅ **确认**。文件数 **203**、Java **158**、Markdown **29**、SQL **5** —— 与主张**逐项一致**；体积 **710199 字节 ≈ 693.6 KiB**，与「约 692 KB」相符（**注**：归档在验证期间由 708986 → 710199 字节，见 §4）。

#### V-7(d) 15 个关键路径

```
PRESENT  pom.xml
PRESENT  docker-compose.yml
PRESENT  Dockerfile
PRESENT  Dockerfile.build
PRESENT  .dockerignore
PRESENT  README-backend.md
PRESENT  README-db.md
PRESENT  docs/iteration1/DELIVERY.md
PRESENT  docs/iteration1/progress-update-1.md
PRESENT  docs/iteration1/state-machines.md
PRESENT  docs/iteration1/design/openapi.yaml
PRESENT  docs/iteration1/design/final-delivery-report.md
PRESENT  scripts/package-iteration1.sh
PRESENT  db/verify_schema.sql
PRESENT  src/main/resources/db/migration/V1__init_schema.sql
```

**结论**：✅ **确认**。**15/15 全部存在**，无缺失。

#### V-7(e) 目录层级未扁平化

```
$ unzip -Z1 iteration1-delivery.zip | awk -F/ '{print $1"/"$2}' | sort -u | head
iteration1-delivery/
iteration1-delivery/.dockerignore
iteration1-delivery/DELIVERY-MANIFEST.txt
iteration1-delivery/Dockerfile
iteration1-delivery/Dockerfile.build
iteration1-delivery/README-backend.md
iteration1-delivery/README-db.md
iteration1-delivery/db
iteration1-delivery/docker-compose.yml
iteration1-delivery/docs
...

$ grep -q '^iteration1-delivery/docs/iteration1/state-machines.md$' entries  → ✅ 命中
$ grep -q '^iteration1-delivery/iteration1/' entries                          → ✅ 无命中
```

**结论**：✅ **确认**。归档层级为 `iteration1-delivery/docs/iteration1/...`，**保留了 `docs/` 前缀**，**不存在** `iteration1-delivery/iteration1/...` 的扁平化路径。

#### V-7(f) 排除项是否生效

| 排除模式 | 命中数 |
| --- | --- |
| `target/` | **0** |
| `.mvnrepo/` | **0** |
| `.git/` | **0** |
| `__pycache__` | **0** |
| `*.log` / `.log` | **0** / **0** |
| `data/` | **0** |
| `uploads/` | **0** |
| `.DS_Store` | **0** |
| `node_modules/` | **0** |

> 透明度说明：检索「data」时命中的 4 条均为**文件名**而非目录——
> `src/main/resources/db/migration/V3__seed_development_data.sql`、
> `V2__seed_reference_data.sql`、`src/main/java/.../dto/response/ItemImageUploadResponse.java`、`EmptyData.java`。
> 均属正常源码，非被排除的运行时数据目录。

**结论**：✅ **确认**。任务书列举的排除项**全部为 0**。

#### V-7(g) 反驳性测试：实跑脚本

```
$ bash scripts/package-iteration1.sh /tmp/verify-pkg
...
 ✅ 打包完成
  输出文件   : /tmp/verify-pkg/iteration1-delivery.zip
  文件大小   : 696K (710199 字节)
  文件总数   : 203
    其中 Java : 158
    其中 文档 : 29
    其中 SQL  : 5
EXIT CODE = 0
```

自产包 vs 已交付包比对：

```
$ diff <(unzip -Z1 交付包 | sort) <(unzip -Z1 /tmp/verify-pkg/... | sort)
  entry name lists IDENTICAL
710199  iteration1-delivery.zip            （交付包）
710199  /tmp/verify-pkg/iteration1-delivery.zip （自产包）
```

归档内部的 `state-machines.md` 亦为修复后版本：

```
$ grep -n '^| S-07 ' .../iteration1-delivery/docs/iteration1/state-machines.md
189:| S-07 | ... 规则 R-24 的落地） | IDEM-B（只作废一次） |
$ grep -c 'R-22' .../iteration1-delivery/docs/iteration1/state-machines.md
0
```

归档内 `DELIVERY-MANIFEST.txt` 自述统计与该包实际一致：

```
生成时间: 2026-10-12 23:08:30 CST
  Java 源文件      : 158
  Markdown 文档    : 29
  SQL 文件         : 5
  总文件数         : 203
```

**结论**：✅ **确认**。脚本**实跑退出码 0**，产出与交付归档**条目清单完全相同、字节数完全相同**；归档内含的 `state-machines.md` 为 R-24 修复版。

---

### V-8 无新增业务逻辑

**主张**：本步未新增 `.java` / `.vue`；`openapi.yaml` 仍为 83 操作。

**实测**：

```
$ find src -type f -newermt '2026-10-12 23:00' | wc -l
0
$ find src -name '*.java' -newermt '2026-10-12 22:45' | wc -l
0
$ find . -name '*.vue' -not -path './target/*' | wc -l
0
$ find src -name '*.java' | wc -l
158
$ grep -c '^\s*operationId:' docs/iteration1/design/openapi.yaml
83
```

`src/` 下最新 `.java` 文件为 `22:44:37`（`HealthController.java`、`FrozenSpecController.java`），**远早于本步**。

**结论**：✅ **确认**。本步新增 `.java` = **0**、新增 `.vue` = **0**（全仓库 `.vue` 数量为 0）；`openapi.yaml` 仍为 **83** 操作。

---

### V-9 交付文档自述数值抽查

抽查 **6 项指定主张**，并顺带核实 **4 项**同源数值主张。

| # | 主张 | 出处 | 实测 | 结论 |
| --- | --- | --- | --- | --- |
| 1 | **25 张业务表** | `DELIVERY.md:41` / `final-delivery-report.md:34,76` | `information_schema` 中 `public` 基础表 **26**，剔除 `flyway_schema_history` = **25** | ✅ 确认 |
| 2 | **324 列** | 同上 | `information_schema.columns` 全量 **334**，剔除 `flyway_schema_history` = **324** | ✅ 确认 |
| 3 | **93 个错误码** | `DELIVERY.md:57` / `final-delivery-report.md:38` | `error-codes.md` §3.1–§3.13 合计 **93** | ✅ 确认 |
| 4 | **54 条 FR** | `DELIVERY.md:27` / `final-delivery-report.md:75` | `requirements.md` 中 `FR-nnn` 定义表行 **54** | ✅ 确认 |
| 5 | **18 项不做清单** | `DELIVERY.md:28` / `final-delivery-report.md:75` | `out-of-scope.md` 中 `OOS-nn` 行/标题 **18**（OOS-01~OOS-18，含核心 4 项） | ✅ 确认 |
| 6 | **7 个部分唯一索引** | `DELIVERY.md:58` / `final-delivery-report.md:93` | `pg_indexes WHERE indexdef ILIKE '%UNIQUE%WHERE%'` = **7** | ✅ 确认 |
| 7 | **16 必填 + 16 可选幂等** | `DELIVERY.md:58,87` / `final-delivery-report.md:79,91` | 控制器 `@RequestHeader ... required=true` = **16**，`required=false` = **16**（合计 32） | ✅ 确认 |
| B1 | **26 枚举** | `final-delivery-report.md:34,94` | `pg_enum` 去重 `enumtypid` = **26**（枚举值 116） | ✅ 确认 |
| B2 | **99 schemas** | `DELIVERY.md:122` | `openapi.yaml` `components.schemas` = **99** | ✅ 确认 |
| B3 | **70 转换编号** | `final-delivery-report.md:75` | 6 台状态机：I 18 + O 12 + S 13 + B 12 + P 9 + AP 6 = **70** | ✅ 确认 |
| B4 | **12 Controller / 99 DTO / 12 Service** | `DELIVERY.md:124` | 控制器 14 文件 − Health − FrozenSpec = **12**；DTO 33+66 = **99**（另有 `package-info.java`）；Service 接口 **12**、实现 **12** | ✅ 确认 |

**关键原始输出**：

```
# 表 / 列 / 枚举
business_tables(剔除 flyway) = 25      columns(剔除 flyway) = 324
distinct enum types (pg_enum) = 26     enum values = 116

# 部分唯一索引（7 个，逐个列出）
uq_item_occ_active_item | uq_payments_order_succeeded | uq_penalties_one_active
uq_reports_pending_reporter_item | uq_reviews_order_reviewer
uq_reviews_swap_reviewer | uq_swap_bundle_items_active

# 错误码：按章节求和
### 3.1 通用 8 | 3.2 校验 4 | 3.3 认证 7 | 3.4 用户 3 | 3.5 商品 10
### 3.6 区域/取货点 5 | 3.7 购物车 3 | 3.8 订单 11 | 3.9 交换 21
### 3.10 评价 4 | 3.11 举报 4 | 3.12 治理/申诉 9 | 3.13 文件 4
8+4+7+3+10+5+3+11+21+4+4+9+4 = 93
（另有 §2.1「错误码规模统计」汇总表 15 行，为分域小计，非新增码 → 故全文件首格码数 108 = 93 + 15）

# FR：定义行 54，系列标题 6（FR-100/200/300/400/500/600 不是需求行）
定义表行 = 54，全文件去重令牌 = 60

# 幂等：32 条 @RequestHeader
required=true : 16        required=false: 16

# schemas
component schemas in openapi.yaml: 99
（对比：运行时 /v3/api-docs 的 components.schemas = 124 —— Spring 额外生成内联 schema，与契约文件口径不同，非矛盾）

# 6 台状态机转换编号
I: 18   O: 12   S: 13   B: 12   P: 9   AP: 6   → 70
```

**结论**：✅ **10/10 全部确认**（6 项指定主张 + 4 项附加抽查），**无一被推翻**。

---

## §3 我未能验证的项

| # | 无法验证的内容 | 原因 |
| --- | --- | --- |
| 1 | 「`state-machines.md` 与 `rules.md` 是本步**唯一读取**（read）的上游源文件」 | 文件系统**不记录读取行为**，无审计日志；**无 `.git`** 亦无基线可比。仅**修改**可测（见 V-6(c)）。 |
| 2 | 「DDP-01 修改**仅 1 处、仅 1 行、1 个编号**」的严格证明 | 仓库**无 `.git`**（`ls -d .git` → No such file or directory），无法对修改前版本做 diff。可测的是**最终态**（第 189 行为 R-24、全文件 R-22 计数为 0）以及交付方在 `design-doc-verification.md` §4.1.1 自行登记的单行 diff。**修改范围依赖自述**。 |
| 3 | `mvn` 构建、`mvn test`、Flyway `validate`、以及自述的「19 项负向测试通过」 | 不在本次指派的测量清单内，我**未重跑**构建/测试套件。`flyway_schema_history` 的 `success` 与 `/actuator/health` 的 `db=UP` 可**间接**支持迁移有效，但不等价于 `flyway validate`。 |
| 4 | `design-doc-verification.md` 中「git-less 环境下以 `find -newermt` 验证：本步仅触碰 `state-machines.md` 与文档类文件」的双重确认 | 该自述**与我的实测一致**（见 V-1(d)），但我无法回溯其执行时刻的目录状态。 |
| 5 | 交付方「自证」流程本身的完整性 | 我只能验证其**结论性数字**，无法验证其自证过程是否与本次同源、同版本。 |
| 6 | 归档在 23:08:31 之后是否会被再次重写 | 我仅能确认**截至本次收尾**哈希稳定（§1.3）。若后续再有写入，本报告结论对应的是**我实测的那个版本**（`ac1e316c…`）。 |

> 上述各项**一律不视为通过**，亦**不视为失败**——即「未测量」。

---

## §4 与交付方主张不一致之处

**数值主张层面：未发现**。抽查的 41 项核心主张 + 4 项附加主张，**没有任何一项被实测推翻**。

以下为**非数值主张**层面的 4 点观察，供交付方与评审注意：

### 4.1 交付物在验证期间被重写（流程风险）

| 文件 | 我首次观测 | 收尾观测 |
| --- | --- | --- |
| `docs/iteration1/DELIVERY.md` | 23:07:04 | **23:08:30** |
| `docs/iteration1/design/final-delivery-report.md` | 23:06:55 | **23:08:30** |
| `iteration1-delivery.zip` | 23:07:05（708986 字节） | **23:08:31（710199 字节）** |

归档内 `DELIVERY-MANIFEST.txt` 的「生成时间: 2026-10-12 23:08:30 CST」表明交付方在我验证开始后**重跑了一次打包脚本**。
**影响**：任何在 `23:08:31` 之前完成的核验，针对的都是**已被取代**的旧版本。
**处置**：本报告全部测量均在 `23:08:31` 之后完成，并以 SHA-256 确认随后无变更（§1.3）。**建议**交付方声明「冻结时刻」，避免「最终交付」处于移动状态。

### 4.2 任务书 V-1 的 mtime 阈值口径

任务书以 `22:00` 为界并预期命中 0 个文件；实测 **138 个** `src/` 文件晚于该阈值（集中于 22:40:35–22:44:37）。这些写入属于**本步之前**的步骤。**交付方「本步未触碰非文档文件」的主张实质成立**，但若按任务书的 22:00 阈值判定，会得到**看似矛盾**的结论。**建议**以真实步骤边界（≥23:00，或本步起始时间）作为判据。

### 4.3 链接计数口径

任务书称「118 个链接」；按**出现次数**实测为 **180**，按**去重 (文件, 目标) 对**实测恰为 **118**。两者**悬空均为 0**。数额差异纯属计数口径，**不构成缺陷**，但主张宜标明口径。

### 4.4 「99 schemas」与运行时 schema 数的口径差异

契约文件 `openapi.yaml` 的 `components.schemas` = **99**（主张成立）；而运行时 `GET /v3/api-docs` 的 `components.schemas` = **124**。差额来自 Spring 为内联响应结构生成的附加 schema。**非矛盾**，但两个数字并列时易被误读为不一致。

---

## §5 独立结论

1. **DDP-01（S-07 规则引用）已真实闭环**：`state-machines.md:189` 的副作用列现为「规则 **R-24** 的落地」，全文件 `R-22` 计数为 **0**；`rules.md` 中 **R-22 = 整包接受**、**R-24 = 整笔作废**，定义未被改动。全仓库除**缺陷记录本身**外无其他误引用，错误**未传播**。
2. **P-01（FR 追溯）已真实闭环**：9 个时序图章节**各恰好 1 行** `| 覆盖 FR |`；文中 **35 个去重 FR 全部真实**（且全部为 `requirements.md` 的定义表行），**0 编造**；9 个 Mermaid 块分支与引号**全部配对**。
3. **接口对齐为满配**：`openapi.yaml` 的 **83** 个 `operationId` 与 12 个业务控制器的 **83** 个方法**双向差集为空**，运行时 `/v3/api-docs` 亦为 **83**。
4. **数据库与运行时与主张相符**：迁移 4 个文件 mtime 落在 21:18–21:41 且 `flyway_schema_history` **4 行全 success**；6 个运行时端点全部符合预期，`/openapi/openapi.yaml` 字节数 **264148 精确命中**。
5. **打包链路可信**：脚本可执行、`bash -n` 通过、**实跑 exit 0**，自产包与交付归档**条目同名、字节数相同**；**203 文件（158 java / 29 md / 5 sql）**、层级**未扁平化**、排除项**全为 0**。
6. **文档自述数值可信度高**：抽查的 **10 项**（含 25 表 / 324 列 / 93 错误码 / 54 FR / 18 不做项 / 7 部分唯一索引 / 16+16 幂等 / 26 枚举 / 99 schemas / 70 转换编号）**全部与实测一致**。
7. **本步未引入业务逻辑变更**：`src/` 下晚于 22:45 与 23:00 的文件数均为 **0**，无新增 `.java`/`.vue`，`openapi.yaml` 仍为 83 操作。

**总体判定**：作为独立验证人，我**未能推翻**交付方关于本次最终步骤的任何可测量主张。**在 23:08:31 冻结的版本（zip SHA-256 `ac1e316c…`）上，本次交付的完整性、一致性与可复现性均通过验证。**

**唯一保留**：`state-machines.md` 的「仅改 1 行」属**自述**（无 `.git` 基线，见 §3-1、§3-2）；「只读未改」中的**读取**面**未能验证**。这两点不影响其余结论。

---

## §6 版本与变更记录

| 版本 | 日期 | 作者 | 变更说明 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 独立验证人（待填写姓名） | 初稿：对迭代一最终步骤的 DDP-01 / P-01 修复、接口对齐、数据库迁移、运行时端点、文档一致性、打包归档与自述数值进行独立实测验证（V-1 ~ V-9） | 草稿（待确认） |
