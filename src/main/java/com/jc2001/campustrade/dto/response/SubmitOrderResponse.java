package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.OrderDetail;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 提交订单结果。若发生拆单，`orders` 包含多笔；`split=true` 时前端需提示用户
「已按卖家拆成 N 笔订单，各自独立付款」。

 *
 * <p>对应契约 {@code components.schemas.SubmitOrderResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SubmitOrderResponse", description = "提交订单结果。若发生拆单，`orders` 包含多笔；`split=true` 时前端需提示用户 「已按卖家拆成 N 笔订单，各自独立付款」。")
public class SubmitOrderResponse {

    @NotNull
    @Size(min = 1)
    @Schema(description = "orders")
    private List<OrderDetail> orders;

    /**
     * 是否发生了跨卖家拆单（C-02）
     */
    @NotNull
    @Schema(description = "是否发生了跨卖家拆单（C-02）")
    private Boolean split;

    @NotNull
    @Schema(description = "orderCount")
    private Integer orderCount;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SubmitOrderResponse() {
    }

    public List<OrderDetail> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDetail> orders) {
        this.orders = orders;
    }

    public Boolean getSplit() {
        return split;
    }

    public void setSplit(Boolean split) {
        this.split = split;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    @Override
    public String toString() {
        return "SubmitOrderResponse{"
                + "orders=" + orders + ", "
                + "split=" + split + ", "
                + "orderCount=" + orderCount + ", "
                + '}';
    }
}
