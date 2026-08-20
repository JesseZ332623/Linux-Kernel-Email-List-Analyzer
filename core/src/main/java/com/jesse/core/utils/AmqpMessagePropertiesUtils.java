package com.jesse.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h3>2026.08.20 新增</h3>
 * {@link MessageProperties} 必要字段 JSON 序列化工具类。
 */
@Component
@RequiredArgsConstructor
final public class AmqpMessagePropertiesUtils
{
    /** 输出格式化 JSON 的对象映射器。*/
    @Qualifier("pretty-object-mapper")
    private final ObjectMapper prettyObjectMapper;

    /**
     * 直接序列化 {@link MessageProperties} 显然是个 “坏味道”，
     * 需要把一些重要的字段提取到自定义的 DTO 中再序列化。
     */
    public String toJson(MessageProperties messageProperties)
    {
        try
        {
            if (Objects.isNull(messageProperties)) {
                return "{}";
            }

            final RecordedMessageProperties recordedMessageProperties
                = RecordedMessageProperties.of(messageProperties);

            return
            this.prettyObjectMapper
                .writeValueAsString(recordedMessageProperties);
        }
        catch (JsonProcessingException ignore) {
            return "{}";
        }
    }

    /** 只记录 {@link MessageProperties} 需要的字段。*/
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecordedMessageProperties
    {
        // 消息标识
        private String messageId;
        private String correlationId;

        // 消息来源
        private String queue;
        private String exchange;
        private String routingKey;
        private String consumerTag;

        // 投递信息
        private Long deliveryTag;
        private Integer retryCount;
        private Boolean redelivered;
        private String deliveryMode;

        // 消息属性
        private String contentType;
        private String contentEncoding;
        private String type;
        private Integer priority;
        private String expiration;
        private String timestamp;
        private Long contentLength;

        // 用户信息
        private String userId;
        private String appId;

        // 关键 headers（如 traceId）
        private Map<String, Object> keyHeaders;

        public static
        RecordedMessageProperties of(MessageProperties props)
        {
            if (props == null) {
                return null;
            }

            final RecordedMessageProperties info = new RecordedMessageProperties();

            // 消息标识
            info.setMessageId(props.getMessageId());
            info.setCorrelationId(props.getCorrelationId());

            // 消息来源
            info.setQueue(props.getConsumerQueue());
            info.setExchange(props.getReceivedExchange());
            info.setRoutingKey(props.getReceivedRoutingKey());
            info.setConsumerTag(props.getConsumerTag());

            // 投递信息
            info.setDeliveryTag(props.getDeliveryTag());
            info.setRetryCount((int) props.getRetryCount());
            info.setRedelivered(props.isRedelivered());

            if (props.getReceivedDeliveryMode() != null) {
                info.setDeliveryMode(props.getReceivedDeliveryMode().toString());
            }
            else if (props.getDeliveryMode() != null) {
                info.setDeliveryMode(props.getDeliveryMode().toString());
            }

            // 消息属性
            info.setContentType(props.getContentType());
            info.setContentEncoding(props.getContentEncoding());
            info.setType(props.getType());
            info.setPriority(props.getPriority());
            info.setExpiration(props.getExpiration());

            if (Objects.nonNull(props.getTimestamp())) {
                info.setTimestamp(props.getTimestamp().toString());
            }

            info.setContentLength(props.getContentLength());

            // 用户信息
            info.setUserId(props.getUserId());
            info.setAppId(props.getAppId());

            // 消息元数据头筛选掉一些无用的数据后保存
            info.setKeyHeaders(
                props.getHeaders().entrySet().stream()
                    .filter((entry) ->
                        !entry.getKey().equals("__TypeId__"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );

            return info;
        }
    }
}
