package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 内核邮件分析报告文本缓存属性类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.analyze-report-cache")
public class AnalyzeReportCacheProperties
{
    /** 内核邮件分析报告缓存键前缀 */
    private String analyzeReportKeyPrefix;

    /** 讨论摘要文本缓存键前缀 */
    private String discussAbstractKeyPrefix;

    /** 缓存有效期 */
    private Duration expire;

    /** 缓存击穿后的锁等待时间 */
    private Duration lockWaitTime;
}
