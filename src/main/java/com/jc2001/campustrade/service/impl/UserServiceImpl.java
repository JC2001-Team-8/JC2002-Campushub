package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.UserService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.dto.request.NotificationSettings;
import com.jc2001.campustrade.dto.request.UpdateProfileRequest;
import com.jc2001.campustrade.dto.request.UserTradeZonesRequest;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;
import com.jc2001.campustrade.dto.response.UserProfile;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * UserService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * 修改昵称与头像（FR-105 / FR-106）（未实现）
     *
     * <p>operationId: <code>updateMyProfile</code>
     */
    @Override
    public CurrentUserResponse updateMyProfile(UpdateProfileRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateMyProfile");
    }

    /**
     * 设置常用交易区域（FR-107）（未实现）
     *
     * <p>operationId: <code>setMyTradeZones</code>
     */
    @Override
    public CurrentUserResponse setMyTradeZones(UserTradeZonesRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement setMyTradeZones");
    }

    /**
     * 查询通知设置（任务书能力，AQ-01）（未实现）
     *
     * <p>operationId: <code>getMyNotificationSettings</code>
     */
    @Override
    public NotificationSettings getMyNotificationSettings() {
        throw new UnsupportedOperationException(
                "TODO: implement getMyNotificationSettings");
    }

    /**
     * 修改通知设置（任务书能力，AQ-01）（未实现）
     *
     * <p>operationId: <code>updateMyNotificationSettings</code>
     */
    @Override
    public NotificationSettings updateMyNotificationSettings(NotificationSettings body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateMyNotificationSettings");
    }

    /**
     * 查看他人公开个人主页（FR-108）（未实现）
     *
     * <p>operationId: <code>getPublicProfile</code>
     */
    @Override
    public UserProfile getPublicProfile(Long userId) {
        throw new UnsupportedOperationException(
                "TODO: implement getPublicProfile");
    }

}
