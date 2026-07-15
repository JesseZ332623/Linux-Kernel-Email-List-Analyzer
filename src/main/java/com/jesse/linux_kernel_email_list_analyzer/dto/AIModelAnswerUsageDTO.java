package com.jesse.linux_kernel_email_list_analyzer.dto;

import lombok.*;

/** 一次大模型请求所消耗的 Token 明细 DTO，用于下游的资费计算。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class AIModelAnswerUsageDTO
{
    /** 模型名称 */
    private String model;

    /** 命中了缓存的输入 Token 数 */
    private Long promptCacheHitTokens;

    /** 未命中缓存的输入 Token 数 */
    private Long promptCacheMissTokens;

    /** 输出的 Token 数 */
    private Long completionTokens;
}