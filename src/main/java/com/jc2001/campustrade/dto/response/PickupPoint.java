package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.PickupPointType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 公共取货点（预设字典，R-06）。**占位数据【待确认】Q-03**：
当前种子为 `PICKUP_LIBRARY_A` 等 13 个。
**只有文字说明，没有经纬度**（OOS-06 不做地图导航）。

 *
 * <p>对应契约 {@code components.schemas.PickupPoint}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "PickupPoint", description = "公共取货点（预设字典，R-06）。**占位数据【待确认】Q-03**： 当前种子为 `PICKUP_LIBRARY_A` 等 13 个。 **只有文字说明，没有经纬度**（OOS-06 不做地图导航）。")
public class PickupPoint {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 取货点编码。**【待确认】占位编码**：PICKUP_LIBRARY_A
     */
    @NotNull
    @Schema(description = "取货点编码。**【待确认】占位编码**：PICKUP_LIBRARY_A", example = "PICKUP_LIBRARY_A")
    private String code;

    /**
     * 取货点名称。**【待确认】占位名称**
     */
    @NotNull
    @Schema(description = "取货点名称。**【待确认】占位名称**", example = "【待确认】图书馆门口-校区A（占位）")
    private String name;

    @NotNull
    @Schema(description = "pointType")
    private PickupPointType pointType;

    /**
     * 所属交易区域。**取货点必须与订单同区域**（R-06）
     */
    @NotNull
    @Schema(description = "所属交易区域。**取货点必须与订单同区域**（R-06）")
    private Long tradeZoneId;

    /**
     * 文字说明，替代地图与导航（OOS-06）
     */
    @Schema(description = "文字说明，替代地图与导航（OOS-06）", example = "图书馆正门台阶右侧，有监控覆盖")
    private String description;

    @Schema(description = "sortOrder")
    private Integer sortOrder;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public PickupPoint() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PickupPointType getPointType() {
        return pointType;
    }

    public void setPointType(PickupPointType pointType) {
        this.pointType = pointType;
    }

    public Long getTradeZoneId() {
        return tradeZoneId;
    }

    public void setTradeZoneId(Long tradeZoneId) {
        this.tradeZoneId = tradeZoneId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "PickupPoint{"
                + "id=" + id + ", "
                + "code=" + code + ", "
                + "name=" + name + ", "
                + "pointType=" + pointType + ", "
                + "tradeZoneId=" + tradeZoneId + ", "
                + "description=" + description + ", "
                + "sortOrder=" + sortOrder + ", "
                + '}';
    }
}
