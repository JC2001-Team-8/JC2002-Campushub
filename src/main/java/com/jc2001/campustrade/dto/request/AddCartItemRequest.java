package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 加入购物车（FR-301）。**不可加入自己发布的商品**（R-04），否则 422 `CART_OWN_ITEM_NOT_ALLOWED`
 *
 * <p>对应契约 {@code components.schemas.AddCartItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "AddCartItemRequest", description = "加入购物车（FR-301）。**不可加入自己发布的商品**（R-04），否则 422 `CART_OWN_ITEM_NOT_ALLOWED`")
public class AddCartItemRequest {

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public AddCartItemRequest() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "AddCartItemRequest{"
                + "itemId=" + itemId + ", "
                + '}';
    }
}
