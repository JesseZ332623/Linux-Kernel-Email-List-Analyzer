package com.jesse.response_audit.components.token_calculate.impl;

import com.jesse.core.dto.AIModelAnswerUsageDTO;
import com.jesse.core.entity.AIModelTokenPricingEntity;
import com.jesse.response_audit.components.token_calculate.ModelTokenCalculator;
import com.jesse.response_audit.repository.AIModelTokenPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 模型 Token 资费计算器实现类。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelTokenCalculatorImpl implements ModelTokenCalculator
{
    /** 默认模型名称（如果查询不到 Token 定价就按这个兜底）。*/
    private static final
    String DEFAULT_MODEL_NAME = "deepseek-v4-flash";

    /** 大模型 Token 定价表仓储类。*/
    private final
    AIModelTokenPricingRepository aiModelTokenPricingRepository;

    /**
     * 根据上游数据库聚合而来的 Token 用量信息，
     * 计算最终的 Token 资费（单位：人民币）。
     *
     * @param modelAnswerUsage 单次调用产生的 Token 消耗明细
     * @param tokenPricing     该模型的 Token 定价数据
     *
     * @return 单次调用在当前定价下的资费消耗
     */
    private static BigDecimal
    calculate(AIModelAnswerUsageDTO modelAnswerUsage, AIModelTokenPricingEntity tokenPricing)
    {
        final BigDecimal promptCacheHitTokens
            = BigDecimal.valueOf(modelAnswerUsage.getPromptCacheHitTokens())
                        .divide(ONE_MILLION, PRECISION, RoundingMode.HALF_UP);

        final BigDecimal promptCacheMissTokens
            = BigDecimal.valueOf(modelAnswerUsage.getPromptCacheMissTokens())
                        .divide(ONE_MILLION, PRECISION, RoundingMode.HALF_UP);

        final BigDecimal completionTokens
            = BigDecimal.valueOf(modelAnswerUsage.getCompletionTokens())
                        .divide(ONE_MILLION, PRECISION, RoundingMode.HALF_UP);

        final BigDecimal standardPrice
            = promptCacheHitTokens.multiply(tokenPricing.getPromptCacheHitPrice())
                .add(promptCacheMissTokens.multiply(tokenPricing.getPromptCacheMissPrice()))
                .add(completionTokens.multiply(tokenPricing.getCompletionPrice()));

        return
        (modelAnswerUsage.getIsPeak())
            ? standardPrice.multiply(BigDecimal.TWO).stripTrailingZeros()
            : standardPrice.stripTrailingZeros();
    }

    /**
     * 根据上游数据库聚合而来的 Token 用量信息，
     * 计算最终的 Token 资费（单位：人民币）。
     *
     * @param modelName 模型名称
     * @param usages    该模型本次收集到的 Token 用量信息
     *
     * @return 当前定价下的最终的 Token 资费
     */
    @Override
    public BigDecimal
    calculate(String modelName, List<AIModelAnswerUsageDTO> usages)
    {
        // (1) 查询模型定价信息，如果查不到则抛出异常
        final AIModelTokenPricingEntity modelTokenPricing
            = this.aiModelTokenPricingRepository
                  .getPricingByModelName(modelName)
                  .orElseGet(() -> {
                      log.warn(
                          "Model {} specialized token calculator not found." +
                          "Charged according to {} standard, please make up for it in a timely manner.",
                          modelName, DEFAULT_MODEL_NAME
                      );

                      return
                      this.aiModelTokenPricingRepository
                          .getPricingByModelName(DEFAULT_MODEL_NAME).orElseThrow();
                  });

        // (2) 计算本模型这批用量的总资费消耗
        return
        usages.stream()
            .map((usage) ->
                ModelTokenCalculatorImpl.calculate(usage, modelTokenPricing))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}