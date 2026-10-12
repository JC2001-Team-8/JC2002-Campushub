package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 公共取货点类型
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "公共取货点类型")
public enum PickupPointType {

    /** LIBRARY */
    LIBRARY,

    /** CANTEEN */
    CANTEEN,

    /** TEACHING */
    TEACHING,

    /** DORM */
    DORM,

    /** GATE */
    GATE,

    /** SQUARE */
    SQUARE;

}
