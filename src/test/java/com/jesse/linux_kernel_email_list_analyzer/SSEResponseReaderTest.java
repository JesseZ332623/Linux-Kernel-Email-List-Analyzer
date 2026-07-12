package com.jesse.linux_kernel_email_list_analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatMessage;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatThinking;
import com.jesse.linux_kernel_email_list_analyzer.properties.DeepSeekChatProperties;
import com.jesse.linux_kernel_email_list_analyzer.repository.ApplicationApiKeysRepository;
import com.jesse.linux_kernel_email_list_analyzer.request.AIModelChatRequest;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import com.jesse.linux_kernel_email_list_analyzer.response.base.AIModelAnswerBaseResponse;
import com.jesse.linux_kernel_email_list_analyzer.response.sse.AIModelAnswerSSEResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** DeepSeek 流式回复练习测试类。*/
@Slf4j
@SpringBootTest
public class SSEResponseReaderTest
{
    private static final
    String RESPONSE_LINE_PREFIX = "data: ";

    private static final
    String RESPONSE_DONE = "[DONE]";

    /** OK HTTP 客户端，专用与处理 SSE 协议的响应数据。*/
    @Autowired
    private OkHttpClient okHttpClient;

    /** 第三方应用访问 API Keys 表仓库类。*/
    @Autowired
    private ApplicationApiKeysRepository apiKeysRepository;

    /** DeepSeek 模型对话属性配置类。*/
    @Autowired
    private DeepSeekChatProperties deepSeekChatProperties;

    /** Jackson 对象映射器。*/
    @Autowired
    private ObjectMapper objectMapper;

    @RequiredArgsConstructor
    private class SSECallBack implements Callback
    {
        private static boolean
        startWithData(String line) {
            return line.startsWith(RESPONSE_LINE_PREFIX);
        }

        private static String
        extractEventData(String line) {
            return line.substring(RESPONSE_LINE_PREFIX.length());
        }

        private AIModelAnswerBaseResponse
        parseResponseLine(String eventData)
        {
            if (RESPONSE_DONE.equals(eventData.trim())) {
                return null;
            }

            try
            {
                return
                SSEResponseReaderTest.this.objectMapper
                    .readValue(eventData, AIModelAnswerSSEResponse.class);
            }
            catch (JsonProcessingException exception)
            {
                try
                {
                    return
                    SSEResponseReaderTest.this.objectMapper
                        .readValue(eventData, AIModelAnswerResponse.class);
                }
                catch (JsonProcessingException exception2)
                {
                    log.error("Parse response line {} failed.", eventData, exception2);
                    return null;
                }
            }
        }

        @Override
        public void
        onFailure(@NotNull Call call, @NotNull IOException exception) {
            log.error("Read SSE reasponse data failed.", exception);
        }

        @Override
        public void
        onResponse(@NotNull Call call, @NotNull Response response) throws IOException
        {
            log.info("HTTP Protocol: {}", response.protocol());

            if (!response.isSuccessful())
            {
                log.error("Unexcepted response: {}", response);
                return;
            }

            try (var bufferReader = new BufferedReader(new InputStreamReader(response.body().byteStream())))
            {
                bufferReader
                    .lines()
                    .filter(SSECallBack::startWithData)
                    .map(SSECallBack::extractEventData)
                    .map(this::parseResponseLine)
                    .filter(Objects::nonNull)
                    .forEach(System.out::println);
            }
        }
    }

    private Request makeOkRequest(AIModelChatRequest request)
    {
        Objects.requireNonNull(request, "AI Model chat request must not be null");

        try
        {
            final String apiKey
                = this.apiKeysRepository
                      .findByAppName(this.deepSeekChatProperties.getAuthorizationName());

            final RequestBody requestBody
                = RequestBody.create(
                    this.objectMapper.writeValueAsString(request),
                    MediaType.get("application/json")
                );

            return new
            Request.Builder()
                .url(this.deepSeekChatProperties.getModelEndpointUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        }
        catch (Exception exception)
        {
            log.error("", exception);

            return new Request.Builder().build();
        }
    }

    private AIModelChatRequest makeAIModelChatRequest()
    {
        final AIModelChatRequest request = new AIModelChatRequest();

        request.setModel(this.deepSeekChatProperties.getModelName());
        request.setThinking(new AIModelChatThinking(this.deepSeekChatProperties.getThinking()));
        request.setReasoningEffort(this.deepSeekChatProperties.getReasoningEffort());
        request.setStream(true);

        request.setMessages(
            List.of(
                new AIModelChatMessage("system", "You are a helpful assistant."),
                new AIModelChatMessage("user", "hello!")
            )
        );

        return request;
    }

    @Test
    public void chatStream() throws Exception
    {
        this.okHttpClient
            .newCall(this.makeOkRequest(this.makeAIModelChatRequest()))
            .enqueue(new SSECallBack());

        TimeUnit.SECONDS.sleep(15L);
    }
}