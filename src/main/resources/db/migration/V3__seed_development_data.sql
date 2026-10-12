-- =====================================================================
-- 迭代一 开发种子数据（仅用于本地开发与联调，禁止用于生产）
-- =====================================================================
-- 文件     : V3__seed_development_data.sql
-- 版本     : v1.0
-- 日期     : 2026-10-12
-- 作者     : 数据库与领域模型设计负责人（待填写姓名）
-- 状态     : 待确认（管理员账号来源与密码占位待 Q-04 确认）
-- 目标库   : PostgreSQL 15
-- 依据文档 : docs/iteration1/requirements.md §5（关键业务流程）
--            docs/iteration1/state-machines.md（各状态取值必须合法）
--            docs/iteration1/glossary-assumptions-questions.md（A-01/A-02/A-03/A-07）
--
-- =====================================================================
-- ⚠️ 本文件为「开发种子数据」，含占位账号与占位密码
-- ---------------------------------------------------------------------
-- Q-04（管理员账号来源）尚未确认。本脚本假设：
--   * 管理员账号由种子数据创建：admin@example.com
--   * 密码为**开发占位密码**，password_hash 使用格式合法的占位 bcrypt 串，
--     真实哈希在「后端工程骨架」步骤由应用初始化逻辑写入或重置。
--   * 生产环境**禁止**执行本迁移，或以环境变量注入方式替换。
-- 所有占位值均以 `-- TODO: 待用户确认后替换` 标注。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 用户 users
--    包含：1 名管理员 + 3 名普通用户（卖家/买家在同一批用户中体现关系角色）
-- ---------------------------------------------------------------------

-- TODO: 待用户确认后替换（Q-04）—— 管理员账号来源、邮箱与密码策略
INSERT INTO users (username, email, password_hash, nickname, avatar_url, role, status, restricted_until) VALUES
    ('admin', 'admin@example.com', '$2a$10$PLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDE', '平台管理员（开发占位）', NULL, 'ADMIN', 'ACTIVE', NULL);

-- TODO: 待用户确认后替换 —— 示例用户账号与密码，仅用于本地开发
INSERT INTO users (username, email, password_hash, nickname, avatar_url, role, status, restricted_until) VALUES
    ('alice', 'alice@example.com', '$2a$10$PLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDE', '爱丽丝（示例卖家）', '/static/avatars/alice.png', 'USER', 'ACTIVE', NULL),
    ('bob',   'bob@example.com',   '$2a$10$PLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDE', '鲍勃（示例买家）',   '/static/avatars/bob.png',   'USER', 'ACTIVE', NULL),
    ('carol', 'carol@example.com', '$2a$10$PLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDERHASHPLACEHOLDE', '卡罗尔（示例用户）', NULL,                        'USER', 'ACTIVE', NULL);

-- ---------------------------------------------------------------------
-- 2. 常用交易区域 user_trade_zones（FR-107：每人 1–5 个）
-- ---------------------------------------------------------------------
INSERT INTO user_trade_zones (user_id, trade_zone_id, created_by) VALUES
    ((SELECT id FROM users WHERE username = 'alice'), (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),     (SELECT id FROM users WHERE username = 'alice')),
    ((SELECT id FROM users WHERE username = 'alice'), (SELECT id FROM trade_zones WHERE code = 'COMMUNITY_A1'), (SELECT id FROM users WHERE username = 'alice')),
    ((SELECT id FROM users WHERE username = 'bob'),   (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),     (SELECT id FROM users WHERE username = 'bob')),
    ((SELECT id FROM users WHERE username = 'carol'), (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),     (SELECT id FROM users WHERE username = 'carol'));

-- ---------------------------------------------------------------------
-- 3. 商品 items
--    alice 发布 4 件：2 件在售（其中 1 件接受交换）、1 件已售出、1 件违规下架
--    carol 发布 1 件，将被 bob 的订单占用（RESERVED）
--    bob   发布 3 件，用于交换演示：carol 用其中 2 件作为交换包发起提议
-- ---------------------------------------------------------------------

