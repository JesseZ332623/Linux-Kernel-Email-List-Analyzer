package com.jesse.response_audit.service.impl;

import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.dto.AIModelAnswerUsageDTO;
import com.jesse.core.entity.AIModelDailyBillingEntity;
import com.jesse.core.utils.ZoneUtils;
import com.jesse.response_audit.components.token_calculate.ModelTokenCalculator;
import com.jesse.response_audit.repository.AIModelAnswerUsageRepository;
import com.jesse.response_audit.repository.AIModelDailyBillingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link AIModelDailyBillingServiceImpl} 幂等结算与汇总规则测试。*/
class AIModelDailyBillingServiceImplTest
{
    private final GlobalIdConsumer globalIdConsumer = mock(GlobalIdConsumer.class);
    private final AIModelDailyBillingRepository billingRepository
        = mock(AIModelDailyBillingRepository.class);
    private final AIModelAnswerUsageRepository usageRepository
        = mock(AIModelAnswerUsageRepository.class);
    private final ModelTokenCalculator tokenCalculator = mock(ModelTokenCalculator.class);

    private final AIModelDailyBillingServiceImpl service = new AIModelDailyBillingServiceImpl(
        this.globalIdConsumer,
        this.billingRepository,
        this.usageRepository,
        this.tokenCalculator
    );

    private final LocalDate yesterday = LocalDate.now(ZoneUtils.LOCAL_TIMEZONE).minusDays(1);

    @Test
    @DisplayName("上一轮结算未完成时重复调用抛 IllegalStateException")
    void concurrentSaveIsRejected()
    {
        ReflectionTestUtils.setField(this.service, "saving", new AtomicBoolean(true));

        assertThatThrownBy(this.service::save)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("still saving");
    }

    @Test
    @DisplayName("昨日无消耗且无汇总记录时插入空汇总并返回昨日")
    void insertsEmptyBillWhenNoUsageAndNoRecord()
    {
        when(this.usageRepository.getDailyUsageGroupByModel(any(), any())).thenReturn(List.of());
        when(this.billingRepository.hasBillRecordByDate(this.yesterday)).thenReturn(false);
        when(this.globalIdConsumer.nextId()).thenReturn(123L);

        final LocalDate result = this.service.save();

        assertThat(result).isEqualTo(this.yesterday);
        verify(this.billingRepository).insert(
            org.mockito.ArgumentMatchers.<AIModelDailyBillingEntity>argThat(bill ->
                bill.getId() == 123L
                    && bill.getBillingDate().equals(this.yesterday)
                    && bill.getModelName() == null
            )
        );
    }

    @Test
    @DisplayName("昨日无消耗但已有汇总记录时直接返回，不再插入")
    void skipsEmptyBillWhenRecordAlreadyExists()
    {
        when(this.usageRepository.getDailyUsageGroupByModel(any(), any())).thenReturn(List.of());
        when(this.billingRepository.hasBillRecordByDate(this.yesterday)).thenReturn(true);

        final LocalDate result = this.service.save();

        assertThat(result).isEqualTo(this.yesterday);
        verify(this.billingRepository, never())
            .insert(org.mockito.ArgumentMatchers.<AIModelDailyBillingEntity>any());
    }

    @Test
    @DisplayName("有消耗时按模型分组、算费并逐模型 upsert")
    void groupsByModelAndUpsertsPerModel()
    {
        final AIModelAnswerUsageDTO flash = new AIModelAnswerUsageDTO();
        flash.setModel("deepseek-v4-flash");
        flash.setPromptCacheHitTokens(1_000_000L);
        flash.setPromptCacheMissTokens(500_000L);
        flash.setCompletionTokens(250_000L);

        final AIModelAnswerUsageDTO pro = new AIModelAnswerUsageDTO();
        pro.setModel("deepseek-v4-pro");
        pro.setPromptCacheHitTokens(0L);
        pro.setPromptCacheMissTokens(2_000_000L);
        pro.setCompletionTokens(1_000_000L);

        when(this.usageRepository.getDailyUsageGroupByModel(any(), any()))
            .thenReturn(List.of(flash, pro));
        when(this.tokenCalculator.calculate("deepseek-v4-flash", List.of(flash)))
            .thenReturn(new BigDecimal("0.02"));
        when(this.tokenCalculator.calculate("deepseek-v4-pro", List.of(pro)))
            .thenReturn(new BigDecimal("0.04"));
        when(this.globalIdConsumer.nextId()).thenReturn(1L, 2L);

        final LocalDate result = this.service.save();

        assertThat(result).isEqualTo(this.yesterday);

        verify(this.billingRepository).upsertBilling(
            org.mockito.ArgumentMatchers.argThat(bill ->
                bill.getModelName().equals("deepseek-v4-flash")
                    && bill.getTotalPromptCacheHitTokens() == 1_000_000L
                    && bill.getTotalPromptCacheMissTokens() == 500_000L
                    && bill.getTotalCompletionTokens() == 250_000L
                    && bill.getTotalCostRmb().compareTo(new BigDecimal("0.02")) == 0
            )
        );
        verify(this.billingRepository).upsertBilling(
            org.mockito.ArgumentMatchers.argThat(bill ->
                bill.getModelName().equals("deepseek-v4-pro")
                    && bill.getTotalCostRmb().compareTo(new BigDecimal("0.04")) == 0
            )
        );
    }
}
