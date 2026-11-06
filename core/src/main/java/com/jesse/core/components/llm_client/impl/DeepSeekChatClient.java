package com.jesse.core.components.llm_client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.annotation.TimeMonitor;
import com.jesse.core.components.llm_client.LLMClient;
import com.jesse.core.components.llm_client.exception.LLMClientException;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.pojo.ai.AIModelChatThinking;
import com.jesse.core.pojo.DeepSeekChatProperties;
import com.jesse.core.repository.ApplicationApiKeysRepository;
import com.jesse.core.request.AIModelChatRequest;
import com.jesse.core.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Deepseek 大模型 API 对接客户端实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekChatClient implements LLMClient
{
    /** Spring 封装的 HTTP 客户端。*/
    private final RestTemplate restTemplate;

    /** OK HTTP 客户端，专用于处理 SSE 协议的响应数据。*/
    private final OkHttpClient okHttpClient;

    /** 通用的 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository applicationApiKeysRepository;

    /** 基本入参校验。*/
    private static void
    validateParameters(DeepSeekChatProperties properties, List<AIModelChatMessage> messages)
    {
        if (Objects.isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null");
        }

        if (CollectionUtils.isEmpty(messages)) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        for (AIModelChatMessage message : messages)
        {
            if (Objects.isNull(message)) {
                throw new IllegalArgumentException("Message cannot be null");
            }

            if (!StringUtils.hasText(message.getRole())) {
                throw new IllegalArgumentException("Message role cannot be empty");
            }

            if (!StringUtils.hasText(message.getContent())) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }
        }
    }

    /**
     * 一次性生成完整的回复。
     *
     * @param properties 模型对话属性配置
     * @param messages   向模型提交的对话 prompt 提示词上下文
     *
     * @throws LLMClientException       API 调用错误所抛出的异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * @return 模型回答响应体
     */
    @Override
    @TimeMonitor(
        value         = "deepseek-complete-chat",
        logArgs       = true,
        warnThreshold = 120L,
        timeunit      = TimeUnit.SECONDS
    )
    public AIModelAnswerResponse
    complete(DeepSeekChatProperties properties, List<AIModelChatMessage> messages)
        throws LLMClientException
    {
        DeepSeekChatClient.validateParameters(properties, messages);

        final HttpHeaders httpHeaders    = new HttpHeaders();
        final AIModelChatRequest request = new AIModelChatRequest();

        // (1) 组装请求头
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        httpHeaders.set(
            "Authorization",
            "Bearer " + this.applicationApiKeysRepository
                            .findByAppName(properties.getAuthorizationName())
        );

        // (2) 组装请求体
        request.setModel(properties.getModelName());
        request.setMessages(messages);
        request.setThinking(new AIModelChatThinking(properties.getThinking()));
        request.setReasoningEffort(properties.getReasoningEffort());
        request.setStream(properties.isStream());

        try
        {
            // (2) 向 DeepSeek 模型 API 发起 POST 请求
            final String responseJSON
                = this.restTemplate
                      .postForObject(
                          properties.getModelEndpointUrl(),
                          new HttpEntity<>(request, httpHeaders),
                          String.class
                      );

            if (!StringUtils.hasText(responseJSON)) {
                throw new LLMClientException("[DeepSeek] API returned empty response body.");
            }

            try {
                return this.objectMapper.readValue(responseJSON, AIModelAnswerResponse.class);
            }
            catch (JsonProcessingException jsonProcessingException)
            {
                log.error("[DeepSeek] response body parse failed, response JSON: {}", responseJSON);
                throw new LLMClientException("Response body parse failed.", jsonProcessingException);
            }
        }
        catch (HttpClientErrorException httpClientErrorException)
        {
            // 4xx 错误：通常是业务逻辑错误
            final int status
                = httpClientErrorException.getStatusCode().value();

            final String body
                = httpClientErrorException.getResponseBodyAsString();

            log.error("[DeepSeek] Business error, status = {}, response body: {}", status, body);

            // 429 Too Many Requests 响应码表示被下游服务限流
            if (status == 429)
            {
                throw new LLMClientException(
                    "(429) Rate limit triggered.",
                    httpClientErrorException
                );
            }

            /*
             * 400 Bad Request（请求体格式非法等）
             * 401 Unauthorized（API Key 不存在等）
             * 402 Payment Required（欠费）都不该重试
             */
            throw new LLMClientException(
                "(%d) Non retryable client error.".formatted(status),
                httpClientErrorException
            );
        }
        catch (HttpServerErrorException httpServerErrorException)
        {
            // 5xx 错误：通常是 DeepSeek 服务端挂了
            final int status
                = httpServerErrorException.getStatusCode().value();

            log.error("[DeepSeek] Server error, status = {}", status);

            throw new LLMClientException(
                "(%d) Server error.".formatted(status),
                httpServerErrorException
            );
        }
        catch (ResourceAccessException resourceAccessException)
        {
            // 网络层错误：超时、DNS、TLS 握手失败、连接重置等
            log.error("[DeepSeek] Network call failed: {}", resourceAccessException.getMessage());

            throw new LLMClientException(
                "Network exception or timeout.", resourceAccessException
            );
        }
    }

    /**
     * 异步流式生成回复。
     *
     * @param properties 模型对话属性配置
     * @param messages   向模型提交的对话 prompt 提示词上下文
     * @param callback   发起请求后的异步回调逻辑实现
     *
     * @throws IllegalArgumentException 当参数校验失败时抛出
     */
    @Override
    @TimeMonitor(
        value         = "deepseek-stream-chat",
        logArgs       = true,
        warnThreshold = 120L,
        timeunit      = TimeUnit.MINUTES
    )
    public void
    stream(DeepSeekChatProperties properties, List<AIModelChatMessage> messages, Callback callback)
    {
        DeepSeekChatClient.validateParameters(properties, messages);

        if (Objects.isNull(callback)) {
            throw new IllegalArgumentException("Callback cannot be null");
        }

        // (1) 组装请求体
        final AIModelChatRequest request = new AIModelChatRequest();

        request.setModel(properties.getModelName());
        request.setThinking(new AIModelChatThinking(properties.getThinking()));
        request.setReasoningEffort(properties.getReasoningEffort());
        request.setStream(properties.isStream());
        request.setMessages(messages);

        try
        {
            // (2) 转化成 OK HTTP 专用的请求体
            final RequestBody requestBody
                = RequestBody.create(
                    this.objectMapper.writeValueAsString(request),
                    okhttp3.MediaType.get("application/json")
            );

            // (3) 获取大模型的 API Key
            final String apiKey
                = this.applicationApiKeysRepository
                      .findByAppName(properties.getAuthorizationName());

            // (4) 构造 OK HTTP 请求实例
            final Request okRequest
                = new Request.Builder()
                    .url(properties.getModelEndpointUrl())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            // (5) 发起异步的请求
            this.okHttpClient.newCall(okRequest).enqueue(callback);
        }
        catch (JsonProcessingException exception) {
            log.error("Serialize ai model chat request failed.", exception);
            throw new IllegalArgumentException("Request argument serialized failed.", exception);
        }
    }
}