-- =====================================================================
-- 迭代一 修正：评价窗口关闭扫描的部分索引条件（FR-604）
-- =====================================================================
-- 文件     : V4__fix_review_window_index.sql
-- 版本     : v1.0
-- 日期     : 2026-10-12
-- 作者     : 数据库与领域模型设计负责人（待填写姓名）
-- 状态     : 草稿（待确认）
-- 目标库   : PostgreSQL 15
--
-- 背景（为什么需要这个迁移）
-- ---------------------------------------------------------------------
-- V1 中为 FR-604「评价窗口关闭」建立的支撑索引条件为：
--     WHERE review_status <> 'WINDOW_CLOSED'
-- 该条件**把终态 `BOTH_REVIEWED` 也纳入了扫描范围**，存在两个问题：
--
--   1. 违反状态机终态保护（docs/iteration1/state-machines.md §8）：
--      `BOTH_REVIEWED`（双方已评）与 `WINDOW_CLOSED` **同为终态**，受 R-05 的 IDEM-C 保护；
--      R-04「窗口到期 → WINDOW_CLOSED」的**起始状态是活跃态**：
--      OPEN / BUYER_REVIEWED / SELLER_REVIEWED。
--      若按原条件扫描，会把「双方都已完成评价」的订单**回退**为 `WINDOW_CLOSED`，
--      造成已完成的互评记录被错误标记为「窗口超期关闭」。
--
--   2. 索引选择性偏差：把终态行也放进部分索引，随数据增长浪费索引空间。
--
-- 本迁移仅**重建该部分索引**，不改表结构、不改数据、不改枚举。
-- 对应的接口契约修正见 docs/iteration1/design/openapi.yaml
--   → operationId `runReviewWindowCloseTask`（FR-604）的扫描条件与 x-locking。
--
-- 注意：本文件为 Flyway 版本化迁移，请勿修改已执行过的版本；后续变更请新增 V5+。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 重建评价窗口扫描索引：仅覆盖「未关闭的活跃态」
--    起始状态集合严格对齐 state-machines.md §8 的 R-04
-- ---------------------------------------------------------------------
DROP INDEX IF EXISTS idx_orders_review_window;

CREATE INDEX idx_orders_review_window
    ON orders (review_deadline)
    WHERE review_status IN ('OPEN', 'BUYER_REVIEWED', 'SELLER_REVIEWED');

COMMENT ON INDEX idx_orders_review_window IS
    'FR-604 评价窗口关闭扫描：仅覆盖未关闭的活跃态（OPEN / BUYER_REVIEWED / SELLER_REVIEWED）；'
    '必须排除终态 BOTH_REVIEWED（受 R-05 IDEM-C 终态保护）与 WINDOW_CLOSED';

-- ---------------------------------------------------------------------
-- 2. 数据自检：确认不存在「双方已评却仍被标记为窗口关闭」的异常行
--    （正常情况下返回 0 行；本语句为只读校验，不修改数据）
-- ---------------------------------------------------------------------
DO $$
DECLARE
    bad_count INTEGER;
BEGIN
    SELECT count(*) INTO bad_count
    FROM orders
    WHERE review_status = 'WINDOW_CLOSED'
      AND completed_at IS NOT NULL
      AND EXISTS (
          SELECT 1 FROM reviews r
          WHERE r.order_id = orders.id
          GROUP BY r.order_id
          HAVING count(*) >= 2
      );

    IF bad_count > 0 THEN
        RAISE WARNING 'FR-604 数据自检：发现 % 条「双方已评价但状态为 WINDOW_CLOSED」的订单，请人工核查', bad_count;
    ELSE
        RAISE NOTICE 'FR-604 数据自检通过：无「双方已评价却被标记窗口关闭」的订单';
    END IF;
END $$;

-- =====================================================================
-- V4 结束
-- =====================================================================
