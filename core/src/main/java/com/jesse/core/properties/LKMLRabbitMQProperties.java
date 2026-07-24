package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** RabbitMQ 队列交换机配置属性类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.rabbitmq-queue-props.lkml")
public class LKMLRabbitMQProperties
{
    /** 内核邮件交换机名 */
    private String exchangeName;

    /** 内核邮件消息队列名 */
    private String queueName;

    /** 内核邮件消息路由键（交换机靠本路由键精确投递消息）*/
    private String routingKey;

    /** 公用的死信交换机名 */
    private String dlxExchangeName;

    /** 内核邮件死信消息队列名 */
    private String dlxQueueName;

    /** 内核邮件死信路由键 */
    private String dlxRoutingKey;
}