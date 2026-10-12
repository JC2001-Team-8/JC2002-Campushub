package com.jc2001.campustrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 校园/社区闲置交易平台 —— 应用入口（迭代一后端工程骨架）。
 *
 * <p><b>本步骤范围</b>：只搭工程骨架。已包含统一响应、全局异常处理、Security/JWT 骨架、
 * Flyway 配置、Swagger UI、Actuator 健康检查。
 *
 * <p><b>明确不做</b>：
 * <ul>
 *   <li>不实现业务逻辑（无 Service/Repository 实现）</li>
 *   <li>不生成 83 个 operationId 对应的业务 Controller（下一步按
 *       {@code docs/iteration1/design/openapi.yaml} 生成）</li>
 *   <li>不写前端代码</li>
 * </ul>
 *
 * <p><b>启动后验证</b>：
 * <pre>
 *   curl http://localhost:8080/actuator/health   → {"status":"UP"}
 *   curl http://localhost:8080/api/v1/health     → 统一响应包络
 *   http://localhost:8080/swagger-ui.html        → 展示已冻结的接口契约
 * </pre>
 *
 * @see com.jc2001.campustrade.config.SecurityConfig
 * @see com.jc2001.campustrade.config.OpenApiConfig
 * @see com.jc2001.campustrade.common.GlobalExceptionHandler
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
