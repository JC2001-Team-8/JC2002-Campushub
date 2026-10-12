package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 设置常用交易区域（FR-107 / R-15）。**只能从预设区域勾选 1–5 个**，不接受自由文本地址
 *
 * <p>对应契约 {@code components.schemas.UserTradeZonesRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UserTradeZonesRequest", description = "设置常用交易区域（FR-107 / R-15）。**只能从预设区域勾选 1–5 个**，不接受自由文本地址")
public class UserTradeZonesRequest {

    /**
     * 交易区域 ID 列表，1–5 个且必须存在于预设字典
     */
    @NotNull
    @Size(min = 1, max = 5)
    @Schema(description = "交易区域 ID 列表，1–5 个且必须存在于预设字典")
    private List<Long> tradeZoneIds;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UserTradeZonesRequest() {
    }

    public List<Long> getTradeZoneIds() {
        return tradeZoneIds;
    }

    public void setTradeZoneIds(List<Long> tradeZoneIds) {
        this.tradeZoneIds = tradeZoneIds;
    }

    @Override
    public String toString() {
        return "UserTradeZonesRequest{"
                + "tradeZoneIds=" + tradeZoneIds + ", "
                + '}';
    }
}
