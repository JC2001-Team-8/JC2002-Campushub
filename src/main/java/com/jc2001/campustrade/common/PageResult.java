package com.jc2001.campustrade.common;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果包络。
 *
 * <p>与接口契约 {@code openapi.yaml} 的 {@code components.schemas.PageResult} 对应。
 *
 * <p><b>契约字段对照（重要）</b>：契约定义的字段名为
 * {@code content} / {@code page} / {@code size} / {@code totalElements} / {@code totalPages} /
 * {@code first} / {@code last}。
 * 本骨架按任务书要求实现 {@code page} / {@code size} / {@code total} / {@code items} 四个核心字段，
 * 并**保留契约别名** {@code totalElements} / {@code totalPages} / {@code content} 以兼容契约。
 *
 * <p><b>待确认</b>：下一步生成 Controller 时需统一为契约字段名（{@code content} /
 * {@code totalElements}），届时移除非契约字段，见 {@code api-open-questions.md}。
 *
 * <p>分页约定（api-contract.md §5 / {@code PageParam}）：{@code page} **从 0 开始**，
 * {@code size} 默认 20、上限 100。
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> {

    /** 当前页码，从 0 开始。 */
    private int page;

    /** 每页条数。 */
    private int size;

    /** 总记录数。 */
    private long total;

    /** 当前页数据。 */
    private List<T> items;

    public PageResult() {
        this.items = Collections.emptyList();
    }

    public PageResult(int page, int size, long total, List<T> items) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.items = items == null ? Collections.emptyList() : items;
    }

    /**
     * 构造分页结果（自动计算总页数）。
     */
    public static <T> PageResult<T> of(int page, int size, long total, List<T> items) {
        return new PageResult<>(page, size, total, items);
    }

    /**
     * 构造空分页结果。
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(page, size, 0L, Collections.emptyList());
    }

    // -----------------------------------------------------------------
    // getter / setter
    // -----------------------------------------------------------------

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items == null ? Collections.emptyList() : items;
    }

    // -----------------------------------------------------------------
    // 契约兼容字段（只读派生）：与 openapi.yaml PageResult 对齐
    // TODO 下一步统一为契约字段名后，可将下列派生 getter 提升为持久字段
    // -----------------------------------------------------------------

    /** 契约字段 {@code totalElements} 的别名。 */
    public long getTotalElements() {
        return total;
    }

    /** 契约字段 {@code content} 的别名。 */
    public List<T> getContent() {
        return items;
    }

    /** 总页数（size<=0 时返回 0，避免除零）。 */
    public int getTotalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / (double) size);
    }

    /** 是否首页。 */
    public boolean isFirst() {
        return page <= 0;
    }

    /** 是否末页。 */
    public boolean isLast() {
        return getTotalPages() == 0 || page >= getTotalPages() - 1;
    }
}
