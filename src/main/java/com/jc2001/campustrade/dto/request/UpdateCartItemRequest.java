package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 修改购物车条目（permission-matrix 4.3）。
因数量固定为 1（A-16），本接口仅用于确认条目有效性，**不接受数量变更**；
如需移除请调用 `DELETE /cart/items/{cartItemId}`。

 *
 * <p>对应契约 {@code components.schemas.UpdateCartItemRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UpdateCartItemRequest", description = "修改购物车条目（permission-matrix 4.3）。 因数量固定为 1（A-16），本接口仅用于确认条目有效性，**不接受数量变更**； 如需移除请调用 `DELETE /cart/items/{cartItemId}`。")
public class UpdateCartItemRequest {

    @Schema(description = "version")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UpdateCartItemRequest() {
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "UpdateCartItemRequest{"
                + "version=" + version + ", "
                + '}';
    }
}
