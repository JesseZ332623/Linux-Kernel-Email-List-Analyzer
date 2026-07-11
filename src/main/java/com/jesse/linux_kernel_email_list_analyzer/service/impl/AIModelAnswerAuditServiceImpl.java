package com.jesse.linux_kernel_email_list_analyzer.service.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.global_id.GlobalIdConsumer;
import com.jesse.linux_kernel_email_list_analyzer.converter.AIModelAnswerAuditConverter;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerAudit;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelAnswerAuditRepository;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import com.jesse.linux_kernel_email_list_analyzer.service.AIModelAnswerAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** AI 模型 LKML 分析任务响应审计表服务类实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelAnswerAuditServiceImpl implements AIModelAnswerAuditService
{
    /** AI 模型 LKML 分析任务响应审计表仓库类。*/
    private final AIModelAnswerAuditRepository repository;

    /** AI 模型 LKML 分析任务响应审计实体转换器。*/
    private final AIModelAnswerAuditConverter answerAuditConverter;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer idConsumer;

    /** 执行完一封内核邮件的分析后，存储本次分析的审计信息。*/
    @Override
    public void save(AIModelAnswerResponse response)
    {
        final AIModelAnswerAudit audit
            = this.answerAuditConverter
                  .convert(this.idConsumer.nextId(), response);

        this.repository.insert(audit);

        log.info(
            "Save AI model answer audit record complete. (id = {}, task-id = {})",
            audit.getId(), audit.getTaskId()
        );
    }
}