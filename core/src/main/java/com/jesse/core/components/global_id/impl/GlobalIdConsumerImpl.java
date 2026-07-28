package com.jesse.core.components.global_id.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.properties.IdConsumerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
        catch (JsonProcessingException exception) {
            log.error("Unexpected response {}", responseJSON, exception);
            return -1;
        }
    }

    /** 获取下一批 ID */
    @Override
    public List<Long> nextIds(int batchSize)
    {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Param batchSize must large then 0");
        }

        final String nextIdsEndPoint
            = this.properties.getDirectoryUrls().getNextBatch()
                + "?size=" + batchSize;

        final String responseJSON
            = this.restTemplate.getForObject(nextIdsEndPoint, String.class);

        try
        {
            return
            this.objectMapper.readValue(
                this.objectMapper.readTree(responseJSON)
                    .get("data")
                    .toString(),
                new TypeReference<>() {}
            );
        }
        catch (JsonProcessingException exception)
        {
            log.error("Unexpected response {}", responseJSON, exception);
            return List.of();
        }
    }
}