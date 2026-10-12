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
import com.jc2001.campustrade.dto.request.BanUserRequest;
import com.jc2001.campustrade.dto.request.CreateAppealRequest;
import com.jc2001.campustrade.dto.request.HandleReportRequest;
import com.jc2001.campustrade.dto.request.LiftPenaltyRequest;
import com.jc2001.campustrade.dto.request.ResolveAppealRequest;
import com.jc2001.campustrade.dto.request.RestoreItemRequest;
import com.jc2001.campustrade.dto.request.TakedownItemRequest;
import com.jc2001.campustrade.dto.response.AdminUserDetail;
import com.jc2001.campustrade.dto.response.AdminUserSummary;
import com.jc2001.campustrade.dto.response.Appeal;
import com.jc2001.campustrade.dto.response.AppealStatus;
import com.jc2001.campustrade.dto.response.AuditLog;
import com.jc2001.campustrade.dto.response.ItemDetail;
import com.jc2001.campustrade.dto.response.Penalty;
import com.jc2001.campustrade.dto.response.PenaltyStatus;
import com.jc2001.campustrade.dto.response.PenaltyType;
import com.jc2001.campustrade.dto.response.Report;
import com.jc2001.campustrade.dto.response.ReportStatus;
import com.jc2001.campustrade.dto.response.TakedownItemResponse;
import com.jc2001.campustrade.dto.response.UserStatus;
import java.time.Instant;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 管理员治理（FR-501 ~ FR-505）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "11-管理员治理", description = "管理员治理（FR-501 ~ FR-505）")
@RestController
@RequestMapping("/api/v1")
public class AdminController {

