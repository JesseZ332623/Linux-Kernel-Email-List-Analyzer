package com.jesse.core.components.imap_connection.impl;

import com.jesse.core.components.imap_connection.SingleImapConnection;
import com.jesse.core.components.imap_connection.StoreOperator;
import com.jesse.core.properties.EmailReceiverProperties;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link ImapConnectionKeepAlive} 调度 / 容错 / 停止逻辑测试。*/
class ImapConnectionKeepAliveTest
{
    /** 每次 execute 都计数的假连接。*/
    private static final class FakeSingleImapConnection implements SingleImapConnection
    {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicBoolean throwOnNextCall = new AtomicBoolean();
        final AtomicBoolean throwAlways = new AtomicBoolean();
        volatile CountDownLatch latch;

        @Override
        public <T> T execute(StoreOperator<T> operation) throws MessagingException
        {
            this.calls.incrementAndGet();

            final CountDownLatch currentLatch = this.latch;
            if (currentLatch != null) {
                currentLatch.countDown();
            }

            if (this.throwAlways.get() || this.throwOnNextCall.compareAndSet(true, false)) {
                throw new MessagingException("boom");
            }
            return null;
        }
    }

    private final FakeSingleImapConnection connection = new FakeSingleImapConnection();
    private ScheduledExecutorService executor;

    private ImapConnectionKeepAlive newKeepAlive(Duration interval)
    {
        final EmailReceiverProperties properties = new EmailReceiverProperties();
        properties.setKeepAliveInterval(interval);
        properties.setKeepAliveShutdownWaitTimeout(Duration.ofSeconds(1));

        this.executor = Executors.newSingleThreadScheduledExecutor();

        return new ImapConnectionKeepAlive(this.connection, properties, this.executor);
    }

    @AfterEach
    void tearDown()
    {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("start() 以固定延迟周期性触发连接保活")
    void startSchedulesPeriodicKeepAlive() throws InterruptedException
    {
        this.connection.latch = new CountDownLatch(2);

        final ImapConnectionKeepAlive keepAlive = this.newKeepAlive(Duration.ofSeconds(1));
        keepAlive.start();

        assertThat(this.connection.latch.await(5, TimeUnit.SECONDS))
            .as("keep-alive should be invoked at least twice")
            .isTrue();
    }

    @Test
    @DisplayName("单次保活异常被吞掉，周期调度不会被中断")
    void keepAliveFailureIsSwallowedAndSchedulingSurvives() throws InterruptedException
    {
        this.connection.latch = new CountDownLatch(2);
        this.connection.throwOnNextCall.set(true);

        final ImapConnectionKeepAlive keepAlive = this.newKeepAlive(Duration.ofSeconds(1));
        keepAlive.start();

        // 第一次调用抛异常、第二次调用成功，两次都发生说明 try-catch 兜住了异常。
        assertThat(this.connection.latch.await(5, TimeUnit.SECONDS))
            .as("scheduling should survive a swallowed failure")
            .isTrue();

        assertThat(this.connection.calls.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("stop() 翻转运行标志、取消任务并关闭执行器")
    void stopCancelsFutureAndShutsDownExecutor() throws InterruptedException
    {
        this.connection.latch = new CountDownLatch(1);

        final ImapConnectionKeepAlive keepAlive = this.newKeepAlive(Duration.ofSeconds(1));
        keepAlive.start();

        assertThat(this.connection.latch.await(5, TimeUnit.SECONDS)).isTrue();

        keepAlive.stop();

        assertThat(this.executor.isShutdown())
            .as("keep-alive executor should be shut down")
            .isTrue();

        final int callsAfterStop = this.connection.calls.get();
        Thread.sleep(300L);

        assertThat(this.connection.calls.get())
            .as("no further keep-alive after stop()")
            .isEqualTo(callsAfterStop);
    }
}
