package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 未读通知数（用于前端角标）
 *
 * <p>对应契约 {@code components.schemas.UnreadCountResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "UnreadCountResponse", description = "未读通知数（用于前端角标）")
public class UnreadCountResponse {

    @NotNull
    @Schema(description = "unreadCount")
    private Long unreadCount;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UnreadCountResponse() {
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }

    @Override
    public String toString() {
        return "UnreadCountResponse{"
                + "unreadCount=" + unreadCount + ", "
                + '}';
    }
}
