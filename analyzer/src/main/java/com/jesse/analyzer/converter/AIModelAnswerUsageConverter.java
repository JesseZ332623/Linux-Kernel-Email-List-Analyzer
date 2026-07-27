package com.jesse.analyzer.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyzer.entity.AIModelAnswerUsageEntity;
import com.jesse.analyzer.pojo.ai.AIModelAnswerUsage;
import com.jesse.analyzer.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** AI 模型 LKML 分析任务 Token 消耗明细实体转换器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class AIModelAnswerUsageConverter
{
    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    public AIModelAnswerUsageEntity
    convert(Long nextId, AIModelAnswerResponse response)
    {
        final AIModelAnswerUsageEntity usageEntity
            = new AIModelAnswerUsageEntity();

        final AIModelAnswerUsage usage = response.getUsage();

        usageEntity.setId(nextId);
        usageEntity.setTaskId(response.getId());
        usageEntity.setPromptTokens(usage.getPromptTokens());
        usageEntity.setCompletionTokens(usage.getCompletionTokens());
        usageEntity.setTotalTokens(usage.getTotalTokens());

        try
        {
            usageEntity.setPromptTokensDetails(
                this.objectMapper
                    .writeValueAsString(usage.getPromptTokensDetails())
            );

            usageEntity.setCompletionTokensDetails(
                this.objectMapper
                    .writeValueAsString(usage.getCompletionTokensDetails())
            );
        }
        catch (JsonProcessingException exception) {
            log.error("Deserialization token details failed.", exception);
        }

        usageEntity.setPromptCacheHitTokens(usage.getPromptCacheHitTokens());
        usageEntity.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());

        return usageEntity;
    }
}
