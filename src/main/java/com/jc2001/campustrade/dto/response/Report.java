package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.ReportReason;
import com.jc2001.campustrade.dto.response.ReportStatus;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 举报记录（FR-211）
 *
 * <p>对应契约 {@code components.schemas.Report}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Report", description = "举报记录（FR-211）")
public class Report {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "itemId")
    private Long itemId;

    @Schema(description = "itemTitle", example = "【示例】违规下架商品（演示用）")
    private String itemTitle;

    @Schema(description = "reporter")
    private UserProfile reporter;

    @NotNull
    @Schema(description = "reason")
    private ReportReason reason;

    @Schema(description = "detail")
    private String detail;

    @NotNull
    @Schema(description = "status")
    private ReportStatus status;

    @Schema(description = "handledBy")
    private Long handledBy;

    @Schema(description = "handledAt")
    private Instant handledAt;

    @Schema(description = "handleNote")
    private String handleNote;

    @NotNull
    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Report() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public UserProfile getReporter() {
        return reporter;
    }

    public void setReporter(UserProfile reporter) {
        this.reporter = reporter;
    }

    public ReportReason getReason() {
        return reason;
    }

    public void setReason(ReportReason reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(Instant handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Report{"
                + "id=" + id + ", "
                + "itemId=" + itemId + ", "
                + "itemTitle=" + itemTitle + ", "
                + "reporter=" + reporter + ", "
                + "reason=" + reason + ", "
                + "detail=" + detail + ", "
                + "status=" + status + ", "
                + "handledBy=" + handledBy + ", "
                + "handledAt=" + handledAt + ", "
                + "handleNote=" + handleNote + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
