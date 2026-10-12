package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 用户基础角色。**卖家/买家不是注册身份**，是关系角色（P-00-A）
 *
 * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。
 */
@Schema(description = "用户基础角色。**卖家/买家不是注册身份**，是关系角色（P-00-A）")
public enum UserRole {

    /** USER */
    USER,

    /** ADMIN */
    ADMIN;

}
