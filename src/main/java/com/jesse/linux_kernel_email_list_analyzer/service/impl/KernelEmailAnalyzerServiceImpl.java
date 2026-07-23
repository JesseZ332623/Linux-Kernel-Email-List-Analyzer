package com.jesse.linux_kernel_email_list_analyzer.service.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.kernel_email_analyzer.KernelEmailAIModelAnalyzer;
import com.jesse.linux_kernel_email_list_analyzer.components.report_persistence.LKMLAnalyzeReportWriter;
import com.jesse.linux_kernel_email_list_analyzer.components.analyze_report_generator.LKMLAnalyzeTemplateGenerator;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import com.jesse.linux_kernel_email_list_analyzer.service.AIModelAnswerAuditService;
import com.jesse.linux_kernel_email_list_analyzer.service.KernelEmailAnalyzerService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/** Linux 内核补丁邮件分析服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class KernelEmailAnalyzerServiceImpl implements KernelEmailAnalyzerService
{
    /** 内核邮件 -> AI 模型 分析器接口类。*/
    private final
    KernelEmailAIModelAnalyzer kernelEmailAIModelAnalyzer;

    /** LKML 内核补丁邮件分析结果生成器接口。*/
    private final LKMLAnalyzeTemplateGenerator templateGenerator;

    /** LKML 内核补丁邮件分析结果持久化器接口。*/
    private final LKMLAnalyzeReportWriter reportWriter;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** AI 模型 LKML 分析任务响应审计表服务类接口。*/
    private final AIModelAnswerAuditService aiModelAnswerAuditService;

    /** 从队列中消费一封邮件消息并处理。*/
    @Override
    @RabbitListener(queues = "${app.rabbitmq-queue-props.lkml.queue-name}")
    public void handleKernelEmail(
        final Map<Long, PlainTextEmail> kernelEmailMap,
        final Channel channel,
        final Message message
    )
    {
        final long deliveryTag
            = message.getMessageProperties().getDeliveryTag();

        try
        {
            for (var kernelEmailEntry : kernelEmailMap.entrySet())
            {
                final long kernelEmailId         = kernelEmailEntry.getKey();
                final PlainTextEmail kernelEmail = kernelEmailEntry.getValue();

                // (1) 变更这封邮件的状态为 ANALYSIS_PENDING
                this.kernelEmailStateMachine
                    .fireEvent(kernelEmailId, KernelEmailEvents.PULL_SUCCESS);

                // (2) 执行分析
                final AIModelAnswerResponse response
                    = this.kernelEmailAIModelAnalyzer
                          .doAnalyze(kernelEmailId, kernelEmail);

                // (3) 审计本次分析的信息
                this.aiModelAnswerAuditService.save(kernelEmail, response);

                // (4) 生成分析报告
                final String htmlText
                    = this.templateGenerator.generate(
                        new AnalyzeResultTemplateData(kernelEmailId, kernelEmail, response)
                    );

                // (5) 写到本地文件中去
                this.reportWriter.write(kernelEmailId, kernelEmail, htmlText);
            }

            // (6) 确认消息
            channel.basicAck(deliveryTag, false);
        }
        catch (Exception exception)
        {
            log.error("", exception);

            try
            {
                // 目前的做法略显粗暴，在分析过程中出现了任何错误，
                // 这封邮件都入死信队列。
                log.warn("Delivery message {} to dead letter queue.", deliveryTag);
                channel.basicNack(deliveryTag, false, false);
            }
            catch (IOException ioException)
            {
                // 如果不确认调用失败了（比如和队列服务的连接断开），
                // 消息会自己回到队列，不会丢失。
                log.error(
                    "Nack kernel email message failed (delivery tag: {})",
                    deliveryTag, ioException
                );
            }
        }
    }
}