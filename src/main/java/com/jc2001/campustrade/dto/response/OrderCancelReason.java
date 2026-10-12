package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 订单取消原因，区分 O-03/O-04/O-11 三条路径
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "订单取消原因，区分 O-03/O-04/O-11 三条路径")
public enum OrderCancelReason {

    /** BUYER_CANCELLED */
    BUYER_CANCELLED,

    /** PAYMENT_TIMEOUT */
    PAYMENT_TIMEOUT,

    /** ADMIN_TAKEDOWN */
    ADMIN_TAKEDOWN;

}
