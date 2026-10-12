package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.Review;
import com.jc2001.campustrade.dto.response.ReviewStatus;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 某个订单/交换的互评状态与我的可评性（state-machines.md §8）
 *
 * <p>对应契约 {@code components.schemas.ReviewStatusResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ReviewStatusResponse", description = "某个订单/交换的互评状态与我的可评性（state-machines.md §8）")
public class ReviewStatusResponse {

    @NotNull
    @Schema(description = "reviewStatus")
    private ReviewStatus reviewStatus;

    @NotNull
    @Schema(description = "canReview")
    private Boolean canReview;

    /**
     * 我已提交的评价（未提交为 null）
     */
    @Schema(description = "我已提交的评价（未提交为 null）")
    private Review myReview;

    /**
     * 对方已提交的评价。**仅在双方均已评价或窗口关闭后可见**（避免互相偷看）
     */
    @Schema(description = "对方已提交的评价。**仅在双方均已评价或窗口关闭后可见**（避免互相偷看）")
    private Review counterpartyReview;

    @NotNull
    @Schema(description = "deadline")
    private Instant deadline;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ReviewStatusResponse() {
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Boolean getCanReview() {
        return canReview;
    }

    public void setCanReview(Boolean canReview) {
        this.canReview = canReview;
    }

    public Review getMyReview() {
        return myReview;
    }

    public void setMyReview(Review myReview) {
        this.myReview = myReview;
    }

    public Review getCounterpartyReview() {
        return counterpartyReview;
    }

    public void setCounterpartyReview(Review counterpartyReview) {
        this.counterpartyReview = counterpartyReview;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return "ReviewStatusResponse{"
                + "reviewStatus=" + reviewStatus + ", "
                + "canReview=" + canReview + ", "
                + "myReview=" + myReview + ", "
                + "counterpartyReview=" + counterpartyReview + ", "
                + "deadline=" + deadline + ", "
                + '}';
    }
}
