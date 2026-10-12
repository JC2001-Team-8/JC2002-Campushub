package com.jc2001.campustrade.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 统一错误响应体（HTTP 状态码非 2xx 时返回）
 *
 * <p>对应契约 {@code components.schemas.ErrorResponse}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "ErrorResponse", description = "统一错误响应体（HTTP 状态码非 2xx 时返回）")
public class ErrorResponse {

    /**
     * 错误码，格式 `DOMAIN_REASON`，例如 `ITEM_ALREADY_RESERVED`
     */
    @NotNull
    @Schema(description = "错误码，格式 `DOMAIN_REASON`，例如 `ITEM_ALREADY_RESERVED`", example = "ITEM_ALREADY_RESERVED")
    private String code;

    /**
     * 面向用户的中文错误描述
     */
    @NotNull
    @Schema(description = "面向用户的中文错误描述", example = "商品已被其他订单占用")
    private String message;

    /**
     * 错误细节。结构随错误码变化，常见键：
     * `field` / `issue`（校验类）、`currentStatus`（状态冲突）、
     * `currentVersion`（乐观锁）、`restrictedUntil`（处罚）、`min` / `max`（范围）。
     * 
     */
    @Schema(description = "错误细节。结构随错误码变化，常见键： `field` / `issue`（校验类）、`currentStatus`（状态冲突）、 `currentVersion`（乐观锁）、`restrictedUntil`（处罚）、`min` / `max`（范围）。")
    private Object details;

    @Schema(description = "traceId", example = "7f9c1a2b3d4e5f60")
    private String traceId;

    @NotNull
    @Schema(description = "timestamp")
    private Instant timestamp;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public ErrorResponse() {
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

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
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
        return "ErrorResponse{"
                + "code=" + code + ", "
                + "message=" + message + ", "
                + "details=" + details + ", "
                + "traceId=" + traceId + ", "
                + "timestamp=" + timestamp + ", "
                + '}';
    }
}
