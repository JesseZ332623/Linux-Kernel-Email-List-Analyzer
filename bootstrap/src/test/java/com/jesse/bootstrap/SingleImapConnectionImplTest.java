package com.jesse.bootstrap;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.jesse.core.components.imap_connection.impl.SingleImapConnectionImpl;
import com.jesse.core.properties.EmailReceiverProperties;
import com.jesse.core.repository.ApplicationApiKeysRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.StoreClosedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link SingleImapConnectionImpl} 连接管理集成测试（GreenMail 进程内 IMAP）。*/
class SingleImapConnectionImplTest
{
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "testpwd";

    private GreenMail greenMail;

    @BeforeEach
    void startGreenMail()
    {
        final ServerSetup setup = new ServerSetup(0, null, ServerSetup.PROTOCOL_IMAP);
        this.greenMail = new GreenMail(setup);
        this.greenMail.setUser(USERNAME, PASSWORD);
        this.greenMail.start();
    }

    @AfterEach
    void stopGreenMail()
    {
        if (this.greenMail != null) {
            this.greenMail.stop();
        }
    }

    private EmailReceiverProperties properties(Duration lockWaitTimeout)
    {
        final EmailReceiverProperties properties = new EmailReceiverProperties();
        properties.setUsername(USERNAME);
        properties.setStoreLockWaitTimeout(lockWaitTimeout);
        return properties;
    }

    private SingleImapConnectionImpl newConnection(EmailReceiverProperties properties)
    {
        final ApplicationApiKeysRepository repository = mock(ApplicationApiKeysRepository.class);
        when(repository.findByAppName(USERNAME)).thenReturn(PASSWORD);

        final Properties mailProperties = new Properties();
        mailProperties.setProperty("mail.store.protocol", "imap");
        mailProperties.setProperty("mail.imap.host", "localhost");
        mailProperties.setProperty("mail.imap.port", String.valueOf(this.greenMail.getImap().getPort()));
        mailProperties.setProperty("mail.imap.connectiontimeout", "5000");
        mailProperties.setProperty("mail.imap.timeout", "5000");

        return new SingleImapConnectionImpl(properties, repository, Session.getInstance(mailProperties));
    }

    @Test
    @DisplayName("首次 execute 懒连接并执行 Store 操作")
    void executeConnectsLazilyAndRunsOperation() throws MessagingException
    {
        final SingleImapConnectionImpl connection
            = this.newConnection(this.properties(Duration.ofSeconds(5)));

        final int messageCount = connection.execute(
            (store) -> store.getFolder("INBOX").getMessageCount()
        );

        assertThat(messageCount).isZero();
    }

    @Test
    @DisplayName("StoreClosedException 触发丢弃旧连接并重连重试")
    void reconnectsWhenStoreClosedExceptionOccurs() throws MessagingException
    {
        final SingleImapConnectionImpl connection
            = this.newConnection(this.properties(Duration.ofSeconds(5)));

        final AtomicInteger calls = new AtomicInteger();
        final List<Store> seenStores = new ArrayList<>();

        final int result = connection.execute((store) -> {
            seenStores.add(store);
            if (calls.getAndIncrement() == 0) {
                throw new StoreClosedException(store);
            }
            return 42;
        });

        assertThat(result).isEqualTo(42);
        assertThat(seenStores).hasSize(2);
        assertThat(seenStores.get(0)).isNotSameAs(seenStores.get(1));
    }

    @Test
    @DisplayName("锁被占用超时后抛出 MessagingException")
    void throwsWhenStoreLockCannotBeAcquiredInTime() throws Exception
    {
        final SingleImapConnectionImpl connection
            = this.newConnection(this.properties(Duration.ofMillis(200)));

        // 先完成一次连接，让 isConnected() 为 true。
        connection.execute((store) -> 0);

        final CountDownLatch locked = new CountDownLatch(1);
        final Thread holder = new Thread(() -> {
            try
            {
                connection.execute((store) -> {
                    locked.countDown();
                    try {
                        Thread.sleep(2000L);
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    return 0;
                });
            }
            catch (MessagingException ignored) {
                // 仅用于占用锁，忽略执行结果
            }
        });
        holder.start();

        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

        try
        {
            assertThatThrownBy(() -> connection.execute((store) -> 1))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Failed to acquire store lock");
        }
        finally {
            holder.join(5000L);
        }
    }
}
