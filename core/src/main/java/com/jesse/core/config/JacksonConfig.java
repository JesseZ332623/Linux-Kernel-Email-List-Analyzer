package com.jesse.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Jackson 配置类。*/
@Configuration
public class JacksonConfig
{
    /** Spring 默认使用的对象映射器。*/
    @Bean
    @Primary
    public ObjectMapper defaultObjectMapper() {
        return new ObjectMapper();
    }

    /** 输出格式化 JSON 的对象映射器。*/
    @Bean(name = "pretty-object-mapper")
    public ObjectMapper prettyObjectMapper()
    {
        return new
        ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}