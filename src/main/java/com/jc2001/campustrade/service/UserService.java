package com.jc2001.campustrade.service;

import com.jc2001.campustrade.dto.request.NotificationSettings;
import com.jc2001.campustrade.dto.request.UpdateProfileRequest;
import com.jc2001.campustrade.dto.request.UserTradeZonesRequest;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;
import com.jc2001.campustrade.dto.response.UserProfile;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * UserService（Service 接口骨架）。
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
public interface UserService {

    /**
     * 修改昵称与头像（FR-105 / FR-106）
     *
     * <p>对应 operationId: <code>updateMyProfile</code>
     * <p>TODO 下一步：实现
     */
    CurrentUserResponse updateMyProfile(UpdateProfileRequest body);

    /**
     * 设置常用交易区域（FR-107）
     *
     * <p>对应 operationId: <code>setMyTradeZones</code>
     * <p>TODO 下一步：实现
     */
    CurrentUserResponse setMyTradeZones(UserTradeZonesRequest body);

    /**
     * 查询通知设置（任务书能力，AQ-01）
     *
     * <p>对应 operationId: <code>getMyNotificationSettings</code>
     * <p>TODO 下一步：实现
     */
    NotificationSettings getMyNotificationSettings();

    /**
     * 修改通知设置（任务书能力，AQ-01）
     *
     * <p>对应 operationId: <code>updateMyNotificationSettings</code>
     * <p>TODO 下一步：实现
     */
    NotificationSettings updateMyNotificationSettings(NotificationSettings body);

    /**
     * 查看他人公开个人主页（FR-108）
     *
     * <p>对应 operationId: <code>getPublicProfile</code>
     * <p>TODO 下一步：实现
     */
    UserProfile getPublicProfile(Long userId);

}
