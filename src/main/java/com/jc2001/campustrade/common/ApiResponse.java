package com.jc2001.campustrade.common;

import java.time.Instant;

/**
 * 统一成功响应包络。
 *
 * <p>与接口契约 {@code docs/iteration1/design/openapi.yaml} 的
 * {@code components.schemas.ApiResponse} **字段一一对应**：
 * {@code code} / {@code message} / {@code data} / {@code traceId} / {@code timestamp}。
 *
 * <p><b>契约约定</b>（api-contract.md AD-01）：成功响应的 {@code code} **固定为 {@code "OK"}**；
 * 业务失败一律使用非 2xx HTTP 状态码 + {@link ErrorResponse}，**不混用**
 * 「HTTP 200 + 业务错误码」模式。
 *
 * <p><b>时间格式</b>（AD-05）：{@code timestamp} 为 ISO 8601 UTC，如
 * {@code 2026-10-12T13:20:41Z}，由 {@link com.jc2001.campustrade.config.JacksonConfig} 统一序列化。
 *
 * @param <T> 业务数据类型；无数据时为 {@code null}
 */
public class ApiResponse<T> {

    /** 业务返回码。成功固定为 {@code "OK"}。 */
    private String code;

    /** 人类可读提示信息（中文）。 */
    private String message;

    /** 业务数据。无数据时该字段不输出（Jackson non_null）。 */
    private T data;

    /** 链路追踪 ID，同时回写在响应头 {@code X-Trace-Id}。 */
    private String traceId;

    /** 服务端响应时间（ISO 8601 UTC）。 */
    private Instant timestamp;

    public ApiResponse() {
        // Jackson 反序列化用
    }

    public ApiResponse(String code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    // -----------------------------------------------------------------
    // 静态工厂：成功响应
    // -----------------------------------------------------------------

    /**
     * 构造成功响应（code = {@code OK}）。
     *
     * @param data 业务数据，可为 {@code null}
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getDefaultMessage(), data, null);
    }

    /**
     * 构造成功响应（带自定义提示信息）。
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(ErrorCode.OK.getCode(), message, data, null);
    }

    /**
     * 构造无业务数据的成功响应。
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getDefaultMessage(), null, null);
    }

    // -----------------------------------------------------------------
    // 链式补充 traceId（由 GlobalExceptionHandler / Controller 统一填充）
    // -----------------------------------------------------------------

    /**
     * 填充 traceId 并返回自身，便于链式调用。
     * <p>若 {@code timestamp} 为空（如反序列化场景）则一并补齐。
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
        return this;
    }

    // -----------------------------------------------------------------
    // getter / setter（不引入 Lombok，保持零注解处理依赖）
    // -----------------------------------------------------------------

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
