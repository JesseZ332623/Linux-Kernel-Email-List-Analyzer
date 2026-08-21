package com.jesse.core.components.rabbitmq_callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 全局消息回退处理器分发器。*/
@Slf4j
@Component
public class CompositeReturnsCallback implements RabbitTemplate.ReturnsCallback
{
    /** 业务域 -> 消息回退处理器映射表。*/
    private final
    Map<String, ReturnedMessageHandler> handlerMap;

    /**
     * 构造方法，Spring 会搜索整个项目的消息回退处理器实现并注入 handlers 中，
     * 方法内按业务域分类到 handlerMap 中去。
     */
    public CompositeReturnsCallback(List<ReturnedMessageHandler> handlers)
    {
        this.handlerMap
            = (CollectionUtils.isEmpty(handlers))
                ? Map.of()
                : handlers.stream()
                    .collect(
                        Collectors.toMap(
                            ReturnedMessageHandler::businessDomain,
                            Function.identity()
                        )
                    );
    }

    /**
     * Returned message callback.
     *
     * @param returned the returned message and metadata.
     */
    @Override
    public void returnedMessage(ReturnedMessage returned)
    {
        // (1) 输出回退消息的元数据
        log.error(
            "Message (which id = {}) returned (UNROUTABLE message will be LOST)."
            + "(reply: [{}] {}, exchange: {}, routing key: {})",
            returned.getMessage().getMessageProperties().getMessageId(),
            returned.getReplyCode(),
            returned.getReplyText(),
            returned.getExchange(),
            returned.getRoutingKey()
        );

        final MessageProperties messageProperties
            = returned.getMessage().getMessageProperties();

        final String domain
            = messageProperties.getHeader("business-domain");

        // (2) 按业务域查询对应的回退处理器
        final ReturnedMessageHandler handler
            = this.handlerMap.get(domain);

        if (Objects.isNull(handler))
        {
            log.error("No returned message handler for business domain: {}.", domain);
            return;
        }

        // (3) 执行消息回退
        handler.handler(returned);
    }
}
