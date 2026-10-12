package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.UserRole;
import com.jc2001.campustrade.dto.response.UserStatus;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 管理员视角的用户摘要（FR-501）
 *
 * <p>对应契约 {@code components.schemas.AdminUserSummary}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "AdminUserSummary", description = "管理员视角的用户摘要（FR-501）")
public class AdminUserSummary {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "username", example = "carol")
    private String username;

    @Schema(description = "email")
    private String email;

    @NotNull
    @Schema(description = "nickname", example = "卡罗尔（示例用户）")
    private String nickname;

    @NotNull
    @Schema(description = "role")
    private UserRole role;

    @NotNull
    @Schema(description = "status")
    private UserStatus status;

    @Schema(description = "restrictedUntil")
    private Instant restrictedUntil;

    @NotNull
    @Schema(description = "itemsInSaleCount")
    private Long itemsInSaleCount;

    /**
     * 历史处罚总数（记录永不删除，R-32）
     */
    @NotNull
    @Schema(description = "历史处罚总数（记录永不删除，R-32）")
    private Long penaltyCount;

    /**
     * 当前生效中/申诉中的处罚 ID（无则为 null）
     */
    @Schema(description = "当前生效中/申诉中的处罚 ID（无则为 null）")
    private Long activePenaltyId;

    @Schema(description = "registeredAt")
    private Instant registeredAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public AdminUserSummary() {
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

    public Long getItemsInSaleCount() {
        return itemsInSaleCount;
    }

    public void setItemsInSaleCount(Long itemsInSaleCount) {
        this.itemsInSaleCount = itemsInSaleCount;
    }

    public Long getPenaltyCount() {
        return penaltyCount;
    }

    public void setPenaltyCount(Long penaltyCount) {
        this.penaltyCount = penaltyCount;
    }

    public Long getActivePenaltyId() {
        return activePenaltyId;
    }

    public void setActivePenaltyId(Long activePenaltyId) {
        this.activePenaltyId = activePenaltyId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "AdminUserSummary{"
                + "id=" + id + ", "
                + "username=" + username + ", "
                + "email=" + email + ", "
                + "nickname=" + nickname + ", "
                + "role=" + role + ", "
                + "status=" + status + ", "
                + "restrictedUntil=" + restrictedUntil + ", "
                + "itemsInSaleCount=" + itemsInSaleCount + ", "
                + "penaltyCount=" + penaltyCount + ", "
                + "activePenaltyId=" + activePenaltyId + ", "
                + "registeredAt=" + registeredAt + ", "
                + '}';
    }
}
