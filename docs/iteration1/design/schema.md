# 迭代一 数据库字段字典（Schema）

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 迭代一 数据库字段字典（Schema） |
| 版本 | v1.0 |
| 日期 | 2026-10-12 |
| 作者 | 数据库与领域模型设计负责人（待填写姓名） |
| 状态 | 草稿（待确认） |
| 目标库 | PostgreSQL 15 |
| 对应迁移 | `V1__init_schema.sql`（表结构与本字典一一对应） |
| 关联文档 | `er-diagram.md`、`db-verification.md`、`open-questions.md`、`../requirements.md`、`../state-machines.md`、`../rules.md` |

> **生成方式**：本字典由 **已在 PostgreSQL 15.19 上成功执行 V1~V3 迁移后的数据库** 直接导出，
> 而非手工转写，因此与迁移脚本严格一致。若两者出现分歧，以迁移脚本为准。

---

## 0. 阅读约定

| 项 | 说明 |
| --- | --- |
| 表名 / 字段名 | 全部 `snake_case`（要求）；复数表名 |
| 主键 | 统一 `id BIGINT GENERATED ALWAYS AS IDENTITY` |
| 审计字段 | `created_at`、`updated_at` 全表必有；`created_by`、`updated_by` 除纯字典表外均有 |
| 乐观锁 | 可并发写的 12 张表含 `version INTEGER NOT NULL DEFAULT 0`（R-18） |
| 时间类型 | 统一 `timestamptz`（R-19 服务端时间为准；A-20 时区 Asia/Shanghai） |
| 金额类型 | `numeric(12,2)`，均带 `>= 0` 校验（R-13 估价仅展示） |
| 状态字段 | 使用 PostgreSQL ENUM，取值与 `state-machines.md` **精确一致** |
| 软删除 | 仅 `items` / `swap_bundles` 有 `deleted_at`；**处罚、申诉、订单、交换记录永不物理删除**（R-32） |
| `_snapshot` 后缀 | 交易发生时的快照值，商品后续编辑不影响历史（R-13） |

---

## 1. 参照与字典表

预设数据，用户在界面上只能选择，不能新增（R-15 / FR-206 / FR-207）。

### trade_zones

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `code` | varchar(32) | 否 | — | 区域占位编码：CAMPUS_A / CAMPUS_B / COMMUNITY_A1 等，待用户确认 Q-01/Q-02 后替换 |
| `name` | varchar(64) | 否 | — | 区域名称 |
| `zone_type` | zone_type | 否 | — | CAMPUS / COMMUNITY |
| `parent_zone_id` | bigint | 是 | — | 可选父区域（如社区归属校区），用于后续「区域组」扩展（OOS-07 迭代 N） |
| `sort_order` | integer | 否 | 0 | 排序值（升序展示） |
| `is_active` | boolean | 否 | true | 停用后新交易不可选，历史数据保留 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |

**trade_zones 的约束与索引**

- 唯一约束：
  - `uq_trade_zones_code`：`UNIQUE (code)`
- 检查约束：
  - `ck_trade_zones_parent_not_self`：`CHECK (((parent_zone_id IS NULL) OR (parent_zone_id <> id)))`
