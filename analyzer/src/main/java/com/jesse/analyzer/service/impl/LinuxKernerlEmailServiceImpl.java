package com.jesse.analyzer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jesse.analyzer.components.state_machine.KernelEmailStatus;
import com.jesse.analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.analyzer.repository.LinuxKernerlEmailRepository;
import com.jesse.analyzer.service.LinuxKernerlEmailService;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.pojo.PlainTextEmail;
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
                .fromPlainTextEmail(nextId, email);

        this.baseMapper.insert(lkml);

        return nextId;
    }

    /** 修改某个内核邮件的分析任务执行状态。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int
    updateStatusByTaskId(String taskId, KernelEmailStatus status) {
        return this.baseMapper.updateStatusByTaskId(taskId, status);
    }

    /** 修改某个内核邮件的分析任务执行状态。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int updateStatusById(Long id, KernelEmailStatus status) {
        return this.baseMapper.updateStatusById(id, status);
    }

    /** 将指定 id 的邮件与指定的分析任务关联。*/
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public int updateTaskIdById(Long id, String taskId) {
        return this.baseMapper.updateTaskIdById(id, taskId);
    }
}