package com.jesse.linux_kernel_email_list_analyzer.components.state_machine.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.linux_kernel_email_list_analyzer.config.KernelEmailStateMachineConfig;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStatus;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.linux_kernel_email_list_analyzer.repository.LinuxKernerlEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;

/** 内核邮件状态机实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailStateMachineImpl implements KernelEmailStateMachine
{
    /** {@link KernelEmailStateMachineConfig} 配置好的状态机工厂。*/
    private final
    StateMachineFactory<KernelEmailStatus, KernelEmailEvents> stateMachineFactory;

    /** 内核邮件数据表仓储类。*/
    private final LinuxKernerlEmailRepository linuxKernerlEmailRepository;

    /** 对指定邮件触发一个事件，驱动状态流转。*/
    @Override
    @Transactional(
        rollbackFor = Exception.class,
        propagation = Propagation.REQUIRES_NEW
    )
    public KernelEmailStatus
    fireEvent(long emailId, KernelEmailEvents event)
    {
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
        final String subject                  = kernerlEmail.getSubject();

        log.info(
            "Drive stastus isolation for kernel email: {} " +
            "(Current status: {}, send event: {}).",
            subject, currentStatus.name(), event
        );

        // (6) 触发事件流转状态，如果违背流转规则就抛异常
        if (!stateMachine.sendEvent(event))
        {
            throw new
            IllegalStateException(
                format(
                    "Status flow rejection. Currrent status: %s not accept event: %s",
                    kernerlEmail.getStatus(), event.name()
                )
            );
        }

        final KernelEmailStatus newStatus = stateMachine.getState().getId();

        kernerlEmail.setStatus(newStatus);

        // (7) 将新状态写回数据库
        this.linuxKernerlEmailRepository.updateById(kernerlEmail);

        log.info(
            "Drive stastus isolation for kernel email: {} ({} -> {})",
            subject, currentStatus.name(), newStatus.name()
        );

        return newStatus;
    }
}