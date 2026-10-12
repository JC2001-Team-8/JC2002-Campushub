package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateReviewRequest;
import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.ReviewStatusResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 评价（FR-312）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "07-评价", description = "评价（FR-312）")
@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    /**
     * 订单互评（FR-312）
     *
     * <p>operationId: <code>createOrderReview</code>
     * <p>权限: USER
     * <p>状态转换: 'OPEN → BUYER_REVIEWED / SELLER_REVIEWED → BOTH_REVIEWED（评价状态机 §8）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "订单互评（FR-312）", operationId = "createOrderReview", description = "**必须为订单参与方（买家或卖家）且未被处罚**。买卖双方**各评一次**（R-14）。  前置条件（不满足返回 409）： - 订单状态必须为 `COMPLETED`，否则 `409 REVIEW_TRANSACTION_NOT_COMPLETED`（E-15）； - 必须在 **7 天互评窗口**内（假设 A-07），否则 `409 REVIEW_WINDOW_CLOSED`； - 该方**此前未评价过**，否则 `409 REVIEW_ALREADY_SUBMITTED`（IDEM-B）。  **迭代一明确不做**：评价标签、信用徽章、评价影响排序/推荐（OOS-09）。 评价公开可见、**不可修改**、暂不支持删除（假设 Q-20）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/orders/{orderId}/reviews")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Review>> createOrderReview(
            @Parameter(description = "") @PathVariable("orderId") Long orderId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CreateReviewRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement createOrderReview");
    }

    /**
     * 查看某订单的评价（permission-matrix 4.14）
     *
     * <p>operationId: <code>listOrderReviews</code>
     * <p>权限: USER
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看某订单的评价（permission-matrix 4.14）", operationId = "listOrderReviews", description = "**必须为订单参与方**。返回该订单的双方评价。 对方评价**仅在双方均已评价或窗口关闭后**才可见（`counterpartyReview` 否则为 null），避免互相偷看。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/orders/{orderId}/reviews")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> listOrderReviews(
            @Parameter(description = "") @PathVariable("orderId") Long orderId
    ) {
        throw new UnsupportedOperationException("TODO: implement listOrderReviews");
    }

    /**
     * 我收到的评价（permission-matrix 4.14）
     *
     * <p>operationId: <code>listMyReceivedReviews</code>
     * <p>权限: USER
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "我收到的评价（permission-matrix 4.14）", operationId = "listMyReceivedReviews", description = "\"**必须本人**。返回他人对我的评价，用于个人主页展示（迭代一不做信用徽章 OOS-09）。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/reviews")
    public ResponseEntity<ApiResponse<PageResult<Review>>> listMyReceivedReviews(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        throw new UnsupportedOperationException("TODO: implement listMyReceivedReviews");
    }

}
