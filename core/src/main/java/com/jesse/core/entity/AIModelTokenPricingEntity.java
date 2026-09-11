package com.jesse.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 大模型 Token 定价表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("ai_model_token_pricing")
public class AIModelTokenPricingEntity
{
    private Long id;

    /** 模型名称 */
    private String modelName;

    /** 每百万输入（缓存命中）的价格（单价：元，保留六位小数）*/
    private BigDecimal promptCacheHitPrice;

    /** 每百万输入（缓存未命中）的价格（单价：元，保留六位小数）*/
    private BigDecimal promptCacheMissPrice;

    /** 每百万输出的价格（单价：元，保留六位小数）*/
    private BigDecimal completionPrice;

    /** 模型定价创建时间 */
    private LocalDateTime createdAt;

    /** 模型定价创建时间 */
    private LocalDateTime updatedAt;
}