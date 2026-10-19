package com.jesse.response_audit.converter;

import com.jesse.core.entity.AIModelAnswerAuditEntity;
import com.jesse.core.response.AIModelAnswerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link AIModelAnswerAuditConverter} 响应审计实体映射测试。*/
class AIModelAnswerAuditConverterTest
{
    private final AIModelAnswerAuditConverter converter = new AIModelAnswerAuditConverter();

    @Test
    @DisplayName("把响应元数据映射到审计实体")
    void mapsResponseMetadataToAuditEntity()
    {
        final AIModelAnswerResponse response = new AIModelAnswerResponse();
        response.setId("task-123");
        response.setObject("chat.completion");
        response.setCreated(1_781_592_208L);
        response.setModel("deepseek-v4-pro");
        response.setSystemFingerPrint("fp_9954");

        final AIModelAnswerAuditEntity audit = this.converter.convert(42L, response);

        assertThat(audit.getId()).isEqualTo(42L);
        assertThat(audit.getTaskId()).isEqualTo("task-123");
        assertThat(audit.getObject()).isEqualTo("chat.completion");
        assertThat(audit.getCreated()).isEqualTo(1_781_592_208L);
        assertThat(audit.getModel()).isEqualTo("deepseek-v4-pro");
        assertThat(audit.getSystemFingerprint()).isEqualTo("fp_9954");
    }
}
