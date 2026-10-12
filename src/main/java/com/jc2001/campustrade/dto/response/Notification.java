package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.NotificationType;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 站内通知（FR-507）。
 *
 * <p>对应契约 {@code components.schemas.Notification}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Notification", description = "站内通知（FR-507）。")
public class Notification {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "type")
    private NotificationType type;

    @NotNull
    @Schema(description = "title", example = "你的商品有新订单")
    private String title;

    @Schema(description = "body", example = "买家已下单，请等待付款。")
    private String body;

    /**
     * 触发者（系统通知为 null）
     */
    @Schema(description = "触发者（系统通知为 null）")
    private UserProfile actor;

    /**
     * 多态业务引用类型，用于前端跳转
     */
    @Schema(description = "多态业务引用类型，用于前端跳转", example = "ORDER")
    private String refType;

    @Schema(description = "refId")
    private Long refId;

    @NotNull
    @Schema(description = "isRead")
    private Boolean isRead;

    @Schema(description = "readAt")
    private Instant readAt;

    @NotNull
    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Notification() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public UserProfile getActor() {
        return actor;
    }

    public void setActor(UserProfile actor) {
        this.actor = actor;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Notification{"
                + "id=" + id + ", "
                + "type=" + type + ", "
                + "title=" + title + ", "
                + "body=" + body + ", "
                + "actor=" + actor + ", "
                + "refType=" + refType + ", "
                + "refId=" + refId + ", "
                + "isRead=" + isRead + ", "
                + "readAt=" + readAt + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
