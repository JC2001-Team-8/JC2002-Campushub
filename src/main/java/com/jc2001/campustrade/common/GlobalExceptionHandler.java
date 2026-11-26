package com.jc2001.campustrade.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理器。
 *
 * <p>把各类异常统一转换为 {@link ErrorResponse}，HTTP 状态码与
 * {@code docs/iteration1/design/error-codes.md} §1.1 的语义分区对齐。
 *
 * <p><b>关键约定</b>：
 * <ul>
 *   <li>越权（非本人）返回 <b>403</b>，**不用 404** 掩盖（api-contract.md AD-02 / E-02）</li>
 *   <li>参数校验失败返回 <b>400</b>，{@code details} 为字段级列表（field / issue）</li>
 *   <li>状态/并发冲突返回 <b>409</b>，{@code details.currentStatus} 供前端刷新</li>
 *   <li>所有响应携带 {@code traceId}，与响应头 {@code X-Trace-Id} 一致</li>
 * </ul>
 *
 * <p><b>本步骤范围</b>：只处理通用异常。业务域异常（如 {@code ItemAlreadyReservedException}）
 * 与对应的域错误码在下一步按 {@code error-codes.md} 补齐。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code, ex.getMessage(), ex.getDetails(), TraceIdFilter.currentTraceId(request)));
    }

    // =================================================================
    // 400 —— 语法/格式错误
    // =================================================================

    /**
     * {@code @Valid} 校验失败（请求体 DTO）。
     * <p>返回 400 + {@code VALIDATION_INVALID_PARAMETER}，details 为字段级列表。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fe.getField());
            item.put("issue", fe.getDefaultMessage());
            fieldErrors.add(item);
        }
        // 类级（全局）校验错误
        ex.getBindingResult().getGlobalErrors().forEach(ge -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", ge.getObjectName());
            item.put("issue", ge.getDefaultMessage());
            fieldErrors.add(item);
        });
        log.warn("[{}] 参数校验失败: {}", TraceIdFilter.currentTraceId(request), fieldErrors);
        return build(ErrorCode.VALIDATION_ERROR, "参数校验失败", fieldErrors, request);
    }

    /**
     * 方法参数级约束（{@code @Validated} + {@code @RequestParam} 等）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", String.valueOf(cv.getPropertyPath()));
            item.put("issue", cv.getMessage());
            fieldErrors.add(item);
        }
        log.warn("[{}] 参数约束失败: {}", TraceIdFilter.currentTraceId(request), fieldErrors);
        return build(ErrorCode.VALIDATION_ERROR, "参数校验失败", fieldErrors, request);
    }

    /**
     * 缺少必填请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("field", ex.getParameterName());
        detail.put("issue", "缺少必填参数");
        return build(ErrorCode.VALIDATION_ERROR, "参数校验失败", List.of(detail), request);
    }

    /**
     * 参数类型不匹配（如把字符串传给 int）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest request) {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("field", ex.getName());
        detail.put("issue", "参数类型不正确");
        return build(ErrorCode.VALIDATION_ERROR, "参数校验失败", List.of(detail), request);
    }

    /**
     * 请求体不可读（JSON 语法错误、枚举值非法等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        log.warn("[{}] 请求体不可读: {}", TraceIdFilter.currentTraceId(request), ex.getMessage());
        return build(ErrorCode.VALIDATION_ERROR, "请求体格式不正确", null, request);
    }

    // =================================================================
    // 401 / 403 —— 认证与授权
    // =================================================================

    /**
     * 未认证（E-01）。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                             HttpServletRequest request) {
        log.warn("[{}] 未认证: {}", TraceIdFilter.currentTraceId(request), ex.getMessage());
        return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage(), null, request);
    }

    /**
     * 已认证但无权限（E-02 非本人 / 非管理员 / P-00-A 管理员参与交易）。
     *
     * <p><b>刻意不用 404</b>：E-02 要求越权返回 403，避免用 404 掩盖越权造成语义混淆。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        log.warn("[{}] 拒绝访问: {}", TraceIdFilter.currentTraceId(request), ex.getMessage());
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), null, request);
    }

    // =================================================================
    // 404 —— 资源不存在
    // =================================================================

    /**
     * JPA 实体不存在。
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex,
                                                             HttpServletRequest request) {
        log.warn("[{}] 资源不存在: {}", TraceIdFilter.currentTraceId(request), ex.getMessage());
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage(), null, request);
    }

    /**
     * 路由不存在。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, "请求的接口不存在", null, request);
    }

    /**
     * HTTP 方法不支持 → 归入 400（语义为「请求方式不正确」）。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, "请求方法不被支持", null, request);
    }

    // =================================================================
    // 409 —— 数据完整性 / 并发冲突
    // =================================================================

    /**
     * 数据库完整性约束冲突。
     *
     * <p>骨架阶段尚无实体与仓储，此处理器预留给下一步：
     * 需要把**部分唯一索引冲突（SQLState 23505）**映射为对应域的 409 错误码，而不是 500。
     * 典型映射（见 {@code idempotency-and-concurrency.md} §7）：
     * <ul>
     *   <li>{@code uq_item_occ_active_item} → {@code ITEM_ALREADY_RESERVED} / {@code SWAP_ITEM_OCCUPIED}</li>
     *   <li>{@code uq_swap_bundle_items_active} → {@code SWAP_ITEM_IN_ACTIVE_BUNDLE}</li>
     *   <li>{@code uq_penalties_one_active} → {@code GOVERNANCE_ACTIVE_PENALTY_EXISTS}</li>
     *   <li>{@code uq_reports_pending_reporter_item} → {@code REPORT_ALREADY_EXISTS}</li>
     *   <li>{@code uq_idempotency_user_key} → 幂等键冲突（E-16）</li>
     * </ul>
     * TODO 下一步：按约束名（{@code PSQLException.getServerErrorMessage().getConstraint()}）
     *      分发到具体域错误码。
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("[{}] 数据完整性冲突: {}", TraceIdFilter.currentTraceId(request), ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICT, "数据冲突，请刷新后重试", null, request);
    }

    /**
     * 乐观锁失败（JPA {@code @Version} 冲突）→ 409（R-18）。
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("[{}] 乐观锁冲突: {}", TraceIdFilter.currentTraceId(request), ex.getMessage());
        return build(ErrorCode.CONFLICT, ErrorCode.CONFLICT.getDefaultMessage(), null, request);
    }

    // =================================================================
    // 500 —— 兜底
    // =================================================================

    /**
     * 未预期异常兜底。**必须记录完整堆栈**并返回 traceId 便于定位。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("[{}] 未预期异常: {}", TraceIdFilter.currentTraceId(request), ex.getMessage(), ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null, request);
    }

    // =================================================================
    // 内部工具
    // =================================================================

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String message, Object details,
                                                HttpServletRequest request) {
        String traceId = TraceIdFilter.currentTraceId(request);
        ErrorResponse body = ErrorResponse.of(errorCode, message, details, traceId);
        HttpStatus status = errorCode.getHttpStatus();
        return ResponseEntity.status(status)
                .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
                .body(body);
    }
}
