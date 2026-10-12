-- =====================================================================
-- 迭代一 参考数据种子（预设字典）
-- =====================================================================
-- 文件     : V2__seed_reference_data.sql
-- 版本     : v1.0
-- 日期     : 2026-10-12
-- 作者     : 数据库与领域模型设计负责人（待填写姓名）
-- 状态     : 待确认（全部占位数据待 Q-01/Q-02/Q-03 确认后替换）
-- 目标库   : PostgreSQL 15
-- 依据文档 : docs/iteration1/requirements.md §2.1/§2.2/§2.3（服务范围与预设字典）
--            docs/iteration1/rules.md R-15（交易区域归属与跨区禁止）
--
-- 本脚本只包含「参照数据 / 字典」：
--   1. trade_zones     交易区域（校区 CAMPUS_A/B/C 与周边社区 COMMUNITY_*）
--   2. pickup_points   公共取货点（图书馆门口 / 食堂门口 / 教学楼大厅 等）
--   3. categories      商品分类
--   4. item_conditions 商品成色
--
-- =====================================================================
-- ⚠️ 占位数据说明（重要）
-- ---------------------------------------------------------------------
-- Q-01（最终服务哪 2-3 个校区及周边社区）、Q-02（交易区域清单与编码规则）、
-- Q-03（公共取货点清单与命名）尚未由用户确认。
-- 因此本脚本中的**全部名称与编码均为占位值**，使用下列约定：
--   校区     : CAMPUS_A / CAMPUS_B / CAMPUS_C
--   周边社区 : COMMUNITY_A1 / COMMUNITY_A2 / COMMUNITY_B1 ...
--   取货点   : PICKUP_<TYPE>_<ZONE>
-- 用户确认后，只需替换本文件中的 code 与 name（以及必要的增删），
-- 不需要修改 V1 表结构，也不需要改动其它任何表。
-- 每一处占位数据均以 `-- TODO: 待用户确认后替换` 标注。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 交易区域 trade_zones
--    R-15：用户可勾选 1–5 个常用区域；商品/订单/交换/取货点必须落在同一区域
--    最终只保留 2–3 个校区及其周边社区（requirements.md §2.1 范围冻结规则）
-- ---------------------------------------------------------------------

-- TODO: 待用户确认后替换（Q-01 / Q-02）—— 校区名称与最终保留数量（2–3 个）
INSERT INTO trade_zones (code, name, zone_type, parent_zone_id, sort_order, is_active) VALUES
    ('CAMPUS_A',    '【待确认】校区 A（占位名称）',        'CAMPUS',    NULL, 10, TRUE),
    ('CAMPUS_B',    '【待确认】校区 B（占位名称）',        'CAMPUS',    NULL, 20, TRUE),
    ('CAMPUS_C',    '【待确认】校区 C（占位名称）',        'CAMPUS',    NULL, 30, TRUE);

