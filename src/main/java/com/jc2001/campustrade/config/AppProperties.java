package com.jc2001.campustrade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用自定义配置（{@code app.*}）的类型安全绑定。
 *
 * <p><b>为什么需要这个类</b>：任务要求「所有占位配置集中，不得散落隐式默认」。
 * 把 {@code app.*} 绑定为强类型对象后，配置项会被 IDE 与
 * {@code spring-boot-configuration-processor} 识别，避免在代码各处用
 * {@code @Value} 散落读取、也避免出现「配置文件写了但代码里是别的默认值」。
 *
 * <p><b>本步骤范围</b>：骨架，只绑定不实现业务；由
 * {@code CampusTradeApplication} 的 {@code @ConfigurationPropertiesScan} 注册。
 *
 * <p><b>全部为占位值</b>，禁止用于生产：生产必须通过环境变量注入
 * （见 {@code README-backend.md} §5）。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 迭代标识，如 {@code iteration-1}。 */
    private String iteration = "iteration-1";

    /** 契约版本（与 openapi.yaml 的 info.version 对齐）。 */
    private String apiVersion = "1.0.0-frozen-candidate";

    private final Jwt jwt = new Jwt();
    private final Internal internal = new Internal();
    private final Cors cors = new Cors();
    private final Storage storage = new Storage();
    private final Pagination pagination = new Pagination();

    public String getIteration() {
        return iteration;
    }

    public void setIteration(String iteration) {
        this.iteration = iteration;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Internal getInternal() {
        return internal;
    }

    public Cors getCors() {
        return cors;
    }

    public Storage getStorage() {
        return storage;
    }

    public Pagination getPagination() {
        return pagination;
    }

    /**
     * JWT 配置。
     *
     * <p>【待确认 Q-07】当前假设仅固定 7 天单令牌（A-06），{@code refreshEnabled=false}，
     * 契约中 {@code /auth/refresh} 保留但返回 501。
     */
    public static class Jwt {
        /** 签名密钥。【占位】生产必须用 ≥32 字节随机值覆盖。 */
        private String secret;
        /** 有效期（秒）。7 天 = 604800（A-06 / R-01）。 */
        private long expirationSeconds = 604800L;
        private String issuer = "campus-trade";
        private String header = "Authorization";
        private String prefix = "Bearer ";
        /** 是否启用 refresh token（当前未启用）。 */
        private boolean refreshEnabled = false;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public boolean isRefreshEnabled() {
            return refreshEnabled;
        }

        public void setRefreshEnabled(boolean refreshEnabled) {
            this.refreshEnabled = refreshEnabled;
        }
    }

    /**
     * 内部接口配置（{@code /internal/**} 定时任务专用，AD-18）。
     */
    public static class Internal {
        /** 内部凭证。【占位】生产必须覆盖；不得下发到浏览器端。 */
        private String token;
        private String header = "X-Internal-Token";
        private String pathPrefix = "/internal";

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }
    }

    /**
     * CORS 配置（前端联调）。
     */
    public static class Cors {
        private boolean enabled = true;
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
        private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private boolean allowCredentials = false;
        private long maxAge = 3600L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * 文件存储配置（OOS-18：本地目录，不使用对象存储/CDN）。
     */
    public static class Storage {
        private String type = "local";
        private String localDir = "./data/uploads";
        private String publicPrefix = "/static/uploads";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public String getPublicPrefix() {
            return publicPrefix;
        }

        public void setPublicPrefix(String publicPrefix) {
            this.publicPrefix = publicPrefix;
        }
    }

    /**
     * 分页默认值（与契约 {@code PageParam} / {@code SizeParam} 一致）。
     */
    public static class Pagination {
        /** 页码从 0 开始。 */
        private int defaultPage = 0;
        private int defaultSize = 20;
        private int maxSize = 100;

        public int getDefaultPage() {
            return defaultPage;
        }

        public void setDefaultPage(int defaultPage) {
            this.defaultPage = defaultPage;
        }

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}
