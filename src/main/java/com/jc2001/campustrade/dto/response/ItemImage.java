package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品图片（FR-205）。`sortOrder=0` 为封面（首图）
 *
 * <p>对应契约 {@code components.schemas.ItemImage}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ItemImage", description = "商品图片（FR-205）。`sortOrder=0` 为封面（首图）")
public class ItemImage {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 图片访问路径（本地存储，OOS-18 不使用对象存储）
     */
    @NotNull
    @Size(max = 255)
    @Schema(description = "图片访问路径（本地存储，OOS-18 不使用对象存储）", example = "/static/uploads/2026/10/abc.png")
    private String imageUrl;

    /**
     * 排序值，**0 表示封面**；同一商品内唯一
     */
    @NotNull
    @Schema(description = "排序值，**0 表示封面**；同一商品内唯一")
    private Integer sortOrder;

    /**
     * MIME 类型，限制 jpg/png/webp（假设 A-05）
     */
    @Schema(description = "MIME 类型，限制 jpg/png/webp（假设 A-05）", example = "image/png")
    private String contentType;

    /**
     * 文件字节数，单张 ≤ 5MB（假设 A-05）
     */
    @Schema(description = "文件字节数，单张 ≤ 5MB（假设 A-05）")
    private Integer fileSize;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ItemImage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "ItemImage{"
                + "id=" + id + ", "
                + "imageUrl=" + imageUrl + ", "
                + "sortOrder=" + sortOrder + ", "
                + "contentType=" + contentType + ", "
                + "fileSize=" + fileSize + ", "
                + '}';
    }
}
