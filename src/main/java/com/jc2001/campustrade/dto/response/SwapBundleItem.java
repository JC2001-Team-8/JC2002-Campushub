package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.BundleItemStatus;
import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.ItemSummary;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换包内的物品（FR-402 / R-21）
 *
 * <p>对应契约 {@code components.schemas.SwapBundleItem}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapBundleItem", description = "交换包内的物品（FR-402 / R-21）")
public class SwapBundleItem {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    @Schema(description = "item")
    private ItemSummary item;

    @NotNull
    @Schema(description = "itemStatus")
    private BundleItemStatus itemStatus;

    @NotNull
    @Schema(description = "priceSnapshot")
    private DecimalString priceSnapshot;

    @Schema(description = "releasedAt")
    private Instant releasedAt;

    @Schema(description = "releaseReason")
    private String releaseReason;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapBundleItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public ItemSummary getItem() {
        return item;
    }

    public void setItem(ItemSummary item) {
        this.item = item;
    }

    public BundleItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(BundleItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public DecimalString getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(DecimalString priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
    }

    @Override
    public String toString() {
        return "SwapBundleItem{"
                + "id=" + id + ", "
                + "itemId=" + itemId + ", "
                + "item=" + item + ", "
                + "itemStatus=" + itemStatus + ", "
                + "priceSnapshot=" + priceSnapshot + ", "
                + "releasedAt=" + releasedAt + ", "
                + "releaseReason=" + releaseReason + ", "
                + '}';
    }
}
