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
import com.jc2001.campustrade.dto.response.ScheduledTaskResult;
import com.jc2001.campustrade.service.InternalTaskService;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 系统定时任务（FR-601 ~ FR-604）—— 内部接口，仅系统调用
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "13-系统定时任务", description = "系统定时任务（FR-601 ~ FR-604）—— 内部接口，仅系统调用")
@RestController
@RequestMapping("/api/v1")
public class InternalController {

    private final InternalTaskService tasks;

    public InternalController(InternalTaskService tasks) { this.tasks = tasks; }

    /**
     * 订单超时释放（FR-601 / FR-307 / R-07）
     *
     * <p>operationId: <code>runOrderTimeoutReleaseTask</code>
     * <p>权限: SYSTEM
     * <p>并发: '条件更新 WHERE status = PENDING_PAYMENT AND now() >= expires_at'
     * <p><b>[INTERNAL]</b> 仅系统定时任务调用（X-Internal-Token），网关须禁止外部访问
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "订单超时释放（FR-601 / FR-307 / R-07）", operationId = "runOrderTimeoutReleaseTask", description = "**⚠️ 内部接口，仅系统定时任务调用**（`/internal/` 前缀不对外暴露）。  扫描 `status=PENDING_PAYMENT AND now() >= expires_at` 的订单： 置为 `TIMEOUT_CANCELLED`（`cancel_reason=PAYMENT_TIMEOUT`）， **释放商品占用**（商品 → `IN_SALE`），通知买卖双方。  **幂等（IDEM-B，必须）**：条件更新 `WHERE status='PENDING_PAYMENT'`， 重复执行不产生重复副作用；**已付款订单绝不被超时取消**（R-07）。 建议每分钟执行一次。  响应 `processed` / `skipped` 用于观测并发跳过情况。")
    @SecurityRequirement(name = "internalToken")
    @PreAuthorize("hasRole('SYSTEM')")
    @PostMapping("/internal/tasks/order-timeout-release")
    public ResponseEntity<ApiResponse<ScheduledTaskResult>> runOrderTimeoutReleaseTask() {
        return ResponseEntity.ok(ApiResponse.ok(tasks.runOrderTimeoutReleaseTask()));
    }

    /**
     * 交换提议 72 小时过期（FR-602 / FR-408 / R-25）
     *
     * <p>operationId: <code>runSwapProposalExpireTask</code>
     * <p>权限: SYSTEM
     * <p>并发: '条件更新 WHERE status = PENDING AND now() >= expires_at'
     * <p><b>[INTERNAL]</b> 仅系统定时任务调用（X-Internal-Token），网关须禁止外部访问
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "交换提议 72 小时过期（FR-602 / FR-408 / R-25）", operationId = "runSwapProposalExpireTask", description = "**⚠️ 内部接口，仅系统定时任务调用**。  扫描 `status=PENDING AND now() >= expires_at` 的交换提议： 置为 `EXPIRED`（`termination_reason=EXPIRED_72H`）， **释放全部占用**（双方商品 → `IN_SALE`），通知双方。  **幂等（IDEM-B，必须）**：重复执行不重复释放、不重复通知。 **已 `ACCEPTED` 的提议绝不参与过期**（避免打断已进入交付环节的交换，R-25）。 建议每分钟执行一次。")
    @SecurityRequirement(name = "internalToken")
    @PreAuthorize("hasRole('SYSTEM')")
    @PostMapping("/internal/tasks/swap-proposal-expire")
    public ResponseEntity<ApiResponse<ScheduledTaskResult>> runSwapProposalExpireTask() {
        return ResponseEntity.ok(ApiResponse.ok(tasks.runSwapProposalExpireTask()));
    }

    /**
     * 封禁到期自动解封（FR-506 / FR-603 / R-31）
     *
     * <p>operationId: <code>runPenaltyAutoUnbanTask</code>
     * <p>权限: SYSTEM
     * <p>并发: \"条件更新 WHERE status IN ('ACTIVE','APPEALING') AND now() >= ends_at\"
     * <p><b>[INTERNAL]</b> 仅系统定时任务调用（X-Internal-Token），网关须禁止外部访问
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "封禁到期自动解封（FR-506 / FR-603 / R-31）", operationId = "runPenaltyAutoUnbanTask", description = "**⚠️ 内部接口，仅系统定时任务调用**。  扫描 `status IN (ACTIVE, APPEALING) AND now() >= ends_at` 的处罚： 置为 `EXPIRED`（`lift_source=SYSTEM_EXPIRY`），用户恢复 `ACTIVE`，通知用户。  **幂等（IDEM-B，必须）**：同一处罚**只解封一次**；重复执行不重复写记录、不重复发通知。 **并发**：与「管理员手动解封」（`/admin/penalties/{id}/lift`）或「申诉通过」竞争时， **只允许一方成功**（R-31 条件更新 `WHERE status IN ('ACTIVE','APPEALING')`）。 建议每分钟执行一次。")
    @SecurityRequirement(name = "internalToken")
    @PreAuthorize("hasRole('SYSTEM')")
    @PostMapping("/internal/tasks/penalty-auto-unban")
    public ResponseEntity<ApiResponse<ScheduledTaskResult>> runPenaltyAutoUnbanTask() {
        return ResponseEntity.ok(ApiResponse.ok(tasks.runPenaltyAutoUnbanTask()));
    }

    /**
     * 评价窗口关闭（FR-604 / R-14）
     *
     * <p>operationId: <code>runReviewWindowCloseTask</code>
     * <p>权限: SYSTEM
     * <p>并发: 条件更新 WHERE review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED') AND now() > review_deadline
     * <p><b>[INTERNAL]</b> 仅系统定时任务调用（X-Internal-Token），网关须禁止外部访问
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "评价窗口关闭（FR-604 / R-14）", operationId = "runReviewWindowCloseTask", description = "**⚠️ 内部接口，仅系统定时任务调用**。  扫描 **仅处于未关闭且非终态** 的订单： `review_status IN ('OPEN','BUYER_REVIEWED','SELLER_REVIEWED') AND now() > review_deadline`， 置为 `WINDOW_CLOSED`，关闭评价入口。  **必须排除终态 `BOTH_REVIEWED`**：`BOTH_REVIEWED`（双方已评）与 `WINDOW_CLOSED` 均为终态， 受 **IDEM-C 终态保护**（评价状态机 R-05）。若写成 `review_status <> 'WINDOW_CLOSED'`， 会把已经双方评价完成的订单**回退**为 `WINDOW_CLOSED`，违反状态机 §8 的 R-04 起始状态集合 （R-04 的起始状态是「任意未关闭」中的**活跃态**：OP")
    @SecurityRequirement(name = "internalToken")
    @PreAuthorize("hasRole('SYSTEM')")
    @PostMapping("/internal/tasks/review-window-close")
    public ResponseEntity<ApiResponse<ScheduledTaskResult>> runReviewWindowCloseTask() {
        return ResponseEntity.ok(ApiResponse.ok(tasks.runReviewWindowCloseTask()));
    }

}
