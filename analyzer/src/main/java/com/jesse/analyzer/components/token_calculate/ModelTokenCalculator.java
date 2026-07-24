package com.jesse.analyzer.components.token_calculate;

import com.jesse.analyzer.dto.AIModelAnswerUsageDTO;

import java.math.BigDecimal;

/** 模型 Token 资费计算器接口。*/
public interface ModelTokenCalculator
{
    /** 一百万 Tokens */
    BigDecimal ONE_MILLION = new BigDecimal("1000000");

    /** 资费计算精度 */
    int PRCESION = 12;

    /**
     * 根据上游数据库聚合而来的 Token 用量信息，
     * 计算最终的 Token 资费（单位：人民币）。
     */
    BigDecimal calculate(AIModelAnswerUsageDTO modelAnswerUsage);
}