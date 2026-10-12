-- =====================================================================
-- 迭代一 数据库验证脚本（只读 + 可重复执行）
-- =====================================================================
-- 文件     : db/verify_schema.sql
-- 版本     : v1.0
-- 日期     : 2026-10-12
-- 作者     : 数据库与领域模型设计负责人（待填写姓名）
-- 状态     : 草稿（待确认）
-- 用法     : docker exec -i jc2001-postgres psql -U jc2001 -d jc2001 -f - < db/verify_schema.sql
--            （Docker Compose 端口映射为 5432 时，见 README-db.md）
-- 说明     : 本脚本**只做查询**，不修改任何数据，可安全重复执行。
--            预期结果与判定标准见 docs/iteration1/design/db-verification.md。
-- =====================================================================

\echo '=============================================================='
\echo '1. Flyway 迁移历史（期望 3 条且 success = t）'
\echo '=============================================================='
SELECT installed_rank, version, description, type, success, execution_time
FROM flyway_schema_history
ORDER BY installed_rank;

\echo ''
\echo '=============================================================='
\echo '2. 业务表清单与数量（期望 25 张业务表 + 1 张 flyway_schema_history）'
\echo '=============================================================='
SELECT count(*) AS business_tables
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
ORDER BY table_name;

\echo ''
\echo '=============================================================='
\echo '3. 状态枚举与取值（必须与 docs/iteration1/state-machines.md 一致）'
\echo '=============================================================='
SELECT t.typname AS enum_name,
       string_agg(e.enumlabel, ', ' ORDER BY e.enumsortorder) AS enum_values
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname = 'public'
GROUP BY t.typname
ORDER BY t.typname;

\echo ''
\echo '=============================================================='
\echo '4. 关键唯一/排他约束（7 个部分唯一索引，承载 C-01/C-05/C-06/R-08/R-23/R-30/SB-03）'
\echo '=============================================================='
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexdef ILIKE '%UNIQUE INDEX%'
  AND indexdef ILIKE '%WHERE%'
ORDER BY indexname;

\echo ''
\echo '=============================================================='
\echo '5. 外键与删除规则统计（期望 0 个 CASCADE 出现在交易类表）'
\echo '=============================================================='
SELECT rc.delete_rule, count(*) AS fk_count
FROM information_schema.table_constraints tc
JOIN information_schema.referential_constraints rc ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
GROUP BY rc.delete_rule
ORDER BY rc.delete_rule;

\echo ''
\echo '=============================================================='
\echo '6. 种子数据行数'
\echo '=============================================================='
SELECT 'users' AS table_name, count(*) AS rows FROM users
UNION ALL SELECT 'trade_zones', count(*) FROM trade_zones
UNION ALL SELECT 'pickup_points', count(*) FROM pickup_points
UNION ALL SELECT 'categories', count(*) FROM categories
UNION ALL SELECT 'item_conditions', count(*) FROM item_conditions
UNION ALL SELECT 'user_trade_zones', count(*) FROM user_trade_zones
UNION ALL SELECT 'items', count(*) FROM items
UNION ALL SELECT 'item_images', count(*) FROM item_images
UNION ALL SELECT 'cart_items', count(*) FROM cart_items
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'order_items', count(*) FROM order_items
UNION ALL SELECT 'payments', count(*) FROM payments
UNION ALL SELECT 'item_occupations', count(*) FROM item_occupations
UNION ALL SELECT 'swap_bundles', count(*) FROM swap_bundles
UNION ALL SELECT 'swap_bundle_items', count(*) FROM swap_bundle_items
UNION ALL SELECT 'swap_proposals', count(*) FROM swap_proposals
UNION ALL SELECT 'swap_proposal_items', count(*) FROM swap_proposal_items
UNION ALL SELECT 'swap_codes', count(*) FROM swap_codes
UNION ALL SELECT 'reviews', count(*) FROM reviews
UNION ALL SELECT 'reports', count(*) FROM reports
UNION ALL SELECT 'penalties', count(*) FROM penalties
UNION ALL SELECT 'appeals', count(*) FROM appeals
UNION ALL SELECT 'notifications', count(*) FROM notifications
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
UNION ALL SELECT 'idempotency_records', count(*) FROM idempotency_records
ORDER BY table_name;

\echo ''
\echo '=============================================================='
\echo '7. 占位数据检查（应全部命中，且 name 含「待确认」或「示例」）'
\echo '=============================================================='
SELECT code, name, zone_type FROM trade_zones ORDER BY sort_order;

\echo ''
\echo '=============================================================='
\echo '8. 违规下架商品必须留痕（R-34：reason/at/by 三者非空）'
\echo '=============================================================='
SELECT id, title, status, banned_reason IS NOT NULL AS has_reason,
       banned_at IS NOT NULL AS has_at, banned_by IS NOT NULL AS has_by
FROM items WHERE status = 'BANNED';

\echo ''
\echo '=============================================================='
\echo '9. 处罚与用户状态一致性（BAN_3D 生效中 → 用户应为 RESTRICTED）'
\echo '=============================================================='
SELECT u.username, u.status AS user_status, u.restricted_until,
       p.penalty_type, p.status AS penalty_status, p.ends_at
