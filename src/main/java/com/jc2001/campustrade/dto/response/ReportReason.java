package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 举报原因（FR-211 必选）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "举报原因（FR-211 必选）")
public enum ReportReason {

    /** FAKE_INFO */
    FAKE_INFO,

    /** PROHIBITED_ITEM */
    PROHIBITED_ITEM,

    /** WRONG_CATEGORY */
    WRONG_CATEGORY,

    /** SUSPECTED_FRAUD */
    SUSPECTED_FRAUD,

    /** OTHER */
    OTHER;

}
