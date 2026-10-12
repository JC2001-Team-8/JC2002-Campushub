package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 取消订单（FR-308）。
**仅 `PENDING_PAYMENT` 可自助取消**；已付款不可自助取消（假设 A-14 / Q-08），
否则 409 `ORDER_CANNOT_CANCEL_AFTER_PAID`。

 *
 * <p>对应契约 {@code components.schemas.CancelOrderRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CancelOrderRequest", description = "取消订单（FR-308）。 **仅 `PENDING_PAYMENT` 可自助取消**；已付款不可自助取消（假设 A-14 / Q-08）， 否则 409 `ORDER_CANNOT_CANCEL_AFTER_PAID`。")
public class CancelOrderRequest {

    @Size(max = 200)
    @Schema(description = "reason", example = "不想买了")
    private String reason;

    @Schema(description = "version")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CancelOrderRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "CancelOrderRequest{"
                + "reason=" + reason + ", "
                + "version=" + version + ", "
                + '}';
    }
}
