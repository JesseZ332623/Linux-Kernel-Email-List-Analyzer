package com.jesse.response_audit.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.entity.AIModelAnswerUsageEntity;
import com.jesse.core.pojo.ai.AIModelAnswerUsage;
import com.jesse.core.response.AIModelAnswerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link AIModelAnswerUsageConverter} Token 消耗明细实体映射测试。*/
class AIModelAnswerUsageConverterTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AIModelAnswerUsageConverter converter
        = new AIModelAnswerUsageConverter(this.objectMapper);

    @Test
    @DisplayName("把 token 用量与详情 JSON 映射到用量实体")
    void mapsUsageFieldsAndDetailsJson() throws Exception
    {
        final AIModelAnswerUsage usage = new AIModelAnswerUsage();
        usage.setPromptTokens(12L);
        usage.setCompletionTokens(43L);
        usage.setTotalTokens(55L);
        usage.setPromptTokensDetails(Map.of("cached_tokens", 0));
        usage.setCompletionTokensDetails(Map.of("reasoning_tokens", 32));
        usage.setPromptCacheHitTokens(0L);
        usage.setPromptCacheMissTokens(12L);

        final AIModelAnswerResponse response = new AIModelAnswerResponse();
        response.setId("task-789");
        response.setUsage(usage);

        final AIModelAnswerUsageEntity entity = this.converter.convert(9L, response);

        assertThat(entity.getId()).isEqualTo(9L);
        assertThat(entity.getTaskId()).isEqualTo("task-789");
        assertThat(entity.getPromptTokens()).isEqualTo(12L);
        assertThat(entity.getCompletionTokens()).isEqualTo(43L);
        assertThat(entity.getTotalTokens()).isEqualTo(55L);
        assertThat(entity.getPromptCacheHitTokens()).isEqualTo(0L);
        assertThat(entity.getPromptCacheMissTokens()).isEqualTo(12L);

        final JsonNode promptDetails
            = this.objectMapper.readTree(entity.getPromptTokensDetails());
        assertThat(promptDetails.get("cached_tokens").asInt()).isZero();

        final JsonNode completionDetails
            = this.objectMapper.readTree(entity.getCompletionTokensDetails());
        assertThat(completionDetails.get("reasoning_tokens").asInt()).isEqualTo(32);
    }
}
