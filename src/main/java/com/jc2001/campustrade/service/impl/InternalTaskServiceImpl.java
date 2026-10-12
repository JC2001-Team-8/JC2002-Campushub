package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.InternalTaskService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.dto.response.ScheduledTaskResult;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * InternalTaskService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class InternalTaskServiceImpl implements InternalTaskService {

    /**
     * 订单超时释放（FR-601 / FR-307 / R-07）（未实现）
     *
     * <p>operationId: <code>runOrderTimeoutReleaseTask</code>
     */
    @Override
    public ScheduledTaskResult runOrderTimeoutReleaseTask() {
        throw new UnsupportedOperationException(
                "TODO: implement runOrderTimeoutReleaseTask");
    }

    /**
     * 交换提议 72 小时过期（FR-602 / FR-408 / R-25）（未实现）
     *
     * <p>operationId: <code>runSwapProposalExpireTask</code>
     */
    @Override
    public ScheduledTaskResult runSwapProposalExpireTask() {
        throw new UnsupportedOperationException(
                "TODO: implement runSwapProposalExpireTask");
    }

    /**
     * 封禁到期自动解封（FR-506 / FR-603 / R-31）（未实现）
     *
     * <p>operationId: <code>runPenaltyAutoUnbanTask</code>
     */
    @Override
    public ScheduledTaskResult runPenaltyAutoUnbanTask() {
        throw new UnsupportedOperationException(
                "TODO: implement runPenaltyAutoUnbanTask");
    }

    /**
     * 评价窗口关闭（FR-604 / R-14）（未实现）
     *
     * <p>operationId: <code>runReviewWindowCloseTask</code>
     */
    @Override
    public ScheduledTaskResult runReviewWindowCloseTask() {
        throw new UnsupportedOperationException(
                "TODO: implement runReviewWindowCloseTask");
    }

}
