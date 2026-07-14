package com.jesse.linux_kernel_email_list_analyzer.service.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.global_id.GlobalIdConsumer;
import com.jesse.linux_kernel_email_list_analyzer.converter.AIModelAnswerAuditConverter;
import com.jesse.linux_kernel_email_list_analyzer.converter.AIModelAnswerContentConverter;
import com.jesse.linux_kernel_email_list_analyzer.converter.AIModelAnswerUsageConverter;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerAuditEntity;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerContentEntity;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerUsageEntity;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelAnswerAuditRepository;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelAnswerContentRepository;
import com.jesse.linux_kernel_email_list_analyzer.repository.AIModelAnswerUsageRepository;
import com.jesse.linux_kernel_email_list_analyzer.repository.LinuxKernerlEmailRepository;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import com.jesse.linux_kernel_email_list_analyzer.service.AIModelAnswerAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AI 模型 LKML 分析任务响应审计表服务类实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelAnswerAuditServiceImpl implements AIModelAnswerAuditService
{
    /** AI 模型 LKML 分析任务响应审计表仓库类。*/
    private final
    AIModelAnswerAuditRepository aiModelAnswerAuditRepository;

    /** AI 模型回答内容表仓储类。*/
    private final
    AIModelAnswerContentRepository aiModelAnswerContentRepository;

    /** AI 模型 LKML 分析任务 Token 消耗明细表仓储类。*/
    private final
    AIModelAnswerUsageRepository aiModelAnswerUsageRepository;

    /** 内核邮件数据表仓储类 */
    private final
    LinuxKernerlEmailRepository lkmlRepository;

    /** AI 模型 LKML 分析任务响应审计实体转换器。*/
    private final
    AIModelAnswerAuditConverter answerAuditConverter;

    /** AI 模型 LKML 分析任务模型文本输出实体转换器。*/
    private final
    AIModelAnswerContentConverter answerContentConverter;

    /** AI 模型 LKML 分析任务 Token 消耗明细实体转换器。*/
    private final
    AIModelAnswerUsageConverter answerUsageConverter;

    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer idConsumer;

    /** 执行完一封内核邮件的分析后，存储本次分析的审计信息。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PlainTextEmail email, AIModelAnswerResponse response)
    {
        final AIModelAnswerAuditEntity audit
            = this.answerAuditConverter
                  .convert(this.idConsumer.nextId(), response);

        final LinuxKernerlEmailEntiy lkml
            = LinuxKernerlEmailEntiy
                .fromPlainTextEmail(this.idConsumer.nextId(), response.getId(), email);

        final AIModelAnswerContentEntity content
            = this.answerContentConverter
                  .convert(this.idConsumer.nextId(), response);

        final AIModelAnswerUsageEntity usage
            = this.answerUsageConverter
                  .convert(this.idConsumer.nextId(), response);

        // (1) 保存模型信息摘要数据
        this.aiModelAnswerAuditRepository.insert(audit);

        // (2) 保存内核邮件数据
        this.lkmlRepository.insert(lkml);

        // (3) 保存模型回复文本数据
        this.aiModelAnswerContentRepository.insert(content);

        // (4) 保存模型 Token 消耗明细数据
        this.aiModelAnswerUsageRepository.insert(usage);

        log.info(
            "Save AI model answer audit record complete. (id = {}, task-id = {})",
            audit.getId(), audit.getTaskId()
        );
    }
}