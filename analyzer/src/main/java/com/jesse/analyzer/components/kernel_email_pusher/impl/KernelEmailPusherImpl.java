package com.jesse.analyzer.components.kernel_email_pusher.impl;

import com.jesse.analyzer.components.kernel_email_pusher.KernelEmailPusher;
import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.core.entity.LinuxKernelEmailEntity;
import com.jesse.analyzer.repository.LinuxKernelEmailRepository;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.components.imap_connection.SingleImapConnection;
import com.jesse.core.pojo.PlainTextEmail;
import com.jesse.core.properties.LKMLRabbitMQProperties;
import com.jesse.core.utils.ZoneUtils;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
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
    /** 内核邮件发件时间标准格式。*/
    private static final
    DateTimeFormatter KERNEL_FORMAT
        = DateTimeFormatter.ofPattern("EEE, MMM d yyyy HH:mm:ss Z (zzzz)");

    /**
     * push() 操作每次循环处理一批邮件的数量，固定值无需写到配置中去
     * 确保上游的 gmail 不会因为下游无节制并发而拒绝。
     */
    private final int PROCESS_BATCH_SIZE = 50;

    /** 表示空 {@link Message} 数组的单例。*/
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

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 内核邮件数据表仓储类。*/
    private final
    LinuxKernelEmailRepository linuxKernelEmailRepository;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** 推送操作是否正在执行？*/
    private final
    AtomicBoolean pushing = new AtomicBoolean(false);

    /** 单次推送的邮件数量。*/
    @Value("${app.lkml-push-limit}")
    private int kernelEmailPushLimit;

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
        final FetchProfile fetchProfile = new FetchProfile();

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
         * UID 预取配置会在 Folder::fetch() 方法
         * 执行时获取邮件的唯一 ID。
         */
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);

        /*
         * MESSAGE 预取配置会在 Folder::fetch() 方法
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
            plainTextEmail.setUtcTime(sentInstant.atZone(ZoneUtils.UTC).format(ISO_DATE_TIME));
            plainTextEmail.setKernelTime(sentInstant.atZone(ZoneUtils.KERNEL_TIMEZONE).format(KERNEL_FORMAT));

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
            // 只能一次性全量拉取未读邮件再做筛选，
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

        /*
         * (2) 将 messages 按片拷贝到每一个 List<Message> 中去，
         * 最后收集成分片列表 List<List<Message>>。
         */
        return
        IntStream.range(0, batches)
            .mapToObj((index) -> {
                final int from = index * PROCESS_BATCH_SIZE;
                final int to   = Math.min(from + PROCESS_BATCH_SIZE, messages.length);

                return Arrays.asList(messages).subList(from, to);
            }).toList();
    }

    /** 将拉取到的内核邮件数据插入数据库。*/
    private Map<Long, PlainTextEmail>
    insertToDatabase(PlainTextEmail kernelEmail)
    {
        final long nextId = this.globalIdConsumer.nextId();

        this.linuxKernelEmailRepository
            .insert(LinuxKernelEmailEntity.fromPlainTextEmail(nextId, kernelEmail));

        return Map.of(nextId, kernelEmail);
    }

    /**
     * <h3>2026.08.19 修订</h3>
     *
     * 同步的等待单条消息的生产端确认（Publisher Confirm）。
     *
     * <p>
     * {@code convertAndSend()} 返回只代表消息帧被写入了本地内核 socket 缓冲区，
     * 并不代表 broker 真的收到并接受了这条消息。因此必须等到 broker 回送
     * {@code basic.ack} / {@code basic.nack} 之后，才能决定状态如何流转。
     * </p>
     *
     * <p>
     * 这里刻意采用<b>同步等待</b>而非异步回调：状态流转必须留在
     * {@code convertAndSend()} 所在的这条执行链上，才能保证 {@code PUSH_SUCCESS}
     * 在绝大多数情况下先于消费端的 {@code PULL_SUCCESS} 完成。
     * 本方法运行在虚拟线程上，阻塞会自动卸载载体线程，代价可控。
     * </p>
     *
     * @param emailId         内核补丁邮件雪花 ID，作为状态流转的依据
     * @param correlationData 投递关联凭据，从中取出确认结果
     *
     * @return broker 确认接受了这条消息则返回 true，否则返回 false
     *
     * @throws InterruptedException 等待确认的过程中当前线程被中断
     */
    private boolean awaitPublisherConfirm(
        final Long            emailId,
        final CorrelationData correlationData
    ) throws InterruptedException
    {
        final long timeoutMillis
            = this.properties.getPublisherConfirmTimeout().toMillis();

        try
        {
            // (1) 阻塞等待 broker 的 ack / nack
            final CorrelationData.Confirm confirm
                = correlationData.getFuture()
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);

            // (2) 正常情况下 confirm 不会为 null，这里做防御性判断，
            //     避免 NPE 逃出本方法冲垮整批推送。
            if (Objects.isNull(confirm) || !confirm.isAck())
            {
                log.error(
                    "Broker rejected kernel email (snowflake-id = {}, message-id = {}). Caused by: {}",
                    emailId, correlationData.getId(),
                    Objects.nonNull(confirm) ? confirm.getReason() : "confirm is null"
                );

                return false;
            }

            log.info(
                "Confirmed kernel email (snowflake-id = {}, message-id = {}) by broker.",
                emailId, correlationData.getId()
            );

            return true;
        }
        catch (TimeoutException exception)
        {
            /*
             * 注意：超时的真实语义是「结果未知」而非「确认失败」，
             * 消息可能已经进入队列、甚至已被消费。
             * 此处暂按失败处理以保证不会误报成功，
             * 后续应引入 PUSH_UNKNOWN 状态 + 对账补偿来精确表达。
             */
            log.error(
                "Publisher confirm timeout after {} ms, " +
                "result UNKNOWN for kernel email (snowflake-id = {}, message-id = {}).",
                timeoutMillis, emailId, correlationData.getId()
            );

            return false;
        }
        catch (ExecutionException exception)
        {
            log.error(
                "Publisher confirm failed for kernel email (snowflake-id = {}, message-id = {}).",
                emailId, correlationData.getId(), exception.getCause()
            );

            return false;
        }
    }

    /** 将一份内核补丁邮件推送至 RabbitMQ。*/
    private Map<Long, PlainTextEmail>
    pushToRabbitMQ(Map<Long, PlainTextEmail> kernelEmailMap)
    {
        if (!CollectionUtils.isEmpty(kernelEmailMap))
        {
            for (var kernelEmail : kernelEmailMap.entrySet())
            {
                final Long emailId     = kernelEmail.getKey();
                final String messageId = kernelEmail.getValue().getMessageId();

                try
                {
                    // (1) 使用邮件的 RFC Message-ID 构造关联凭据 ID
                    final CorrelationData correlationData
                        = new CorrelationData(messageId);

                    // (2) 声明 “往消息元数据中写入邮件的 Message-ID 和 雪花 ID” 的回调函数
                    final MessagePostProcessor messagePostProcessor
                        = (message) -> {
                            final MessageProperties messageProperties
                                = message.getMessageProperties();

                            messageProperties.setMessageId(messageId);
                            messageProperties.setHeader("email-snowflake-id", emailId);
                            messageProperties.setHeader("content-length", message.getBody().length);

                            return message;
                    };

                    // (3) 把邮件投递到 RabbitMQ（写入客户端 socket 缓冲区）
                    this.rabbitTemplate.convertAndSend(
                        this.properties.getExchangeName(),
                        this.properties.getRoutingKey(),
                        kernelEmail,
                        messagePostProcessor,
                        correlationData
                    );

                    // (4) 同步等待 broker 确认
                    final boolean acked
                        = this.awaitPublisherConfirm(emailId, correlationData);

                    // (5) 拿到结果后再流转状态
                    this.kernelEmailStateMachine.fireEvent(
                            emailId,
                            (acked)
                                ? KernelEmailEvents.PUSH_SUCCESS
                                : KernelEmailEvents.PUSH_FAILURE
                        );

                    // 若这封邮件投出去如果没有被 broker 确认，
                    // 则返回 null 供下游统计
                    if (!acked) { return null; }
                }
                catch (InterruptedException interrupted)
                {
                    // 中断意味着应用正在关闭
                    // 必须恢复中断标志位并立刻停止处理本批剩余的邮件。
                    Thread.currentThread().interrupt();

                    log.warn(
                        "Interrupted while waiting publisher confirm " +
                        "for kernel email (snowflake-id = {}), abort this batch.",
                        emailId
                    );

                    this.kernelEmailStateMachine
                        .fireEvent(emailId, KernelEmailEvents.PUSH_FAILURE);

                    return null;
                }
                catch (AmqpException exception)
                {
                    // 如果这封邮件投递失败
                    //（仅包括 消息序列化失败、连接不可用、Channel 创建失败等同步错误）
                    this.kernelEmailStateMachine
                        .fireEvent(emailId, KernelEmailEvents.PUSH_FAILURE);

                    log.error("Kernel email join queue failed.", exception);

                    return null;
                }
            }

            return kernelEmailMap;
        }

        return null;
    }

    /** 推送操作的核心逻辑。*/
    private Object
    doPush(Store store) throws MessagingException
    {
        Folder inbox = null;

        try
        {
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // (1) 从邮箱服务拉取所有的未读邮件，保留最新的前 limit 封返回。
            final Message[] messages
                = this.fetchUnreadPlainTextEmails(inbox, kernelEmailPushLimit);

            // (2) 将邮件数据分片
            final List<List<Message>> splitMessages
                = this.splitMessages(messages);

            for (final List<Message> batch : splitMessages)
            {
                // (3) 标记这一片的邮件为 “阅后即焚”。
                inbox.setFlags(batch.toArray(Message[]::new), DEFAULT_FLAGS, true);

                // (4) 一边解析一边往 MQ 推送邮件，
                // Rabbit MQ 与服务建立的是 AMQP 长连接，目前的体量不需要批量操作。
                final var pushFutures
                    = batch.stream()
                           .filter(Objects::nonNull)
                           .map((message) ->
                               CompletableFuture
                                   .supplyAsync(() -> this.parseToPlainText(message), this.emailServiceExecutor)
                                   .thenApply(this::insertToDatabase)
                                   .thenApply(this::pushToRabbitMQ)
                           ).toList();

                // (5) 等待完成并统计这一个批次的成功发送的数量
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
            this.singleImapConnection.execute(this::doPush);
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