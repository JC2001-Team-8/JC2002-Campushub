package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 拒绝交换提议（FR-406）。仅**目标方**可调用，状态须为 `PENDING`，否则 409
 *
 * <p>对应契约 {@code components.schemas.RejectSwapProposalRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "RejectSwapProposalRequest", description = "拒绝交换提议（FR-406）。仅**目标方**可调用，状态须为 `PENDING`，否则 409")
public class RejectSwapProposalRequest {

    @Size(max = 200)
    @Schema(description = "reason", example = "暂时不想换")
    private String reason;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public RejectSwapProposalRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RejectSwapProposalRequest{"
                + "reason=" + reason + ", "
                + '}';
    }
}
