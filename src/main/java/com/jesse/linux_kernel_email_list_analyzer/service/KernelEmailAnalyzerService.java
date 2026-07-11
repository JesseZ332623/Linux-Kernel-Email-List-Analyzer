package com.jesse.linux_kernel_email_list_analyzer.service;

import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

/** Linux 内核补丁邮件分析服务接口。*/
public interface KernelEmailAnalyzerService
{
    /** 从队列中消费一封邮件消息并处理。*/
    void handleKernelEmail(
        final PlainTextEmail kernalEmail,
        final Channel channel,
        final Message message
    );
}