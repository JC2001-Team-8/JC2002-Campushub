package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.AuthService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.dto.request.ChangePasswordRequest;
import com.jc2001.campustrade.dto.request.LoginRequest;
import com.jc2001.campustrade.dto.request.RefreshTokenRequest;
import com.jc2001.campustrade.dto.request.RegisterRequest;
import com.jc2001.campustrade.dto.response.AuthTokenResponse;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * AuthService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 用户注册（FR-101）（未实现）
     *
     * <p>operationId: <code>registerUser</code>
     */
    @Override
    public CurrentUserResponse registerUser(RegisterRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement registerUser");
    }

    /**
     * 用户登录（FR-102）（未实现）
     *
     * <p>operationId: <code>login</code>
     */
    @Override
    public AuthTokenResponse login(LoginRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement login");
    }

    /**
     * 刷新访问令牌（预留，FR-103）（未实现）
     *
     * <p>operationId: <code>refreshToken</code>
     */
    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement refreshToken");
    }

    /**
     * 退出登录（FR-104）（未实现）
     *
     * <p>operationId: <code>logout</code>
     */
    @Override
    public Void logout() {
        throw new UnsupportedOperationException(
                "TODO: implement logout");
    }

    /**
     * 获取当前登录用户（FR-103）（未实现）
     *
     * <p>operationId: <code>getCurrentUser</code>
     */
    @Override
    public CurrentUserResponse getCurrentUser() {
        throw new UnsupportedOperationException(
                "TODO: implement getCurrentUser");
    }

    /**
     * 修改密码（permission-matrix 2.10）（未实现）
     *
     * <p>operationId: <code>changePassword</code>
     */
    @Override
    public Void changePassword(ChangePasswordRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement changePassword");
    }

}
