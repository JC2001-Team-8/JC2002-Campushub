package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 处理申诉（FR-505 / AP-02 / AP-03）。**必须填处理意见**（R-34）。
`APPROVED` → 立即解封（处罚 `LIFTED`，`lift_source=APPEAL_APPROVED`）；
`REJECTED` → 处罚恢复 `ACTIVE` 并继续计到期（R-32）。
重复处理返回首次结论（IDEM-A）。

 *
 * <p>对应契约 {@code components.schemas.ResolveAppealRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "ResolveAppealRequest", description = "处理申诉（FR-505 / AP-02 / AP-03）。**必须填处理意见**（R-34）。 `APPROVED` → 立即解封（处罚 `LIFTED`，`lift_source=APPEAL_APPROVED`）； `REJECTED` → 处罚恢复 `ACTIVE` 并继续计到期（R-32）。 重复处理返回首次结论（IDEM-A）。")
public class ResolveAppealRequest {

    /**
     * `APPROVED` 申诉通过（解封）；`REJECTED` 申诉驳回
     */
    @NotNull
    @Schema(description = "`APPROVED` 申诉通过（解封）；`REJECTED` 申诉驳回", example = "APPROVED")
    private String decision;

    /**
     * "**必填**处理意见"
     */
    @NotNull
    @Size(min = 1, max = 1000)
    @Schema(description = "\"**必填**处理意见\"", example = "经复核，处罚依据不足，予以解封")
    private String note;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ResolveAppealRequest() {
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "ResolveAppealRequest{"
                + "decision=" + decision + ", "
                + "note=" + note + ", "
                + '}';
    }
}
