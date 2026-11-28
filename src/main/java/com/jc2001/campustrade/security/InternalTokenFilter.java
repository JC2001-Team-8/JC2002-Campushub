package com.jc2001.campustrade.security;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 内部任务接口鉴权（契约 {@code securityScheme: internalToken}，header {@code X-Internal-Token}）。
 *
 * <p>系统定时任务不是普通用户，没有 JWT；本过滤器校验共享密钥后，为当前请求授予
 * {@code ROLE_SYSTEM}，与 {@code InternalController} 上的 {@code @PreAuthorize("hasRole('SYSTEM')")}
 * 以及 {@code SecurityConfig} 的路径规则配合。
 *
 * <p>密钥错误或缺失时<b>不</b>授予权限，请求会继续走到路径规则并得到 401/403；
 * 不在此处直接拒绝，避免影响其它路径。
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    @Value("${app.internal.token:dev-internal-token-change-me}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided != null && !provided.isEmpty() && constantTimeEquals(provided, expectedToken)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "system", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (b == null) return false;
        byte[] x = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] y = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }
}
