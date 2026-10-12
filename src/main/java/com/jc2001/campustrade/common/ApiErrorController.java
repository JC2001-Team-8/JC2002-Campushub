package com.jc2001.campustrade.common;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一错误出口（{@code /error}）。
 *
 * <p><b>为什么需要它</b>：{@link GlobalExceptionHandler} 只能处理**进入 DispatcherServlet 之后**
 * 抛出的异常。以下两类错误**不会**经过它，而是由 Servlet 容器转发到 {@code /error}：
 * <ul>
 *   <li>未匹配到任何 Handler 的请求（未映射路径、静态资源缺失）→ 404</li>
 *   <li>过滤器链中提前返回的错误（部分容器级错误）</li>
 * </ul>
 * 若不处理，这些响应会落到 Spring Boot 默认的 Whitelabel 错误结构
 * （{@code timestamp/status/error/path}），与契约的 {@code ErrorResponse}
 * （{@code code/message/details/traceId/timestamp}）**不一致**。
 *
 * <p>本控制器把 {@code /error} 的响应改写为契约格式，保证**任何**错误响应结构一致。
 *
 * <p>契约依据：{@code openapi.yaml} 的 {@code components.schemas.ErrorResponse}；
 * 错误码取值见 {@code docs/iteration1/design/error-codes.md}。
 * 注意 404 仅用于「资源/路由不存在」，**越权仍由 Service 层返回 403**（E-02 / AD-02）。
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApiErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ApiErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * 把容器级错误转发为契约格式的错误响应。
     */
    @RequestMapping
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        ErrorCode errorCode = mapStatus(status);
        String traceId = TraceIdFilter.currentTraceId(request);

        // 取原始异常消息作为内部参考，但**不直接暴露给前端**（避免泄露实现细节）
        Throwable cause = errorAttributes.getError(new ServletWebRequest(request));
        String message = errorCode.getDefaultMessage();
        if (status == HttpStatus.NOT_FOUND) {
            message = "请求的接口不存在";
        } else if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            message = "请求方法不被支持";
        }

        ErrorResponse body = ErrorResponse.of(errorCode, message, null, traceId);
        if (cause != null) {
            body.setDetails(null);  // 不向客户端暴露内部异常信息；详细堆栈已由日志记录
        }
        return ResponseEntity.status(status)
                .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
                .body(body);
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object raw = request.getAttribute("javax.servlet.error.status_code");
        if (raw instanceof Integer) {
            HttpStatus resolved = HttpStatus.resolve((Integer) raw);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 把 Servlet 状态码映射到契约错误码。
     *
     * <p>TODO 下一步：按 {@code error-codes.md} 细化——同一状态码下不同场景应返回不同
     * {@code DOMAIN_REASON} 码（例如 403 区分「非本人」与「被处罚用户禁写」）。
     */
    private ErrorCode mapStatus(HttpStatus status) {
        switch (status) {
            case BAD_REQUEST:
                return ErrorCode.VALIDATION_ERROR;
            case UNAUTHORIZED:
                return ErrorCode.UNAUTHORIZED;
            case FORBIDDEN:
                return ErrorCode.FORBIDDEN;
            case NOT_FOUND:
                return ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED:
                return ErrorCode.VALIDATION_ERROR;
            case CONFLICT:
                return ErrorCode.CONFLICT;
            default:
                return ErrorCode.INTERNAL_ERROR;
        }
    }
}
