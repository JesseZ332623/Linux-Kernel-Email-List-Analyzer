package com.jesse.core.exception.handler;

import com.jesse.core.response.CustomizedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Objects;

/** 项目控制器类异常处理器。*/
@Slf4j
@RestControllerAdvice(basePackages = {
    "com.jesse.analyzer.controller",
    "com.jesse.analyze_report_discuss.controller"
})
public class GlobalExceptionHandler
{
    /** 判断一个请求是否为 SSE 请求。*/
    private boolean
    isEventStreamRequest(HttpServletRequest request)
    {
        final String acceptType
            = request.getHeader("Accept");

        return Objects.nonNull(acceptType) && acceptType.contains("text/event-stream");
    }

    /**
     * 该异常通常表示：
     * “服务器在异步处理请求时，发现用来返回数据的"通道"（即响应对象）已经不能用了”，
     * 有以下几种可能会抛出：
     *
     * <ul>
     *     <li>（最常见的）客户端提前断开连接</li>
     *     <li>异步请求超时</li>
     *     <li>步处理完成后继续写入</li>
     *     <li>I/O 写入失败</li>
     * </ul>
     *
     * 像这样的异常抛出不代表服务本身的问题，所以只需要打一条警告的日志即可。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequetNotUsableException(
        final AsyncRequestNotUsableException exception
    )
    {
        log.warn(
            "Async request not usable. Caused by: {}",
            exception.getMessage()
        );
    }

    /** 兜底的通用异常处理。*/
    @ExceptionHandler(Exception.class)
    public CustomizedResponse<Object>
    handleException(
        final HttpServletRequest  request,
        final HttpServletResponse response,
        final Exception           exception
    )
    {
        log.error("System error.", exception);

        if (this.isEventStreamRequest(request)) {
            return null;
        }

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.INTERNAL_SERVER_ERROR,
            "System error.",
            ""
        );
    }
}