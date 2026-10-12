package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 站内通知类型（FR-507）。【待确认 Q-05】是否仅站内渠道
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "站内通知类型（FR-507）。【待确认 Q-05】是否仅站内渠道")
public enum NotificationType {

    /** ORDER_CREATED */
    ORDER_CREATED,

    /** ORDER_PAID */
    ORDER_PAID,

    /** ORDER_DELIVERED */
    ORDER_DELIVERED,

    /** ORDER_COMPLETED */
    ORDER_COMPLETED,

    /** ORDER_CANCELLED */
    ORDER_CANCELLED,

    /** ORDER_TIMEOUT_CANCELLED */
    ORDER_TIMEOUT_CANCELLED,

    /** ORDER_ADMIN_CANCELLED */
    ORDER_ADMIN_CANCELLED,

    /** SWAP_PROPOSED */
    SWAP_PROPOSED,

    /** SWAP_ACCEPTED */
    SWAP_ACCEPTED,

    /** SWAP_REJECTED */
    SWAP_REJECTED,

    /** SWAP_COUNTERED */
    SWAP_COUNTERED,

    /** SWAP_EXPIRED */
    SWAP_EXPIRED,

    /** SWAP_CANCELLED */
    SWAP_CANCELLED,

    /** SWAP_VOIDED */
    SWAP_VOIDED,

    /** SWAP_COMPLETED */
    SWAP_COMPLETED,

    /** PENALTY_BANNED */
    PENALTY_BANNED,

    /** PENALTY_LIFTED */
    PENALTY_LIFTED,

    /** PENALTY_EXPIRED */
    PENALTY_EXPIRED,

    /** ITEM_BANNED */
    ITEM_BANNED,

    /** ITEM_RESTORED */
    ITEM_RESTORED,

    /** APPEAL_SUBMITTED */
    APPEAL_SUBMITTED,

    /** APPEAL_RESOLVED */
    APPEAL_RESOLVED,

    /** REPORT_HANDLED */
    REPORT_HANDLED,

    /** REVIEW_WINDOW_OPENED */
    REVIEW_WINDOW_OPENED,

    /** REVIEW_WINDOW_CLOSED */
    REVIEW_WINDOW_CLOSED;

}
