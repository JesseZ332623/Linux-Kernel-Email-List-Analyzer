package com.jesse.linux_kernel_email_list_analyzer.components.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.KernelEmailPusher;
import com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.SingleImapConnection;
import com.jesse.linux_kernel_email_list_analyzer.components.imap_connection.StoreOperator;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.properties.LKMLRabbitMQProperties;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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

    /**
     * push() 操作每次循环处理一批邮件的数量，固定值无需写到配置中去
     * 确保上游的 gmail 不会因为下游无节制并发而拒绝。
     */
    private final int PROCESS_BATCH_SIZE = 50;

    /** 表示空 {@link jakarta.mail.Message} 数组的单例。*/
    private static final
    Message[] EMPTY_MESSAGE_ARRAY = new Message[]{};

    /** 默认的邮件标记（阅后即焚）。*/
    private static final
    Flags DEFAULT_FLAGS = defaultFlags();

    /** 默认的邮件数据预取配置。*/
    private static final
    FetchProfile DEFAULT_FETCH_PROFILE = defaultFetchProfile();

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

    /** 构造默认的邮件标记（阅后即焚）。*/
    private static Flags defaultFlags()
    {
        final Flags flags = new Flags();

        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);

        return flags;
    }

    /** 构造默认的邮件数据预取配置。*/
    private static FetchProfile defaultFetchProfile()
    {
        FetchProfile fetchProfile = new FetchProfile();

        /*
         * ENVELOPE 预取配置会在 Folder::fetch() 方法
         * 执行时获取邮件的头信息，包括：
         *
         *   From	        发件人
         *   To / Cc / Bcc	收件人、抄送、密送
         *   Subject	    主题
         *   Date	        发送日期
         *   Message-ID	    消息唯一 ID
         *   Reply-To	    回复地址
         *   In-Reply-To	回复哪封邮件
         *
         *  这样 parseToPlainText() 方法中 message 的很多细碎的 get 操作
         *  都可以不走网络了。
         */
        fetchProfile.add(FetchProfile.Item.ENVELOPE);

        /*
         * ENVELOPE 预取配置会在 Folder::fetch() 方法
         * 执行时获取邮件的唯一 ID。
         */
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);

        /*
         * ENVELOPE 预取配置会在 Folder::fetch() 方法
         * 执行时获取邮件的正文内容。
         */
        fetchProfile.add(IMAPFolder.FetchProfileItem.MESSAGE);

        return fetchProfile;
    }

    /**
     * 虽然 LKML 100% 是纯文本邮件，
     * 但邮箱服务可能会包装成 {@link MimeMultipart}，所以本方法做的就是提取内部的文本，
     * 不要出现：
     *
     * <pre>
     *      Skip non-plain-text email. Content Type: MimeMultipart,
     *      Subject Re: [PATCH bpf-next v4 3/3] selftests/bpf: Add bpf_fib_lookup() VLAN flag tests
     * </pre>
     *
     * 这种意外地丢件情况。
     */
    private String
    getPlainTextFromMimeMultipart(MimeMultipart multipart)
        throws IOException, MessagingException
    {
        for (int index = 0; index < multipart.getCount(); ++index)
        {
            final BodyPart bodyPart
                = multipart.getBodyPart(index);

            if (bodyPart.isMimeType("text/plain")) {
                return (String) bodyPart.getContent();
            }
        }

        return null;
    }

    /** 内核补丁邮件解析。*/
    private PlainTextEmail
    parseToPlainText(Message message)
    {
        try
        {
            final PlainTextEmail plainTextEmail = new PlainTextEmail();

            // Message 采用懒加载策略，获取正文是一次网络 I/O
            final Object    content     = message.getContent();
            final String[]  messageIds  = message.getHeader("Message-ID");
            final Address[] from        = message.getFrom();
            final Instant   sentInstant = message.getSentDate().toInstant();

            // 如果邮件内容本身就是文本
            if (content instanceof String) {
                plainTextEmail.setTextContent(String.valueOf(content));
            }
            else if (content instanceof MimeMultipart)
            {
                // 如果邮件被邮箱服务包装成了 MimeMultipart，
                // 去提取内部的文本
                plainTextEmail.setTextContent(
                    this.getPlainTextFromMimeMultipart((MimeMultipart) content)
                );
            }
            else
            {
                // 如果这封邮件并不是纯文本邮件，
                // 可能是邮箱服务这边可能意外地拉取了别的邮件，
                // 直接返回 null 丢弃即可
                log.warn(
                    "Skip non-plain-text email. Content Type: {}, Subject {}",
                    content.getClass().getSimpleName(),
                    message.getSubject()
                );

                return null;
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
        catch (Exception exception)
        {
            log.error("Parse email failed.", exception);
            return null;
        }
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
    private Message[]
    fetchUnreadPlainTextEmails(Folder inbox, int limit)
    {
        try
        {
            final FlagTerm flagTerm
                = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

            final int unreadCount = inbox.getUnreadMessageCount();

            log.info("Total number of unread emails: {}", unreadCount);

            if (unreadCount == 0) {
                return EMPTY_MESSAGE_ARRAY;
            }

            // (1) 由于 IMAP 协议的限制，
            // 只能一次性全量拉去未读邮件再做筛选，
            // 后续的标记操作也需要这个数组。
            final Message[] messages
                = this.reverseUnreadMessages(inbox.search(flagTerm), limit)
                      .toArray(Message[]::new);

            // (2) 预取这批邮件的部分数据（包括正文）
            inbox.fetch(messages, DEFAULT_FETCH_PROFILE);

            log.info("Pull and filter {} latest unread emails.", messages.length);

            return messages;
        }
        catch (Exception exception)
        {
            log.error("Get unread email failed.", exception);
            return EMPTY_MESSAGE_ARRAY;
        }
    }

    /**
     * 将上游拉下来的邮件数据分片，
     * 下游一片片的处理，这样可以限制并发量和 OOM。
     */
    private List<List<Message>>
    splitMessages(final Message[] messages)
    {
        if (Arrays.equals(messages, EMPTY_MESSAGE_ARRAY)) {
            return List.of();
        }

        // (1) 向上取整的计算批次数
        final int batches
            = (messages.length + PROCESS_BATCH_SIZE - 1) / PROCESS_BATCH_SIZE;

        final List<Message> messageList
            = Arrays.asList(messages);

        /*
         * (2) 将 messages 按片拷贝到每一个 List<Message> 中去，
         * 最后收集成分片列表 List<List<Message>>。
         */
        return
        IntStream.range(0, batches)
            .mapToObj((index) -> {
                final int from = index * PROCESS_BATCH_SIZE;
                final int to   = Math.min(from + PROCESS_BATCH_SIZE, messages.length);

                return messageList.subList(from, to);
            }).toList();
    }

    /** 将一份内核补丁邮件推送至 RabbitMQ。*/
    private PlainTextEmail pushToRabbitMQ(PlainTextEmail kernelEmail)
    {
        if (Objects.nonNull(kernelEmail))
        {
            try
            {
                this.rabbitTemplate.convertAndSend(
                    this.properties.getExchangeName(),
                    this.properties.getRoutingKey(),
                    kernelEmail,
                    new CorrelationData(kernelEmail.getMessageId())
                );

                log.info(
                    "Pushed kernel email (message-id = {}) complete.",
                    kernelEmail.getMessageId()
                );

                return kernelEmail;
            }
            catch (AmqpException exception)
            {
                log.error("Kernel email join queue failed", exception);
                return null;
            }
        }

        return null;
    }

    /** 推送操作的核心逻辑。*/
    private StoreOperator<Void> doPush()
    {
        return (store) -> {
            Folder inbox = null;

            try
            {
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);

                // (1) 从邮箱服务拉取所有的未读邮件，保留最新的前 limit 封返回。
                final Message[] messages
                    = this.fetchUnreadPlainTextEmails(inbox, kernalEmailPushLimit);

                // (2) 将邮件数据分片
                final List<List<Message>> splitMessages
                    = this.splitMessages(messages);

                for (final List<Message> batch : splitMessages)
                {
                    // (3) 标记这一片的邮件为 “阅后即焚”。
                    inbox.setFlags(batch.toArray(Message[]::new), DEFAULT_FLAGS, true);

                    // (4) 一边解析一边往 MQ 推送邮件，
                    // Rabbit MQ 与服务建立的是 AMQP 长连接，目前的体量不需要批量操作。
                    final List<CompletableFuture<PlainTextEmail>> pushFutures
                        = batch.stream()
                            .filter(Objects::nonNull)
                            .map((message) ->
                                CompletableFuture
                                    .supplyAsync(() -> this.parseToPlainText(message), this.emailServiceExecutor)
                                    .thenApply(this::pushToRabbitMQ)
                            ).toList();

                    // (5) 等待完成并统计这一个批次的成功数量
                    final long successCount
                        = pushFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .count();

                    log.info("Pushed kernel emails complete ({} / {}).", successCount, batch.size());
                }
            }
            finally
            {
                try
                {
                    if (Objects.nonNull(inbox) && inbox.isOpen())
                    {
                        // (6) 关闭收件箱列表，
                        // expunges 值为 true 意味着全部删除标记为 DELETED 的邮件。
                        inbox.close(true);
                    }
                }
                catch (Exception exception) {
                    log.error("Close inbox failed.", exception);
                }
            }

            return null;
        };
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

        try {
            this.singleImapConnection.execute(this.doPush());
        }
        catch (MessagingException exception) {
            log.error("Push lkml email to message queue failed.", exception);
        }
        finally {
            // 翻转并发标志位
            this.pushing.set(false);
        }
    }
}