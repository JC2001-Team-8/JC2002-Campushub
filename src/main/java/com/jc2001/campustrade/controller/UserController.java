package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jc2001.campustrade.dto.request.NotificationSettings;
import com.jc2001.campustrade.dto.request.UpdateProfileRequest;
import com.jc2001.campustrade.dto.request.UserTradeZonesRequest;
import com.jc2001.campustrade.dto.response.CurrentUserResponse;
import com.jc2001.campustrade.dto.response.UserProfile;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 个人资料（FR-105 ~ FR-108）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "02-个人资料", description = "个人资料（FR-105 ~ FR-108）")
@RestController
@RequestMapping("/api/v1")
public class UserController {

    /**
     * 修改昵称与头像（FR-105 / FR-106）
     *
     * <p>operationId: <code>updateMyProfile</code>
     * <p>权限: USER
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "修改昵称与头像（FR-105 / FR-106）", operationId = "updateMyProfile", description = "修改昵称（1–20 字符，不唯一 A-04）与头像。**必须本人**。  - **被处罚用户在处罚期内禁止修改昵称与头像**（假设 A-13 / Q-12）：   返回 `403 USER_RESTRICTED_CANNOT_WRITE`。 - 头像需先通过 `POST /files/images` 上传取得 URL；传 `null` 表示删除头像。 - 【待确认 A-05】头像 1 张、JPG/PNG/WebP、≤5MB。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/me/profile")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> updateMyProfile(
            @RequestBody UpdateProfileRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement updateMyProfile");
    }

    /**
     * 设置常用交易区域（FR-107）
     *
     * <p>operationId: <code>setMyTradeZones</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "设置常用交易区域（FR-107）", operationId = "setMyTradeZones", description = "从**预设交易区域列表**勾选 **1–5 个**。**只允许勾选，不接受自由文本地址**（R-15 / GEO-01 / GEO-02）。  - **被处罚用户允许修改**（非交易写入，permission-matrix §8）。 - 少于 1 个或多于 5 个返回 `400 VALIDATION_INVALID_PARAMETER`。 - 传入不存在的区域 ID 返回 `422 TRADE_ZONE_NOT_FOUND`。 - **【待确认 Q-01/Q-02】**：当前区域为占位数据（`CAMPUS_A`、`COMMUNITY_A1` 等），   替换真实校区/社区时**不改变本接口路径与请求结构**。 - 本迭代**不读定位、不填住址、不做地图**（OOS-05 / OOS-06）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/me/trade-zones")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> setMyTradeZones(
            @RequestBody UserTradeZonesRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement setMyTradeZones");
    }

    /**
     * 查询通知设置（任务书能力，AQ-01）
     *
     * <p>operationId: <code>getMyNotificationSettings</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查询通知设置（任务书能力，AQ-01）", operationId = "getMyNotificationSettings", description = "【待确认 Q-05】通知渠道是否仅站内。  需求基线**未含独立 FR**，本接口由任务书的「通知开关」要求引入， 已登记为 **AQ-01**（见 api-open-questions.md）。当前默认：仅站内通知。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/notification-settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> getMyNotificationSettings() {
        throw new UnsupportedOperationException("TODO: implement getMyNotificationSettings");
    }

    /**
     * 修改通知设置（任务书能力，AQ-01）
     *
     * <p>operationId: <code>updateMyNotificationSettings</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "修改通知设置（任务书能力，AQ-01）", operationId = "updateMyNotificationSettings", description = "修改站内通知开关。**关闭后仍保留处罚类强制性通知**（治理通知不可关闭，R-32/R-34）。 【待确认 Q-05】：邮件/短信渠道迭代一未实现，`emailEnabled` 恒为 false。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/users/me/notification-settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> updateMyNotificationSettings(
            @RequestBody NotificationSettings body
    ) {
        throw new UnsupportedOperationException("TODO: implement updateMyNotificationSettings");
    }

    /**
     * 查看他人公开个人主页（FR-108）
     *
     * <p>operationId: <code>getPublicProfile</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看他人公开个人主页（FR-108）", operationId = "getPublicProfile", description = "**无需登录**（公开资料）。仅返回昵称、头像、在售商品数、评价均分、注册时间。  - **不返回任何联系方式、住址、定位信息**（GEO-02 / OOS-05）。 - 用户不存在返回 `404 USER_NOT_FOUND`。 - 迭代一评价**不参与排序/推荐**（OOS-09），`ratingAverage` 仅展示。 - **本接口公开**（`security: []`），7 类主体均可访问；`GUEST` 表示无需登录，   登录用户（USER / ADMIN / RESTRICTED）访问时返回内容相同（不返回任何私有字段）。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/users/{userId}/public-profile")
    public ResponseEntity<ApiResponse<UserProfile>> getPublicProfile(
            @Parameter(description = "用户 ID") @PathVariable("userId") Long userId
    ) {
        throw new UnsupportedOperationException("TODO: implement getPublicProfile");
    }

}
