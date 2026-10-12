package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.TraceIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（**手工维护，非契约生成**）。
 *
 * <p><b>注意</b>：本类不属于已冻结契约的 83 个 operationId，属工程自检补充端点。
 * 契约接口骨架由 {@code tools/generate_api_skeleton.py} 生成，
 * 生成器**不得删除本文件**（已加入生成器保留清单）。
 *
 * <p>与 {@code /actuator/health} 的区别：
 * <ul>
 *   <li>{@code /actuator/health}：Spring Boot 标准健康检查，返回 {@code {"status":"UP"}}，
 *       供部署/编排层使用（Docker HEALTHCHECK 依赖它）</li>
 *   <li>{@code /api/v1/health}（本接口）：返回**契约格式**统一响应包络，
 *       供前端与联调脚本确认「应用 + 数据库 + 迁移」整体就绪</li>
 * </ul>
 */
@Hidden  // 不属于已冻结契约的 83 个 operationId，从 OpenAPI 文档中隐藏
@Tag(name = "00-工程自检", description = "骨架阶段的健康检查（不属于迭代一冻结契约，属工程自检端点）")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.iteration:iteration-1}")
    private String iteration;

    @Value("${app.api-version:1.0.0-frozen-candidate}")
    private String apiVersion;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 应用健康检查（仅应用本身，不触达数据库）。 */
    @Operation(
            summary = "应用健康检查",
            description = "返回统一响应包络，`data` 为 `UP`。不查询数据库。"
                    + "数据库连通性请查 `/actuator/health`。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "应用正常",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health(HttpServletRequest request) {
        String traceId = TraceIdFilter.currentTraceId(request);
        return ResponseEntity.ok(ApiResponse.<String>ok("UP", "服务正常").withTraceId(traceId));
    }

    /** 就绪检查（含数据库连通性与 Flyway 迁移状态）。 */
    @Operation(
            summary = "就绪检查（含数据库与 Flyway）",
            description = "执行 `SELECT 1` 验证数据库连通，并返回 Flyway 迁移版本与成功条数。"
                    + "依赖不可用时 `data.status=DEGRADED`（HTTP 仍为 200，便于排查）。")
    @SecurityRequirements
    @GetMapping("/health/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ready(HttpServletRequest request) {
        String traceId = TraceIdFilter.currentTraceId(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("iteration", iteration);
        data.put("apiVersion", apiVersion);

        boolean dbUp;
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbUp = one != null && one == 1;
        } catch (Exception ex) {
            dbUp = false;
            log.warn("就绪检查：数据库不可达 - {}", ex.getMessage());
            data.put("dbError", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        data.put("database", dbUp ? "UP" : "DOWN");

        if (dbUp) {
            try {
                Map<String, Object> flyway = new LinkedHashMap<>();
                Integer applied = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
                String currentVersion = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(max(version), 'none') FROM flyway_schema_history WHERE success = true",
                        String.class);
                flyway.put("appliedCount", applied);
                flyway.put("currentVersion", currentVersion);
                flyway.put("expectedCount", 4);
                data.put("flyway", flyway);
            } catch (Exception ex) {
                log.warn("就绪检查：无法读取 flyway_schema_history - {}", ex.getMessage());
                data.put("flyway", "UNKNOWN: " + ex.getClass().getSimpleName());
            }
        }

        data.put("status", dbUp ? "UP" : "DEGRADED");
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>ok(data, "就绪检查完成").withTraceId(traceId));
    }
}
