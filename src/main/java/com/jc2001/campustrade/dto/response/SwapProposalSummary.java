package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.SwapProposalStatus;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换提议摘要（列表用，PF-17）
 *
 * <p>对应契约 {@code components.schemas.SwapProposalSummary}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapProposalSummary", description = "交换提议摘要（列表用，PF-17）")
public class SwapProposalSummary {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "proposalNo", example = "SWP-20261012-0001")
    private String proposalNo;

    @NotNull
    @Schema(description = "status")
    private SwapProposalStatus status;

    /**
     * 当前用户在这条提议中的角色：`PROPOSER` = 我是发起方；`TARGET` = 我是目标方
     */
    @NotNull
    @Schema(description = "当前用户在这条提议中的角色：`PROPOSER` = 我是发起方；`TARGET` = 我是目标方", example = "TARGET")
    private String role;

    @Schema(description = "counterparty")
    private UserProfile counterparty;

    @Schema(description = "offerTotal")
    private DecimalString offerTotal;

    @Schema(description = "wantTotal")
    private DecimalString wantTotal;

    @Schema(description = "offerItemCount")
    private Integer offerItemCount;

    @Schema(description = "wantItemCount")
    private Integer wantItemCount;

    /**
     * `0` = 原提议，`1` = 反提案。**不可再反提案**（R-28 仅一轮，假设 A-10）
     * 
     */
    @NotNull
    @Schema(description = "`0` = 原提议，`1` = 反提案。**不可再反提案**（R-28 仅一轮，假设 A-10）")
    private Integer counterDepth;

    /**
     * 72 小时有效期绝对时间（R-25 / A-02）
     */
    @NotNull
    @Schema(description = "72 小时有效期绝对时间（R-25 / A-02）")
    private Instant expiresAt;

    /**
     * 剩余有效秒数，由服务端计算（R-19）
     */
    @NotNull
    @Schema(description = "剩余有效秒数，由服务端计算（R-19）")
    private Long remainingSeconds;

    @Schema(description = "createdAt")
    private Instant createdAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapProposalSummary() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(String proposalNo) {
        this.proposalNo = proposalNo;
    }

    public SwapProposalStatus getStatus() {
        return status;
    }

    public void setStatus(SwapProposalStatus status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserProfile getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(UserProfile counterparty) {
        this.counterparty = counterparty;
    }

    public DecimalString getOfferTotal() {
        return offerTotal;
    }

    public void setOfferTotal(DecimalString offerTotal) {
        this.offerTotal = offerTotal;
    }

    public DecimalString getWantTotal() {
        return wantTotal;
    }

    public void setWantTotal(DecimalString wantTotal) {
        this.wantTotal = wantTotal;
    }

    public Integer getOfferItemCount() {
        return offerItemCount;
    }

    public void setOfferItemCount(Integer offerItemCount) {
        this.offerItemCount = offerItemCount;
    }

    public Integer getWantItemCount() {
        return wantItemCount;
    }

    public void setWantItemCount(Integer wantItemCount) {
        this.wantItemCount = wantItemCount;
    }

    public Integer getCounterDepth() {
        return counterDepth;
    }

    public void setCounterDepth(Integer counterDepth) {
        this.counterDepth = counterDepth;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SwapProposalSummary{"
                + "id=" + id + ", "
                + "proposalNo=" + proposalNo + ", "
                + "status=" + status + ", "
                + "role=" + role + ", "
                + "counterparty=" + counterparty + ", "
                + "offerTotal=" + offerTotal + ", "
                + "wantTotal=" + wantTotal + ", "
                + "offerItemCount=" + offerItemCount + ", "
                + "wantItemCount=" + wantItemCount + ", "
                + "counterDepth=" + counterDepth + ", "
                + "expiresAt=" + expiresAt + ", "
                + "remainingSeconds=" + remainingSeconds + ", "
                + "createdAt=" + createdAt + ", "
                + '}';
    }
}
