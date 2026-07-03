package com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.SingleImapConnection;
import com.jesse.linux_kernel_email_list_analyzer.properties.EmailReceiverProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.iap.Protocol;
import org.eclipse.angus.mail.iap.ProtocolException;
import org.eclipse.angus.mail.iap.Response;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** IMAP 连接实例 keep-alive 定期保活组件。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ImapConnectionKeepAlive
{
    /** 单邮件服务 IMAP 连接实例管理接口。*/
    private final SingleImapConnection singleImapConnection;

    /** 邮箱服务属性配置类。*/
    private final EmailReceiverProperties properties;

    /** IMAP Connection 连接保活专用单线程执行器。*/
    @Qualifier(value = "imap-connection-keepalive-executor")
    private final
    ScheduledExecutorService imapConnectionKeepAliveExecutor;

    /** 连接保活任务是否正在执行（读多写少，不需要使用原子类型）。*/
    private volatile boolean running = true;

    /** 往邮箱服务发送 NOOP 命令并处理响应操作的实现。*/
    private static Object
    noop(Protocol protocol) throws ProtocolException
    {
        final Response[] responses
            = protocol.command("NOOP", null);

        /*
         * 通知 jakarta.mail 内部注册的的 ResponseHandler，比如：
         *
         * 监听新邮件到达（EXISTS、RECENT 等 untagged 响应）
         *
         * 处理 EXPUNGE（邮件被删除）
         *
         * IDLE 模式下的消息推送
         *
         * 其他内部状态更新
         */
        protocol.notifyResponseHandlers(responses);

        // 处理末尾的结果响应（成功、失败、结束等）
        protocol.handleResult(responses[responses.length - 1]);

        // 拼接响应消息字符串
        return
        Arrays.stream(responses)
              .map(Response::toString)
              .collect(Collectors.joining(" | "));
    }

    /** 邮箱服务连接保活操作的实现。*/
    private static Object
    keepAlive(Store store) throws MessagingException
    {
        final Folder folder = store.getFolder("INBOX");

        if (folder instanceof IMAPFolder imapFolder)
        {
            log.debug(
                "IMAP NOOP keep-alive execute success. (Response message: {})",
                imapFolder.doCommand(ImapConnectionKeepAlive::noop)
            );
        }
        else
        {
            // 如果没有使用 IMAPFolder，
            // 可以用一个轻量级操作代替进行连接保活。
            log.debug(
                "IMAP NOOP keep-alive execute success. (Folder message count: {})",
                folder.getMessageCount()
            );
        }

        return null;
    }

    /** 检查IMAP Connection 连接保活专用单线程执行器是否被意外关闭。*/
    private boolean isExecutorShutdown()
    {
        return
        Objects.isNull(this.imapConnectionKeepAliveExecutor) ||
        this.imapConnectionKeepAliveExecutor.isShutdown()    ||
        this.imapConnectionKeepAliveExecutor.isTerminated();
    }

    /** 执行一次连接保活操作。*/
    private void performKeepAlive()
    {
        if (!this.running) { return; }

        try
        {
            this.singleImapConnection
                .execute(ImapConnectionKeepAlive::keepAlive);
        }
        catch (Exception exception)
        {
            log.warn(
                "IMAP keep-alive execute failed, will retry in the next cycle.",
                exception
            );
        }
    }

    /**
     * 往 imapConnectionKeepAliveExecutor 中提交 performKeepAlive() 任务，
     * 令其周期性的执行连接保活操作。
     */
    @PostConstruct
    public void start()
    {
        final long interval
            = this.properties.getKeepAliveInterval().getSeconds();

        /*
         * 使用 scheduleWithFixedDelay() 而非 scheduleAtFixedRate()
         * 避免在获取连接耗时较长情况下的任务队列堆积导致的高频执行。
         */
        this.imapConnectionKeepAliveExecutor
            .scheduleWithFixedDelay(
                this::performKeepAlive,
                interval, interval, TimeUnit.SECONDS
            );

        log.info(
            "IMAP connection keep-alive component started, interval {} seconds.",
            interval
        );
    }

    /** 在服务实例关闭时停止连接保活操作。*/
    @PreDestroy
    public void stop()
    {
        // 翻转运行标志位，后续的连接保活操作全部跳过
        this.running = false;

        if (this.isExecutorShutdown())
        {
            log.debug("IMAP keep-alive executor already stopped.");
            return;
        }

        log.info("Shutting down IMAP Keep-Alive executor...");

        // 关闭连接保活操作专用单线程执行器
        this.imapConnectionKeepAliveExecutor.shutdown();

        try
        {
            final long timeout
                = this.properties.getKeepAliveShutdownWaitTimeout().toSeconds();

            // 最多等待指定时间，超时直接强制关闭。
            final boolean terminated
                = this.imapConnectionKeepAliveExecutor
                       .awaitTermination(timeout, TimeUnit.SECONDS);

            if (!terminated)
            {
                log.warn("IMAP Keep-Alive executor did not terminate gracefully, forcing shutdown.");
                this.imapConnectionKeepAliveExecutor.shutdownNow();
            }
        }
        catch (InterruptedException interrupted)
        {
            Thread.currentThread().interrupt();

            // 被中断了也应该立刻关闭防止泄漏。
            this.imapConnectionKeepAliveExecutor.shutdownNow();
        }

        log.info("IMAP connection Keep-Alive component stopped.");
    }
}