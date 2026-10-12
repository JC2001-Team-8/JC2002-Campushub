package com.jc2001.campustrade.service;

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
 * SwapService（Service 接口骨架）。
 *
 * <p><b>本步骤不实现业务逻辑</b>：实现类统一抛 UnsupportedOperationException。
 *
 * <p><b>返回原始领域类型</b>（如 OrderDetail / PageResult&lt;ItemSummary&gt;），
 * 由 Controller 负责包成 ApiResponse —— 包络属接口层职责，不下沉到 Service。
 *
 * <p><b>下一步实现必须遵守</b>（见 idempotency-and-concurrency.md）：
 * <ul>
 *   <li>状态转换使用条件更新或乐观锁，<b>禁止「先查后改」</b>（R-18）</li>
 *   <li>占用/释放必须与 items.status 投影<b>同一事务</b>（R-16 / DD-01）</li>
 *   <li>部分唯一索引冲突（SQLState 23505）映射为对应域 409，而非 500</li>
 *   <li>归属校验失败返回 403，不返回 404（E-02）</li>
 * </ul>
 */
public interface SwapService {

    /**
     * 创建交换包（FR-402 / R-21）
     *
     * <p>对应 operationId: <code>createSwapBundle</code>
     * <p>TODO 下一步：实现
     */
    SwapBundleDetail createSwapBundle(String idempotencyKey, CreateSwapBundleRequest body);

    /**
     * 我的交换包列表（permission-matrix 5.4 / PF-15）
     *
     * <p>对应 operationId: <code>listMySwapBundles</code>
     * <p>TODO 下一步：实现
     */
    PageResult<SwapBundleSummary> listMySwapBundles(Integer page, Integer size, List<SwapBundleStatus> status);

    /**
     * 交换包详情（PF-15）
     *
     * <p>对应 operationId: <code>getSwapBundle</code>
     * <p>TODO 下一步：实现
     */
    SwapBundleDetail getSwapBundle(Long bundleId);

    /**
     * 编辑交换包（permission-matrix 5.3）
     *
     * <p>对应 operationId: <code>updateSwapBundle</code>
     * <p>TODO 下一步：实现
     */
    SwapBundleDetail updateSwapBundle(Long bundleId, String idempotencyKey, UpdateSwapBundleRequest body);

    /**
     * 删除交换包（permission-matrix 5.5 / B-11）
     *
     * <p>对应 operationId: <code>deleteSwapBundle</code>
     * <p>TODO 下一步：实现
     */
    Void deleteSwapBundle(Long bundleId);

    /**
     * 发起交换提议（FR-404）
     *
     * <p>对应 operationId: <code>createSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail createSwapProposal(String idempotencyKey, CreateSwapProposalRequest body);

    /**
     * 我参与的交换提议（permission-matrix 5.7 / PF-17）
     *
     * <p>对应 operationId: <code>listSwapProposals</code>
     * <p>TODO 下一步：实现
     */
    PageResult<SwapProposalSummary> listSwapProposals(
            Integer page,
            Integer size,
            List<String> sort,
            String role,
            List<SwapProposalStatus> status
    );

    /**
     * 交换提议详情（PF-17）
     *
     * <p>对应 operationId: <code>getSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail getSwapProposal(Long proposalId);

    /**
     * 整包接受交换提议（FR-405）
     *
     * <p>对应 operationId: <code>acceptSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail acceptSwapProposal(Long proposalId, String idempotencyKey);

    /**
     * 拒绝交换提议（FR-406）
     *
     * <p>对应 operationId: <code>rejectSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail rejectSwapProposal(Long proposalId, String idempotencyKey, RejectSwapProposalRequest body);

    /**
     * 发起反提案（FR-407 / R-28）
     *
     * <p>对应 operationId: <code>counterSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail counterSwapProposal(Long proposalId, String idempotencyKey, CounterSwapProposalRequest body);

    /**
     * 取消进行中的交换提议（permission-matrix 5.16）
     *
     * <p>对应 operationId: <code>cancelSwapProposal</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail cancelSwapProposal(Long proposalId, String idempotencyKey);

    /**
     * 查看交换码（FR-409）
     *
     * <p>对应 operationId: <code>getSwapCode</code>
     * <p>TODO 下一步：实现
     */
    SwapCode getSwapCode(Long proposalId);

    /**
     * 校验并核销交换码（FR-409 / R-26）
     *
     * <p>对应 operationId: <code>verifySwapCode</code>
     * <p>TODO 下一步：实现
     */
    SwapCode verifySwapCode(Long proposalId, String idempotencyKey, VerifySwapCodeRequest body);

    /**
     * 双方确认（交付 / 收到）（FR-410 / R-27）
     *
     * <p>对应 operationId: <code>confirmSwap</code>
     * <p>TODO 下一步：实现
     */
    SwapProposalDetail confirmSwap(Long proposalId, String idempotencyKey, ConfirmSwapRequest body);

    /**
     * 交换互评（FR-312 适用于交换）
     *
     * <p>对应 operationId: <code>createSwapReview</code>
     * <p>TODO 下一步：实现
     */
    Review createSwapReview(Long proposalId, String idempotencyKey, CreateReviewRequest body);

}
