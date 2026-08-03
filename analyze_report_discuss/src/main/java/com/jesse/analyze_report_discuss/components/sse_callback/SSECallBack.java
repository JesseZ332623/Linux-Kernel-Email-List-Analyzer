package com.jesse.analyze_report_discuss.components.sse_callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyze_report_discuss.components.discuss_abstract.AnalyzeReportDiscussAbstractor;
import com.jesse.analyze_report_discuss.request.DiscussAbstractReqest;
import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionDetailsService;
import com.jesse.core.pojo.ai.AIModelAnswerChoice;
import com.jesse.core.pojo.ai.AIModelAnswerMessage;
import com.jesse.core.pojo.ai.AIModelAnswerUsage;
import com.jesse.core.pojo.ai.sse.AIModelAnswerMessageBySSE;
import com.jesse.core.response.AIModelAnswerResponse;
import com.jesse.core.response.base.AIModelAnswerBaseResponse;
import com.jesse.core.response.sse.AIModelAnswerSSEResponse;
import com.jesse.response_audit.service.AIModelAnswerAuditService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static java.lang.String.format;

/** AI 模型 SSE 协议响应流处理回调实现。*/
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SSECallBack implements Callback
{
    /** 响应数据片前缀。*/
    private static final
    String RESPONSE_LINE_PREFIX = "data: ";

    /** 响应结束标记。*/
    private static final
    String RESPONSE_DONE = "[DONE]";

    /** 会话明细 ID */
    private Long sessionDetailId;

    /** 对某个分析报告发起讨论的请求体。*/
    final ReportDiscussRequest discussRequest;

    /** SSE 事件发射器，由控制器方法完成构造并传入。*/
    private final SseEmitter sseEmitter;

    /** 通用对象映射器。*/
    private final ObjectMapper objectMapper;

    /** AI 模型响应审计表服务类接口。*/
    private final
    AIModelAnswerAuditService aiModelAnswerAuditService;

    /** Linux 内核邮件分析报告疑惑解答会话对话内容表服务接口。*/
    private final
    AnalyzeReportDiscussSessionDetailsService discussSessionDetailsService;

    /** 分析报告讨论上下文摘要器接口。*/
    private final
    AnalyzeReportDiscussAbstractor analyzeReportDiscussAbstractor;

    /** SSE 响应数据片处理器。*/
    private final
    ResponseChunkHandler responseChunkHandler;

    /** 每次发起 Stream 模式调用的时候，构造本回调的实例。*/
    public static SSECallBack create(
        final Long                 sessionDetailId,
        final ReportDiscussRequest discussRequest,
        final SseEmitter           sseEmitter,
        final ObjectMapper         objectMapper,
        final AnalyzeReportDiscussSessionDetailsService discussSessionDetailsService,
        final AIModelAnswerAuditService aiModelAnswerAuditService,
        final AnalyzeReportDiscussAbstractor analyzeReportDiscussAbstractor
    )
    {
        final ResponseChunkHandler responseChunkHandler
            = ResponseChunkHandler.create(sseEmitter);

        return new SSECallBack(
            sessionDetailId,
            discussRequest,
            sseEmitter,
            objectMapper,
            aiModelAnswerAuditService,
            discussSessionDetailsService,
            analyzeReportDiscussAbstractor,
            responseChunkHandler
        );
    }

    /** 数据片是否是 "data: " 开头？*/
    private static boolean
    startWithData(String line) {
        return line.startsWith(RESPONSE_LINE_PREFIX);
    }

    /** 提取数据片 JSON 文本 */
    private static String
    extractChunkJsonText(String line) {
        return line.substring(RESPONSE_LINE_PREFIX.length());
    }

    /** 基于响应体字节流构造 BufferedReader。*/
    private static BufferedReader
    makeBufferedReader(ResponseBody body) {
        return new BufferedReader(new InputStreamReader(body.byteStream()));
    }

    /** 聚合完整的 SSE 响应体数据片，用于本次调用的审计信息持久化。*/
    private Optional<AIModelAnswerResponse>
    agregateResponseChunks(List<AIModelAnswerSSEResponse> chunks)
    {
        if (CollectionUtils.isEmpty(chunks))
        {
            log.warn("Upstream did not return any chunks, skip.");
            return Optional.empty();
        }

        // 从第一个数据片中就能拿到固定的数据信息
        final AIModelAnswerSSEResponse firstChunk = chunks.getFirst();

        // 最后一个数据片提供 Token 消耗的明细
        final AIModelAnswerSSEResponse lastChunk  = chunks.getLast();

        final StringBuilder content       = new StringBuilder();
        final StringBuilder reasonContent = new StringBuilder();

        for (AIModelAnswerSSEResponse chunk : chunks)
        {
            final AIModelAnswerMessageBySSE answerMessage
                = chunk.getChoices().getFirst().getDelta();

            if (Objects.nonNull(answerMessage.getReasonContent())) {
                reasonContent.append(answerMessage.getReasonContent());
            }

            if (Objects.nonNull(answerMessage.getContent())) {
                content.append(answerMessage.getContent());
            }
        }

        // 对象类型，固定为 "chat.completion.chunks"
        final String taskId = firstChunk.getId();
        final String object = firstChunk.getObject() + "s";
        final Long created  = firstChunk.getCreated();
        final String model  = firstChunk.getModel();
        final String systemFingerPrint = firstChunk.getSystemFingerPrint();
        final AIModelAnswerUsage usage = lastChunk.getUsage();

        final AIModelAnswerMessage aiModelAnswerMessage
            = new AIModelAnswerMessage(
                "assistant",
                content.toString(), reasonContent.toString()
        );

        final AIModelAnswerChoice aiModelAnswerChoice
            = new AIModelAnswerChoice(
                0, aiModelAnswerMessage, null, "stop"
            );

        final AIModelAnswerResponse auditResponse
            = new AIModelAnswerResponse();

        auditResponse.setId(taskId);
        auditResponse.setObject(object);
        auditResponse.setModel(model);
        auditResponse.setCreated(created);
        auditResponse.setChoices(List.of(aiModelAnswerChoice));
        auditResponse.setUsage(usage);
        auditResponse.setSystemFingerPrint(systemFingerPrint);

        return Optional.of(auditResponse);
    }

    /** 解析数据片 JSON 为指定的 POJO。*/
    private AIModelAnswerBaseResponse
    parseResponseLine(String eventData)
    {
        if (RESPONSE_DONE.equals(eventData.trim())) {
            return null;
        }

        try
        {
            return
            this.objectMapper
                .readValue(eventData, AIModelAnswerSSEResponse.class);
        }
        catch (JsonProcessingException exception)
        {
            try
            {
                return
                this.objectMapper
                    .readValue(eventData, AIModelAnswerResponse.class);
            }
            catch (JsonProcessingException exception2)
            {
                log.error("Parse response line {} failed.", eventData, exception2);
                return null;
            }
        }
    }

    /** 上游传输时出错则调用本方法，向前端推送错误事件。*/
    @Override
    public void
    onFailure(@NotNull Call call, @NotNull IOException exception)
    {
        try
        {
            final SseEmitter.SseEventBuilder errorEvent
                = SseEmitter.event()
                    .name("error")
                    .data(format("Upstream failed: %s", exception.getMessage()));

            this.sseEmitter.send(errorEvent);

            this.sseEmitter.completeWithError(exception);
        }
        catch (IOException ioException) {
            this.sseEmitter.completeWithError(ioException);
        }

        final Request request = call.request();

        log.error(
            "Read SSE response data failed. URL: {}, Method: {}",
            request.url(), request.method(), exception
        );
    }

    /** 本次对话完成后需要在后台执行的审计与上下文摘要任务。*/
    private void
    auditAndAbstract(AIModelAnswerResponse agregatedResponse)
    {
        log.debug("Model response id = {}", agregatedResponse.getId());

        // (1) 审计大模型调用信息
        this.aiModelAnswerAuditService.save(agregatedResponse);

        // (2) 将会话下的某个对话记录与大模型回复关联，代表大模型已经回答了这个问题
        this.discussSessionDetailsService
            .updateModelResponseIdBySessionId(
                this.sessionDetailId,
                this.discussRequest.getSessionId(),
                agregatedResponse.getId()
            );

        // (3) 摘要并缓存本次回答信息
        this.analyzeReportDiscussAbstractor.discussAbstract(
            new DiscussAbstractReqest(
                this.discussRequest.getTaskId(),
                this.discussRequest.getSessionId(),
                agregatedResponse.getId(),
                this.discussRequest.getQuestion()
            )
        );
    }

    /**
     * 正常处理 SSE 协议响应的逻辑，
     * 向前端推送推理和回复信息，最终聚合起来审计。
     */
    @Override
    public void
    onResponse(@NotNull Call call, @NotNull Response response) throws IOException
    {
        final Request request = call.request();

        log.debug("HTTP Protocol: {}", response.protocol());
        log.debug(
            "Start read SSE response data. URL: {}, Method: {}",
            request.url(), request.method()
        );

        try (ResponseBody body = response.body())
        {
            if (!response.isSuccessful())
            {
                this.responseChunkHandler.handleFailedResponse(response);
                return;
            }

            final List<AIModelAnswerSSEResponse> chunks = new ArrayList<>();

            try (var bufferedReader = makeBufferedReader(body))
            {
                bufferedReader
                    .lines()
                    .filter(SSECallBack::startWithData)
                    .map(SSECallBack::extractChunkJsonText)
                    .map(this::parseResponseLine)
                    .filter(Objects::nonNull)
                    .map(this.responseChunkHandler::handleResponseChunk)
                    .filter(Objects::nonNull)
                    .forEach(chunks::add);
            }

            this.sseEmitter.complete();

            // 收集完所有的响应数据片后，再统一作审计信息持久化。
            this.agregateResponseChunks(chunks)
                .ifPresent(this::auditAndAbstract);
        }
    }
}