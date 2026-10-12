package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.ReviewRole;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 评价记录（R-14）
 *
 * <p>对应契约 {@code components.schemas.Review}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "Review", description = "评价记录（R-14）")
public class Review {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 与 `swapProposalId` 二选一
     */
    @Schema(description = "与 `swapProposalId` 二选一")
    private Long orderId;

    @Schema(description = "swapProposalId")
    private Long swapProposalId;

    @NotNull
    @Schema(description = "reviewer")
    private UserProfile reviewer;

    @NotNull
    @Schema(description = "reviewee")
    private UserProfile reviewee;

    @NotNull
    @Schema(description = "reviewerRole")
    private ReviewRole reviewerRole;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "rating")
    private Integer rating;

    @Schema(description = "content", example = "物品与描述一致。")
    private String content;

    @NotNull
    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public Review() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getSwapProposalId() {
        return swapProposalId;
    }

    public void setSwapProposalId(Long swapProposalId) {
        this.swapProposalId = swapProposalId;
    }

    public UserProfile getReviewer() {
        return reviewer;
    }

    public void setReviewer(UserProfile reviewer) {
        this.reviewer = reviewer;
    }

    public UserProfile getReviewee() {
        return reviewee;
    }

    public void setReviewee(UserProfile reviewee) {
        this.reviewee = reviewee;
    }

    public ReviewRole getReviewerRole() {
        return reviewerRole;
    }

    public void setReviewerRole(ReviewRole reviewerRole) {
        this.reviewerRole = reviewerRole;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Review{"
                + "id=" + id + ", "
                + "orderId=" + orderId + ", "
                + "swapProposalId=" + swapProposalId + ", "
                + "reviewer=" + reviewer + ", "
                + "reviewee=" + reviewee + ", "
                + "reviewerRole=" + reviewerRole + ", "
                + "rating=" + rating + ", "
                + "content=" + content + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
