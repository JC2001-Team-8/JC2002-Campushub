package com.jc2001.campustrade.dto.response;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 分页结果
 *
 * <p>对应契约 {@code components.schemas.PageResult}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "PageResult", description = "分页结果")
public class PageResult {

    /**
     * 当前页数据
     */
    @NotNull
    @Schema(description = "当前页数据")
    private List<Object> content;

    /**
     * 当前页码（从 0 开始）
     */
    @NotNull
    @Schema(description = "当前页码（从 0 开始）")
    private Integer page;

    /**
     * 每页条数
     */
    @NotNull
    @Schema(description = "每页条数")
    private Integer size;

    /**
     * 总记录数
     */
    @NotNull
    @Schema(description = "总记录数")
    private Long totalElements;

    /**
     * 总页数
     */
    @NotNull
    @Schema(description = "总页数")
    private Integer totalPages;

    /**
     * 是否首页
     */
    @NotNull
    @Schema(description = "是否首页")
    private Boolean first;

    /**
     * 是否末页
     */
    @NotNull
    @Schema(description = "是否末页")
    private Boolean last;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public PageResult() {
    }

    public List<Object> getContent() {
        return content;
    }

    public void setContent(List<Object> content) {
        this.content = content;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getFirst() {
        return first;
    }

    public void setFirst(Boolean first) {
        this.first = first;
    }

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "PageResult{"
                + "content=" + content + ", "
                + "page=" + page + ", "
                + "size=" + size + ", "
                + "totalElements=" + totalElements + ", "
                + "totalPages=" + totalPages + ", "
                + "first=" + first + ", "
                + "last=" + last + ", "
                + '}';
    }
}
