package com.jesse.linux_kernel_email_list_analyzer.service.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.global_id.GlobalIdConsumer;
import com.jesse.linux_kernel_email_list_analyzer.components.token_calculate.ModelTokenCalculator;
import com.jesse.linux_kernel_email_list_analyzer.dto.AIModelAnswerUsageDTO;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelDailyBillingEntity;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelAnswerUsageRepository;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelDailyBillingRepository;
import com.jesse.linux_kernel_email_list_analyzer.service.AIModelDailyBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** AI 模型 token 资费消耗每日汇总表服务类实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelDailyBillingServiceImpl implements AIModelDailyBillingService
{
    /** 默认时区 */
    private static final
    ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /** 默认模型名称（如果查询不到 Token 资费计算器就按这个兜底）*/
    private static final
    String DEFAULT_MODEL = "deepseek-v4-flash";

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** AI 模型 token 资费消耗每日汇总表仓储类。*/
    private final
    AIModelDailyBillingRepository aiModelDailyBillingRepository;

    /** AI 模型 LKML 分析任务 Token 消耗明细表仓储类。*/
    private final
    AIModelAnswerUsageRepository aiModelAnswerUsageRepository;

    /** 模型计算器表。*/
    @Qualifier("model-token-calculator-map")
    private final
    Map<String, ModelTokenCalculator> modelTokenCalculatorMap;

    /** 是否正在执行保存？避免自动 / 手动调用冲突。*/
    private final
    AtomicBoolean saving = new AtomicBoolean(false);

    /** 明天凌晨 4 点自动保存每天所有模型 token 资费消耗汇总数据。*/
    @Scheduled(cron = "0 0 4 * * ?")
    public void autoSave()
    {
        try {
            this.save();
        }
        catch (IllegalStateException exception) {
            log.warn("{}", exception.getMessage());
        }
    }

    /** 保存每天所有模型 token 资费消耗汇总数据。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalDate save()
    {
        if (!this.saving.compareAndSet(false, true))
        {
            throw new
            IllegalStateException(
                "Precious daily billing is still saving, skip this round."
            );
        }

        final LocalDate yesterday
            = LocalDate.now(DEFAULT_ZONE).minusDays(1);

        final LocalDateTime startOfDay = yesterday.atStartOfDay();
        final LocalDateTime endOfDay   = yesterday.atTime(LocalTime.MAX);

        try
        {
            // (1) 查询昨天一整天不同模型总共的 Token 消耗明细
            final List<AIModelAnswerUsageDTO> modelDailyUsages
                = this.aiModelAnswerUsageRepository
                      .getDailyUsageGroupByModel(startOfDay, endOfDay);

            for (AIModelAnswerUsageDTO modelDailyUsage : modelDailyUsages)
            {
                final String modelName
                    = modelDailyUsage.getModel();

                // (2) 获取大模型对应的 Token 资费计算器实例
                //（查不到就按 DEFAULT_MODEL 兜底并告警）
                final ModelTokenCalculator calculator
                    = this.modelTokenCalculatorMap
                    .getOrDefault(
                        modelName,
                        this.modelTokenCalculatorMap.get(DEFAULT_MODEL)
                    );

                if (!this.modelTokenCalculatorMap.containsKey(modelName))
                {
                    log.warn(
                        "Model {} specialized token calculator not found." +
                        "Charged according to {} standard, please make up for it in a timely manner.",
                        modelName, DEFAULT_MODEL
                    );
                }

                // (3) 计算该模型昨日总共的 Token 资费消耗
                final BigDecimal costRmb
                    = calculator.calculate(modelDailyUsage);

                final AIModelDailyBillingEntity dailyBilling
                    = new AIModelDailyBillingEntity();

                dailyBilling.setId(globalIdConsumer.nextId());
                dailyBilling.setBillingDate(yesterday);
                dailyBilling.setModelName(modelName);
                dailyBilling.setTotalPromptCacheHitTokens(modelDailyUsage.getPromptCacheHitTokens());
                dailyBilling.setTotalPromptCacheMissTokens(modelDailyUsage.getPromptCacheMissTokens());
                dailyBilling.setTotalCompletionTokens(modelDailyUsage.getCompletionTokens());
                dailyBilling.setTotalCostRmb(costRmb);
                dailyBilling.setCreateAt(LocalDateTime.now(DEFAULT_ZONE));

                // (4) 保存昨日的 Token 消耗与资费汇总数据
                this.aiModelDailyBillingRepository.upsertBilling(dailyBilling);

                log.info(
                    "Save AI model daily bill of token usage complete. " +
                    "(billing date: {})",
                    yesterday
                );
            }
        }
        finally {
            // (5) 翻转运行标志位
            this.saving.set(false);
        }

        return yesterday;
    }
}