package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 注册请求（FR-101）。用户名唯一、大小写不敏感（数据库 citext）
 *
 * <p>对应契约 {@code components.schemas.RegisterRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "RegisterRequest", description = "注册请求（FR-101）。用户名唯一、大小写不敏感（数据库 citext）")
public class RegisterRequest {

    /**
     * 登录用户名，唯一，大小写不敏感，仅允许字母/数字/下划线
     */
    @NotNull
    @Size(min = 3, max = 32)
    @Schema(description = "登录用户名，唯一，大小写不敏感，仅允许字母/数字/下划线", example = "alice")
    private String username;

    /**
     * 【待确认 Q-06】是否必填与是否需要邮箱验证；当前假设**可选且不验证**
     */
    @Size(max = 254)
    @Schema(description = "【待确认 Q-06】是否必填与是否需要邮箱验证；当前假设**可选且不验证**", example = "alice@example.com")
    private String email;

    /**
     * 密码，至少 8 位；强度规则待接口契约冻结后由实现细化
     */
    @NotNull
    @Size(min = 8, max = 64)
    @Schema(description = "密码，至少 8 位；强度规则待接口契约冻结后由实现细化", example = "Passw0rd!2026")
    private String password;

    /**
     * 昵称，1–20 字符，**不要求唯一**（假设 A-04）
     */
    @NotNull
    @Size(min = 1, max = 20)
    @Schema(description = "昵称，1–20 字符，**不要求唯一**（假设 A-04）", example = "爱丽丝")
    private String nickname;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "RegisterRequest{"
                + "username=" + username + ", "
                + "email=" + email + ", "
                + "password=" + password + ", "
                + "nickname=" + nickname + ", "
                + '}';
    }
}
