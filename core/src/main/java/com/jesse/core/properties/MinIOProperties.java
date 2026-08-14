package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** MinIO OSS 服务属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties("app.minio")
public class MinIOProperties
{
    /** 访问端点 */
    private String endpoint;

    /** 访问密钥（对应用户名）*/
    private String accessKey;

    /** 秘密密钥（对应密码）*/
    private String secretKey;

    /** 存储分析报告对象的桶名 */
    private String analyzeReportBucket;
}