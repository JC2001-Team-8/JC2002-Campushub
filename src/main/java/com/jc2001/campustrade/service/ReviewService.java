package com.jc2001.campustrade.service;

import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateReviewRequest;
import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.ReviewStatusResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * ReviewService（Service 接口骨架）。
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
public interface ReviewService {

    /**
     * 订单互评（FR-312）
     *
     * <p>对应 operationId: <code>createOrderReview</code>
     * <p>TODO 下一步：实现
     */
    Review createOrderReview(Long orderId, String idempotencyKey, CreateReviewRequest body);

    /**
     * 查看某订单的评价（permission-matrix 4.14）
     *
     * <p>对应 operationId: <code>listOrderReviews</code>
     * <p>TODO 下一步：实现
     */
    ReviewStatusResponse listOrderReviews(Long orderId);

    /**
     * 我收到的评价（permission-matrix 4.14）
     *
     * <p>对应 operationId: <code>listMyReceivedReviews</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Review> listMyReceivedReviews(Integer page, Integer size);

}
