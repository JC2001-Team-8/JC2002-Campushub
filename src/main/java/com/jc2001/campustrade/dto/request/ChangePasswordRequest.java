package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 修改密码（permission-matrix 2.10）。需校验原密码
 *
 * <p>对应契约 {@code components.schemas.ChangePasswordRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "ChangePasswordRequest", description = "修改密码（permission-matrix 2.10）。需校验原密码")
public class ChangePasswordRequest {

    @NotNull
    @Size(max = 64)
    @Schema(description = "oldPassword", example = "Passw0rd!2026")
    private String oldPassword;

    @NotNull
    @Size(min = 8, max = 64)
    @Schema(description = "newPassword", example = "NewPassw0rd!2026")
    private String newPassword;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ChangePasswordRequest() {
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest{"
                + "oldPassword=" + oldPassword + ", "
                + "newPassword=" + newPassword + ", "
                + '}';
    }
}
