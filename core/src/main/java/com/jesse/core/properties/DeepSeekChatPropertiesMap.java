package com.jesse.core.properties;

import com.jesse.core.pojo.DeepSeekChatProperties;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/** DeepSeek 模型（业务域 -> 模型属性）映射表配置类。*/
@Setter
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app")
public class DeepSeekChatPropertiesMap
{
    /** DeepSeek 模型属性配置类映射表。*/
    private Map<String, DeepSeekChatProperties> deepseekChat;

    /** Linux 内核邮件分析任务模型配置。*/
    public DeepSeekChatProperties getKernelEmailAnalyzerProp() {
        return this.deepseekChat.get("lkml-analyzer");
    }

    /** 获取 内核邮件分析报告答疑解惑模型配置。*/
    public DeepSeekChatProperties getAnalyzerReportChatProp() {
        return this.deepseekChat.get("analyzer-report-chat");
    }

    /** 内核邮件分析报告答疑解惑上下文摘要模型配置。*/
    public DeepSeekChatProperties getAnalyzerReportChatAbstractProp() {
        return this.deepseekChat.get("analyzer-report-chat-abstract");
    }
}