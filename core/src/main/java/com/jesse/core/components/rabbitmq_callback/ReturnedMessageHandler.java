package com.jesse.core.components.rabbitmq_callback;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 回退消息处理器接口。
 * 未来如果有多个模块需要共用同一个 {@link RabbitTemplate} 处理消息回退，
 * 则可以实现本接口来表达 “某个业务要怎么回退消息”。
 */
public interface ReturnedMessageHandler
{
    /** 哪个业务域？ */
    String businessDomain();

    /** 处理一条回退的消息。*/
    void handler(ReturnedMessage returned);
}