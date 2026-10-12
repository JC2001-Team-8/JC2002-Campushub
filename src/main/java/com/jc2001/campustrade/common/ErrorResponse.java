package com.jc2001.campustrade.common;

import java.time.Instant;

/**
 * 统一错误响应体。
 *
 * <p>与接口契约 {@code openapi.yaml} 的 {@code components.schemas.ErrorResponse}
 * **字段一一对应**：{@code code} / {@code message} / {@code details} / {@code traceId} / {@code timestamp}。
 *
 * <p>HTTP 状态码非 2xx 时返回本结构 {@code details} 的结构随 {@code code} 变化，常见键：
 * <ul>
 *   <li>校验类：{@code field} / {@code issue}</li>
 *   <li>状态冲突：{@code currentStatus}</li>
 *   <li>乐观锁：{@code currentVersion}</li>
 *   <li>处罚：{@code restrictedUntil} / {@code penaltyId}</li>
 *   <li>范围类：{@code min} / {@code max} / {@code currentCount}</li>
 * </ul>
 *
 * @see ErrorCode
 * @see GlobalExceptionHandler
 */
public class ErrorResponse {

    /** 错误码，格式 {@code DOMAIN_REASON}，如 {@code ITEM_ALREADY_RESERVED}。 */
    private String code;

    /** 面向用户的中文错误描述。 */
    private String message;

    /** 错误细节。结构随错误码变化；无细节时为 {@code null}。 */
    private Object details;

    /** 链路追踪 ID。 */
    private String traceId;

    /** 服务端响应时间（ISO 8601 UTC）。 */
    private Instant timestamp;

    public ErrorResponse() {
        // Jackson 反序列化用
    }

    public ErrorResponse(String code, String message, Object details, String traceId) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    // -----------------------------------------------------------------
    // 静态工厂
    // -----------------------------------------------------------------

    /**
     * 由 {@link ErrorCode} 构造错误响应（使用其默认文案）。
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getDefaultMessage(), null, traceId);
    }

    /**
     * 由 {@link ErrorCode} 构造错误响应（覆盖文案）。
     */
    public static ErrorResponse of(ErrorCode errorCode, String message, String traceId) {
        return new ErrorResponse(errorCode.getCode(), message, null, traceId);
    }

    /**
     * 由 {@link ErrorCode} 构造带细节的错误响应。
     */
    public static ErrorResponse of(ErrorCode errorCode, String message, Object details, String traceId) {
        return new ErrorResponse(errorCode.getCode(), message, details, traceId);
    }

    /**
     * 构造自定义错误码的错误响应（用于契约中已冻结、但尚未加入 {@link ErrorCode} 枚举的码）。
     *
     * <p><b>注意</b>：{@code code} 必须与 {@code docs/iteration1/design/error-codes.md}
     * 中已冻结的码逐字一致，**禁止自行发明**新码名。
     */
    public static ErrorResponse ofRaw(String code, String message, Object details, String traceId) {
        return new ErrorResponse(code, message, details, traceId);
    }

    // -----------------------------------------------------------------
    // getter / setter
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
}
