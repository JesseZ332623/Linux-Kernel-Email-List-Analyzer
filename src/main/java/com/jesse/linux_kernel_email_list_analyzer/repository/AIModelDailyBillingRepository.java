package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelDailyBillingEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** AI 模型 token 资费消耗每日汇总表仓储类。*/
@Mapper
public interface AIModelDailyBillingRepository
    extends BaseMapper<AIModelDailyBillingEntity>
{
    /**
     * 由于 ai_model_daily_billing 表是复合主键，
     * 所以应该使用 MySQL 方言：
     *
     * <pre>
     * # 有则更新，无则插入
     * INSERT INTO ... ON DUPLICATE KEY UPDATE ...
     * </pre>
     *
     * 语法代替 {@link BaseMapper} 中的 insert()，
     * 这样在意外重复执行本方法的时候会用新值覆盖旧值，确保不会出现主键冲突异常。
     */
    @Insert("""
        INSERT INTO `ai_model_daily_billing`(
            id,
            billing_date,
            model_name,
            total_prompt_cache_hit_tokens,
            total_prompt_cache_miss_tokens,
            total_completion_tokens,
            total_cost_rmb,
            create_at
        )
        VALUES (
            #{entity.id},
            #{entity.billingDate},
            #{entity.modelName},
            #{entity.totalPromptCacheHitTokens},
            #{entity.totalPromptCacheMissTokens},
            #{entity.totalCompletionTokens},
            #{entity.totalCostRmb},
            #{entity.createAt}
        )
        ON DUPLICATE KEY UPDATE
            total_prompt_cache_hit_tokens  = VALUES(total_prompt_cache_hit_tokens),
            total_prompt_cache_miss_tokens = VALUES(total_prompt_cache_miss_tokens),
            total_completion_tokens        = VALUES(total_completion_tokens),
            total_cost_rmb                 = VALUES(total_cost_rmb),
            create_at                      = VALUES(create_at)
    """)
    int upsertBilling(@Param("entity") AIModelDailyBillingEntity entity);
}