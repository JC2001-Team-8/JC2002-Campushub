package com.jc2001.campustrade.dto.request;

import com.jc2001.campustrade.dto.response.DecimalString;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 编辑商品（FR-202 / R-11）。
**占用态（RESERVED / SWAPPING）禁止修改**：标题、估价、分类、成色、交易区域；
允许修改：描述、图片（仍须 ≥1 张）。`SOLD` / `BANNED` 终态禁止任何编辑。

 *
 * <p>对应契约 {@code components.schemas.UpdateItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UpdateItemRequest", description = "编辑商品（FR-202 / R-11）。 **占用态（RESERVED / SWAPPING）禁止修改**：标题、估价、分类、成色、交易区域； 允许修改：描述、图片（仍须 ≥1 张）。`SOLD` / `BANNED` 终态禁止任何编辑。")
public class UpdateItemRequest {

    @Size(min = 1, max = 60)
    @Schema(description = "title", example = "高等数学教材（第七版）上册（已降价）")
    private String title;

    @Size(max = 1000)
    @Schema(description = "description", example = "有少量笔记。")
    private String description;

    @Schema(description = "categoryId")
    private Long categoryId;

    @Schema(description = "conditionId")
    private Long conditionId;

    @Schema(description = "estimatedPrice")
    private DecimalString estimatedPrice;

    /**
     * 仅当商品未被占用时可修改（R-11）
     */
    @Schema(description = "仅当商品未被占用时可修改（R-11）")
    private Long tradeZoneId;

    /**
     * 图片全量替换，1–9 张；顺序即 `sortOrder`，第 1 张为封面
     */
    @Size(min = 1, max = 9)
    @Schema(description = "图片全量替换，1–9 张；顺序即 `sortOrder`，第 1 张为封面")
    private List<String> imageUrls;

    /**
     * "**必填**乐观锁版本号（R-18）。版本不匹配返回 409 `COMMON_VERSION_CONFLICT`"
     */
    @Schema(description = "\"**必填**乐观锁版本号（R-18）。版本不匹配返回 409 `COMMON_VERSION_CONFLICT`\"")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UpdateItemRequest() {
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

    public DecimalString getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(DecimalString estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "UpdateItemRequest{"
                + "title=" + title + ", "
                + "description=" + description + ", "
                + "categoryId=" + categoryId + ", "
                + "conditionId=" + conditionId + ", "
                + "estimatedPrice=" + estimatedPrice + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "imageUrls=" + imageUrls + ", "
                + "version=" + version + ", "
                + '}';
    }
}