- 外键：
  - `parent_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）

- 索引：
  - `idx_trade_zones_type_active`
  - `trade_zones_pkey`
  - `uq_trade_zones_code`

### pickup_points

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `code` | varchar(32) | 否 | — | 占位编码：PICKUP_LIBRARY / PICKUP_CANTEEN 等，待用户确认 Q-03 后替换 |
| `trade_zone_id` | bigint | 否 | — | 所属交易区域（R-15 必须与交易方同区域） |
| `name` | varchar(64) | 否 | — | 取货点名称，如「图书馆正门台阶右侧」 |
| `point_type` | pickup_point_type | 否 | — | 取货点类型（LIBRARY/CANTEEN/TEACHING/DORM/GATE/SQUARE） |
| `description` | varchar(255) | 是 | — | 文字说明替代地图与导航（OOS-06），不使用经纬度 |
| `sort_order` | integer | 否 | 0 | 排序值（升序展示） |
| `is_active` | boolean | 否 | true | 是否启用 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |

**pickup_points 的约束与索引**

- 唯一约束：
  - `uq_pickup_points_code`：`UNIQUE (code)`
- 外键：
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）

- 索引：
  - `idx_pickup_points_zone_active`
  - `pickup_points_pkey`
  - `uq_pickup_points_code`

### categories

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `code` | varchar(32) | 否 | — | 分类编码（唯一） |
| `name` | varchar(64) | 否 | — | 分类名称 |
| `sort_order` | integer | 否 | 0 | 排序值（升序展示） |
| `is_active` | boolean | 否 | true | 是否启用（停用后不出现在发布页） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段） |

**categories 的约束与索引**

- 唯一约束：
  - `uq_categories_code`：`UNIQUE (code)`

- 索引：
  - `categories_pkey`
  - `idx_categories_active_sort`
  - `uq_categories_code`

### item_conditions

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `code` | varchar(32) | 否 | — | 成色编码（唯一） |
| `name` | varchar(32) | 否 | — | 成色名称（如「几乎全新」） |
| `sort_order` | integer | 否 | 0 | 排序值（由新到旧展示） |
| `is_active` | boolean | 否 | true | 是否启用 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段） |

**item_conditions 的约束与索引**

- 唯一约束：
  - `uq_item_conditions_code`：`UNIQUE (code)`

- 索引：
  - `idx_item_conditions_active_sort`
  - `item_conditions_pkey`
  - `uq_item_conditions_code`

## 2. 用户与账户

users 合并了「用户 + 个人资料」两个需求实体（决策 D-02）。

### users

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `username` | citext | 否 | — | FR-101 用户名唯一、大小写不敏感 |
| `email` | citext | 是 | — | FR-101 邮箱（可选，Q-06 无需验证） |
| `password_hash` | varchar(100) | 否 | — | 密码摘要，禁止存明文；哈希算法在接口契约阶段冻结 |
| `nickname` | varchar(20) | 否 | — | FR-105 昵称 1-20 字符，不唯一（A-04/A-09） |
| `avatar_url` | varchar(255) | 是 | — | FR-106 头像（1 张） |
| `role` | user_role | 否 | 'USER'::user_role | 管理员为 ADMIN（P-00-A 不参与交易） |
| `status` | user_status | 否 | 'ACTIVE'::user_status | RESTRICTED 表示处于生效中处罚（permission-matrix §8 白名单约束） |
| `restricted_until` | timestamptz | 是 | — | 处罚到期时间，用于前端提示（E-03） |
| `last_login_at` | timestamptz | 是 | — | FR-103 登录态相关 |
| `version` | integer | 否 | 0 | 乐观锁版本号：状态转换必须条件更新（R-18），禁止先查后改 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，由 trg_users_updated_at 自动刷新） |
| `created_by` | bigint | 是 | — | 审计字段 |
| `updated_by` | bigint | 是 | — | 审计字段 |

**users 的约束与索引**

- 唯一约束：
  - `uq_users_email`：`UNIQUE (email)`
  - `uq_users_username`：`UNIQUE (username)`
- 检查约束：
  - `ck_users_nickname_len`：`CHECK (((char_length((nickname)::text) >= 1) AND (char_length((nickname)::text) <= 20)))`
  - `ck_users_restricted_until`：`CHECK ((((status = 'RESTRICTED'::user_status) AND (restricted_until IS NOT NULL)) OR ((status = 'ACTIVE'::user_status) AND (restricted_until IS NULL))))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_users_created_at`
  - `idx_users_role_status`
  - `idx_users_status_restricted_until`
  - `uq_users_email`
  - `uq_users_username`
  - `users_pkey`

### user_trade_zones

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `user_id` | bigint | 否 | — | 用户（勾选人） |
| `trade_zone_id` | bigint | 否 | — | 仅可勾选 trade_zones 中 is_active 的预设区域，禁止自由输入地址 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**user_trade_zones 的约束与索引**

- 唯一约束：
  - `uq_user_trade_zones`：`UNIQUE (user_id, trade_zone_id)`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）
  - `user_id` → `users.id`（ON DELETE CASCADE）

- 索引：
  - `idx_user_trade_zones_zone`
  - `uq_user_trade_zones`
  - `user_trade_zones_pkey`

## 3. 商品

商品状态为 state-machines.md §2.1 的投影；占用的真值来源见 §4。

### items

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `seller_id` | bigint | 否 | — | 卖家（发布人）；在该商品上成为卖家关系角色 |
| `category_id` | bigint | 否 | — | 分类（预设字典，FR-206） |
| `condition_id` | bigint | 否 | — | 成色（预设字典，FR-207） |
| `trade_zone_id` | bigint | 否 | — | R-15 必须 ∈ 卖家已勾选区域 |
| `title` | varchar(60) | 否 | — | FR-201 标题 1-60 |
| `description` | varchar(1000) | 是 | — | FR-201 描述（可选） |
| `estimated_price` | numeric(12,2) | 否 | — | 卖家自填估价（FR-208），平台不定价、不校验合理性（R-13）；下单/发起交换时须快照 |
| `status` | item_status | 否 | 'IN_SALE'::item_status | RESERVED=订单占用，SWAPPING=交换占用，两者互斥（R-23/C-01），由 uq_item_occ_active_item 保证 |
| `swap_preference` | swap_preference | 否 | 'NOT_ACCEPTING'::swap_preference | 卖家交换意愿（R-20）；占用态与终态不可改为 ACCEPTING |
| `published_at` | timestamptz | 是 | — | I-01/I-02 发布时刻（列表排序依据） |
| `banned_reason` | varchar(255) | 是 | — | I-14/I-15 违规下架原因（R-34 必填） |
| `banned_at` | timestamptz | 是 | — | 违规下架时间 |
| `banned_by` | bigint | 是 | — | 处置管理员 |
| `version` | integer | 否 | 0 | R-18 乐观锁：商品占用必须条件更新 |
| `deleted_at` | timestamptz | 是 | — | 软删除；SOLD/BANNED 终态保护（I-17） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**items 的约束与索引**

- 检查约束：
  - `ck_items_banned_trace`：`CHECK (((status <> 'BANNED'::item_status) OR ((banned_reason IS NOT NULL) AND (banned_at IS NOT NULL) AND (banned_by IS NOT NULL))))`
  - `ck_items_price_nonneg`：`CHECK ((estimated_price >= (0)::numeric))`
  - `ck_items_published_when_on_sale`：`CHECK (((status <> ALL (ARRAY['IN_SALE'::item_status, 'RESERVED'::item_status, 'SWAPPING'::item_status])) OR (published_at IS NOT NULL)))`
  - `ck_items_title_len`：`CHECK (((char_length((title)::text) >= 1) AND (char_length((title)::text) <= 60)))`
- 外键：
  - `banned_by` → `users.id`（ON DELETE RESTRICT）
  - `category_id` → `categories.id`（ON DELETE RESTRICT）
  - `condition_id` → `item_conditions.id`（ON DELETE RESTRICT）
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `seller_id` → `users.id`（ON DELETE RESTRICT）
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_items_category_status`
  - `idx_items_condition`
  - `idx_items_seller_status`
  - `idx_items_status_published`
  - `idx_items_swap_pref`
  - `idx_items_title_lower`
  - `idx_items_zone_status`
  - `items_pkey`

