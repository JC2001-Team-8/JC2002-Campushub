package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 解封来源（P-04 系统到期 / P-05 申诉通过 / P-07 管理员手动）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "解封来源（P-04 系统到期 / P-05 申诉通过 / P-07 管理员手动）")
public enum PenaltyLiftSource {

    /** SYSTEM_EXPIRY */
    SYSTEM_EXPIRY,

    /** APPEAL_APPROVED */
    APPEAL_APPROVED,

    /** ADMIN_MANUAL */
    ADMIN_MANUAL;

}