-- TODO: 待用户确认后替换（Q-01）—— 周边社区名称；parent_zone_id 表示归属校区
INSERT INTO trade_zones (code, name, zone_type, parent_zone_id, sort_order, is_active) VALUES
    ('COMMUNITY_A1', '【待确认】校区 A 周边社区 1（占位名称）', 'COMMUNITY', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), 11, TRUE),
    ('COMMUNITY_A2', '【待确认】校区 A 周边社区 2（占位名称）', 'COMMUNITY', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), 12, TRUE),
    ('COMMUNITY_B1', '【待确认】校区 B 周边社区 1（占位名称）', 'COMMUNITY', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_B'), 21, TRUE),
    ('COMMUNITY_C1', '【待确认】校区 C 周边社区 1（占位名称）', 'COMMUNITY', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_C'), 31, TRUE);

-- ---------------------------------------------------------------------
-- 2. 公共取货点 pickup_points
--    requirements.md §2.3：图书馆门口 / 食堂门口 / 教学楼大厅 / 宿舍区门口 / 校门口 / 社区广场
--    R-06：取货点必须属于订单所在交易区域；OOS-06：只用文字说明，不使用地图与经纬度
-- ---------------------------------------------------------------------

-- TODO: 待用户确认后替换（Q-03）—— 取货点具体名称、位置描述与开放时间
INSERT INTO pickup_points (code, trade_zone_id, name, point_type, description, sort_order, is_active) VALUES
    -- 校区 A
    ('PICKUP_LIBRARY_A',  (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), '【待确认】图书馆门口-校区A（占位）',   'LIBRARY',  '图书馆正门台阶右侧，有监控覆盖', 10, TRUE),
    ('PICKUP_CANTEEN_A',  (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), '【待确认】食堂门口-校区A（占位）',     'CANTEEN',  '第一食堂正门入口左侧',           20, TRUE),
    ('PICKUP_TEACHING_A', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), '【待确认】教学楼大厅-校区A（占位）',   'TEACHING', '第一教学楼一层大厅',             30, TRUE),
    ('PICKUP_DORM_A',     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), '【待确认】宿舍区门口-校区A（占位）',   'DORM',     '学生宿舍 1 号楼门口',            40, TRUE),
    ('PICKUP_GATE_A',     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_A'), '【待确认】校门口-校区A（占位）',       'GATE',     '校区A东门门卫室旁',              50, TRUE),
    -- 校区 B
    ('PICKUP_LIBRARY_B',  (SELECT id FROM trade_zones WHERE code = 'CAMPUS_B'), '【待确认】图书馆门口-校区B（占位）',   'LIBRARY',  '图书馆正门入口',                 10, TRUE),
    ('PICKUP_CANTEEN_B',  (SELECT id FROM trade_zones WHERE code = 'CAMPUS_B'), '【待确认】食堂门口-校区B（占位）',     'CANTEEN',  '第二食堂正门入口',               20, TRUE),
    ('PICKUP_TEACHING_B', (SELECT id FROM trade_zones WHERE code = 'CAMPUS_B'), '【待确认】教学楼大厅-校区B（占位）',   'TEACHING', '第二教学楼一层大厅',             30, TRUE),
    -- 校区 C
    ('PICKUP_LIBRARY_C',  (SELECT id FROM trade_zones WHERE code = 'CAMPUS_C'), '【待确认】图书馆门口-校区C（占位）',   'LIBRARY',  '图书馆正门入口',                 10, TRUE),
    ('PICKUP_GATE_C',     (SELECT id FROM trade_zones WHERE code = 'CAMPUS_C'), '【待确认】校门口-校区C（占位）',       'GATE',     '校区C南门门卫室旁',              20, TRUE),
    -- 社区（社区用户之间的交易可用广场/活动中心）
    ('PICKUP_SQUARE_A1',  (SELECT id FROM trade_zones WHERE code = 'COMMUNITY_A1'), '【待确认】社区广场-社区A1（占位）', 'SQUARE', '社区中心广场凉亭旁',            10, TRUE),
    ('PICKUP_GATE_A1',    (SELECT id FROM trade_zones WHERE code = 'COMMUNITY_A1'), '【待确认】社区门口-社区A1（占位）', 'GATE',   '社区居民区北门',                 20, TRUE),
    ('PICKUP_SQUARE_B1',  (SELECT id FROM trade_zones WHERE code = 'COMMUNITY_B1'), '【待确认】社区广场-社区B1（占位）', 'SQUARE', '社区活动中心门口',               10, TRUE);

-- ---------------------------------------------------------------------
-- 3. 商品分类 categories（FR-206；Q-21 假设仅管理员维护，用户不可自定义）
-- ---------------------------------------------------------------------
INSERT INTO categories (code, name, sort_order, is_active) VALUES
    ('TEXTBOOK',   '教材书籍', 10, TRUE),
    ('ELECTRONICS','电子数码', 20, TRUE),
    ('DAILY',      '生活用品', 30, TRUE),
    ('CLOTHING',   '服饰',     40, TRUE),
    ('SPORTS',     '运动器材', 50, TRUE),
    ('OTHER',      '其他',     999, TRUE);

-- ---------------------------------------------------------------------
-- 4. 商品成色 item_conditions（FR-207：预设枚举）
-- ---------------------------------------------------------------------
INSERT INTO item_conditions (code, name, sort_order, is_active) VALUES
    ('BRAND_NEW',      '全新',         10, TRUE),
    ('LIKE_NEW',       '几乎全新',     20, TRUE),
    ('LIGHTLY_USED',   '轻微使用痕迹', 30, TRUE),
    ('WELL_USED',      '明显使用痕迹', 40, TRUE),
    ('DEFECTIVE',      '有瑕疵',       50, TRUE);

-- =====================================================================
-- V2 结束
-- 说明：状态枚举（item_status / order_status / swap_proposal_status 等）已以
--       PostgreSQL ENUM 类型形式定义在 V1__init_schema.sql 中，无需字典表。
-- =====================================================================
