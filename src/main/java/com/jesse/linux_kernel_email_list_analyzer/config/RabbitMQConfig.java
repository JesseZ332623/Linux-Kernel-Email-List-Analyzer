package com.jesse.linux_kernel_email_list_analyzer.config;

import com.jesse.linux_kernel_email_list_analyzer.properties.LKMLRabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/** RabbitMQ 配置类。*/
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig
{
    /** RabbitMQ 队列交换机配置属性类。*/
    private final LKMLRabbitMQProperties properties;

    /** Jackson 消息转换器（队列中的消息按 JSON 存储）。*/
    @Bean
    public MessageConverter messageConverter()
    {
        final JacksonJsonMessageConverter converter
            = new JacksonJsonMessageConverter();

        final DefaultJacksonJavaTypeMapper typeMapper
            = new DefaultJacksonJavaTypeMapper();

        typeMapper.addTrustedPackages(
            "com.jesse.linux_kernal_email_list_analyzer.pojo"
        );

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    /** Spring Rabbit MQ 操作模板配置。*/
    @Bean
    public RabbitTemplate
    rabbitTemplate(ConnectionFactory connectionFactory)
    {
        final RabbitTemplate rabbitTemplate
            = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(this.messageConverter());

        rabbitTemplate.setConfirmCallback(
            (correlationData, ack, cause) -> {
                if (ack)
                {
                    if (Objects.nonNull(correlationData)) {
                        log.info("Message arrived Exchange: {}", correlationData.getId());
                    }
                }
                else
                {
                    if (Objects.nonNull(correlationData))
                    {
                        log.error(
                            "Message send failed: {}, Caused by: {}",
                            correlationData.getId(), cause
                        );
                    }
                }
            }
        );

        return rabbitTemplate;
    }

    /** LKML 内核邮件交换机配置。*/
    @Bean
    public DirectExchange lkmlExchange()
    {
        return new
        DirectExchange(this.properties.getExchangeName(), true, false);
    }

    /** LKML 内核邮件消息队列配置。*/
    @Bean
    public Queue lkmlQueue()
    {
        return
        QueueBuilder.durable(this.properties.getQueueName())
            .deadLetterExchange(this.properties.getDlxExchangeName())
            .deadLetterRoutingKey(this.properties.getDlxRoutingKey())
            .build();
    }

    /** 通用死信交换机配置。*/
    @Bean
    public DirectExchange globalDlxExchange()
    {
        return new
        DirectExchange(this.properties.getDlxExchangeName(), true, false);
    }

    /**  LKML 内核邮件死信消息队列配置。*/
    @Bean
    public Queue lkmlDlxQueue()
    {
        return
        QueueBuilder.durable(this.properties.getDlxQueueName())
                    .build();
    }

    /**
     * LKML 交换机 {@literal <=>} 消息队列的绑定配置。
     * 整个绑定关系如下所示：
     * <pre>
     *                             lkml.key
     * (Suppplier) lkml.exchange ----------> lkml.queue (Consumer)
     *                              |
     *                              | dead letter
     *                              |
     *                              |         lkml.dlx.key
     *                         dlx.exchange ---------------> lkml.dlx.queue (Consumer)
     * </pre>
     *
     * 还需要说明的是，方法中虽然直接调用了 this.lkmlQueue() 这样的方法，
     * 但是在 @Configuration + @Bean 的组合下，Soring 会拦截这个调用，
     * 先去容器里检查有没有已经构造好的实例，有则直接返回，没有则创建。
     */
    @Bean
    public Binding lkmlBinding()
    {
        return
        BindingBuilder.bind(this.lkmlQueue())
            .to(this.lkmlExchange())
            .with(this.properties.getRoutingKey());
    }

    /** LKML 死信 交换机 {@literal <=>} 消息队列的绑定配置。*/
    @Bean
    public Binding lkmlDlxBinding()
    {
        return
        BindingBuilder.bind(this.lkmlDlxQueue())
            .to(this.globalDlxExchange())
            .with(this.properties.getDlxRoutingKey());
    }
}