package com.jesse.core.components.llm_client;

import com.jesse.core.components.llm_client.exception.LLMClientException;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.pojo.DeepSeekChatProperties;
import com.jesse.core.response.AIModelAnswerResponse;
import okhttp3.Callback;

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
     * @throws LLMClientException       API 调用错误所抛出的异常
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * @return 模型回答响应体
     */
    AIModelAnswerResponse
    complete(DeepSeekChatProperties properties, List<AIModelChatMessage> messages)
        throws LLMClientException;

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