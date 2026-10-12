package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.UserRole;
import com.jc2001.campustrade.dto.response.UserStatus;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 当前登录用户信息（FR-103 登录态保持的校验入口）
 *
 * <p>对应契约 {@code components.schemas.CurrentUserResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "CurrentUserResponse", description = "当前登录用户信息（FR-103 登录态保持的校验入口）")
public class CurrentUserResponse {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "username", example = "alice")
    private String username;

    @Schema(description = "email", example = "alice@example.com")
    private String email;

    @NotNull
    @Schema(description = "nickname", example = "爱丽丝")
    private String nickname;

    @Schema(description = "avatarUrl", example = "/static/avatars/alice.png")
    private String avatarUrl;

    @NotNull
    @Schema(description = "role")
    private UserRole role;

    @NotNull
    @Schema(description = "status")
    private UserStatus status;

    /**
     * 处罚解封时间。`status=RESTRICTED` 时必有值（E-03 前端提示用）
     */
    @Schema(description = "处罚解封时间。`status=RESTRICTED` 时必有值（E-03 前端提示用）")
    private Instant restrictedUntil;

    /**
     * 已勾选的常用交易区域（1–5 个，FR-107）
     */
    @NotNull
    @Schema(description = "已勾选的常用交易区域（1–5 个，FR-107）")
    private List<TradeZone> tradeZones;

    /**
     * 乐观锁版本号（R-18）
     */
    @Schema(description = "乐观锁版本号（R-18）")
    private Integer version;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CurrentUserResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getRestrictedUntil() {
        return restrictedUntil;
    }

    public void setRestrictedUntil(Instant restrictedUntil) {
        this.restrictedUntil = restrictedUntil;
    }

    public List<TradeZone> getTradeZones() {
        return tradeZones;
    }

    public void setTradeZones(List<TradeZone> tradeZones) {
        this.tradeZones = tradeZones;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CurrentUserResponse{"
                + "id=" + id + ", "
                + "username=" + username + ", "
                + "email=" + email + ", "
                + "nickname=" + nickname + ", "
                + "avatarUrl=" + avatarUrl + ", "
                + "role=" + role + ", "
                + "status=" + status + ", "
                + "restrictedUntil=" + restrictedUntil + ", "
                + "tradeZones=" + tradeZones + ", "
                + "version=" + version + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
