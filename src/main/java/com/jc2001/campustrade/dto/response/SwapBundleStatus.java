package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换包状态（state-machines.md §5.1）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "交换包状态（state-machines.md §5.1）")
public enum SwapBundleStatus {

    /** DRAFT */
    DRAFT,

    /** PACKED */
    PACKED,

    /** IN_SWAP */
    IN_SWAP,

    /** OCCUPIED */
    OCCUPIED,

    /** RELEASED */
    RELEASED,

    /** COMPLETED */
    COMPLETED;

}
