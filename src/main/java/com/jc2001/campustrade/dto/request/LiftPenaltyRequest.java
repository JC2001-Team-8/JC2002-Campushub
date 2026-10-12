package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 手动解封（permission-matrix 6.8）。**必须填原因**；与到期自动解封并发时只允许一方成功（R-31 条件更新）
 *
 * <p>对应契约 {@code components.schemas.LiftPenaltyRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "LiftPenaltyRequest", description = "手动解封（permission-matrix 6.8）。**必须填原因**；与到期自动解封并发时只允许一方成功（R-31 条件更新）")
public class LiftPenaltyRequest {

    @NotNull
    @Size(min = 1, max = 255)
    @Schema(description = "reason", example = "经人工复核，提前解封")
    private String reason;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public LiftPenaltyRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "LiftPenaltyRequest{"
                + "reason=" + reason + ", "
                + '}';
    }
}
