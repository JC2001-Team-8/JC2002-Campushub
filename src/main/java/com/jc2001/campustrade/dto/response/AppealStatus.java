package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 申诉状态（state-machines.md §7.1）。一个处罚最多一条申诉（AP-01）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "申诉状态（state-machines.md §7.1）。一个处罚最多一条申诉（AP-01）")
public enum AppealStatus {

    /** SUBMITTED */
    SUBMITTED,

    /** APPROVED */
    APPROVED,

    /** REJECTED */
    REJECTED;

}
