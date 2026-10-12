package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 订单状态（state-machines.md §3.1）。**无退款状态**（OOS-14 不做退款）。
`COMPLETED` / `CANCELLED` / `TIMEOUT_CANCELLED` 为终态（IDEM-C）。

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "订单状态（state-machines.md §3.1）。**无退款状态**（OOS-14 不做退款）。 `COMPLETED` / `CANCELLED` / `TIMEOUT_CANCELLED` 为终态（IDEM-C）。")
public enum OrderStatus {

    /** PENDING_PAYMENT */
    PENDING_PAYMENT,

    /** PAID */
    PAID,

    /** DELIVERED */
    DELIVERED,

    /** COMPLETED */
    COMPLETED,

    /** CANCELLED */
    CANCELLED,

    /** TIMEOUT_CANCELLED */
    TIMEOUT_CANCELLED;

}
