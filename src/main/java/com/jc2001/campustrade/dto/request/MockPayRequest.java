package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 模拟付款（FR-306 / R-08）。**不接入任何真实支付通道**（OOS-01），不产生资金流水，不支持退款。
幂等（IDEM-A）：重复请求返回首次成功结果，不重复记录付款时间。

 *
 * <p>对应契约 {@code components.schemas.MockPayRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "MockPayRequest", description = "模拟付款（FR-306 / R-08）。**不接入任何真实支付通道**（OOS-01），不产生资金流水，不支持退款。 幂等（IDEM-A）：重复请求返回首次成功结果，不重复记录付款时间。")
public class MockPayRequest {

    /**
     * 前端二次确认标记（【待确认 Q-26】是否需要额外交互，当前假设一键成功）
     */
    @Schema(description = "前端二次确认标记（【待确认 Q-26】是否需要额外交互，当前假设一键成功）")
    private Boolean confirm;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public MockPayRequest() {
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }

    @Override
    public String toString() {
        return "MockPayRequest{"
                + "confirm=" + confirm + ", "
                + '}';
    }
}
