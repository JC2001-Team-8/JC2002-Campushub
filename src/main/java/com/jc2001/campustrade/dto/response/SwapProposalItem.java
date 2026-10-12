package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.ItemSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换提议的目标商品（C-04）。**整包接受即接受此处全部行，无部分接受**（OOS-04）
 *
 * <p>对应契约 {@code components.schemas.SwapProposalItem}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapProposalItem", description = "交换提议的目标商品（C-04）。**整包接受即接受此处全部行，无部分接受**（OOS-04）")
public class SwapProposalItem {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    @NotNull
    @Schema(description = "titleSnapshot", example = "【示例】羽毛球拍（含拍套）")
    private String titleSnapshot;

    @NotNull
    @Schema(description = "priceSnapshot")
    private DecimalString priceSnapshot;

    @Schema(description = "item")
    private ItemSummary item;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapProposalItem() {
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

    public ItemSummary getItem() {
        return item;
    }

    public void setItem(ItemSummary item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "SwapProposalItem{"
                + "id=" + id + ", "
                + "itemId=" + itemId + ", "
                + "titleSnapshot=" + titleSnapshot + ", "
                + "priceSnapshot=" + priceSnapshot + ", "
                + "item=" + item + ", "
                + '}';
    }
}
