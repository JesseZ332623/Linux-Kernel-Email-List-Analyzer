package com.jesse.core.components.global_id.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.properties.IdConsumerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 全局 ID 消费机实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIdConsumerImpl implements GlobalIdConsumer
{
    /** Spring 封装的 HTTP 客户端。*/
    private final RestTemplate restTemplate;

    /** 全局 ID 消费机属性类。*/
    private final IdConsumerProperties properties;

    /** Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 获取下一个 ID */
    @Override
    public long nextId()
    {
        final String nextIdEndpoint
            = this.properties.getDirectoryUrls().getNext();

        final String responseJSON
            = this.restTemplate.getForObject(nextIdEndpoint, String.class);

        try
        {
            return
            this.objectMapper.readTree(responseJSON)
                .get("data")
                .asLong();
        }
        catch (JsonProcessingException exception)
        {
            log.error("Unexpected response {}", responseJSON, exception);
            return -1;
        }
    }
}