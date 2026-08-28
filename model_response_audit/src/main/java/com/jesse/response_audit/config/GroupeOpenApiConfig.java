package com.jesse.response_audit.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 大模型 API 调用响应审计模块 OpenAPI 分组配置。*/
@Configuration("model-response-audit-openapi-conf")
public class GroupeOpenApiConfig
{
    @Bean(name = "model-response-audit-openapi")
    public GroupedOpenApi analyzerApi()
    {
        return
        GroupedOpenApi.builder()
            .group("大模型 API 调用响应元数据、Token 消耗、资费审计模块")
            .pathsToMatch("/api/ai-model-audit/**")
            .build();
    }
}