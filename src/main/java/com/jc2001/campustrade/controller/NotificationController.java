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
import com.jc2001.campustrade.dto.request.MarkNotificationsReadRequest;
import com.jc2001.campustrade.dto.response.Notification;
import com.jc2001.campustrade.dto.response.NotificationType;
import com.jc2001.campustrade.dto.response.UnreadCountResponse;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 站内通知（FR-507）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "12-通知", description = "站内通知（FR-507）")
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    /**
     * 站内通知列表（FR-507）
     *
     * <p>operationId: <code>listMyNotifications</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "站内通知列表（FR-507）", operationId = "listMyNotifications", description = "**必须本人**，被处罚用户可访问。返回发给自己的站内通知，按时间倒序。  - 【待确认 Q-05】通知渠道是否仅站内；当前仅实现站内（`emailEnabled` 恒为 false）； - 【待确认 Q-33】通知列表页是否纳入迭代一页面基线（当前为可选项），   但接口先行冻结以支撑治理通知（FR-507）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PageResult<Notification>>> listMyNotifications(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "排序表达式，格式 `字段,方向`（方向 `asc` | `desc`），可重复传入多级排序。 **迭代一允许的排序字段是白名单**（OOS-09 不做推荐/个性化排序）： 商品列表：`published_at`（默认 desc）、`estimated_price`； 订单列表：`created_at`（默认 desc）； 交换提议：`created_at`（默认 desc）； 通知：`created_at`（默认 desc）。 传入白名单外字段返回 `COMMON_INVALID_SORT_FIELD`。") @RequestParam(value = "sort", required = false) List<String> sort,
            @Parameter(description = "仅返回未读") @RequestParam(value = "unreadOnly", required = false, defaultValue = "False") Boolean unreadOnly,
            @Parameter(description = "按通知类型过滤") @RequestParam(value = "type", required = false) NotificationType type
    ) {
        throw new UnsupportedOperationException("TODO: implement listMyNotifications");
    }

    /**
     * 未读通知数（FR-507）
     *
     * <p>operationId: <code>getUnreadNotificationCount</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "未读通知数（FR-507）", operationId = "getUnreadNotificationCount", description = "\"**必须本人**。用于前端角标。**被处罚用户可访问**。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadNotificationCount() {
        throw new UnsupportedOperationException("TODO: implement getUnreadNotificationCount");
    }

    /**
     * 标记通知已读（FR-507）
     *
     * <p>operationId: <code>markNotificationsRead</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "标记通知已读（FR-507）", operationId = "markNotificationsRead", description = "**必须本人**。`ids` 为空或不传表示**全部标记已读**。**幂等（IDEM-A）**：重复标记结果一致。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/notifications/read")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<UnreadCountResponse>> markNotificationsRead(
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody MarkNotificationsReadRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement markNotificationsRead");
    }

}
