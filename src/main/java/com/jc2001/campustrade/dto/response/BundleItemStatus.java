package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换包物品行状态。仅 `ACTIVE` 参与「同一商品同时只在一个进行中包内」的唯一约束
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "交换包物品行状态。仅 `ACTIVE` 参与「同一商品同时只在一个进行中包内」的唯一约束")
public enum BundleItemStatus {

    /** ACTIVE */
    ACTIVE,

    /** RELEASED */
    RELEASED,

    /** COMPLETED */
    COMPLETED;

}
