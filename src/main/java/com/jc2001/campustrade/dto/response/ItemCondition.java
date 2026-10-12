package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品成色（预设字典，FR-207）
 *
 * <p>对应契约 {@code components.schemas.ItemCondition}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ItemCondition", description = "商品成色（预设字典，FR-207）")
public class ItemCondition {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "code", example = "LIGHTLY_USED")
    private String code;

    @NotNull
    @Schema(description = "name", example = "轻微使用痕迹")
    private String name;

    @Schema(description = "sortOrder")
    private Integer sortOrder;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ItemCondition() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "ItemCondition{"
                + "id=" + id + ", "
                + "code=" + code + ", "
                + "name=" + name + ", "
                + "sortOrder=" + sortOrder + ", "
                + '}';
    }
}
