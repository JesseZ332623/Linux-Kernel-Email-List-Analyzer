package com.jesse.response_audit.service.impl;

import com.jesse.response_audit.components.token_calculate.ModelTokenCalculator;
import com.jesse.core.dto.AIModelAnswerUsageDTO;
import com.jesse.core.entity.AIModelDailyBillingEntity;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.utils.ZoneUtils;
import com.jesse.response_audit.repository.AIModelAnswerUsageRepository;
import com.jesse.response_audit.repository.AIModelDailyBillingRepository;
import com.jesse.response_audit.service.AIModelDailyBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** AI 模型 token 资费消耗每日汇总表服务类实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelDailyBillingServiceImpl implements AIModelDailyBillingService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** AI 模型 token 资费消耗每日汇总表仓储类。*/
    private final
    AIModelDailyBillingRepository aiModelDailyBillingRepository;

    /** AI 模型 LKML 分析任务 Token 消耗明细表仓储类。*/
    private final
    AIModelAnswerUsageRepository aiModelAnswerUsageRepository;

    /** 模型计算器实现类。*/
    private final ModelTokenCalculator modelTokenCalculator;

    /** 是否正在执行保存？避免自动 / 手动调用冲突。*/
    private final
    AtomicBoolean saving = new AtomicBoolean(false);

    /** 组装 Token 消耗与资费汇总实体。*/
    private AIModelDailyBillingEntity makeDailyBilling(
        final LocalDate         yesterday,
        final String            modelName,
        final BigDecimal        costRmb,
        final List<AIModelAnswerUsageDTO> dailyUsages
    )
    {
        final AIModelDailyBillingEntity dailyBilling
            = new AIModelDailyBillingEntity();

        long totalPromptCacheHitTokens  = 0L;
        long totalPromptCacheMissTokens = 0L;
        long totalCompletionTokens      = 0L;

        for (AIModelAnswerUsageDTO usage : dailyUsages)
        {
            totalPromptCacheHitTokens  += usage.getPromptCacheHitTokens();
            totalPromptCacheMissTokens += usage.getPromptCacheMissTokens();
            totalCompletionTokens      += usage.getCompletionTokens();
        }

        dailyBilling.setId(this.globalIdConsumer.nextId());
        dailyBilling.setBillingDate(yesterday);
        dailyBilling.setModelName(modelName);
        dailyBilling.setTotalPromptCacheHitTokens(totalPromptCacheHitTokens);
        dailyBilling.setTotalPromptCacheMissTokens(totalPromptCacheMissTokens);
        dailyBilling.setTotalCompletionTokens(totalCompletionTokens);

        dailyBilling.setTotalCostRmb(costRmb);
        dailyBilling.setCreateAt(LocalDateTime.now(ZoneUtils.LOCAL_TIMEZONE));

        return dailyBilling;
    }

    /** 每天凌晨 4 点自动保存每天所有模型 token 资费消耗汇总数据。*/
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
                "Previous daily billing is still saving, skip this round."
            );
        }

        final LocalDate yesterday
            = LocalDate.now(ZoneUtils.LOCAL_TIMEZONE).minusDays(1);

        final LocalDateTime startOfDay = yesterday.atStartOfDay();
        final LocalDateTime endOfDay   = yesterday.atTime(LocalTime.MAX);

        try
        {
            // (1) 查询昨天一整天不同模型总共的 Token 消耗明细。
            final List<AIModelAnswerUsageDTO> dailyUsage
                = this.aiModelAnswerUsageRepository
                      .getDailyUsageGroupByModel(startOfDay, endOfDay);

            // 如果昨日没有任何 Token 消耗记录
            if (CollectionUtils.isEmpty(dailyUsage))
            {
                // 如果已经有记录了（比如同一天内反复调用），直接返回
                if (this.aiModelDailyBillingRepository.hasBillRecordByDate(yesterday)) {
                    return yesterday;
                }

                // 构造空记录并插入即可
                final long nextId = this.globalIdConsumer.nextId();

                this.aiModelDailyBillingRepository
                    .insert(AIModelDailyBillingEntity.makeEmptyDailyBill(nextId, yesterday));

                log.info(
                    "No AI model consumption found on {}, " +
                    "insert empty daily bill record (id = {})",
                    yesterday, nextId
                );

                return yesterday;
            }

            // (2) 再按照 model 分组成 Map<String, List<AIModelAnswerUsageDTO>>
            final Map<String, List<AIModelAnswerUsageDTO>> modelDailyUsageMap
                = dailyUsage.stream()
                    .collect(Collectors.groupingBy(AIModelAnswerUsageDTO::getModel));

            for (var modelDailyUsage : modelDailyUsageMap.entrySet())
            {
                final String modelName                        = modelDailyUsage.getKey();
                final List<AIModelAnswerUsageDTO> dailyUsages = modelDailyUsage.getValue();

                // (3) 计算该模型昨日的 token 资费
                final BigDecimal costRmb
                    = this.modelTokenCalculator.calculate(modelName, dailyUsages);

                // (4) 组装 Token 消耗与资费汇总实体
                final AIModelDailyBillingEntity dailyBilling
                    = this.makeDailyBilling(yesterday, modelName, costRmb, dailyUsages);

                // (5) 保存昨日的 Token 消耗与资费汇总数据
                this.aiModelDailyBillingRepository.upsertBilling(dailyBilling);
            }

            log.info(
                "Save AI model daily bill of token usage complete. " +
                "(billing date: {})",
                yesterday
            );
        }
        finally {
            // (6) 翻转运行标志位
            this.saving.set(false);
        }

        return yesterday;
    }
}