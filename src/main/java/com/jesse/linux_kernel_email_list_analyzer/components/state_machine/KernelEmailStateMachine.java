package com.jesse.linux_kernel_email_list_analyzer.components.state_machine;

/** 内核邮件状态机接口。*/
public interface KernelEmailStateMachine
{
    /** 对指定邮件触发一个事件，驱动状态流转。*/
    KernelEmailStatus
    fireEvent(long emailId, KernelEmailEvents event);
}