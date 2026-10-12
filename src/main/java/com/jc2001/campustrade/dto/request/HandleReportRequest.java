package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 处理举报（permission-matrix 6.9）。管理员受理或驳回，**必须填处理结论**（R-34 留痕）
 *
 * <p>对应契约 {@code components.schemas.HandleReportRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "HandleReportRequest", description = "处理举报（permission-matrix 6.9）。管理员受理或驳回，**必须填处理结论**（R-34 留痕）")
public class HandleReportRequest {

    /**
     * `HANDLED` 受理；`DISMISSED` 驳回
     */
    @NotNull
    @Schema(description = "`HANDLED` 受理；`DISMISSED` 驳回", example = "HANDLED")
    private String status;

    /**
     * "**必填**处理结论"
     */
    @NotNull
    @Size(min = 1, max = 255)
    @Schema(description = "\"**必填**处理结论\"", example = "已核实违规，商品已下架")
    private String note;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public HandleReportRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "HandleReportRequest{"
                + "status=" + status + ", "
                + "note=" + note + ", "
                + '}';
    }
}
