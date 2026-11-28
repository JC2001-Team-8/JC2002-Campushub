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
import com.jc2001.campustrade.dto.request.ConfirmSwapRequest;
import com.jc2001.campustrade.dto.request.CounterSwapProposalRequest;
import com.jc2001.campustrade.dto.request.CreateReviewRequest;
import com.jc2001.campustrade.dto.request.CreateSwapBundleRequest;
import com.jc2001.campustrade.dto.request.CreateSwapProposalRequest;
import com.jc2001.campustrade.dto.request.RejectSwapProposalRequest;
import com.jc2001.campustrade.dto.request.UpdateSwapBundleRequest;
import com.jc2001.campustrade.dto.request.VerifySwapCodeRequest;
import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.SwapBundleDetail;
import com.jc2001.campustrade.dto.response.SwapBundleStatus;
import com.jc2001.campustrade.dto.response.SwapBundleSummary;
import com.jc2001.campustrade.dto.response.SwapCode;
import com.jc2001.campustrade.dto.response.SwapProposalDetail;
import com.jc2001.campustrade.dto.response.SwapProposalStatus;
import com.jc2001.campustrade.dto.response.SwapProposalSummary;
import com.jc2001.campustrade.service.SwapService;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换包与交换提议（FR-402 ~ FR-412）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>业务逻辑已实现</b>（见对应 ServiceImpl）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "08-交换包", description = "交换包与交换提议（FR-402 ~ FR-412）")
@RestController
@RequestMapping("/api/v1")
public class SwapController {

    private final SwapService swapService;

    public SwapController(SwapService swapService) { this.swapService = swapService; }

