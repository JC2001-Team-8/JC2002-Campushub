package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.ItemSummary;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 购物车条目（FR-301 / R-04）。**数量固定为 1**（假设 A-16 / Q-23），单件闲置交易。

 *
 * <p>对应契约 {@code components.schemas.CartItem}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "CartItem", description = "购物车条目（FR-301 / R-04）。**数量固定为 1**（假设 A-16 / Q-23），单件闲置交易。")
public class CartItem {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    /**
     * 固定为 1（A-16）
     */
    @NotNull
    @Schema(description = "固定为 1（A-16）")
    private Integer quantity;

    @Schema(description = "item")
    private ItemSummary item;

    /**
     * 是否可结算。当商品被占用/售出/下架时为 `false`（R-04），
     * 前端须灰显并展示 `unavailableReason`。
     * 
     */
    @NotNull
    @Schema(description = "是否可结算。当商品被占用/售出/下架时为 `false`（R-04）， 前端须灰显并展示 `unavailableReason`。")
    private Boolean purchasable;

    /**
     * 不可结算原因，`purchasable=false` 时返回
     */
    @Schema(description = "不可结算原因，`purchasable=false` 时返回")
    private String unavailableReason;

    @Schema(description = "addedAt")
    private Instant addedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CartItem() {
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public ItemSummary getItem() {
        return item;
    }

    public void setItem(ItemSummary item) {
        this.item = item;
    }

    public Boolean getPurchasable() {
        return purchasable;
    }

    public void setPurchasable(Boolean purchasable) {
        this.purchasable = purchasable;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return "CartItem{"
                + "id=" + id + ", "
                + "itemId=" + itemId + ", "
                + "quantity=" + quantity + ", "
                + "item=" + item + ", "
                + "purchasable=" + purchasable + ", "
                + "unavailableReason=" + unavailableReason + ", "
                + "addedAt=" + addedAt + ", "
                + '}';
    }
}
