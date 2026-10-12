package com.jc2001.campustrade.dto.response;

import java.time.Instant;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 审计日志（permission-matrix 6.10 / R-34）。追加写，不可修改
 *
 * <p>对应契约 {@code components.schemas.AuditLog}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "AuditLog", description = "审计日志（permission-matrix 6.10 / R-34）。追加写，不可修改")
public class AuditLog {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 操作者；`null` 表示系统定时任务
     */
    @Schema(description = "操作者；`null` 表示系统定时任务")
    private Long actorId;

    @Schema(description = "actorNickname")
    private String actorNickname;

    @Schema(description = "actorRole", example = "ADMIN")
    private String actorRole;

    /**
     * 动作标识，如 `ITEM_BAN` / `USER_BAN` / `ITEM_RESTORE` / `APPEAL_RESOLVE` / `REPORT_HANDLE`
     */
    @NotNull
    @Schema(description = "动作标识，如 `ITEM_BAN` / `USER_BAN` / `ITEM_RESTORE` / `APPEAL_RESOLVE` / `REPORT_HANDLE`", example = "ITEM_BAN")
    private String action;

    @NotNull
    @Schema(description = "targetType", example = "ITEM")
    private String targetType;

    @NotNull
    @Schema(description = "targetId")
    private Long targetId;

    /**
     * 治理动作原因（治理类动作必填，R-34）
     */
    @Schema(description = "治理动作原因（治理类动作必填，R-34）", example = "商品信息违反平台规定")
    private String reason;

    @Schema(description = "beforeState")
    private Map<String, Object> beforeState;

    @Schema(description = "afterState")
    private Map<String, Object> afterState;

    @NotNull
    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorNickname() {
        return actorNickname;
    }

    public void setActorNickname(String actorNickname) {
        this.actorNickname = actorNickname;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, Object> getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(Map<String, Object> beforeState) {
        this.beforeState = beforeState;
    }

    public Map<String, Object> getAfterState() {
        return afterState;
    }

    public void setAfterState(Map<String, Object> afterState) {
        this.afterState = afterState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuditLog{"
                + "id=" + id + ", "
                + "actorId=" + actorId + ", "
                + "actorNickname=" + actorNickname + ", "
                + "actorRole=" + actorRole + ", "
                + "action=" + action + ", "
                + "targetType=" + targetType + ", "
                + "targetId=" + targetId + ", "
                + "reason=" + reason + ", "
                + "beforeState=" + beforeState + ", "
                + "afterState=" + afterState + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
