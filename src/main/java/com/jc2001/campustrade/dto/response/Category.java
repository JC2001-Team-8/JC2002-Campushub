package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品分类（预设字典，FR-206）。Q-21 假设仅管理员维护，用户不可自定义
 *
 * <p>对应契约 {@code components.schemas.Category}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Category", description = "商品分类（预设字典，FR-206）。Q-21 假设仅管理员维护，用户不可自定义")
public class Category {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "code", example = "TEXTBOOK")
    private String code;

    @NotNull
    @Schema(description = "name", example = "教材书籍")
    private String name;

    @Schema(description = "sortOrder")
    private Integer sortOrder;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Category() {
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
        return "Category{"
                + "id=" + id + ", "
                + "code=" + code + ", "
                + "name=" + name + ", "
                + "sortOrder=" + sortOrder + ", "
                + '}';
    }
}
