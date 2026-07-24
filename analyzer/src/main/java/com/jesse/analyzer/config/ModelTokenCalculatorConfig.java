package com.jesse.analyzer.config;

import com.jesse.analyzer.components.token_calculate.ModelTokenCalculator;
import com.jesse.analyzer.components.token_calculate.impl.DeepSeekV4FlashTokenCalculator;
import com.jesse.analyzer.components.token_calculate.impl.DeepSeekV4ProTokenCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/** 模型 Token 资费计算器表配置类。*/
@Configuration
public class ModelTokenCalculatorConfig
{
    /** 手动配置模型 Token 资费计算器表。*/
    @Bean(name = "model-token-calculator-map")
    public Map<String, ModelTokenCalculator> modelTokenCalculatorMap()
    {
        return
        Map.of(
            "deepseek-v4-pro",   new DeepSeekV4ProTokenCalculator(),
            "deepseek-v4-flash", new DeepSeekV4FlashTokenCalculator()
        );
    }
}