### item_images

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键 |
| `item_id` | bigint | 否 | — | 所属商品 |
| `image_url` | varchar(255) | 否 | — | 本地存储路径（OOS-18 不使用对象存储） |
| `sort_order` | integer | 否 | 0 | 0 为封面（首图）；调整顺序即调整封面 |
| `file_size` | integer | 是 | — | 单张 ≤5MB（A-05）；格式限 JPG/PNG/WebP |
| `content_type` | varchar(32) | 是 | — | image/jpeg \| image/png \| image/webp（A-05） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**item_images 的约束与索引**

- 唯一约束：
  - `uq_item_images_sort`：`UNIQUE (item_id, sort_order)`
- 检查约束：
  - `ck_item_images_size`：`CHECK (((file_size IS NULL) OR (file_size <= 5242880)))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE CASCADE）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_item_images_item`
  - `item_images_pkey`
  - `uq_item_images_sort`

## 4. 占用台账（C-01 / R-16 / R-23 核心）

「一物一占用 + 订单/交换互斥」由部分唯一索引 uq_item_occ_active_item 在数据库级保证。

### item_occupations

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `item_id` | bigint | 否 | — | 关联商品 |
| `ref_type` | occupation_type | 否 | — | ORDER \| SWAP |
| `ref_id` | bigint | 否 | — | 多态引用：ref_type=ORDER 时指向 orders.id，=SWAP 时指向 swap_proposals.id（不使用外键，见 design/README.md D-03） |
| `occupied_at` | timestamptz | 否 | now() | R-16 占用发生在下单/发起交换时 |
| `released_at` | timestamptz | 是 | — | 释放幂等锚点（IDEM-B）：重复释放不得再改此行，也不得二次释放 |
| `release_reason` | varchar(64) | 是 | — | 释放原因，便于审计与对账 |
| `version` | integer | 否 | 0 | R-18：释放/占用推进使用条件更新 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**item_occupations 的约束与索引**

- 检查约束：
  - `ck_item_occupations_release`：`CHECK ((((released_at IS NULL) AND (release_reason IS NULL)) OR ((released_at IS NOT NULL) AND (release_reason IS NOT NULL))))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_item_occ_active_occupied`
  - `idx_item_occ_item_history`
  - `idx_item_occ_ref`
  - `item_occupations_pkey`
  - `uq_item_occ_active_item`

## 5. 购物车与订单

订单占用发生在下单时（R-16）；付款为模拟付款（R-08 / OOS-01）。

### cart_items

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `user_id` | bigint | 否 | — | 买家 |
| `item_id` | bigint | 否 | — | 关联商品 |
| `quantity` | smallint | 否 | 1 | A-16：闲置交易单件 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**cart_items 的约束与索引**

- 唯一约束：
  - `uq_cart_items_user_item`：`UNIQUE (user_id, item_id)`
- 检查约束：
  - `ck_cart_items_quantity`：`CHECK ((quantity = 1))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE CASCADE）
  - `updated_by` → `users.id`（ON DELETE SET NULL）
  - `user_id` → `users.id`（ON DELETE CASCADE）

- 索引：
  - `cart_items_pkey`
  - `idx_cart_items_item`
  - `uq_cart_items_user_item`

### orders

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `order_no` | varchar(32) | 否 | — | 业务单号，对外展示 |
| `buyer_id` | bigint | 否 | — | 买家（下单人） |
| `seller_id` | bigint | 否 | — | C-02/R-05 一单一卖家：跨卖家提交时按卖家拆成多笔订单 |
| `trade_zone_id` | bigint | 否 | — | R-15 |
| `pickup_point_id` | bigint | 是 | — | R-06 公共取货点，必须属于本订单 trade_zone_id（应用层校验） |
| `pickup_point_selected_at` | timestamptz | 是 | — | 公共取货点选择时间（O-05） |
| `status` | order_status | 否 | 'PENDING_PAYMENT'::order_status | 订单状态机 §3.1 |
| `estimated_total` | numeric(12,2) | 否 | 0 | 估价合计快照（R-13 仅展示） |
| `expires_at` | timestamptz | 否 | — | R-07 付款截止绝对时间（服务端时间，A-01=30 分钟）；定时任务扫描 status=PENDING_PAYMENT AND now()>=expires_at |
| `paid_at` | timestamptz | 是 | — | R-08 |
| `delivered_at` | timestamptz | 是 | — | O-06 |
| `completed_at` | timestamptz | 是 | — | O-07 |
| `cancelled_at` | timestamptz | 是 | — | 取消时间（O-03/O-04/O-11） |
| `cancel_reason` | order_cancel_reason | 是 | — | O-03/O-04/O-11 三条取消路径 |
| `cancel_note` | varchar(255) | 是 | — | 取消补充说明（如系统超时说明） |
| `review_status` | review_status | 否 | 'OPEN'::review_status | 评价状态机 §8（订单维度） |
| `review_deadline` | timestamptz | 是 | — | A-07 互评窗口 7 天 |
| `version` | integer | 否 | 0 | R-18 条件更新：付款与超时取消并发时 UPDATE ... WHERE status=PENDING_PAYMENT 只允许一方成功 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**orders 的约束与索引**

- 唯一约束：
  - `uq_orders_order_no`：`UNIQUE (order_no)`
- 检查约束：
  - `ck_orders_cancel_trace`：`CHECK (((status = ANY (ARRAY['CANCELLED'::order_status, 'TIMEOUT_CANCELLED'::order_status])) = ((cancelled_at IS NOT NULL) AND (cancel_reason IS NOT NULL))))`
  - `ck_orders_completed`：`CHECK (((status <> 'COMPLETED'::order_status) OR (completed_at IS NOT NULL)))`
  - `ck_orders_delivered`：`CHECK (((status = ANY (ARRAY['PENDING_PAYMENT'::order_status, 'PAID'::order_status, 'CANCELLED'::order_status, 'TIMEOUT_CANCELLED'::order_status])) OR (delivered_at IS NOT NULL)))`
  - `ck_orders_paid_at`：`CHECK (((status = ANY (ARRAY['PENDING_PAYMENT'::order_status, 'CANCELLED'::order_status, 'TIMEOUT_CANCELLED'::order_status])) OR (paid_at IS NOT NULL)))`
  - `ck_orders_pickup_pair`：`CHECK ((((pickup_point_id IS NULL) AND (pickup_point_selected_at IS NULL)) OR ((pickup_point_id IS NOT NULL) AND (pickup_point_selected_at IS NOT NULL))))`
  - `ck_orders_total_nonneg`：`CHECK ((estimated_total >= (0)::numeric))`
- 外键：
  - `buyer_id` → `users.id`（ON DELETE RESTRICT）
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `pickup_point_id` → `pickup_points.id`（ON DELETE RESTRICT）
  - `seller_id` → `users.id`（ON DELETE RESTRICT）
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_orders_buyer_status`
  - `idx_orders_pickup_point`
  - `idx_orders_review_window`
  - `idx_orders_seller_status`
  - `idx_orders_status_expires`
  - `idx_orders_trade_zone`
  - `orders_pkey`
  - `uq_orders_order_no`

