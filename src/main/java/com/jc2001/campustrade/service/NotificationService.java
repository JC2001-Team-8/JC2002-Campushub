package com.jc2001.campustrade.service;

import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.MarkNotificationsReadRequest;
import com.jc2001.campustrade.dto.response.Notification;
import com.jc2001.campustrade.dto.response.NotificationType;
import com.jc2001.campustrade.dto.response.UnreadCountResponse;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * NotificationService（Service 接口骨架）。
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
public interface NotificationService {

    /**
     * 站内通知列表（FR-507）
     *
     * <p>对应 operationId: <code>listMyNotifications</code>
     * <p>TODO 下一步：实现
     */
    PageResult<Notification> listMyNotifications(
            Integer page,
            Integer size,
            List<String> sort,
            Boolean unreadOnly,
            NotificationType type
    );

    /**
     * 未读通知数（FR-507）
     *
     * <p>对应 operationId: <code>getUnreadNotificationCount</code>
     * <p>TODO 下一步：实现
     */
    UnreadCountResponse getUnreadNotificationCount();

    /**
     * 标记通知已读（FR-507）
     *
     * <p>对应 operationId: <code>markNotificationsRead</code>
     * <p>TODO 下一步：实现
     */
    UnreadCountResponse markNotificationsRead(String idempotencyKey, MarkNotificationsReadRequest body);

}
