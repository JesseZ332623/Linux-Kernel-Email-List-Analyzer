package com.jesse.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link AmqpMessagePropertiesUtils} 必要字段 JSON 序列化行为测试。*/
class AmqpMessagePropertiesUtilsTest
{
    private final AmqpMessagePropertiesUtils utils
        = new AmqpMessagePropertiesUtils(new ObjectMapper());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode toJsonNode(MessageProperties props) throws Exception
    {
        return this.objectMapper.readTree(this.utils.toJson(props));
    }

    @Test
    @DisplayName("null 输入退化为空对象 {}")
    void nullInputYieldsEmptyObject() throws Exception
    {
        assertThat(this.utils.toJson(null)).isEqualTo("{}");
    }

    @Test
    @DisplayName("消息标识、来源、投递、属性、用户字段被逐一提取")
    void extractsEssentialFields() throws Exception
    {
        final MessageProperties props = new MessageProperties();
        props.setMessageId("msg-1");
        props.setCorrelationId("corr-1");
        props.setConsumerQueue("lkml.queue");
        props.setReceivedExchange("lkml.exchange");
        props.setReceivedRoutingKey("lkml.routing");
        props.setConsumerTag("tag-1");
        props.setDeliveryTag(42L);
        props.setRetryCount(3L);
        props.setRedelivered(true);
        props.setReceivedDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setContentType("application/json");
        props.setContentEncoding("UTF-8");
        props.setType("lkml");
        props.setPriority(7);
        props.setExpiration("60000");
        props.setTimestamp(new Date(1_700_000_000_000L));
        props.setUserId("guest");
        props.setAppId("analyzer");

        final JsonNode node = this.toJsonNode(props);

        assertThat(node.get("messageId").asText()).isEqualTo("msg-1");
        assertThat(node.get("correlationId").asText()).isEqualTo("corr-1");
        assertThat(node.get("queue").asText()).isEqualTo("lkml.queue");
        assertThat(node.get("exchange").asText()).isEqualTo("lkml.exchange");
        assertThat(node.get("routingKey").asText()).isEqualTo("lkml.routing");
        assertThat(node.get("consumerTag").asText()).isEqualTo("tag-1");
        assertThat(node.get("deliveryTag").asLong()).isEqualTo(42L);
        assertThat(node.get("retryCount").asInt()).isEqualTo(3);
        assertThat(node.get("redelivered").asBoolean()).isTrue();
        assertThat(node.get("deliveryMode").asText()).isEqualTo("PERSISTENT");
        assertThat(node.get("contentType").asText()).isEqualTo("application/json");
        assertThat(node.get("contentEncoding").asText()).isEqualTo("UTF-8");
        assertThat(node.get("type").asText()).isEqualTo("lkml");
        assertThat(node.get("priority").asInt()).isEqualTo(7);
        assertThat(node.get("expiration").asText()).isEqualTo("60000");
        assertThat(node.get("userId").asText()).isEqualTo("guest");
        assertThat(node.get("appId").asText()).isEqualTo("analyzer");
        assertThat(node.has("timestamp")).isTrue();
    }

    @Test
    @DisplayName("关键 header 被保留，而 __TypeId__ 被剔除")
    void keepsKeyHeadersAndDropsTypeId() throws Exception
    {
        final MessageProperties props = new MessageProperties();
        props.setHeader("traceId", "abc-123");
        props.setHeader("__TypeId__", "com.example.SomePayload");

        final JsonNode headers = this.toJsonNode(props).get("keyHeaders");

        assertThat(headers.get("traceId").asText()).isEqualTo("abc-123");
        assertThat(headers.has("__TypeId__")).isFalse();
    }

    @Test
    @DisplayName("未设置的可选字段由于 NON_NULL 不参与序列化")
    void unsetOptionalFieldsAreOmitted() throws Exception
    {
        final JsonNode node = this.toJsonNode(new MessageProperties());

        assertThat(node.has("messageId")).isFalse();
        assertThat(node.has("timestamp")).isFalse();
    }

    @Test
    @DisplayName("投递模式在未显式设置时按 MessageProperties 默认值落为 PERSISTENT")
    void deliveryModeDefaultsToPersistent() throws Exception
    {
        final JsonNode node = this.toJsonNode(new MessageProperties());

        assertThat(node.get("deliveryMode").asText()).isEqualTo("PERSISTENT");
    }
}
