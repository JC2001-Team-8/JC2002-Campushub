package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 双方确认（FR-410 / R-27）。**双方各确认一次**，两次齐备才判定交换完成
（全部商品 → `SOLD`，提议 → `COMPLETED`）。
前置：交换码已核销（否则 409 `SWAP_CODE_NOT_VERIFIED`）。
同一方重复确认**幂等成功**（IDEM-A），不重复计数。

 *
 * <p>对应契约 {@code components.schemas.ConfirmSwapRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "ConfirmSwapRequest", description = "双方确认（FR-410 / R-27）。**双方各确认一次**，两次齐备才判定交换完成 （全部商品 → `SOLD`，提议 → `COMPLETED`）。 前置：交换码已核销（否则 409 `SWAP_CODE_NOT_VERIFIED`）。 同一方重复确认**幂等成功**（IDEM-A），不重复计数。")
public class ConfirmSwapRequest {

    /**
     * 固定为 true（语义为「我已交付/已收到」）
     */
    @NotNull
    @Schema(description = "固定为 true（语义为「我已交付/已收到」）")
    private Boolean confirm;

    @Size(max = 200)
    @Schema(description = "remark")
    private String remark;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ConfirmSwapRequest() {
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ConfirmSwapRequest{"
                + "confirm=" + confirm + ", "
                + "remark=" + remark + ", "
                + '}';
    }
}
