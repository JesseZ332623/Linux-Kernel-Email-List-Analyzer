package com.jesse.analyze_report_discuss.components.sse_callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyze_report_discuss.components.discuss_abstract.AnalyzeReportDiscussAbstractor;
import com.jesse.analyze_report_discuss.components.discuss_session_lock.DiscussSessionLockGuard;
import com.jesse.analyze_report_discuss.request.DiscussAbstractRequest;
import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionDetailsService;
import com.jesse.core.pojo.ai.AIModelAnswerChoice;
import com.jesse.core.pojo.ai.AIModelAnswerMessage;
import com.jesse.core.pojo.ai.AIModelAnswerUsage;
import com.jesse.core.pojo.ai.sse.AIModelAnswerMessageBySSE;
import com.jesse.core.response.AIModelAnswerResponse;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    /** 内核邮件分析报告讨论专用虚拟线程池执行器。*/
    private final
    ExecutorService analyzeReportDiscussExecutor;

    /** 讨论会话锁管理器接口。*/
    private final
    DiscussSessionLockGuard discussSessionLockGuard;

    /** 每次发起 Stream 模式调用的时候，构造本回调的实例。*/
    public static SSECallBack create(
        final Long                 sessionDetailId,
        final ReportDiscussRequest discussRequest,
        final SseEmitter           sseEmitter,
        final ObjectMapper         objectMapper,
        final AnalyzeReportDiscussSessionDetailsService discussSessionDetailsService,
        final AIModelAnswerAuditService                 aiModelAnswerAuditService,
        final AnalyzeReportDiscussAbstractor            analyzeReportDiscussAbstractor,
        final ExecutorService                           executorService,
        final DiscussSessionLockGuard                  discussSessionLockGuard
    )
    {
        final ResponseChunkHandler responseChunkHandler
            = ResponseChunkHandler.create(sseEmitter);

        return new
        SSECallBack(
            sessionDetailId,
            discussRequest,
            sseEmitter,
            objectMapper,
            aiModelAnswerAuditService,
            discussSessionDetailsService,
            analyzeReportDiscussAbstractor,
            responseChunkHandler,
            executorService,
            discussSessionLockGuard
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
    aggregateResponseChunks(List<AIModelAnswerSSEResponse> chunks)
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
    private AIModelAnswerSSEResponse
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
        catch (JsonProcessingException exception2)
        {
            log.error("Parse response line {} failed.", eventData, exception2);
            return null;
        }
    }

    /** 本次对话完成后需要在后台执行的审计与上下文摘要任务。*/
    private void
    auditAndAbstract(AIModelAnswerResponse aggregatedResponse)
    {
        // (1) 审计大模型调用信息
        final CompletableFuture<String> saveAudit
            = CompletableFuture.supplyAsync(
                () -> {
                    this.aiModelAnswerAuditService.save(aggregatedResponse);
                    return "OK";
                },
                this.analyzeReportDiscussExecutor
            ).exceptionally((exception) -> {
                log.error(
                    "Model response audit failed. (response id = {})",
                    aggregatedResponse.getId(), exception
                );

                return exception.getMessage();
        });

        // (2) 将会话下的某个对话记录与大模型回复关联，代表大模型已经回答了这个问题
        final CompletableFuture<String> associateResponse
            = CompletableFuture.supplyAsync(
                () -> {
                    this.discussSessionDetailsService
                        .updateModelResponseIdBySessionId(
                            this.sessionDetailId,
                            this.discussRequest.getSessionId(),
                            aggregatedResponse.getId()
                        );

                    return "OK";
                },
                this.analyzeReportDiscussExecutor
            ).exceptionally((exception) -> {
                log.error(
                    "Associate a conversation failed " +
                    "(session id = {}, conversation id = {}, response id = {})",
                    this.discussRequest.getSessionId(),
                    this.sessionDetailId,
                    aggregatedResponse.getId(),
                    exception
                );

                return exception.getMessage();
        });

        // (3) 摘要并缓存本次回答信息
        //（如果 SSE 事件发射器被客户端中断了，则跳过本任务）
        final CompletableFuture<String> discussAbstract
            = (!this.responseChunkHandler.isEmitterInterrupted())
                ? CompletableFuture.supplyAsync(
                    () -> {
                        final DiscussAbstractRequest abstractRequest
                            = new DiscussAbstractRequest(
                                this.discussRequest.getTaskId(),
                                this.discussRequest.getSessionId(),
                                aggregatedResponse,
                                this.discussRequest.getQuestion()
                            );

                        this.analyzeReportDiscussAbstractor.discussAbstract(abstractRequest);

                        return "OK";
                    }, this.analyzeReportDiscussExecutor
                ).exceptionally((exception) -> {
                    log.error(
                        "Generate discuss abstract failed. " +
                        "(task id = {}, session id = {}, response id = {})",
                        this.discussRequest.getTaskId(),
                        this.discussRequest.getSessionId(),
                        aggregatedResponse.getId(),
                        exception
                    );

                    return exception.getMessage();
                })
                : CompletableFuture.supplyAsync(() -> "SKIP");


        try
        {
            // 将各个任务写到 Map 中去统一管理
            final Map<String, CompletableFuture<String>> taskMap
                = Map.of(
                    "save audit",         saveAudit,
                    "associate response", associateResponse,
                    "discuss abstract",   discussAbstract
                );

            // 等待所有任务完成
            CompletableFuture.allOf(taskMap.values().toArray(CompletableFuture[]::new))
                .get(10, TimeUnit.SECONDS);

            // 收集各个任务的执行状态
            final String executeResultInfo
                = taskMap.entrySet().stream()
                    .map((entry) ->
                        "%s: %s".formatted(
                            entry.getKey(),
                            entry.getValue().getNow("Unknown error")
                        )
                    )
                    .collect(Collectors.joining(" | "));

            log.debug(
                "Background task complete. (response id: {}, {})",
                aggregatedResponse.getId(), executeResultInfo
            );
        }
        catch (TimeoutException timeout) {
            log.error("Background task timeout.", timeout);
        }
        catch (InterruptedException interrupted) {
            log.error("Background task has been interrupted.", interrupted);
        }
        catch (ExecutionException ignore) { /* 不会来到此处 */ }
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
        finally
        {
            // 失败了也不要忘记释放锁
            this.discussSessionLockGuard
                .release(this.discussRequest.getSessionId());
        }

        final Request request = call.request();

        log.error(
            "Read SSE response data failed. URL: {}, Method: {}",
            request.url(), request.method(), exception
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

            /*
             * 2026 年 8 月 10 日问题与修复：
             * 在调用 bufferedReader.lines() 接受大模型 API 传来的数据片时，
             * 如果前端关闭、刷新页面或者点击停止按钮，客户端的连接已经中断，
             * 但大模型服务这边却还 “孜孜不倦” 的往已经关闭的 SseEmitter 推送数据片，
             * 最终导致了这样的异常链条：
             *
             * org.springframework.web.context.request.async.AsyncRequestNotUsableException
             *      "ServletOutputStream failed to flush: java.io.IOException: Connection reset by peer"
             * 抛点来自 SseEmitter 的 send() 方法
             *
             * java.io.IOException
             *      "Connection reset by peer"
             *
             * 解决方案：
             *
             * 在 ResponseChunkHandler 类内添加一个 isEmitterInterrupted 标志位，
             * 如果推送数据片失败，则翻转该标志位为 true，
             * 后续大模型 API 传来的数据片则绕过 handleResponseChunk() 方法，
             * 直接传递数据片给下游即可。
             *
             * 此外，还需要在全局异常处理器中
             * “抑制” AsyncRequestNotUsableException 异常，令其只输出异常信息，
             * 不打印异常堆栈。
             */
            try (var bufferedReader = makeBufferedReader(body))
            {
                bufferedReader
                    .lines()
                    .filter(SSECallBack::startWithData)
                    .map(SSECallBack::extractChunkJsonText)
                    .map(this::parseResponseLine)
                    .filter(Objects::nonNull)
                    .map((chunk) ->
                        // 如果客户端的连接已经断了，则跳过推送
                        // 但是数据片还是应该收集给下游审计
                        (this.responseChunkHandler.isEmitterInterrupted())
                            ? chunk
                            : this.responseChunkHandler.handleResponseChunk(chunk)
                    )
                    .forEach(chunks::add);

                if (!this.responseChunkHandler.isEmitterInterrupted()) {
                    this.sseEmitter.complete();
                }
            }

            // 收集完所有的响应数据片后，再统一作审计信息持久化。
            this.aggregateResponseChunks(chunks)
                .ifPresent(this::auditAndAbstract);
        }
        finally
        {
            // 最后释放锁，表示本会话可以开始下一轮讨论了
            this.discussSessionLockGuard
                .release(this.discussRequest.getSessionId());
        }
    }
}