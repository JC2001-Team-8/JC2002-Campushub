package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 无业务数据的成功响应（`data` 为 null）
 *
 * <p>对应契约 {@code components.schemas.EmptyData}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "EmptyData", description = "无业务数据的成功响应（`data` 为 null）")
public class EmptyData {

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public EmptyData() {
    }

    @Override
    public String toString() {
        return "EmptyData{"
                + '}';
    }
}
