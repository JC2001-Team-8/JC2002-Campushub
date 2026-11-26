package com.jc2001.campustrade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.common.ErrorResponse;
import com.jc2001.campustrade.security.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置（骨架）。
 *
 * <p>API is stateless. Public routes are explicitly allowlisted; all other API routes require a JWT.
 *
 * <p>Authorization baseline follows {@code docs/iteration1/design/api-permission-mapping.md}:
 * <pre>
 *   /auth/register, /auth/login, /auth/refresh        → permitAll（契约 security: []）
 *   /trade-zones/**, /categories, /item-conditions    → permitAll（公开字典）
 *   /items (GET), /items/{id} (GET)                   → permitAll（访客可浏览）
 *   /users/{userId}/public-profile (GET)              → permitAll（公开资料）
 *   /admin/**                                         → hasRole('ADMIN')
 *   /internal/**                                      → 内部凭证校验（X-Internal-Token），
 *                                                        网关层须禁止外部访问（AD-18）
 *   其余 /api/v1/**                                   → authenticated()
 * </pre>
 *
 * <p><b>关键安全约束（必须实现，见契约）</b>：
 * <ul>
 *   <li><b>越权返回 403 而非 404</b>（E-02 / AD-02），归属校验在 Service 层完成</li>
 *   <li><b>被处罚用户（RESTRICTED）禁止交易类写操作</b>，返回
 *       {@code 403 USER_RESTRICTED_CANNOT_WRITE} + {@code restrictedUntil}（E-03）</li>
 *   <li><b>管理员不参与交易</b>（P-00-A）：交易类接口须显式拒绝 {@code ROLE_ADMIN}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
    @EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** 无需认证即可访问的路径（公开接口）。 */
    private static final String[] PUBLIC_ENDPOINTS = {
            // 认证相关（契约 security: []）
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/trade-zones/**",
            "/api/v1/categories",
            "/api/v1/item-conditions",
            "/api/v1/users/*/public-profile",
            // 健康检查与运维
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            // OpenAPI / Swagger UI
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            // 已冻结的契约文档（静态资源）
            "/openapi/**",
            // 骨架阶段的健康接口
            "/api/v1/health",
            // 静态资源（图片本地存储，OOS-18）
            "/static/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.enabled:true}")
    private boolean corsEnabled;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Value("${app.cors.allow-credentials:false}")
    private boolean corsAllowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * 安全过滤链。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + Bearer 令牌：不需要 CSRF
                .csrf().disable()
                // 无状态：不创建 HttpSession
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // CORS（配置见下方 corsConfigurationSource）
                .cors().configurationSource(corsConfigurationSource())
                .and()
                // 统一 401/403 响应体，使用契约的 ErrorResponse 结构
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) ->
                        writeError(response, request, ErrorCode.UNAUTHORIZED, "未登录或登录态已失效，请重新登录"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeError(response, request, ErrorCode.FORBIDDEN, "无权执行该操作"))
                .and()
                // 路径授权规则
                .authorizeRequests()
                .antMatchers(PUBLIC_ENDPOINTS).permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/v1/items", "/api/v1/items/*", "/api/v1/users/*/public-profile").permitAll()
                .antMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .antMatchers("/api/v1/internal/**").denyAll()
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
                .and()
                // 关闭默认表单登录与 HTTP Basic（纯 API）
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                // JWT 过滤器置于用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器：BCrypt。
     *
     * <p>契约与规则要求**禁止明文/可逆存储**（{@code users.password_hash}）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器（供下一步 {@code POST /auth/login} 使用）。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * CORS 配置（前端联调用；迭代一前端尚未实现）。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if (corsEnabled) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(configuration::addAllowedOrigin);
            Arrays.stream(corsAllowedMethods.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(configuration::addAllowedMethod);
            if ("*".equals(corsAllowedHeaders.trim())) {
                configuration.addAllowedHeader("*");
            } else {
                Arrays.stream(corsAllowedHeaders.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(configuration::addAllowedHeader);
            }
            // 暴露 traceId 供前端读取
            configuration.addExposedHeader("X-Trace-Id");
            configuration.setAllowCredentials(corsAllowCredentials);
            configuration.setMaxAge(corsMaxAge);
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 写出契约格式的错误响应（与 {@code GlobalExceptionHandler} 结构一致）。
     *
     * <p>Security 过滤器链中的异常发生在 {@code @RestControllerAdvice} 之前，
     * 因此这里手工序列化 {@link ErrorResponse}。
     */
    private void writeError(HttpServletResponse response,
                            javax.servlet.http.HttpServletRequest request,
                            ErrorCode errorCode,
                            String message) throws java.io.IOException {
        String traceId = com.jc2001.campustrade.common.TraceIdFilter.currentTraceId(request);
        ErrorResponse body = ErrorResponse.of(errorCode, message, null, traceId);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(com.jc2001.campustrade.common.TraceIdFilter.TRACE_ID_HEADER, traceId);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /** 供日志展示的白名单路径（便于排查 401/403）。 */
    @SuppressWarnings("unused")
    private List<String> publicEndpoints() {
        return Arrays.asList(PUBLIC_ENDPOINTS);
    }
}
