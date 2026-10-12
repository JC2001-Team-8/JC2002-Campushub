package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.AppealStatus;
import com.jc2001.campustrade.dto.response.Penalty;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 申诉记录（FR-505 / R-32）。一个处罚最多一条申诉（AP-01），记录永不删除
 *
 * <p>对应契约 {@code components.schemas.Appeal}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Appeal", description = "申诉记录（FR-505 / R-32）。一个处罚最多一条申诉（AP-01），记录永不删除")
public class Appeal {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "penaltyId")
    private Long penaltyId;

    @Schema(description = "penalty")
    private Penalty penalty;

    @NotNull
    @Schema(description = "appellant")
    private UserProfile appellant;

    /**
     * 申诉理由，**必填**（AP-01）
     */
    @NotNull
    @Schema(description = "申诉理由，**必填**（AP-01）", example = "该商品已主动下架，请复核处罚")
    private String reason;

    @NotNull
    @Schema(description = "status")
    private AppealStatus status;

    @Schema(description = "handlerId")
    private Long handlerId;

    @Schema(description = "handlerNickname")
    private String handlerNickname;

    /**
     * 处理意见，**处理时必填**（R-34）
     */
    @Schema(description = "处理意见，**处理时必填**（R-34）")
    private String resolutionNote;

    @Schema(description = "resolvedAt")
    private Instant resolvedAt;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Appeal() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPenaltyId() {
        return penaltyId;
    }

    public void setPenaltyId(Long penaltyId) {
        this.penaltyId = penaltyId;
    }

    public Penalty getPenalty() {
        return penalty;
    }

    public void setPenalty(Penalty penalty) {
        this.penalty = penalty;
    }

    public UserProfile getAppellant() {
        return appellant;
    }

    public void setAppellant(UserProfile appellant) {
        this.appellant = appellant;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AppealStatus getStatus() {
        return status;
    }

    public void setStatus(AppealStatus status) {
        this.status = status;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerNickname() {
        return handlerNickname;
    }

    public void setHandlerNickname(String handlerNickname) {
        this.handlerNickname = handlerNickname;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Appeal{"
                + "id=" + id + ", "
                + "penaltyId=" + penaltyId + ", "
                + "penalty=" + penalty + ", "
                + "appellant=" + appellant + ", "
                + "reason=" + reason + ", "
                + "status=" + status + ", "
                + "handlerId=" + handlerId + ", "
                + "handlerNickname=" + handlerNickname + ", "
                + "resolutionNote=" + resolutionNote + ", "
                + "resolvedAt=" + resolvedAt + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
