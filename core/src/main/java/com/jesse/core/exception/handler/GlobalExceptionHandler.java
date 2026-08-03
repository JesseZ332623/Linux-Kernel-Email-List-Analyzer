package com.jesse.core.exception.handler;

import com.jesse.core.response.CustomizedResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 项目控制器类异常处理器。*/
@Slf4j
@RestControllerAdvice(basePackages = {
    "com.jesse.analyzer.controller",
    "com.jesse.analyze_report_discuss.controller"
})
public class GlobalExceptionHandler
{
    @ExceptionHandler(Exception.class)
    public CustomizedResponse<Object>
    handleException(
        final HttpServletResponse response,
        final Exception           exception
    )
    {
        log.error("System error.", exception);

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.INTERNAL_SERVER_ERROR,
            "System error.",
            ""
        );
    }
}