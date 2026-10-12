-- =====================================================================
-- 迭代一 数据库初始化脚本
-- =====================================================================
-- 文件     : V1__init_schema.sql
-- 版本     : v1.0
-- 日期     : 2026-10-12
-- 作者     : 数据库与领域模型设计负责人（待填写姓名）
-- 状态     : 草稿（待确认）
-- 目标库   : PostgreSQL 15
-- 依据文档 : docs/iteration1/requirements.md      （54 FR / 约束 C-01~C-07 / 错误场景 E-01~E-16）
--            docs/iteration1/state-machines.md    （6 大状态机 + 评价状态机，枚举值以此为准）
--            docs/iteration1/rules.md             （R-01~R-35，幂等/并发/占用/作废/解封）
--            docs/iteration1/permission-matrix.md （7 主体权限边界）
--            docs/iteration1/design/schema.md     （字段字典，与本脚本一一对应）
--
-- 设计要点（继承迭代一关键决策）：
--   1. 占用发生在下单时（R-16）：orders 创建即写 item_occupations 并置 items.status = RESERVED。
--   2. 幂等键 + 条件更新双层防护（R-17/R-18）：idempotency_records 表 + 各表 version 乐观锁列。
--   3. 商品占用互斥（R-23 / C-01）：item_occupations 上部分唯一索引
--      uq_item_occ_active_item，保证「一个商品同时最多一个进行中占用」。
--   4. 交换整包接受（R-22）与整笔作废（R-24）：无任何「部分接受」字段；作废由提议状态整体驱动。
--   5. 订单占用（RESERVED）与交换占用（SWAPPING）互斥：由第 3 点的同一索引统一保证。
--   6. 违规下架联动（R-35）：orders.cancel_reason='ADMIN_TAKEDOWN' + penalties(type=ITEM_TAKEDOWN) 留痕。
--   7. 处罚记录、申诉记录、订单、交换记录永不物理删除（R-32）：
--      这些表不设 deleted_at，删除一律以状态字段表达。
--   8. 时间以服务端为准（R-19）：expires_at 存绝对时间；统一 timestamptz。
--
-- 注意：本脚本为 Flyway 版本化迁移，请勿修改已执行过的版本；变更请新增 V4+。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 扩展
-- ---------------------------------------------------------------------
-- citext 用于 username / email 大小写不敏感唯一（FR-101 用户名唯一、FR-102 登录）
-- 说明：citext 为 PostgreSQL contrib 扩展，官方 postgres:15 镜像已内置。
CREATE EXTENSION IF NOT EXISTS citext;

-- ---------------------------------------------------------------------
-- 1. 枚举类型
--    枚举值严格对齐 docs/iteration1/state-machines.md（验收标准 7）
-- ---------------------------------------------------------------------

-- 用户基础角色（requirements.md §3.1；卖家/买家是关系角色，不落库为角色）
--   USER 注册用户；ADMIN 管理员（不参与交易，P-00-A）
--   被处罚用户不是独立角色，由 users.status = RESTRICTED + penalties 表达
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- 用户账号状态（permission-matrix.md §8；处罚到期由 FR-603 恢复 ACTIVE）
CREATE TYPE user_status AS ENUM ('ACTIVE', 'RESTRICTED');

-- 交易区域类型（requirements.md §2.2）
CREATE TYPE zone_type AS ENUM ('CAMPUS', 'COMMUNITY');

-- 公共取货点类型（requirements.md §2.3）
CREATE TYPE pickup_point_type AS ENUM ('LIBRARY', 'CANTEEN', 'TEACHING', 'DORM', 'GATE', 'SQUARE');

-- 商品状态机（state-machines.md §2.1）
--   DRAFT 草稿（Q-14：默认不使用）；IN_SALE 在售
--   RESERVED 订单占用；SWAPPING 交换占用（两者互斥，R-23）
--   SOLD 终态；OFF_SHELF 卖家主动下架；BANNED 违规下架（不可自行上架，I-16）
CREATE TYPE item_status AS ENUM ('DRAFT', 'IN_SALE', 'RESERVED', 'SWAPPING', 'SOLD', 'OFF_SHELF', 'BANNED');

-- 商品交换意愿（FR-401 / R-20）：详情页「可交换 / 暂不交换」的持久化依据
CREATE TYPE swap_preference AS ENUM ('ACCEPTING', 'NOT_ACCEPTING');

-- 订单状态机（state-machines.md §3.1）
--   PENDING_PAYMENT 待付款（30 分钟倒计时，A-01）
--   PAID 已付款/待取货；DELIVERED 已交付待确认
--   COMPLETED 终态；CANCELLED 买家主动取消；TIMEOUT_CANCELLED 超时取消（R-07）
--   本迭代不做退款（OOS-14），故无退款状态
CREATE TYPE order_status AS ENUM ('PENDING_PAYMENT', 'PAID', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'TIMEOUT_CANCELLED');

-- 订单取消原因（区分 O-03 / O-04 / O-11 三条取消路径）
CREATE TYPE order_cancel_reason AS ENUM ('BUYER_CANCELLED', 'PAYMENT_TIMEOUT', 'ADMIN_TAKEDOWN');

-- 模拟付款渠道（R-08 / OOS-01：仅 MOCK，不接入真实支付）
CREATE TYPE payment_channel AS ENUM ('MOCK');

-- 模拟付款结果（R-08）
CREATE TYPE payment_status AS ENUM ('SUCCEEDED', 'FAILED');

-- 占用来源类型（R-23 / C-01：订单占用与交换占用互斥）
CREATE TYPE occupation_type AS ENUM ('ORDER', 'SWAP');

-- 交换包状态机（state-machines.md §5.1）
CREATE TYPE swap_bundle_status AS ENUM ('DRAFT', 'PACKED', 'IN_SWAP', 'OCCUPIED', 'RELEASED', 'COMPLETED');

-- 交换包物品行状态（SB-03：同一物品同时只能在一个「进行中」包内）
--   ACTIVE 进行中；RELEASED 已释放；COMPLETED 已随交换完成
CREATE TYPE bundle_item_status AS ENUM ('ACTIVE', 'RELEASED', 'COMPLETED');

-- 交换提议状态机（state-machines.md §4.1）
--   PENDING 待处理（72 小时，A-02）；ACCEPTED 已接受（生成交换码，C-05）
--   REJECTED 已拒绝；COUNTERED 已反提案（仅一轮，R-28/A-10）
--   EXPIRED 已过期（R-25）；CANCELLED 已取消（含整笔作废 R-24）；COMPLETED 已完成
CREATE TYPE swap_proposal_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'COUNTERED', 'EXPIRED', 'CANCELLED', 'COMPLETED');

-- 交换提议终结原因（R-24 整笔作废 / S-03 / S-05 / S-06 / S-13 可解释性）
CREATE TYPE swap_termination_reason AS ENUM (
    'TARGET_REJECTED', 'PROPOSER_CANCELLED', 'EXPIRED_72H',
    'VOID_ITEM_OCCUPIED', 'VOID_ITEM_OFF_SHELF', 'VOID_ITEM_BANNED', 'COUNTERED'
);

-- 交换码状态（R-26 / C-05：一次性核销）
CREATE TYPE swap_code_status AS ENUM ('ISSUED', 'VERIFIED', 'EXPIRED');

-- 评价状态机（state-machines.md §8，作用于 order 维度）
CREATE TYPE review_status AS ENUM ('OPEN', 'BUYER_REVIEWED', 'SELLER_REVIEWED', 'BOTH_REVIEWED', 'WINDOW_CLOSED');

-- 评价提交方在订单/交换中的角色（R-14：买卖双方各评一次）
CREATE TYPE review_role AS ENUM ('BUYER', 'SELLER');

-- 举报状态（requirements.md §6 关系图 / C-06）
CREATE TYPE report_status AS ENUM ('PENDING', 'HANDLED', 'DISMISSED');

-- 举报原因（FR-211：需选择原因）
CREATE TYPE report_reason AS ENUM ('FAKE_INFO', 'PROHIBITED_ITEM', 'WRONG_CATEGORY', 'SUSPECTED_FRAUD', 'OTHER');

-- 处罚类型（state-machines.md §6.1 说明）
--   BAN_3D 封禁三天（R-30，固定 72h，A-03）
--   ITEM_TAKEDOWN 违规下架处置（P-02，仅用于举证，不影响用户交易能力）
CREATE TYPE penalty_type AS ENUM ('BAN_3D', 'ITEM_TAKEDOWN');

-- 处罚状态机（state-machines.md §6.1）
--   APPEALING 申诉中，处罚仍生效（不停表，P-03）
CREATE TYPE penalty_status AS ENUM ('ACTIVE', 'APPEALING', 'LIFTED', 'EXPIRED');

-- 处罚解封来源（区分 P-04 系统自动 / P-05 申诉通过 / P-07 管理员手动）
CREATE TYPE penalty_lift_source AS ENUM ('SYSTEM_EXPIRY', 'APPEAL_APPROVED', 'ADMIN_MANUAL');

-- 申诉状态机（state-machines.md §7.1）
CREATE TYPE appeal_status AS ENUM ('SUBMITTED', 'APPROVED', 'REJECTED');

-- 站内通知类型（FR-507；Q-05 待确认是否仅站内）
CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED', 'ORDER_PAID', 'ORDER_DELIVERED', 'ORDER_COMPLETED',
    'ORDER_CANCELLED', 'ORDER_TIMEOUT_CANCELLED', 'ORDER_ADMIN_CANCELLED',
    'SWAP_PROPOSED', 'SWAP_ACCEPTED', 'SWAP_REJECTED', 'SWAP_COUNTERED',
    'SWAP_EXPIRED', 'SWAP_CANCELLED', 'SWAP_VOIDED', 'SWAP_COMPLETED',
    'PENALTY_BANNED', 'PENALTY_LIFTED', 'PENALTY_EXPIRED',
    'ITEM_BANNED', 'ITEM_RESTORED', 'APPEAL_SUBMITTED', 'APPEAL_RESOLVED',
    'REPORT_HANDLED', 'REVIEW_WINDOW_OPENED', 'REVIEW_WINDOW_CLOSED'
);

-- 幂等记录状态（R-17 / E-06 / E-16）
CREATE TYPE idempotency_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'FAILED');

-- ---------------------------------------------------------------------
-- 2. 通用触发器函数：自动维护 updated_at（审计字段要求）
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION set_updated_at() IS '通用触发器：写入时自动刷新 updated_at（审计字段要求）';

-- =====================================================================
-- 3. 参照与字典表（预设数据，用户在界面上只能选择）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.1 trade_zones 交易区域（requirements.md §2.2 / R-15）
--     预设字典，用户只能勾选不能新增；是权限校验、过滤、交付的最小地理单位
-- ---------------------------------------------------------------------
CREATE TABLE trade_zones (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(32)  NOT NULL,                       -- 区域编码，如 CAMPUS_A / COMMUNITY_A1
    name            VARCHAR(64)  NOT NULL,                       -- 区域名称
    zone_type       zone_type    NOT NULL,                       -- CAMPUS / COMMUNITY
    parent_zone_id  BIGINT       NULL REFERENCES trade_zones(id) ON DELETE RESTRICT, -- 社区可归属某校区（可选）
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,          -- 停用后新交易不可选，历史数据保留
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_trade_zones_code UNIQUE (code),
    CONSTRAINT ck_trade_zones_parent_not_self CHECK (parent_zone_id IS NULL OR parent_zone_id <> id)
);

