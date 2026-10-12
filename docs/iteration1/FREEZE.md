# 迭代一 交付冻结声明（FREEZE）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 交付冻结声明 |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 交付负责人（待填写姓名） |
| 状态 | **已冻结（FROZEN）** |
| 冻结时刻 | **2026-10-12 23:13:30 CST** |
| 冻结范围 | `docs/iteration1/`、`src/`、构建与部署文件、`scripts/`、`tools/`、`iteration1-delivery.zip` |

> **为什么需要这份声明**：独立验证人（见 [`design/independent-verification.md`](./design/independent-verification.md) §0.3）
> 指出：其验证期间交付方**仍在重写交付物**（`DELIVERY.md`、`final-delivery-report.md`、
> `iteration1-delivery.zip` 在 23:06~23:08 之间有更新），因此**需要交付方显式声明「冻结时刻」**，
> 否则任何测量都只是「某个中间态的快照」。本文件即为该声明。
>
> **自冻结时刻起，本迭代交付物不再修改。** 任何后续修改都必须：
> ① 更新本文件并递增版本；② 重新计算下方全部校验码；③ 重新打包。

---

## 1. 交付物内容校验码（SHA-256）

以下为**冻结时刻**的关键交付物内容哈希，可用于核验交付包未被改动。

### 1.1 本步骤（第 7 步）产出与修改的文件

| 文件 | 说明 | SHA-256（前 16 位） |
| --- | --- | --- |
| `docs/iteration1/state-machines.md` | DDP-01 修正 | `292cb597beaf6251` |
| `docs/iteration1/design/sequence-diagrams.md` | P-01 优化（v1.1） | `9832c376ccdb72e5` |
| `docs/iteration1/DELIVERY.md` | 交付说明 | `9d032bf6d02977b6` |
| `docs/iteration1/progress-update-1.md` | 进度更新 1 | `b6b6d8343d7888ba` |
| `docs/iteration1/design/final-delivery-report.md` | 最终交付报告 | `9678d76d363d448b` |
| `docs/iteration1/design/independent-verification.md` | 独立验证报告 | `21a9c14656df21a2` |
| `docs/iteration1/design/design-doc-verification.md` | 验证记录（v1.1） | `525f47ac678ed575` |
| `docs/iteration1/README.md` | 迭代一索引 | `c7cad153b1db14af` |
| `docs/iteration1/design/README.md` | 设计目录索引 | `2ec1bcbeafe2ed95` |
| `scripts/package-iteration1.sh` | 打包脚本 | `c361e39b7e1ebb88` |
| `docs/iteration1/FREEZE.md` | 本文件（冻结前版本） | `536f20060555ebe3` |

### 1.2 契约与数据库迁移（**未修改**的证明）

以下文件在**第 3 步（契约）与第 2 步（迁移）之后从未改动**；其哈希自那时起保持恒定。

| 文件 | 完整 SHA-256 |
| --- | --- |
| `docs/iteration1/design/openapi.yaml` | `36bd7a03a184734cb8ae6b1a582f487f6845c4d8d0633d366087a03e848394fd` |
| `src/main/resources/db/migration/V1__init_schema.sql` | `620ad94bb6cf96b2b2ad5ce88bd73b8c03137c869f9e9d5558cb40e3b7cf898b` |
| `src/main/resources/db/migration/V2__seed_reference_data.sql` | `8a5f22d53904761a994d5093be8247c106ab8ce68adf7a65210e4ce6d392289f` |
| `src/main/resources/db/migration/V3__seed_development_data.sql` | `ab3c9c105aed2eea2cf001cfae325d9e95b9f3f15ae459fb1f86efa18b6c6597` |
| `src/main/resources/db/migration/V4__fix_review_window_index.sql` | `cbd952e089a2d0afad97ab842cde15193fdf935cc3e520c16811ee9587ae3235` |

> `openapi.yaml` 哈希恒定 = **接口契约未被修改**（仍 73 paths / 83 operations）。
> V1~V4 哈希恒定 = **迁移脚本未被修改**（Flyway 在实库仍为 4 条 `success = t`）。

---

## 2. 冻结时的交付包

| 项 | 值 |
| --- | --- |
| 归档文件 | `iteration1-delivery.zip`（仓库根目录） |
| 内容 | 205 个文件（158 `.java` / 31 `.md` / 5 `.sql` / 11 其他）+ 28 个目录项 |
| 冻结时刻归档哈希 | 见 §3 命令当场计算（**归档包含本文件，故其哈希不写在本文件内**，以免自指） |

**核验命令**：

