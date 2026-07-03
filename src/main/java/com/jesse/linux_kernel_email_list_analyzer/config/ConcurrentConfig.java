package com.jesse.linux_kernel_email_list_analyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/** 并发组件配置类。*/
@Configuration
public class ConcurrentConfig
{
    /** 邮件服务专用虚拟线程执行器。*/
    @Bean(
        name          = "email-service-executor",
        destroyMethod = "shutdown"
    )
    public ExecutorService emailServiceExecutor()
    {
        final ThreadFactory threadFactory
            = Thread.ofVirtual()
                    .name("email-service-", 0)
                    .factory();

        return
        Executors.newThreadPerTaskExecutor(threadFactory);
    }

    /** IMAP Connection 连接保活专用单线程执行器。*/
    @Bean(
        name          = "imap-connection-keepalive-executor",
        destroyMethod = "shutdown"
    )
    public ScheduledExecutorService imapConnectionKeepAliveExecutor()
    {
        final ThreadFactory threadFactory
            = Thread.ofVirtual()
                    .name("imap-keepalive-", 0)
                    .factory();

        return
        Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}