COMMENT ON TABLE  trade_zones IS '交易区域（预设字典）：校区/社区，R-15 交叉区域交易范围；Q-01/Q-02 待确认后替换种子数据';
COMMENT ON COLUMN trade_zones.code IS '区域占位编码：CAMPUS_A / CAMPUS_B / COMMUNITY_A1 等，待用户确认 Q-01/Q-02 后替换';
COMMENT ON COLUMN trade_zones.parent_zone_id IS '可选父区域（如社区归属校区），用于后续「区域组」扩展（OOS-07 迭代 N）';

CREATE INDEX idx_trade_zones_type_active ON trade_zones (zone_type, is_active);
CREATE TRIGGER trg_trade_zones_updated_at BEFORE UPDATE ON trade_zones
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 3.2 pickup_points 公共取货点（requirements.md §2.3 / R-06）
--     归属交易区域；不读定位、不填住址、不做地图（GEO-01~GEO-04）
-- ---------------------------------------------------------------------
CREATE TABLE pickup_points (
    id              BIGINT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(32)        NOT NULL,                 -- 取货点编码，如 PICKUP_LIBRARY_A
    trade_zone_id   BIGINT             NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT,
    name            VARCHAR(64)        NOT NULL,                 -- 取货点名称，如「图书馆正门台阶右侧」
    point_type      pickup_point_type  NOT NULL,
    description     VARCHAR(255)       NULL,                     -- 文字说明（替代地图/导航，OOS-06）
    sort_order      INTEGER            NOT NULL DEFAULT 0,
    is_active       BOOLEAN            NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    CONSTRAINT uq_pickup_points_code UNIQUE (code)
);

COMMENT ON TABLE  pickup_points IS '公共取货点（预设字典）：线下当面交付地点，R-06 要求必须属于订单所在交易区域';
COMMENT ON COLUMN pickup_points.description IS '文字说明替代地图与导航（OOS-06），不使用经纬度';
COMMENT ON COLUMN pickup_points.code IS '占位编码：PICKUP_LIBRARY / PICKUP_CANTEEN 等，待用户确认 Q-03 后替换';

CREATE INDEX idx_pickup_points_zone_active ON pickup_points (trade_zone_id, is_active);
CREATE TRIGGER trg_pickup_points_updated_at BEFORE UPDATE ON pickup_points
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 3.3 categories 商品分类（FR-206：预设分类字典，用户不可自定义 Q-21）
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    code        VARCHAR(32)  NOT NULL,  -- 分类编码（唯一）
    name        VARCHAR(64)  NOT NULL,  -- 分类名称
    sort_order  INTEGER      NOT NULL DEFAULT 0,  -- 排序值（升序展示）
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,  -- 是否启用（停用后不出现在发布页）
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 更新时间（审计字段）
    CONSTRAINT uq_categories_code UNIQUE (code)
);

COMMENT ON TABLE categories IS '商品分类预设字典（FR-206）；仅管理员维护（Q-21），用户不可自定义';

CREATE INDEX idx_categories_active_sort ON categories (is_active, sort_order);
CREATE TRIGGER trg_categories_updated_at BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 3.4 item_conditions 商品成色（FR-207：预设枚举）
--     使用字典表而非 enum，便于管理员调整文案与排序
-- ---------------------------------------------------------------------
CREATE TABLE item_conditions (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    code        VARCHAR(32)  NOT NULL,  -- 成色编码（唯一）
    name        VARCHAR(32)  NOT NULL,  -- 成色名称（如「几乎全新」）
    sort_order  INTEGER      NOT NULL DEFAULT 0,  -- 排序值（由新到旧展示）
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,  -- 是否启用
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 更新时间（审计字段）
    CONSTRAINT uq_item_conditions_code UNIQUE (code)
);

COMMENT ON TABLE item_conditions IS '商品成色预设字典（FR-207）：全新/几乎全新/轻微使用痕迹/明显使用痕迹/有瑕疵';

CREATE INDEX idx_item_conditions_active_sort ON item_conditions (is_active, sort_order);
CREATE TRIGGER trg_item_conditions_updated_at BEFORE UPDATE ON item_conditions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 4. 用户与账户
-- =====================================================================

-- ---------------------------------------------------------------------
-- 4.1 users 用户（FR-101~FR-106；users 即用户与个人资料的 1:1 合并）
--     设计说明：需求文档列出「用户、个人资料」两个实体，但二者严格 1:1 且无独立生命周期，
--     故合并为单表以避免无意义的 JOIN 与空值分支（详见 design/README.md 决策 D-02）。
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    username            CITEXT       NOT NULL,                  -- FR-101 用户名唯一、大小写不敏感
    email               CITEXT       NULL,                      -- FR-101 邮箱（可选，Q-06 无需验证）
    password_hash       VARCHAR(100) NOT NULL,                  -- 密码摘要（不存明文；算法待接口契约冻结）
    nickname            VARCHAR(20)  NOT NULL,                  -- FR-105 昵称 1-20 字符，不唯一（A-04/A-09）
    avatar_url          VARCHAR(255) NULL,                      -- FR-106 头像（1 张）
    role                user_role    NOT NULL DEFAULT 'USER',   -- 管理员为 ADMIN（P-00-A 不参与交易）
    status              user_status  NOT NULL DEFAULT 'ACTIVE', -- RESTRICTED = 被处罚用户（FR-503）
    restricted_until    TIMESTAMPTZ  NULL,                      -- 处罚到期时间，用于前端提示（E-03）
    last_login_at       TIMESTAMPTZ  NULL,                      -- FR-103 登录态相关
    version             INTEGER      NOT NULL DEFAULT 0,        -- R-18 乐观锁/条件更新
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 更新时间（审计字段，由 trg_users_updated_at 自动刷新）
    created_by          BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL, -- 审计字段
    updated_by          BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL, -- 审计字段
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_nickname_len CHECK (char_length(nickname) BETWEEN 1 AND 20),
    -- 状态与受限到期时间必须一致：RESTRICTED 必须有到期时间，ACTIVE 必须为空（R-30/R-31）
    CONSTRAINT ck_users_restricted_until CHECK (
        (status = 'RESTRICTED' AND restricted_until IS NOT NULL)
        OR (status = 'ACTIVE' AND restricted_until IS NULL)
    )
);

COMMENT ON TABLE  users IS '用户表（含个人资料）：FR-101~FR-108；卖家/买家为关系角色不落库（P-00-A/B）';
COMMENT ON COLUMN users.password_hash IS '密码摘要，禁止存明文；哈希算法在接口契约阶段冻结';
COMMENT ON COLUMN users.status IS 'RESTRICTED 表示处于生效中处罚（permission-matrix §8 白名单约束）';
COMMENT ON COLUMN users.version IS '乐观锁版本号：状态转换必须条件更新（R-18），禁止先查后改';

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_status_restricted_until ON users (status, restricted_until)
    WHERE status = 'RESTRICTED';                                 -- 支撑 FR-603 到期自动解封扫描
CREATE INDEX idx_users_created_at ON users (created_at);
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 4.2 user_trade_zones 用户常用交易区域（FR-107 / R-15）
--     1–5 个；不读定位、不填住址（GEO-01/GEO-02）
-- ---------------------------------------------------------------------
CREATE TABLE user_trade_zones (
    id             BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- 用户（勾选人）
    trade_zone_id  BIGINT      NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT,  -- 常用交易区域（须为 is_active 的预设区域）
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),  -- 更新时间（审计字段）
    created_by     BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,  -- 创建人（审计字段）
    updated_by     BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,  -- 更新人（审计字段）
    CONSTRAINT uq_user_trade_zones UNIQUE (user_id, trade_zone_id)
);

COMMENT ON TABLE user_trade_zones IS '用户勾选的常用交易区域（FR-107）：1–5 个，R-15 校验商品/订单/交换必须落在同一区域';
COMMENT ON COLUMN user_trade_zones.trade_zone_id IS '仅可勾选 trade_zones 中 is_active 的预设区域，禁止自由输入地址';

CREATE INDEX idx_user_trade_zones_zone ON user_trade_zones (trade_zone_id);
CREATE TRIGGER trg_user_trade_zones_updated_at BEFORE UPDATE ON user_trade_zones
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 5. 商品
-- =====================================================================

-- ---------------------------------------------------------------------
-- 5.1 items 商品（FR-201~FR-211 / 商品状态机 §2 / R-10~R-13、R-15、R-20）
-- ---------------------------------------------------------------------
CREATE TABLE items (
    id                 BIGINT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    seller_id          BIGINT           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,  -- 卖家（发布人）；在该商品上成为卖家关系角色
    category_id        BIGINT           NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,  -- 分类（预设字典，FR-206）
    condition_id       BIGINT           NOT NULL REFERENCES item_conditions(id) ON DELETE RESTRICT,  -- 成色（预设字典，FR-207）
    trade_zone_id      BIGINT           NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT, -- R-15 必须 ∈ 卖家已勾选区域
    title              VARCHAR(60)      NOT NULL,                -- FR-201 标题 1-60
    description        VARCHAR(1000)    NULL,                    -- FR-201 描述（可选）
    estimated_price    NUMERIC(12,2)    NOT NULL,                -- FR-208 卖家自填估价（R-13 平台不定价）
    status             item_status      NOT NULL DEFAULT 'IN_SALE', -- 商品状态机 §2.1
    swap_preference    swap_preference  NOT NULL DEFAULT 'NOT_ACCEPTING', -- FR-401 / R-20
    published_at       TIMESTAMPTZ      NULL,                    -- I-01/I-02 发布时刻（列表排序依据）
    banned_reason      VARCHAR(255)     NULL,                    -- I-14/I-15 违规下架原因（R-34 必填）
    banned_at          TIMESTAMPTZ      NULL,  -- 违规下架时间
    banned_by          BIGINT           NULL REFERENCES users(id) ON DELETE RESTRICT, -- 处置管理员
    version            INTEGER          NOT NULL DEFAULT 0,      -- R-18 乐观锁：商品占用必须条件更新
    deleted_at         TIMESTAMPTZ      NULL,                    -- 软删除：有交易历史的商品禁止物理删除
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),  -- 更新时间（审计字段）
    created_by         BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,  -- 创建人（审计字段）
    updated_by         BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,  -- 更新人（审计字段）
    CONSTRAINT ck_items_title_len     CHECK (char_length(title) BETWEEN 1 AND 60),
    CONSTRAINT ck_items_price_nonneg  CHECK (estimated_price >= 0),      -- FR-208 / R-13
    -- 违规下架必须留痕（R-34）：BANNED 必须有原因、时间与操作人
    CONSTRAINT ck_items_banned_trace  CHECK (
        status <> 'BANNED'
        OR (banned_reason IS NOT NULL AND banned_at IS NOT NULL AND banned_by IS NOT NULL)
    ),
    -- 占位态一致性（R-16/R-23 的投影）：占用状态必须存在对应进行中占用记录，
    -- 该不变量由应用层在同一事务内维护，并可用 db-verification.md 的对账查询校验
    CONSTRAINT ck_items_published_when_on_sale CHECK (
        status NOT IN ('IN_SALE', 'RESERVED', 'SWAPPING') OR published_at IS NOT NULL
    )
);

