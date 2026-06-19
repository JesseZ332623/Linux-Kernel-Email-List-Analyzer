package com.jesse.linux_kernel_email_list_analyzer.pojo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI 模型回复消息类，JSON 示例如下：
 *
 * <pre>
 * message": {
 *     "role": "assistant",
 *     "content": "Hi there! How can I help you today?",
 *     "reasoning_content": "We are asked: \"Hello!\" This is a simple greeting. The assistant should respond in a friendly manner. No complex reasoning needed. Just a greeting back."
 * }
 * </pre>
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelAnswerMessage
{
    /** 固定为 "assistant"，表示这是AI的回复 */
    private String role;

    /** 最终可见的回复内容 */
    private String content;

    /** DeepSeek 特有的推理过程文本 */
    @JsonProperty("reasoning_content")
    private String reasonContent;
}
