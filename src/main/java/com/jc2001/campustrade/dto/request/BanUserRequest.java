package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 封禁用户（FR-503 / R-30）。
**默认且固定 3 天（72 小时）**，不提供时长参数（假设 A-03 / Q-09：固定不可调）。
**必须填原因**（R-34）。目标用户当前已有生效中处罚时返回 409
`GOVERNANCE_ACTIVE_PENALTY_EXISTS`（假设 Q-10：拒绝重复封禁）。
目标为管理员时返回 403 `GOVERNANCE_CANNOT_PUNISH_ADMIN`。

 *
 * <p>对应契约 {@code components.schemas.BanUserRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "BanUserRequest", description = "封禁用户（FR-503 / R-30）。 **默认且固定 3 天（72 小时）**，不提供时长参数（假设 A-03 / Q-09：固定不可调）。 **必须填原因**（R-34）。目标用户当前已有生效中处罚时返回 409 `GOVERNANCE_ACTIVE_PENALTY_EXISTS`（假设 Q-10：拒绝重复封禁）。 目标为管理员时返回 403 `GOVERNANCE_CANNOT_PUNISH_ADMIN`。")
public class BanUserRequest {

    /**
     * "**必填**封禁原因"
     */
    @NotNull
    @Size(min = 1, max = 255)
    @Schema(description = "\"**必填**封禁原因\"", example = "多次发布违规商品")
    private String reason;

    /**
     * 固定 72。**只读字段**，携带将被忽略（R-30 / A-03）
     */
    @Schema(description = "固定 72。**只读字段**，携带将被忽略（R-30 / A-03）")
    private Integer durationHours;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public BanUserRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    @Override
    public String toString() {
        return "BanUserRequest{"
                + "reason=" + reason + ", "
                + "durationHours=" + durationHours + ", "
                + '}';
    }
}
