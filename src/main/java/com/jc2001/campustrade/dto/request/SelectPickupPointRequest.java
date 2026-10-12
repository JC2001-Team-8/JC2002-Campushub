package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 选择公共取货点（FR-309 / R-06）。
取货点必须属于订单所在交易区域，否则 422 `PICKUP_POINT_ZONE_MISMATCH`。
订单 `DELIVERED` 之后不可修改。

 *
 * <p>对应契约 {@code components.schemas.SelectPickupPointRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "SelectPickupPointRequest", description = "选择公共取货点（FR-309 / R-06）。 取货点必须属于订单所在交易区域，否则 422 `PICKUP_POINT_ZONE_MISMATCH`。 订单 `DELIVERED` 之后不可修改。")
public class SelectPickupPointRequest {

    @NotNull
    @Schema(description = "pickupPointId")
    private Long pickupPointId;

    @Schema(description = "version")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SelectPickupPointRequest() {
    }

    public Long getPickupPointId() {
        return pickupPointId;
    }

    public void setPickupPointId(Long pickupPointId) {
        this.pickupPointId = pickupPointId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "SelectPickupPointRequest{"
                + "pickupPointId=" + pickupPointId + ", "
                + "version=" + version + ", "
                + '}';
    }
}
