package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateReportRequest;
import com.jc2001.campustrade.dto.response.Report;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 举报（FR-211）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "10-举报", description = "举报（FR-211）")
@RestController
@RequestMapping("/api/v1")
public class ReportController {

    /**
     * 举报商品（FR-211）
     *
     * <p>operationId: <code>reportItem</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "举报商品（FR-211）", operationId = "reportItem", description = "任何登录用户可举报商品（**被处罚用户也可举报**）。需选择举报原因（`reason` 必填）。  - **同一用户对同一商品最多 1 条未处理举报**（C-06）。重复举报**幂等**（E-14）：   返回 `200` 并携带既有举报记录，`code=REPORT_ALREADY_EXISTS`。 - 举报自己的商品返回 `422 REPORT_OWN_ITEM_NOT_ALLOWED`。 - 商品不存在返回 `404 ITEM_NOT_FOUND`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/items/{itemId}/reports")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Report>> reportItem(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateReportRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement reportItem");
    }

    /**
     * 我提交的举报（permission-matrix 3.13）
     *
     * <p>operationId: <code>listMyReports</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "我提交的举报（permission-matrix 3.13）", operationId = "listMyReports", description = "查看自己提交的举报及其处理状态。**必须本人**。 迭代一**不提供**「查看自己被举报的记录」给普通用户（避免举报人暴露）， 仅管理员可通过治理接口查看（permission-matrix 3.13 的「仅本人或管理员」中， 本接口实现「本人=举报人」视角）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/reports")
    public ResponseEntity<ApiResponse<PageResult<Report>>> listMyReports(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        throw new UnsupportedOperationException("TODO: implement listMyReports");
    }

    /**
     * 查看我的举报详情（permission-matrix 3.13）
     *
     * <p>operationId: <code>getMyReport</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看我的举报详情（permission-matrix 3.13）", operationId = "getMyReport", description = "\"**仅举报人本人或管理员**可查看。非本人且非管理员返回 `403 AUTH_NOT_RESOURCE_OWNER`。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Report>> getMyReport(
            @Parameter(description = "") @PathVariable("reportId") Long reportId
    ) {
        throw new UnsupportedOperationException("TODO: implement getMyReport");
    }

}
