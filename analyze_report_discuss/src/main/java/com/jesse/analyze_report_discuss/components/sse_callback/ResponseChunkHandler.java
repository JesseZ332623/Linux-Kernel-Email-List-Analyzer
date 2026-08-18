package com.jesse.analyze_report_discuss.components.sse_callback;

import com.jesse.core.pojo.ai.sse.AIModelAnswerMessageBySSE;
import com.jesse.core.response.sse.AIModelAnswerSSEResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;

/** SSE 响应数据片处理器。*/
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseChunkHandler
{
    /** 外部传递的 SSE 事件发射器。*/
    private final SseEmitter sseEmitter;

    /** 客户端的连接是否被中断？*/
    @Getter
    private volatile boolean isEmitterInterrupted;

    public static
    ResponseChunkHandler
    create(SseEmitter sseEmitter) {
        return new ResponseChunkHandler(sseEmitter, false);
    }

    private void
    handleContentChunk(AIModelAnswerMessageBySSE modelAnswerMessage)
        throws IOException
    {
        final String contentChunk = modelAnswerMessage.getContent();

        if (Objects.nonNull(contentChunk))
        {
            this.sseEmitter.send(
                SseEmitter.event()
                    .name("content-msg")
                    .data(contentChunk)
            );
        }
    }

    private void
    handleReasonContentChunk(AIModelAnswerMessageBySSE modelAnswerMessage)
        throws IOException
    {
        final String reasonContentChunk
            = modelAnswerMessage.getReasonContent();

        if (Objects.nonNull(reasonContentChunk))
        {
            this.sseEmitter.send(
                SseEmitter.event()
                    .name("reason-content-msg")
                    .data(reasonContentChunk)
            );
        }
    }

    public void
    handleFailedResponse(Response response) throws IOException
    {
        this.sseEmitter.send(
            SseEmitter.event()
                .name("error")
                .data("HTTP " + response.code())
        );

        sseEmitter.complete();
    }

    public AIModelAnswerSSEResponse
    handleResponseChunk(AIModelAnswerSSEResponse responseChunk)
    {
        final AIModelAnswerMessageBySSE modelAnswerMessage
            = responseChunk.getChoices().getFirst().getDelta();

        try
        {
            if (!this.isEmitterInterrupted)
            {
                // 处理并推送推理响应数据片
                this.handleReasonContentChunk(modelAnswerMessage);

                // 处理并推送回复响应数据片
                this.handleContentChunk(modelAnswerMessage);
            }
        }
        catch (Exception exception)
        {
            this.isEmitterInterrupted = true;

            // 如果客户端中断了对话生成，翻转中断标志位
            if (exception instanceof IOException)
            {
                log.error(
                    "Send response chunk (which model response id = {}) interrupted.",
                    responseChunk.getId()
                );
            }
            else
            {
                log.error(
                    "Unexpected exception with sending SSE chunk (model response id = {})",
                    responseChunk.getId(), exception
                );

                try {
                    this.sseEmitter.completeWithError(exception);
                }
                catch (Exception ignored) {}
            }
        }

        return responseChunk;
    }
}