COMMENT ON TABLE  items IS '商品表：FR-201~FR-211；status 为 state-machines §2.1 的投影，占用真值见 item_occupations';
COMMENT ON COLUMN items.estimated_price IS '卖家自填估价（FR-208），平台不定价、不校验合理性（R-13）；下单/发起交换时须快照';
COMMENT ON COLUMN items.status IS 'RESERVED=订单占用，SWAPPING=交换占用，两者互斥（R-23/C-01），由 uq_item_occ_active_item 保证';
COMMENT ON COLUMN items.swap_preference IS '卖家交换意愿（R-20）；占用态与终态不可改为 ACCEPTING';
COMMENT ON COLUMN items.deleted_at IS '软删除；SOLD/BANNED 终态保护（I-17）';

CREATE INDEX idx_items_status_published   ON items (status, published_at DESC);   -- PF-01 列表主查询
CREATE INDEX idx_items_zone_status        ON items (trade_zone_id, status);       -- R-15 同区域过滤
CREATE INDEX idx_items_category_status    ON items (category_id, status);         -- FR-206 分类筛选
CREATE INDEX idx_items_seller_status      ON items (seller_id, status);           -- PF-08 我的物品
CREATE INDEX idx_items_condition          ON items (condition_id);
CREATE INDEX idx_items_swap_pref          ON items (swap_preference) WHERE status = 'IN_SALE'; -- FR-210
-- 关键词搜索用（FR-03 搜索）：lower(title) 的 B-Tree 索引，支持前缀匹配。
-- 注意：完整的中文分词/模糊搜索（pg_trgm、全文检索）不在迭代一范围（Q-34 待确认搜索维度），
--       后续若需要只新增 V4+ 迁移，不改本行。
CREATE INDEX idx_items_title_lower        ON items (lower(title));
CREATE TRIGGER trg_items_updated_at BEFORE UPDATE ON items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 5.2 item_images 商品图片（FR-205：1–9 张，首图为封面）
-- ---------------------------------------------------------------------
CREATE TABLE item_images (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 主键
    item_id      BIGINT       NOT NULL REFERENCES items(id) ON DELETE CASCADE,  -- 所属商品
    image_url    VARCHAR(255) NOT NULL,                          -- 本地存储路径（OOS-18 不使用对象存储）
    sort_order   INTEGER      NOT NULL DEFAULT 0,                -- 0 = 封面（首图）
    file_size    INTEGER      NULL,                              -- 字节数，校验 ≤5MB（A-05）
    content_type VARCHAR(32)  NULL,                              -- image/jpeg | image/png | image/webp（A-05）
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 创建时间（审计字段）
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),  -- 更新时间（审计字段）
    created_by   BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,  -- 创建人（审计字段）
    updated_by   BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,  -- 更新人（审计字段）
    CONSTRAINT uq_item_images_sort UNIQUE (item_id, sort_order),
    CONSTRAINT ck_item_images_size CHECK (file_size IS NULL OR file_size <= 5242880)  -- A-05 5MB
);

COMMENT ON TABLE  item_images IS '商品图片（FR-205）：每商品 1–9 张（数量上限由应用层校验，见 design/README.md D-05）';
COMMENT ON COLUMN item_images.sort_order IS '0 为封面（首图）；调整顺序即调整封面';
COMMENT ON COLUMN item_images.file_size IS '单张 ≤5MB（A-05）；格式限 JPG/PNG/WebP';

CREATE INDEX idx_item_images_item ON item_images (item_id, sort_order);

-- =====================================================================
-- 6. 占用（C-01 / R-16 / R-23 的核心表）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 6.1 item_occupations 商品占用台账
--     语义：一行 = 一次「进行中」占用；released_at IS NULL 表示仍占用。
--     互斥真值来源：部分唯一索引 uq_item_occ_active_item 保证同一商品
--     同时最多一条未释放占用 —— 无论来源是订单（ORDER）还是交换（SWAP），
--     因此同时满足 C-01（一物一占用）与 R-23（订单占用与交换占用互斥）。
--     订单：ref_type=ORDER，ref_id=orders.id；交换：ref_type=SWAP，ref_id=swap_proposals.id
-- ---------------------------------------------------------------------
CREATE TABLE item_occupations (
    id            BIGINT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id       BIGINT           NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    ref_type      occupation_type  NOT NULL,                     -- ORDER | SWAP
    ref_id        BIGINT           NOT NULL,                     -- 多态引用：orders.id 或 swap_proposals.id
    occupied_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),       -- R-16 占用发生在下单/发起交换时
    released_at   TIMESTAMPTZ      NULL,                         -- NULL = 进行中；非 NULL = 已释放（IDEM-B 只释放一次）
    release_reason VARCHAR(64)     NULL,                         -- 释放原因，便于审计与对账
    version       INTEGER          NOT NULL DEFAULT 0,           -- R-18：释放/占用推进使用条件更新
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    created_by    BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by    BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_item_occupations_release CHECK (
        (released_at IS NULL AND release_reason IS NULL)
        OR (released_at IS NOT NULL AND release_reason IS NOT NULL)
    )
);

COMMENT ON TABLE  item_occupations IS
    '商品占用台账（C-01/R-16/R-23）：released_at IS NULL 为进行中占用；'
    'uq_item_occ_active_item 部分唯一索引是「一物一占用 + 订单/交换互斥」的数据库级保证';
COMMENT ON COLUMN item_occupations.ref_id IS '多态引用：ref_type=ORDER 时指向 orders.id，=SWAP 时指向 swap_proposals.id（不使用外键，见 design/README.md D-03）';
COMMENT ON COLUMN item_occupations.released_at IS '释放幂等锚点（IDEM-B）：重复释放不得再改此行，也不得二次释放';

-- 【关键约束】C-01 + R-23：同一商品同时最多一个进行中占用
CREATE UNIQUE INDEX uq_item_occ_active_item
    ON item_occupations (item_id)
    WHERE released_at IS NULL;

CREATE INDEX idx_item_occ_ref              ON item_occupations (ref_type, ref_id);
CREATE INDEX idx_item_occ_item_history     ON item_occupations (item_id, occupied_at DESC);
CREATE INDEX idx_item_occ_active_occupied  ON item_occupations (occupied_at) WHERE released_at IS NULL;
CREATE TRIGGER trg_item_occupations_updated_at BEFORE UPDATE ON item_occupations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 7. 购物车与订单
-- =====================================================================

-- ---------------------------------------------------------------------
-- 7.1 cart_items 购物车项（FR-301/FR-302 / R-04）
--     数量固定为 1（A-16，Q-23 待确认）；按卖家分组展示由查询实现
-- ---------------------------------------------------------------------
CREATE TABLE cart_items (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,   -- 买家
    item_id     BIGINT      NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    quantity    SMALLINT    NOT NULL DEFAULT 1,                  -- A-16：闲置交易单件
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by  BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_cart_items_user_item UNIQUE (user_id, item_id),   -- R-04 同一商品不重复加入
    CONSTRAINT ck_cart_items_quantity CHECK (quantity = 1)          -- A-16
);

COMMENT ON TABLE  cart_items IS '购物车（FR-301/FR-302）：R-04 不可加自家商品（应用层校验）、同商品不重复；数量固定 1（A-16/Q-23）';

CREATE INDEX idx_cart_items_item ON cart_items (item_id);
CREATE TRIGGER trg_cart_items_updated_at BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 7.2 orders 订单（FR-303~FR-312 / 订单状态机 §3 / R-05、R-07、R-09、R-16）
--     一单一卖家（C-02 / R-05）：seller_id 冗余存储，便于按卖家分组
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id                BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no          VARCHAR(32)     NOT NULL,                  -- 业务单号，对外展示
    buyer_id          BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    seller_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,  -- C-02 一单一卖家
    trade_zone_id     BIGINT          NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT, -- R-15
    pickup_point_id   BIGINT          NULL REFERENCES pickup_points(id) ON DELETE RESTRICT,   -- R-06 付款后选择
    pickup_point_selected_at TIMESTAMPTZ NULL,
    status            order_status    NOT NULL DEFAULT 'PENDING_PAYMENT',  -- 订单状态机 §3.1
    estimated_total   NUMERIC(12,2)   NOT NULL DEFAULT 0,        -- 估价合计快照（R-13 仅展示）
    expires_at        TIMESTAMPTZ     NOT NULL,                  -- R-07 付款截止（created_at + 30min，A-01）
    paid_at           TIMESTAMPTZ     NULL,                      -- R-08
    delivered_at      TIMESTAMPTZ     NULL,                      -- O-06
    completed_at      TIMESTAMPTZ     NULL,                      -- O-07
    cancelled_at      TIMESTAMPTZ     NULL,
    cancel_reason     order_cancel_reason NULL,                  -- O-03/O-04/O-11 三条取消路径
    cancel_note       VARCHAR(255)    NULL,
    review_status     review_status   NOT NULL DEFAULT 'OPEN',   -- 评价状态机 §8（订单维度）
    review_deadline   TIMESTAMPTZ     NULL,                      -- A-07 互评窗口 7 天
    version           INTEGER         NOT NULL DEFAULT 0,        -- R-18 乐观锁（付款 vs 超时必须条件更新）
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by        BIGINT          NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by        BIGINT          NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_orders_order_no UNIQUE (order_no),
    CONSTRAINT ck_orders_total_nonneg CHECK (estimated_total >= 0),
    -- 取货点选择时间与取货点必须同时存在或同时为空（O-05）
    CONSTRAINT ck_orders_pickup_pair CHECK (
        (pickup_point_id IS NULL AND pickup_point_selected_at IS NULL)
        OR (pickup_point_id IS NOT NULL AND pickup_point_selected_at IS NOT NULL)
    ),
    -- 取消类状态必须有取消时间与原因（O-03/O-04/O-11）
    CONSTRAINT ck_orders_cancel_trace CHECK (
        (status IN ('CANCELLED', 'TIMEOUT_CANCELLED'))
        = (cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)
    ),
    -- 付/交付/完成时间点与状态一致（订单状态机 §3.2）
    -- 注意：CANCELLED / TIMEOUT_CANCELLED 属于「从未付款即终结」，因此不要求 paid_at
    CONSTRAINT ck_orders_paid_at    CHECK (status IN ('PENDING_PAYMENT','CANCELLED','TIMEOUT_CANCELLED') OR paid_at IS NOT NULL),
    CONSTRAINT ck_orders_delivered  CHECK (status IN ('PENDING_PAYMENT','PAID','CANCELLED','TIMEOUT_CANCELLED') OR delivered_at IS NOT NULL),
    CONSTRAINT ck_orders_completed  CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

