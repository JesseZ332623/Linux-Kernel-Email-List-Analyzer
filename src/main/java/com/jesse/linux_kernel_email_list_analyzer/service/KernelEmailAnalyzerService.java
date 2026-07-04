package com.jesse.linux_kernel_email_list_analyzer.service;

import com.jesse.linux_kernel_email_list_analyzer.components.KernelEmailAIModelAnalyzer;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeReportWriter;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeTemplateGenerator;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/** Linux 内核补丁邮件分析服务。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class KernelEmailAnalyzerService
{
    /** 内核邮件 -> AI 模型 分析器接口类。*/
    private final
    KernelEmailAIModelAnalyzer kernelEmailAIModelAnalyzer;

    /** LKML 内核补丁邮件分析结果生成器接口。*/
    private final LKMLAnalyzeTemplateGenerator templateGenerator;

    /** LKML 内核补丁邮件分析结果持久化器接口。*/
    private final LKMLAnalyzeReportWriter reportWriter;

    /** 从队列中消费一封邮件消息并处理。*/
    @RabbitListener(queues = "${app.rabbitmq-queue-props.lkml.queue-name}")
    public void handleKernelEmail(
        final PlainTextEmail kernalEmail,
        final Channel        channel,
        final Message        message
    )
    {
        final long deliveryTag
            = message.getMessageProperties().getDeliveryTag();

        try
        {
            // (1) 执行分析
            final AIModelAnswerResponse response
                = this.kernelEmailAIModelAnalyzer.doAnalyze(kernalEmail);

            // (2) 生成分析报告
            final String htmlText
                = this.templateGenerator.generate(
                    new AnalyzeResultTemplateData(kernalEmail, response)
                );

            // (3) 写到本地文件中去
            this.reportWriter.write(kernalEmail, htmlText);

            // (4) 确认消息
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