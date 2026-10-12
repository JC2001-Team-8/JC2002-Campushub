package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 互评状态（state-machines.md §8，作用于订单维度）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "互评状态（state-machines.md §8，作用于订单维度）")
public enum ReviewStatus {

    /** OPEN */
    OPEN,

    /** BUYER_REVIEWED */
    BUYER_REVIEWED,

    /** SELLER_REVIEWED */
    SELLER_REVIEWED,

    /** BOTH_REVIEWED */
    BOTH_REVIEWED,

    /** WINDOW_CLOSED */
    WINDOW_CLOSED;

}
