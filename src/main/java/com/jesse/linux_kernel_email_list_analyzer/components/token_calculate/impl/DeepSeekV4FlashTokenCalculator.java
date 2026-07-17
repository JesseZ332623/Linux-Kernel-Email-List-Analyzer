package com.jesse.linux_kernel_email_list_analyzer.components.token_calculate.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.token_calculate.ModelTokenCalculator;
import com.jesse.linux_kernel_email_list_analyzer.dto.AIModelAnswerUsageDTO;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DeepSeek-V4-Flash 模型 Token 资费计算器。
 *
 * <pre>
 * DeepSeek-V4-Flash Token 费计算公式：
 * 百万输入词元 (缓存命中)   * 0.02  +
 * 百万输入词元 (缓存未命中) * 1.00   +
 * 百万输出词源              * 2.00
 * </pre>
 */
@NoArgsConstructor
public class DeepSeekV4FlashTokenCalculator implements ModelTokenCalculator
{
    /** 命中缓存的输入提示词每百万 Token 的价格 */
    private static final
    BigDecimal PROMPT_CACHE_HIT_PRICE = new BigDecimal("0.020");

    /** 未命中缓存的输入提示词每百万 Token 的价格 */
    private static final
    BigDecimal PROMPT_CACHE_MISS_PRICE = new BigDecimal("1.00");

    /** 输出提示词每百万 Token 的价格 */
    private static final
    BigDecimal COMPLETION_PRICE = new BigDecimal("2.00");

    /**
     * 根据上游数据库聚合而来的 Token 用量信息，
     * 计算最终的 Token 资费（单位：人民币）。
     */
    @Override
    public BigDecimal
    calculate(AIModelAnswerUsageDTO modelAnswerUsage)
    {
        final BigDecimal promptCacheHitTokens
            = BigDecimal.valueOf(modelAnswerUsage.getPromptCacheHitTokens())
            .divide(ONE_MILLION, PRCESION, RoundingMode.HALF_UP);

        final BigDecimal promptCacheMissTokens
            = BigDecimal.valueOf(modelAnswerUsage.getPromptCacheMissTokens())
            .divide(ONE_MILLION, PRCESION, RoundingMode.HALF_UP);

        final BigDecimal completionTokens
            = BigDecimal.valueOf(modelAnswerUsage.getCompletionTokens())
            .divide(ONE_MILLION, PRCESION, RoundingMode.HALF_UP);

        final BigDecimal standardPrice
            = promptCacheHitTokens.multiply(PROMPT_CACHE_HIT_PRICE)
            .add(promptCacheMissTokens.multiply(PROMPT_CACHE_MISS_PRICE))
            .add(completionTokens.multiply(COMPLETION_PRICE));

        return
        (modelAnswerUsage.getIsPeak())
            ? standardPrice.multiply(BigDecimal.TWO).stripTrailingZeros()
            : standardPrice.stripTrailingZeros();
    }
}