### order_items

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `order_id` | bigint | 否 | — | 关联订单 |
| `item_id` | bigint | 否 | — | 关联商品 |
| `title_snapshot` | varchar(60) | 否 | — | R-13 快照：商品后续编辑不影响历史订单 |
| `price_snapshot` | numeric(12,2) | 否 | — | 下单时刻估价快照，商品后续编辑不影响本行（R-13） |
| `condition_snapshot` | varchar(32) | 是 | — | 成色快照（展示用） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**order_items 的约束与索引**

- 唯一约束：
  - `uq_order_items_order_item`：`UNIQUE (order_id, item_id)`
- 检查约束：
  - `ck_order_items_price_nonneg`：`CHECK ((price_snapshot >= (0)::numeric))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `order_id` → `orders.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_order_items_active`
  - `idx_order_items_item`
  - `order_items_pkey`
  - `uq_order_items_order_item`

### payments

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `order_id` | bigint | 否 | — | 关联订单 |
| `payer_id` | bigint | 否 | — | 付款人（模拟付款） |
| `channel` | payment_channel | 否 | 'MOCK'::payment_channel | 本迭代仅 MOCK；未来接入真实支付时新增枚举值即可（不改状态机） |
| `status` | payment_status | 否 | 'SUCCEEDED'::payment_status | 状态字段（取值见对应状态机） |
| `amount` | numeric(12,2) | 否 | — | 估价合计（非真实扣款） |
| `paid_at` | timestamptz | 否 | now() | 模拟付款时间（R-08） |
| `failure_reason` | varchar(255) | 是 | — | 失败原因（模拟付款失败时） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**payments 的约束与索引**

- 检查约束：
  - `ck_payments_amount_nonneg`：`CHECK ((amount >= (0)::numeric))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `order_id` → `orders.id`（ON DELETE RESTRICT）
  - `payer_id` → `users.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_payments_payer`
  - `payments_pkey`
  - `uq_payments_order_succeeded`

## 6. 交换（交换包 → 交换提议 → 交换码）

整包接受（R-22）与整笔作废（R-24）：无任何「部分接受」字段；反提案仅一轮由 counter_depth 限制。

### swap_bundles

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `owner_id` | bigint | 否 | — | SB-02 包主 |
| `trade_zone_id` | bigint | 否 | — | R-15 |
| `title` | varchar(60) | 是 | — | 可选命名，便于识别 |
| `status` | swap_bundle_status | 否 | 'DRAFT'::swap_bundle_status | IN_SWAP=被进行中提议引用；OCCUPIED=包内存在被占用物品导致不可用（§5.1 说明） |
| `estimated_total` | numeric(12,2) | 否 | 0 | FR-403 估价合计；OOS-03 明确不做现金补差价，故无差额字段 |
| `packed_at` | timestamptz | 是 | — | B-04 打包完成时刻 |
| `completed_at` | timestamptz | 是 | — | B-06 |
| `released_at` | timestamptz | 是 | — | B-07 |
| `version` | integer | 否 | 0 | R-18 |
| `deleted_at` | timestamptz | 是 | — | 软删除（B-11）；被提议引用过的包禁止物理删除 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**swap_bundles 的约束与索引**

- 检查约束：
  - `ck_swap_bundles_packed_trace`：`CHECK (((status = 'DRAFT'::swap_bundle_status) OR (packed_at IS NOT NULL)))`
  - `ck_swap_bundles_total_nonneg`：`CHECK ((estimated_total >= (0)::numeric))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `owner_id` → `users.id`（ON DELETE RESTRICT）
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_swap_bundles_owner_status`
  - `idx_swap_bundles_zone`
  - `swap_bundles_pkey`

