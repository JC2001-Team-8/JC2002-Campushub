package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 提交订单（FR-303 / FR-304 / R-16）。
**同一订单只包含一个卖家的商品**：若购物车选中项跨卖家，服务端**自动拆成多笔订单**
（每卖家一笔，各自独立倒计时与付款，C-02 / R-05）。
商品**在下单时即被占用**（不是付款时），并开始 30 分钟付款倒计时（假设 A-01）。

 *
 * <p>对应契约 {@code components.schemas.SubmitOrderRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "SubmitOrderRequest", description = "提交订单（FR-303 / FR-304 / R-16）。 **同一订单只包含一个卖家的商品**：若购物车选中项跨卖家，服务端**自动拆成多笔订单** （每卖家一笔，各自独立倒计时与付款，C-02 / R-05）。 商品**在下单时即被占用**（不是付款时），并开始 30 分钟付款倒计时（假设 A-01）。")
public class SubmitOrderRequest {

    /**
     * 待下单的购物车条目 ID（可跨卖家，服务端自动拆单）
     */
    @NotNull
    @Size(min = 1)
    @Schema(description = "待下单的购物车条目 ID（可跨卖家，服务端自动拆单）")
    private List<Long> cartItemIds;

    /**
     * 交易区域。必须同时满足：属于买家已勾选区域、且与**每个**商品所属区域一致（R-15），
     * 否则 422 `TRADE_ZONE_MISMATCH`。
     * 
     */
    @NotNull
    @Schema(description = "交易区域。必须同时满足：属于买家已勾选区域、且与**每个**商品所属区域一致（R-15）， 否则 422 `TRADE_ZONE_MISMATCH`。")
    private Long tradeZoneId;

    /**
     * 买家留言（可选，非聊天功能 OOS-08）
     */
    @Size(max = 255)
    @Schema(description = "买家留言（可选，非聊天功能 OOS-08）", example = "希望周末交付")
    private String note;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SubmitOrderRequest() {
    }

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "SubmitOrderRequest{"
                + "cartItemIds=" + cartItemIds + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "note=" + note + ", "
                + '}';
    }
}
