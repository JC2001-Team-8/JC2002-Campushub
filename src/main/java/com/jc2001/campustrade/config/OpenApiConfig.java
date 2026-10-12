package com.jc2001.campustrade.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI 配置（骨架）。
 *
 * <p><b>两套文档并存</b>：
 * <ol>
 *   <li><b>已冻结的业务契约</b>（83 个 operationId）：
 *       {@code docs/iteration1/design/openapi.yaml}，由 {@code pom.xml} 复制为
 *       classpath 资源 {@code openapi/openapi.yaml}，通过
 *       {@link com.jc2001.campustrade.controller.FrozenSpecController} 暴露在
 *       {@code /openapi/openapi.yaml}。
 *       {@code springdoc.swagger-ui.url} 默认指向它，因此
 *       <b>Swagger UI 打开即可看到完整迭代一契约</b>。</li>
 *   <li><b>运行时自动生成的文档</b>：springdoc 扫描
 *       {@code com.jc2001.campustrade.controller} 包生成的少量端点
 *       （骨架阶段只有 HealthController），位于 {@code /v3/api-docs}。</li>
 * </ol>
 *
 * <p><b>本步骤范围</b>：骨架。此处定义的 {@link OpenAPI} Bean 用于
 * 「运行时文档」的元信息与安全方案；业务 DTO/Schema 由下一步按契约补齐。
 *
 * <p><b>契约冻结状态</b>：{@code 1.0.0-frozen-candidate}，
 * 见 {@code docs/iteration1/design/api-freeze.md}。
 */
@Configuration
public class OpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

    @Value("${app.api-version:1.0.0-frozen-candidate}")
    private String apiVersion;

    @Value("${app.iteration:iteration-1}")
    private String iteration;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 运行时文档的元信息与安全方案。
     *
     * <p>注意：真正的业务契约以 {@code /openapi/openapi.yaml} 为准（冻结候选），
     * 本 Bean 只描述骨架自身暴露的端点。
     */
    @Bean
    public OpenAPI campusTradeOpenAPI() {
        log.info("初始化 OpenAPI 文档：version={}, 冻结契约见 /openapi/openapi.yaml", apiVersion);

        return new OpenAPI()
                .info(new Info()
                        .title("校园/社区闲置交易平台 API（运行时文档）")
                        .version(apiVersion)
                        .description(buildDescription())
                        .contact(new Contact().name("接口契约负责人（待填写姓名）"))
                        .license(new License().name("仅用于课程/内部迭代交付")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort + "/api/v1")
                                .description("本地开发（后端工程骨架）")))
                .components(new Components()
                        // 与冻结契约一致的 Bearer JWT 方案
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录成功后签发的访问令牌，有效期 7 天（假设 A-06 / R-01）。"
                                        + "令牌过期返回 401 AUTH_TOKEN_EXPIRED。"
                                        + "【待确认 Q-07】是否需要 refresh token（契约已保留 /auth/refresh）。"))
                        // 内部接口专用凭证（/internal/** 定时任务，AD-18）
                        .addSecuritySchemes("internalToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Token")
                                .description("内部接口专用凭证（不是用户 JWT）。"
                                        + "仅 /internal/** 使用；生产环境应在网关层禁止外部访问该前缀。")));
    }

    private String buildDescription() {
        return String.join("\n",
                "## 迭代一（" + iteration + "）后端工程骨架",
                "",
                "本页为 **运行时自动生成** 的文档，骨架阶段只包含健康检查等少量端点。",
                "",
                "### 完整契约请查看",
                "- **Swagger UI 默认加载**：`/openapi/openapi.yaml` —— 即已冻结的迭代一接口契约",
                "  （83 个 operationId / 13 个模块），来源 `docs/iteration1/design/openapi.yaml`",
                "- 契约状态：**冻结候选（1.0.0-frozen-candidate）**",
                "- 冻结约束：**2026-10-09 之后不允许新增接口**，变更须走变更流程",
                "",
                "### 迭代一明确不做",
                "真实支付（仅模拟付款）、多人交换链、现金补差价、部分接受交换包、",
                "定位/住址、地图导航、跨区交易、站内聊天、推荐算法、第三方登录、物流、退款等。",
                "详见 `docs/iteration1/out-of-scope.md`（18 项）。",
                "",
                "### 全局约定",
                "| 项 | 约定 |",
                "| --- | --- |",
                "| 基础路径 | `/api/v1` |",
                "| 认证 | `Authorization: Bearer <JWT>` |",
                "| 成功响应 | `ApiResponse`：code（固定 `OK`）/ message / data / traceId / timestamp |",
                "| 错误响应 | `ErrorResponse`：code（`DOMAIN_REASON`）/ message / details / traceId / timestamp |",
                "| 分页 | 请求 `page`（0 起）/ `size` / `sort`；响应 `PageResult` |",
                "| 幂等 | 关键写操作要求 `Idempotency-Key` 头（16 个接口必填） |",
                "| 时间 | ISO 8601 UTC；倒计时由服务端计算 |",
                "| 越权 | 返回 **403**（不用 404 掩盖，E-02） |",
                "",
                "### 本步骤未实现",
                "未实现业务逻辑、未生成 83 个业务 Controller。",
                "下一步将按 `docs/iteration1/design/api-contract.md` 逐条生成。");
    }
}
