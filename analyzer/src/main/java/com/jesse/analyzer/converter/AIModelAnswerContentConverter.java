package com.jesse.analyzer.converter;

import com.jesse.analyzer.entity.AIModelAnswerContentEntity;
import com.jesse.analyzer.pojo.ai.AIModelAnswerMessage;
import com.jesse.analyzer.response.AIModelAnswerResponse;
import org.springframework.stereotype.Component;

/** AI 模型 LKML 分析任务模型文本输出实体转换器。*/
@Component
public class AIModelAnswerContentConverter
{
    public AIModelAnswerContentEntity
    convert(Long nextId, AIModelAnswerResponse response)
    {
        final AIModelAnswerContentEntity content = new AIModelAnswerContentEntity();
        final AIModelAnswerMessage message = response.getChoices().getFirst().getMessage();

        content.setId(nextId);
        content.setTaskId(response.getId());
        content.setContent(message.getContent());
        content.setReasoningContent(message.getReasonContent());

        return content;
    }
}