    /**
     * 搜索用户（FR-501）
     *
     * <p>operationId: <code>adminSearchUsers</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "搜索用户（FR-501）", operationId = "adminSearchUsers", description = "**仅管理员**（`ROLE_ADMIN`）。按 **用户名 / 昵称 / 用户 ID** 检索，分页返回。  - 普通用户调用返回 `403 AUTH_ADMIN_REQUIRED`； - **管理员不参与交易**（P-00-A），管理员账号不会出现在交易方列表的正常业务路径中； - 返回历史处罚次数与当前生效处罚 ID，便于判断是否可再次封禁（R-30 / E-13）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<PageResult<AdminUserSummary>>> adminSearchUsers(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "关键词，匹配用户名或昵称（大小写不敏感）") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "精确按用户 ID 查询") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "按账号状态过滤") @RequestParam(value = "status", required = false) UserStatus status
    ) {
        throw new UnsupportedOperationException("TODO: implement adminSearchUsers");
    }

    /**
     * 查看用户详情与交易概览（permission-matrix 6.2）
     *
     * <p>operationId: <code>adminGetUserDetail</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看用户详情与交易概览（permission-matrix 6.2）", operationId = "adminGetUserDetail", description = "**仅管理员**。返回用户资料、在售商品、处罚记录、举报记录、申诉记录，用于治理判断。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetail>> adminGetUserDetail(
            @Parameter(description = "") @PathVariable("userId") Long userId
    ) {
        throw new UnsupportedOperationException("TODO: implement adminGetUserDetail");
    }

    /**
     * 封禁用户三天（FR-503 / R-30）
     *
     * <p>operationId: <code>adminBanUser</code>
     * <p>权限: ADMIN
     * <p>状态转换: '（无） → penalties.ACTIVE（P-01）；users.status ACTIVE → RESTRICTED'
     * <p>并发: '部分唯一索引 uq_penalties_one_active（同一用户同时最多一条生效中 BAN_3D）'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "封禁用户三天（FR-503 / R-30）", operationId = "adminBanUser", description = "**仅管理员**。封禁 **固定 3 天（72 小时）**（假设 A-03 / Q-09：不可调时长）。  - **`reason` 必填**（R-34 审计要求），缺失返回 `400 VALIDATION_REASON_REQUIRED`； - 目标用户**已有生效中封禁**时返回 `409 GOVERNANCE_ACTIVE_PENALTY_EXISTS`   （假设 Q-10：**拒绝重复封禁**，不产生两条并存生效处罚）； - 目标为管理员返回 `403 GOVERNANCE_CANNOT_PUNISH_ADMIN`； - 副作用：用户 → `RESTRICTED` + `restrictedUntil = now + 72h`，创建 `penalties` 记录，   下发站内通知，写审计日志； - **幂等（IDEM-A）**：同幂等键重复封禁不产生第二条生效处罚。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{userId}/ban")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Penalty>> adminBanUser(
            @Parameter(description = "") @PathVariable("userId") Long userId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody BanUserRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminBanUser");
    }

    /**
     * 查看某用户处罚记录（FR-504）
     *
     * <p>operationId: <code>adminListUserPenalties</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看某用户处罚记录（FR-504）", operationId = "adminListUserPenalties", description = "**仅管理员**。返回该用户**全部历史处罚**（**记录永不删除**，R-32）， 含状态、原因、起止时间、操作管理员、申诉结论。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{userId}/penalties")
    public ResponseEntity<ApiResponse<PageResult<Penalty>>> adminListUserPenalties(
            @Parameter(description = "") @PathVariable("userId") Long userId,
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "") @RequestParam(value = "penaltyType", required = false) PenaltyType penaltyType,
            @Parameter(description = "") @RequestParam(value = "status", required = false) PenaltyStatus status
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListUserPenalties");
    }

    /**
     * 全量处罚记录查询（FR-504）
     *
     * <p>operationId: <code>adminListPenalties</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "全量处罚记录查询（FR-504）", operationId = "adminListPenalties", description = "**仅管理员**。按用户、时间、类型、状态查询**全量**处罚历史（用户仅可见自己的，见 `/users/me/penalties`）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/penalties")
    public ResponseEntity<ApiResponse<PageResult<Penalty>>> adminListPenalties(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "") @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @Parameter(description = "") @RequestParam(value = "penaltyType", required = false) PenaltyType penaltyType,
            @Parameter(description = "") @RequestParam(value = "status", required = false) PenaltyStatus status,
            @Parameter(description = "起始时间（含），ISO 8601 UTC") @RequestParam(value = "from", required = false) Instant from,
            @Parameter(description = "结束时间（含）") @RequestParam(value = "to", required = false) Instant to
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListPenalties");
    }

    /**
     * 手动解封（permission-matrix 6.8）
     *
     * <p>operationId: <code>adminLiftPenalty</code>
     * <p>权限: ADMIN
     * <p>状态转换: 'ACTIVE / APPEALING → LIFTED（P-07）'
     * <p>并发: '条件更新 WHERE status IN (ACTIVE, APPEALING)'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "手动解封（permission-matrix 6.8）", operationId = "adminLiftPenalty", description = "**仅管理员**。**`reason` 必填**（R-34）。  - 处罚记录**保留**，仅状态改为 `LIFTED`（`lift_source=ADMIN_MANUAL`），用户恢复 `ACTIVE`（P-07）； - **并发**：与「到期自动解封」（FR-603）竞争时**只允许一方成功**   （R-18 条件更新 `WHERE status IN ('ACTIVE','APPEALING')`）；   已解封/已过期时返回 `409 GOVERNANCE_PENALTY_ALREADY_RESOLVED`； - **幂等（IDEM-A）**：重复解封返回首次结果。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/penalties/{penaltyId}/lift")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Penalty>> adminLiftPenalty(
            @Parameter(description = "") @PathVariable("penaltyId") Long penaltyId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody LiftPenaltyRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminLiftPenalty");
    }

    /**
     * 下架违规商品（FR-502 / R-35）
     *
     * <p>operationId: <code>adminTakedownItem</code>
     * <p>权限: ADMIN
     * <p>状态转换: 'IN_SALE / OFF_SHELF / RESERVED / SWAPPING → BANNED（I-14 / I-15）+ 联动作废（R-35）'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "下架违规商品（FR-502 / R-35）", operationId = "adminTakedownItem", description = "**仅管理员**。`reason` **必填**（R-34）。商品 → `BANNED`（I-14 / I-15）。  **联动处理（R-35，本接口的关键语义）**： - 若商品被**订单**占用 → 该订单**取消**（`cancel_reason=ADMIN_TAKEDOWN`），释放占用； - 若商品被**交换**占用 → 该交换提议**整笔作废**（`termination_reason=VOID_ITEM_BANNED`），   释放**全部**相关占用（R-24）； - 必须通知所有受影响方（买家 / 交换双方 / 卖家）； - 响应明示影响范围（`affectedOrders` / `voidedSwapProposals`）。  **幂等（IDEM-A）**：重复下架不产生重复记录、不重复作废。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/items/{itemId}/takedown")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<TakedownItemResponse>> adminTakedownItem(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody TakedownItemRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminTakedownItem");
    }

    /**
     * 恢复被误下架商品（permission-matrix 6.4）
     *
     * <p>operationId: <code>adminRestoreItem</code>
     * <p>权限: ADMIN
     * <p>状态转换: 'BANNED → IN_SALE / OFF_SHELF（I-16）'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "恢复被误下架商品（permission-matrix 6.4）", operationId = "adminRestoreItem", description = "**仅管理员**。`reason` **必填**。将 `BANNED` 商品恢复为 `IN_SALE` 或 `OFF_SHELF`（I-16）。  - 恢复前须**无占用冲突**（商品已被占用时返回 `409 ITEM_LOCKED_BY_OCCUPATION`）； - **幂等（IDEM-A）**：重复恢复不产生重复记录。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/items/{itemId}/restore")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> adminRestoreItem(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody RestoreItemRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminRestoreItem");
    }

    /**
     * 申诉列表（FR-505）
     *
     * <p>operationId: <code>adminListAppeals</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "申诉列表（FR-505）", operationId = "adminListAppeals", description = "\"**仅管理员**。按状态查询待处理/已处理申诉（**记录永不删除**，R-32）。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/appeals")
    public ResponseEntity<ApiResponse<PageResult<Appeal>>> adminListAppeals(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "") @RequestParam(value = "status", required = false) AppealStatus status
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListAppeals");
    }

    /**
     * 处理申诉（FR-505 / AP-02 / AP-03）
     *
     * <p>operationId: <code>adminResolveAppeal</code>
     * <p>权限: ADMIN
     * <p>状态转换: 'SUBMITTED → APPROVED / REJECTED（AP-02 / AP-03）；联动 penalties'
     * <p>并发: '条件更新 WHERE status = SUBMITTED'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "处理申诉（FR-505 / AP-02 / AP-03）", operationId = "adminResolveAppeal", description = "**仅管理员**。**`note`（处理意见）必填**（R-34）。  - `decision=APPROVED` → 立即解封：处罚 `LIFTED`（`lift_source=APPEAL_APPROVED`），用户恢复 `ACTIVE`； - `decision=REJECTED` → 处罚恢复 `ACTIVE`，**继续计到期**（R-32）； - 处罚记录与申诉记录**均保留**，仅更新状态与结论； - **幂等（IDEM-A）**：重复处理返回**首次结论**，不重复改状态； - 已处理（非 `SUBMITTED`）时返回 `409 APPEAL_ALREADY_RESOLVED`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/appeals/{appealId}/resolve")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Appeal>> adminResolveAppeal(
            @Parameter(description = "") @PathVariable("appealId") Long appealId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody ResolveAppealRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminResolveAppeal");
    }

    /**
     * 举报列表（permission-matrix 6.9）
     *
     * <p>operationId: <code>adminListReports</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "举报列表（permission-matrix 6.9）", operationId = "adminListReports", description = "\"**仅管理员**。按状态/时间查询举报，默认返回待处理（`PENDING`）。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports")
    public ResponseEntity<ApiResponse<PageResult<Report>>> adminListReports(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "") @RequestParam(value = "status", required = false) ReportStatus status,
            @Parameter(description = "") @RequestParam(value = "itemId", required = false) Long itemId
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListReports");
    }

    /**
     * 处理举报（permission-matrix 6.9）
     *
     * <p>operationId: <code>adminHandleReport</code>
     * <p>权限: ADMIN
     * <p>状态转换: 'PENDING → HANDLED / DISMISSED'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "处理举报（permission-matrix 6.9）", operationId = "adminHandleReport", description = "**仅管理员**。受理（`HANDLED`）或驳回（`DISMISSED`），**`note` 必填**（R-34 留痕）。  - 受理后可根据情况另行调用 `POST /admin/items/{itemId}/takedown` 下架商品（R-35 联动）； - **幂等（IDEM-A）**：重复处理返回首次结论，已处理时返回 `409 REPORT_ALREADY_HANDLED`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reports/{reportId}/handle")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Report>> adminHandleReport(
            @Parameter(description = "") @PathVariable("reportId") Long reportId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody HandleReportRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement adminHandleReport");
    }

    /**
     * 查看审计日志（permission-matrix 6.10 / R-34）
     *
     * <p>operationId: <code>adminListAuditLogs</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看审计日志（permission-matrix 6.10 / R-34）", operationId = "adminListAuditLogs", description = "**仅管理员**。查询治理动作留痕：操作者、时间、对象、原因、前后状态。 审计日志为**追加写**，不可修改或删除（Q-32 假设不支持导出）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/audit-logs")
    public ResponseEntity<ApiResponse<PageResult<AuditLog>>> adminListAuditLogs(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "") @RequestParam(value = "targetType", required = false) String targetType,
            @Parameter(description = "") @RequestParam(value = "targetId", required = false) Long targetId,
            @Parameter(description = "") @RequestParam(value = "actorId", required = false) Long actorId,
            @Parameter(description = "") @RequestParam(value = "action", required = false) String action
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListAuditLogs");
    }

    /**
     * 查看预设字典（permission-matrix 6.11）
     *
     * <p>operationId: <code>adminListDictionary</code>
     * <p>权限: ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看预设字典（permission-matrix 6.11）", operationId = "adminListDictionary", description = "**仅管理员**。查看分类 / 成色 / 交易区域 / 公共取货点字典（含**已停用**条目）。  - 【待确认 Q-01/Q-02/Q-03】区域与取货点为占位数据； - 迭代一**仅提供查看**：字典维护（增删改）视为超出范围（Q-21 假设仅管理员维护，   具体是否本迭代提供写接口见 api-open-questions.md AQ-04）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dictionaries/{dictionaryType}")
    public ResponseEntity<ApiResponse<List<Object>>> adminListDictionary(
            @Parameter(description = "") @PathVariable("dictionaryType") String dictionaryType
    ) {
        throw new UnsupportedOperationException("TODO: implement adminListDictionary");
    }

    /**
     * 查看自己的处罚记录（permission-matrix 2.11）
     *
     * <p>operationId: <code>listMyPenalties</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看自己的处罚记录（permission-matrix 2.11）", operationId = "listMyPenalties", description = "**必须本人**（**被处罚用户也可访问**，permission-matrix §8 白名单）。 返回自己的处罚历史与当前解封时间，用于前端展示处罚提示与申诉入口。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/penalties")
    public ResponseEntity<ApiResponse<PageResult<Penalty>>> listMyPenalties(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        throw new UnsupportedOperationException("TODO: implement listMyPenalties");
    }

    /**
     * 提交申诉（permission-matrix 2.12 / AP-01）
     *
     * <p>operationId: <code>createAppeal</code>
     * <p>权限: USER, RESTRICTED
     * <p>状态转换: '（无） → appeals.SUBMITTED（AP-01）；penalties.ACTIVE → APPEALING（P-03）'
     * <p>幂等: required
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "提交申诉（permission-matrix 2.12 / AP-01）", operationId = "createAppeal", description = "**必须为处罚对象本人**（**被处罚用户可访问**，白名单）。  - 仅可对**生效中**（`ACTIVE`）处罚申诉，否则 `409 APPEAL_PENALTY_NOT_ACTIVE`（假设 Q-17）； - **一个处罚最多一条申诉**（AP-01 / `uq_appeals_penalty`），重复提交返回 `409 APPEAL_ALREADY_EXISTS`； - 提交后处罚标记 `APPEALING`，**处罚仍然生效**（不停表，P-03）； - **幂等（IDEM-A）**：同幂等键重复提交返回同一条申诉。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/penalties/{penaltyId}/appeals")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<Appeal>> createAppeal(
            @Parameter(description = "") @PathVariable("penaltyId") Long penaltyId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CreateAppealRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement createAppeal");
    }

    /**
     * 我的申诉（permission-matrix 2.12）
     *
     * <p>operationId: <code>listMyAppeals</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "我的申诉（permission-matrix 2.12）", operationId = "listMyAppeals", description = "\"**必须本人**，被处罚用户可访问。返回自己提交的申诉及其处理结论。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/appeals/mine")
    public ResponseEntity<ApiResponse<PageResult<Appeal>>> listMyAppeals(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        throw new UnsupportedOperationException("TODO: implement listMyAppeals");
    }

}
