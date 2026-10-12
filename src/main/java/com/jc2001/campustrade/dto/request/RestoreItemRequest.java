package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 恢复被误下架商品（permission-matrix 6.4）。**必须填原因**；无占用冲突时恢复为在售
 *
 * <p>对应契约 {@code components.schemas.RestoreItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "RestoreItemRequest", description = "恢复被误下架商品（permission-matrix 6.4）。**必须填原因**；无占用冲突时恢复为在售")
public class RestoreItemRequest {

    @NotNull
    @Size(min = 1, max = 255)
    @Schema(description = "reason", example = "经复核为误判，予以恢复")
    private String reason;

    /**
     * 恢复目标状态（I-16）
     */
    @Schema(description = "恢复目标状态（I-16）", example = "IN_SALE")
    private String restoreTo;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public RestoreItemRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRestoreTo() {
        return restoreTo;
    }

    public void setRestoreTo(String restoreTo) {
        this.restoreTo = restoreTo;
    }

    @Override
    public String toString() {
        return "RestoreItemRequest{"
                + "reason=" + reason + ", "
                + "restoreTo=" + restoreTo + ", "
                + '}';
    }
}
