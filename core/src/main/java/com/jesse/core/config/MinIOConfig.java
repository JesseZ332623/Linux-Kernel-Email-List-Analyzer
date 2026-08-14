package com.jesse.core.config;

import com.jesse.core.properties.MinIOProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** 使用 S3 标准 SDK 对接 MinIO OSS 服务配置。*/
@Configuration
@RequiredArgsConstructor
public class MinIOConfig
{
    /** MinIO OSS 服务属性配置类。*/
    private final MinIOProperties minIOProperties;

    /** S3 客户端配置。*/
    @Primary
    @Bean(name = "minio-client")
    public S3Client s3Client()
    {
        // 构造 AWS 基础凭证实例
        final AwsBasicCredentials basicCredentials
            = AwsBasicCredentials.create(
                this.minIOProperties.getAccessKey(),
                this.minIOProperties.getSecretKey()
        );

        // S3 标准的配置
        final S3Configuration s3Configuration
            = S3Configuration.builder()
                // 使用路径风格
                .pathStyleAccessEnabled(true)
                .build();

        return
        S3Client.builder()
            .endpointOverride(URI.create(minIOProperties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build();
    }
}