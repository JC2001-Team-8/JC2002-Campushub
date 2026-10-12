package com.jc2001.campustrade.dto.request;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.SwapPreference;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 发布商品（FR-201 / R-10）。必填：标题、分类、成色、估价、交易区域、至少 1 张图片。
交易区域必须 ∈ 当前用户已勾选的常用交易区域（R-15），否则 422 `TRADE_ZONE_NOT_SELECTED`。

 *
 * <p>对应契约 {@code components.schemas.CreateItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateItemRequest", description = "发布商品（FR-201 / R-10）。必填：标题、分类、成色、估价、交易区域、至少 1 张图片。 交易区域必须 ∈ 当前用户已勾选的常用交易区域（R-15），否则 422 `TRADE_ZONE_NOT_SELECTED`。")
public class CreateItemRequest {

    @NotNull
    @Size(min = 1, max = 60)
    @Schema(description = "title", example = "高等数学教材（第七版）上册")
    private String title;

    @Size(max = 1000)
    @Schema(description = "description", example = "有少量笔记，无破损。")
    private String description;

    /**
     * 分类 ID（预设字典，FR-206）
     */
    @NotNull
    @Schema(description = "分类 ID（预设字典，FR-206）")
    private Long categoryId;

    /**
     * 成色 ID（预设字典，FR-207）
     */
    @NotNull
    @Schema(description = "成色 ID（预设字典，FR-207）")
    private Long conditionId;

    /**
     * 交易区域 ID，必须属于当前用户已勾选区域（R-15 / FR-209）
     */
    @NotNull
    @Schema(description = "交易区域 ID，必须属于当前用户已勾选区域（R-15 / FR-209）")
    private Long tradeZoneId;

    @NotNull
    @Schema(description = "estimatedPrice")
    private DecimalString estimatedPrice;

    /**
     * 图片 URL 列表（1–9 张），先调用 `POST /files/images` 上传；**第 1 张为封面**
     */
    @NotNull
    @Size(min = 1, max = 9)
    @Schema(description = "图片 URL 列表（1–9 张），先调用 `POST /files/images` 上传；**第 1 张为封面**")
    private List<String> imageUrls;

    @Schema(description = "swapPreference")
    private SwapPreference swapPreference;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateItemRequest() {
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public void setConditionId(Long conditionId) {
        this.conditionId = conditionId;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public DecimalString getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(DecimalString estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public SwapPreference getSwapPreference() {
        return swapPreference;
    }

    public void setSwapPreference(SwapPreference swapPreference) {
        this.swapPreference = swapPreference;
    }

    @Override
    public String toString() {
        return "CreateItemRequest{"
                + "title=" + title + ", "
                + "description=" + description + ", "
                + "categoryId=" + categoryId + ", "
                + "conditionId=" + conditionId + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "estimatedPrice=" + estimatedPrice + ", "
                + "imageUrls=" + imageUrls + ", "
                + "swapPreference=" + swapPreference + ", "
                + '}';
    }
}
