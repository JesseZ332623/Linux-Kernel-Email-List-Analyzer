package com.jesse.analyzer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** AI 模型 token 资费消耗每日汇总表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("ai_model_daily_billing")
public class AIModelDailyBillingEntity
{
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 汇总日期 */
    private LocalDate billingDate;

    /** 模型名称 */
    private String modelName;

    /** 当日累计命中缓存的输入 Token 数 */
    private Long totalPromptCacheHitTokens;

    /** 当日累计未命中缓存的输入 Token 数 */
    private Long totalPromptCacheMissTokens;

    /** 当日累计输出 Token 数 */
    private Long totalCompletionTokens;

    /** 当日本币总花费（保留 6 位小数） */
    private BigDecimal totalCostRmb;

    /** 生成时间 */
    private LocalDateTime createAt;

    /**
     * 在当日没有任何 Token 消耗记录的时候插入这个空对象，
     * 数据库这边会用默认值填充。
     */
    public static AIModelDailyBillingEntity
    makeEmptyDailyBill(Long nextId, LocalDate yesterday)
    {
        final AIModelDailyBillingEntity empty
            = new AIModelDailyBillingEntity();

        empty.setId(nextId);
        empty.setBillingDate(yesterday);
        empty.setCreateAt(LocalDateTime.now());

        return empty;
    }
}