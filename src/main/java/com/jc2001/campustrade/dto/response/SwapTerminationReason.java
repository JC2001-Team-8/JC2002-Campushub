package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换提议终结原因。`VOID_*` 系列对应 R-24 **整笔作废**（任一件被占用/下架即整笔作废）。

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "交换提议终结原因。`VOID_*` 系列对应 R-24 **整笔作废**（任一件被占用/下架即整笔作废）。")
public enum SwapTerminationReason {

    /** TARGET_REJECTED */
    TARGET_REJECTED,

    /** PROPOSER_CANCELLED */
    PROPOSER_CANCELLED,

    /** EXPIRED_72H */
    EXPIRED_72H,

    /** VOID_ITEM_OCCUPIED */
    VOID_ITEM_OCCUPIED,

    /** VOID_ITEM_OFF_SHELF */
    VOID_ITEM_OFF_SHELF,

    /** VOID_ITEM_BANNED */
    VOID_ITEM_BANNED,

    /** COUNTERED */
    COUNTERED;

}
