package com.jesse.response_audit.components.token_calculate.impl;

import com.jesse.core.dto.AIModelAnswerUsageDTO;
import com.jesse.core.entity.AIModelTokenPricingEntity;
import com.jesse.response_audit.repository.AIModelTokenPricingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link ModelTokenCalculatorImpl} Token 资费计算规则测试。*/
class ModelTokenCalculatorImplTest
{
    private final AIModelTokenPricingRepository pricingRepository
        = mock(AIModelTokenPricingRepository.class);

    private final ModelTokenCalculatorImpl calculator
        = new ModelTokenCalculatorImpl(this.pricingRepository);

    private AIModelTokenPricingEntity pricing(String hit, String miss, String completion)
    {
        final AIModelTokenPricingEntity pricing = new AIModelTokenPricingEntity();
        pricing.setPromptCacheHitPrice(new BigDecimal(hit));
        pricing.setPromptCacheMissPrice(new BigDecimal(miss));
        pricing.setCompletionPrice(new BigDecimal(completion));
        return pricing;
    }

    private AIModelAnswerUsageDTO usage(long hit, long miss, long completion, Boolean peak)
    {
        final AIModelAnswerUsageDTO usage = new AIModelAnswerUsageDTO();
        usage.setPromptCacheHitTokens(hit);
        usage.setPromptCacheMissTokens(miss);
        usage.setCompletionTokens(completion);
        usage.setIsPeak(peak);
        return usage;
    }

    @Test
    @DisplayName("按每百万 token 单价线性计费")
    void chargesPerMillionTokens()
    {
        when(this.pricingRepository.getPricingByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.of(this.pricing("0.02", "1.00", "2.00")));

        // 命中 1_000_000 token -> 0.02 元
        final BigDecimal cost = this.calculator.calculate(
            "deepseek-v4-flash",
            List.of(this.usage(1_000_000L, 0L, 0L, false))
        );

        assertThat(cost).isEqualByComparingTo("0.02");
    }

    @Test
    @DisplayName("峰期消耗按双倍计费")
    void peakUsageIsDoubled()
    {
        when(this.pricingRepository.getPricingByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.of(this.pricing("0.02", "1.00", "2.00")));

        final BigDecimal cost = this.calculator.calculate(
            "deepseek-v4-flash",
            List.of(this.usage(1_000_000L, 0L, 0L, true))
        );

        assertThat(cost).isEqualByComparingTo("0.04");
    }

    @Test
    @DisplayName("多段用量累加求和")
    void sumsMultipleUsages()
    {
        when(this.pricingRepository.getPricingByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.of(this.pricing("0.02", "1.00", "2.00")));

        // 两段各命中 1_000_000 -> 0.02 + 0.02 = 0.04
        final BigDecimal cost = this.calculator.calculate(
            "deepseek-v4-flash",
            List.of(
                this.usage(1_000_000L, 0L, 0L, false),
                this.usage(1_000_000L, 0L, 0L, false)
            )
        );

        assertThat(cost).isEqualByComparingTo("0.04");
    }

    @Test
    @DisplayName("查不到模型定价时按默认模型 deepseek-v4-flash 兜底计费")
    void fallsBackToDefaultModelPricing()
    {
        when(this.pricingRepository.getPricingByModelName("unknown-model"))
            .thenReturn(Optional.empty());
        when(this.pricingRepository.getPricingByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.of(this.pricing("0.02", "1.00", "2.00")));

        final BigDecimal cost = this.calculator.calculate(
            "unknown-model",
            List.of(this.usage(1_000_000L, 0L, 0L, false))
        );

        assertThat(cost).isEqualByComparingTo("0.02");
    }

    @Test
    @DisplayName("模型和默认模型定价都缺失时抛异常")
    void throwsWhenNeitherModelNorDefaultHasPricing()
    {
        when(this.pricingRepository.getPricingByModelName("unknown-model"))
            .thenReturn(Optional.empty());
        when(this.pricingRepository.getPricingByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            this.calculator.calculate("unknown-model", List.of(this.usage(1L, 0L, 0L, false)))
        ).isInstanceOf(RuntimeException.class);
    }
}
