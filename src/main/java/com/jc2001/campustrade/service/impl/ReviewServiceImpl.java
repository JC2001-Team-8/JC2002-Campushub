package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.ReviewService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateReviewRequest;
import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.ReviewStatusResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * ReviewService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    /**
     * 订单互评（FR-312）（未实现）
     *
     * <p>operationId: <code>createOrderReview</code>
     */
    @Override
    public Review createOrderReview(Long orderId, String idempotencyKey, CreateReviewRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createOrderReview");
    }

    /**
     * 查看某订单的评价（permission-matrix 4.14）（未实现）
     *
     * <p>operationId: <code>listOrderReviews</code>
     */
    @Override
    public ReviewStatusResponse listOrderReviews(Long orderId) {
        throw new UnsupportedOperationException(
                "TODO: implement listOrderReviews");
    }

    /**
     * 我收到的评价（permission-matrix 4.14）（未实现）
     *
     * <p>operationId: <code>listMyReceivedReviews</code>
     */
    @Override
    public PageResult<Review> listMyReceivedReviews(Integer page, Integer size) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyReceivedReviews");
    }

}
