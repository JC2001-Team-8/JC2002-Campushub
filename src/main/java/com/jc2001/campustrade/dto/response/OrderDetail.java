package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.OrderCancelReason;
import com.jc2001.campustrade.dto.response.OrderItem;
import com.jc2001.campustrade.dto.response.OrderStatus;
import com.jc2001.campustrade.dto.response.PickupPoint;
import com.jc2001.campustrade.dto.response.ReviewStatus;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 订单详情（PF-11）。操作可用性由 `status` + `role` 决定，前端**不得**自行推断状态转换
 *
 * <p>对应契约 {@code components.schemas.OrderDetail}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "OrderDetail", description = "订单详情（PF-11）。操作可用性由 `status` + `role` 决定，前端**不得**自行推断状态转换")
public class OrderDetail {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "orderNo", example = "ORD-20261012-0002")
    private String orderNo;

    @NotNull
    @Schema(description = "status")
    private OrderStatus status;

    @NotNull
    @Schema(description = "role", example = "BUYER")
    private String role;

    @NotNull
    @Schema(description = "estimatedTotal")
    private DecimalString estimatedTotal;

    @Schema(description = "priceDisclaimer", example = "合计为卖家自填估价之和，仅供参考")
    private String priceDisclaimer;

    @NotNull
    @Size(min = 1)
    @Schema(description = "items")
    private List<OrderItem> items;

    @NotNull
    @Schema(description = "buyer")
    private UserProfile buyer;

    @NotNull
    @Schema(description = "seller")
    private UserProfile seller;

    @NotNull
    @Schema(description = "tradeZone")
    private TradeZone tradeZone;

    /**
     * 已选公共取货点；未选择时为 null。**卖家未选定取货点前不可标记交付**（R-06）
     */
    @Schema(description = "已选公共取货点；未选择时为 null。**卖家未选定取货点前不可标记交付**（R-06）")
    private PickupPoint pickupPoint;

    @NotNull
    @Schema(description = "expiresAt")
    private Instant expiresAt;

    /**
     * 服务端计算的剩余付款秒数（R-19）
     */
    @NotNull
    @Schema(description = "服务端计算的剩余付款秒数（R-19）")
    private Long remainingSeconds;

    @Schema(description = "paidAt")
    private Instant paidAt;

    @Schema(description = "deliveredAt")
    private Instant deliveredAt;

    @Schema(description = "completedAt")
    private Instant completedAt;

    @Schema(description = "cancelledAt")
    private Instant cancelledAt;

    @Schema(description = "cancelReason")
    private OrderCancelReason cancelReason;

    /**
     * 取消说明（如系统超时说明）
     */
    @Schema(description = "取消说明（如系统超时说明）")
    private String cancelNote;

    @NotNull
    @Schema(description = "reviewStatus")
    private ReviewStatus reviewStatus;

    /**
     * 互评窗口截止时间（仅订单 `COMPLETED` 后有意义，假设 A-07 = 7 天）
     */
    @Schema(description = "互评窗口截止时间（仅订单 `COMPLETED` 后有意义，假设 A-07 = 7 天）")
    private Instant reviewDeadline;

    /**
     * 当前用户此刻是否可以评价（`status=COMPLETED` 且在窗口内且本人未评过）
     */
    @Schema(description = "当前用户此刻是否可以评价（`status=COMPLETED` 且在窗口内且本人未评过）")
    private Boolean canReview;

    @Schema(description = "version")
    private Integer version;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public OrderDetail() {
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

    public String getPriceDisclaimer() {
        return priceDisclaimer;
    }

    public void setPriceDisclaimer(String priceDisclaimer) {
        this.priceDisclaimer = priceDisclaimer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public UserProfile getBuyer() {
        return buyer;
    }

    public void setBuyer(UserProfile buyer) {
        this.buyer = buyer;
    }

    public UserProfile getSeller() {
        return seller;
    }

    public void setSeller(UserProfile seller) {
        this.seller = seller;
    }

    public TradeZone getTradeZone() {
        return tradeZone;
    }

    public void setTradeZone(TradeZone tradeZone) {
        this.tradeZone = tradeZone;
    }

    public PickupPoint getPickupPoint() {
        return pickupPoint;
    }

    public void setPickupPoint(PickupPoint pickupPoint) {
        this.pickupPoint = pickupPoint;
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

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public OrderCancelReason getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(OrderCancelReason cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelNote() {
        return cancelNote;
    }

    public void setCancelNote(String cancelNote) {
        this.cancelNote = cancelNote;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Instant getReviewDeadline() {
        return reviewDeadline;
    }

    public void setReviewDeadline(Instant reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public Boolean getCanReview() {
        return canReview;
    }

    public void setCanReview(Boolean canReview) {
        this.canReview = canReview;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "OrderDetail{"
                + "id=" + id + ", "
                + "orderNo=" + orderNo + ", "
                + "status=" + status + ", "
                + "role=" + role + ", "
                + "estimatedTotal=" + estimatedTotal + ", "
                + "priceDisclaimer=" + priceDisclaimer + ", "
                + "items=" + items + ", "
                + "buyer=" + buyer + ", "
                + "seller=" + seller + ", "
                + "tradeZone=" + tradeZone + ", "
                + "pickupPoint=" + pickupPoint + ", "
                + "expiresAt=" + expiresAt + ", "
                + "remainingSeconds=" + remainingSeconds + ", "
                + "paidAt=" + paidAt + ", "
                + "deliveredAt=" + deliveredAt + ", "
                + "completedAt=" + completedAt + ", "
                + "cancelledAt=" + cancelledAt + ", "
                + "cancelReason=" + cancelReason + ", "
                + "cancelNote=" + cancelNote + ", "
                + "reviewStatus=" + reviewStatus + ", "
                + "reviewDeadline=" + reviewDeadline + ", "
                + "canReview=" + canReview + ", "
                + "version=" + version + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
