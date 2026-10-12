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
import com.jc2001.campustrade.dto.request.ChangePasswordRequest;
import com.jc2001.campustrade.dto.request.LoginRequest;
import com.jc2001.campustrade.dto.request.RefreshTokenRequest;
import com.jc2001.campustrade.dto.request.RegisterRequest;
import com.jc2001.campustrade.dto.response.AuthTokenResponse;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 认证与账户（FR-101 ~ FR-104）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "01-认证与账户", description = "认证与账户（FR-101 ~ FR-104）")
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    /**
     * 用户注册（FR-101）
     *
     * <p>operationId: <code>registerUser</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "用户注册（FR-101）", operationId = "registerUser", description = "使用「用户名 + 密码 + 昵称」注册。用户名唯一且**大小写不敏感**（数据库 citext）。  - **无需登录**。注册成功后**不自动签发令牌**，需调用 `/auth/login`。 - 用户名重复返回 `409 AUTH_USERNAME_TAKEN`；密码强度不足返回 `400 VALIDATION_INVALID_PARAMETER`。 - 【待确认 Q-06】邮箱是否必填与是否验证：当前假设**可选且不验证**。 - 【待确认 A-09】昵称 1–20 字符、**不唯一**（假设 A-04）。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> registerUser(
            @RequestBody RegisterRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement registerUser");
    }

    /**
     * 用户登录（FR-102）
     *
     * <p>operationId: <code>login</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "用户登录（FR-102）", operationId = "login", description = "账号密码登录，返回 Bearer 令牌（有效期 7 天，假设 A-06）。  - **无需登录**（`security: []`）。 - 凭据错误统一返回 `401 AUTH_INVALID_CREDENTIALS`，**不区分账号不存在与密码错误**（防账号枚举）。 - **被处罚用户可正常登录**（permission-matrix §8），登录后 `status=RESTRICTED` 且带 `restrictedUntil`。 - 该接口为敏感接口，触发限流返回 `429 COMMON_RATE_LIMITED`。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @RequestBody LoginRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement login");
    }

    /**
     * 刷新访问令牌（预留，FR-103）
     *
     * <p>operationId: <code>refreshToken</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "刷新访问令牌（预留，FR-103）", operationId = "refreshToken", description = "【待确认 Q-07】是否需要 refresh token 机制。  **当前默认假设（A-06）：仅固定 7 天单令牌，不启用刷新**。 因此本接口在迭代一**未启用**：启用前调用返回 `501 COMMON_NOT_IMPLEMENTED`。 契约先行冻结路径与 DTO，启用时**不改变路径与结构**，仅切换实现开关。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(
            @RequestBody RefreshTokenRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement refreshToken");
    }

    /**
     * 退出登录（FR-104）
     *
     * <p>operationId: <code>logout</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "退出登录（FR-104）", operationId = "logout", description = "使当前令牌失效。**幂等**：重复退出返回成功（终态式幂等，IDEM-A）。  【待确认 Q-07】若最终采用无状态 JWT，服务端无法真正撤销令牌， 此时实现为「客户端丢弃令牌 + 服务端加入黑名单（可选）」——语义在接口层冻结，实现方式待定。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        throw new UnsupportedOperationException("TODO: implement logout");
    }

    /**
     * 获取当前登录用户（FR-103）
     *
     * <p>operationId: <code>getCurrentUser</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "获取当前登录用户（FR-103）", operationId = "getCurrentUser", description = "校验登录态并返回当前用户完整信息（含 `role` / `status` / `restrictedUntil` / 已勾选交易区域）。 前端用于判断：是否登录、是否被处罚、是否管理员。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/auth/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        throw new UnsupportedOperationException("TODO: implement getCurrentUser");
    }

    /**
     * 修改密码（permission-matrix 2.10）
     *
     * <p>operationId: <code>changePassword</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "修改密码（permission-matrix 2.10）", operationId = "changePassword", description = "需校验原密码（`oldPassword`）。**必须本人**。 原密码错误返回 `401 AUTH_INVALID_CREDENTIALS`。 **被处罚用户允许修改密码**（非交易写入，permission-matrix §8）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/auth/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement changePassword");
    }

}
