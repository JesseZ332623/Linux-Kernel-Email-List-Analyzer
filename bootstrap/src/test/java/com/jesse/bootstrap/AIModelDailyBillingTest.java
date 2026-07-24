package com.jesse.bootstrap;

import com.jesse.analyzer.components.token_calculate.ModelTokenCalculator;
import com.jesse.analyzer.dto.AIModelAnswerUsageDTO;
import com.jesse.analyzer.entity.AIModelDailyBillingEntity;
import com.jesse.analyzer.repository.AIModelAnswerUsageRepository;
import com.jesse.analyzer.repository.AIModelDailyBillingRepository;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.utils.ZoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** AI 模型 token 资费消耗每日汇总测试类。*/
@Slf4j
@SpringBootTest
public class AIModelDailyBillingTest
{
    /** AI 模型 LKML 分析任务 Token 消耗明细表仓储类。*/
    @Autowired
    private AIModelAnswerUsageRepository aiModelAnswerUsageRepository;

    /** AI 模型 token 资费消耗每日汇总表仓储类。*/
    @Autowired
    private AIModelDailyBillingRepository aiModelDailyBillingRepository;

    /** 模型计算器表。*/
    @Autowired
    @Qualifier("model-token-calculator-map")
    private Map<String, ModelTokenCalculator> modelTokenCalculatorMap;

    /** 全局 ID 消费机接口。*/
    @Autowired
    private GlobalIdConsumer globalIdConsumer;

    /** 默认模型名称（如果查询不到 Token 资费计算器就按这个兜底）*/
    private static final
    String DEFAULT_MODEL_NAME = "deepseek-v4-flash";

    /** 获取大模型对应的 Token 资费计算器实例。*/
    private ModelTokenCalculator
    selectCalculator(String modelName)
    {
        if (!this.modelTokenCalculatorMap.containsKey(modelName))
        {
            log.warn(
                "Model {} specialized token calculator not found." +
                "Charged according to {} standard, please make up for it in a timely manner.",
                modelName, DEFAULT_MODEL_NAME
            );
        }

        return
        this.modelTokenCalculatorMap
            .getOrDefault(
                modelName,
                this.modelTokenCalculatorMap.get(DEFAULT_MODEL_NAME)
            );
    }

    @Test
    @Transactional(readOnly = true)
    public void testModelDailyBilling()
    {
        final LocalDate yesterday
            = LocalDate.now(ZoneUtils.LOCAL_TIMEZONE).minusDays(1);

        final LocalDateTime startOfDay = yesterday.atStartOfDay();
        final LocalDateTime endOfDay   = yesterday.atTime(LocalTime.MAX);

        // (1) 查询昨天一整天不同模型总共的 Token 消耗明细，
        // 再按照 model 分组成 Map<String, List<AIModelAnswerUsageDTO>>
        final Map<String, List<AIModelAnswerUsageDTO>> modelDailyUsages
            = this.aiModelAnswerUsageRepository
                  .getDailyUsageGroupByModel(startOfDay, endOfDay)
                  .stream()
                  .collect(Collectors.groupingBy(AIModelAnswerUsageDTO::getModel));

        for (var modelDailyUsage : modelDailyUsages.entrySet())
        {
            final String modelName                        = modelDailyUsage.getKey();
            final List<AIModelAnswerUsageDTO> dailyUsages = modelDailyUsage.getValue();

            // (2) 获取大模型对应的 Token 资费计算器实例
            //（查不到就按 DEFAULT_MODEL 兜底并告警）
            final ModelTokenCalculator calculator
                = this.selectCalculator(modelName);

            // (3) 计算该模型昨日总共的 Token 资费消耗
            final BigDecimal costRmb
                = dailyUsages.stream()
                    .map(calculator::calculate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info(
                "Model: {} cost {} RMB from {} to {}",
                modelName, costRmb, startOfDay, endOfDay
            );
        }
    }

    /**
     * 测试空记录的插入，在测试方法中使用事务注解，
     * 最后默认会回滚事务。
     */
    @Test
    @Transactional(rollbackFor = Exception.class)
    public void insertEmptyRecodTest()
    {
        final LocalDate yesterday
            = LocalDate.now(ZoneUtils.LOCAL_TIMEZONE).minusDays(1);

        final long nextId
            = this.globalIdConsumer.nextId();

        this.aiModelDailyBillingRepository
            .insert(AIModelDailyBillingEntity.makeEmptyDailyBill(nextId, yesterday));
    }
}