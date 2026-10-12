package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.AdminService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.BanUserRequest;
import com.jc2001.campustrade.dto.request.CreateAppealRequest;
import com.jc2001.campustrade.dto.request.HandleReportRequest;
import com.jc2001.campustrade.dto.request.LiftPenaltyRequest;
import com.jc2001.campustrade.dto.request.ResolveAppealRequest;
import com.jc2001.campustrade.dto.request.RestoreItemRequest;
import com.jc2001.campustrade.dto.request.TakedownItemRequest;
import com.jc2001.campustrade.dto.response.AdminUserDetail;
import com.jc2001.campustrade.dto.response.AdminUserSummary;
import com.jc2001.campustrade.dto.response.Appeal;
import com.jc2001.campustrade.dto.response.AppealStatus;
import com.jc2001.campustrade.dto.response.AuditLog;
import com.jc2001.campustrade.dto.response.ItemDetail;
import com.jc2001.campustrade.dto.response.Penalty;
import com.jc2001.campustrade.dto.response.PenaltyStatus;
import com.jc2001.campustrade.dto.response.PenaltyType;
import com.jc2001.campustrade.dto.response.Report;
import com.jc2001.campustrade.dto.response.ReportStatus;
import com.jc2001.campustrade.dto.response.TakedownItemResponse;
import com.jc2001.campustrade.dto.response.UserStatus;
import java.time.Instant;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * AdminService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class AdminServiceImpl implements AdminService {

    /**
     * 搜索用户（FR-501）（未实现）
     *
     * <p>operationId: <code>adminSearchUsers</code>
     */
    @Override
    public PageResult<AdminUserSummary> adminSearchUsers(Integer page, Integer size, String keyword, Long userId, UserStatus status) {
        throw new UnsupportedOperationException(
                "TODO: implement adminSearchUsers");
    }

    /**
     * 查看用户详情与交易概览（permission-matrix 6.2）（未实现）
     *
     * <p>operationId: <code>adminGetUserDetail</code>
     */
    @Override
    public AdminUserDetail adminGetUserDetail(Long userId) {
        throw new UnsupportedOperationException(
                "TODO: implement adminGetUserDetail");
    }

    /**
     * 封禁用户三天（FR-503 / R-30）（未实现）
     *
     * <p>operationId: <code>adminBanUser</code>
     */
    @Override
    public Penalty adminBanUser(Long userId, String idempotencyKey, BanUserRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminBanUser");
    }

    /**
     * 查看某用户处罚记录（FR-504）（未实现）
     *
     * <p>operationId: <code>adminListUserPenalties</code>
     */
    @Override
    public PageResult<Penalty> adminListUserPenalties(Long userId, Integer page, Integer size, PenaltyType penaltyType, PenaltyStatus status) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListUserPenalties");
    }

    /**
     * 全量处罚记录查询（FR-504）（未实现）
     *
     * <p>operationId: <code>adminListPenalties</code>
     */
    @Override
    public PageResult<Penalty> adminListPenalties(Integer page, Integer size, Long targetUserId, PenaltyType penaltyType, PenaltyStatus status, Instant from, Instant to) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListPenalties");
    }

    /**
     * 手动解封（permission-matrix 6.8）（未实现）
     *
     * <p>operationId: <code>adminLiftPenalty</code>
     */
    @Override
    public Penalty adminLiftPenalty(Long penaltyId, String idempotencyKey, LiftPenaltyRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminLiftPenalty");
    }

    /**
     * 下架违规商品（FR-502 / R-35）（未实现）
     *
     * <p>operationId: <code>adminTakedownItem</code>
     */
    @Override
    public TakedownItemResponse adminTakedownItem(Long itemId, String idempotencyKey, TakedownItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminTakedownItem");
    }

    /**
     * 恢复被误下架商品（permission-matrix 6.4）（未实现）
     *
     * <p>operationId: <code>adminRestoreItem</code>
     */
    @Override
    public ItemDetail adminRestoreItem(Long itemId, String idempotencyKey, RestoreItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminRestoreItem");
    }

    /**
     * 申诉列表（FR-505）（未实现）
     *
     * <p>operationId: <code>adminListAppeals</code>
     */
    @Override
    public PageResult<Appeal> adminListAppeals(Integer page, Integer size, AppealStatus status) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListAppeals");
    }

    /**
     * 处理申诉（FR-505 / AP-02 / AP-03）（未实现）
     *
     * <p>operationId: <code>adminResolveAppeal</code>
     */
    @Override
    public Appeal adminResolveAppeal(Long appealId, String idempotencyKey, ResolveAppealRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminResolveAppeal");
    }

    /**
     * 举报列表（permission-matrix 6.9）（未实现）
     *
     * <p>operationId: <code>adminListReports</code>
     */
    @Override
    public PageResult<Report> adminListReports(Integer page, Integer size, ReportStatus status, Long itemId) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListReports");
    }

    /**
     * 处理举报（permission-matrix 6.9）（未实现）
     *
     * <p>operationId: <code>adminHandleReport</code>
     */
    @Override
    public Report adminHandleReport(Long reportId, String idempotencyKey, HandleReportRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement adminHandleReport");
    }

    /**
     * 查看审计日志（permission-matrix 6.10 / R-34）（未实现）
     *
     * <p>operationId: <code>adminListAuditLogs</code>
     */
    @Override
    public PageResult<AuditLog> adminListAuditLogs(Integer page, Integer size, String targetType, Long targetId, Long actorId, String action) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListAuditLogs");
    }

    /**
     * 查看预设字典（permission-matrix 6.11）（未实现）
     *
     * <p>operationId: <code>adminListDictionary</code>
     */
    @Override
    public List<Object> adminListDictionary(String dictionaryType) {
        throw new UnsupportedOperationException(
                "TODO: implement adminListDictionary");
    }

    /**
     * 查看自己的处罚记录（permission-matrix 2.11）（未实现）
     *
     * <p>operationId: <code>listMyPenalties</code>
     */
    @Override
    public PageResult<Penalty> listMyPenalties(Integer page, Integer size) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyPenalties");
    }

    /**
     * 提交申诉（permission-matrix 2.12 / AP-01）（未实现）
     *
     * <p>operationId: <code>createAppeal</code>
     */
    @Override
    public Appeal createAppeal(Long penaltyId, String idempotencyKey, CreateAppealRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createAppeal");
    }

    /**
     * 我的申诉（permission-matrix 2.12）（未实现）
     *
     * <p>operationId: <code>listMyAppeals</code>
     */
    @Override
    public PageResult<Appeal> listMyAppeals(Integer page, Integer size) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyAppeals");
    }

}
