package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.SwapService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.ConfirmSwapRequest;
import com.jc2001.campustrade.dto.request.CounterSwapProposalRequest;
import com.jc2001.campustrade.dto.request.CreateReviewRequest;
import com.jc2001.campustrade.dto.request.CreateSwapBundleRequest;
import com.jc2001.campustrade.dto.request.CreateSwapProposalRequest;
import com.jc2001.campustrade.dto.request.RejectSwapProposalRequest;
import com.jc2001.campustrade.dto.request.UpdateSwapBundleRequest;
import com.jc2001.campustrade.dto.request.VerifySwapCodeRequest;
import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.SwapBundleDetail;
import com.jc2001.campustrade.dto.response.SwapBundleStatus;
import com.jc2001.campustrade.dto.response.SwapBundleSummary;
import com.jc2001.campustrade.dto.response.SwapCode;
import com.jc2001.campustrade.dto.response.SwapProposalDetail;
import com.jc2001.campustrade.dto.response.SwapProposalStatus;
import com.jc2001.campustrade.dto.response.SwapProposalSummary;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * SwapService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class SwapServiceImpl implements SwapService {

    /**
     * 创建交换包（FR-402 / R-21）（未实现）
     *
     * <p>operationId: <code>createSwapBundle</code>
     */
    @Override
    public SwapBundleDetail createSwapBundle(String idempotencyKey, CreateSwapBundleRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createSwapBundle");
    }

    /**
     * 我的交换包列表（permission-matrix 5.4 / PF-15）（未实现）
     *
     * <p>operationId: <code>listMySwapBundles</code>
     */
    @Override
    public PageResult<SwapBundleSummary> listMySwapBundles(Integer page, Integer size, List<SwapBundleStatus> status) {
        throw new UnsupportedOperationException(
                "TODO: implement listMySwapBundles");
    }

    /**
     * 交换包详情（PF-15）（未实现）
     *
     * <p>operationId: <code>getSwapBundle</code>
     */
    @Override
    public SwapBundleDetail getSwapBundle(Long bundleId) {
        throw new UnsupportedOperationException(
                "TODO: implement getSwapBundle");
    }

    /**
     * 编辑交换包（permission-matrix 5.3）（未实现）
     *
     * <p>operationId: <code>updateSwapBundle</code>
     */
    @Override
    public SwapBundleDetail updateSwapBundle(Long bundleId, String idempotencyKey, UpdateSwapBundleRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateSwapBundle");
    }

    /**
     * 删除交换包（permission-matrix 5.5 / B-11）（未实现）
     *
     * <p>operationId: <code>deleteSwapBundle</code>
     */
    @Override
    public Void deleteSwapBundle(Long bundleId) {
        throw new UnsupportedOperationException(
                "TODO: implement deleteSwapBundle");
    }

    /**
     * 发起交换提议（FR-404）（未实现）
     *
     * <p>operationId: <code>createSwapProposal</code>
     */
    @Override
    public SwapProposalDetail createSwapProposal(String idempotencyKey, CreateSwapProposalRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createSwapProposal");
    }

    /**
     * 我参与的交换提议（permission-matrix 5.7 / PF-17）（未实现）
     *
     * <p>operationId: <code>listSwapProposals</code>
     */
    @Override
    public PageResult<SwapProposalSummary> listSwapProposals(Integer page, Integer size, List<String> sort, String role, List<SwapProposalStatus> status) {
        throw new UnsupportedOperationException(
                "TODO: implement listSwapProposals");
    }

    /**
     * 交换提议详情（PF-17）（未实现）
     *
     * <p>operationId: <code>getSwapProposal</code>
     */
    @Override
    public SwapProposalDetail getSwapProposal(Long proposalId) {
        throw new UnsupportedOperationException(
                "TODO: implement getSwapProposal");
    }

    /**
     * 整包接受交换提议（FR-405）（未实现）
     *
     * <p>operationId: <code>acceptSwapProposal</code>
     */
    @Override
    public SwapProposalDetail acceptSwapProposal(Long proposalId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement acceptSwapProposal");
    }

    /**
     * 拒绝交换提议（FR-406）（未实现）
     *
     * <p>operationId: <code>rejectSwapProposal</code>
     */
    @Override
    public SwapProposalDetail rejectSwapProposal(Long proposalId, String idempotencyKey, RejectSwapProposalRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement rejectSwapProposal");
    }

    /**
     * 发起反提案（FR-407 / R-28）（未实现）
     *
     * <p>operationId: <code>counterSwapProposal</code>
     */
    @Override
    public SwapProposalDetail counterSwapProposal(Long proposalId, String idempotencyKey, CounterSwapProposalRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement counterSwapProposal");
    }

    /**
     * 取消进行中的交换提议（permission-matrix 5.16）（未实现）
     *
     * <p>operationId: <code>cancelSwapProposal</code>
     */
    @Override
    public SwapProposalDetail cancelSwapProposal(Long proposalId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement cancelSwapProposal");
    }

    /**
     * 查看交换码（FR-409）（未实现）
     *
     * <p>operationId: <code>getSwapCode</code>
     */
    @Override
    public SwapCode getSwapCode(Long proposalId) {
        throw new UnsupportedOperationException(
                "TODO: implement getSwapCode");
    }

    /**
     * 校验并核销交换码（FR-409 / R-26）（未实现）
     *
     * <p>operationId: <code>verifySwapCode</code>
     */
    @Override
    public SwapCode verifySwapCode(Long proposalId, String idempotencyKey, VerifySwapCodeRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement verifySwapCode");
    }

    /**
     * 双方确认（交付 / 收到）（FR-410 / R-27）（未实现）
     *
     * <p>operationId: <code>confirmSwap</code>
     */
    @Override
    public SwapProposalDetail confirmSwap(Long proposalId, String idempotencyKey, ConfirmSwapRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement confirmSwap");
    }

    /**
     * 交换互评（FR-312 适用于交换）（未实现）
     *
     * <p>operationId: <code>createSwapReview</code>
     */
    @Override
    public Review createSwapReview(Long proposalId, String idempotencyKey, CreateReviewRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createSwapReview");
    }

}
