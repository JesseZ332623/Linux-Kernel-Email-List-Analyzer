package com.jesse.linux_kernel_email_list_analyzer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStatus;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;

/** 内核邮件数据表服务接口类。*/
public interface LinuxKernerlEmailService
    extends IService<LinuxKernerlEmailEntiy>
{
    /** 插入一条新内核邮件数据，返回这条数据的 ID。*/
    long insertNew(PlainTextEmail email);

    /** 修改指定 taskId 的内核邮件的状态。*/
    int updateStatusByTaskId(String taskId, KernelEmailStatus status);

    /** 修改指定 id 的内核邮件的状态。*/
    int updateStatusById(Long id, KernelEmailStatus status);

    /** 将指定 id 的邮件与指定的分析任务关联。*/
    int updateTaskIdById(Long id, String taskId);
}