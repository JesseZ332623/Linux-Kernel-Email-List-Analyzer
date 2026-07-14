package com.jesse.linux_kernel_email_list_analyzer.converter;

import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerAuditEntity;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import org.springframework.stereotype.Component;

/** AI 模型 LKML 分析任务响应审计实体转换器。*/
@Component
public class AIModelAnswerAuditConverter
{
    public  AIModelAnswerAuditEntity
    convert(Long nextId, AIModelAnswerResponse response)
    {
        final AIModelAnswerAuditEntity audit = new AIModelAnswerAuditEntity();

        audit.setId(nextId);
        audit.setTaskId(response.getId());
        audit.setObject(response.getObject());
        audit.setCreated(response.getCreated());
        audit.setModel(response.getModel());

        audit.setSystemFingerprint(response.getSystemFingerPrint());

        return audit;
    }
}