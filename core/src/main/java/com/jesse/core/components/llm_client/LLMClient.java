package com.jesse.core.components.llm_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.pojo.DeepSeekChatProperties;
import com.jesse.core.response.AIModelAnswerResponse;
import okhttp3.Callback;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** LLM 大模型 API 对接客户端接口。*/
public interface LLMClient
{
    /**
     * 一次性生成完整的回复。
     *
     * @param properties 模型对话属性配置
     * @param messages   向模型提交的对话 prompt 提示词上下文
     *
     * @throws JsonProcessingException  反序列化响应体 JSON 错误时抛出（比如上游模型响应体格式有变化）
     * @throws RestClientException      当目标服务不可用或者服务器网络问题时抛出
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * @return 模型回答响应体
     */
    AIModelAnswerResponse
    complete(DeepSeekChatProperties properties, List<AIModelChatMessage> messages)
        throws JsonProcessingException, RestClientException;

    /**
     * 异步流式生成回复。
     *
     * @param properties 模型对话属性配置
     * @param messages   向模型提交的对话 prompt 提示词上下文
     * @param callback   发起请求后的异步回调逻辑实现
     *
     * @throws IllegalArgumentException 当参数校验失败时抛出
     */
    void stream(DeepSeekChatProperties properties, List<AIModelChatMessage> messages, Callback callback);
}