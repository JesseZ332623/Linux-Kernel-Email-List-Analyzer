package com.jesse.linux_kernel_email_list_analyzer.config;

import com.jesse.linux_kernel_email_list_analyzer.properties.EmailReceiverProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.*;

/** 并发组件配置类。*/
@Configuration
@RequiredArgsConstructor
public class ConcurrentConfig
{
    /** 邮箱服务属性配置类。*/
    private final EmailReceiverProperties emailReceiverProperties;

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
    @Bean(name = "imap-connection-keepalive-executor")
    public ScheduledExecutorService imapConnectionKeepAliveExecutor()
    {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        final int awaitTimeout
            = (int) this.emailReceiverProperties.getKeepAliveShutdownWaitTimeout()
                        .toSeconds();

        scheduler.setPoolSize(1);                              // 单线程
        scheduler.setThreadNamePrefix("imap-keepalive-");
        scheduler.setDaemon(true);                            // 守护线程
        scheduler.setAwaitTerminationSeconds(awaitTimeout);   // 关闭时超时时间
        scheduler.setWaitForTasksToCompleteOnShutdown(true);  // 优雅关闭，等待任务完成
        scheduler.setRemoveOnCancelPolicy(true);              // 队列中的线程被中断则立刻离队

        // 初始化
        scheduler.initialize();

        return scheduler.getScheduledExecutor();
    }
}