-- alice 在售商品（1 件标记接受交换，供 bob 发起交换）
INSERT INTO items (seller_id, category_id, condition_id, trade_zone_id, title, description,
                   estimated_price, status, swap_preference, published_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM categories WHERE code = 'TEXTBOOK'),
     (SELECT id FROM item_conditions WHERE code = 'LIGHTLY_USED'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】高等数学教材（第七版）上册', '有少量笔记，无破损，适合下学期使用。',
     15.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '3 days',
     (SELECT id FROM users WHERE username = 'alice')),

-- alice 在售商品（暂不交换）
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM categories WHERE code = 'DAILY'),
     (SELECT id FROM item_conditions WHERE code = 'LIKE_NEW'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】宿舍小台灯（可调光）', '用了不到一个月，功能完好。',
     25.00, 'IN_SALE', 'NOT_ACCEPTING', now() - INTERVAL '2 days',
     (SELECT id FROM users WHERE username = 'alice')),

-- alice 违规下架商品：先以 IN_SALE 落库，再通过 UPDATE 完成违规下架处置。
-- 原因：V1 的 ck_items_banned_trace 要求 status=BANNED 时 banned_reason/banned_at/banned_by
--       必须同时非空，因此不能在一次 INSERT 中直接写入 BANNED。
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM categories WHERE code = 'OTHER'),
     (SELECT id FROM item_conditions WHERE code = 'WELL_USED'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】违规下架商品（演示用）', NULL,
     10.00, 'IN_SALE', 'NOT_ACCEPTING', now() - INTERVAL '5 days',
     (SELECT id FROM users WHERE username = 'alice'));

-- alice 违规下架商品的处置留痕（I-14 / P-02 / R-34）
UPDATE items
SET status        = 'BANNED',
    banned_reason = '示例：违反平台规定（演示数据）',
    banned_at     = now() - INTERVAL '2 days',
    banned_by     = (SELECT id FROM users WHERE username = 'admin'),
    updated_by    = (SELECT id FROM users WHERE username = 'admin')
WHERE title = '【示例】违规下架商品（演示用）';

-- carol 的在售商品（后续被 bob 的 PENDING_PAYMENT 订单占用 → RESERVED）
INSERT INTO items (seller_id, category_id, condition_id, trade_zone_id, title, description,
                   estimated_price, status, swap_preference, published_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM categories WHERE code = 'ELECTRONICS'),
     (SELECT id FROM item_conditions WHERE code = 'LIKE_NEW'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】蓝牙耳机（带充电盒）', '使用三个月，续航正常，附原装充电盒。',
     120.00, 'IN_SALE', 'NOT_ACCEPTING', now() - INTERVAL '1 day',
     (SELECT id FROM users WHERE username = 'carol'));

-- bob 的商品：2 件将进入 carol 的交换包，1 件已随交换完成售出
INSERT INTO items (seller_id, category_id, condition_id, trade_zone_id, title, description,
                   estimated_price, status, swap_preference, published_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM categories WHERE code = 'SPORTS'),
     (SELECT id FROM item_conditions WHERE code = 'LIGHTLY_USED'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】羽毛球拍（含拍套）', '打过几次，拍面无明显磨损。',
     60.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '4 days',
     (SELECT id FROM users WHERE username = 'bob')),

    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM categories WHERE code = 'CLOTHING'),
     (SELECT id FROM item_conditions WHERE code = 'BRAND_NEW'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】全新运动水壶（未拆封）', '尺寸不合，未使用过。',
     30.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '4 days',
     (SELECT id FROM users WHERE username = 'bob')),

    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM categories WHERE code = 'TEXTBOOK'),
     (SELECT id FROM item_conditions WHERE code = 'LIGHTLY_USED'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】大学英语四级真题集', '已随交换完成，用于演示 SOLD 终态。',
     20.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '10 days',
     (SELECT id FROM users WHERE username = 'bob'));

-- ---------------------------------------------------------------------
-- 4. 商品图片 item_images（FR-205：每商品 1–9 张，sort_order=0 为封面）
--    TODO: 待替换为真实可访问的本地静态资源路径
-- ---------------------------------------------------------------------
INSERT INTO item_images (item_id, image_url, sort_order, file_size, content_type, created_by)
SELECT i.id,
       '/static/uploads/demo/' || i.id || '-1.png',
       0,
       102400,
       'image/png',
       i.seller_id
