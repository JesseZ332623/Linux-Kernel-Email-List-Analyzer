package com.jesse.analyzer.components.kernel_email_pusher.impl;

import com.jesse.core.components.rabbitmq_callback.ReturnedMessageHandler;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.stereotype.Component;

/**
 * 内核邮件投递专用的消息回退处理器。
 *
 * <p>
 * 本实现刻意为空：路由失败的状态流转由 {@code awaitPublisherConfirm()}
 * 通过 {@code CorrelationData.getReturned()} 在主链上统一处理，
 * 以避免 IO 线程与推送线程并发写状态机。
 * 本类目前仅用于声明业务域，供分发器路由。
 * </p>
 */
@Component("lkml-returned-message-handler")
public class KernelEmailReturnedMessageHandler
    implements ReturnedMessageHandler
{
    /** 业务域常量。*/
    public static final
    String BUSINESS_DOMAIN = "lkml-analyzer";

    /** 哪个业务域？*/
    @Override
    public String businessDomain() {
        return BUSINESS_DOMAIN;
    }

    /** 处理一条回退的消息。*/
    @Override
    public void handler(ReturnedMessage returned) {
        // NOTHING TO DO
    }
}