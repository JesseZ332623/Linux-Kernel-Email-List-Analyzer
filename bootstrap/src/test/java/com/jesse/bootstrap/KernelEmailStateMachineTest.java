package com.jesse.bootstrap;

import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.core.enums.KernelEmailStatus;
import com.jesse.analyzer.components.state_machine.impl.KernelEmailStateMachineImpl;
import com.jesse.core.entity.LinuxKernelEmailEntiy;
import com.jesse.analyzer.service.LinuxKernerlEmailService;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.pojo.PlainTextEmail;
import com.jesse.core.utils.ZoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** {@link KernelEmailStateMachine} 状态流转测试类。*/
@Slf4j
@SpringBootTest
public class KernelEmailStateMachineTest
{
    /** 总测试邮件数。*/
    private static final int TEST_EMAILS = 5000;

    /** 所有成功的邮件状态流转事件。*/
    private static final
    EnumSet<KernelEmailEvents> EMAIL_EMAIL_EVENTS = successEvents();

    /** 全局 ID 消费机接口。*/
    @Autowired
    private GlobalIdConsumer globalIdConsumer;

    /** 内核邮件数据表服务服务类。*/
    @Autowired
    private LinuxKernerlEmailService linuxKernerlEmailService;

    /** 邮件服务专用虚拟线程执行器。*/
    @Autowired
    @Qualifier("email-service-executor")
    private ExecutorService emailServiceExecutor;

    /** 内核邮件状态机接口。*/
    @Autowired
    private KernelEmailStateMachine kernelEmailStateMachine;

    /**
     * 忽略 {@link KernelEmailStateMachineImpl#fireEvent(long, KernelEmailEvents)}
     * 调用的返回值。
     */
    private static void
    ignoreFireEventReturn(KernelEmailStatus status) { /* DO NOTHING */ }

    /** 任意链表的分片逻辑。*/
    public static <T> List<List<T>>
    partition(List<T> list, int size)
    {
        if (CollectionUtils.isEmpty(list)) {
            throw new IllegalArgumentException("List must not be null");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("split size must bigger than 0");
        }

        return
        IntStream.range(0, (list.size() + size - 1) / size)
            .mapToObj(i -> list.subList(
                i * size,
                Math.min((i + 1) * size, list.size())
            ))
            .collect(Collectors.toList());
    }

    /** 筛选所有成功的邮件状态流转事件并返回。*/
    private static EnumSet<KernelEmailEvents> successEvents()
    {
        return
        EnumSet.allOf(KernelEmailEvents.class)
            .stream()
            .filter((event) ->
                !event.name().contains("FAILURE"))
            .collect(Collectors.toCollection(() ->
                EnumSet.noneOf(KernelEmailEvents.class))
            );
    }

    /** 构造测试邮件实体列表。*/
    private List<LinuxKernelEmailEntiy> makeTestEmail()
    {
        return
        IntStream.range(0, TEST_EMAILS)
            .mapToObj((ignore) -> {
                final long nextId              = this.globalIdConsumer.nextId();
                final PlainTextEmail testEmail = new PlainTextEmail();

                testEmail.setMessageId("TEST");
                testEmail.setFrom("TEST");
                testEmail.setSubject("TEST");
                testEmail.setUtcTime(LocalDateTime.now(ZoneUtils.UTC).toString());
                testEmail.setKernelTime(LocalDateTime.now(ZoneUtils.KERNEL_TIMEZONE).toString());
                testEmail.setTextContent("TEST CONTENT");

                return
                LinuxKernelEmailEntiy.fromPlainTextEmail(nextId, testEmail);
            }).toList();
    }

    /** 每一批邮件状态流转的核心逻辑。*/
    private CompletableFuture<Void>
    doStateTransition(List<Long> partitionIds)
    {
        return
        CompletableFuture.runAsync(
            () -> {
                for (Long id : partitionIds)
                {
                    // (1) 对于每一封邮件完整的流转一次状态
                    EMAIL_EMAIL_EVENTS.stream()
                        .map((event) ->
                            this.kernelEmailStateMachine.fireEvent(id, event))
                        .forEach(KernelEmailStateMachineTest::ignoreFireEventReturn);
                }

                // (2) 删除这一批邮件数据
                this.linuxKernerlEmailService
                    .getBaseMapper().deleteByIds(partitionIds);
            }, this.emailServiceExecutor
        ).exceptionally((exception) -> {
            log.error("", exception); return null;
        });
    }

    /** 邮件状态流转测试。*/
    @Test
    public void stateTransitionTest()
    {
        // (1) 构造测试邮件实体列表
        final List<LinuxKernelEmailEntiy> testEmails
            = this.makeTestEmail();

        // (2) 获取所有测试邮件的 ID 并分片
        final List<List<Long>> testEmailIds
            = partition(
                testEmails.stream()
                    .map(LinuxKernelEmailEntiy::getId)
                    .toList(),
                500
            );

        // (3) 批量插入测试用邮件数据
        this.linuxKernerlEmailService.saveBatch(testEmails, 500);

        // (4) 构造所有状态流转任务并提交给线程池
        final List<CompletableFuture<Void>> stateTransitionTasks
            = testEmailIds.stream()
                .map(this::doStateTransition)
                .toList();

        // (5) 等待所有任务完成
        CompletableFuture.allOf(stateTransitionTasks.toArray(CompletableFuture[]::new))
            .join();
    }
}