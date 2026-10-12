package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 发起交换提议（FR-404 / R-21~R-23）。
前置条件（不满足返回 409/422）：
- 交换包 1–5 件且属本人；目标商品 ≥1 件且**全部属于同一目标用户**（C-04）；
- 目标商品 `swapPreference=ACCEPTING`（否则 422 `SWAP_TARGET_NOT_ACCEPTING`）；
- 双方同交易区域（否则 422 `TRADE_ZONE_MISMATCH`）；
- **所有涉及商品当前均未被占用**（否则 409 `SWAP_ITEM_OCCUPIED` / `ITEM_ALREADY_RESERVED`）。

成功后**同时占用**交换包内全部商品与目标全部商品（→ `SWAPPING`），
并记录 `expiresAt = now + 72h`（A-02）。

 *
 * <p>对应契约 {@code components.schemas.CreateSwapProposalRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateSwapProposalRequest", description = "发起交换提议（FR-404 / R-21~R-23）。 前置条件（不满足返回 409/422）： - 交换包 1–5 件且属本人；目标商品 ≥1 件且**全部属于同一目标用户**（C-04）； - 目标商品 `swapPreference=ACCEPTING`（否则 422 `SWAP_TARGET_NOT_ACCEPTING`）； - 双方同交易区域（否则 422 `TRADE_ZONE_MISMATCH`）； - **所有涉及商品当前均未被占用**（否则 409 `SWAP_ITEM_OCCUPIED` / `ITEM_ALREADY_RESERVED`）。  成功后**同时占用**交换包内全部商品与目标全部商品（→ `SWAPPING`）， 并记录 `expiresAt = now + 72h`（A-02）。")
public class CreateSwapProposalRequest {

    /**
     * 我方交换包 ID
     */
    @NotNull
    @Schema(description = "我方交换包 ID")
    private Long proposerBundleId;

    /**
     * 目标商品 ID 列表（≥1 件，必须同属一个卖家）
     */
    @NotNull
    @Size(min = 1)
    @Schema(description = "目标商品 ID 列表（≥1 件，必须同属一个卖家）")
    private List<Long> targetItemIds;

    @NotNull
    @Schema(description = "tradeZoneId")
    private Long tradeZoneId;

    @Size(max = 500)
    @Schema(description = "message", example = "想用收纳盒换你的羽毛球拍，可以吗？")
    private String message;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateSwapProposalRequest() {
    }

    public Long getProposerBundleId() {
        return proposerBundleId;
    }

    public void setProposerBundleId(Long proposerBundleId) {
        this.proposerBundleId = proposerBundleId;
    }

    public List<Long> getTargetItemIds() {
        return targetItemIds;
    }

    public void setTargetItemIds(List<Long> targetItemIds) {
        this.targetItemIds = targetItemIds;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CreateSwapProposalRequest{"
                + "proposerBundleId=" + proposerBundleId + ", "
                + "targetItemIds=" + targetItemIds + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "message=" + message + ", "
                + '}';
    }
}
