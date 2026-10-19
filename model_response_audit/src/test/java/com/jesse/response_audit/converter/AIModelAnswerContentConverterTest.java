package com.jesse.response_audit.converter;

import com.jesse.core.entity.AIModelAnswerContentEntity;
import com.jesse.core.pojo.ai.AIModelAnswerChoice;
import com.jesse.core.pojo.ai.AIModelAnswerMessage;
import com.jesse.core.response.AIModelAnswerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link AIModelAnswerContentConverter} 模型输出内容实体映射测试。*/
class AIModelAnswerContentConverterTest
{
    private final AIModelAnswerContentConverter converter = new AIModelAnswerContentConverter();

    @Test
    @DisplayName("把首个 choice 的文本与推理内容映射到内容实体")
    void mapsFirstChoiceContentAndReasoning()
    {
        final AIModelAnswerMessage message
            = new AIModelAnswerMessage("assistant", "Hi there!", "simple greeting");

        final AIModelAnswerChoice choice = new AIModelAnswerChoice();
        choice.setMessage(message);

        final AIModelAnswerResponse response = new AIModelAnswerResponse();
        response.setId("task-456");
        response.setChoices(List.of(choice));

        final AIModelAnswerContentEntity content = this.converter.convert(7L, response);

        assertThat(content.getId()).isEqualTo(7L);
        assertThat(content.getTaskId()).isEqualTo("task-456");
        assertThat(content.getContent()).isEqualTo("Hi there!");
        assertThat(content.getReasoningContent()).isEqualTo("simple greeting");
    }
}
