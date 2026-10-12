package com.jc2001.campustrade.dto.request;

import com.jc2001.campustrade.dto.response.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 举报商品（FR-211）。任何登录用户可举报；**被处罚用户也可举报**（permission-matrix §8）。
同一用户对同一商品最多 1 条**未处理**举报（C-06）；重复举报幂等（E-14）。

 *
 * <p>对应契约 {@code components.schemas.CreateReportRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateReportRequest", description = "举报商品（FR-211）。任何登录用户可举报；**被处罚用户也可举报**（permission-matrix §8）。 同一用户对同一商品最多 1 条**未处理**举报（C-06）；重复举报幂等（E-14）。")
public class CreateReportRequest {

    @NotNull
    @Schema(description = "reason")
    private ReportReason reason;

    @Size(max = 500)
    @Schema(description = "detail", example = "该商品疑似违规")
    private String detail;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateReportRequest() {
    }

    public ReportReason getReason() {
        return reason;
    }

    public void setReason(ReportReason reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "CreateReportRequest{"
                + "reason=" + reason + ", "
                + "detail=" + detail + ", "
                + '}';
    }
}
