package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 处罚类型。`BAN_3D` 封禁三天（R-30，固定 72 小时，A-03）；
`ITEM_TAKEDOWN` 违规下架处置（仅举证，不影响用户交易能力）

 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "处罚类型。`BAN_3D` 封禁三天（R-30，固定 72 小时，A-03）； `ITEM_TAKEDOWN` 违规下架处置（仅举证，不影响用户交易能力）")
public enum PenaltyType {

    /** BAN_3D */
    BAN_3D,

    /** ITEM_TAKEDOWN */
    ITEM_TAKEDOWN;

}
