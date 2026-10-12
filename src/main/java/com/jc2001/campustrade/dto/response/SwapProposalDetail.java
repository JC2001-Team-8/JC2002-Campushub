package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.SwapBundleDetail;
import com.jc2001.campustrade.dto.response.SwapCode;
import com.jc2001.campustrade.dto.response.SwapProposalItem;
import com.jc2001.campustrade.dto.response.SwapProposalStatus;
import com.jc2001.campustrade.dto.response.SwapTerminationReason;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.UserProfile;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 交换提议详情（PF-17）。**接受为整包操作，界面不得提供物品级勾选**（OOS-04 / R-22）。

 *
 * <p>对应契约 {@code components.schemas.SwapProposalDetail}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "SwapProposalDetail", description = "交换提议详情（PF-17）。**接受为整包操作，界面不得提供物品级勾选**（OOS-04 / R-22）。")
public class SwapProposalDetail {

    @NotNull
    @Schema(description = "id")
    private Long id;

    @NotNull
    @Schema(description = "proposalNo", example = "SWP-20261012-0001")
    private String proposalNo;

    @NotNull
    @Schema(description = "status")
    private SwapProposalStatus status;

    @NotNull
    @Schema(description = "role", example = "TARGET")
    private String role;

    @NotNull
    @Schema(description = "proposer")
    private UserProfile proposer;

    @NotNull
    @Schema(description = "target")
    private UserProfile target;

    /**
     * 发起方贡献的交换包（其物品被占用）
     */
    @NotNull
    @Schema(description = "发起方贡献的交换包（其物品被占用）")
    private SwapBundleDetail offerBundle;

    /**
     * 目标方的被请求商品（≥1 件，C-04）
     */
    @NotNull
    @Size(min = 1)
    @Schema(description = "目标方的被请求商品（≥1 件，C-04）")
    private List<SwapProposalItem> wantItems;

    @Schema(description = "offerTotal")
    private DecimalString offerTotal;

    @Schema(description = "wantTotal")
    private DecimalString wantTotal;

    @Schema(description = "priceDisclaimer", example = "双方估价合计仅供参考，平台不做等价判定、不支持现金补差价")
    private String priceDisclaimer;

    /**
     * 发起说明（**非聊天**，OOS-08）
     */
    @Schema(description = "发起说明（**非聊天**，OOS-08）", example = "想用收纳盒换你的羽毛球拍")
    private String message;

    @Schema(description = "tradeZone")
    private TradeZone tradeZone;

    @NotNull
    @Schema(description = "counterDepth")
    private Integer counterDepth;

    /**
     * 反提案来源提议 ID（仅 `counterDepth=1` 时有值，R-28）
     */
    @Schema(description = "反提案来源提议 ID（仅 `counterDepth=1` 时有值，R-28）")
    private Long parentProposalId;

    /**
     * 当前用户能否发起反提案（仅目标方、状态 `PENDING`、且 `counterDepth=0`）
     */
    @Schema(description = "当前用户能否发起反提案（仅目标方、状态 `PENDING`、且 `counterDepth=0`）")
    private Boolean canCounter;

    @Schema(description = "canAccept")
    private Boolean canAccept;

    @Schema(description = "canReject")
    private Boolean canReject;

    /**
     * 发起方是否可取消（仅 `PENDING`）
     */
    @Schema(description = "发起方是否可取消（仅 `PENDING`）")
    private Boolean canCancel;

    /**
     * 终结原因，`VOID_*` 表示整笔作废（R-24）
     */
    @Schema(description = "终结原因，`VOID_*` 表示整笔作废（R-24）")
    private SwapTerminationReason terminationReason;

    @NotNull
    @Schema(description = "expiresAt")
    private Instant expiresAt;

    @NotNull
    @Schema(description = "remainingSeconds")
    private Long remainingSeconds;

    @Schema(description = "acceptedAt")
    private Instant acceptedAt;

    /**
     * 一次性交换码。**仅提议双方可见**；仅在 `status=ACCEPTED` 后存在（R-26 / C-05）。
     * 状态未到时为 null。
     * 
     */
    @Schema(description = "一次性交换码。**仅提议双方可见**；仅在 `status=ACCEPTED` 后存在（R-26 / C-05）。 状态未到时为 null。")
    private SwapCode swapCode;

    /**
     * 发起方确认时间（R-27 双方确认 1/2）
     */
    @Schema(description = "发起方确认时间（R-27 双方确认 1/2）")
    private Instant proposerConfirmedAt;

    /**
     * 目标方确认时间（R-27 双方确认 2/2）
     */
    @Schema(description = "目标方确认时间（R-27 双方确认 2/2）")
    private Instant targetConfirmedAt;

    /**
     * 当前用户是否已确认（幂等，重复确认返回成功态）
     */
    @Schema(description = "当前用户是否已确认（幂等，重复确认返回成功态）")
    private Boolean myConfirmed;