FROM penalties p
JOIN users u ON u.id = p.target_user_id
ORDER BY p.id;

\echo ''
\echo '=============================================================='
\echo '10. 对账查询 A：占用互斥违规（期望 0 行）'
\echo '    任何一行都表示「同一商品存在多个进行中占用」，即 C-01/R-23 被破坏'
\echo '=============================================================='
SELECT item_id, count(*) AS active_occupations
FROM item_occupations
WHERE released_at IS NULL
GROUP BY item_id
HAVING count(*) > 1;

\echo ''
\echo '=============================================================='
\echo '11. 对账查询 B：商品占用态与占用台账不一致（期望 0 行）'
\echo '    RESERVED 必须对应 ORDER 占用；SWAPPING 必须对应 SWAP 占用'
\echo '=============================================================='
SELECT i.id, i.title, i.status AS item_status,
       io.ref_type, io.released_at
FROM items i
LEFT JOIN item_occupations io ON io.item_id = i.id AND io.released_at IS NULL
WHERE (i.status = 'RESERVED' AND (io.id IS NULL OR io.ref_type <> 'ORDER'))
   OR (i.status = 'SWAPPING' AND (io.id IS NULL OR io.ref_type <> 'SWAP'))
   OR (i.status = 'IN_SALE' AND io.id IS NOT NULL);

\echo ''
\echo '=============================================================='
\echo '12. 对账查询 C：已付款订单缺失付款记录（期望 0 行，跨表不变量需应用层保证）'
\echo '=============================================================='
SELECT o.id, o.order_no, o.status
FROM orders o
LEFT JOIN payments p ON p.order_id = o.id AND p.status = 'SUCCEEDED'
WHERE o.status IN ('PAID', 'DELIVERED', 'COMPLETED') AND p.id IS NULL;

\echo ''
\echo '=============================================================='
\echo '13. 对账查询 D：违反约束的业务数据（期望 0 行）'
\echo '    - 交换包件数不在 1–5（C-03/SB-01）'
\echo '    - 交换包内商品不属于包主（SB-02）'
\echo '    - 订单明细商品不属于订单卖家（C-02）'
\echo '    - 交换提议目标商品不属于目标方（C-04）'
\echo '    - 订单取货点不属于订单交易区域（R-06）'
\echo '    - 商品区域不在卖家已勾选区域内（R-15）'
\echo '=============================================================='
SELECT 'bundle_size_out_of_range' AS check_name, count(*) AS violations
FROM (
    SELECT sb.id FROM swap_bundles sb
    JOIN swap_bundle_items sbi ON sbi.swap_bundle_id = sb.id AND sbi.item_status = 'ACTIVE'
    GROUP BY sb.id HAVING count(*) NOT BETWEEN 1 AND 5
) t
UNION ALL
SELECT 'bundle_item_not_owner', count(*)
FROM swap_bundle_items sbi
JOIN swap_bundles sb ON sb.id = sbi.swap_bundle_id
JOIN items i ON i.id = sbi.item_id
WHERE i.seller_id <> sb.owner_id
UNION ALL
SELECT 'order_item_not_seller', count(*)
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN items i ON i.id = oi.item_id
WHERE i.seller_id <> o.seller_id
UNION ALL
SELECT 'swap_target_not_target_user', count(*)
FROM swap_proposal_items spi
JOIN swap_proposals sp ON sp.id = spi.swap_proposal_id
JOIN items i ON i.id = spi.item_id
WHERE i.seller_id <> sp.target_id
UNION ALL
SELECT 'pickup_point_wrong_zone', count(*)
FROM orders o
JOIN pickup_points pp ON pp.id = o.pickup_point_id
WHERE pp.trade_zone_id <> o.trade_zone_id
UNION ALL
SELECT 'item_zone_not_selected_by_seller', count(*)
FROM items i
WHERE i.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_trade_zones utz
      WHERE utz.user_id = i.seller_id AND utz.trade_zone_id = i.trade_zone_id
  );

\echo ''
\echo '=============================================================='
\echo '14. 索引覆盖检查：关键查询是否有索引支撑'
\echo '=============================================================='
SELECT tablename, indexname
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('items', 'orders', 'item_occupations', 'swap_proposals', 'idempotency_records')
ORDER BY tablename, indexname;

\echo ''
\echo '=============================================================='
\echo '15. 审计字段与乐观锁字段覆盖检查'
\echo '    期望：每张业务表都有 created_at；可并发写表都有 version'
\echo '=============================================================='
SELECT c.table_name,
       bool_or(c.column_name = 'created_at') AS has_created_at,
       bool_or(c.column_name = 'updated_at') AS has_updated_at,
       bool_or(c.column_name = 'created_by') AS has_created_by,
       bool_or(c.column_name = 'version')    AS has_version
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_name = c.table_name AND t.table_schema = c.table_schema
WHERE c.table_schema = 'public'
  AND c.table_name <> 'flyway_schema_history'
  AND t.table_type = 'BASE TABLE'
GROUP BY c.table_name
ORDER BY c.table_name;

\echo ''
\echo '=============================================================='
\echo '验证脚本结束'
\echo '=============================================================='
