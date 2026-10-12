package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 互评（FR-312 / R-14）。前置条件：订单 `COMPLETED` 且在 7 天互评窗口内（假设 A-07），
否则 409 `REVIEW_WINDOW_CLOSED` / `REVIEW_TRANSACTION_NOT_COMPLETED`。
同一订单同一方**只能评一次**（IDEM-B），重复提交返回 409 `REVIEW_ALREADY_SUBMITTED`。
迭代一**不做**评价标签、不做信用徽章、评价不参与排序（OOS-09）。

 *
 * <p>对应契约 {@code components.schemas.CreateReviewRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CreateReviewRequest", description = "互评（FR-312 / R-14）。前置条件：订单 `COMPLETED` 且在 7 天互评窗口内（假设 A-07）， 否则 409 `REVIEW_WINDOW_CLOSED` / `REVIEW_TRANSACTION_NOT_COMPLETED`。 同一订单同一方**只能评一次**（IDEM-B），重复提交返回 409 `REVIEW_ALREADY_SUBMITTED`。 迭代一**不做**评价标签、不做信用徽章、评价不参与排序（OOS-09）。")
public class CreateReviewRequest {

    /**
     * 评分 1–5
     */
    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "评分 1–5")
    private Integer rating;

    /**
     * 评价内容（可选）
     */
    @Size(max = 500)
    @Schema(description = "评价内容（可选）", example = "物品与描述一致，交付很守时。")
    private String content;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CreateReviewRequest() {
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

    @Override
    public String toString() {
        return "CreateReviewRequest{"
                + "rating=" + rating + ", "
                + "content=" + content + ", "
                + '}';
    }
}
