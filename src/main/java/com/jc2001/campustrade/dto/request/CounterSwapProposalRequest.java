package com.jc2001.campustrade.dto.request;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 发起反提案（FR-407 / R-28）。仅**目标方**、`PENDING` 状态、且**此前未反提案过**可调用
（`counterDepth` 必须为 0）。
原提议 → `COUNTERED`（释放其占用），并创建一条新提议（proposer=原目标方，target=原发起方）。
**不支持「反提案的反提案」**（假设 A-10），违规返回 409 `SWAP_COUNTER_NOT_ALLOWED`。

 *
 * <p>对应契约 {@code components.schemas.CounterSwapProposalRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "CounterSwapProposalRequest", description = "发起反提案（FR-407 / R-28）。仅**目标方**、`PENDING` 状态、且**此前未反提案过**可调用 （`counterDepth` 必须为 0）。 原提议 → `COUNTERED`（释放其占用），并创建一条新提议（proposer=原目标方，target=原发起方）。 **不支持「反提案的反提案」**（假设 A-10），违规返回 409 `SWAP_COUNTER_NOT_ALLOWED`。")
public class CounterSwapProposalRequest {

    /**
     * 我方（原目标方）用于反提案的交换包 ID
     */
    @NotNull
    @Schema(description = "我方（原目标方）用于反提案的交换包 ID")
    private Long targetBundleId;

    /**
     * 反提案中请求对方提供的商品 ID 列表
     */
    @NotNull
    @Size(min = 1)
    @Schema(description = "反提案中请求对方提供的商品 ID 列表")
    private List<Long> targetItemIds;

    @Size(max = 500)
    @Schema(description = "message", example = "我想换这件，可以吗？")
    private String message;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public CounterSwapProposalRequest() {
    }

    public Long getTargetBundleId() {
        return targetBundleId;
    }

    public void setTargetBundleId(Long targetBundleId) {
        this.targetBundleId = targetBundleId;
    }

    public List<Long> getTargetItemIds() {
        return targetItemIds;
    }

    public void setTargetItemIds(List<Long> targetItemIds) {
        this.targetItemIds = targetItemIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CounterSwapProposalRequest{"
                + "targetBundleId=" + targetBundleId + ", "
                + "targetItemIds=" + targetItemIds + ", "
                + "message=" + message + ", "
                + '}';
    }
}