COMMENT ON TABLE  orders IS '订单表（FR-303~FR-312）：C-02 一笔订单只含一个卖家的商品；超时释放见 R-07';
COMMENT ON COLUMN orders.expires_at IS 'R-07 付款截止绝对时间（服务端时间，A-01=30 分钟）；定时任务扫描 status=PENDING_PAYMENT AND now()>=expires_at';
COMMENT ON COLUMN orders.seller_id IS 'C-02/R-05 一单一卖家：跨卖家提交时按卖家拆成多笔订单';
COMMENT ON COLUMN orders.pickup_point_id IS 'R-06 公共取货点，必须属于本订单 trade_zone_id（应用层校验）';
COMMENT ON COLUMN orders.version IS 'R-18 条件更新：付款与超时取消并发时 UPDATE ... WHERE status=PENDING_PAYMENT 只允许一方成功';

CREATE INDEX idx_orders_buyer_status   ON orders (buyer_id, status, created_at DESC);
CREATE INDEX idx_orders_seller_status  ON orders (seller_id, status, created_at DESC);
CREATE INDEX idx_orders_status_expires ON orders (expires_at) WHERE status = 'PENDING_PAYMENT';  -- FR-601 超时扫描
CREATE INDEX idx_orders_trade_zone     ON orders (trade_zone_id);
CREATE INDEX idx_orders_pickup_point   ON orders (pickup_point_id);
CREATE INDEX idx_orders_review_window  ON orders (review_deadline)
    WHERE review_status <> 'WINDOW_CLOSED';                                                       -- FR-604 评价窗口关闭
CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 7.3 order_items 订单明细（FR-303 / R-13 价格快照）
--     历史记录，永不删除；允许同一商品在历史上出现在多笔订单（取消后可再售）
-- ---------------------------------------------------------------------
CREATE TABLE order_items (
    id                BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT        NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    item_id           BIGINT        NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    title_snapshot    VARCHAR(60)   NOT NULL,                    -- R-13 快照：商品后续编辑不影响历史订单
    price_snapshot    NUMERIC(12,2) NOT NULL,                    -- R-13 估价快照
    condition_snapshot VARCHAR(32)  NULL,                        -- 成色快照（展示用）
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by        BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_order_items_order_item UNIQUE (order_id, item_id),
    CONSTRAINT ck_order_items_price_nonneg CHECK (price_snapshot >= 0)
);

COMMENT ON TABLE order_items IS '订单明细（FR-303）：一行一件；估价快照保证历史可追溯（R-13）；永不物理删除';
COMMENT ON COLUMN order_items.price_snapshot IS '下单时刻估价快照，商品后续编辑不影响本行（R-13）';

CREATE INDEX idx_order_items_item  ON order_items (item_id);
-- 支撑「商品是否已被某笔未终结订单占用」的查询（R-16）
CREATE INDEX idx_order_items_active ON order_items (item_id);

-- ---------------------------------------------------------------------
-- 7.4 payments 模拟付款记录（FR-306 / R-08 / OOS-01）
--     仅 MOCK 渠道；不产生资金流水、不支持退款
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id            BIGINT           GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      BIGINT           NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    payer_id      BIGINT           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    channel       payment_channel  NOT NULL DEFAULT 'MOCK',      -- OOS-01：唯一允许值
    status        payment_status   NOT NULL DEFAULT 'SUCCEEDED',
    amount        NUMERIC(12,2)    NOT NULL,                     -- 估价合计（非真实扣款）
    paid_at       TIMESTAMPTZ      NOT NULL DEFAULT now(),
    failure_reason VARCHAR(255)    NULL,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    created_by    BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by    BIGINT           NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_payments_amount_nonneg CHECK (amount >= 0)
);

COMMENT ON TABLE  payments IS '模拟付款记录（FR-306/R-08）：OOS-01 不接入真实支付，不产生资金流水与退款';
COMMENT ON COLUMN payments.channel IS '本迭代仅 MOCK；未来接入真实支付时新增枚举值即可（不改状态机）';

-- 【幂等】R-08 IDEM-A：同一订单只有一条成功付款记录，重复付款不新增
CREATE UNIQUE INDEX uq_payments_order_succeeded
    ON payments (order_id) WHERE status = 'SUCCEEDED';
CREATE INDEX idx_payments_payer ON payments (payer_id, paid_at DESC);
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 8. 交换（交换包 → 交换提议 → 交换码）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 8.1 swap_bundles 交换包（FR-402 / 交换包状态机 §5 / R-21）
--     1–5 件（SB-01）；包内商品必须同属 owner（SB-02）
-- ---------------------------------------------------------------------
CREATE TABLE swap_bundles (
    id            BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id      BIGINT              NOT NULL REFERENCES users(id) ON DELETE RESTRICT,  -- SB-02 包主
    trade_zone_id BIGINT              NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT, -- R-15
    title         VARCHAR(60)         NULL,                      -- 可选命名，便于识别
    status        swap_bundle_status  NOT NULL DEFAULT 'DRAFT',  -- 交换包状态机 §5.1
    estimated_total NUMERIC(12,2)     NOT NULL DEFAULT 0,        -- FR-403 估价合计（仅展示，R-13/OOS-03）
    packed_at     TIMESTAMPTZ         NULL,                      -- B-04 打包完成时刻
    completed_at  TIMESTAMPTZ         NULL,                      -- B-06
    released_at   TIMESTAMPTZ         NULL,                      -- B-07
    version       INTEGER             NOT NULL DEFAULT 0,        -- R-18
    deleted_at    TIMESTAMPTZ         NULL,                      -- 软删除（B-11）；被提议引用过的包禁止物理删除
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT now(),
    created_by    BIGINT              NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by    BIGINT              NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_swap_bundles_total_nonneg CHECK (estimated_total >= 0),
    CONSTRAINT ck_swap_bundles_packed_trace CHECK (
        status = 'DRAFT' OR packed_at IS NOT NULL
    )
);

COMMENT ON TABLE  swap_bundles IS '交换包（FR-402）：1–5 件自有商品；estimated_total 仅展示，不做等价判定与补差价（R-13/OOS-03）';
COMMENT ON COLUMN swap_bundles.status IS 'IN_SWAP=被进行中提议引用；OCCUPIED=包内存在被占用物品导致不可用（§5.1 说明）';
COMMENT ON COLUMN swap_bundles.estimated_total IS 'FR-403 估价合计；OOS-03 明确不做现金补差价，故无差额字段';

CREATE INDEX idx_swap_bundles_owner_status ON swap_bundles (owner_id, status);
CREATE INDEX idx_swap_bundles_zone         ON swap_bundles (trade_zone_id);
CREATE TRIGGER trg_swap_bundles_updated_at BEFORE UPDATE ON swap_bundles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 8.2 swap_bundle_items 交换包物品（R-21 / SB-01~SB-04）
--     SB-03 由部分唯一索引 uq_swap_bundle_items_active 保证：
--     同一商品同时最多出现在一个「进行中」交换包内。
-- ---------------------------------------------------------------------
CREATE TABLE swap_bundle_items (
    id             BIGINT              GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    swap_bundle_id BIGINT              NOT NULL REFERENCES swap_bundles(id) ON DELETE RESTRICT,
    item_id        BIGINT              NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    item_status    bundle_item_status  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE=进行中（占用中）
    price_snapshot NUMERIC(12,2)       NOT NULL,                   -- R-13 加入包时估价快照
    released_at    TIMESTAMPTZ         NULL,
    release_reason VARCHAR(64)         NULL,
    created_at     TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ         NOT NULL DEFAULT now(),
    created_by     BIGINT              NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by     BIGINT              NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_swap_bundle_items_bundle_item UNIQUE (swap_bundle_id, item_id),
    CONSTRAINT ck_swap_bundle_items_price_nonneg CHECK (price_snapshot >= 0),
    CONSTRAINT ck_swap_bundle_items_release CHECK (
        (item_status = 'ACTIVE' AND released_at IS NULL)
        OR (item_status <> 'ACTIVE' AND released_at IS NOT NULL)
    )
);

COMMENT ON TABLE swap_bundle_items IS '交换包物品（R-21）：包内 1–5 件（数量由应用层校验，D-05），所有件须同属包主（SB-02）';

-- 【关键约束】SB-03：同一商品同时最多在一个进行中交换包内
CREATE UNIQUE INDEX uq_swap_bundle_items_active
    ON swap_bundle_items (item_id)
    WHERE item_status = 'ACTIVE';

CREATE INDEX idx_swap_bundle_items_bundle ON swap_bundle_items (swap_bundle_id);
CREATE INDEX idx_swap_bundle_items_item   ON swap_bundle_items (item_id, created_at DESC);
CREATE TRIGGER trg_swap_bundle_items_updated_at BEFORE UPDATE ON swap_bundle_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 8.3 swap_proposals 交换提议（FR-404~FR-408 / 交换提议状态机 §4 / R-22、R-24、R-25、R-28）
--     参与方恰好两个：proposer（贡献交换包）与 target（其商品被请求）
--     反提案：原提议 → COUNTERED，并新建一条提议（parent_proposal_id 指向原提议）
-- ---------------------------------------------------------------------
CREATE TABLE swap_proposals (
    id                    BIGINT                 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proposal_no           VARCHAR(32)            NOT NULL,
    proposer_id           BIGINT                 NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_id             BIGINT                 NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    proposer_bundle_id    BIGINT                 NOT NULL REFERENCES swap_bundles(id) ON DELETE RESTRICT, -- 发起方交换包
    target_bundle_id      BIGINT                 NULL REFERENCES swap_bundles(id) ON DELETE RESTRICT,     -- 仅反提案时使用
    parent_proposal_id    BIGINT                 NULL REFERENCES swap_proposals(id) ON DELETE RESTRICT,  -- 反提案来源（R-28 仅一轮）
    trade_zone_id         BIGINT                 NOT NULL REFERENCES trade_zones(id) ON DELETE RESTRICT,  -- R-15 同区域
    status                swap_proposal_status   NOT NULL DEFAULT 'PENDING',  -- 交换提议状态机 §4.1
    termination_reason    swap_termination_reason NULL,          -- R-24 整笔作废 / 拒绝 / 过期 / 取消 原因
    counter_depth         SMALLINT               NOT NULL DEFAULT 0,  -- R-28/A-10：0=原提议，1=反提案（最大 1）
    message               VARCHAR(500)           NULL,           -- 发起说明（结构化交互，不做聊天 OOS-08）
    estimated_total_offer NUMERIC(12,2)          NOT NULL DEFAULT 0,  -- 发起方包估价合计快照（仅展示）
    estimated_total_want  NUMERIC(12,2)          NOT NULL DEFAULT 0,  -- 目标方商品估价合计快照（仅展示）
    expires_at            TIMESTAMPTZ            NOT NULL,       -- R-25 72 小时绝对时间（A-02）
    accepted_at           TIMESTAMPTZ            NULL,           -- S-02 整包接受时刻
    completed_at          TIMESTAMPTZ            NULL,           -- S-10 交换完成时刻
    proposer_confirmed_at TIMESTAMPTZ            NULL,           -- R-27 发起方确认（1/2）
    target_confirmed_at   TIMESTAMPTZ            NULL,           -- R-27 目标方确认（2/2）
    version               INTEGER                NOT NULL DEFAULT 0,  -- R-18：接受/拒绝/过期并发只允许一方成功
    created_at            TIMESTAMPTZ            NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ            NOT NULL DEFAULT now(),
    created_by            BIGINT                 NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by            BIGINT                 NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_swap_proposals_proposal_no UNIQUE (proposal_no),
    CONSTRAINT ck_swap_proposals_parties CHECK (proposer_id <> target_id),        -- S-01 不能与自己交换
    CONSTRAINT ck_swap_proposals_counter_depth CHECK (counter_depth BETWEEN 0 AND 1), -- R-28 仅一轮
    CONSTRAINT ck_swap_proposals_totals_nonneg CHECK (
        estimated_total_offer >= 0 AND estimated_total_want >= 0
    ),
    -- 反提案必须有来源提议；原提议 counter_depth 必须为 0（R-28 结构保证）
    CONSTRAINT ck_swap_proposals_counter_source CHECK (
        (counter_depth = 0 AND parent_proposal_id IS NULL)
        OR (counter_depth = 1 AND parent_proposal_id IS NOT NULL AND target_bundle_id IS NOT NULL)
    ),
    -- 接受后必须记录接受时间（S-02）
    CONSTRAINT ck_swap_proposals_accepted_trace CHECK (
        status <> 'ACCEPTED' OR accepted_at IS NOT NULL
    ),
    -- 完成必须双方都确认过（R-27）
    CONSTRAINT ck_swap_proposals_completed_trace CHECK (
        status <> 'COMPLETED'
        OR (completed_at IS NOT NULL AND proposer_confirmed_at IS NOT NULL AND target_confirmed_at IS NOT NULL)
    ),
    -- 终结类状态必须写终结原因（R-24 可解释性）
    CONSTRAINT ck_swap_proposals_termination_trace CHECK (
        status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED', 'COUNTERED')
        OR termination_reason IS NOT NULL
    )
);

