package com.jesse.linux_kernel_email_list_analyzer.components.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.KernelEmailPusher;
import com.jesse.linux_kernel_email_list_analyzer.components.SingleImapConnection;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.properties.LKMLRabbitMQProperties;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

/** Linux 内核补丁邮件推送器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailPusherImpl implements KernelEmailPusher
{
    /** LKML 常用时区。*/
    private static final
    ZoneId KERNEL_TIMEZONE = ZoneId.of("America/Los_Angeles");

    /** UTC 标准时区。*/
    private static final
    ZoneId UTC = ZoneId.of("UTC");

    /** 内核邮件发件时间标准格式。*/
    private static final
    DateTimeFormatter KERNEL_FORMAT
        = DateTimeFormatter.ofPattern("EEE, MMM d yyyy HH:mm:ss Z (zzzz)");

    /** 默认的邮件标记（阅后即焚）。*/
    private static final
    Flags DEFAULT_FLAGS = defaultFlags();

    /** RabbitMQ 队列交换机配置属性类。*/
    private final LKMLRabbitMQProperties properties;

    /** Spring Rabbit MQ 操作模板。*/
    private final RabbitTemplate rabbitTemplate;

    /** 单邮件服务 IMAP 连接实例管理接口。*/
    private final SingleImapConnection singleImapConnection;

    /** 邮件服务专用虚拟线程执行器。*/
    @Qualifier(value = "email-service-executor")
    private final ExecutorService emailServiceExecutor;

    /** 推送操作是否正在执行？*/
    private final
    AtomicBoolean pushing = new AtomicBoolean(false);

    /** 单次推送的邮件数量。*/
    @Value("${app.lkml-push-limit}")
    private int kernalEmailPushLimit;

    private static Flags defaultFlags()
    {
        final Flags flags = new Flags();

        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);

        return flags;
    }

    /** 内核补丁邮件解析。*/
    private PlainTextEmail
    parseToPlainText(Message message) throws Exception
    {
        final PlainTextEmail plainTextEmail = new PlainTextEmail();

        // Message 采用懒加载策略，获取正文是一次网络 I/O
        final Object content       = message.getContent();
        final String[] messageIds  = message.getHeader("Message-ID");
        final Address[] from       = message.getFrom();
        final Instant sentInstant  = message.getSentDate().toInstant();

        if (!(content instanceof String)) {
            return null;
        }

        if (message.isMimeType("text/plain")) {
            plainTextEmail.setTextContent(String.valueOf(content));
        }

        plainTextEmail.setMessageId(
            (Objects.nonNull(messageIds) && messageIds.length > 0)
                ? messageIds[0] : ""
        );

        plainTextEmail.setFrom(
            (Objects.nonNull(from) && from.length > 0)
                ? from[0].toString() : "Unknowns"
        );

        plainTextEmail.setSubject(message.getSubject());
        plainTextEmail.setUtcTime(sentInstant.atZone(UTC).format(ISO_DATE_TIME));
        plainTextEmail.setKernalTime(sentInstant.atZone(KERNEL_TIMEZONE).format(KERNEL_FORMAT));

        log.info("Parse kernel email (message-id = {}) complete.", plainTextEmail.getMessageId());

        return plainTextEmail;
    }

    /** 翻转从邮箱服务拉取的邮件数据，并保留最新的 limit 封。*/
    private Stream<Message>
    reverseUnreadMessages(final Message[] messages, int limit)
    {
        if (limit < -1)
        {
            throw new
            IllegalArgumentException("Argument limit must not less then -1");
        }

        if (Objects.isNull(messages) || messages.length == 0) {
            return Stream.of();
        }

        final Stream<Message> reverseStream
            = IntStream.range(0, messages.length)
                .mapToObj((index) -> messages[messages.length - 1 - index]);

        return
        (limit == -1) ? reverseStream : reverseStream.limit(limit);
    }

    /**
     * 从邮箱服务拉取所有的未读邮件，
     * 保留最新的前 limit 封返回（limit 填 -1 则表示全部）。
     * 被拉取的邮件的前 limit 封会被标记为已读。
     */
    private List<Message>
    fetchUnreadPlainTextEmails(Folder inbox, int limit)
    {
        try
        {
            final FlagTerm flagTerm
                = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

            final int unreadCount = inbox.getUnreadMessageCount();

            log.info("Total number of unread emails: {}", unreadCount);

            if (unreadCount == 0)
            {
                inbox.close(false);
                return List.of();
            }

            // (1) 由于 IMAP 协议的限制，
            // 只能一次性全量拉去未读邮件再做筛选，
            // 后续的标记操作也需要这个数组。
            final Message[] messages
                = this.reverseUnreadMessages(inbox.search(flagTerm), limit)
                      .toArray(Message[]::new);

            log.info("Pull and filter {} latest unread emails.", messages.length);

            // (2) 批量标记已读并删除
            if (messages.length > 0) {
                inbox.setFlags(messages, DEFAULT_FLAGS, true);
            }

            return List.of(messages);
        }
        catch (Exception exception)
        {
            log.error("Get unread email failed.", exception);
            return List.of();
        }
    }

    /** 每个整点自动执行一次推送。*/
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledPush() {
        this.push();
    }

    /**
     * 手动的将邮箱中的未读内核补丁邮件推送到消息队列，
     * 返回成功推送的邮件数量。
     */
    @Override
    public void push()
    {
        if (!this.pushing.compareAndSet(false, true))
        {
            log.warn("Previous push task is still running, skip this round.");
            return;
        }

        final Store store = this.singleImapConnection.getStore();
        Folder inbox      = null;

        try
        {
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // (1) 从邮箱服务拉取所有的未读邮件， 保留最新的前 limit 封返回。
            final List<Message> messages
                = this.fetchUnreadPlainTextEmails(inbox, kernalEmailPushLimit);

            if (CollectionUtils.isEmpty(messages)) {
                return;
            }

            // (2) 一边解析一边往 MQ 推送邮件，
            // Rabbit MQ 与服务建立的是 AMQP 长连接，目前的体量不需要批量操作。
            final List<CompletableFuture<PlainTextEmail>> pushFutures
                = messages.stream()
                    .filter(Objects::nonNull)
                    .map((message) ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return this.parseToPlainText(message);
                            }
                            catch (Exception exception)
                            {
                                log.error("Parse email failed.", exception);
                                return null;
                            }
                            }, this.emailServiceExecutor)
                            .thenApply((kernalEmail) -> {
                                if (Objects.nonNull(kernalEmail))
                                {
                                    try
                                    {
                                        this.rabbitTemplate.convertAndSend(
                                            this.properties.getExchangeName(),
                                            this.properties.getRoutingKey(),
                                            kernalEmail,
                                            new CorrelationData(kernalEmail.getMessageId())
                                        );

                                        log.info(
                                            "Pushed kernel email (message-id = {}) complete.",
                                            kernalEmail.getMessageId()
                                        );

                                        return kernalEmail;
                                    }
                                    catch (AmqpException exception)
                                    {
                                        log.error("Kernel email join queue failed", exception);
                                        return null;
                                    }
                                }

                                return null;
                            })
                ).toList();

            // (3) 等待完成并统计
            final long successCount
                = pushFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .count();

            log.info("Pushed kernel emails complete ({} / {}).", successCount, messages.size());
        }
        catch (MessagingException exception) {
            log.error("Get INBOX folder failed.", exception);
        }
        finally
        {
            try
            {
                if (Objects.nonNull(inbox) && inbox.isOpen())
                {
                    // (4) 关闭收件箱列表，
                    // expunges 值为 true 意味着全部删除标记为 DELETED 的邮件。
                    inbox.close(true);
                }
            }
            catch (Exception exception) {
                log.error("Close inbox failed.", exception);
            }
        }
    }
}
