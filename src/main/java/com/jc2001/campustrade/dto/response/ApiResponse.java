package com.jc2001.campustrade.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 统一响应包络。**所有非 204 响应均使用此结构**。
 *
 * <p>对应契约 {@code components.schemas.ApiResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ApiResponse", description = "统一响应包络。**所有非 204 响应均使用此结构**。")
public class ApiResponse {

    /**
     * 业务返回码。成功固定为 `OK`；失败为 `DOMAIN_REASON` 形式（见 error-codes.md）。
     */
    @NotNull
    @Schema(description = "业务返回码。成功固定为 `OK`；失败为 `DOMAIN_REASON` 形式（见 error-codes.md）。", example = "OK")
    private String code;

    /**
     * 人类可读提示信息（中文）
     */
    @NotNull
    @Schema(description = "人类可读提示信息（中文）", example = "成功")
    private String message;

    /**
     * 业务数据。无数据时为 `null`。
     */
    @Schema(description = "业务数据。无数据时为 `null`。")
    private Object data;

    /**
     * 链路追踪 ID，同时回写在响应头 `X-Trace-Id`
     */
    @Schema(description = "链路追踪 ID，同时回写在响应头 `X-Trace-Id`", example = "7f9c1a2b3d4e5f60")
    private String traceId;

    /**
     * 服务端响应时间（ISO 8601 UTC）
     */
    @NotNull
    @Schema(description = "服务端响应时间（ISO 8601 UTC）")
    private Instant timestamp;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ApiResponse() {
    }

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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
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

    @Override
    public String toString() {
        return "ApiResponse{"
                + "code=" + code + ", "
                + "message=" + message + ", "
                + "data=" + data + ", "
                + "traceId=" + traceId + ", "
                + "timestamp=" + timestamp + ", "
                + '}';
    }
}
