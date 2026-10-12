package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.OrderService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CancelOrderRequest;
import com.jc2001.campustrade.dto.request.MockPayRequest;
import com.jc2001.campustrade.dto.request.SelectPickupPointRequest;
import com.jc2001.campustrade.dto.request.SubmitOrderRequest;
import com.jc2001.campustrade.dto.response.OrderDetail;
import com.jc2001.campustrade.dto.response.OrderStatus;
import com.jc2001.campustrade.dto.response.OrderSummary;
import com.jc2001.campustrade.dto.response.PaymentResult;
import com.jc2001.campustrade.dto.response.SubmitOrderResponse;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * OrderService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 提交订单 / 库存预留（FR-303 / FR-304）（未实现）
     *
     * <p>operationId: <code>submitOrder</code>
     */
    @Override
    public SubmitOrderResponse submitOrder(String idempotencyKey, SubmitOrderRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement submitOrder");
    }

    /**
     * 我的订单列表（permission-matrix 4.5）（未实现）
     *
     * <p>operationId: <code>listOrders</code>
     */
    @Override
    public PageResult<OrderSummary> listOrders(Integer page, Integer size, List<String> sort, String role, List<OrderStatus> status) {
        throw new UnsupportedOperationException(
                "TODO: implement listOrders");
    }

    /**
     * 订单详情与付款倒计时（FR-305 / PF-11）（未实现）
     *
     * <p>operationId: <code>getOrderDetail</code>
     */
    @Override
    public OrderDetail getOrderDetail(Long orderId) {
        throw new UnsupportedOperationException(
                "TODO: implement getOrderDetail");
    }

    /**
     * 模拟付款（FR-306）（未实现）
     *
     * <p>operationId: <code>mockPayOrder</code>
     */
    @Override
    public PaymentResult mockPayOrder(Long orderId, String idempotencyKey, MockPayRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement mockPayOrder");
    }

    /**
     * 主动取消订单（FR-308）（未实现）
     *
     * <p>operationId: <code>cancelOrder</code>
     */
    @Override
    public OrderDetail cancelOrder(Long orderId, String idempotencyKey, CancelOrderRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement cancelOrder");
    }

    /**
     * 选择公共取货点（FR-309 / R-06）（未实现）
     *
     * <p>operationId: <code>selectPickupPoint</code>
     */
    @Override
    public OrderDetail selectPickupPoint(Long orderId, String idempotencyKey, SelectPickupPointRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement selectPickupPoint");
    }

    /**
     * 卖家标记已交付（FR-310）（未实现）
     *
     * <p>operationId: <code>markOrderDelivered</code>
     */
    @Override
    public OrderDetail markOrderDelivered(Long orderId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement markOrderDelivered");
    }

    /**
     * 买家确认收货（FR-311）（未实现）
     *
     * <p>operationId: <code>confirmOrderReceipt</code>
     */
    @Override
    public OrderDetail confirmOrderReceipt(Long orderId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement confirmOrderReceipt");
    }

}
