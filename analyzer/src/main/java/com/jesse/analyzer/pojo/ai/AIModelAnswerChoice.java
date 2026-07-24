package com.jesse.analyzer.pojo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI 模型回复消息选择类，JSON 示例：
 *
 * <pre>
 * {
 *     "index": 0,
 *     "message": { ... },
 *     "logprobs": null,
 *     "finish_reason": "stop"
 * }
 * </pre>
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelAnswerChoice
{
    /** 结果索引，通常为 0 */
    private Integer index;

    /** 模型生成的完整消息对象 */
    private AIModelAnswerMessage message;

    /** 对数概率信息，通常为 null */
    private Object logprobs;

    /**
     * 回答生成的结束原因，通常为：
     *
     * <ul>
     *     <li>stop           正常结束</li>
     *     <li>length         达到长度限制</li>
     *     <li>content_filter 内容过滤</li>
     *     <li> ... </li>
     * </ul>
     */
    @JsonProperty("finish_reason")
    private String finishReason;
}