FROM items i
WHERE i.deleted_at IS NULL;

INSERT INTO item_images (item_id, image_url, sort_order, file_size, content_type, created_by)
SELECT i.id,
       '/static/uploads/demo/' || i.id || '-2.png',
       1,
       98200,
       'image/png',
       i.seller_id
FROM items i
WHERE i.title IN ('【示例】高等数学教材（第七版）上册', '【示例】蓝牙耳机（带充电盒）');

-- ---------------------------------------------------------------------
-- 5. 购物车 cart_items（FR-301 / R-04）
-- ---------------------------------------------------------------------
INSERT INTO cart_items (user_id, item_id, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM items WHERE title = '【示例】宿舍小台灯（可调光）'),
     (SELECT id FROM users WHERE username = 'bob'));

-- ---------------------------------------------------------------------
-- 6. 订单 orders + 明细 order_items + 占用 item_occupations
--    演示 4 种订单状态：
--      ORD-DEMO-0001 PENDING_PAYMENT + 进行中占用（在售→RESERVED）
--      ORD-DEMO-0002 PAID + 进行中占用 + 已选公共取货点
--      ORD-DEMO-0003 DELIVERED + 进行中占用 + 已选公共取货点
--      ORD-DEMO-0004 TIMEOUT_CANCELLED + 占用已释放（商品回到 IN_SALE）
-- =====================================================================

-- ---- 订单 1：待付款（演示 R-07 付款倒计时，A-01 = 30 分钟）----
INSERT INTO orders (order_no, buyer_id, seller_id, trade_zone_id, status, estimated_total,
                    expires_at, version, created_by)
VALUES
    ('ORD-DEMO-0001',
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     'PENDING_PAYMENT',
     120.00,
     now() + INTERVAL '30 minutes',          -- R-07 / A-01：创建时刻 + 30 分钟
     0,
     (SELECT id FROM users WHERE username = 'bob'));

INSERT INTO order_items (order_id, item_id, title_snapshot, price_snapshot, condition_snapshot, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0001'),
     (SELECT id FROM items WHERE title = '【示例】蓝牙耳机（带充电盒）'),
     '【示例】蓝牙耳机（带充电盒）', 120.00, '几乎全新',
     (SELECT id FROM users WHERE username = 'bob'));

-- R-16：占用发生在下单时；同时把商品状态投影为 RESERVED
INSERT INTO item_occupations (item_id, ref_type, ref_id, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】蓝牙耳机（带充电盒）'),
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0001'),
     NULL,
     (SELECT id FROM users WHERE username = 'bob'));

UPDATE items SET status = 'RESERVED', updated_by = (SELECT id FROM users WHERE username = 'bob')
WHERE title = '【示例】蓝牙耳机（带充电盒）';

-- ---- 订单 2：已付款（演示 R-08 模拟付款，未接入真实支付）----
INSERT INTO orders (order_no, buyer_id, seller_id, trade_zone_id, pickup_point_id,
                    pickup_point_selected_at, status, estimated_total, expires_at, paid_at,
                    version, created_by)
VALUES
    ('ORD-DEMO-0002',
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     (SELECT id FROM pickup_points WHERE code = 'PICKUP_LIBRARY_A'),
     now() - INTERVAL '10 minutes',
     'PAID',
     25.00,
     now() + INTERVAL '20 minutes',
     now() - INTERVAL '15 minutes',
     0,
     (SELECT id FROM users WHERE username = 'bob'));

INSERT INTO order_items (order_id, item_id, title_snapshot, price_snapshot, condition_snapshot, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0002'),
     (SELECT id FROM items WHERE title = '【示例】宿舍小台灯（可调光）'),
     '【示例】宿舍小台灯（可调光）', 25.00, '几乎全新',
     (SELECT id FROM users WHERE username = 'bob'));

INSERT INTO item_occupations (item_id, ref_type, ref_id, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】宿舍小台灯（可调光）'),
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0002'),
     NULL,
     (SELECT id FROM users WHERE username = 'bob'));

UPDATE items SET status = 'RESERVED', updated_by = (SELECT id FROM users WHERE username = 'bob')
WHERE title = '【示例】宿舍小台灯（可调光）';

