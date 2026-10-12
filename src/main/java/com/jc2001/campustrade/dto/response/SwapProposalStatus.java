package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换提议状态（state-machines.md §4.1）。
`COUNTERED` 表示已反提案（仅一轮，R-28）；
`COMPLETED` / `REJECTED` / `EXPIRED` / `CANCELLED` 为终态（IDEM-C）。

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "交换提议状态（state-machines.md §4.1）。 `COUNTERED` 表示已反提案（仅一轮，R-28）； `COMPLETED` / `REJECTED` / `EXPIRED` / `CANCELLED` 为终态（IDEM-C）。")
public enum SwapProposalStatus {

    /** PENDING */
    PENDING,

    /** ACCEPTED */
    ACCEPTED,

    /** REJECTED */
    REJECTED,

    /** COUNTERED */
    COUNTERED,

    /** EXPIRED */
    EXPIRED,

    /** CANCELLED */
    CANCELLED,

    /** COMPLETED */
    COMPLETED;

}
