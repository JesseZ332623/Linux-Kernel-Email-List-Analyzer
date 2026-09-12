package com.jesse.response_audit.components.token_calculate;

import com.jesse.core.dto.AIModelAnswerUsageDTO;

import java.math.BigDecimal;
import java.util.List;

/** 模型 Token 资费计算器接口。*/
public interface ModelTokenCalculator
{
    /** 一百万 Tokens */
    BigDecimal ONE_MILLION = new BigDecimal("1000000");

    /** 资费计算精度 */
    int PRECISION = 12;

    /**
     * 根据上游数据库聚合而来的 Token 用量信息，
     * 计算最终的 Token 资费（单位：人民币）。
     *
     * @param modelName 模型名称
     * @param usages    该模型本次收集到的 Token 用量信息
     *
     * @return 当前定价下的最终的 Token 资费
     */
    BigDecimal calculate(String modelName, List<AIModelAnswerUsageDTO> usages);
}