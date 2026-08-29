package com.jesse.core.components.imap_connection.impl;

import com.jesse.core.components.imap_connection.SingleImapConnection;
import com.jesse.core.components.imap_connection.StoreOperator;
import com.jesse.core.properties.EmailReceiverProperties;
import com.jesse.core.repository.ApplicationApiKeysRepository;
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
    private volatile Store store;

    /** Store 实例是否已经连接？ */
    private boolean isConnected() {
        return Objects.nonNull(this.store) && this.store.isConnected();
    }

    /**
     * {@link MessagingException} 是一个非常宽泛的异常，
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

    /**
     * 在锁和自动重连保护下执行任意 Store 操作。
     *
     * @param <T> operation 操作执行结果的类型
     *
     * @param operation        借用 {@link Store} 实例执行的操作
     * @param remainingRetries 剩余的重连重试次数，为 0 时不再重试直接抛出
     * @param discardStore     是否丢弃旧的 Store 实例标志位
     *
     * @return 返回 operation 操作执行的结果
     */
    private <T> T
    executeWithRetries(
        final StoreOperator<T> operation,
        final int              remainingRetries,
        final boolean          discardStore
    ) throws MessagingException
    {
        final long waitSeconds
            = this.properties.getStoreLockWaitTimeout().toSeconds();

        boolean isLocked = false;

        try
        {
            // (1) 等待锁
            isLocked = this.lock.tryLock(waitSeconds, TimeUnit.SECONDS);

            // 规定时间内拿不到锁直接抛异常
            if (!isLocked)
            {
                throw new MessagingException(
                    format("Failed to acquire store lock within %d seconds.", waitSeconds)
                );
            }

            // 线程拿到锁后先检查自己的中断状态，
            // 如果自己已经被外部中断了，则直接抛异常。
            if (Thread.currentThread().isInterrupted()) {
                throw new MessagingException("Interrupted before executing operation.");
            }

            // 上一轮判定连接已失效，在锁内丢弃坏连接，
            // 交由 ensureConnected 重建。
            if (discardStore) { this.store = null; }

            // (2) 在锁内确保连接可用
            this.ensureConnected();

            // (3) 执行业务逻辑
            return operation.execute(this.store);
        }
        catch (MessagingException exception)
        {
            /*
             * 2026.08.29 紧急修复
             *
             * 1. 中断判断 + 恢复中断标志位的操作在这里属于无效操作，
             *    isInterrupted() 不清除标志，条件成立时标志本就在，再设一次无效；
             *    标志真被清了则条件为假、不执行。两种情况都没用，这里直接去除。
             *
             * 2. 在锁超时后 + isRetryException() 返回 true 的情况下，
             *    operation.execute(this.store) 调用不受锁的保护，
             *    这又回到最初并发操作同一个有状态的 Store 的大坑里面去了。。。
             *    所以我的修复方案总结是这样的：“有中止条件的 this.execute(operation) 递归重试”，
             *    具体看提交变更。
             */

            // 在重试预算耗尽、线程已经被中断或异常不值得重试的情况下，
            // 一律向外传播异常。
            if (
                remainingRetries <= 0                    ||
                Thread.currentThread().isInterrupted()   ||
                !this.isRetryException(exception)
            )
            { throw exception; }

            log.warn(
                "Connection lost during operation, attempting reconnect.",
                exception
            );

            // 消耗一次 remainingRetries 执行重试，
            // 且下次重试要重建连接。
            return this.executeWithRetries(operation, remainingRetries - 1, true);
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
        // 默认只允许重试一次，一个 operation 任务不要占用锁太长时间。
        return this.executeWithRetries(operation, 1, false);
    }
}