### swap_bundle_items

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `swap_bundle_id` | bigint | 否 | — | 关联交换包 |
| `item_id` | bigint | 否 | — | 关联商品 |
| `item_status` | bundle_item_status | 否 | 'ACTIVE'::bundle_item_status | ACTIVE=进行中（占用中） |
| `price_snapshot` | numeric(12,2) | 否 | — | R-13 加入包时估价快照 |
| `released_at` | timestamptz | 是 | — | 释放时间；非空表示该占用/关联已终结 |
| `release_reason` | varchar(64) | 是 | — | 释放原因（审计与对账用） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**swap_bundle_items 的约束与索引**

- 唯一约束：
  - `uq_swap_bundle_items_bundle_item`：`UNIQUE (swap_bundle_id, item_id)`
- 检查约束：
  - `ck_swap_bundle_items_price_nonneg`：`CHECK ((price_snapshot >= (0)::numeric))`
  - `ck_swap_bundle_items_release`：`CHECK ((((item_status = 'ACTIVE'::bundle_item_status) AND (released_at IS NULL)) OR ((item_status <> 'ACTIVE'::bundle_item_status) AND (released_at IS NOT NULL))))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `swap_bundle_id` → `swap_bundles.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_swap_bundle_items_bundle`
  - `idx_swap_bundle_items_item`
  - `swap_bundle_items_pkey`
  - `uq_swap_bundle_items_active`
  - `uq_swap_bundle_items_bundle_item`

### swap_proposals

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `proposal_no` | varchar(32) | 否 | — | 交换提议业务编号，对外展示 |
| `proposer_id` | bigint | 否 | — | 发起方用户（贡献交换包的一方） |
| `target_id` | bigint | 否 | — | 目标方用户（其商品被请求的一方） |
| `proposer_bundle_id` | bigint | 否 | — | 发起方交换包 |
| `target_bundle_id` | bigint | 是 | — | 仅反提案时使用 |
| `parent_proposal_id` | bigint | 是 | — | 反提案来源（R-28 仅一轮） |
| `trade_zone_id` | bigint | 否 | — | R-15 同区域 |
| `status` | swap_proposal_status | 否 | 'PENDING'::swap_proposal_status | 交换提议状态机 §4.1 |
| `termination_reason` | swap_termination_reason | 是 | — | R-24 整笔作废 / 拒绝 / 过期 / 取消 原因 |
| `counter_depth` | smallint | 否 | 0 | R-28/A-10：0=原提议，1=反提案；CHECK 限制 ≤1，数据库级禁止「反提案的反提案」 |
| `message` | varchar(500) | 是 | — | 发起说明（结构化交互，不做聊天 OOS-08） |
| `estimated_total_offer` | numeric(12,2) | 否 | 0 | 发起方包估价合计快照（仅展示） |
| `estimated_total_want` | numeric(12,2) | 否 | 0 | 目标方商品估价合计快照（仅展示） |
| `expires_at` | timestamptz | 否 | — | R-25/A-02：created_at + 72h，绝对时间；已 ACCEPTED 的提议不参与过期 |
| `accepted_at` | timestamptz | 是 | — | S-02 整包接受时刻 |
| `completed_at` | timestamptz | 是 | — | S-10 交换完成时刻 |
| `proposer_confirmed_at` | timestamptz | 是 | — | R-27 双方确认：与 target_confirmed_at 齐备才允许置 COMPLETED（见 ck_swap_proposals_completed_trace） |
| `target_confirmed_at` | timestamptz | 是 | — | R-27 目标方确认（2/2） |
| `version` | integer | 否 | 0 | R-18：接受/拒绝/过期并发只允许一方成功 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**swap_proposals 的约束与索引**

- 唯一约束：
  - `uq_swap_proposals_proposal_no`：`UNIQUE (proposal_no)`
- 检查约束：
  - `ck_swap_proposals_accepted_trace`：`CHECK (((status <> 'ACCEPTED'::swap_proposal_status) OR (accepted_at IS NOT NULL)))`
  - `ck_swap_proposals_completed_trace`：`CHECK (((status <> 'COMPLETED'::swap_proposal_status) OR ((completed_at IS NOT NULL) AND (proposer_confirmed_at IS NOT NULL) AND (target_confirmed_at IS NOT NULL))))`
  - `ck_swap_proposals_counter_depth`：`CHECK (((counter_depth >= 0) AND (counter_depth <= 1)))`
  - `ck_swap_proposals_counter_source`：`CHECK ((((counter_depth = 0) AND (parent_proposal_id IS NULL)) OR ((counter_depth = 1) AND (parent_proposal_id IS NOT NULL) AND (target_bundle_id IS NOT NULL))))`
  - `ck_swap_proposals_parties`：`CHECK ((proposer_id <> target_id))`
  - `ck_swap_proposals_termination_trace`：`CHECK (((status <> ALL (ARRAY['REJECTED'::swap_proposal_status, 'EXPIRED'::swap_proposal_status, 'CANCELLED'::swap_proposal_status, 'COUNTERED'::swap_proposal_status])) OR (termination_reason IS NOT NULL)))`
  - `ck_swap_proposals_totals_nonneg`：`CHECK (((estimated_total_offer >= (0)::numeric) AND (estimated_total_want >= (0)::numeric)))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `parent_proposal_id` → `swap_proposals.id`（ON DELETE RESTRICT）
  - `proposer_bundle_id` → `swap_bundles.id`（ON DELETE RESTRICT）
  - `proposer_id` → `users.id`（ON DELETE RESTRICT）
  - `target_bundle_id` → `swap_bundles.id`（ON DELETE RESTRICT）
  - `target_id` → `users.id`（ON DELETE RESTRICT）
  - `trade_zone_id` → `trade_zones.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_swap_proposals_bundle`
  - `idx_swap_proposals_parent`
  - `idx_swap_proposals_proposer_status`
  - `idx_swap_proposals_status_expires`
  - `idx_swap_proposals_target_bundle`
  - `idx_swap_proposals_target_status`
  - `idx_swap_proposals_zone`
  - `swap_proposals_pkey`
  - `uq_swap_proposals_proposal_no`

