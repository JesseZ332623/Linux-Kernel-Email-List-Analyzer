package com.jesse.analyzer.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jesse.analyzer.pojo.ai.AIModelAnswerChoice;
import com.jesse.analyzer.pojo.ai.AIModelAnswerUsage;
import com.jesse.analyzer.response.base.AIModelAnswerBaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * AI 模型回答响应体类，JSON 示例如下：
 *
 * <pre>
 * {
 *     "id": "18e28045-db18-4d6f-9fa7-b787af3a6d09",
 *     "object": "chat.completion",
 *     "created": 1781592208,
 *     "model": "deepseek-v4-pro",
 *     "choices": [
 *         {
 *             "index": 0,
 *             "message": {
 *                 "role": "assistant",
 *                 "content": "Hi there! How can I help you today?",
 *                 "reasoning_content": "We are asked: \"Hello!\" This is a simple greeting. The assistant should respond in a friendly manner. No complex reasoning needed. Just a greeting back."
 *             },
 *             "logprobs": null,
 *             "finish_reason": "stop"
 *         }
 *     ],
 *     "usage": {
 *         "prompt_tokens": 12,
 *         "completion_tokens": 43,
 *         "total_tokens": 55,
 *         "prompt_tokens_details": {
 *             "cached_tokens": 0
 *         },
 *         "completion_tokens_details": {
 *             "reasoning_tokens": 32
 *         },
 *         "prompt_cache_hit_tokens": 0,
 *         "prompt_cache_miss_tokens": 12
 *     },
 *     "system_fingerprint": "fp_9954b31ca7_prod0820_fp8_kvcache_20260402"
 * }
 * </pre>
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AIModelAnswerResponse extends AIModelAnswerBaseResponse
{
    /** 对象类型，固定为 "chat.completion"，表示这是一个完整的对话生成结果 */
    private String object;

    /** Unix 时间戳，表示响应的生成时间 */
    private Long created;

    /** 实际处理请求的模型名称 */
    private String model;

    /** 核心结果数组，包含模型生成的回复 */
    private List<AIModelAnswerChoice> choices;

    /** Token 用量统计，用于计费和分析 */
    private AIModelAnswerUsage usage;

    /** 后端环境标识，表示处理该请求的具体系统版本 / 配置，调试用 */
    @JsonProperty("system_fingerprint")
    private String systemFingerPrint;
}