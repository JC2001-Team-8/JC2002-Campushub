package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 编辑交换包（permission-matrix 5.3）。
仅 `DRAFT` / `PACKED` / `RELEASED` 且**未被进行中提议引用**时可编辑（SB-04）；
否则 409 `SWAP_BUNDLE_LOCKED`。

 *
 * <p>对应契约 {@code components.schemas.UpdateSwapBundleRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UpdateSwapBundleRequest", description = "编辑交换包（permission-matrix 5.3）。 仅 `DRAFT` / `PACKED` / `RELEASED` 且**未被进行中提议引用**时可编辑（SB-04）； 否则 409 `SWAP_BUNDLE_LOCKED`。")
public class UpdateSwapBundleRequest {

    /**
     * 物品 ID 全量替换（1–5 件）
     */
    @Size(min = 1, max = 5)
    @Schema(description = "物品 ID 全量替换（1–5 件）")
    private List<Long> itemIds;

    @Size(max = 60)
    @Schema(description = "title")
    private String title;

    @Schema(description = "version")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UpdateSwapBundleRequest() {
    }

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "UpdateSwapBundleRequest{"
                + "itemIds=" + itemIds + ", "
                + "title=" + title + ", "
                + "version=" + version + ", "
                + '}';
    }
}
