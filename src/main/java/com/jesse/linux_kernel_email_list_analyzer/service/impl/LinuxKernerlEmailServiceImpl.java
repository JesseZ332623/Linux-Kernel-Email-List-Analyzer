package com.jesse.linux_kernel_email_list_analyzer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jesse.linux_kernel_email_list_analyzer.components.global_id.GlobalIdConsumer;
import com.jesse.linux_kernel_email_list_analyzer.constant.KernelEmailAnalyzeStatus;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.repository.LinuxKernerlEmailRepository;
import com.jesse.linux_kernel_email_list_analyzer.service.LinuxKernerlEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 内核邮件数据表服务实现类。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class LinuxKernerlEmailServiceImpl
    extends ServiceImpl<LinuxKernerlEmailRepository, LinuxKernerlEmailEntiy>
    implements LinuxKernerlEmailService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer idConsumer;

    /** 插入一条新内核邮件数据。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public long insertNew(PlainTextEmail email)
    {
        final long nextId = this.idConsumer.nextId();

        final LinuxKernerlEmailEntiy lkml
            = LinuxKernerlEmailEntiy
                .fromPlainTextEmail(nextId, "", email);

        this.baseMapper.insert(lkml);

        return nextId;
    }

    /** 修改某个内核邮件的分析任务执行状态。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int
    updateAnalyzeStatusByTaskId(String taskId, KernelEmailAnalyzeStatus status) {
        return this.baseMapper.updateAnalyzeStatusByTaskId(taskId, status);
    }

    /** 修改某个内核邮件的分析任务执行状态。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int updateAnalyzeStatusById(Long id, KernelEmailAnalyzeStatus status) {
        return this.baseMapper.updateAnalyzeStatusById(id, status);
    }

    /** 将指定 id 的邮件与指定的分析任务关联。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int updateTaskIdById(Long id, String taskId) {
        return this.baseMapper.updateTaskIdById(id, taskId);
    }
}