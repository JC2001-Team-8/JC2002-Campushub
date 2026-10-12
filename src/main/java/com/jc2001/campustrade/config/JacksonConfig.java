package com.jc2001.campustrade.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Jackson 序列化配置。
 *
 * <p><b>契约依据</b>（{@code api-contract.md} §5 / AD-05）：
 * <ul>
 *   <li>时间统一 <b>ISO 8601 UTC</b>，形如 {@code 2026-10-12T13:20:41Z}</li>
 *   <li>金额使用 {@code DecimalString}（十进制字符串），**不得**输出为浮点数（R-13）</li>
 *   <li>成功响应 {@code code} 固定为 {@code OK}；业务失败用非 2xx + {@code ErrorResponse}</li>
 * </ul>
 *
 * <p><b>本步骤范围</b>：骨架，仅统一时间与时区行为。
 */
@Configuration
public class JacksonConfig {

    /**
     * 定制 Spring Boot 默认的 ObjectMapper。
     *
     * <p>与 {@code application.yml} 中 {@code spring.jackson.*} 互补：
     * 此处用代码显式声明**必须成立**的行为，避免仅依赖配置文件。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Java 8 时间类型支持（Instant / LocalDateTime 等）
            builder.modules(new JavaTimeModule());
            // 时间输出为 ISO 8601 字符串，而不是时间戳数字
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 反序列化时忽略未知字段（前端多传字段不应导致 400）
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // null 字段不输出（与契约中「无数据时 data 不出现」一致）
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            // 统一 UTC，保证时间格式带 Z 后缀（服务端时间为准，R-19）
            builder.timeZone(TimeZone.getTimeZone("UTC"));
        };
    }

    // =================================================================
    // TODO（下一步实现）
    //   1. 金额序列化：为 numeric(12,2) 对应的金额字段注册序列化器，
    //      输出为 DecimalString（字符串，2 位小数），禁止 double 精度损失（R-13）。
    //      契约中 estimatedPrice / estimatedTotal / *_snapshot 均为 DecimalString。
    //   2. 枚举序列化：确认按枚举名（大写）输出，与 V1__init_schema.sql 的
    //      PostgreSQL ENUM 取值逐字一致（api-contract.md AD-09）。
    //   3. 分页字段名对齐：PageResult 的 content / totalElements 与契约统一
    //      （见 common/PageResult.java 的待确认说明）。
    // =================================================================

    /**
     * 便捷方法：创建与 Spring 一致的 ObjectMapper（离线/测试场景可用）。
     */
    public static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        return mapper;
    }
}