### swap_proposal_items

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `swap_proposal_id` | bigint | 否 | — | 关联交换提议 |
| `item_id` | bigint | 否 | — | 关联商品 |
| `title_snapshot` | varchar(60) | 否 | — | R-13 快照 |
| `price_snapshot` | numeric(12,2) | 否 | — | R-13 快照 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**swap_proposal_items 的约束与索引**

- 唯一约束：
  - `uq_swap_proposal_items_proposal_item`：`UNIQUE (swap_proposal_id, item_id)`
- 检查约束：
  - `ck_swap_proposal_items_price_nonneg`：`CHECK ((price_snapshot >= (0)::numeric))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `swap_proposal_id` → `swap_proposals.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_swap_proposal_items_item`
  - `idx_swap_proposal_items_proposal`
  - `swap_proposal_items_pkey`
  - `uq_swap_proposal_items_proposal_item`

### swap_codes

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `swap_proposal_id` | bigint | 否 | — | 关联交换提议 |
| `code` | varchar(16) | 否 | — | Q-30 待确认格式；仅提议双方可见，禁止出现在公开列表接口 |
| `status` | swap_code_status | 否 | 'ISSUED'::swap_code_status | 状态字段（取值见对应状态机） |
| `verified_at` | timestamptz | 是 | — | S-08 核销时刻 |
| `verified_by` | bigint | 是 | — | 核销人（提议双方之一） |
| `expires_at` | timestamptz | 是 | — | 过期绝对时间（服务端时间为准，R-19） |
| `version` | integer | 否 | 0 | R-18：并发核销只允许一方成功 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**swap_codes 的约束与索引**

- 唯一约束：
  - `uq_swap_codes_code`：`UNIQUE (code)`
  - `uq_swap_codes_proposal`：`UNIQUE (swap_proposal_id)`
- 检查约束：
  - `ck_swap_codes_verified_trace`：`CHECK ((((status = 'ISSUED'::swap_code_status) AND (verified_at IS NULL) AND (verified_by IS NULL)) OR ((status <> 'ISSUED'::swap_code_status) AND (((status = 'VERIFIED'::swap_code_status) AND (verified_at IS NOT NULL) AND (verified_by IS NOT NULL)) OR ((status = 'EXPIRED'::swap_code_status) AND (verified_at IS NULL))))))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `swap_proposal_id` → `swap_proposals.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）
  - `verified_by` → `users.id`（ON DELETE RESTRICT）

- 索引：
  - `idx_swap_codes_status`
  - `swap_codes_pkey`
  - `uq_swap_codes_code`
  - `uq_swap_codes_proposal`

## 7. 评价与举报

评价前置为交易完成（R-14）；举报未处理唯一由部分唯一索引保证（C-06）。

### reviews

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `order_id` | bigint | 是 | — | 与 swap_proposal_id 二选一（ck_reviews_exactly_one_target） |
| `swap_proposal_id` | bigint | 是 | — | 关联交换提议 |
| `reviewer_id` | bigint | 否 | — | 评价提交人 |
| `reviewee_id` | bigint | 否 | — | 被评价人 |
| `reviewer_role` | review_role | 否 | — | BUYER \| SELLER（R-14） |
| `rating` | smallint | 否 | — | 1–5 |
| `content` | varchar(500) | 是 | — | 评价内容（可选） |
| `version` | integer | 否 | 0 | R-18：评价入口关闭与提交并发时条件更新 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**reviews 的约束与索引**

- 检查约束：
  - `ck_reviews_exactly_one_target`：`CHECK ((((order_id IS NOT NULL) AND (swap_proposal_id IS NULL)) OR ((order_id IS NULL) AND (swap_proposal_id IS NOT NULL))))`
  - `ck_reviews_not_self`：`CHECK ((reviewer_id <> reviewee_id))`
  - `ck_reviews_rating`：`CHECK (((rating >= 1) AND (rating <= 5)))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `order_id` → `orders.id`（ON DELETE RESTRICT）
  - `reviewee_id` → `users.id`（ON DELETE RESTRICT）
  - `reviewer_id` → `users.id`（ON DELETE RESTRICT）
  - `swap_proposal_id` → `swap_proposals.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_reviews_reviewee`
  - `idx_reviews_reviewer`
  - `reviews_pkey`
  - `uq_reviews_order_reviewer`
  - `uq_reviews_swap_reviewer`

### reports

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `reporter_id` | bigint | 否 | — | 举报人 |
| `item_id` | bigint | 否 | — | 关联商品 |
| `reason` | report_reason | 否 | — | FR-211 需选择原因 |
| `detail` | varchar(500) | 是 | — | 举报补充说明 |
| `status` | report_status | 否 | 'PENDING'::report_status | 状态字段（取值见对应状态机） |
| `handled_by` | bigint | 是 | — | 处理管理员 |
| `handled_at` | timestamptz | 是 | — | 举报处理时间 |
| `handle_note` | varchar(255) | 是 | — | 举报处理结论 |
| `version` | integer | 否 | 0 | R-18 乐观锁/条件更新版本号 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**reports 的约束与索引**

