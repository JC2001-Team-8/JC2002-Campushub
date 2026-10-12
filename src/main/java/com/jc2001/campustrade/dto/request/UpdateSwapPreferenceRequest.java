package com.jc2001.campustrade.dto.request;

import com.jc2001.campustrade.dto.response.SwapPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 设置商品交换意愿（FR-401 / R-20）。占用态与终态不可改为 `ACCEPTING`
 *
 * <p>对应契约 {@code components.schemas.UpdateSwapPreferenceRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UpdateSwapPreferenceRequest", description = "设置商品交换意愿（FR-401 / R-20）。占用态与终态不可改为 `ACCEPTING`")
public class UpdateSwapPreferenceRequest {

    @NotNull
    @Schema(description = "swapPreference")
    private SwapPreference swapPreference;

    /**
     * 乐观锁版本号（R-18）
     */
    @Schema(description = "乐观锁版本号（R-18）")
    private Integer version;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UpdateSwapPreferenceRequest() {
    }

    public SwapPreference getSwapPreference() {
        return swapPreference;
    }

    public void setSwapPreference(SwapPreference swapPreference) {
        this.swapPreference = swapPreference;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "UpdateSwapPreferenceRequest{"
                + "swapPreference=" + swapPreference + ", "
                + "version=" + version + ", "
                + '}';
    }
}
