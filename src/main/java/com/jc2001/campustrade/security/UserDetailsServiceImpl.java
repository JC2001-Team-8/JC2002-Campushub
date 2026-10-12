package com.jc2001.campustrade.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * {@link UserDetailsService} 骨架。
 *
 * <p><b>本步骤范围（重要）</b>：骨架阶段**不查数据库**——{@code entity} / {@code repository}
 * 尚未创建（下一步按 {@code design/schema.md} 的 25 张表生成）。
 * 因此本实现**始终抛出** {@link UsernameNotFoundException}，而不是返回伪造用户，
 * 以避免骨架被误当成可用的认证实现。
 *
 * <p><b>下一步实现要点</b>（按契约与规则）：
 * <ol>
 *   <li>按 {@code users.username} 查询（大小写不敏感，数据库为 {@code citext}）</li>
 *   <li>校验 {@code users.status}：<b>被处罚用户（RESTRICTED）允许登录</b>
 *       （permission-matrix §8），但交易类写操作须返回
 *       {@code 403 USER_RESTRICTED_CANNOT_WRITE} + {@code restrictedUntil}（E-03）</li>
 *   <li>角色映射：{@code users.role = 'ADMIN'} → {@code ROLE_ADMIN}，否则 {@code ROLE_USER}；
 *       仅此两个角色（P-00-A，不做多管理员角色分层）</li>
 *   <li>密码使用 BCrypt 校验（{@code PasswordEncoder}），**禁止**明文或可逆加密</li>
 * </ol>
 *
 * @see AuthenticatedUser
 * @see com.jc2001.campustrade.config.SecurityConfig
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    /**
     * 按用户名加载用户。
     *
     * <p>TODO 下一步：注入 {@code UserRepository} 并实现真实查询：
     * <pre>{@code
     * UserEntity user = userRepository.findByUsername(username)
     *         .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
     * return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash(),
     *         user.getStatus().name(), toIso8601(user.getRestrictedUntil()),
     *         List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
     * }</pre>
     *
     * @param username 登录用户名
     * @throws UsernameNotFoundException 骨架阶段始终抛出（尚未接入用户仓储）
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("UserDetailsServiceImpl 被调用（骨架阶段未接入用户仓储）: username={}", username);
        throw new UsernameNotFoundException(
                "骨架阶段尚未实现用户加载：entity/repository 将在下一步按 design/schema.md 生成");
    }
}
