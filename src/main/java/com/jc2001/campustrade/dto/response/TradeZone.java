package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.ZoneType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交易区域（预设字典，R-15）。**占位数据【待确认】Q-01/Q-02**：
当前种子为 `CAMPUS_A` / `CAMPUS_B` / `CAMPUS_C` 与 `COMMUNITY_A1` 等。
替换占位数据时**不得改变本结构的字段与路径**。

 *
 * <p>对应契约 {@code components.schemas.TradeZone}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "TradeZone", description = "交易区域（预设字典，R-15）。**占位数据【待确认】Q-01/Q-02**： 当前种子为 `CAMPUS_A` / `CAMPUS_B` / `CAMPUS_C` 与 `COMMUNITY_A1` 等。 替换占位数据时**不得改变本结构的字段与路径**。")
public class TradeZone {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 区域编码。**【待确认】占位编码**：CAMPUS_A / COMMUNITY_A1
     */
    @NotNull
    @Schema(description = "区域编码。**【待确认】占位编码**：CAMPUS_A / COMMUNITY_A1", example = "CAMPUS_A")
    private String code;

    /**
     * 区域名称。**【待确认】占位名称**（种子含「【待确认】」前缀）
     */
    @NotNull
    @Schema(description = "区域名称。**【待确认】占位名称**（种子含「【待确认】」前缀）", example = "【待确认】校区 A（占位名称）")
    private String name;

    @NotNull
    @Schema(description = "zoneType")
    private ZoneType zoneType;

    /**
     * 父区域（社区归属校区的层级预留，OOS-07 迭代 N 跨区扩展）
     */
    @Schema(description = "父区域（社区归属校区的层级预留，OOS-07 迭代 N 跨区扩展）")
    private Long parentZoneId;

    @Schema(description = "sortOrder")
    private Integer sortOrder;

    @Schema(description = "active")
    private Boolean active;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public TradeZone() {
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

    public ZoneType getZoneType() {
        return zoneType;
    }

    public void setZoneType(ZoneType zoneType) {
        this.zoneType = zoneType;
    }

    public Long getParentZoneId() {
        return parentZoneId;
    }

    public void setParentZoneId(Long parentZoneId) {
        this.parentZoneId = parentZoneId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "TradeZone{"
                + "id=" + id + ", "
                + "code=" + code + ", "
                + "name=" + name + ", "
                + "zoneType=" + zoneType + ", "
                + "parentZoneId=" + parentZoneId + ", "
                + "sortOrder=" + sortOrder + ", "
                + "active=" + active + ", "
                + '}';
    }
}
