package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 订单明细（FR-303）。快照字段保证商品后续编辑不影响历史（R-13）
 *
 * <p>对应契约 {@code components.schemas.OrderItem}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "OrderItem", description = "订单明细（FR-303）。快照字段保证商品后续编辑不影响历史（R-13）")
public class OrderItem {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    /**
     * 下单时的标题快照
     */
    @NotNull
    @Schema(description = "下单时的标题快照", example = "【示例】高等数学教材（第七版）上册")
    private String titleSnapshot;

    @NotNull
    @Schema(description = "priceSnapshot")
    private DecimalString priceSnapshot;

    /**
     * 下单时的成色名称快照
     */
    @Schema(description = "下单时的成色名称快照", example = "轻微使用痕迹")
    private String conditionSnapshot;

    @Schema(description = "coverImageUrl", example = "/static/uploads/demo/7-1.png")
    private String coverImageUrl;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public OrderItem() {
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

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    public DecimalString getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(DecimalString priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public String getConditionSnapshot() {
        return conditionSnapshot;
    }

    public void setConditionSnapshot(String conditionSnapshot) {
        this.conditionSnapshot = conditionSnapshot;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    @Override
    public String toString() {
        return "OrderItem{"
                + "id=" + id + ", "
                + "itemId=" + itemId + ", "
                + "titleSnapshot=" + titleSnapshot + ", "
                + "priceSnapshot=" + priceSnapshot + ", "
                + "conditionSnapshot=" + conditionSnapshot + ", "
                + "coverImageUrl=" + coverImageUrl + ", "
                + '}';
    }
}