-- 模拟付款记录（R-08；uq_payments_order_succeeded 保证一单一成功记录）
INSERT INTO payments (order_id, payer_id, channel, status, amount, paid_at, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0002'),
     (SELECT id FROM users WHERE username = 'bob'),
     'MOCK', 'SUCCEEDED', 25.00, now() - INTERVAL '15 minutes',
     (SELECT id FROM users WHERE username = 'bob'));

-- ---- 订单 3：已交付待确认（演示 O-06 卖家交付）----
INSERT INTO orders (order_no, buyer_id, seller_id, trade_zone_id, pickup_point_id,
                    pickup_point_selected_at, status, estimated_total, expires_at, paid_at,
                    delivered_at, version, created_by)
VALUES
    ('ORD-DEMO-0003',
     (SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     (SELECT id FROM pickup_points WHERE code = 'PICKUP_CANTEEN_A'),
     now() - INTERVAL '2 hours',
     'DELIVERED',
     15.00,
     now() - INTERVAL '1 hour',
     now() - INTERVAL '90 minutes',
     now() - INTERVAL '30 minutes',
     0,
     (SELECT id FROM users WHERE username = 'carol'));

INSERT INTO order_items (order_id, item_id, title_snapshot, price_snapshot, condition_snapshot, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0003'),
     (SELECT id FROM items WHERE title = '【示例】高等数学教材（第七版）上册'),
     '【示例】高等数学教材（第七版）上册', 15.00, '轻微使用痕迹',
     (SELECT id FROM users WHERE username = 'carol'));

INSERT INTO item_occupations (item_id, ref_type, ref_id, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】高等数学教材（第七版）上册'),
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0003'),
     NULL,
     (SELECT id FROM users WHERE username = 'carol'));

UPDATE items SET status = 'RESERVED', swap_preference = 'NOT_ACCEPTING',
                 updated_by = (SELECT id FROM users WHERE username = 'carol')
WHERE title = '【示例】高等数学教材（第七版）上册';

INSERT INTO payments (order_id, payer_id, channel, status, amount, paid_at, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0003'),
     (SELECT id FROM users WHERE username = 'carol'),
     'MOCK', 'SUCCEEDED', 15.00, now() - INTERVAL '90 minutes',
     (SELECT id FROM users WHERE username = 'carol'));

-- ---- 订单 4：超时取消（演示 O-04 + R-07：释放占用，商品回到 IN_SALE）----
INSERT INTO orders (order_no, buyer_id, seller_id, trade_zone_id, status, estimated_total,
                    expires_at, cancelled_at, cancel_reason, cancel_note, version, created_by)
VALUES
    ('ORD-DEMO-0004',
     (SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     'TIMEOUT_CANCELLED',
     30.00,
     now() - INTERVAL '2 hours',
     now() - INTERVAL '2 hours',
     'PAYMENT_TIMEOUT',
     '系统定时任务示例：超过 30 分钟未付款，占用已释放（R-07）',
     0,
     (SELECT id FROM users WHERE username = 'carol'));

INSERT INTO order_items (order_id, item_id, title_snapshot, price_snapshot, condition_snapshot, created_by)
VALUES
    ((SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0004'),
     (SELECT id FROM items WHERE title = '【示例】全新运动水壶（未拆封）'),
     '【示例】全新运动水壶（未拆封）', 30.00, '全新',
     (SELECT id FROM users WHERE username = 'carol'));

-- 占用已释放（released_at 非空 + release_reason 非空，见 V1 ck_item_occupations_release）
INSERT INTO item_occupations (item_id, ref_type, ref_id, occupied_at, released_at, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】全新运动水壶（未拆封）'),
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0004'),
     now() - INTERVAL '3 hours',
     now() - INTERVAL '2 hours',
     'PAYMENT_TIMEOUT',
     (SELECT id FROM users WHERE username = 'carol'));

-- ---------------------------------------------------------------------
-- 7. 交换演示 1：待处理提议（carol 用自己的交换包向 bob 索要球拍）
--    S-01：创建提议即同时占用「交换包内商品」与「目标商品」→ 全部 SWAPPING
-- ---------------------------------------------------------------------

-- carol 的交换包（含 1 件自己的商品：蓝牙耳机已被订单占用，故改用 carol 另建的商品）
INSERT INTO items (seller_id, category_id, condition_id, trade_zone_id, title, description,
                   estimated_price, status, swap_preference, published_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM categories WHERE code = 'DAILY'),
     (SELECT id FROM item_conditions WHERE code = 'LIGHTLY_USED'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】桌面收纳盒（三层）', '用于交换演示的交换包物品。',
     40.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '2 days',
     (SELECT id FROM users WHERE username = 'carol'));

INSERT INTO swap_bundles (owner_id, trade_zone_id, title, status, estimated_total, packed_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】卡罗尔的交换包（待处理提议）',
     'IN_SWAP',
     40.00,
     now() - INTERVAL '1 hour',
     (SELECT id FROM users WHERE username = 'carol'));

INSERT INTO swap_bundle_items (swap_bundle_id, item_id, item_status, price_snapshot, created_by)
VALUES
    ((SELECT id FROM swap_bundles WHERE title = '【示例】卡罗尔的交换包（待处理提议）'),
     (SELECT id FROM items WHERE title = '【示例】桌面收纳盒（三层）'),
     'ACTIVE', 40.00,
     (SELECT id FROM users WHERE username = 'carol'));

-- 提议：carol（发起方）↔ bob（目标方）
INSERT INTO swap_proposals (proposal_no, proposer_id, target_id, proposer_bundle_id, parent_proposal_id,
                           trade_zone_id, status, counter_depth, message,
                           estimated_total_offer, estimated_total_want, expires_at, version, created_by)
VALUES
    ('SWP-DEMO-0001',
     (SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM swap_bundles WHERE title = '【示例】卡罗尔的交换包（待处理提议）'),
     NULL,
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     'PENDING',
     0,
     '想用收纳盒换你的羽毛球拍，可以吗？',
     40.00,
     60.00,
     now() + INTERVAL '72 hours',            -- R-25 / A-02：72 小时有效期
     0,
     (SELECT id FROM users WHERE username = 'carol'));

-- C-04：目标商品 ≥1 件，且必须全部属于同一 target（bob）
INSERT INTO swap_proposal_items (swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by)
VALUES
    ((SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0001'),
     (SELECT id FROM items WHERE title = '【示例】羽毛球拍（含拍套）'),
     '【示例】羽毛球拍（含拍套）', 60.00,
     (SELECT id FROM users WHERE username = 'carol'));

-- R-22/R-23：占用交换包内商品与目标商品（ref_type=SWAP，ref_id=swap_proposals.id）
INSERT INTO item_occupations (item_id, ref_type, ref_id, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】桌面收纳盒（三层）'),
     'SWAP',
     (SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0001'),
     NULL,
     (SELECT id FROM users WHERE username = 'carol')),
    ((SELECT id FROM items WHERE title = '【示例】羽毛球拍（含拍套）'),
     'SWAP',
     (SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0001'),
     NULL,
     (SELECT id FROM users WHERE username = 'carol'));

UPDATE items SET status = 'SWAPPING', swap_preference = 'NOT_ACCEPTING',
                 updated_by = (SELECT id FROM users WHERE username = 'carol')
WHERE title IN ('【示例】桌面收纳盒（三层）', '【示例】羽毛球拍（含拍套）');

-- ---------------------------------------------------------------------
-- 8. 交换演示 2：已完成的交换（含交换码已核销 + 双方确认 + 商品 SOLD）
--    bob 用自己的交换包向 carol 索要「桌面收纳盒」是不成立的（该物已占用），
--    故本演示使用 bob 的「大学英语四级真题集」与 alice 的交换包完成一笔历史交换。
--    TODO: 示例数据，待用户确认后替换
-- ---------------------------------------------------------------------

-- alice 的交换包（已随交换完成）
INSERT INTO swap_bundles (owner_id, trade_zone_id, title, status, estimated_total, packed_at,
                          completed_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】爱丽丝的交换包（已完成交换）',
     'COMPLETED',
     25.00,
     now() - INTERVAL '10 days',
     now() - INTERVAL '6 days',
     (SELECT id FROM users WHERE username = 'alice'));

INSERT INTO items (seller_id, category_id, condition_id, trade_zone_id, title, description,
                   estimated_price, status, swap_preference, published_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM categories WHERE code = 'DAILY'),
     (SELECT id FROM item_conditions WHERE code = 'LIKE_NEW'),
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     '【示例】保温杯（已完成交换）', '用于演示已完成交换的 SOLD 终态。',
     25.00, 'IN_SALE', 'ACCEPTING', now() - INTERVAL '12 days',
     (SELECT id FROM users WHERE username = 'alice'));

INSERT INTO swap_bundle_items (swap_bundle_id, item_id, item_status, price_snapshot,
                               released_at, release_reason, created_by)
VALUES
    ((SELECT id FROM swap_bundles WHERE title = '【示例】爱丽丝的交换包（已完成交换）'),
     (SELECT id FROM items WHERE title = '【示例】保温杯（已完成交换）'),
     'COMPLETED', 25.00,
     now() - INTERVAL '6 days', 'SWAP_COMPLETED',
     (SELECT id FROM users WHERE username = 'alice'));

-- 提议（已完成：双方确认齐备，见 V1 ck_swap_proposals_completed_trace）
INSERT INTO swap_proposals (proposal_no, proposer_id, target_id, proposer_bundle_id, parent_proposal_id,
                           trade_zone_id, status, counter_depth, message,
                           estimated_total_offer, estimated_total_want, expires_at,
                           accepted_at, completed_at, proposer_confirmed_at, target_confirmed_at,
                           version, created_by)
VALUES
    ('SWP-DEMO-0002',
     (SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM swap_bundles WHERE title = '【示例】爱丽丝的交换包（已完成交换）'),
     NULL,
     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'),
     'COMPLETED',
     0,
     '用保温杯换你的四级真题集。',
     25.00,
     20.00,
     now() - INTERVAL '7 days',
     now() - INTERVAL '7 days',
     now() - INTERVAL '6 days',
     now() - INTERVAL '6 days',
     now() - INTERVAL '6 days',
     0,
     (SELECT id FROM users WHERE username = 'alice'));

INSERT INTO swap_proposal_items (swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by)
VALUES
    ((SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     (SELECT id FROM items WHERE title = '【示例】大学英语四级真题集'),
     '【示例】大学英语四级真题集', 20.00,
     (SELECT id FROM users WHERE username = 'alice'));

-- 交换码（R-26：一次性，已核销 → status=VERIFIED 且必须有核销时间与核销人）
INSERT INTO swap_codes (swap_proposal_id, code, status, verified_at, verified_by, created_by)
VALUES
    ((SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     'SWP8A2F',                              -- TODO: 待确认交换码格式（Q-30）
     'VERIFIED',
     now() - INTERVAL '7 days',
     (SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM users WHERE username = 'alice'));

-- 占用：已随交换完成释放
INSERT INTO item_occupations (item_id, ref_type, ref_id, occupied_at, released_at, release_reason, created_by)
VALUES
    ((SELECT id FROM items WHERE title = '【示例】保温杯（已完成交换）'),
     'SWAP',
     (SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     now() - INTERVAL '8 days',
     now() - INTERVAL '6 days',
     'SWAP_COMPLETED',
     (SELECT id FROM users WHERE username = 'alice')),
    ((SELECT id FROM items WHERE title = '【示例】大学英语四级真题集'),
     'SWAP',
     (SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     now() - INTERVAL '8 days',
     now() - INTERVAL '6 days',
     'SWAP_COMPLETED',
     (SELECT id FROM users WHERE username = 'alice'));

-- 交换完成 → 双方商品 SOLD（I-13 / S-10）
UPDATE items SET status = 'SOLD', swap_preference = 'NOT_ACCEPTING',
                 updated_by = (SELECT id FROM users WHERE username = 'alice')
WHERE title IN ('【示例】保温杯（已完成交换）', '【示例】大学英语四级真题集');

-- ---------------------------------------------------------------------
-- 9. 评价 reviews（FR-312 / R-14：仅已完成交易；同一交易同一方一次）
--    针对已完成交换 SWP-DEMO-0002，双方各评一次
-- ---------------------------------------------------------------------
INSERT INTO reviews (swap_proposal_id, reviewer_id, reviewee_id, reviewer_role, rating, content, created_by)
VALUES
    ((SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     (SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM users WHERE username = 'bob'),
     'SELLER', 5, '交换顺利，对方很守时。',
     (SELECT id FROM users WHERE username = 'alice')),
    ((SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0002'),
     (SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM users WHERE username = 'alice'),
     'BUYER', 5, '物品与描述一致，沟通顺畅。',
     (SELECT id FROM users WHERE username = 'bob'));

-- ---------------------------------------------------------------------
-- 10. 举报 reports（FR-211 / C-06：同一用户对同一商品最多 1 条未处理举报）
-- ---------------------------------------------------------------------
INSERT INTO reports (reporter_id, item_id, reason, detail, status, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM items WHERE title = '【示例】违规下架商品（演示用）'),
     'PROHIBITED_ITEM',
     '示例：该商品疑似违反平台规定（演示数据）。',
     'PENDING',
     (SELECT id FROM users WHERE username = 'bob'));

-- ---------------------------------------------------------------------
-- 11. 处罚 penalties（FR-503 / R-30：固定 3 天；R-34 reason 必填）
--     演示 2 条：1 条生效中封禁、1 条商品违规下架处置（仅举证，不影响交易能力）
-- ---------------------------------------------------------------------

-- 生效中封禁（ends_at = started_at + 72h，A-03）
INSERT INTO penalties (target_user_id, operator_id, penalty_type, item_id, reason, status,
                       started_at, ends_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'admin'),
     'BAN_3D',
     NULL,
     '示例：多次发布违规商品（演示数据）',
     'ACTIVE',
     now() - INTERVAL '1 day',
     now() + INTERVAL '2 days',              -- 固定三天（A-03 / Q-09）
     (SELECT id FROM users WHERE username = 'admin'));

-- 违规下架处置（R-35：与 items.status=BANNED 对应，用于举证）
INSERT INTO penalties (target_user_id, operator_id, penalty_type, item_id, reason, status,
                       started_at, ends_at, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM users WHERE username = 'admin'),
     'ITEM_TAKEDOWN',
     (SELECT id FROM items WHERE title = '【示例】违规下架商品（演示用）'),
     '示例：商品信息违反平台规定（演示数据）',
     'ACTIVE',
     now() - INTERVAL '2 days',
     now() + INTERVAL '1 day',
     (SELECT id FROM users WHERE username = 'admin'));

-- carol 因生效中封禁 → 账号状态置为 RESTRICTED，并写入解封时间
-- （与 V1 ck_users_restricted_until 约束一致：RESTRICTED 必须有 restricted_until）
UPDATE users
SET status = 'RESTRICTED',
    restricted_until = now() + INTERVAL '2 days',
    updated_by = (SELECT id FROM users WHERE username = 'admin')
WHERE username = 'carol';

-- ---------------------------------------------------------------------
-- 12. 申诉 appeals（FR-505 / AP-01：一个处罚最多一条申诉）
--     演示 1 条已提交待处理的申诉
-- ---------------------------------------------------------------------
INSERT INTO appeals (penalty_id, appellant_id, reason, status, created_by)
VALUES
    ((SELECT p.id FROM penalties p
      JOIN users u ON u.id = p.target_user_id
      WHERE u.username = 'carol' AND p.penalty_type = 'BAN_3D'),
     (SELECT id FROM users WHERE username = 'carol'),
     '示例：该商品已主动下架，请复核处罚（演示数据）。',
     'SUBMITTED',
     (SELECT id FROM users WHERE username = 'carol'));

-- 申诉提交后处罚标记为 APPEALING（P-03：处罚仍生效，不停表）
UPDATE penalties
SET status = 'APPEALING',
    updated_by = (SELECT id FROM users WHERE username = 'carol')
WHERE id = (SELECT a.penalty_id FROM appeals a
            JOIN users u ON u.id = a.appellant_id
            WHERE u.username = 'carol');

-- ---------------------------------------------------------------------
-- 13. 通知 notifications（FR-507）
-- ---------------------------------------------------------------------
INSERT INTO notifications (recipient_id, actor_id, type, title, body, ref_type, ref_id, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'bob'),
     'ORDER_CREATED',
     '你的商品有新订单',
     '买家已下单，请等待付款。',
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0001'),
     (SELECT id FROM users WHERE username = 'bob')),

    ((SELECT id FROM users WHERE username = 'alice'),
     (SELECT id FROM users WHERE username = 'bob'),
     'ORDER_PAID',
     '订单已付款',
     '买家已完成模拟付款，请按约定取货点交付。',
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0002'),
     (SELECT id FROM users WHERE username = 'bob')),

    ((SELECT id FROM users WHERE username = 'bob'),
     (SELECT id FROM users WHERE username = 'carol'),
     'SWAP_PROPOSED',
     '收到新的交换提议',
     '对方希望用「桌面收纳盒（三层）」交换你的商品。',
     'SWAP',
     (SELECT id FROM swap_proposals WHERE proposal_no = 'SWP-DEMO-0001'),
     (SELECT id FROM users WHERE username = 'carol')),

    ((SELECT id FROM users WHERE username = 'carol'),
     (SELECT id FROM users WHERE username = 'admin'),
     'PENALTY_BANNED',
     '账号已被限制',
     '你因发布违规商品被限制交易，可在处罚期内提交申诉。',
     'PENALTY',
     (SELECT p.id FROM penalties p
      JOIN users u ON u.id = p.target_user_id
      WHERE u.username = 'carol' AND p.penalty_type = 'BAN_3D'),
     (SELECT id FROM users WHERE username = 'admin')),

    ((SELECT id FROM users WHERE username = 'admin'),
     (SELECT id FROM users WHERE username = 'carol'),
     'APPEAL_SUBMITTED',
     '有新的申诉待处理',
     '用户 carol 提交了处罚申诉，请及时处理。',
     'APPEAL',
     (SELECT a.id FROM appeals a
      JOIN users u ON u.id = a.appellant_id
      WHERE u.username = 'carol'),
     (SELECT id FROM users WHERE username = 'carol'));

-- ---------------------------------------------------------------------
-- 14. 审计日志 audit_logs（R-34：治理动作必须留痕）
-- ---------------------------------------------------------------------
INSERT INTO audit_logs (actor_id, actor_role, action, target_type, target_id, reason,
                        before_state, after_state, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'admin'),
     'ADMIN',
     'ITEM_BAN',
     'ITEM',
     (SELECT id FROM items WHERE title = '【示例】违规下架商品（演示用）'),
     '示例：违反平台规定（演示数据）',
     '{"status": "IN_SALE"}'::jsonb,
     '{"status": "BANNED"}'::jsonb,
     (SELECT id FROM users WHERE username = 'admin')),

    ((SELECT id FROM users WHERE username = 'admin'),
     'ADMIN',
     'USER_BAN',
     'USER',
     (SELECT id FROM users WHERE username = 'carol'),
     '示例：多次发布违规商品（演示数据）',
     '{"status": "ACTIVE"}'::jsonb,
     '{"status": "RESTRICTED"}'::jsonb,
     (SELECT id FROM users WHERE username = 'admin'));

-- ---------------------------------------------------------------------
-- 15. 幂等记录 idempotency_records（R-17 示例，演示「重复提交返回首次结果」）
-- ---------------------------------------------------------------------
INSERT INTO idempotency_records (user_id, idempotency_key, endpoint, request_hash, status,
                                response_status, response_body, resource_type, resource_id, created_by)
VALUES
    ((SELECT id FROM users WHERE username = 'bob'),
     'demo-0000-0000-0000-000000000001',
     'POST /orders',
     'demo-request-hash-0001',
     'COMPLETED',
     201,
     '{"orderNo": "ORD-DEMO-0001"}'::jsonb,
     'ORDER',
     (SELECT id FROM orders WHERE order_no = 'ORD-DEMO-0001'),
     (SELECT id FROM users WHERE username = 'bob'));

-- =====================================================================
-- V3 结束
-- 再次提醒：本迁移含开发占位账号与密码，**禁止在生产环境执行**。
-- =====================================================================
