package com.jesse.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/** DeepSeek 分析报告答疑解惑模块模型属性配置类。*/
@Setter
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app")
public class DeepSeekAnalyzerReportDiscussProperties
{
    private Map<String, DeepSeekChatProperties> deepseekChat;

    /** 获取 内核邮件分析报告答疑解惑模型配置。*/
    public DeepSeekChatProperties getAnalyzerReportChatProp() {
        return this.deepseekChat.get("analyzer-report-chat");
    }

    /** 内核邮件分析报告答疑解惑上下文摘要模型配置。*/
    public DeepSeekChatProperties getAnalyzerReportChatAbstractProp() {
        return this.deepseekChat.get("analyzer-report-chat-abstract");
    }
}