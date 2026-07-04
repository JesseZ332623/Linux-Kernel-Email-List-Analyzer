package com.jesse.linux_kernel_email_list_analyzer.pojo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * AI 模型一轮对话下消耗的 Token 明细记录类，JSON 示例如下：
 *
 * <pre>
 * "usage": {
 *     "prompt_tokens": 12,
 *     "completion_tokens": 56,
 *     "total_tokens": 68,
 *      "prompt_tokens_details": {
 *         "cached_tokens": 0
 *      },
 *      "completion_tokens_details": {
 *         "reasoning_tokens": 45
 *      },
 *      "prompt_cache_hit_tokens": 0,
 *      "prompt_cache_miss_tokens": 12
 * }
 * </pre>
 *
 * 其中 xxx_details 字段是变化的，无法精确映射，就先写成 Object 类型，
 * Jackson 会默认按照 {@link java.util.LinkedHashMap} 来反序列化。
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelAnswerUsage
{
    /** 输入提示词消耗的 Token 数 */
    @JsonProperty("prompt_tokens")
    private Long promptTokens;

    /** AI 模型输出消耗的 Token 数（推理消耗 + 最终输出的消耗）*/
    @JsonProperty("completion_tokens")
    private Long completionTokens;

    /** 总消耗 Token 数 */
    @JsonProperty("total_tokens")
    private Long totalTokens;

    /** 输入 Token 的详细构成（暂时用不到）*/
    @JsonProperty("prompt_tokens_details")
    private Object promptTokensDetails;

    /** 输出 Token 的详细构成（暂时用不到）*/
    @JsonProperty("completion_tokens_details")
    private Object completionTokensDetails;

    @JsonProperty("prompt_cache_hit_tokens")
    private Long promptCacheHitTokens;

    @JsonProperty("prompt_cache_miss_tokens")
    private Long promptCacheMissTokens;
}
