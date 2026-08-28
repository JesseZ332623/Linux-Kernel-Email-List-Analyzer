package com.jesse.core.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Open API 配置类。*/
@Configuration
public class OpenApiConfig
{
    @Bean
    public OpenAPI openAPI()
    {
        final ExternalDocumentation externalDocumentation
            = new ExternalDocumentation()
                    .description("项目地址")
                    .url("https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer");

        final Contact contact
            = new Contact().name("Jesse")
                    .email("zhj3191955858@gmail.com");

        final Info info
            = new Info().title("Linux 内核补丁邮件分析、归档、讨论服务")
                    .description("项目接口文档描述")
                    .version("v1.0.0")
                    .contact(contact);

        return new
        OpenAPI().info(info)
            .externalDocs(externalDocumentation);
    }
}