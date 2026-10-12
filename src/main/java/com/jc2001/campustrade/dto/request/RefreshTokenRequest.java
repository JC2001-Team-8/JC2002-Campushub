package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 刷新令牌请求。【待确认 Q-07】当前假设采用**固定 7 天单令牌**（A-06），
本接口为预留能力，未启用时返回 `501`（见 api-open-questions.md AQ-02）。

 *
 * <p>对应契约 {@code components.schemas.RefreshTokenRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "RefreshTokenRequest", description = "刷新令牌请求。【待确认 Q-07】当前假设采用**固定 7 天单令牌**（A-06）， 本接口为预留能力，未启用时返回 `501`（见 api-open-questions.md AQ-02）。")
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    @NotNull
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
    private String refreshToken;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{"
                + "refreshToken=" + refreshToken + ", "
                + '}';
    }
}
