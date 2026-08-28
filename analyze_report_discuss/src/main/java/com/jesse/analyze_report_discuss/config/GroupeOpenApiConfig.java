package com.jesse.analyze_report_discuss.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 内核邮件分析报告讨论模块 OpenAPI 分组配置。*/
@Configuration("analyzer-report-discuss-openapi-conf")
public class GroupeOpenApiConfig
{
    @Bean(name = "analyzer-report-discuss-grouped-openapi")
    public GroupedOpenApi analyzerApi()
    {
        return
        GroupedOpenApi.builder()
            .group("内核补丁邮件分析报告讨论模块")
            .pathsToMatch("/api/analyze-report/**")
            .build();
    }
}