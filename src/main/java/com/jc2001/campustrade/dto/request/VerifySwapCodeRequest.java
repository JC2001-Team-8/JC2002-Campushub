package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 校验收换码（线下交付核销，R-26）。**一次性**：成功核销后再次核销返回 409 `SWAP_CODE_ALREADY_VERIFIED`。
仅提议双方可核销，且提议状态必须为 `ACCEPTED`。

 *
 * <p>对应契约 {@code components.schemas.VerifySwapCodeRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "VerifySwapCodeRequest", description = "校验收换码（线下交付核销，R-26）。**一次性**：成功核销后再次核销返回 409 `SWAP_CODE_ALREADY_VERIFIED`。 仅提议双方可核销，且提议状态必须为 `ACCEPTED`。")
public class VerifySwapCodeRequest {

    @NotNull
    @Size(max = 16)
    @Schema(description = "code", example = "SWP8A2F")
    private String code;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public VerifySwapCodeRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "VerifySwapCodeRequest{"
                + "code=" + code + ", "
                + '}';
    }
}
