package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品状态（state-machines.md §2.1）。
`RESERVED` = 订单占用，`SWAPPING` = 交换占用，两者**互斥**（R-23）。
`SOLD` / `BANNED` 为终态，任何写操作返回 409（IDEM-C）。

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "商品状态（state-machines.md §2.1）。 `RESERVED` = 订单占用，`SWAPPING` = 交换占用，两者**互斥**（R-23）。 `SOLD` / `BANNED` 为终态，任何写操作返回 409（IDEM-C）。")
public enum ItemStatus {

    /** DRAFT */
    DRAFT,

    /** IN_SALE */
    IN_SALE,

    /** RESERVED */
    RESERVED,

    /** SWAPPING */
    SWAPPING,

    /** SOLD */
    SOLD,

    /** OFF_SHELF */
    OFF_SHELF,

    /** BANNED */
    BANNED;

}
