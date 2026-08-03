package com.jesse.analyze_report_discuss.components.sse_callback;

import com.jesse.core.pojo.ai.sse.AIModelAnswerMessageBySSE;
import com.jesse.core.response.base.AIModelAnswerBaseResponse;
import com.jesse.core.response.sse.AIModelAnswerSSEResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
    private final SseEmitter sseEmitter;

    public static
    ResponseChunkHandler
    create(SseEmitter sseEmitter) {
        return new ResponseChunkHandler(sseEmitter);
    }

    private void
    handleContentChunk(AIModelAnswerMessageBySSE modelAnswerMessage)
        throws IOException
    {
        final String contentChunk = modelAnswerMessage.getContent();

        this.sseEmitter.send(
            SseEmitter.event()
                .name("content-msg")
                .data(contentChunk)
        );
    }

    private void
    handleReasonContentChunk(AIModelAnswerMessageBySSE modelAnswerMessage) throws IOException
    {
        final String reasonContentChunk
            = modelAnswerMessage.getReasonContent();

        this.sseEmitter.send(
            SseEmitter.event()
                .name("reason-content-msg")
                .data(reasonContentChunk)
        );
    }

    public void
    handleFailedResponse(Response response) throws IOException
    {
        this.sseEmitter
            .send(
                SseEmitter.event()
                    .name("error")
                    .data("HTTP " + response.code())
            );

        sseEmitter.complete();
    }

    public AIModelAnswerSSEResponse
    handleResponseChunk(AIModelAnswerBaseResponse responseChunk)
    {
        if (responseChunk instanceof AIModelAnswerSSEResponse sseResponse)
        {
            final AIModelAnswerMessageBySSE modelAnswerMessage
                = sseResponse.getChoices().getFirst().getDelta();

            try
            {
                // 处理并推送推理响应数据片
                if (Objects.nonNull(modelAnswerMessage.getReasonContent())) {
                    this.handleReasonContentChunk(modelAnswerMessage);
                }

                // 处理并推送回复响应数据片
                if (Objects.nonNull(modelAnswerMessage.getContent())) {
                    this.handleContentChunk(modelAnswerMessage);
                }
            }
            catch (Exception exception)
            {
                try {
                    this.sseEmitter.completeWithError(exception);
                }
                catch (Exception ignored) {}
            }

            return sseResponse;
        }

        // 上游模型可能返回其他类型的响应，这里仅仅是兜底
        log.warn("Unexcepted responseChunk: {}", responseChunk);

        return null;
    }
}
