package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.ItemStatus;
import com.jc2001.campustrade.dto.response.SwapPreference;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品摘要（列表用，PF-01 商品卡片）
 *
 * <p>对应契约 {@code components.schemas.ItemSummary}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ItemSummary", description = "商品摘要（列表用，PF-01 商品卡片）")
public class ItemSummary {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "title", example = "【示例】高等数学教材（第七版）上册")
    private String title;

    /**
     * 封面图（`sortOrder=0` 的图片）
     */
    @Schema(description = "封面图（`sortOrder=0` 的图片）", example = "/static/uploads/demo/7-1.png")
    private String coverImageUrl;

    @NotNull
    @Schema(description = "estimatedPrice")
    private DecimalString estimatedPrice;

    /**
     * 币种，固定 CNY
     */
    @Schema(description = "币种，固定 CNY", example = "CNY")
    private String currency;

    @NotNull
    @Schema(description = "status")
    private ItemStatus status;

    @NotNull
    @Schema(description = "swapPreference")
    private SwapPreference swapPreference;

    @Schema(description = "categoryId")
    private Long categoryId;

    @Schema(description = "categoryName", example = "教材书籍")
    private String categoryName;

    @Schema(description = "conditionId")
    private Long conditionId;

    @Schema(description = "conditionName", example = "轻微使用痕迹")
    private String conditionName;

    @NotNull
    @Schema(description = "tradeZoneId")
    private Long tradeZoneId;

    /**
     * "**【待确认】占位区域名称**"
     */
    @Schema(description = "\"**【待确认】占位区域名称**\"", example = "【待确认】校区 A（占位名称）")
    private String tradeZoneName;

    @Schema(description = "sellerId")
    private Long sellerId;

    @Schema(description = "sellerNickname", example = "爱丽丝（示例卖家）")
    private String sellerNickname;

    @Schema(description = "publishedAt")
    private Instant publishedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ItemSummary() {
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public DecimalString getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(DecimalString estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public SwapPreference getSwapPreference() {
        return swapPreference;
    }

    public void setSwapPreference(SwapPreference swapPreference) {
        this.swapPreference = swapPreference;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public void setConditionId(Long conditionId) {
        this.conditionId = conditionId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public String getTradeZoneName() {
        return tradeZoneName;
    }

    public void setTradeZoneName(String tradeZoneName) {
        this.tradeZoneName = tradeZoneName;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return "ItemSummary{"
                + "id=" + id + ", "
                + "title=" + title + ", "
                + "coverImageUrl=" + coverImageUrl + ", "
                + "estimatedPrice=" + estimatedPrice + ", "
                + "currency=" + currency + ", "
                + "status=" + status + ", "
                + "swapPreference=" + swapPreference + ", "
                + "categoryId=" + categoryId + ", "
                + "categoryName=" + categoryName + ", "
                + "conditionId=" + conditionId + ", "
                + "conditionName=" + conditionName + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "tradeZoneName=" + tradeZoneName + ", "
                + "sellerId=" + sellerId + ", "
                + "sellerNickname=" + sellerNickname + ", "
                + "publishedAt=" + publishedAt + ", "
                + '}';
    }
}
