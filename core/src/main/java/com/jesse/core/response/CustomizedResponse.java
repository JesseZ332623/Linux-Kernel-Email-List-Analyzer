package com.jesse.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 自定义通用响应体。*/
@Getter
@Builder
@RequiredArgsConstructor
public class CustomizedResponse<T>
{
    private final long timestamp = System.currentTimeMillis();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> CustomizedResponse<T>
    responseOf(
        final HttpServletResponse response,
        final HttpStatus         status,
        final String             message,
        final T                  data
    )
    {
        response.setStatus(status.value());
        return new CustomizedResponse<>(message, data);
    }
}
