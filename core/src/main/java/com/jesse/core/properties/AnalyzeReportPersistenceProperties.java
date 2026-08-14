package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** LKML 分析报告持久化配置属性类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.analyze-report-persistence")
public class AnalyzeReportPersistenceProperties
{
    /** 报告需要保存到的本地路径 */
    private String reportPathPrefix;

    /** 存储分析报告对象的桶名 */
    private String reportBucketName;

    /** 报告文件名最大长度 */
    private int maxFileNameLen;
}
