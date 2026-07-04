package com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.SingleImapConnection;
import com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.StoreOperator;
import com.jesse.linux_kernel_email_list_analyzer.properties.EmailReceiverProperties;
import com.jesse.linux_kernel_email_list_analyzer.repository.ApplicationApiKeysRepository;
import jakarta.annotation.PreDestroy;
import jakarta.mail.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

/** 单邮件服务 IMAP 连接实例管理接口实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class SingleImapConnectionImpl implements SingleImapConnection
{
    /**
     * {@link Store} 是有状态的，
     * 在运行时有且只有一个线程能拿到这个锁去执行业务逻辑。
     */
    private final ReentrantLock lock = new ReentrantLock();

    /** 邮箱服务属性配置类。*/
    private final EmailReceiverProperties properties;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository applicationApiKeysRepository;

    /** 邮箱服务会话类。*/
    @Qualifier(value = "gmail-session")
    private final Session session;

    /** 邮件服务 IMAP 连接实例。*/
    private Store store;

    /** Store 实例是否已经连接？ */
    private boolean isConnected() {
        return Objects.nonNull(this.store) && this.store.isConnected();
    }

    /**
     * {@link MessagingException} 是一个非常宽泛地异常，
     * 我们需要进一步判断是否值得重试。
     */
    private boolean
    isRetryException(MessagingException exception)
    {
        return !isConnected()                          ||
            exception instanceof FolderClosedException ||
            exception instanceof StoreClosedException;
    }

    /** 在锁内确保连接可用。*/
    private void ensureConnected() throws MessagingException
    {
        if (!isConnected())
        {
            log.info("Connecting IMAP store...");

            this.connect();

            if (!isConnected()) {
                throw new MessagingException("IMAP connection failed");
            }
        }
    }

    /** 开始连接邮箱服务。（懒加载模式）*/
    private void connect() throws MessagingException
    {
        final String username = this.properties.getUsername();
        final Store  newStore = this.session.getStore();

        newStore.connect(
            username,
            this.applicationApiKeysRepository.findByAppName(username)
        );

        this.store = newStore;
    }

    /** 服务关闭的时候断开与邮箱服务的连接。*/
    @PreDestroy
    public void close()
    {
        try
        {
            if (this.isConnected())
            {
                this.store.close();
                log.info("Closing email service connection...");
            }
        }
        catch (MessagingException exception) {
            log.error("Closing email service connection failed...", exception);
        }
    }


    /** 在锁和自动重连保护下执行任意 Store 操作。*/
    @Override
    public <T> T
    execute(StoreOperator<T> operation) throws MessagingException
    {
        final long waitSeconds
            = this.properties.getStoreLockWaitTimeout().toSeconds();

        boolean isLocked = false;

        try
        {
            // (1) 等待锁
            isLocked = this.lock.tryLock(waitSeconds, TimeUnit.SECONDS);

            if (!isLocked)
            {
                throw new MessagingException(
                    format("Failed to acquire store lock within %d seconds.", waitSeconds)
                );
            }

            // (2) 在锁内确保连接可用
            this.ensureConnected();

            try {
                // (3) 执行业务逻辑
                return operation.execute(this.store);
            }
            catch (MessagingException exception)
            {
                // 如果执行业务逻辑时连接意外关闭，重连再次执行
                if (this.isRetryException(exception))
                {
                    log.warn(
                        "Connection lost during operation, attempting reconnect.",
                        exception
                    );

                    this.store = null;
                    this.ensureConnected();

                    return operation.execute(this.store);  // 重试一次
                }

                // 反之向外传递异常
                throw exception;
            }

        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();

            throw new
            MessagingException("Interrupted during waiting store lock.");
        }
        finally
        {
            if (isLocked) {
                this.lock.unlock();
            }
        }
    }
}