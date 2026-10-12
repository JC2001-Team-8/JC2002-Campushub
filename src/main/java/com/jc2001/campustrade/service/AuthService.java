package com.jc2001.campustrade.service;

import com.jc2001.campustrade.dto.request.ChangePasswordRequest;
import com.jc2001.campustrade.dto.request.LoginRequest;
import com.jc2001.campustrade.dto.request.RefreshTokenRequest;
import com.jc2001.campustrade.dto.request.RegisterRequest;
import com.jc2001.campustrade.dto.response.AuthTokenResponse;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * AuthService（Service 接口骨架）。
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
public interface AuthService {

    /**
     * 用户注册（FR-101）
     *
     * <p>对应 operationId: <code>registerUser</code>
     * <p>TODO 下一步：实现
     */
    CurrentUserResponse registerUser(RegisterRequest body);

    /**
     * 用户登录（FR-102）
     *
     * <p>对应 operationId: <code>login</code>
     * <p>TODO 下一步：实现
     */
    AuthTokenResponse login(LoginRequest body);

    /**
     * 刷新访问令牌（预留，FR-103）
     *
     * <p>对应 operationId: <code>refreshToken</code>
     * <p>TODO 下一步：实现
     */
    AuthTokenResponse refreshToken(RefreshTokenRequest body);

    /**
     * 退出登录（FR-104）
     *
     * <p>对应 operationId: <code>logout</code>
     * <p>TODO 下一步：实现
     */
    Void logout();

    /**
     * 获取当前登录用户（FR-103）
     *
     * <p>对应 operationId: <code>getCurrentUser</code>
     * <p>TODO 下一步：实现
     */
    CurrentUserResponse getCurrentUser();

    /**
     * 修改密码（permission-matrix 2.10）
     *
     * <p>对应 operationId: <code>changePassword</code>
     * <p>TODO 下一步：实现
     */
    Void changePassword(ChangePasswordRequest body);

}
