package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.ItemStatus;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 下架结果与联动影响（R-35 需要向管理员明示影响范围）
 *
 * <p>对应契约 {@code components.schemas.TakedownItemResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "TakedownItemResponse", description = "下架结果与联动影响（R-35 需要向管理员明示影响范围）")
public class TakedownItemResponse {

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    @NotNull
    @Schema(description = "status")
    private ItemStatus status;

    /**
     * 被联动取消的订单数
     */
    @NotNull
    @Schema(description = "被联动取消的订单数")
    private Integer affectedOrders;

    /**
     * 被联动**整笔作废**的交换提议数（R-24）
     */
    @NotNull
    @Schema(description = "被联动**整笔作废**的交换提议数（R-24）")
    private Integer voidedSwapProposals;

    /**
     * 已收到通知的受影响用户
     */
    @Schema(description = "已收到通知的受影响用户")
    private List<Long> notifiedUserIds;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public TakedownItemResponse() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public Integer getAffectedOrders() {
        return affectedOrders;
    }

    public void setAffectedOrders(Integer affectedOrders) {
        this.affectedOrders = affectedOrders;
    }

    public Integer getVoidedSwapProposals() {
        return voidedSwapProposals;
    }

    public void setVoidedSwapProposals(Integer voidedSwapProposals) {
        this.voidedSwapProposals = voidedSwapProposals;
    }

    public List<Long> getNotifiedUserIds() {
        return notifiedUserIds;
    }

    public void setNotifiedUserIds(List<Long> notifiedUserIds) {
        this.notifiedUserIds = notifiedUserIds;
    }

    @Override
    public String toString() {
        return "TakedownItemResponse{"
                + "itemId=" + itemId + ", "
                + "status=" + status + ", "
                + "affectedOrders=" + affectedOrders + ", "
                + "voidedSwapProposals=" + voidedSwapProposals + ", "
                + "notifiedUserIds=" + notifiedUserIds + ", "
                + '}';
    }
}
