package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 下架违规商品（FR-502 / R-35）。**必须填原因**。
若商品存在进行中占用，服务端**联动处理**（R-35）：
被订单占用 → 该订单取消（`cancel_reason=ADMIN_TAKEDOWN`）；
被交换占用 → **整笔交换作废**（`termination_reason=VOID_ITEM_BANNED`），释放全部占用。

 *
 * <p>对应契约 {@code components.schemas.TakedownItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "TakedownItemRequest", description = "下架违规商品（FR-502 / R-35）。**必须填原因**。 若商品存在进行中占用，服务端**联动处理**（R-35）： 被订单占用 → 该订单取消（`cancel_reason=ADMIN_TAKEDOWN`）； 被交换占用 → **整笔交换作废**（`termination_reason=VOID_ITEM_BANNED`），释放全部占用。")
public class TakedownItemRequest {

    @NotNull
    @Size(min = 1, max = 255)
    @Schema(description = "reason", example = "商品信息违反平台规定")
    private String reason;

    /**
     * 商品 ID（与路径参数一致时以路径参数为准）
     */
    @Schema(description = "商品 ID（与路径参数一致时以路径参数为准）")
    private Long itemId;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public TakedownItemRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "TakedownItemRequest{"
                + "reason=" + reason + ", "
                + "itemId=" + itemId + ", "
                + '}';
    }
}
