package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 创建交换包（FR-402 / R-21）。
**1–5 件**，且**所有物品必须属于本人**，否则 422 `SWAP_BUNDLE_ITEM_NOT_OWNED`。
同一物品同时只能存在于一个**进行中**交换包（SB-03），冲突返回 409 `SWAP_ITEM_IN_ACTIVE_BUNDLE`。

 *
 * <p>对应契约 {@code components.schemas.CreateSwapBundleRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateSwapBundleRequest", description = "创建交换包（FR-402 / R-21）。 **1–5 件**，且**所有物品必须属于本人**，否则 422 `SWAP_BUNDLE_ITEM_NOT_OWNED`。 同一物品同时只能存在于一个**进行中**交换包（SB-03），冲突返回 409 `SWAP_ITEM_IN_ACTIVE_BUNDLE`。")
public class CreateSwapBundleRequest {

    /**
     * 物品 ID 列表，1–5 件（E-07：越界返回 422）
     */
    @NotNull
    @Size(min = 1, max = 5)
    @Schema(description = "物品 ID 列表，1–5 件（E-07：越界返回 422）")
    private List<Long> itemIds;

    @Size(max = 60)
    @Schema(description = "title", example = "我的交换包")
    private String title;

    /**
     * 交换包交易区域，须与后续目标商品同区域（R-15）
     */
    @Schema(description = "交换包交易区域，须与后续目标商品同区域（R-15）")
    private Long tradeZoneId;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateSwapBundleRequest() {
    }

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    @Override
    public String toString() {
        return "CreateSwapBundleRequest{"
                + "itemIds=" + itemIds + ", "
                + "title=" + title + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + '}';
    }
}
