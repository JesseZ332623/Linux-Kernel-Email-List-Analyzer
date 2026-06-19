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

    /** 内核补丁邮件解析。*/
    private PlainTextEmail
    parseToPlainText(Message message) throws Exception
    {
        final PlainTextEmail plainTextEmail = new PlainTextEmail();

        final Object content       = message.getContent();
        final String[] messageIds  = message.getHeader("Message-ID");
        final Address[] from       = message.getFrom();
        final Instant sentInstant  = message.getSentDate().toInstant();

        if (content instanceof String)
        {
            if (message.isMimeType("text/plain")) {
                plainTextEmail.setTextContent(String.valueOf(content));
            }
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
    private List<PlainTextEmail> fetchUnreadPlainTextEmails(int limit)
    {
        try
        {
            final Store  store = this.singleImapConnection.getStore();
            final Folder inbox = store.getFolder("INBOX");
            final FlagTerm flagTerm
                = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

            inbox.open(Folder.READ_WRITE);

            final int unreadCount = inbox.getUnreadMessageCount();

            log.info("Total number of unread emails: {}", unreadCount);

            if (unreadCount == 0)
            {
                inbox.close(false);
                return List.of();
            }

            final Message[] messages = inbox.search(flagTerm);

            log.info("Searched {} unread emails.", messages.length);

            final List<PlainTextEmail> plainTextEmails
                = this.reverseUnreadMessages(messages, limit)
                      .map((message) ->
                          CompletableFuture.supplyAsync(
                              () -> {
                                  try
                                  {
                                      final PlainTextEmail plainTextEmail
                                          = this.parseToPlainText(message);

                                      final Flags flags = new Flags();
                                      flags.add(Flags.Flag.SEEN);
                                      flags.add(Flags.Flag.DELETED);

                                      // 标记这封邮件为 “阅后即焚”
                                      message.setFlags(flags, true);

                                      log.info(
                                          "Set flag [SEEN & DELETED] for email: {}",
                                          plainTextEmail.getMessageId()
                                      );

                                      return plainTextEmail;
                                  }
                                  catch (Exception exception)
                                  {
                                      log.error("Parse email failed.", exception);
                                      return null;
                                  }
                              },
                              this.emailServiceExecutor
                          )
                      )
                     .toList()
                     .stream()
                     .map(CompletableFuture::join)
                     .filter(Objects::nonNull)
                     .toList();

            inbox.close(true);

            return plainTextEmails;
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

        try
        {
            final List<PlainTextEmail> kernalEmails
                = this.fetchUnreadPlainTextEmails(kernalEmailPushLimit);

            int successCount = 0;

            for (PlainTextEmail kernalEmail : kernalEmails)
            {
                try
                {
                    this.rabbitTemplate.convertAndSend(
                        this.properties.getExchangeName(),
                        this.properties.getRoutingKey(),
                        kernalEmail,
                        new CorrelationData(kernalEmail.getMessageId())
                    );

                    ++successCount;
                }
                catch (AmqpException exception) {
                    log.error("Kernel email join queue failed", exception);
                }
            }

            log.info(
                "Kernel email push complete: {} / {}",
                successCount, kernalEmails.size()
            );
        }
        finally {
            this.pushing.set(false);
        }
    }
}