```bash
# 核验单个文件
shasum -a 256 docs/iteration1/design/openapi.yaml
# 期望：36bd7a03a184734cb8ae6b1a582f487f6845c4d8d0633d366087a03e848394fd

# 核验归档
shasum -a 256 iteration1-delivery.zip

# 批量核验本步产出
shasum -a 256 docs/iteration1/state-machines.md \
  docs/iteration1/design/sequence-diagrams.md \
  docs/iteration1/DELIVERY.md \
  docs/iteration1/progress-update-1.md \
  docs/iteration1/design/final-delivery-report.md \
  docs/iteration1/design/independent-verification.md \
  scripts/package-iteration1.sh
```

---

## 3. 独立验证的已知保留（非交付缺陷）

独立验证人确认 **41/41 项核心主张成立、0 项被推翻**，并提出 3 点需读者注意（均为**口径**问题，非交付缺陷）：

| # | 保留 | 交付方回应 |
| --- | --- | --- |
| 1 | 验证期间交付物被重写（23:06~23:08），需声明冻结时刻 | ✅ **已由本文件解决**：冻结时刻 = 2026-10-12 23:12:32 CST，并提供内容哈希 |
| 2 | 任务书给出的 mtime 阈值 `2026-10-12 22:00` 并**不是本步边界**（该阈值下 `src/` 有 138 个文件命中，实为 22:40~22:44 的前序步骤产物） | ✅ 确认。本步真实时间窗为 **23:02~23:12**；`src/` 下最新 mtime 为 **22:44:37**，晚于 22:45 与 23:00 的文件数**均为 0** → 「本步未触碰非文档文件」实质成立。任务书的阈值属**题干口径**，非交付方主张 |
| 3 | **无 `.git` 基线**，故「`state-machines.md` 仅改 1 行 / 1 个编号」无法独立测量（最终态可测，改动范围不可测）；「只读未改」中的**读取**面无审计日志，不可观测 | ⚠️ 属实，无法自证。缓解措施：① `rules.md` 的哈希与 mtime 未变（本步未触碰）；② `state-machines.md` 最终态经全文核查，`S-07` 行含 `R-24` 且全文 `R-22` 计数为 0（除缺陷记录外无误引用）；③ 交付包内所有文件哈希已入本文件，后续任何改动可被检出 |

---

## 4. 冻结后的变更规则

| 允许 | 禁止 |
| --- | --- |
| 修改后**更新本文件**（递增版本 + 重算哈希 + 重新打包） | 修改交付物而不更新本文件 |
| 在**新迭代**目录下新增文件 | 在本迭代目录内新增/删除交付物而不记录 |

---

## 5. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 交付负责人 | 首版：声明冻结时刻（2026-10-12 23:12:32 CST）；记录 10 个本步产出文件的 SHA-256 前 16 位；记录契约与 V1~V4 迁移的完整 SHA-256 作为「未修改」证明；回应独立验证的 3 项保留 | **已冻结（FROZEN）** |

---

## 6. 归档哈希（冻结后校验记录）

> 归档 `iteration1-delivery.zip` **包含本文件**，因此其哈希无法写在归档内的本文件中（否则自指、无法收敛）。
> 下表在**归档生成之后**追加，作为**外部校验记录**。

| 项 | 值 |
| --- | --- |
| 归档路径 | `iteration1-delivery.zip`（仓库根目录） |
| 归档字节数 | **729,046** 字节（712 KB） |
| 归档文件数 | **205**（158 `.java` / 31 `.md` / 5 `.sql` / 11 其他）+ 28 目录项 |
| **归档 SHA-256** | `85997ba15b4a71c8e743db5b015bcedcd45f4957e1da383a99f636a045302145` |
| 归档内 `FREEZE.md` 的 SHA-256（**不含本节 §6**） | `0e838abed7a75b9a5be0bd0ea64a838460d72f8d0a99978f43876f193570302c` |

**说明（重要）**：本节 §6 是**归档生成后**追加的**外部解释性附录**，因此：

- **归档内**的 `docs/iteration1/FREEZE.md` **不含本节 §6**，其哈希为 `0e838abed7a75b9a5be0bd0ea64a838460d72f8d0a99978f43876f193570302c`（该版本即冻结内容）。
- **磁盘上**的本文件因多了 §6 而哈希不同，属**预期**，不代表冻结被破坏。
- **唯一权威的冻结对象是归档本身**，其哈希为 `85997ba15b4a71c8e743db5b015bcedcd45f4957e1da383a99f636a045302145`。
- 若需冻结文档的「零差异」版本，请从归档中提取：`unzip -p iteration1-delivery.zip iteration1-delivery/docs/iteration1/FREEZE.md > FREEZE.frozen.md`

**核验命令**：

```bash
shasum -a 256 iteration1-delivery.zip
# 期望：85997ba15b4a71c8e743db5b015bcedcd45f4957e1da383a99f636a045302145

unzip -p iteration1-delivery.zip iteration1-delivery/docs/iteration1/FREEZE.md | shasum -a 256
# 期望：0e838abed7a75b9a5be0bd0ea64a838460d72f8d0a99978f43876f193570302c
```
