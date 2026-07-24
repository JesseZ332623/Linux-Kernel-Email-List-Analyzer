package com.jesse.analyzer.pojo.ai.sse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI 模型基于 SSE 协议流式回复消息类，JSON 示例如下：
 *
 * <pre>

 * "delta": {
 *    "role": "assistant",
 *    "content": null,
 *    "reasoning_content": " should"
 * }
 * </pre>
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelAnswerMessageBySSE
{
    /**
     * 固定为 "assistant"，表示这是AI的回复，
     * 但在流式模式下仅在第一个响应行出现。
     */
    private String role;

    /** 最终可见的回复内容 */
    private String content;

    /** DeepSeek 特有的推理过程文本 */
    @JsonProperty("reasoning_content")
    private String reasonContent;
}
