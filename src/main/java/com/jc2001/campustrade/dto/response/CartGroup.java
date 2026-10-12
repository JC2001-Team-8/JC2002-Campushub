package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.CartItem;
import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 按卖家分组的购物车（FR-302）。分组即「提交订单时按卖家拆单」的依据（R-05 / C-02）
 *
 * <p>对应契约 {@code components.schemas.CartGroup}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "CartGroup", description = "按卖家分组的购物车（FR-302）。分组即「提交订单时按卖家拆单」的依据（R-05 / C-02）")
public class CartGroup {

    @NotNull
    @Schema(description = "seller")
    private UserProfile seller;

    @NotNull
    @Schema(description = "items")
    private List<CartItem> items;

    @NotNull
    @Schema(description = "groupTotal")
    private DecimalString groupTotal;

    /**
     * 该分组内可结算条目数
     */
    @Schema(description = "该分组内可结算条目数")
    private Integer purchasableCount;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CartGroup() {
    }

    public UserProfile getSeller() {
        return seller;
    }

    public void setSeller(UserProfile seller) {
        this.seller = seller;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public DecimalString getGroupTotal() {
        return groupTotal;
    }

    public void setGroupTotal(DecimalString groupTotal) {
        this.groupTotal = groupTotal;
    }

    public Integer getPurchasableCount() {
        return purchasableCount;
    }

    public void setPurchasableCount(Integer purchasableCount) {
        this.purchasableCount = purchasableCount;
    }

    @Override
    public String toString() {
        return "CartGroup{"
                + "seller=" + seller + ", "
                + "items=" + items + ", "
                + "groupTotal=" + groupTotal + ", "
                + "purchasableCount=" + purchasableCount + ", "
                + '}';
    }
}
