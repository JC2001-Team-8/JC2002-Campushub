package com.jc2001.campustrade.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 定时任务执行结果（FR-601~FR-604）。**必须幂等**（E-16 / R-07 / R-25 / R-31）：
重复执行不产生重复副作用，`processed` 为本次实际变更条数，
`skipped` 为因状态已变化而跳过的条数。

 *
 * <p>对应契约 {@code components.schemas.ScheduledTaskResult}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ScheduledTaskResult", description = "定时任务执行结果（FR-601~FR-604）。**必须幂等**（E-16 / R-07 / R-25 / R-31）： 重复执行不产生重复副作用，`processed` 为本次实际变更条数， `skipped` 为因状态已变化而跳过的条数。")
public class ScheduledTaskResult {

    @NotNull
    @Schema(description = "task", example = "ORDER_TIMEOUT_RELEASE")
    private String task;

    /**
     * 本次实际处理的记录数
     */
    @NotNull
    @Schema(description = "本次实际处理的记录数")
    private Integer processed;

    /**
     * 因并发/状态已变更而跳过的记录数（幂等保护）
     */
    @NotNull
    @Schema(description = "因并发/状态已变更而跳过的记录数（幂等保护）")
    private Integer skipped;

    @NotNull
    @Schema(description = "executedAt")
    private Instant executedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ScheduledTaskResult() {
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Integer getProcessed() {
        return processed;
    }

    public void setProcessed(Integer processed) {
        this.processed = processed;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(Integer skipped) {
        this.skipped = skipped;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    @Override
    public String toString() {
        return "ScheduledTaskResult{"
                + "task=" + task + ", "
                + "processed=" + processed + ", "
                + "skipped=" + skipped + ", "
                + "executedAt=" + executedAt + ", "
                + '}';
    }
}