    @Schema(description = "version")
    private Integer version;

    @Schema(description = "createdAt")
    private Instant createdAt;

    @Schema(description = "updatedAt")
    private Instant updatedAt;

    @Schema(description = "completedAt")
    private Instant completedAt;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public SwapProposalDetail() {
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

    public UserProfile getProposer() {
        return proposer;
    }

    public void setProposer(UserProfile proposer) {
        this.proposer = proposer;
    }

    public UserProfile getTarget() {
        return target;
    }

    public void setTarget(UserProfile target) {
        this.target = target;
    }

    public SwapBundleDetail getOfferBundle() {
        return offerBundle;
    }

    public void setOfferBundle(SwapBundleDetail offerBundle) {
        this.offerBundle = offerBundle;
    }

    public List<SwapProposalItem> getWantItems() {
        return wantItems;
    }

    public void setWantItems(List<SwapProposalItem> wantItems) {
        this.wantItems = wantItems;
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

    public String getPriceDisclaimer() {
        return priceDisclaimer;
    }

    public void setPriceDisclaimer(String priceDisclaimer) {
        this.priceDisclaimer = priceDisclaimer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TradeZone getTradeZone() {
        return tradeZone;
    }

    public void setTradeZone(TradeZone tradeZone) {
        this.tradeZone = tradeZone;
    }

    public Integer getCounterDepth() {
        return counterDepth;
    }

    public void setCounterDepth(Integer counterDepth) {
        this.counterDepth = counterDepth;
    }

    public Long getParentProposalId() {
        return parentProposalId;
    }

    public void setParentProposalId(Long parentProposalId) {
        this.parentProposalId = parentProposalId;
    }

    public Boolean getCanCounter() {
        return canCounter;
    }

    public void setCanCounter(Boolean canCounter) {
        this.canCounter = canCounter;
    }

    public Boolean getCanAccept() {
        return canAccept;
    }

    public void setCanAccept(Boolean canAccept) {
        this.canAccept = canAccept;
    }

    public Boolean getCanReject() {
        return canReject;
    }

    public void setCanReject(Boolean canReject) {
        this.canReject = canReject;
    }

    public Boolean getCanCancel() {
        return canCancel;
    }

    public void setCanCancel(Boolean canCancel) {
        this.canCancel = canCancel;
    }

    public SwapTerminationReason getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(SwapTerminationReason terminationReason) {
        this.terminationReason = terminationReason;
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

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public SwapCode getSwapCode() {
        return swapCode;
    }

    public void setSwapCode(SwapCode swapCode) {
        this.swapCode = swapCode;
    }

    public Instant getProposerConfirmedAt() {
        return proposerConfirmedAt;
    }

    public void setProposerConfirmedAt(Instant proposerConfirmedAt) {
        this.proposerConfirmedAt = proposerConfirmedAt;
    }

    public Instant getTargetConfirmedAt() {
        return targetConfirmedAt;
    }

    public void setTargetConfirmedAt(Instant targetConfirmedAt) {
        this.targetConfirmedAt = targetConfirmedAt;
    }

    public Boolean getMyConfirmed() {
        return myConfirmed;
    }

    public void setMyConfirmed(Boolean myConfirmed) {
        this.myConfirmed = myConfirmed;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "SwapProposalDetail{"
                + "id=" + id + ", "
                + "proposalNo=" + proposalNo + ", "
                + "status=" + status + ", "
                + "role=" + role + ", "
                + "proposer=" + proposer + ", "
                + "target=" + target + ", "
                + "offerBundle=" + offerBundle + ", "
                + "wantItems=" + wantItems + ", "
                + "offerTotal=" + offerTotal + ", "
                + "wantTotal=" + wantTotal + ", "
                + "priceDisclaimer=" + priceDisclaimer + ", "
                + "message=" + message + ", "
                + "tradeZone=" + tradeZone + ", "
                + "counterDepth=" + counterDepth + ", "
                + "parentProposalId=" + parentProposalId + ", "
                + "canCounter=" + canCounter + ", "
                + "canAccept=" + canAccept + ", "
                + "canReject=" + canReject + ", "
                + "canCancel=" + canCancel + ", "
                + "terminationReason=" + terminationReason + ", "
                + "expiresAt=" + expiresAt + ", "
                + "remainingSeconds=" + remainingSeconds + ", "
                + "acceptedAt=" + acceptedAt + ", "
                + "swapCode=" + swapCode + ", "
                + "proposerConfirmedAt=" + proposerConfirmedAt + ", "
                + "targetConfirmedAt=" + targetConfirmedAt + ", "
                + "myConfirmed=" + myConfirmed + ", "
                + "version=" + version + ", "
                + "createdAt=" + createdAt + ", "
                + "updatedAt=" + updatedAt + ", "
                + "completedAt=" + completedAt + ", "
                + '}';
    }
}
