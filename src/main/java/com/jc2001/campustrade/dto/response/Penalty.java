package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.AdminUserSummary;
import com.jc2001.campustrade.dto.response.PenaltyLiftSource;
import com.jc2001.campustrade.dto.response.PenaltyStatus;
import com.jc2001.campustrade.dto.response.PenaltyType;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 处罚记录（FR-504 / R-32）。**永不物理删除**，`status` 只反映当前态，历史结论保留。

 *
 * <p>对应契约 {@code components.schemas.Penalty}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Penalty", description = "处罚记录（FR-504 / R-32）。**永不物理删除**，`status` 只反映当前态，历史结论保留。")
public class Penalty {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "targetUser")
    private AdminUserSummary targetUser;

    /**
     * 操作管理员 ID
     */
    @Schema(description = "操作管理员 ID")
    private Long operatorId;

    @Schema(description = "operatorNickname", example = "平台管理员（开发占位）")
    private String operatorNickname;

    @NotNull
    @Schema(description = "penaltyType")
    private PenaltyType penaltyType;

    /**
     * `ITEM_TAKEDOWN` 时关联的商品
     */
    @Schema(description = "`ITEM_TAKEDOWN` 时关联的商品")
    private Long itemId;

    /**
     * "**治理动作原因，必填**（R-34）"
     */
    @NotNull
    @Schema(description = "\"**治理动作原因，必填**（R-34）\"", example = "多次发布违规商品")
    private String reason;

    @NotNull
    @Schema(description = "status")
    private PenaltyStatus status;

    @NotNull
    @Schema(description = "startedAt")
    private Instant startedAt;

    /**
     * 到期时间。`BAN_3D` 固定为 `startedAt + 72h`（R-30 / A-03）
     */
    @NotNull
    @Schema(description = "到期时间。`BAN_3D` 固定为 `startedAt + 72h`（R-30 / A-03）")
    private Instant endsAt;

    /**
     * 距解封剩余秒数（服务端计算，R-19）
     */
    @Schema(description = "距解封剩余秒数（服务端计算，R-19）")
    private Long remainingSeconds;

    @Schema(description = "liftedAt")
    private Instant liftedAt;

    @Schema(description = "liftSource")
    private PenaltyLiftSource liftSource;

    @Schema(description = "liftReason")
    private String liftReason;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Penalty() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AdminUserSummary getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(AdminUserSummary targetUser) {
        this.targetUser = targetUser;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorNickname() {
        return operatorNickname;
    }

    public void setOperatorNickname(String operatorNickname) {
        this.operatorNickname = operatorNickname;
    }

    public PenaltyType getPenaltyType() {
        return penaltyType;
    }

    public void setPenaltyType(PenaltyType penaltyType) {
        this.penaltyType = penaltyType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public PenaltyStatus getStatus() {
        return status;
    }

    public void setStatus(PenaltyStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Instant getLiftedAt() {
        return liftedAt;
    }

    public void setLiftedAt(Instant liftedAt) {
        this.liftedAt = liftedAt;
    }

    public PenaltyLiftSource getLiftSource() {
        return liftSource;
    }

    public void setLiftSource(PenaltyLiftSource liftSource) {
        this.liftSource = liftSource;
    }

    public String getLiftReason() {
        return liftReason;
    }

    public void setLiftReason(String liftReason) {
        this.liftReason = liftReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Penalty{"
                + "id=" + id + ", "
                + "targetUser=" + targetUser + ", "
                + "operatorId=" + operatorId + ", "
                + "operatorNickname=" + operatorNickname + ", "
                + "penaltyType=" + penaltyType + ", "
                + "itemId=" + itemId + ", "
                + "reason=" + reason + ", "
                + "status=" + status + ", "
                + "startedAt=" + startedAt + ", "
                + "endsAt=" + endsAt + ", "
                + "remainingSeconds=" + remainingSeconds + ", "
                + "liftedAt=" + liftedAt + ", "
                + "liftSource=" + liftSource + ", "
                + "liftReason=" + liftReason + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