COMMENT ON TABLE  swap_proposals IS
    '交换提议（FR-404~FR-408）：R-22 整包接受（无部分接受字段）；R-24 整笔作废；R-25 72 小时过期；R-28 反提案仅一轮';
COMMENT ON COLUMN swap_proposals.expires_at IS 'R-25/A-02：created_at + 72h，绝对时间；已 ACCEPTED 的提议不参与过期';
COMMENT ON COLUMN swap_proposals.counter_depth IS 'R-28/A-10：0=原提议，1=反提案；CHECK 限制 ≤1，数据库级禁止「反提案的反提案」';
COMMENT ON COLUMN swap_proposals.proposer_confirmed_at IS 'R-27 双方确认：与 target_confirmed_at 齐备才允许置 COMPLETED（见 ck_swap_proposals_completed_trace）';

CREATE INDEX idx_swap_proposals_proposer_status ON swap_proposals (proposer_id, status, created_at DESC);
CREATE INDEX idx_swap_proposals_target_status   ON swap_proposals (target_id, status, created_at DESC);
CREATE INDEX idx_swap_proposals_status_expires  ON swap_proposals (expires_at) WHERE status = 'PENDING'; -- FR-602 过期扫描
CREATE INDEX idx_swap_proposals_bundle         ON swap_proposals (proposer_bundle_id);
CREATE INDEX idx_swap_proposals_target_bundle  ON swap_proposals (target_bundle_id);
CREATE INDEX idx_swap_proposals_parent         ON swap_proposals (parent_proposal_id);
CREATE INDEX idx_swap_proposals_zone           ON swap_proposals (trade_zone_id);
CREATE TRIGGER trg_swap_proposals_updated_at BEFORE UPDATE ON swap_proposals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 8.4 swap_proposal_items 交换提议目标商品（C-04 / R-22）
--     目标方的被请求商品，≥1 件且必须全部属于同一 target；
--     接受即接受此处全部行，不存在 item 级选择（OOS-04 部分接受明确不做）。
-- ---------------------------------------------------------------------
CREATE TABLE swap_proposal_items (
    id                BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    swap_proposal_id  BIGINT        NOT NULL REFERENCES swap_proposals(id) ON DELETE RESTRICT,
    item_id           BIGINT        NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    title_snapshot    VARCHAR(60)   NOT NULL,                    -- R-13 快照
    price_snapshot    NUMERIC(12,2) NOT NULL,                    -- R-13 快照
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by        BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_swap_proposal_items_proposal_item UNIQUE (swap_proposal_id, item_id),
    CONSTRAINT ck_swap_proposal_items_price_nonneg CHECK (price_snapshot >= 0)
);

COMMENT ON TABLE swap_proposal_items IS
    '交换提议目标商品（C-04）：≥1 件、同属目标方；R-22 整包接受即接受本表全部行，无部分接受（OOS-04）';

CREATE INDEX idx_swap_proposal_items_item     ON swap_proposal_items (item_id, created_at DESC);
CREATE INDEX idx_swap_proposal_items_proposal ON swap_proposal_items (swap_proposal_id);

-- ---------------------------------------------------------------------
-- 8.5 swap_codes 交换码（FR-409 / R-26 / C-05：一次性核销）
-- ---------------------------------------------------------------------
CREATE TABLE swap_codes (
    id                BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    swap_proposal_id  BIGINT            NOT NULL REFERENCES swap_proposals(id) ON DELETE RESTRICT,
    code              VARCHAR(16)       NOT NULL,                -- Q-30 待确认格式，建议 6–8 位字母数字
    status            swap_code_status  NOT NULL DEFAULT 'ISSUED',
    verified_at       TIMESTAMPTZ       NULL,                    -- S-08 核销时刻
    verified_by       BIGINT            NULL REFERENCES users(id) ON DELETE RESTRICT,  -- 核销人（提议双方之一）
    expires_at        TIMESTAMPTZ       NULL,
    version           INTEGER           NOT NULL DEFAULT 0,      -- R-18：并发核销只允许一方成功
    created_at        TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ       NOT NULL DEFAULT now(),
    created_by        BIGINT            NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by        BIGINT            NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_swap_codes_code UNIQUE (code),
    -- C-05：一条提议最多一个交换码
    CONSTRAINT uq_swap_codes_proposal UNIQUE (swap_proposal_id),
    -- R-26 一次性：已核销必须有核销时间与核销人
    CONSTRAINT ck_swap_codes_verified_trace CHECK (
        (status = 'ISSUED' AND verified_at IS NULL AND verified_by IS NULL)
        OR (status <> 'ISSUED' AND (
              (status = 'VERIFIED' AND verified_at IS NOT NULL AND verified_by IS NOT NULL)
              OR (status = 'EXPIRED' AND verified_at IS NULL)
           ))
    )
);

COMMENT ON TABLE  swap_codes IS '交换码（FR-409/R-26）：C-05 一条提议最多一码；一次性核销，重复核销返回 E-12';
COMMENT ON COLUMN swap_codes.code IS 'Q-30 待确认格式；仅提议双方可见，禁止出现在公开列表接口';

CREATE INDEX idx_swap_codes_status ON swap_codes (status);
CREATE TRIGGER trg_swap_codes_updated_at BEFORE UPDATE ON swap_codes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 9. 评价、举报
-- =====================================================================

-- ---------------------------------------------------------------------
-- 9.1 reviews 评价（FR-312 / R-14 / 评价状态机 §8）
--     交易完成后双方各评一次；同一交易同一提交方最多一次（IDEM-B）
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
    id                 BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id           BIGINT        NULL REFERENCES orders(id) ON DELETE RESTRICT,
    swap_proposal_id   BIGINT        NULL REFERENCES swap_proposals(id) ON DELETE RESTRICT,
    reviewer_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reviewee_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reviewer_role      review_role   NOT NULL,                   -- BUYER | SELLER（R-14）
    rating             SMALLINT      NOT NULL,                   -- 1–5
    content            VARCHAR(500)  NULL,
    version            INTEGER       NOT NULL DEFAULT 0,         -- R-18：评价入口关闭与提交并发时条件更新
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by         BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    -- 评价必须且只能挂在一个已完成交易上（R-14 前置条件）
    CONSTRAINT ck_reviews_exactly_one_target CHECK (
        (order_id IS NOT NULL AND swap_proposal_id IS NULL)
        OR (order_id IS NULL AND swap_proposal_id IS NOT NULL)
    ),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_not_self CHECK (reviewer_id <> reviewee_id)
);

COMMENT ON TABLE  reviews IS '评价（FR-312/R-14）：前置条件为交易已完成；同一交易同一方仅一次（IDEM-B）；不做排序影响（OOS-09）';
COMMENT ON COLUMN reviews.order_id IS '与 swap_proposal_id 二选一（ck_reviews_exactly_one_target）';

-- R-14/R-02 IDEM-B：同一订单同一方只能评一次
CREATE UNIQUE INDEX uq_reviews_order_reviewer
    ON reviews (order_id, reviewer_id) WHERE order_id IS NOT NULL;
CREATE UNIQUE INDEX uq_reviews_swap_reviewer
    ON reviews (swap_proposal_id, reviewer_id) WHERE swap_proposal_id IS NOT NULL;
CREATE INDEX idx_reviews_reviewee ON reviews (reviewee_id, created_at DESC);
CREATE INDEX idx_reviews_reviewer ON reviews (reviewer_id, created_at DESC);
CREATE TRIGGER trg_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 9.2 reports 举报（FR-211 / C-06 / E-14）
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id   BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    item_id       BIGINT        NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    reason        report_reason NOT NULL,                        -- FR-211 需选择原因
    detail        VARCHAR(500)  NULL,
    status        report_status NOT NULL DEFAULT 'PENDING',
    handled_by    BIGINT        NULL REFERENCES users(id) ON DELETE RESTRICT,  -- 处理管理员
    handled_at    TIMESTAMPTZ   NULL,
    handle_note   VARCHAR(255)  NULL,
    version       INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by    BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_reports_handled_trace CHECK (
        (status = 'PENDING' AND handled_by IS NULL AND handled_at IS NULL)
        OR (status <> 'PENDING' AND handled_by IS NOT NULL AND handled_at IS NOT NULL)
    )
);

COMMENT ON TABLE reports IS '举报（FR-211）：C-06 同一用户对同一商品最多 1 条未处理举报；E-14 重复举报须幂等';

