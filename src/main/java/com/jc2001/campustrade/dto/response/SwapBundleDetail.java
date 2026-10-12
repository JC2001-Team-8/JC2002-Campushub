package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.SwapBundleItem;
import com.jc2001.campustrade.dto.response.SwapBundleStatus;
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
 * 交换包详情（PF-15）
 *
 * <p>对应契约 {@code components.schemas.SwapBundleDetail}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapBundleDetail", description = "交换包详情（PF-15）")
public class SwapBundleDetail {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @Schema(description = "title")
    private String title;

    @NotNull
    @Schema(description = "owner")
    private UserProfile owner;

    @NotNull
    @Schema(description = "status")
    private SwapBundleStatus status;

    @NotNull
    @Schema(description = "estimatedTotal")
    private DecimalString estimatedTotal;

    /**
     * 强调不做差价（OOS-03）
     */
    @Schema(description = "强调不做差价（OOS-03）", example = "估价合计仅供参考，平台不做等价判定、不支持现金补差价")
    private String priceDisclaimer;

    @Schema(description = "tradeZone")
    private TradeZone tradeZone;

    /**
     * 包内物品，**1–5 件**（SB-01）
     */
    @NotNull
    @Size(max = 5)
    @Schema(description = "包内物品，**1–5 件**（SB-01）")
    private List<SwapBundleItem> items;

    /**
     * 当前是否可编辑。被进行中提议引用（`IN_SWAP`）时不可编辑（SB-04）
     */
    @Schema(description = "当前是否可编辑。被进行中提议引用（`IN_SWAP`）时不可编辑（SB-04）")
    private Boolean editable;

    @Schema(description = "version")
    private Integer version;

    @Schema(description = "createdAt")
    private Instant createdAt;

    @Schema(description = "updatedAt")
    private Instant updatedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapBundleDetail() {
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

    public UserProfile getOwner() {
        return owner;
    }

    public void setOwner(UserProfile owner) {
        this.owner = owner;
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

    public String getPriceDisclaimer() {
        return priceDisclaimer;
    }

    public void setPriceDisclaimer(String priceDisclaimer) {
        this.priceDisclaimer = priceDisclaimer;
    }

    public TradeZone getTradeZone() {
        return tradeZone;
    }

    public void setTradeZone(TradeZone tradeZone) {
        this.tradeZone = tradeZone;
    }

    public List<SwapBundleItem> getItems() {
        return items;
    }

    public void setItems(List<SwapBundleItem> items) {
        this.items = items;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SwapBundleDetail{"
                + "id=" + id + ", "
                + "title=" + title + ", "
                + "owner=" + owner + ", "
                + "status=" + status + ", "
                + "estimatedTotal=" + estimatedTotal + ", "
                + "priceDisclaimer=" + priceDisclaimer + ", "
                + "tradeZone=" + tradeZone + ", "
                + "items=" + items + ", "
                + "editable=" + editable + ", "
                + "version=" + version + ", "
                + "createdAt=" + createdAt + ", "
                + "updatedAt=" + updatedAt + ", "
                + '}';
    }
}
