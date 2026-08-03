package com.jesse.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.pojo.ai.AIModelChatThinking;
import com.jesse.core.properties.DeepSeekChatProperties;
import com.jesse.core.request.AIModelChatRequest;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** 自定义 HTTP 工具类。*/
@Component
@RequiredArgsConstructor
public final class HttpClientUtils
{
    /** 通用 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /**
     * 构造 {@link okhttp3.Request} 实例
     * 用于供 {@link okhttp3.OkHttpClient} 发起大模型请求。
     */
    public okhttp3.Request makeOkRequest(
        final String apiKey,
        final String chatSysPrompt,
        final String chatUsrPrompt,
        final DeepSeekChatProperties properties
    )
    {
        final AIModelChatRequest request = new AIModelChatRequest();

        request.setModel(properties.getModelName());
        request.setThinking(new AIModelChatThinking(properties.getThinking()));
        request.setReasoningEffort(properties.getReasoningEffort());
        request.setStream(properties.isStream());

        request.setMessages(
            List.of(
                new AIModelChatMessage("system", chatSysPrompt),
                new AIModelChatMessage("user", chatUsrPrompt)
            )
        );

        try
        {
            final RequestBody requestBody
                = RequestBody.create(
                    this.objectMapper.writeValueAsString(request),
                    MediaType.get("application/json")
            );

            return new
            Request.Builder()
                .url(properties.getModelEndpointUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();
        }
        catch (JsonProcessingException exception) {
            return new Request.Builder().build();
        }
    }

    /** 构造 {@link HttpEntity} 供 {@link RestTemplate} 发起 HTTP 请求。*/
    public HttpEntity<AIModelChatRequest> makeAIModelChatRequest(
        final String apiKey,
        final String chatSysPrompt,
        final String chatUsrPrompt,
        final DeepSeekChatProperties properties
    )
    {
        final HttpHeaders headers = new HttpHeaders();

        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        final AIModelChatRequest request = new AIModelChatRequest();

        final List<AIModelChatMessage> aiModelChatMessages
            = List.of(
                new AIModelChatMessage("system", chatSysPrompt),
                new AIModelChatMessage("user",   chatUsrPrompt)
            );

        request.setModel(properties.getModelName());
        request.setMessages(aiModelChatMessages);
        request.setThinking(new AIModelChatThinking(properties.getThinking()));
        request.setReasoningEffort(properties.getReasoningEffort());
        request.setStream(properties.isStream());

        return new HttpEntity<>(request, headers);
    }
}