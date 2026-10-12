package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.CartGroup;
import com.jc2001.campustrade.dto.response.DecimalString;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 购物车视图（PF-09）
 *
 * <p>对应契约 {@code components.schemas.CartView}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "CartView", description = "购物车视图（PF-09）")
public class CartView {

    @NotNull
    @Schema(description = "groups")
    private List<CartGroup> groups;

    @NotNull
    @Schema(description = "totalItems")
    private Integer totalItems;

    @NotNull
    @Schema(description = "totalAmount")
    private DecimalString totalAmount;

    @Schema(description = "priceDisclaimer", example = "合计为卖家自填估价之和，仅供参考")
    private String priceDisclaimer;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CartView() {
    }

    public List<CartGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<CartGroup> groups) {
        this.groups = groups;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public DecimalString getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(DecimalString totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPriceDisclaimer() {
        return priceDisclaimer;
    }

    public void setPriceDisclaimer(String priceDisclaimer) {
        this.priceDisclaimer = priceDisclaimer;
    }

    @Override
    public String toString() {
        return "CartView{"
                + "groups=" + groups + ", "
                + "totalItems=" + totalItems + ", "
                + "totalAmount=" + totalAmount + ", "
                + "priceDisclaimer=" + priceDisclaimer + ", "
                + '}';
    }
}
