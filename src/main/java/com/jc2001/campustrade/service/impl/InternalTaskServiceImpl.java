package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.dto.response.ScheduledTaskResult;
import com.jc2001.campustrade.service.InternalTaskService;
import com.jc2001.campustrade.service.ItemService;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统定时任务实现。
 *
 * <p>切片 3 范围内实现：订单超时释放（FR-601 / R-07）、交换提议 72 小时过期（FR-602 / R-25）。
 * 另外两个任务（封禁自动解封、评价窗口关闭）属切片 4，暂保持未实现。
 *
 * <p>幂等性（IDEM-B）：所有状态推进均为条件更新，重复执行不产生重复副作用。
 */
@Service
public class InternalTaskServiceImpl implements InternalTaskService {

    /** 审计字段 created_by/updated_by 有外键约束，系统操作使用管理员账号作为 actor。 */
    private Long systemActor() {
        List<Long> ids = jdbc.queryForList("SELECT id FROM users WHERE role='ADMIN' ORDER BY id LIMIT 1", Long.class);
        if (!ids.isEmpty()) return ids.get(0);
        List<Long> any = jdbc.queryForList("SELECT id FROM users ORDER BY id LIMIT 1", Long.class);
        return any.isEmpty() ? null : any.get(0);
    }

    private final JdbcTemplate jdbc;
    private final ItemService items;

    public InternalTaskServiceImpl(JdbcTemplate jdbc, ItemService items) {
        this.jdbc = jdbc;
        this.items = items;
    }

    @Override @Transactional public ScheduledTaskResult runOrderTimeoutReleaseTask() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM orders WHERE status='PENDING_PAYMENT' AND now() >= expires_at ORDER BY id LIMIT 500",
                Long.class);
        int processed = 0, skipped = 0;
        for (Long orderId : ids) {
            int n = jdbc.update(
                    "UPDATE orders SET status='TIMEOUT_CANCELLED', cancelled_at=now(), cancel_reason='PAYMENT_TIMEOUT', "
                    + "version=version+1, updated_by=? WHERE id=? AND status='PENDING_PAYMENT' AND now() >= expires_at",
                    systemActor(), orderId);
            if (n != 1) { skipped++; continue; } // 与付款并发时被抢先，属正常跳过
            for (Long itemId : jdbc.queryForList("SELECT item_id FROM order_items WHERE order_id=?", Long.class, orderId)) {
                items.releaseOrderOccupation(itemId, orderId, systemActor(), "PAYMENT_TIMEOUT");
            }
            processed++;
        }
        ScheduledTaskResult result = new ScheduledTaskResult();
        result.setTask("runOrderTimeoutReleaseTask");
        result.setProcessed(processed);
        result.setSkipped(skipped);
        result.setExecutedAt(Instant.now());
        return result;
    }

    @Override @Transactional public ScheduledTaskResult runSwapProposalExpireTask() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM swap_proposals WHERE status='PENDING' AND now() >= expires_at ORDER BY id LIMIT 500", Long.class);
        int processed = 0, skipped = 0;
        for (Long proposalId : ids) {
            int n = jdbc.update(
                    "UPDATE swap_proposals SET status='EXPIRED', termination_reason='EXPIRED_72H', version=version+1 "
                    + "WHERE id=? AND status='PENDING' AND now() >= expires_at", proposalId);
            if (n != 1) { skipped++; continue; }
            List<Long> itemIds = jdbc.queryForList(
                    "SELECT item_id FROM swap_proposal_items WHERE swap_proposal_id=?", Long.class, proposalId);
            for (Long itemId : itemIds) {
                items.releaseSwapOccupation(itemId, proposalId, systemActor(), "EXPIRED_72H");
            }
            jdbc.update("UPDATE swap_bundles SET status='PACKED', version=version+1 WHERE id=("
                    + "SELECT proposer_bundle_id FROM swap_proposals WHERE id=?) AND status='IN_SWAP'", proposalId);
            processed++;
        }
        ScheduledTaskResult result = new ScheduledTaskResult();
        result.setTask("runSwapProposalExpireTask");
        result.setProcessed(processed);
        result.setSkipped(skipped);
        result.setExecutedAt(Instant.now());
        return result;
    }

    @Override public ScheduledTaskResult runPenaltyAutoUnbanTask() {
        throw new UnsupportedOperationException("TODO(slice4): implement runPenaltyAutoUnbanTask");
    }

    @Override public ScheduledTaskResult runReviewWindowCloseTask() {
        throw new UnsupportedOperationException("TODO(slice4): implement runReviewWindowCloseTask");
    }
}
