package com.jesse.analyzer.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 内核补丁邮件推送、分析模块 OpenAPI 分组配置类。*/
@Configuration("analyzer-grouped-openapi-conf")
public class GroupeOpenApiConfig
{
    @Bean(name = "analyzer-grouped-openapi")
    public GroupedOpenApi analyzerApi()
    {
        return
        GroupedOpenApi.builder()
            .group("内核补丁邮件推送、分析模块")
            .pathsToMatch("/api/analyzer/**")
            .build();
    }
}