    /**
     * 创建交换包（FR-402 / R-21）
     *
     * <p>operationId: <code>createSwapBundle</code>
     * <p>权限: USER
     * <p>状态转换: '（无） → DRAFT / PACKED（B-01）'
     * <p>并发: '部分唯一索引 uq_swap_bundle_items_active（WHERE item_status=ACTIVE）'
     * <p>幂等: required
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "创建交换包（FR-402 / R-21）", operationId = "createSwapBundle", description = "**必须登录且未被处罚**。将 **1–5 件自己的商品**打包成交换包。  规则（R-21 / SB-01~SB-03）： - 件数必须 **1–5**，越界返回 `422 SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE`（E-07）； - 包内物品**必须全部属于本人**，否则 `422 SWAP_BUNDLE_ITEM_NOT_OWNED`； - 同一物品同时只能存在于一个**进行中**交换包（SB-03），   冲突返回 `409 SWAP_ITEM_IN_ACTIVE_BUNDLE`； - 物品已被订单占用返回 `409 ITEM_ALREADY_RESERVED`； - 交换包区域须与后续目标商品同区域（R-15）。  `estimatedTotal` 为包内估价合计，**仅展示**（R-13）， **不自动判定等价、不支持现金补差价**（OOS-03）")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-bundles")
    public ResponseEntity<ApiResponse<SwapBundleDetail>> createSwapBundle(
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CreateSwapBundleRequest body
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(swapService.createSwapBundle(idempotencyKey, body)));
    }

    /**
     * 我的交换包列表（permission-matrix 5.4 / PF-15）
     *
     * <p>operationId: <code>listMySwapBundles</code>
     * <p>权限: USER, RESTRICTED
     *
     */
    @Operation(summary = "我的交换包列表（permission-matrix 5.4 / PF-15）", operationId = "listMySwapBundles", description = "\"**必须本人**。返回自己创建的交换包及其状态（草稿/已打包/交换中/已占用/已释放/已完成）。\"")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/swap-bundles")
    public ResponseEntity<ApiResponse<PageResult<SwapBundleSummary>>> listMySwapBundles(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "按状态过滤，可重复传入") @RequestParam(value = "status", required = false) List<SwapBundleStatus> status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.listMySwapBundles(page,size,status)));
    }

    /**
     * 交换包详情（PF-15）
     *
     * <p>operationId: <code>getSwapBundle</code>
     * <p>权限: USER, ADMIN
     *
     */
    @Operation(summary = "交换包详情（PF-15）", operationId = "getSwapBundle", description = "**必须本人**（交换包仅包主可见；管理员在治理场景可只读）。 非包主返回 `403 AUTH_NOT_RESOURCE_OWNER`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/swap-bundles/{bundleId}")
    public ResponseEntity<ApiResponse<SwapBundleDetail>> getSwapBundle(
            @Parameter(description = "") @PathVariable("bundleId") Long bundleId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.getSwapBundle(bundleId)));
    }

    /**
     * 编辑交换包（permission-matrix 5.3）
     *
     * <p>operationId: <code>updateSwapBundle</code>
     * <p>权限: USER
     * <p>状态转换: 'PACKED / RELEASED → DRAFT（B-08）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "编辑交换包（permission-matrix 5.3）", operationId = "updateSwapBundle", description = "**仅包主本人**。全量替换包内物品（仍须 1–5 件）。  - **被进行中提议引用（`IN_SWAP`）时不可编辑**（SB-04），返回 `409 SWAP_BUNDLE_LOCKED`； - `COMPLETED` 为终态，不可编辑（`409 SWAP_BUNDLE_STATUS_NOT_EDITABLE`）； - **并发**：请求体 `version` 参与条件更新（R-18），冲突返回 `409 COMMON_VERSION_CONFLICT`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/swap-bundles/{bundleId}")
    public ResponseEntity<ApiResponse<SwapBundleDetail>> updateSwapBundle(
            @Parameter(description = "") @PathVariable("bundleId") Long bundleId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody UpdateSwapBundleRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.updateSwapBundle(bundleId,idempotencyKey,body)));
    }

    /**
     * 删除交换包（permission-matrix 5.5 / B-11）
     *
     * <p>operationId: <code>deleteSwapBundle</code>
     * <p>权限: USER
     * <p>状态转换: 'PACKED → （软删除）（B-11）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "删除交换包（permission-matrix 5.5 / B-11）", operationId = "deleteSwapBundle", description = "**仅包主本人**。软删除（`deleted_at`，B-11：建议软删除）。  - **被进行中提议引用时不可删除**，返回 `409 SWAP_BUNDLE_LOCKED`； - **幂等**：重复删除返回 `204`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/swap-bundles/{bundleId}")
    public ResponseEntity<Void> deleteSwapBundle(
            @Parameter(description = "") @PathVariable("bundleId") Long bundleId
    ) {
        swapService.deleteSwapBundle(bundleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发起交换提议（FR-404）
     *
     * <p>operationId: <code>createSwapProposal</code>
     * <p>权限: USER
     * <p>状态转换: '（无） → PENDING（S-01）；双方商品 IN_SALE → SWAPPING（I-11）'
     * <p>并发: '部分唯一索引 uq_item_occ_active_item（同一商品同时最多一个进行中占用）'
     * <p>幂等: required
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "发起交换提议（FR-404）", operationId = "createSwapProposal", description = "**必须登录且未被处罚**。用**整个交换包**向**他人的一件或多件商品**发起交换提议。  前置条件（不满足返回 409 / 422）： - 交换包 1–5 件且属本人；目标商品 ≥1 件且**全部属于同一目标用户**（C-04）； - 目标商品 `swapPreference=ACCEPTING`，否则 `422 SWAP_TARGET_NOT_ACCEPTING`（E-08）； - **双方同交易区域**，否则 `422 TRADE_ZONE_MISMATCH`（E-17 / OOS-07 不做跨区）； - 不能与自己交换（`proposer != target`），否则 `422 SWAP_SELF_PROPOSAL_NOT_ALLOWED`； - **所有涉及商品当前均未被占用**，否则 `409 SWAP_ITEM_OCCUPIED`（E-10）/ `ITEM_ALREADY")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> createSwapProposal(
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CreateSwapProposalRequest body
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(swapService.createSwapProposal(idempotencyKey, body)));
    }

    /**
     * 我参与的交换提议（permission-matrix 5.7 / PF-17）
     *
     * <p>operationId: <code>listSwapProposals</code>
     * <p>权限: USER, RESTRICTED
     *
     */
    @Operation(summary = "我参与的交换提议（permission-matrix 5.7 / PF-17）", operationId = "listSwapProposals", description = "**必须本人**。返回当前用户**作为发起方或目标方**参与的交换提议， 每条带 `role`（`PROPOSER` / `TARGET`）。非参与方不可见。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/swap-proposals")
    public ResponseEntity<ApiResponse<PageResult<SwapProposalSummary>>> listSwapProposals(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "排序表达式，格式 `字段,方向`（方向 `asc` | `desc`），可重复传入多级排序。 **迭代一允许的排序字段是白名单**（OOS-09 不做推荐/个性化排序）： 商品列表：`published_at`（默认 desc）、`estimated_price`； 订单列表：`created_at`（默认 desc）； 交换提议：`created_at`（默认 desc）； 通知：`created_at`（默认 desc）。 传入白名单外字段返回 `COMMON_INVALID_SORT_FIELD`。") @RequestParam(value = "sort", required = false) List<String> sort,
            @Parameter(description = "按我的角色过滤") @RequestParam(value = "role", required = false) String role,
            @Parameter(description = "按提议状态过滤，可重复传入") @RequestParam(value = "status", required = false) List<SwapProposalStatus> status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.listSwapProposals(page,size,sort,role,status)));
    }

    /**
     * 交换提议详情（PF-17）
     *
     * <p>operationId: <code>getSwapProposal</code>
     * <p>权限: USER, ADMIN
     *
     */
    @Operation(summary = "交换提议详情（PF-17）", operationId = "getSwapProposal", description = "**仅提议双方**可查看（`proposer` 或 `target`）；非参与方返回 `403 AUTH_NOT_RESOURCE_OWNER`。 管理员在治理场景可只读查看。  - 响应含 `canAccept` / `canReject` / `canCounter` / `canCancel` / `myConfirmed`，   由服务端按**当前用户角色 + 提议状态**计算，前端**不得自行推断**。 - `canCounter` 仅当「我是目标方 + `PENDING` + `counterDepth=0`」为 true（R-28 仅一轮）。 - `remainingSeconds` 由服务端计算（R-19）。 - `swapCode` **仅双方可见**，且仅在 `ACCEPTED` 后存在（R-26）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/swap-proposals/{proposalId}")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> getSwapProposal(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.getSwapProposal(proposalId)));
    }

    /**
     * 整包接受交换提议（FR-405）
     *
     * <p>operationId: <code>acceptSwapProposal</code>
     * <p>权限: USER
     * <p>状态转换: 'PENDING → ACCEPTED（S-02）；生成 swap_code（C-05）'
     * <p>并发: '条件更新 WHERE status = PENDING（受影响行数=0 → 409）'
     * <p>幂等: required
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "整包接受交换提议（FR-405）", operationId = "acceptSwapProposal", description = "**仅目标方**（`target`）。`PENDING` → `ACCEPTED`（S-02），保持占用，并**生成一次性交换码**。  - **整包接受（all-or-nothing）**：接受即接受提议中**全部**目标商品，   同时占用交换包内**全部**商品。**不支持部分接受**（OOS-04 / R-22）：   **本接口不接受任何物品子集参数**；若请求体携带物品列表将被忽略或返回   `422 SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED`。 - **幂等（IDEM-B）**：重复接受**明确失败**，返回   `409 SWAP_PROPOSAL_ALREADY_ACCEPTED` + 当前状态；**不重复生成交换码**（E-09）。 - **并发**：「接受 vs 拒绝 vs 过期」竞争时**只允许一方成功**   （R-18 条件更新 `WH")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> acceptSwapProposal(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.acceptSwapProposal(proposalId,idempotencyKey)));
    }

    /**
     * 拒绝交换提议（FR-406）
     *
     * <p>operationId: <code>rejectSwapProposal</code>
     * <p>权限: USER
     * <p>状态转换: 'PENDING → REJECTED（S-03）；双方商品 SWAPPING → IN_SALE（I-12）'
     * <p>并发: '条件更新 WHERE status = PENDING'
     * <p>幂等: optional
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "拒绝交换提议（FR-406）", operationId = "rejectSwapProposal", description = "**仅目标方**。`PENDING` → `REJECTED`（S-03），并**释放双方全部占用**（商品 → `IN_SALE`）。  - **幂等（IDEM-A）**：重复拒绝返回成功态，**不重复释放**。 - 状态非 `PENDING` 返回 `409 SWAP_PROPOSAL_ALREADY_FINALIZED`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/reject")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> rejectSwapProposal(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RejectSwapProposalRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.rejectSwapProposal(proposalId,idempotencyKey,body)));
    }

    /**
     * 发起反提案（FR-407 / R-28）
     *
     * <p>operationId: <code>counterSwapProposal</code>
     * <p>权限: USER
     * <p>状态转换: 'PENDING → COUNTERED（S-04）；新建一条 PENDING 提议（counterDepth=1）'
     * <p>幂等: required
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "发起反提案（FR-407 / R-28）", operationId = "counterSwapProposal", description = "**仅目标方**，且**仅允许一轮反提案**（假设 A-10）。  行为（S-04）：原提议 → `COUNTERED`（释放原占用）， 并创建**一条新提议**（`proposer` = 原目标方，`target` = 原发起方，`counterDepth=1`）。  - 前置：状态 `PENDING`、`counterDepth=0`、此前未反提案过； - **不允许「反提案的反提案」**：对 `counterDepth=1` 的提议调用返回   `409 SWAP_COUNTER_NOT_ALLOWED`； - 反提案的交换包同样受 1–5 件与占用互斥约束（R-21 / R-23）； - **幂等（IDEM-B）**：重复反提案失败，仅允许一轮。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/counter")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> counterSwapProposal(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CounterSwapProposalRequest body
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(swapService.counterSwapProposal(proposalId,idempotencyKey,body)));
    }

    /**
     * 取消进行中的交换提议（permission-matrix 5.16）
     *
     * <p>operationId: <code>cancelSwapProposal</code>
     * <p>权限: USER
     * <p>状态转换: 'PENDING → CANCELLED（S-05）；双方商品 SWAPPING → IN_SALE'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "取消进行中的交换提议（permission-matrix 5.16）", operationId = "cancelSwapProposal", description = "**仅发起方**。`PENDING` → `CANCELLED`（S-05），并**释放全部占用**。  - **幂等（IDEM-A）**：重复取消返回成功态。 - 目标方取消返回 `403 AUTH_NOT_RESOURCE_OWNER`（应使用 reject）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/cancel")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> cancelSwapProposal(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.cancelSwapProposal(proposalId,idempotencyKey)));
    }

    /**
     * 查看交换码（FR-409）
     *
     * <p>operationId: <code>getSwapCode</code>
     * <p>权限: USER
     *
     */
    @Operation(summary = "查看交换码（FR-409）", operationId = "getSwapCode", description = "**仅提议双方**可见（R-26）。提议被整包接受后生成，**一次性核销**。  - 仅 `status=ACCEPTED`（或其后）时可获取，否则 `409 SWAP_CODE_NOT_ISSUED`； - **交换码禁止出现在任何公开列表接口中**； - 响应携带安全提示文案。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/swap-proposals/{proposalId}/swap-code")
    public ResponseEntity<ApiResponse<SwapCode>> getSwapCode(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.getSwapCode(proposalId)));
    }

    /**
     * 校验并核销交换码（FR-409 / R-26）
     *
     * <p>operationId: <code>verifySwapCode</code>
     * <p>权限: USER
     * <p>状态转换: 'swap_code ISSUED → VERIFIED（S-08）；提议状态保持 ACCEPTED'
     * <p>并发: '条件更新 WHERE status = ISSUED（受影响行数=0 → 409）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "校验并核销交换码（FR-409 / R-26）", operationId = "verifySwapCode", description = "**仅提议双方**（线下见面时由任一方核销）。**一次性**：成功核销后交换码失效， 再次核销返回 `409 SWAP_CODE_ALREADY_VERIFIED`（E-12）。  - 前置：提议 `ACCEPTED`，否则 `409 SWAP_CODE_NOT_ISSUED`； - 交换码错误返回 `422 SWAP_CODE_INVALID`； - **并发**：`version` 条件更新保证**只允许一次成功核销**（R-18）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/swap-code/verify")
    public ResponseEntity<ApiResponse<SwapCode>> verifySwapCode(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody VerifySwapCodeRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.verifySwapCode(proposalId,idempotencyKey,body)));
    }

    /**
     * 双方确认（交付 / 收到）（FR-410 / R-27）
     *
     * <p>operationId: <code>confirmSwap</code>
     * <p>权限: USER
     * <p>状态转换: 'ACCEPTED → ACCEPTED（记录 1/2）→ COMPLETED（S-09 / S-10）；商品 SWAPPING → SOLD（I-13）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "双方确认（交付 / 收到）（FR-410 / R-27）", operationId = "confirmSwap", description = "**仅提议双方**。交换码核销后，**双方各确认一次**；**两次齐备**才判定交换完成： 全部商品 → `SOLD`，提议 → `COMPLETED`（S-10）。  - 前置：交换码**已核销**，否则 `409 SWAP_CODE_NOT_VERIFIED`； - **幂等（IDEM-A）**：同一方重复确认返回成功态，**不重复计数**； - **双方齐备的清算也是幂等的**：重复触发不重复把商品置为 `SOLD`； - 【待确认 Q-15】：一方长期不确认时如何处理——本迭代**不做**自动完成/自动取消   （假设 A-10 相关），由管理员介入。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/confirm")
    public ResponseEntity<ApiResponse<SwapProposalDetail>> confirmSwap(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ConfirmSwapRequest body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(swapService.confirmSwap(proposalId,idempotencyKey,body)));
    }

    /**
     * 交换互评（FR-312 适用于交换）
     *
     * <p>operationId: <code>createSwapReview</code>
     * <p>权限: USER
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     */
    @Operation(summary = "交换互评（FR-312 适用于交换）", operationId = "createSwapReview", description = "**仅提议双方**。交换 `COMPLETED` 后可互评，双方各一次（R-14）。 前置不满足时与订单评价同样的 409 语义（未完成 / 窗口关闭 / 已评价）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/swap-proposals/{proposalId}/reviews")
    public ResponseEntity<ApiResponse<Review>> createSwapReview(
            @Parameter(description = "") @PathVariable("proposalId") Long proposalId,
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody CreateReviewRequest body
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(swapService.createSwapReview(proposalId,idempotencyKey,body)));
    }

}
