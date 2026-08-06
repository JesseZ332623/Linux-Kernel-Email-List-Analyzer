package com.jesse.analyze_report_discuss.controller;

import com.jesse.analyze_report_discuss.exception.DiscussException;
import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/** 内核邮件分析报告讨论控制器。*/
@Slf4j
@RestController
@RequestMapping(path = "/api/analyze-report")
@RequiredArgsConstructor
public class AnalyzeReportDiscussController
{
    /** 内核邮件分析报告讨论服务接口。*/
    private final
    AnalyzeReportDiscussService analyzeReportDiscussService;

    /** {@link SseEmitter} 的自定义构造方法。*/
    private static SseEmitter
    newSseEmitter(ReportDiscussRequest request, long startTimestamp)
    {
        // 设置超时（0 = 永不超时，根据业务调整）
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(
            () -> log.debug(
                "SSE completed. (request: {}, duration: {} ms)",
                request.toString(),
                System.currentTimeMillis() - startTimestamp
            )
        );

        // emitter.onTimeout();

        emitter.onError((exception) ->
            log.error("SEE error. (request: {}, duration: {} ms)",
                request.toString(),
                System.currentTimeMillis() - startTimestamp,
                exception
            )
        );

        return emitter;
    }

    /**
     * 处理异步响应处理回调开始前所抛出的异常
     *（比如锁占用，提示词文件不存在等）。
     */
    private void
    handleSyncError(SseEmitter emitter, DiscussException discussException)
    {
        try
        {
            final Map<String, String> errorData
                = Map.of(
                    "type", "Discuss Error",
                    "message", discussException.getMessage()
                );

            final SseEmitter.SseEventBuilder errorEvent
                = SseEmitter.event()
                    .name("error")
                    .data(errorData);

            emitter.send(errorEvent);
            emitter.complete();
        }
        catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    /** 在某个分析报告的会话下发起一次讨论。*/
    @PostMapping(path = "/discuss")
    public SseEmitter
    analyzeReportDiscuss(@RequestBody ReportDiscussRequest request)
    {
        final long timestamp        = System.currentTimeMillis();
        final SseEmitter sseEmitter = newSseEmitter(request, timestamp);

        try {
            this.analyzeReportDiscussService.discuss(request, sseEmitter);
        }
        catch (DiscussException discussException) {
            this.handleSyncError(sseEmitter, discussException);
        }
        catch (Exception exception)
        {
            log.error(
                "Unexcepted error during discuss initialization.",
                exception
            );

            this.handleSyncError(
                sseEmitter,
                new DiscussException("Internal server error")
            );
        }

        return sseEmitter;
    }
}