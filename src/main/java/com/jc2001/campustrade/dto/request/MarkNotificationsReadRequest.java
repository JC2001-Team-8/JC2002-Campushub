package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 标记通知已读。`ids` 为空或不传表示**全部标记已读**
 *
 * <p>对应契约 {@code components.schemas.MarkNotificationsReadRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "MarkNotificationsReadRequest", description = "标记通知已读。`ids` 为空或不传表示**全部标记已读**")
public class MarkNotificationsReadRequest {

    /**
     * 通知 ID 列表；为空表示全部
     */
    @Schema(description = "通知 ID 列表；为空表示全部")
    private List<Long> ids;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public MarkNotificationsReadRequest() {
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    @Override
    public String toString() {
        return "MarkNotificationsReadRequest{"
                + "ids=" + ids + ", "
                + '}';
    }
}
