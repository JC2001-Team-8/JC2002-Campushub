package com.jc2001.campustrade.common;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器：为每个请求确定 {@code traceId} 并贯穿响应体、响应头与日志。
 *
 * <p><b>行为</b>：
 * <ol>
 *   <li>请求头带 {@code X-Trace-Id} 时复用之（便于前端/网关串联）；否则生成新的 16 位十六进制 ID</li>
 *   <li>写入 SLF4J {@link MDC}，日志格式可引用 {@code %X{traceId}}</li>
 *   <li>放入 request attribute，供 {@link ApiResponse} / {@link ErrorResponse} 填充</li>
 *   <li>回写响应头 {@code X-Trace-Id}，与响应体 {@code traceId} 保持一致</li>
 * </ol>
 *
 * <p><b>契约依据</b>：{@code openapi.yaml} 的复用参数 {@code TraceIdHeader} 与
 * {@code ApiResponse.traceId} / {@code ErrorResponse.traceId}。
 *
 * <p><b>顺序</b>：设为最高优先级，确保后续所有过滤器（含 Security 链）与业务代码都能取到 traceId。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    /** 请求/响应头名称（与契约 {@code TraceIdHeader} 一致）。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** request attribute 键。 */
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    /** MDC 键。 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final int MAX_TRACE_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理，避免线程复用造成 traceId 串号
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 从当前请求解析 traceId：优先复用请求头，其次生成。
     *
     * @return 非空的 traceId
     */
    public static String currentTraceId(HttpServletRequest request) {
        if (request != null) {
            Object attr = request.getAttribute(TRACE_ID_ATTRIBUTE);
            if (attr instanceof String && !((String) attr).isEmpty()) {
                return (String) attr;
            }
        }
        String fromMdc = MDC.get(TRACE_ID_MDC_KEY);
        return fromMdc != null && !fromMdc.isEmpty() ? fromMdc : newTraceId();
    }

    /** 生成新的 traceId（16 位十六进制）。 */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header != null) {
            String trimmed = header.trim();
            // 只接受合理长度与可见字符的调用方值，避免注入与超长值
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_TRACE_ID_LENGTH
                    && trimmed.chars().allMatch(c -> c > 32 && c < 127)) {
                return trimmed;
            }
        }
        return newTraceId();
    }
}
