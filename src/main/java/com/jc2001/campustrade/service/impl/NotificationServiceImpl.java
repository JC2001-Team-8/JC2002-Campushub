package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.NotificationService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.MarkNotificationsReadRequest;
import com.jc2001.campustrade.dto.response.Notification;
import com.jc2001.campustrade.dto.response.NotificationType;
import com.jc2001.campustrade.dto.response.UnreadCountResponse;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * NotificationService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /**
     * 站内通知列表（FR-507）（未实现）
     *
     * <p>operationId: <code>listMyNotifications</code>
     */
    @Override
    public PageResult<Notification> listMyNotifications(Integer page, Integer size, List<String> sort, Boolean unreadOnly, NotificationType type) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyNotifications");
    }

    /**
     * 未读通知数（FR-507）（未实现）
     *
     * <p>operationId: <code>getUnreadNotificationCount</code>
     */
    @Override
    public UnreadCountResponse getUnreadNotificationCount() {
        throw new UnsupportedOperationException(
                "TODO: implement getUnreadNotificationCount");
    }

    /**
     * 标记通知已读（FR-507）（未实现）
     *
     * <p>operationId: <code>markNotificationsRead</code>
     */
    @Override
    public UnreadCountResponse markNotificationsRead(String idempotencyKey, MarkNotificationsReadRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement markNotificationsRead");
    }

}
