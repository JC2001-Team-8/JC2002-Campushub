package com.jc2001.campustrade.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.common.ErrorResponse;
import com.jc2001.campustrade.common.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证过滤器（骨架）。
 *
 * <p><b>行为</b>：从 {@code Authorization: Bearer <token>} 解析令牌；
 * 合法则把认证信息写入 {@code SecurityContext}，非法则**静默跳过**
 * （由后续的授权规则决定是否返回 401，而不是在此直接拒绝）。
 *
 * <p><b>为什么非法令牌不直接拒绝</b>：骨架阶段 Security 为 {@code permitAll}，
 * 公开接口（如商品详情）携带过期令牌也应可访问。真正的「必须登录」判定交给
 * 授权规则与 {@code AuthenticationEntryPoint}。
 *
 * <p><b>本步骤范围</b>：骨架。用户名/角色从令牌声明中读取，
 * **不查询数据库**（{@link UserDetailsServiceImpl} 尚未接入仓储）。
 * 因此本过滤器不会强制认证，见 {@code SecurityConfig} 的 TODO。
 *
 * <p><b>下一步补齐</b>：
 * <ul>
 *   <li>令牌过期 → {@code 401 AUTH_TOKEN_EXPIRED}；无效 → {@code 401 AUTH_UNAUTHENTICATED}</li>
 *   <li>从数据库加载用户以获取实时 {@code status}（处罚状态可能已变化）</li>
 *   <li>按 {@code api-permission-mapping.md} 收紧各路径的访问规则</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(request.getHeader("Authorization"));

        if (token == null) {
            // 未携带令牌：匿名放行（是否需要登录由授权规则决定）
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                if (jwtTokenProvider.isExpired(token) && isProtectedApi(request)) {
                    writeUnauthorized(response, request, ErrorCode.AUTH_TOKEN_EXPIRED);
                    return;
                }
                log.debug("携带非法 JWT，按匿名处理: uri={}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 仅在尚未认证时写入，避免覆盖其他认证机制的结果
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtTokenProvider.getUsername(token);
                if (username != null) {
                    UserDetails principal = userDetailsService.loadUserByUsername(username);
                    List<SimpleGrantedAuthority> authorities = principal.getAuthorities().stream()
                            .map(a -> new SimpleGrantedAuthority(a.getAuthority())).collect(java.util.stream.Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 认证成功: username={}", username);
                }
            }
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // 骨架阶段不因认证异常中断请求；清空上下文后按匿名继续
            SecurityContextHolder.clearContext();
            log.warn("JWT 过滤器异常，按匿名继续: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private boolean isProtectedApi(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) return false;
        if ("GET".equalsIgnoreCase(request.getMethod()) && (path.equals("/api/v1/items")
                || path.matches("/api/v1/items/[^/]+") || path.matches("/api/v1/users/[^/]+/public-profile"))) return false;
        if (path.startsWith("/api/v1/auth/register") || path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/refresh") || path.startsWith("/api/v1/trade-zones/")
                || path.equals("/api/v1/trade-zones") || path.equals("/api/v1/categories")
                || path.equals("/api/v1/item-conditions") || path.matches("/api/v1/users/[^/]+/public-profile")) return false;
        return true;
    }

    /**
     * 写出统一格式的 401 响应。
     *
     * <p>骨架阶段未被调用（非法令牌按匿名放行）；下一步收紧权限后，
     * 对必须登录的路径直接调用本方法返回 {@code AUTH_UNAUTHENTICATED}。
     */
    @SuppressWarnings("unused")
    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, ErrorCode errorCode)
            throws IOException {
        String traceId = TraceIdFilter.currentTraceId(request);
        ErrorResponse body = ErrorResponse.of(errorCode, traceId);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
