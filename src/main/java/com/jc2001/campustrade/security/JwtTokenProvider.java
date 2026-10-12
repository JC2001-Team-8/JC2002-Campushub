package com.jc2001.campustrade.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 令牌工具（骨架）。
 *
 * <p>提供**生成**与**解析**访问令牌的能力，签名算法 HS256，密钥来自 {@code app.jwt.secret}。
 *
 * <p><b>契约依据</b>：
 * <ul>
 *   <li>令牌有效期 <b>7 天</b>（假设 A-06 / 规则 R-01），默认 {@code app.jwt.expiration-seconds=604800}</li>
 *   <li>令牌过期返回 <b>401</b> {@code AUTH_TOKEN_EXPIRED}；无效返回 401 {@code AUTH_UNAUTHENTICATED}</li>
 *   <li>【待确认 Q-07】是否需要 refresh token：当前假设**仅固定 7 天单令牌**，
 *       {@code /auth/refresh} 在契约中保留但默认返回 501，故本类**不实现** refresh 逻辑</li>
 * </ul>
 *
 * <p><b>本步骤范围</b>：骨架，仅令牌编解码；不查询用户、不校验处罚状态。
 *
 * <p><b>安全提示</b>：{@code app.jwt.secret} 默认值为开发占位串，
 * <b>生产环境必须</b>通过环境变量 {@code APP_JWT_SECRET} 注入 ≥32 字节的随机密钥。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** 自定义声明：用户 ID。 */
    public static final String CLAIM_USER_ID = "uid";

    /** 自定义声明：账号状态（ACTIVE / RESTRICTED）。 */
    public static final String CLAIM_STATUS = "status";

    /** 自定义声明：角色。 */
    public static final String CLAIM_ROLE = "role";

    private final String secret;
    private final long expirationSeconds;
    private final String issuer;

    private SecretKey signingKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-seconds:604800}") long expirationSeconds,
                            @Value("${app.jwt.issuer:campus-trade}") String issuer) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    /**
     * 初始化签名密钥。HS256 要求密钥长度 ≥ 256 bit（32 字节）。
     *
     * @throws IllegalStateException 当密钥过短时快速失败，避免启动后才发现配置错误
     */
    @PostConstruct
    void init() {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret 长度不足：HS256 要求至少 32 字节，当前 " + keyBytes.length
                            + " 字节。请通过环境变量 APP_JWT_SECRET 注入足够长度的密钥。");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtTokenProvider 初始化完成：issuer={}, 有效期={} 秒（{} 天）",
                issuer, expirationSeconds, expirationSeconds / 86400.0);
        if (secret.startsWith("CHANGE_ME")) {
            log.warn("检测到 app.jwt.secret 仍为开发占位值，禁止用于生产环境！");
        }
    }

    // =================================================================
    // 生成
    // =================================================================

    /**
     * 生成访问令牌。
     *
     * @param userId   用户 ID
     * @param username 用户名（作为 subject）
     * @param role     角色（USER / ADMIN）
     * @param status   账号状态（ACTIVE / RESTRICTED）
     * @return 已签名的 JWT
     */
    public String generateToken(Long userId, String username, String role, String status) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_STATUS, status)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 令牌有效期（秒）。契约 {@code AuthTokenResponse.expiresIn} 使用该值。
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    /**
     * 由令牌中的过期时间计算绝对到期时刻。
     *
     * @return 到期时刻；解析失败返回 {@code null}
     */
    public Instant getExpiresAt(String token) {
        try {
            Date expiration = parseClaims(token).getBody().getExpiration();
            return expiration == null ? null : expiration.toInstant();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    // =================================================================
    // 解析与校验
    // =================================================================

    /**
     * 校验令牌签名与有效期。
     *
     * @return 合法返回 {@code true}；签名错误、过期、格式错误均返回 {@code false}
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // 过期与非法统一在此降级为 false，由调用方映射为 401
            log.debug("JWT 校验失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 判断令牌是否因**过期**而失效（用于区分 401 的两个错误码）。
     *
     * @return 过期返回 {@code true}
     */
    public boolean isExpired(String token) {
        try {
            Date expiration = parseClaims(token).getBody().getExpiration();
            return expiration != null && expiration.toInstant().isBefore(Instant.now());
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 从令牌解析用户名（subject）。
     *
     * @return 用户名；解析失败返回 {@code null}
     */
    public String getUsername(String token) {
        try {
            return parseClaims(token).getBody().getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 从令牌解析用户 ID。
     *
     * @return 用户 ID；缺失或解析失败返回 {@code null}
     */
    public Long getUserId(String token) {
        try {
            Object raw = parseClaims(token).getBody().get(CLAIM_USER_ID);
            if (raw == null) {
                return null;
            }
            return raw instanceof Number ? ((Number) raw).longValue() : Long.valueOf(raw.toString());
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 从令牌解析角色。
     *
     * @return USER / ADMIN；解析失败返回 {@code null}
     */
    public String getRole(String token) {
        try {
            Object raw = parseClaims(token).getBody().get(CLAIM_ROLE);
            return raw == null ? null : raw.toString();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }

    // =================================================================
    // 从请求头提取令牌
    // =================================================================

    /**
     * 从 {@code Authorization} 请求头提取裸令牌（去掉 {@code Bearer } 前缀）。
     *
     * @param authorizationHeader 请求头原值
     * @return 裸令牌；缺失或格式不符返回 {@code null}
     */
    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String token = authorizationHeader.substring(prefix.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
