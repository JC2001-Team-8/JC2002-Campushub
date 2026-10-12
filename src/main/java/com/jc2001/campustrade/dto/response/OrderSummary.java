package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.OrderStatus;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 订单摘要（列表用，PF-11）
 *
 * <p>对应契约 {@code components.schemas.OrderSummary}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "OrderSummary", description = "订单摘要（列表用，PF-11）")
public class OrderSummary {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "orderNo", example = "ORD-20261012-0002")
    private String orderNo;

    @NotNull
    @Schema(description = "status")
    private OrderStatus status;

    /**
     * 当前用户在这笔订单中的关系角色（**卖家/买家是关系角色，不是注册身份**）：
     * `BUYER` = 我是买家；`SELLER` = 我是卖家。前端据此渲染不同操作区。
     * 
     */
    @NotNull
    @Schema(description = "当前用户在这笔订单中的关系角色（**卖家/买家是关系角色，不是注册身份**）： `BUYER` = 我是买家；`SELLER` = 我是卖家。前端据此渲染不同操作区。", example = "BUYER")
    private String role;

    @NotNull
    @Schema(description = "estimatedTotal")
    private DecimalString estimatedTotal;

    @Schema(description = "itemCount")
    private Integer itemCount;

    @Schema(description = "coverImageUrl")
    private String coverImageUrl;

    @Schema(description = "counterparty")
    private UserProfile counterparty;

    @Schema(description = "tradeZoneId")
    private Long tradeZoneId;

    /**
     * 已选公共取货点名称；未选择时为 null（R-06）
     */
    @Schema(description = "已选公共取货点名称；未选择时为 null（R-06）", example = "【待确认】图书馆门口-校区A（占位）")
    private String pickupPointName;

    /**
     * 付款截止绝对时间（仅 `PENDING_PAYMENT` 有意义，R-07 / A-01）
     */
    @NotNull
    @Schema(description = "付款截止绝对时间（仅 `PENDING_PAYMENT` 有意义，R-07 / A-01）")
    private Instant expiresAt;

    /**
     * 剩余付款秒数，**由服务端计算**（R-19：**禁止前端自行推算**）。
     * 非 `PENDING_PAYMENT` 状态时为 0。
     * 
     */
    @NotNull
    @Schema(description = "剩余付款秒数，**由服务端计算**（R-19：**禁止前端自行推算**）。 非 `PENDING_PAYMENT` 状态时为 0。")
    private Long remainingSeconds;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public OrderSummary() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public DecimalString getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(DecimalString estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public UserProfile getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(UserProfile counterparty) {
        this.counterparty = counterparty;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public String getPickupPointName() {
        return pickupPointName;
    }

    public void setPickupPointName(String pickupPointName) {
        this.pickupPointName = pickupPointName;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "OrderSummary{"
                + "id=" + id + ", "
                + "orderNo=" + orderNo + ", "
                + "status=" + status + ", "
                + "role=" + role + ", "
                + "estimatedTotal=" + estimatedTotal + ", "
                + "itemCount=" + itemCount + ", "
                + "coverImageUrl=" + coverImageUrl + ", "
                + "counterparty=" + counterparty + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "pickupPointName=" + pickupPointName + ", "
                + "expiresAt=" + expiresAt + ", "
                + "remainingSeconds=" + remainingSeconds + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
