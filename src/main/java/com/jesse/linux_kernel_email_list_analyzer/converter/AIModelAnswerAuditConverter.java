package com.jesse.linux_kernel_email_list_analyzer.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerAudit;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelAnswerMessage;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelAnswerUsage;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** AI 模型 LKML 分析任务响应审计实体转换器。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class AIModelAnswerAuditConverter
{
    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    public AIModelAnswerAudit
    convert(Long nextId, AIModelAnswerResponse response)
    {
        final AIModelAnswerAudit audit = new AIModelAnswerAudit();

        final AIModelAnswerMessage message
            = response.getChoices().getFirst().getMessage();

        final AIModelAnswerUsage usage = response.getUsage();

        audit.setId(nextId);
        audit.setTaskId(response.getId());
        audit.setObject(response.getObject());
        audit.setCreated(response.getCreated());
        audit.setModel(response.getModel());

        audit.setContent(message.getContent());
        audit.setReasoningContent(message.getReasonContent());

        audit.setSystemFingerprint(response.getSystemFingerPrint());

        audit.setPromptTokens(usage.getPromptTokens());
        audit.setCompletionTokens(usage.getCompletionTokens());
        audit.setTotalTokens(usage.getTotalTokens());

        try
        {
            audit.setPromptTokensDetails(
                this.objectMapper
                    .writeValueAsString(usage.getPromptTokensDetails())
            );

            audit.setCompletionTokensDetails(
                this.objectMapper
                    .writeValueAsString(usage.getCompletionTokensDetails())
            );
        }
        catch (JsonProcessingException exception)
        {
            log.error("Deserialization token details failed.", exception);

            audit.setPromptTokensDetails(null);
            audit.setCompletionTokensDetails(null);
        }

        audit.setPromptCacheHitTokens(usage.getPromptCacheHitTokens());
        audit.setPromptCacheMissTokens(usage.getPromptCacheMissTokens());

        audit.setCreateTime(LocalDateTime.now());

        return audit;
    }
}