package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.Category;
import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.ItemCondition;
import com.jc2001.campustrade.dto.response.ItemImage;
import com.jc2001.campustrade.dto.response.ItemStatus;
import com.jc2001.campustrade.dto.response.SwapPreference;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品详情（PF-02）
 *
 * <p>对应契约 {@code components.schemas.ItemDetail}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ItemDetail", description = "商品详情（PF-02）")
public class ItemDetail {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "title", example = "【示例】高等数学教材（第七版）上册")
    private String title;

    @NotNull
    @Schema(description = "description", example = "有少量笔记，无破损，适合下学期使用。")
    private String description;

    @NotNull
    @Schema(description = "estimatedPrice")
    private DecimalString estimatedPrice;

    @Schema(description = "currency", example = "CNY")
    private String currency;

    /**
     * 固定免责声明，强调估价非平台定价（R-13）
     */
    @Schema(description = "固定免责声明，强调估价非平台定价（R-13）", example = "用户自填估价，仅供参考，非平台定价")
    private String priceDisclaimer;

    @NotNull
    @Schema(description = "status")
    private ItemStatus status;

    @NotNull
    @Schema(description = "swapPreference")
    private SwapPreference swapPreference;

    /**
     * 详情页展示用的交换状态文案（FR-210）：
     * `可交换` / `暂不交换` / `交换中` / `已交换`。
     * 由 `swapPreference` + `status` 推导，非数据库独立字段。
     * 
     */
    @Schema(description = "详情页展示用的交换状态文案（FR-210）： `可交换` / `暂不交换` / `交换中` / `已交换`。 由 `swapPreference` + `status` 推导，非数据库独立字段。", example = "可交换")
    private String swapDisplayStatus;

    @Schema(description = "category")
    private Category category;

    @Schema(description = "condition")
    private ItemCondition condition;

    @Schema(description = "tradeZone")
    private TradeZone tradeZone;

    /**
     * 图片列表，1–9 张，`sortOrder=0` 为封面
     */
    @NotNull
    @Schema(description = "图片列表，1–9 张，`sortOrder=0` 为封面")
    private List<ItemImage> images;

    @NotNull
    @Schema(description = "seller")
    private UserProfile seller;

    /**
     * 违规下架原因。仅当 `status=BANNED` 且有权限查看时返回（R-34 留痕）
     */
    @Schema(description = "违规下架原因。仅当 `status=BANNED` 且有权限查看时返回（R-34 留痕）")
    private String bannedReason;

    /**
     * 乐观锁版本号，编辑时需要回传（R-18）
     */
    @Schema(description = "乐观锁版本号，编辑时需要回传（R-18）")
    private Integer version;

    @Schema(description = "publishedAt")
    private Instant publishedAt;

    @Schema(description = "updatedAt")
    private Instant updatedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ItemDetail() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getPriceDisclaimer() {
        return priceDisclaimer;
    }

    public void setPriceDisclaimer(String priceDisclaimer) {
        this.priceDisclaimer = priceDisclaimer;
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

    public String getSwapDisplayStatus() {
        return swapDisplayStatus;
    }

    public void setSwapDisplayStatus(String swapDisplayStatus) {
        this.swapDisplayStatus = swapDisplayStatus;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public void setCondition(ItemCondition condition) {
        this.condition = condition;
    }

    public TradeZone getTradeZone() {
        return tradeZone;
    }

    public void setTradeZone(TradeZone tradeZone) {
        this.tradeZone = tradeZone;
    }

    public List<ItemImage> getImages() {
        return images;
    }

    public void setImages(List<ItemImage> images) {
        this.images = images;
    }

    public UserProfile getSeller() {
        return seller;
    }

    public void setSeller(UserProfile seller) {
        this.seller = seller;
    }

    public String getBannedReason() {
        return bannedReason;
    }

    public void setBannedReason(String bannedReason) {
        this.bannedReason = bannedReason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ItemDetail{"
                + "id=" + id + ", "
                + "title=" + title + ", "
                + "description=" + description + ", "
                + "estimatedPrice=" + estimatedPrice + ", "
                + "currency=" + currency + ", "
                + "priceDisclaimer=" + priceDisclaimer + ", "
                + "status=" + status + ", "
                + "swapPreference=" + swapPreference + ", "
                + "swapDisplayStatus=" + swapDisplayStatus + ", "
                + "category=" + category + ", "
                + "condition=" + condition + ", "
                + "tradeZone=" + tradeZone + ", "
                + "images=" + images + ", "
                + "seller=" + seller + ", "
                + "bannedReason=" + bannedReason + ", "
                + "version=" + version + ", "
                + "publishedAt=" + publishedAt + ", "
                + "updatedAt=" + updatedAt + ", "
                + '}';
    }
}
