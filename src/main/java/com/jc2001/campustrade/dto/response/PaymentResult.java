package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.OrderStatus;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 模拟付款结果（PF-12）
 *
 * <p>对应契约 {@code components.schemas.PaymentResult}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "PaymentResult", description = "模拟付款结果（PF-12）")
public class PaymentResult {

    @NotNull
    @Schema(description = "orderId")
    private Long orderId;

    @NotNull
    @Schema(description = "orderNo", example = "ORD-20261012-0002")
    private String orderNo;

    @NotNull
    @Schema(description = "status")
    private OrderStatus status;

    @NotNull
    @Schema(description = "amount")
    private DecimalString amount;

    /**
     * 付款渠道，迭代一恒为 `MOCK`
     */
    @Schema(description = "付款渠道，迭代一恒为 `MOCK`", example = "MOCK")
    private String channel;

    @NotNull
    @Schema(description = "paidAt")
    private Instant paidAt;

    /**
     * 固定提示文案
     */
    @Schema(description = "固定提示文案", example = "模拟付款，未产生真实扣款")
    private String mockNotice;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public PaymentResult() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public DecimalString getAmount() {
        return amount;
    }

    public void setAmount(DecimalString amount) {
        this.amount = amount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getMockNotice() {
        return mockNotice;
    }

    public void setMockNotice(String mockNotice) {
        this.mockNotice = mockNotice;
    }

    @Override
    public String toString() {
        return "PaymentResult{"
                + "orderId=" + orderId + ", "
                + "orderNo=" + orderNo + ", "
                + "status=" + status + ", "
                + "amount=" + amount + ", "
                + "channel=" + channel + ", "
                + "paidAt=" + paidAt + ", "
                + "mockNotice=" + mockNotice + ", "
                + '}';
    }
}
