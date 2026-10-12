package com.jc2001.campustrade.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 用户公开资料（FR-108）。**不含任何联系方式、住址、定位字段**
 *
 * <p>对应契约 {@code components.schemas.UserProfile}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "UserProfile", description = "用户公开资料（FR-108）。**不含任何联系方式、住址、定位字段**")
public class UserProfile {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "nickname", example = "爱丽丝")
    private String nickname;

    @Schema(description = "avatarUrl", example = "/static/avatars/alice.png")
    private String avatarUrl;

    /**
     * 在售商品数（由 items 聚合，非冗余字段）
     */
    @Schema(description = "在售商品数（由 items 聚合，非冗余字段）")
    private Long itemsInSaleCount;

    /**
     * 收到的评价平均分（1–5，保留 1 位小数）。迭代一仅展示，不参与排序（OOS-09）
     */
    @Schema(description = "收到的评价平均分（1–5，保留 1 位小数）。迭代一仅展示，不参与排序（OOS-09）", example = "4.8")
    private String ratingAverage;

    @Schema(description = "ratingCount")
    private Long ratingCount;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UserProfile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getItemsInSaleCount() {
        return itemsInSaleCount;
    }

    public void setItemsInSaleCount(Long itemsInSaleCount) {
        this.itemsInSaleCount = itemsInSaleCount;
    }

    public String getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(String ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public Long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserProfile{"
                + "id=" + id + ", "
                + "nickname=" + nickname + ", "
                + "avatarUrl=" + avatarUrl + ", "
                + "itemsInSaleCount=" + itemsInSaleCount + ", "
                + "ratingAverage=" + ratingAverage + ", "
                + "ratingCount=" + ratingCount + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