-- 【关键约束】C-06：同一用户对同一商品的「未处理」举报最多一条
CREATE UNIQUE INDEX uq_reports_pending_reporter_item
    ON reports (reporter_id, item_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_reports_item_status   ON reports (item_id, status);
CREATE INDEX idx_reports_status_created ON reports (status, created_at);
CREATE INDEX idx_reports_reporter      ON reports (reporter_id, created_at DESC);
CREATE TRIGGER trg_reports_updated_at BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 10. 治理：处罚与申诉（R-30~R-35；记录永不物理删除，R-32）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 10.1 penalties 处罚记录（FR-503~FR-506 / 处罚状态机 §6 / R-30~R-32）
--      记录永不删除；「解封」只改状态，不改历史
-- ---------------------------------------------------------------------
CREATE TABLE penalties (
    id              BIGINT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_user_id  BIGINT             NOT NULL REFERENCES users(id) ON DELETE RESTRICT,  -- 被处罚用户
    operator_id     BIGINT             NOT NULL REFERENCES users(id) ON DELETE RESTRICT,  -- 操作管理员
    penalty_type    penalty_type       NOT NULL,                 -- BAN_3D | ITEM_TAKEDOWN
    item_id         BIGINT             NULL REFERENCES items(id) ON DELETE RESTRICT,      -- ITEM_TAKEDOWN 关联商品
    reason          VARCHAR(255)       NOT NULL,                 -- R-32/R-34：治理动作必须填原因
    status          penalty_status     NOT NULL DEFAULT 'ACTIVE',
    started_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    ends_at         TIMESTAMPTZ        NOT NULL,                 -- R-30/A-03：started_at + 72h（固定三天）
    lifted_at       TIMESTAMPTZ        NULL,                     -- 解封时刻
    lift_source     penalty_lift_source NULL,                    -- 解封来源（系统/申诉/管理员手动）
    lift_reason     VARCHAR(255)       NULL,
    version         INTEGER            NOT NULL DEFAULT 0,       -- R-18：到期自动解封 vs 手动解封并发只允许一方成功
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    created_by      BIGINT             NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by      BIGINT             NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_penalties_reason_not_blank CHECK (btrim(reason) <> ''),   -- R-34 reason 必填非空
    CONSTRAINT ck_penalties_period CHECK (ends_at > started_at),
    -- ITEM_TAKEDOWN 必须关联商品；BAN_3D 不应关联商品
    CONSTRAINT ck_penalties_item_by_type CHECK (
        (penalty_type = 'ITEM_TAKEDOWN' AND item_id IS NOT NULL)
        OR (penalty_type = 'BAN_3D' AND item_id IS NULL)
    ),
    -- 解封信息必须成对出现（P-04/P-05/P-07）
    CONSTRAINT ck_penalties_lift_trace CHECK (
        (status IN ('ACTIVE', 'APPEALING') AND lifted_at IS NULL AND lift_source IS NULL)
        OR (status IN ('LIFTED', 'EXPIRED') AND lifted_at IS NOT NULL AND lift_source IS NOT NULL)
    )
);

COMMENT ON TABLE  penalties IS
    '处罚记录（FR-503~FR-506）：R-32 永不物理删除；R-30 默认固定 3 天；R-31 到期自动解封；R-34 reason 必填';
COMMENT ON COLUMN penalties.ends_at IS 'R-30/A-03：started_at + 72h（固定三天，Q-09 假设不可调）';
COMMENT ON COLUMN penalties.penalty_type IS 'BAN_3D 影响用户交易能力；ITEM_TAKEDOWN 仅下架商品并留痕举证';
COMMENT ON COLUMN penalties.status IS 'APPEALING 期间处罚仍生效（不停表，P-03），仅用于标记申诉进行中';

-- FR-603 到期自动解封扫描：status IN (ACTIVE, APPEALING) AND now() >= ends_at
CREATE INDEX idx_penalties_active_ends ON penalties (ends_at)
    WHERE status IN ('ACTIVE', 'APPEALING');
CREATE INDEX idx_penalties_target     ON penalties (target_user_id, started_at DESC);
CREATE INDEX idx_penalties_operator   ON penalties (operator_id);
CREATE INDEX idx_penalties_item       ON penalties (item_id);
CREATE INDEX idx_penalties_type_status ON penalties (penalty_type, status);

-- 【关键约束】R-30/E-13/Q-10：同一用户同时最多一条生效中处罚（假设拒绝重复封禁）
CREATE UNIQUE INDEX uq_penalties_one_active
    ON penalties (target_user_id, penalty_type)
    WHERE status IN ('ACTIVE', 'APPEALING') AND penalty_type = 'BAN_3D';

CREATE TRIGGER trg_penalties_updated_at BEFORE UPDATE ON penalties
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 10.2 appeals 申诉记录（FR-505 / 申诉状态机 §7 / R-32）
--      AP-01：一个处罚最多一条申诉（uq_appeals_penalty）；记录永不删除
-- ---------------------------------------------------------------------
CREATE TABLE appeals (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    penalty_id      BIGINT        NOT NULL REFERENCES penalties(id) ON DELETE RESTRICT,
    appellant_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,     -- 申诉人（须为被处罚用户）
    reason          VARCHAR(1000) NOT NULL,                    -- AP-01 必须填申诉理由
    status          appeal_status NOT NULL DEFAULT 'SUBMITTED',
    handler_id      BIGINT        NULL REFERENCES users(id) ON DELETE RESTRICT,         -- 处理管理员
    resolution_note VARCHAR(1000) NULL,                        -- AP-02/AP-03 必须填处理意见
    resolved_at     TIMESTAMPTZ   NULL,
    version         INTEGER       NOT NULL DEFAULT 0,          -- R-18：重复处理必须条件更新
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by      BIGINT        NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_appeals_reason_not_blank CHECK (btrim(reason) <> ''),
    -- 处理完成必须记录处理人、意见与时间（R-34 审计要求）
    CONSTRAINT ck_appeals_resolution_trace CHECK (
        (status = 'SUBMITTED' AND resolved_at IS NULL AND handler_id IS NULL)
        OR (status <> 'SUBMITTED' AND resolved_at IS NOT NULL AND handler_id IS NOT NULL
            AND resolution_note IS NOT NULL AND btrim(resolution_note) <> '')
    )
);

COMMENT ON TABLE  appeals IS '申诉记录（FR-505）：R-32 永不物理删除；AP-01 一个处罚最多一条申诉；R-34 处理意见必填';
COMMENT ON COLUMN appeals.reason IS 'AP-01 申诉理由必填；提交后处罚仍生效（P-03）';

-- AP-01：一个处罚最多一条申诉（重复提交返回同一条或明确冲突）
CREATE UNIQUE INDEX uq_appeals_penalty ON appeals (penalty_id);
CREATE INDEX idx_appeals_status_created ON appeals (status, created_at);
CREATE INDEX idx_appeals_appellant     ON appeals (appellant_id, created_at DESC);
CREATE INDEX idx_appeals_handler       ON appeals (handler_id);
CREATE TRIGGER trg_appeals_updated_at BEFORE UPDATE ON appeals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 11. 通知（FR-507 / Q-05 待确认是否仅站内）
-- =====================================================================
CREATE TABLE notifications (
    id             BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_id   BIGINT            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id       BIGINT            NULL REFERENCES users(id) ON DELETE SET NULL,  -- 触发者（系统通知为空）
    type           notification_type NOT NULL,
    title          VARCHAR(120)      NOT NULL,
    body           VARCHAR(500)      NULL,
    ref_type       VARCHAR(32)       NULL,                       -- ORDER | SWAP | ITEM | PENALTY | APPEAL | REPORT
    ref_id         BIGINT            NULL,
    is_read        BOOLEAN           NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMPTZ       NULL,
    created_at     TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ       NOT NULL DEFAULT now(),
    created_by     BIGINT            NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by     BIGINT            NULL REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_notifications_read_pair CHECK (
        (is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL)
    ),
    CONSTRAINT ck_notifications_ref_pair CHECK (
        (ref_type IS NULL AND ref_id IS NULL) OR (ref_type IS NOT NULL AND ref_id IS NOT NULL)
    )
);

COMMENT ON TABLE  notifications IS '站内通知（FR-507）：Q-05 待确认是否仅站内；当前只实现站内渠道';
COMMENT ON COLUMN notifications.ref_type IS '多态业务引用，无外键（D-03）；用于前端跳转到对应详情页';

CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_recipient_all    ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notifications_ref              ON notifications (ref_type, ref_id);
CREATE TRIGGER trg_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 12. 幂等（R-17 / E-06 / E-16）与审计（R-34）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 12.1 idempotency_records 幂等记录
--      R-17：同一用户 + 同一幂等键 + 同一接口 → 返回首次结果（IDEM-A）
--      E-16：同一幂等键但请求体不同 → 冲突
--      建议保留 24 小时（A-08），过期清理由定时任务负责
-- ---------------------------------------------------------------------
CREATE TABLE idempotency_records (
    id                BIGINT             GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT             NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idempotency_key   VARCHAR(64)        NOT NULL,               -- A-15：前端生成 UUID v4
    endpoint          VARCHAR(255)       NOT NULL,               -- 接口标识，如 POST /orders
    request_hash      VARCHAR(64)        NOT NULL,               -- 请求体摘要，用于 E-16 同键异义检测
    status            idempotency_status NOT NULL DEFAULT 'IN_PROGRESS',
    response_status   INTEGER            NULL,                   -- 首次响应 HTTP 状态码
    response_body     JSONB              NULL,                   -- 首次响应体快照，重复请求直接回放
    resource_type     VARCHAR(32)        NULL,                   -- 首次产生的资源类型（ORDER/SWAP/...）
    resource_id       BIGINT             NULL,                   -- 首次产生的资源 ID
    error_message     VARCHAR(500)       NULL,
    version           INTEGER            NOT NULL DEFAULT 0,     -- R-18：并发同键请求以 INSERT ... ON CONFLICT 抢占后再推进
    expires_at        TIMESTAMPTZ        NOT NULL DEFAULT (now() + INTERVAL '24 hours'),  -- A-08
    created_at        TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ        NOT NULL DEFAULT now(),
    created_by        BIGINT             NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by        BIGINT             NULL REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE  idempotency_records IS
    '幂等记录（R-17）：双层防护之「幂等键」层；另一层为各业务表的 version 条件更新（R-18）';
COMMENT ON COLUMN idempotency_records.request_hash IS 'E-16：同键不同请求体必须返回冲突，禁止静默回放他人请求结果';
COMMENT ON COLUMN idempotency_records.expires_at IS 'A-08 建议保留 24 小时；过期记录由定时任务清理';

-- 【关键约束】R-17：同一用户 + 同一幂等键最多一条记录
CREATE UNIQUE INDEX uq_idempotency_user_key ON idempotency_records (user_id, idempotency_key);
CREATE INDEX idx_idempotency_expires     ON idempotency_records (expires_at);
CREATE INDEX idx_idempotency_resource    ON idempotency_records (resource_type, resource_id);
CREATE INDEX idx_idempotency_endpoint    ON idempotency_records (endpoint);
CREATE TRIGGER trg_idempotency_records_updated_at BEFORE UPDATE ON idempotency_records
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 12.2 audit_logs 审计日志（R-34）
--      治理动作（下架、恢复、封禁、解封、申诉处理、举报处理）必须留痕：
--      操作者、时间、对象、原因（必填）、前后状态
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id       BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,  -- NULL = 系统定时任务（ROLE_SYSTEM）
    actor_role     VARCHAR(32)  NULL,                            -- ADMIN | SYSTEM | USER
    action         VARCHAR(64)  NOT NULL,                        -- ITEM_BAN / ITEM_RESTORE / USER_BAN / USER_UNBAN / APPEAL_RESOLVE / REPORT_HANDLE ...
    target_type    VARCHAR(32)  NOT NULL,                        -- ITEM | USER | PENALTY | APPEAL | REPORT | ORDER | SWAP_PROPOSAL
    target_id      BIGINT       NOT NULL,
    reason         VARCHAR(255) NULL,                            -- R-34：治理动作必填（应用层强制）
    before_state   JSONB        NULL,                            -- 变更前状态快照
    after_state    JSONB        NULL,                            -- 变更后状态快照
    ip_address     VARCHAR(45)  NULL,
    user_agent     VARCHAR(255) NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by     BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE  audit_logs IS '审计日志（R-34）：治理动作必须留痕；追加写，不允许更新与删除业务语义';
COMMENT ON COLUMN audit_logs.reason IS 'R-34：下架/封禁/恢复/申诉处理的原因或意见，应用层必须强制非空';

CREATE INDEX idx_audit_logs_target     ON audit_logs (target_type, target_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor      ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action     ON audit_logs (action, created_at DESC);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE TRIGGER trg_audit_logs_updated_at BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- 13. 列级注释（由各列行内说明生成，使数据库自解释：\d+ 可查看）
--     与 V1 内联注释保持一致；如需修改注释请新增 V4+ 迁移。
-- ---------------------------------------------------------------------

-- appeals
COMMENT ON COLUMN appeals.appellant_id IS '申诉人（须为被处罚用户）';
COMMENT ON COLUMN appeals.handler_id IS '处理管理员';
COMMENT ON COLUMN appeals.resolution_note IS 'AP-02/AP-03 必须填处理意见';
COMMENT ON COLUMN appeals.version IS 'R-18：重复处理必须条件更新';

-- audit_logs
COMMENT ON COLUMN audit_logs.actor_id IS 'NULL = 系统定时任务（ROLE_SYSTEM）';
COMMENT ON COLUMN audit_logs.actor_role IS 'ADMIN | SYSTEM | USER';
COMMENT ON COLUMN audit_logs.action IS 'ITEM_BAN / ITEM_RESTORE / USER_BAN / USER_UNBAN / APPEAL_RESOLVE / REPORT_HANDLE ...';
COMMENT ON COLUMN audit_logs.target_type IS 'ITEM | USER | PENALTY | APPEAL | REPORT | ORDER | SWAP_PROPOSAL';
COMMENT ON COLUMN audit_logs.before_state IS '变更前状态快照';
COMMENT ON COLUMN audit_logs.after_state IS '变更后状态快照';

-- cart_items
COMMENT ON COLUMN cart_items.user_id IS '买家';
COMMENT ON COLUMN cart_items.quantity IS 'A-16：闲置交易单件';

-- categories
COMMENT ON COLUMN categories.id IS '主键';
COMMENT ON COLUMN categories.code IS '分类编码（唯一）';
COMMENT ON COLUMN categories.name IS '分类名称';
COMMENT ON COLUMN categories.sort_order IS '排序值（升序展示）';
COMMENT ON COLUMN categories.is_active IS '是否启用（停用后不出现在发布页）';
COMMENT ON COLUMN categories.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN categories.updated_at IS '更新时间（审计字段）';

-- idempotency_records
COMMENT ON COLUMN idempotency_records.idempotency_key IS 'A-15：前端生成 UUID v4';
COMMENT ON COLUMN idempotency_records.endpoint IS '接口标识，如 POST /orders';
COMMENT ON COLUMN idempotency_records.response_status IS '首次响应 HTTP 状态码';
COMMENT ON COLUMN idempotency_records.response_body IS '首次响应体快照，重复请求直接回放';
COMMENT ON COLUMN idempotency_records.resource_type IS '首次产生的资源类型（ORDER/SWAP/...）';
COMMENT ON COLUMN idempotency_records.resource_id IS '首次产生的资源 ID';
COMMENT ON COLUMN idempotency_records.version IS 'R-18：并发同键请求以 INSERT ... ON CONFLICT 抢占后再推进';

-- item_conditions
COMMENT ON COLUMN item_conditions.id IS '主键';
COMMENT ON COLUMN item_conditions.code IS '成色编码（唯一）';
COMMENT ON COLUMN item_conditions.name IS '成色名称（如「几乎全新」）';
COMMENT ON COLUMN item_conditions.sort_order IS '排序值（由新到旧展示）';
COMMENT ON COLUMN item_conditions.is_active IS '是否启用';
COMMENT ON COLUMN item_conditions.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN item_conditions.updated_at IS '更新时间（审计字段）';

-- item_images
COMMENT ON COLUMN item_images.id IS '主键';
COMMENT ON COLUMN item_images.item_id IS '所属商品';
COMMENT ON COLUMN item_images.image_url IS '本地存储路径（OOS-18 不使用对象存储）';
COMMENT ON COLUMN item_images.content_type IS 'image/jpeg | image/png | image/webp（A-05）';
COMMENT ON COLUMN item_images.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN item_images.updated_at IS '更新时间（审计字段）';
COMMENT ON COLUMN item_images.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN item_images.updated_by IS '更新人（审计字段）';

-- item_occupations
COMMENT ON COLUMN item_occupations.ref_type IS 'ORDER | SWAP';
COMMENT ON COLUMN item_occupations.occupied_at IS 'R-16 占用发生在下单/发起交换时';
COMMENT ON COLUMN item_occupations.release_reason IS '释放原因，便于审计与对账';
COMMENT ON COLUMN item_occupations.version IS 'R-18：释放/占用推进使用条件更新';

-- items
COMMENT ON COLUMN items.id IS '主键';
COMMENT ON COLUMN items.seller_id IS '卖家（发布人）；在该商品上成为卖家关系角色';
COMMENT ON COLUMN items.category_id IS '分类（预设字典，FR-206）';
COMMENT ON COLUMN items.condition_id IS '成色（预设字典，FR-207）';
COMMENT ON COLUMN items.trade_zone_id IS 'R-15 必须 ∈ 卖家已勾选区域';
COMMENT ON COLUMN items.title IS 'FR-201 标题 1-60';
COMMENT ON COLUMN items.description IS 'FR-201 描述（可选）';
COMMENT ON COLUMN items.published_at IS 'I-01/I-02 发布时刻（列表排序依据）';
COMMENT ON COLUMN items.banned_reason IS 'I-14/I-15 违规下架原因（R-34 必填）';
COMMENT ON COLUMN items.banned_at IS '违规下架时间';
COMMENT ON COLUMN items.banned_by IS '处置管理员';
COMMENT ON COLUMN items.version IS 'R-18 乐观锁：商品占用必须条件更新';
COMMENT ON COLUMN items.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN items.updated_at IS '更新时间（审计字段）';
COMMENT ON COLUMN items.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN items.updated_by IS '更新人（审计字段）';

-- notifications
COMMENT ON COLUMN notifications.actor_id IS '触发者（系统通知为空）';

-- order_items
COMMENT ON COLUMN order_items.title_snapshot IS 'R-13 快照：商品后续编辑不影响历史订单';
COMMENT ON COLUMN order_items.condition_snapshot IS '成色快照（展示用）';

-- orders
COMMENT ON COLUMN orders.order_no IS '业务单号，对外展示';
COMMENT ON COLUMN orders.trade_zone_id IS 'R-15';
COMMENT ON COLUMN orders.status IS '订单状态机 §3.1';
COMMENT ON COLUMN orders.estimated_total IS '估价合计快照（R-13 仅展示）';
COMMENT ON COLUMN orders.paid_at IS 'R-08';
COMMENT ON COLUMN orders.delivered_at IS 'O-06';
COMMENT ON COLUMN orders.completed_at IS 'O-07';
COMMENT ON COLUMN orders.cancel_reason IS 'O-03/O-04/O-11 三条取消路径';
COMMENT ON COLUMN orders.review_status IS '评价状态机 §8（订单维度）';
COMMENT ON COLUMN orders.review_deadline IS 'A-07 互评窗口 7 天';

-- payments
COMMENT ON COLUMN payments.amount IS '估价合计（非真实扣款）';

-- penalties
COMMENT ON COLUMN penalties.target_user_id IS '被处罚用户';
COMMENT ON COLUMN penalties.operator_id IS '操作管理员';
COMMENT ON COLUMN penalties.item_id IS 'ITEM_TAKEDOWN 关联商品';
COMMENT ON COLUMN penalties.reason IS 'R-32/R-34：治理动作必须填原因';
COMMENT ON COLUMN penalties.lifted_at IS '解封时刻';
COMMENT ON COLUMN penalties.lift_source IS '解封来源（系统/申诉/管理员手动）';
COMMENT ON COLUMN penalties.version IS 'R-18：到期自动解封 vs 手动解封并发只允许一方成功';

-- pickup_points
COMMENT ON COLUMN pickup_points.name IS '取货点名称，如「图书馆正门台阶右侧」';

-- reports
COMMENT ON COLUMN reports.reason IS 'FR-211 需选择原因';
COMMENT ON COLUMN reports.handled_by IS '处理管理员';

-- reviews
COMMENT ON COLUMN reviews.reviewer_role IS 'BUYER | SELLER（R-14）';
COMMENT ON COLUMN reviews.rating IS '1–5';
COMMENT ON COLUMN reviews.version IS 'R-18：评价入口关闭与提交并发时条件更新';

-- swap_bundle_items
COMMENT ON COLUMN swap_bundle_items.item_status IS 'ACTIVE=进行中（占用中）';
COMMENT ON COLUMN swap_bundle_items.price_snapshot IS 'R-13 加入包时估价快照';

-- swap_bundles
COMMENT ON COLUMN swap_bundles.owner_id IS 'SB-02 包主';
COMMENT ON COLUMN swap_bundles.trade_zone_id IS 'R-15';
COMMENT ON COLUMN swap_bundles.title IS '可选命名，便于识别';
COMMENT ON COLUMN swap_bundles.packed_at IS 'B-04 打包完成时刻';
COMMENT ON COLUMN swap_bundles.completed_at IS 'B-06';
COMMENT ON COLUMN swap_bundles.released_at IS 'B-07';
COMMENT ON COLUMN swap_bundles.version IS 'R-18';
COMMENT ON COLUMN swap_bundles.deleted_at IS '软删除（B-11）；被提议引用过的包禁止物理删除';

-- swap_codes
COMMENT ON COLUMN swap_codes.verified_at IS 'S-08 核销时刻';
COMMENT ON COLUMN swap_codes.verified_by IS '核销人（提议双方之一）';
COMMENT ON COLUMN swap_codes.version IS 'R-18：并发核销只允许一方成功';

-- swap_proposal_items
COMMENT ON COLUMN swap_proposal_items.title_snapshot IS 'R-13 快照';
COMMENT ON COLUMN swap_proposal_items.price_snapshot IS 'R-13 快照';

-- swap_proposals
COMMENT ON COLUMN swap_proposals.proposer_bundle_id IS '发起方交换包';
COMMENT ON COLUMN swap_proposals.target_bundle_id IS '仅反提案时使用';
COMMENT ON COLUMN swap_proposals.parent_proposal_id IS '反提案来源（R-28 仅一轮）';
COMMENT ON COLUMN swap_proposals.trade_zone_id IS 'R-15 同区域';
COMMENT ON COLUMN swap_proposals.status IS '交换提议状态机 §4.1';
COMMENT ON COLUMN swap_proposals.termination_reason IS 'R-24 整笔作废 / 拒绝 / 过期 / 取消 原因';
COMMENT ON COLUMN swap_proposals.message IS '发起说明（结构化交互，不做聊天 OOS-08）';
COMMENT ON COLUMN swap_proposals.estimated_total_offer IS '发起方包估价合计快照（仅展示）';
COMMENT ON COLUMN swap_proposals.estimated_total_want IS '目标方商品估价合计快照（仅展示）';
COMMENT ON COLUMN swap_proposals.accepted_at IS 'S-02 整包接受时刻';
COMMENT ON COLUMN swap_proposals.completed_at IS 'S-10 交换完成时刻';
COMMENT ON COLUMN swap_proposals.target_confirmed_at IS 'R-27 目标方确认（2/2）';
COMMENT ON COLUMN swap_proposals.version IS 'R-18：接受/拒绝/过期并发只允许一方成功';

-- trade_zones
COMMENT ON COLUMN trade_zones.name IS '区域名称';
COMMENT ON COLUMN trade_zones.zone_type IS 'CAMPUS / COMMUNITY';
COMMENT ON COLUMN trade_zones.is_active IS '停用后新交易不可选，历史数据保留';

-- user_trade_zones
COMMENT ON COLUMN user_trade_zones.id IS '主键';
COMMENT ON COLUMN user_trade_zones.user_id IS '用户（勾选人）';
COMMENT ON COLUMN user_trade_zones.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN user_trade_zones.updated_at IS '更新时间（审计字段）';
COMMENT ON COLUMN user_trade_zones.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN user_trade_zones.updated_by IS '更新人（审计字段）';

-- users
COMMENT ON COLUMN users.id IS '主键';
COMMENT ON COLUMN users.username IS 'FR-101 用户名唯一、大小写不敏感';
COMMENT ON COLUMN users.email IS 'FR-101 邮箱（可选，Q-06 无需验证）';
COMMENT ON COLUMN users.nickname IS 'FR-105 昵称 1-20 字符，不唯一（A-04/A-09）';
COMMENT ON COLUMN users.avatar_url IS 'FR-106 头像（1 张）';
COMMENT ON COLUMN users.role IS '管理员为 ADMIN（P-00-A 不参与交易）';
COMMENT ON COLUMN users.restricted_until IS '处罚到期时间，用于前端提示（E-03）';
COMMENT ON COLUMN users.last_login_at IS 'FR-103 登录态相关';
COMMENT ON COLUMN users.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN users.updated_at IS '更新时间（审计字段，由 trg_users_updated_at 自动刷新）';
COMMENT ON COLUMN users.created_by IS '审计字段';
COMMENT ON COLUMN users.updated_by IS '审计字段';


-- ---------------------------------------------------------------------
-- 14. 通用列注释补充（审计列、外键列与常见状态列，使每列均可通过 \d+ 自查）
--     与第 13 节合并后覆盖全部业务列；修改注释请新增 V4+ 迁移。
-- ---------------------------------------------------------------------

-- trade_zones
COMMENT ON COLUMN trade_zones.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN trade_zones.sort_order IS '排序值（升序展示）';
COMMENT ON COLUMN trade_zones.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN trade_zones.updated_at IS '更新时间（审计字段，触发器自动刷新）';

-- pickup_points
COMMENT ON COLUMN pickup_points.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN pickup_points.trade_zone_id IS '所属交易区域（R-15 必须与交易方同区域）';
COMMENT ON COLUMN pickup_points.point_type IS '取货点类型（LIBRARY/CANTEEN/TEACHING/DORM/GATE/SQUARE）';
COMMENT ON COLUMN pickup_points.sort_order IS '排序值（升序展示）';
COMMENT ON COLUMN pickup_points.is_active IS '是否启用';
COMMENT ON COLUMN pickup_points.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN pickup_points.updated_at IS '更新时间（审计字段，触发器自动刷新）';

-- item_occupations
COMMENT ON COLUMN item_occupations.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN item_occupations.item_id IS '关联商品';
COMMENT ON COLUMN item_occupations.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN item_occupations.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN item_occupations.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN item_occupations.updated_by IS '更新人（审计字段）';

-- cart_items
COMMENT ON COLUMN cart_items.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN cart_items.item_id IS '关联商品';
COMMENT ON COLUMN cart_items.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN cart_items.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN cart_items.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN cart_items.updated_by IS '更新人（审计字段）';

-- orders
COMMENT ON COLUMN orders.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN orders.buyer_id IS '买家（下单人）';
COMMENT ON COLUMN orders.pickup_point_selected_at IS '公共取货点选择时间（O-05）';
COMMENT ON COLUMN orders.cancelled_at IS '取消时间（O-03/O-04/O-11）';
COMMENT ON COLUMN orders.cancel_note IS '取消补充说明（如系统超时说明）';
COMMENT ON COLUMN orders.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN orders.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN orders.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN orders.updated_by IS '更新人（审计字段）';

-- order_items
COMMENT ON COLUMN order_items.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN order_items.order_id IS '关联订单';
COMMENT ON COLUMN order_items.item_id IS '关联商品';
COMMENT ON COLUMN order_items.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN order_items.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN order_items.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN order_items.updated_by IS '更新人（审计字段）';

-- payments
COMMENT ON COLUMN payments.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN payments.order_id IS '关联订单';
COMMENT ON COLUMN payments.payer_id IS '付款人（模拟付款）';
COMMENT ON COLUMN payments.status IS '状态字段（取值见对应状态机）';
COMMENT ON COLUMN payments.paid_at IS '模拟付款时间（R-08）';
COMMENT ON COLUMN payments.failure_reason IS '失败原因（模拟付款失败时）';
COMMENT ON COLUMN payments.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN payments.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN payments.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN payments.updated_by IS '更新人（审计字段）';

-- swap_bundles
COMMENT ON COLUMN swap_bundles.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN swap_bundles.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN swap_bundles.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN swap_bundles.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN swap_bundles.updated_by IS '更新人（审计字段）';

-- swap_bundle_items
COMMENT ON COLUMN swap_bundle_items.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN swap_bundle_items.swap_bundle_id IS '关联交换包';
COMMENT ON COLUMN swap_bundle_items.item_id IS '关联商品';
COMMENT ON COLUMN swap_bundle_items.released_at IS '释放时间；非空表示该占用/关联已终结';
COMMENT ON COLUMN swap_bundle_items.release_reason IS '释放原因（审计与对账用）';
COMMENT ON COLUMN swap_bundle_items.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN swap_bundle_items.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN swap_bundle_items.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN swap_bundle_items.updated_by IS '更新人（审计字段）';

-- swap_proposals
COMMENT ON COLUMN swap_proposals.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN swap_proposals.proposal_no IS '交换提议业务编号，对外展示';
COMMENT ON COLUMN swap_proposals.proposer_id IS '发起方用户（贡献交换包的一方）';
COMMENT ON COLUMN swap_proposals.target_id IS '目标方用户（其商品被请求的一方）';
COMMENT ON COLUMN swap_proposals.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN swap_proposals.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN swap_proposals.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN swap_proposals.updated_by IS '更新人（审计字段）';

-- swap_proposal_items
COMMENT ON COLUMN swap_proposal_items.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN swap_proposal_items.swap_proposal_id IS '关联交换提议';
COMMENT ON COLUMN swap_proposal_items.item_id IS '关联商品';
COMMENT ON COLUMN swap_proposal_items.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN swap_proposal_items.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN swap_proposal_items.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN swap_proposal_items.updated_by IS '更新人（审计字段）';

-- swap_codes
COMMENT ON COLUMN swap_codes.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN swap_codes.swap_proposal_id IS '关联交换提议';
COMMENT ON COLUMN swap_codes.status IS '状态字段（取值见对应状态机）';
COMMENT ON COLUMN swap_codes.expires_at IS '过期绝对时间（服务端时间为准，R-19）';
COMMENT ON COLUMN swap_codes.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN swap_codes.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN swap_codes.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN swap_codes.updated_by IS '更新人（审计字段）';

-- reviews
COMMENT ON COLUMN reviews.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN reviews.swap_proposal_id IS '关联交换提议';
COMMENT ON COLUMN reviews.reviewer_id IS '评价提交人';
COMMENT ON COLUMN reviews.reviewee_id IS '被评价人';
COMMENT ON COLUMN reviews.content IS '评价内容（可选）';
COMMENT ON COLUMN reviews.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN reviews.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN reviews.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN reviews.updated_by IS '更新人（审计字段）';

-- reports
COMMENT ON COLUMN reports.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN reports.reporter_id IS '举报人';
COMMENT ON COLUMN reports.item_id IS '关联商品';
COMMENT ON COLUMN reports.detail IS '举报补充说明';
COMMENT ON COLUMN reports.status IS '状态字段（取值见对应状态机）';
COMMENT ON COLUMN reports.handled_at IS '举报处理时间';
COMMENT ON COLUMN reports.handle_note IS '举报处理结论';
COMMENT ON COLUMN reports.version IS 'R-18 乐观锁/条件更新版本号';
COMMENT ON COLUMN reports.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN reports.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN reports.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN reports.updated_by IS '更新人（审计字段）';

-- penalties
COMMENT ON COLUMN penalties.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN penalties.started_at IS '处罚开始时间';
COMMENT ON COLUMN penalties.lift_reason IS '解封原因或说明';
COMMENT ON COLUMN penalties.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN penalties.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN penalties.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN penalties.updated_by IS '更新人（审计字段）';

-- appeals
COMMENT ON COLUMN appeals.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN appeals.penalty_id IS '关联处罚记录';
COMMENT ON COLUMN appeals.status IS '状态字段（取值见对应状态机）';
COMMENT ON COLUMN appeals.resolved_at IS '申诉处理完成时间';
COMMENT ON COLUMN appeals.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN appeals.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN appeals.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN appeals.updated_by IS '更新人（审计字段）';

-- notifications
COMMENT ON COLUMN notifications.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN notifications.recipient_id IS '通知接收人';
COMMENT ON COLUMN notifications.type IS '类型枚举';
COMMENT ON COLUMN notifications.title IS '标题';
COMMENT ON COLUMN notifications.body IS '正文内容';
COMMENT ON COLUMN notifications.ref_id IS '多态业务引用 ID（配合 ref_type 使用）';
COMMENT ON COLUMN notifications.is_read IS '是否已读';
COMMENT ON COLUMN notifications.read_at IS '已读时间';
COMMENT ON COLUMN notifications.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN notifications.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN notifications.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN notifications.updated_by IS '更新人（审计字段）';

-- idempotency_records
COMMENT ON COLUMN idempotency_records.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN idempotency_records.user_id IS '关联用户';
COMMENT ON COLUMN idempotency_records.status IS '状态字段（取值见对应状态机）';
COMMENT ON COLUMN idempotency_records.error_message IS '失败原因（幂等记录）';
COMMENT ON COLUMN idempotency_records.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN idempotency_records.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN idempotency_records.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN idempotency_records.updated_by IS '更新人（审计字段）';

-- audit_logs
COMMENT ON COLUMN audit_logs.id IS '主键（IDENTITY 自增）';
COMMENT ON COLUMN audit_logs.target_id IS '审计对象 ID';
COMMENT ON COLUMN audit_logs.ip_address IS '请求来源 IP（审计）';
COMMENT ON COLUMN audit_logs.user_agent IS '请求 User-Agent（审计）';
COMMENT ON COLUMN audit_logs.created_at IS '创建时间（审计字段）';
COMMENT ON COLUMN audit_logs.updated_at IS '更新时间（审计字段，触发器自动刷新）';
COMMENT ON COLUMN audit_logs.created_by IS '创建人（审计字段）';
COMMENT ON COLUMN audit_logs.updated_by IS '更新人（审计字段）';

-- =====================================================================
-- V1 结束
-- 后续迁移：V2__seed_reference_data.sql（预设字典）、V3__seed_development_data.sql（开发种子）
-- =====================================================================
