package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 提交申诉（permission-matrix 2.12 / AP-01）。
仅可对**归属本人的生效中处罚**（`ACTIVE`）提交；提交后处罚标记为 `APPEALING`，
**处罚仍然生效**（不停表，P-03）。同一处罚重复提交返回 409 `APPEAL_ALREADY_EXISTS`。
已解封/已过期处罚不可申诉（假设 Q-17 不允许），否则 409 `APPEAL_PENALTY_NOT_ACTIVE`。

 *
 * <p>对应契约 {@code components.schemas.CreateAppealRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateAppealRequest", description = "提交申诉（permission-matrix 2.12 / AP-01）。 仅可对**归属本人的生效中处罚**（`ACTIVE`）提交；提交后处罚标记为 `APPEALING`， **处罚仍然生效**（不停表，P-03）。同一处罚重复提交返回 409 `APPEAL_ALREADY_EXISTS`。 已解封/已过期处罚不可申诉（假设 Q-17 不允许），否则 409 `APPEAL_PENALTY_NOT_ACTIVE`。")
public class CreateAppealRequest {

    @NotNull
    @Size(min = 1, max = 1000)
    @Schema(description = "reason", example = "该商品已主动下架，请复核处罚")
    private String reason;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateAppealRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "CreateAppealRequest{"
                + "reason=" + reason + ", "
                + '}';
    }
}
