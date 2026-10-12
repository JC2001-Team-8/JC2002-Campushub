package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.SwapBundleStatus;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换包摘要（列表用，PF-15）
 *
 * <p>对应契约 {@code components.schemas.SwapBundleSummary}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapBundleSummary", description = "交换包摘要（列表用，PF-15）")
public class SwapBundleSummary {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @Schema(description = "title", example = "我的交换包")
    private String title;

    @NotNull
    @Schema(description = "status")
    private SwapBundleStatus status;

    @NotNull
    @Schema(description = "estimatedTotal")
    private DecimalString estimatedTotal;

    /**
     * 包内**进行中**（`itemStatus=ACTIVE`）物品数
     */
    @NotNull
    @Schema(description = "包内**进行中**（`itemStatus=ACTIVE`）物品数")
    private Integer itemCount;

    @Schema(description = "tradeZoneId")
    private Long tradeZoneId;

    @Schema(description = "packedAt")
    private Instant packedAt;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapBundleSummary() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SwapBundleStatus getStatus() {
        return status;
    }

    public void setStatus(SwapBundleStatus status) {
        this.status = status;
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

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public Instant getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(Instant packedAt) {
        this.packedAt = packedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SwapBundleSummary{"
                + "id=" + id + ", "
                + "title=" + title + ", "
                + "status=" + status + ", "
                + "estimatedTotal=" + estimatedTotal + ", "
                + "itemCount=" + itemCount + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "packedAt=" + packedAt + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
