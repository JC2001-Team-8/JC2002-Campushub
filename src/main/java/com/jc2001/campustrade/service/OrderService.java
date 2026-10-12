package com.jc2001.campustrade.service;

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
 * OrderService（Service 接口骨架）。
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
public interface OrderService {

    /**
     * 提交订单 / 库存预留（FR-303 / FR-304）
     *
     * <p>对应 operationId: <code>submitOrder</code>
     * <p>TODO 下一步：实现
     */
    SubmitOrderResponse submitOrder(String idempotencyKey, SubmitOrderRequest body);

    /**
     * 我的订单列表（permission-matrix 4.5）
     *
     * <p>对应 operationId: <code>listOrders</code>
     * <p>TODO 下一步：实现
     */
    PageResult<OrderSummary> listOrders(
            Integer page,
            Integer size,
            List<String> sort,
            String role,
            List<OrderStatus> status
    );

    /**
     * 订单详情与付款倒计时（FR-305 / PF-11）
     *
     * <p>对应 operationId: <code>getOrderDetail</code>
     * <p>TODO 下一步：实现
     */
    OrderDetail getOrderDetail(Long orderId);

    /**
     * 模拟付款（FR-306）
     *
     * <p>对应 operationId: <code>mockPayOrder</code>
     * <p>TODO 下一步：实现
     */
    PaymentResult mockPayOrder(Long orderId, String idempotencyKey, MockPayRequest body);

    /**
     * 主动取消订单（FR-308）
     *
     * <p>对应 operationId: <code>cancelOrder</code>
     * <p>TODO 下一步：实现
     */
    OrderDetail cancelOrder(Long orderId, String idempotencyKey, CancelOrderRequest body);

    /**
     * 选择公共取货点（FR-309 / R-06）
     *
     * <p>对应 operationId: <code>selectPickupPoint</code>
     * <p>TODO 下一步：实现
     */
    OrderDetail selectPickupPoint(Long orderId, String idempotencyKey, SelectPickupPointRequest body);

    /**
     * 卖家标记已交付（FR-310）
     *
     * <p>对应 operationId: <code>markOrderDelivered</code>
     * <p>TODO 下一步：实现
     */
    OrderDetail markOrderDelivered(Long orderId, String idempotencyKey);

    /**
     * 买家确认收货（FR-311）
     *
     * <p>对应 operationId: <code>confirmOrderReceipt</code>
     * <p>TODO 下一步：实现
     */
    OrderDetail confirmOrderReceipt(Long orderId, String idempotencyKey);

}
