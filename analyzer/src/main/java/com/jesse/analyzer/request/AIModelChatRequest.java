package com.jesse.analyzer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jesse.analyzer.pojo.ai.AIModelChatMessage;
import com.jesse.analyzer.pojo.ai.AIModelChatThinking;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/** 向 AI 模型发起提问的请求体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelChatRequest
{
    /** AI 模型名称（比如  deepseek-v4-pro）*/
    private String model;

    /** 对话提示词上下文列表 */
    private List<AIModelChatMessage> messages;

    /** 是否开启推理模式 */
    private AIModelChatThinking thinking;

    /**
     * 推理强度选项，
     * 控制模型在推理上的投入程度（"low"、"medium"、"high"）
     */
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;

    /** 是否采用流式输出 */
    private Boolean stream;
}