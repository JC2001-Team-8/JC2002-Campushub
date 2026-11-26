package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.TraceIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jc2001.campustrade.dto.response.Category;
import com.jc2001.campustrade.dto.response.ItemCondition;
import com.jc2001.campustrade.dto.response.PickupPoint;
import com.jc2001.campustrade.dto.response.PickupPointType;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.ZoneType;
import com.jc2001.campustrade.service.DictionaryService;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 预设字典与区域（R-15 / R-06 / FR-206 / FR-207）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "03-字典与区域", description = "预设字典与区域（R-15 / R-06 / FR-206 / FR-207）")
@RestController
@RequestMapping("/api/v1")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    public DictionaryController(DictionaryService dictionaryService) { this.dictionaryService = dictionaryService; }

    /**
     * 预设交易区域列表（R-15）
     *
     * <p>operationId: <code>listTradeZones</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "预设交易区域列表（R-15）", operationId = "listTradeZones", description = "返回全部启用的预设交易区域，供用户勾选常用区域、发布商品时选择。  - **无需登录**。 - **【待确认 Q-01/Q-02】**：当前为**占位数据**（`CAMPUS_A`、`CAMPUS_B`、`CAMPUS_C`、   `COMMUNITY_A1` 等 7 条），名称含「【待确认】」前缀。   替换真实校区/社区时**不改变路径与 DTO 结构**。 - 迭代一最终只保留 **2–3 个校区及周边社区**；超出范围的地理区域不支持选择。 - 本迭代**不提供距离/附近排序**（GEO-04 / OOS-06）。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/trade-zones")
    public ResponseEntity<ApiResponse<List<TradeZone>>> listTradeZones(
            @Parameter(description = "按类型过滤") @RequestParam(value = "zoneType", required = false) ZoneType zoneType,
            @Parameter(description = "是否包含已停用区域（仅管理员可用，其他角色传 true 被忽略）") @RequestParam(value = "includeInactive", required = false, defaultValue = "False") Boolean includeInactive,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dictionaryService.listTradeZones(zoneType, includeInactive))
                .withTraceId(TraceIdFilter.currentTraceId(request)));
    }

    /**
     * 按交易区域查询公共取货点（R-06）
     *
     * <p>operationId: <code>listPickupPointsByZone</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "按交易区域查询公共取货点（R-06）", operationId = "listPickupPointsByZone", description = "返回指定交易区域内启用的公共取货点，供买家选择交付地点（FR-309 / PF-13）。  - **无需登录**（浏览阶段即可查看）。 - **【待确认 Q-03】**：当前为**占位数据**（`PICKUP_LIBRARY_A` 等 13 条），名称含「【待确认】」。 - **只有文字说明，没有经纬度、没有地图导航**（OOS-06 / GEO-03）。 - 区域不存在返回 `404 TRADE_ZONE_NOT_FOUND`。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/trade-zones/{tradeZoneId}/pickup-points")
    public ResponseEntity<ApiResponse<List<PickupPoint>>> listPickupPointsByZone(
            @Parameter(description = "交易区域 ID") @PathVariable("tradeZoneId") Long tradeZoneId,
            @Parameter(description = "按取货点类型过滤") @RequestParam(value = "pointType", required = false) PickupPointType pointType,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(dictionaryService.listPickupPointsByZone(tradeZoneId, pointType))
                .withTraceId(TraceIdFilter.currentTraceId(request)));
    }

    /**
     * 商品分类字典（FR-206）
     *
     * <p>operationId: <code>listCategories</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "商品分类字典（FR-206）", operationId = "listCategories", description = "返回预设分类字典（教材书籍、电子数码、生活用品、服饰、运动器材、其他）。 **无需登录**。Q-21 假设：仅管理员维护，用户不可自定义。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> listCategories(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dictionaryService.listCategories()).withTraceId(TraceIdFilter.currentTraceId(request)));
    }

    /**
     * 商品成色字典（FR-207）
     *
     * <p>operationId: <code>listItemConditions</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "商品成色字典（FR-207）", operationId = "listItemConditions", description = "返回预设成色枚举（全新、几乎全新、轻微使用痕迹、明显使用痕迹、有瑕疵）。**无需登录**。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/item-conditions")
    public ResponseEntity<ApiResponse<List<ItemCondition>>> listItemConditions(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dictionaryService.listItemConditions()).withTraceId(TraceIdFilter.currentTraceId(request)));
    }

}
