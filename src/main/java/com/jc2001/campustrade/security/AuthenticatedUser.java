package com.jc2001.campustrade.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 认证主体（Security 骨架）。
 *
 * <p>承载当前登录用户的最小信息：用户 ID、用户名、角色、账号状态。
 * 与契约 {@code CurrentUserResponse} 对应，但**不含**任何敏感字段（无密码、无内部审计字段）。
 *
 * <p><b>角色说明</b>（{@code permission-matrix.md} §3.1）：
 * 卖家/买家是**关系角色**，不是注册身份，因此这里只有 {@code USER} / {@code ADMIN}；
 * 被处罚用户通过 {@code status = RESTRICTED} 表达，不占独立角色。
 *
 * <p><b>本步骤范围</b>：骨架。真实用户加载见 {@link UserDetailsServiceImpl} 的 TODO。
 */
public class AuthenticatedUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（对应 {@code users.id}）。 */
    private final Long userId;

    /** 登录用户名（对应 {@code users.username}，大小写不敏感）。 */
    private final String username;

    /** 密码摘要。骨架阶段为占位；真实值由用户仓储加载。 */
    private final String password;

    /** 账号状态：ACTIVE / RESTRICTED（被处罚用户）。 */
    private final String status;

    /** 解封时间（ISO 8601 UTC 字符串）；仅 RESTRICTED 时非空（E-03 前端提示用）。 */
    private final String restrictedUntil;

    /** 权限集合（形如 {@code ROLE_USER} / {@code ROLE_ADMIN}）。 */
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long userId,
                             String username,
                             String password,
                             String status,
                             String restrictedUntil,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.restrictedUntil = restrictedUntil;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public String getRestrictedUntil() {
        return restrictedUntil;
    }

    /**
     * 是否为被处罚用户。
     *
     * <p>TODO 下一步：交易类写接口需据此拦截并返回
     * {@code 403 USER_RESTRICTED_CANNOT_WRITE}（E-03），
     * 白名单见 {@code api-permission-mapping.md} §7。
     */
    public boolean isRestricted() {
        return "RESTRICTED".equals(status);
    }

    /**
     * 是否管理员。
     *
     * <p>注意 P-00-A：**管理员不参与交易**，交易类接口必须显式拒绝 ADMIN。
     */
    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    // -----------------------------------------------------------------
    // UserDetails
    // -----------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO 下一步：可根据 penalties 表判断是否存在生效中封禁
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 被处罚用户**允许登录**（permission-matrix §8：可登录、可申诉），故不在此禁用
        return true;
    }
}
