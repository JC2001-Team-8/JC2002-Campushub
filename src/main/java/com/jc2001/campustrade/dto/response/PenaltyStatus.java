package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 处罚状态（state-machines.md §6.1）。`APPEALING` 期间**处罚仍生效**（不停表，P-03）。
记录**永不物理删除**（R-32）。

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "处罚状态（state-machines.md §6.1）。`APPEALING` 期间**处罚仍生效**（不停表，P-03）。 记录**永不物理删除**（R-32）。")
public enum PenaltyStatus {

    /** ACTIVE */
    ACTIVE,

    /** APPEALING */
    APPEALING,

    /** LIFTED */
    LIFTED,

    /** EXPIRED */
    EXPIRED;

}
