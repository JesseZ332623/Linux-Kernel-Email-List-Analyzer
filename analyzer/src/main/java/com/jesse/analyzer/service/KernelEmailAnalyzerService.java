package com.jesse.analyzer.service;

import com.jesse.core.pojo.PlainTextEmail;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.util.Map;

/** Linux 内核补丁邮件分析服务接口。*/
public interface KernelEmailAnalyzerService
{
    /** 从队列中消费一封邮件消息并处理。*/
    void handleKernelEmail(
        final Map<Long, PlainTextEmail> kernelEmailMap,
        final Channel channel,
        final Message message
    );
}