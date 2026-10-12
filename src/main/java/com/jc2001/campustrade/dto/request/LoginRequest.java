package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 登录请求（FR-102）。凭据错误统一返回 401 `AUTH_INVALID_CREDENTIALS`，不区分账号是否存在
 *
 * <p>对应契约 {@code components.schemas.LoginRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "LoginRequest", description = "登录请求（FR-102）。凭据错误统一返回 401 `AUTH_INVALID_CREDENTIALS`，不区分账号是否存在")
public class LoginRequest {

    @NotNull
    @Size(max = 32)
    @Schema(description = "username", example = "alice")
    private String username;

    @NotNull
    @Size(max = 64)
    @Schema(description = "password", example = "Passw0rd!2026")
    private String password;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{"
                + "username=" + username + ", "
                + "password=" + password + ", "
                + '}';
    }
}
