package com.jc2001.campustrade.service;

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
 * AdminService（Service 接口骨架）。
 *
 * <p><b>本步骤不实现业务逻辑</b>：实现类统一抛 UnsupportedOperationException。
 *
 * <p><b>返回原始领域类型</b>（如 OrderDetail / PageResult&lt;ItemSummary&gt;），
 * 由 Controller 负责包成 ApiResponse —— 包络属接口层职责，不下沉到 Service。
 *
 * <p><b>下一步实现必须遵守</b>（见 idempotency-and-concurrency.md）：
 * <ul>
 *   <li>状态转换使用条件更新或乐观锁，<b>禁止「先查后改」</b>（R-18）</li>
 *   <li>占用/释放必须与 items.status 投影<b>同一事务</b>（R-16 / DD-01）</li>
 *   <li>部分唯一索引冲突（SQLState 23505）映射为对应域 409，而非 500</li>
 *   <li>归属校验失败返回 403，不返回 404（E-02）</li>
 * </ul>
 */
public interface AdminService {

    /**
     * 搜索用户（FR-501）
     *
     * <p>对应 operationId: <code>adminSearchUsers</code>
     * <p>TODO 下一步：实现
     */
    PageResult<AdminUserSummary> adminSearchUsers(
            Integer page,
            Integer size,
            String keyword,
            Long userId,
            UserStatus status
    );

    /**
     * 查看用户详情与交易概览（permission-matrix 6.2）
     *
     * <p>对应 operationId: <code>adminGetUserDetail</code>
     * <p>TODO 下一步：实现
     */
    AdminUserDetail adminGetUserDetail(Long userId);

    /**
     * 封禁用户三天（FR-503 / R-30）
     *
     * <p>对应 operationId: <code>adminBanUser</code>
     * <p>TODO 下一步：实现
     */
    Penalty adminBanUser(Long userId, String idempotencyKey, BanUserRequest body);

    /**
     * 查看某用户处罚记录（FR-504）
     *
     * <p>对应 operationId: <code>adminListUserPenalties</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Penalty> adminListUserPenalties(
            Long userId,
            Integer page,
            Integer size,
            PenaltyType penaltyType,
            PenaltyStatus status
    );

    /**
     * 全量处罚记录查询（FR-504）
     *
     * <p>对应 operationId: <code>adminListPenalties</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Penalty> adminListPenalties(
            Integer page,
            Integer size,
            Long targetUserId,
            PenaltyType penaltyType,
            PenaltyStatus status,
            Instant from,
            Instant to
    );

    /**
     * 手动解封（permission-matrix 6.8）
     *
     * <p>对应 operationId: <code>adminLiftPenalty</code>
     * <p>TODO 下一步：实现
     */
    Penalty adminLiftPenalty(Long penaltyId, String idempotencyKey, LiftPenaltyRequest body);

    /**
     * 下架违规商品（FR-502 / R-35）
     *
     * <p>对应 operationId: <code>adminTakedownItem</code>
     * <p>TODO 下一步：实现
     */
    TakedownItemResponse adminTakedownItem(Long itemId, String idempotencyKey, TakedownItemRequest body);

    /**
     * 恢复被误下架商品（permission-matrix 6.4）
     *
     * <p>对应 operationId: <code>adminRestoreItem</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail adminRestoreItem(Long itemId, String idempotencyKey, RestoreItemRequest body);

    /**
     * 申诉列表（FR-505）
     *
     * <p>对应 operationId: <code>adminListAppeals</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Appeal> adminListAppeals(Integer page, Integer size, AppealStatus status);

    /**
     * 处理申诉（FR-505 / AP-02 / AP-03）
     *
     * <p>对应 operationId: <code>adminResolveAppeal</code>
     * <p>TODO 下一步：实现
     */
    Appeal adminResolveAppeal(Long appealId, String idempotencyKey, ResolveAppealRequest body);

    /**
     * 举报列表（permission-matrix 6.9）
     *
     * <p>对应 operationId: <code>adminListReports</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Report> adminListReports(Integer page, Integer size, ReportStatus status, Long itemId);

    /**
     * 处理举报（permission-matrix 6.9）
     *
     * <p>对应 operationId: <code>adminHandleReport</code>
     * <p>TODO 下一步：实现
     */
    Report adminHandleReport(Long reportId, String idempotencyKey, HandleReportRequest body);

    /**
     * 查看审计日志（permission-matrix 6.10 / R-34）
     *
     * <p>对应 operationId: <code>adminListAuditLogs</code>
     * <p>TODO 下一步：实现
     */
    PageResult<AuditLog> adminListAuditLogs(
            Integer page,
            Integer size,
            String targetType,
            Long targetId,
            Long actorId,
            String action
    );

    /**
     * 查看预设字典（permission-matrix 6.11）
     *
     * <p>对应 operationId: <code>adminListDictionary</code>
     * <p>TODO 下一步：实现
     */
    List<Object> adminListDictionary(String dictionaryType);

    /**
     * 查看自己的处罚记录（permission-matrix 2.11）
     *
     * <p>对应 operationId: <code>listMyPenalties</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Penalty> listMyPenalties(Integer page, Integer size);

    /**
     * 提交申诉（permission-matrix 2.12 / AP-01）
     *
     * <p>对应 operationId: <code>createAppeal</code>
     * <p>TODO 下一步：实现
     */
    Appeal createAppeal(Long penaltyId, String idempotencyKey, CreateAppealRequest body);

    /**
     * 我的申诉（permission-matrix 2.12）
     *
     * <p>对应 operationId: <code>listMyAppeals</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Appeal> listMyAppeals(Integer page, Integer size);

}
