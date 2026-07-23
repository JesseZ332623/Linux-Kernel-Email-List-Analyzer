package com.jesse.linux_kernel_email_list_analyzer.config;

import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/** 内核补丁邮件状态机工厂配置类。*/
@Configuration
@EnableStateMachineFactory
public class KernelEmailStateMachineConfig
    extends StateMachineConfigurerAdapter<KernelEmailStatus, KernelEmailEvents>
{
    /** 状态机初始状态和所有状态的声明配置。*/
    @Override
    public void
    configure(StateMachineStateConfigurer<KernelEmailStatus, KernelEmailEvents> states)
        throws Exception
    {
        states.withStates()
            .initial(KernelEmailStatus.FETCHED)
            .states(EnumSet.allOf(KernelEmailStatus.class));
    }

    /** 状态机状态流转声明配置。*/
    @Override
    public void
    configure(StateMachineTransitionConfigurer<KernelEmailStatus, KernelEmailEvents> transitions)
        throws Exception
    {
        transitions.withExternal()
                .source(KernelEmailStatus.FETCHED)
                .target(KernelEmailStatus.PUSHED)
                .event(KernelEmailEvents.PUSH_SUCCESS)
            .and().withExternal()
                .source(KernelEmailStatus.FETCHED)
                .target(KernelEmailStatus.PUSH_FAILED)
                .event(KernelEmailEvents.PUSH_FAILURE)
            .and().withExternal()
                .source(KernelEmailStatus.PUSHED)
                .target(KernelEmailStatus.ANALYSIS_PENDING)
                .event(KernelEmailEvents.PULL_SUCCESS)
            .and().withExternal()
                .source(KernelEmailStatus.ANALYSIS_PENDING)
                .target(KernelEmailStatus.ANALYZING)
                .event(KernelEmailEvents.START_ANALYSIS)
            .and().withExternal()
                .source(KernelEmailStatus.ANALYZING)
                .target(KernelEmailStatus.ANALYSIS_SUCCESS)
                .event(KernelEmailEvents.ANALYSIS_SUCCESS)
            .and().withExternal()
                .source(KernelEmailStatus.ANALYZING)
                .target(KernelEmailStatus.ANALYSIS_FAILED)
                .event(KernelEmailEvents.ANALYSIS_FAILURE)
            .and().withExternal()
                .source(KernelEmailStatus.ANALYSIS_SUCCESS)
                .target(KernelEmailStatus.GENERATING)
                .event(KernelEmailEvents.START_GENERATE)
            .and().withExternal()
                .source(KernelEmailStatus.GENERATING)
                .target(KernelEmailStatus.GENERATE_SUCCESS)
                .event(KernelEmailEvents.GENERATE_SUCCESS)
            .and().withExternal()
                .source(KernelEmailStatus.GENERATING)
                .target(KernelEmailStatus.GENERATE_FAILED)
                .event(KernelEmailEvents.GENERATE_FAILURE)
            .and().withExternal()
                .source(KernelEmailStatus.GENERATE_SUCCESS)
                .target(KernelEmailStatus.REPORT_PESISTING)
                .event(KernelEmailEvents.START_PESISTING)
            .and().withExternal()
                .source(KernelEmailStatus.REPORT_PESISTING)
                .target(KernelEmailStatus.REPORT_PERSISTENCE_SUCCESS)
                .event(KernelEmailEvents.PERSISTENCE_SUCCESS)
            .and().withExternal()
                .source(KernelEmailStatus.REPORT_PESISTING)
                .target(KernelEmailStatus.REPORT_PERSISTENCE_FAILED)
                .event(KernelEmailEvents.PERSISTENCE_FAILURE);
    }
}