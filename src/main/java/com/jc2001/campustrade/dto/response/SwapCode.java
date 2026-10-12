package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.SwapCodeStatus;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 一次性交换码（FR-409 / R-26 / C-05）。**仅提议双方可见**，禁止出现在公开列表。
核销后状态变为 `VERIFIED`，**不可再次核销**（E-12）。

 *
 * <p>对应契约 {@code components.schemas.SwapCode}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapCode", description = "一次性交换码（FR-409 / R-26 / C-05）。**仅提议双方可见**，禁止出现在公开列表。 核销后状态变为 `VERIFIED`，**不可再次核销**（E-12）。")
public class SwapCode {

    @NotNull
    @Schema(description = "id")
    private Long id;

    /**
     * 交换码。**【待确认 Q-30】**格式与长度（当前种子示例 `SWP8A2F`）
     */
    @NotNull
    @Size(max = 16)
    @Schema(description = "交换码。**【待确认 Q-30】**格式与长度（当前种子示例 `SWP8A2F`）", example = "SWP8A2F")
    private String code;

    @NotNull
    @Schema(description = "status")
    private SwapCodeStatus status;

    @Schema(description = "verifiedAt")
    private Instant verifiedAt;

    /**
     * 核销人用户 ID（提议双方之一）
     */
    @Schema(description = "核销人用户 ID（提议双方之一）")
    private Long verifiedBy;

    @Schema(description = "expiresAt")
    private Instant expiresAt;

    @Schema(description = "notice", example = "交换码仅双方可见，请勿对外泄露；核销后即失效")
    private String notice;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapCode() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SwapCodeStatus getStatus() {
        return status;
    }

    public void setStatus(SwapCodeStatus status) {
        this.status = status;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    @Override
    public String toString() {
        return "SwapCode{"
                + "id=" + id + ", "
                + "code=" + code + ", "
                + "status=" + status + ", "
                + "verifiedAt=" + verifiedAt + ", "
                + "verifiedBy=" + verifiedBy + ", "
                + "expiresAt=" + expiresAt + ", "
                + "notice=" + notice + ", "
                + '}';
    }
}