- 检查约束：
  - `ck_reports_handled_trace`：`CHECK ((((status = 'PENDING'::report_status) AND (handled_by IS NULL) AND (handled_at IS NULL)) OR ((status <> 'PENDING'::report_status) AND (handled_by IS NOT NULL) AND (handled_at IS NOT NULL))))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `handled_by` → `users.id`（ON DELETE RESTRICT）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `reporter_id` → `users.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_reports_item_status`
  - `idx_reports_reporter`
  - `idx_reports_status_created`
  - `reports_pkey`
  - `uq_reports_pending_reporter_item`

## 8. 治理：处罚与申诉

记录永不物理删除（R-32）：这两张表没有 deleted_at，只有状态字段。

### penalties

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `target_user_id` | bigint | 否 | — | 被处罚用户 |
| `operator_id` | bigint | 否 | — | 操作管理员 |
| `penalty_type` | penalty_type | 否 | — | BAN_3D 影响用户交易能力；ITEM_TAKEDOWN 仅下架商品并留痕举证 |
| `item_id` | bigint | 是 | — | ITEM_TAKEDOWN 关联商品 |
| `reason` | varchar(255) | 否 | — | R-32/R-34：治理动作必须填原因 |
| `status` | penalty_status | 否 | 'ACTIVE'::penalty_status | APPEALING 期间处罚仍生效（不停表，P-03），仅用于标记申诉进行中 |
| `started_at` | timestamptz | 否 | now() | 处罚开始时间 |
| `ends_at` | timestamptz | 否 | — | R-30/A-03：started_at + 72h（固定三天，Q-09 假设不可调） |
| `lifted_at` | timestamptz | 是 | — | 解封时刻 |
| `lift_source` | penalty_lift_source | 是 | — | 解封来源（系统/申诉/管理员手动） |
| `lift_reason` | varchar(255) | 是 | — | 解封原因或说明 |
| `version` | integer | 否 | 0 | R-18：到期自动解封 vs 手动解封并发只允许一方成功 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**penalties 的约束与索引**

- 检查约束：
  - `ck_penalties_item_by_type`：`CHECK ((((penalty_type = 'ITEM_TAKEDOWN'::penalty_type) AND (item_id IS NOT NULL)) OR ((penalty_type = 'BAN_3D'::penalty_type) AND (item_id IS NULL))))`
  - `ck_penalties_lift_trace`：`CHECK ((((status = ANY (ARRAY['ACTIVE'::penalty_status, 'APPEALING'::penalty_status])) AND (lifted_at IS NULL) AND (lift_source IS NULL)) OR ((status = ANY (ARRAY['LIFTED'::penalty_status, 'EXPIRED'::penalty_status])) AND (lifted_at IS NOT NULL) AND (lift_source IS NOT NULL))))`
  - `ck_penalties_period`：`CHECK ((ends_at > started_at))`
  - `ck_penalties_reason_not_blank`：`CHECK ((btrim((reason)::text) <> ''::text))`
- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `item_id` → `items.id`（ON DELETE RESTRICT）
  - `operator_id` → `users.id`（ON DELETE RESTRICT）
  - `target_user_id` → `users.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_penalties_active_ends`
  - `idx_penalties_item`
  - `idx_penalties_operator`
  - `idx_penalties_target`
  - `idx_penalties_type_status`
  - `penalties_pkey`
  - `uq_penalties_one_active`

### appeals

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `penalty_id` | bigint | 否 | — | 关联处罚记录 |
| `appellant_id` | bigint | 否 | — | 申诉人（须为被处罚用户） |
| `reason` | varchar(1000) | 否 | — | AP-01 申诉理由必填；提交后处罚仍生效（P-03） |
| `status` | appeal_status | 否 | 'SUBMITTED'::appeal_status | 状态字段（取值见对应状态机） |
| `handler_id` | bigint | 是 | — | 处理管理员 |
| `resolution_note` | varchar(1000) | 是 | — | AP-02/AP-03 必须填处理意见 |
| `resolved_at` | timestamptz | 是 | — | 申诉处理完成时间 |
| `version` | integer | 否 | 0 | R-18：重复处理必须条件更新 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**appeals 的约束与索引**

- 检查约束：
  - `ck_appeals_reason_not_blank`：`CHECK ((btrim((reason)::text) <> ''::text))`
  - `ck_appeals_resolution_trace`：`CHECK ((((status = 'SUBMITTED'::appeal_status) AND (resolved_at IS NULL) AND (handler_id IS NULL)) OR ((status <> 'SUBMITTED'::appeal_status) AND (resolved_at IS NOT NULL) AND (handler_id IS NOT NULL) AND (resolution_note IS NOT NULL) AND (btrim((resolution_note)::text) <> ''::text))))`
- 外键：
  - `appellant_id` → `users.id`（ON DELETE RESTRICT）
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `handler_id` → `users.id`（ON DELETE RESTRICT）
  - `penalty_id` → `penalties.id`（ON DELETE RESTRICT）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `appeals_pkey`
  - `idx_appeals_appellant`
  - `idx_appeals_handler`
  - `idx_appeals_status_created`
  - `uq_appeals_penalty`

## 9. 通知

站内通知（FR-507）；Q-05 待确认是否仅站内。

### notifications

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `recipient_id` | bigint | 否 | — | 通知接收人 |
| `actor_id` | bigint | 是 | — | 触发者（系统通知为空） |
| `type` | notification_type | 否 | — | 类型枚举 |
| `title` | varchar(120) | 否 | — | 标题 |
| `body` | varchar(500) | 是 | — | 正文内容 |
| `ref_type` | varchar(32) | 是 | — | 多态业务引用，无外键（D-03）；用于前端跳转到对应详情页 |
| `ref_id` | bigint | 是 | — | 多态业务引用 ID（配合 ref_type 使用） |
| `is_read` | boolean | 否 | false | 是否已读 |
| `read_at` | timestamptz | 是 | — | 已读时间 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**notifications 的约束与索引**

- 检查约束：
  - `ck_notifications_read_pair`：`CHECK ((((is_read = false) AND (read_at IS NULL)) OR ((is_read = true) AND (read_at IS NOT NULL))))`
  - `ck_notifications_ref_pair`：`CHECK ((((ref_type IS NULL) AND (ref_id IS NULL)) OR ((ref_type IS NOT NULL) AND (ref_id IS NOT NULL))))`
- 外键：
  - `actor_id` → `users.id`（ON DELETE SET NULL）
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `recipient_id` → `users.id`（ON DELETE CASCADE）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `idx_notifications_recipient_all`
  - `idx_notifications_recipient_unread`
  - `idx_notifications_ref`
  - `notifications_pkey`

## 10. 幂等与审计

幂等键层（R-17）；治理动作留痕（R-34）。

### idempotency_records

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `user_id` | bigint | 否 | — | 关联用户 |
| `idempotency_key` | varchar(64) | 否 | — | A-15：前端生成 UUID v4 |
| `endpoint` | varchar(255) | 否 | — | 接口标识，如 POST /orders |
| `request_hash` | varchar(64) | 否 | — | E-16：同键不同请求体必须返回冲突，禁止静默回放他人请求结果 |
| `status` | idempotency_status | 否 | 'IN_PROGRESS'::idempotency_status | 状态字段（取值见对应状态机） |
| `response_status` | integer | 是 | — | 首次响应 HTTP 状态码 |
| `response_body` | jsonb | 是 | — | 首次响应体快照，重复请求直接回放 |
| `resource_type` | varchar(32) | 是 | — | 首次产生的资源类型（ORDER/SWAP/...） |
| `resource_id` | bigint | 是 | — | 首次产生的资源 ID |
| `error_message` | varchar(500) | 是 | — | 失败原因（幂等记录） |
| `version` | integer | 否 | 0 | R-18：并发同键请求以 INSERT ... ON CONFLICT 抢占后再推进 |
| `expires_at` | timestamptz | 否 | (now() + '24:00:00'::interval) | A-08 建议保留 24 小时；过期记录由定时任务清理 |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**idempotency_records 的约束与索引**

- 外键：
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `updated_by` → `users.id`（ON DELETE SET NULL）
  - `user_id` → `users.id`（ON DELETE CASCADE）

- 索引：
  - `idempotency_records_pkey`
  - `idx_idempotency_endpoint`
  - `idx_idempotency_expires`
  - `idx_idempotency_resource`
  - `uq_idempotency_user_key`

### audit_logs

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | bigint | 否 | — | 主键（IDENTITY 自增） |
| `actor_id` | bigint | 是 | — | NULL = 系统定时任务（ROLE_SYSTEM） |
| `actor_role` | varchar(32) | 是 | — | ADMIN \| SYSTEM \| USER |
| `action` | varchar(64) | 否 | — | ITEM_BAN / ITEM_RESTORE / USER_BAN / USER_UNBAN / APPEAL_RESOLVE / REPORT_HANDLE ... |
| `target_type` | varchar(32) | 否 | — | ITEM \| USER \| PENALTY \| APPEAL \| REPORT \| ORDER \| SWAP_PROPOSAL |
| `target_id` | bigint | 否 | — | 审计对象 ID |
| `reason` | varchar(255) | 是 | — | R-34：下架/封禁/恢复/申诉处理的原因或意见，应用层必须强制非空 |
| `before_state` | jsonb | 是 | — | 变更前状态快照 |
| `after_state` | jsonb | 是 | — | 变更后状态快照 |
| `ip_address` | varchar(45) | 是 | — | 请求来源 IP（审计） |
| `user_agent` | varchar(255) | 是 | — | 请求 User-Agent（审计） |
| `created_at` | timestamptz | 否 | now() | 创建时间（审计字段） |
| `updated_at` | timestamptz | 否 | now() | 更新时间（审计字段，触发器自动刷新） |
| `created_by` | bigint | 是 | — | 创建人（审计字段） |
| `updated_by` | bigint | 是 | — | 更新人（审计字段） |

**audit_logs 的约束与索引**

- 外键：
  - `actor_id` → `users.id`（ON DELETE SET NULL）
  - `created_by` → `users.id`（ON DELETE SET NULL）
  - `updated_by` → `users.id`（ON DELETE SET NULL）

- 索引：
  - `audit_logs_pkey`
  - `idx_audit_logs_action`
  - `idx_audit_logs_actor`
  - `idx_audit_logs_created_at`
  - `idx_audit_logs_target`

---

## 11. 列注释完整性

| 指标 | 值 |
| --- | --- |
| 业务表 | 25 |
| 业务列总数 | 324 |
| 有列注释的列 | 324（100%） |
| 表注释 | 25 / 25 |

> 全部列均有 `COMMENT ON COLUMN`，可用 `\d+ <table>` 在 psql 中直接查看。

---

## 12. 版本与变更记录

| 版本 | 日期 | 变更人 | 变更内容 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 2026-10-12 | 数据库与领域模型设计负责人 | 初稿：25 张业务表、324 列字段字典、约束与索引清单，由迁移后数据库导出 | 草稿（待确认） |
