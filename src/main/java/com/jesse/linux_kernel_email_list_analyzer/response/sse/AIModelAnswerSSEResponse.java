package com.jesse.linux_kernel_email_list_analyzer.response.sse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelAnswerUsage;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.sse.AIModelAnswerChoiceBySSE;
import com.jesse.linux_kernel_email_list_analyzer.response.base.AIModelAnswerBaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * AI 模型基于 SSE 协议的流式回答响应体类，JSON 示例如下：
 *
 * <pre>
 * {
 *     "id": "b3c01378-b89f-48d8-a57d-4867ced673af",
 *     "object": "chat.completion.chunk",
 *     "created": 1783646331,
 *     "model": "deepseek-v4-pro",
 *     "system_fingerprint": "fp_9954b31ca7_prod0820_fp8_kvcache_20260402",
 *     "choices": [
 *         {
 *             "index": 0,
 *             "delta": {
 *                 "content": "",
 *                 "reasoning_content": null
 *             },
 *             "logprobs": null,
 *             "finish_reason": "stop"
 *         }
 *     ],
 *     "usage": {
 *         "prompt_tokens": 12,
 *         "completion_tokens": 49,
 *         "total_tokens": 61,
 *         "prompt_tokens_details": {
 *             "cached_tokens": 0
 *         },
 *         "completion_tokens_details": {
 *             "reasoning_tokens": 38
 *         },
 *         "prompt_cache_hit_tokens": 0,
 *         "prompt_cache_miss_tokens": 12
 *     }
 * }
 * </pre>
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AIModelAnswerSSEResponse extends AIModelAnswerBaseResponse
{
    /** 对象类型，固定为 "chat.completion.chunk"，表示这是一个完整的对话生成结果 */
    private String object;

    /** Unix 时间戳，表示响应的生成时间 */
    private Long created;

    /** 实际处理请求的模型名称 */
    private String model;

    /** 后端环境标识，表示处理该请求的具体系统版本 / 配置，调试用 */
    @JsonProperty("system_fingerprint")
    private String systemFingerPrint;

    /** 核心结果数组，包含模型生成的回复 */
    private List<AIModelAnswerChoiceBySSE> choices;

    /** Token 用量统计，用于计费和分析 */
    private AIModelAnswerUsage usage;
}
