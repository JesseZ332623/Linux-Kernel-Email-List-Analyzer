package com.jesse.analyzer.components.state_machine.impl;

import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.analyzer.components.state_machine.KernelEmailStatus;
import com.jesse.analyzer.config.KernelEmailStateMachineConfig;
import com.jesse.analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.analyzer.repository.LinuxKernerlEmailRepository;
import com.jesse.core.exception.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static java.lang.String.format;

/** 内核邮件状态机实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailStateMachineImpl implements KernelEmailStateMachine
{
    /** fireEvent() 操作最多重试次数。*/
    private static final int FIRE_EVENT_MAX_RETRIES = 5;

    /** {@link KernelEmailStateMachineConfig} 配置好的状态机工厂。*/
    private final
    StateMachineFactory<KernelEmailStatus, KernelEmailEvents> stateMachineFactory;

    /** 内核邮件数据表仓储类。*/
    private final LinuxKernerlEmailRepository linuxKernerlEmailRepository;

    /** 获取当前重试次数。*/
    private int getCurrentRetryCount()
    {
        return
        Objects.requireNonNull(RetrySynchronizationManager.getContext(), "Illegal invoke")
               .getRetryCount() + 1;
    }

    /** 对指定邮件触发一个事件，驱动状态流转。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
        retryFor    = OptimisticLockException.class,
        maxAttempts = FIRE_EVENT_MAX_RETRIES,
        backoff     = @Backoff(delay = 5L, multiplier = 2, maxDelay = 50L)
    )
    public KernelEmailStatus
    fireEvent(long emailId, KernelEmailEvents event)
    {
        final int retryCount
            = this.getCurrentRetryCount();

        log.info(
            "Executing fireEvent() for email: {}, retry attempt: {} / {}",
            emailId, retryCount, FIRE_EVENT_MAX_RETRIES
        );

        // (1) 查询当前这封邮件的状态
        final LinuxKernerlEmailEntiy kernerlEmail
            = this.linuxKernerlEmailRepository.selectById(emailId);

        // (2) 构造新的状态机
        final StateMachine<KernelEmailStatus, KernelEmailEvents>
            stateMachine = this.stateMachineFactory.getStateMachine();

        // (3) 执行所有退出动作，清理状态机内部环境
        stateMachine.stop();

        // (4) 恢复当前的状态
        stateMachine.getStateMachineAccessor()
            .doWithAllRegions((access) ->
                access.resetStateMachine(
                    new DefaultStateMachineContext<>(
                        kernerlEmail.getStatus(), null, null, null
                    )
                )
            );

        // (5) 重新启动状态机
        stateMachine.start();

        final KernelEmailStatus currentStatus = kernerlEmail.getStatus();

        log.info(
            "Drive status isolation for kernel email: {} " +
            "(Current status: {}, send event: {}).",
            emailId, currentStatus.name(), event
        );

        // (6) 触发事件流转状态，如果违背流转规则就抛异常
        if (!stateMachine.sendEvent(event))
        {
            throw new
            OptimisticLockException(
                format(
                    "Status flow rejection. Current status: %s not accept event: %s) " +
                    "(Kernel email id: %d, retry attempt: %d / %d)",
                    kernerlEmail.getStatus(), event.name(),
                    emailId, retryCount, FIRE_EVENT_MAX_RETRIES
                )
            );
        }

        final KernelEmailStatus newStatus = stateMachine.getState().getId();

        kernerlEmail.setStatus(newStatus);

        // (7) 将新状态写回数据库
        final int updated
            = this.linuxKernerlEmailRepository.updateById(kernerlEmail);

        // 如果写入时抢不到乐观锁，就抛异常然后重试
        if (updated == 0)
        {
            throw new
            OptimisticLockException(
                format(
                    "Kernel email status has been updated by other thread. " +
                    "(email id: %d, retry attempt: %d / %d)",
                    emailId, retryCount, FIRE_EVENT_MAX_RETRIES
                )
            );
        }

        log.info(
            "Drive status isolation for kernel email: {} ({} -> {})",
            emailId, currentStatus.name(), newStatus.name()
        );

        return newStatus;
    }
}