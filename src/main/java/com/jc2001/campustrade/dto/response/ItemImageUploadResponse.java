package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 图片上传结果（FR-205）。上传与商品绑定分离，避免重复上传
 *
 * <p>对应契约 {@code components.schemas.ItemImageUploadResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ItemImageUploadResponse", description = "图片上传结果（FR-205）。上传与商品绑定分离，避免重复上传")
public class ItemImageUploadResponse {

    /**
     * 可直接用于 `CreateItemRequest.imageUrls` 的访问路径
     */
    @NotNull
    @Schema(description = "可直接用于 `CreateItemRequest.imageUrls` 的访问路径", example = "/static/uploads/2026/10/9f8e7d6c.png")
    private String imageUrl;

    @NotNull
    @Schema(description = "contentType", example = "image/png")
    private String contentType;

    @NotNull
    @Schema(description = "fileSize")
    private Integer fileSize;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ItemImageUploadResponse() {
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
        return "ItemImageUploadResponse{"
                + "imageUrl=" + imageUrl + ", "
                + "contentType=" + contentType + ", "
                + "fileSize=" + fileSize + ", "
                + '}';
    }
}
