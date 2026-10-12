package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 登录/刷新成功返回的令牌
 *
 * <p>对应契约 {@code components.schemas.AuthTokenResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "AuthTokenResponse", description = "登录/刷新成功返回的令牌")
public class AuthTokenResponse {

    /**
     * JWT 访问令牌
     */
    @NotNull
    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9.access")
    private String accessToken;

    @NotNull
    @Schema(description = "tokenType", example = "Bearer")
    private String tokenType;

    /**
     * 剩余有效秒数（7 天 = 604800，假设 A-06）
     */
    @NotNull
    @Schema(description = "剩余有效秒数（7 天 = 604800，假设 A-06）")
    private Long expiresIn;

    /**
     * 令牌到期绝对时间（服务端时间为准，R-19）
     */
    @NotNull
    @Schema(description = "令牌到期绝对时间（服务端时间为准，R-19）")
    private Instant expiresAt;

    /**
     * 刷新令牌（仅当启用刷新机制时返回；【待确认 Q-07】）
     */
    @Schema(description = "刷新令牌（仅当启用刷新机制时返回；【待确认 Q-07】）")
    private String refreshToken;

    @Schema(description = "user")
    private UserProfile user;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public AuthTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "AuthTokenResponse{"
                + "accessToken=" + accessToken + ", "
                + "tokenType=" + tokenType + ", "
                + "expiresIn=" + expiresIn + ", "
                + "expiresAt=" + expiresAt + ", "
                + "refreshToken=" + refreshToken + ", "
                + "user=" + user + ", "
                + '}';
    }
}
