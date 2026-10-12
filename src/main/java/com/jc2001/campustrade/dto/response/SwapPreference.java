package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 卖家交换意愿（R-20）。占用态与终态不可改为 `ACCEPTING`
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "卖家交换意愿（R-20）。占用态与终态不可改为 `ACCEPTING`")
public enum SwapPreference {

    /** ACCEPTING */
    ACCEPTING,

    /** NOT_ACCEPTING */
    NOT_ACCEPTING;

}
