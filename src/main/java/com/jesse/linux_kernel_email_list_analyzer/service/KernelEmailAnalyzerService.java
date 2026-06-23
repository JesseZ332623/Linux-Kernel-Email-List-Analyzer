package com.jesse.linux_kernel_email_list_analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeReportWriter;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeTemplateGanerator;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatMessage;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatThinking;
import com.jesse.linux_kernel_email_list_analyzer.properties.DeepSeekChatProperties;
import com.jesse.linux_kernel_email_list_analyzer.repository.ApplicationApiKeysRepository;
import com.jesse.linux_kernel_email_list_analyzer.request.AIModelChatRequest;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Linux 内核补丁邮件分析服务。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class KernelEmailAnalyzerService
{
    /** DeepSeek 模型对话属性配置类。*/
    private final DeepSeekChatProperties deepSeekChatProperties;

    /** Spring 封装的 HTTP 客户端。*/
    private final RestTemplate restTemplate;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository applicationApiKeysRepository;

    /** LKML 内核补丁邮件分析结果生成器接口。*/
    private final LKMLAnalyzeTemplateGanerator templateGanerator;

    /** LKML 内核补丁邮件分析结果持久化器接口。*/
    private final LKMLAnalyzeReportWriter reportWriter;

    /** 通用的 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** Spring 封装的资源加载器。*/
    private final ResourceLoader resourceLoader;

    /** 用内核补丁邮件的内容格式化用户指令提示词。*/
    private AIModelChatMessage
    formatUserRolePrompt(PlainTextEmail kernalEmail)
    {
        try
        {
            final String userRolePromptPattern
                = this.resourceLoader
                      .getResource(this.deepSeekChatProperties.getUsrPromptsClasspath())
                      .getContentAsString(StandardCharsets.UTF_8);

            return new
            AIModelChatMessage(
                "user",
                userRolePromptPattern.formatted(
                    kernalEmail.getMessageId(),
                    kernalEmail.getFrom(),
                    kernalEmail.getKernalTime(),
                    kernalEmail.getSubject(),
                    kernalEmail.getTextContent()
                )
            );
        }
        catch (IOException exception)
        {
            log.error(
                "User LKML prompt pattern {} not exist...",
                this.deepSeekChatProperties.getUsrPromptsClasspath()
            );

            return new AIModelChatMessage("user", "");
        }
    }

    /** 从 classpath 中读取 AI 系统指令提示词。*/
    private AIModelChatMessage readSystemRolePrompt()
    {
        try
        {
            final String userRolePromptPattern
                = this.resourceLoader
                      .getResource(this.deepSeekChatProperties.getSysPromptsClasspath())
                      .getContentAsString(StandardCharsets.UTF_8);

            return new
            AIModelChatMessage("system", userRolePromptPattern);
        }
        catch (IOException exception)
        {
            log.error(
                "System LKML prompt {} not exist...",
                this.deepSeekChatProperties.getSysPromptsClasspath()
            );

            return new AIModelChatMessage("system", "");
        }
    }

    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果字符串。*/
    private AIModelAnswerResponse
    doAnalyzer(PlainTextEmail kernelEmail) throws JsonProcessingException
    {
        log.info("Analyzing kernel email: {}", kernelEmail.getMessageId());

        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(
            "Authorization",
            "Bearer " + this.applicationApiKeysRepository
                            .findByAppName(this.deepSeekChatProperties.getAuthorizationName())
        );

        final AIModelChatRequest request = new AIModelChatRequest();

        request.setModel(this.deepSeekChatProperties.getModelName());
        request.setMessages(List.of(this.readSystemRolePrompt(), this.formatUserRolePrompt(kernelEmail)));
        request.setThinking(new AIModelChatThinking(this.deepSeekChatProperties.getThinking()));
        request.setReasoningEffort(this.deepSeekChatProperties.getReasoningEffort());
        request.setStream(this.deepSeekChatProperties.isStream());

        final HttpEntity<AIModelChatRequest> httpEntity
            = new HttpEntity<>(request, httpHeaders);

        // 启动计时器
        final StopWatch stopWatch = StopWatch.createStarted();

        final String responseJSON
            = this.restTemplate
                  .postForObject(
                      this.deepSeekChatProperties.getModelEndpointUrl(),
                      httpEntity, String.class
                  );

        stopWatch.stop();

        log.info(
            "POST {} call took [{}] milliseconds.",
            this.deepSeekChatProperties.getModelEndpointUrl(), stopWatch.getDuration().toMillis()
        );

        return
        this.objectMapper
            .readValue(responseJSON, AIModelAnswerResponse.class);
    }

    @RabbitListener(queues = "${app.rabbitmq-queue-props.lkml.queue-name}")
    public void handleKernelEmail(
        final PlainTextEmail        kernalEmail,
        final Channel               channel,
        final Message     message
    )
    {
        final long deliveryTag
            = message.getMessageProperties().getDeliveryTag();

        try
        {
            // (1) 执行分析
            final AIModelAnswerResponse response
                = this.doAnalyzer(kernalEmail);

            // (2) 生成分析报告
            final String htmlText
                = this.templateGanerator.generate(
                    new AnalyzeResultTemplateData(kernalEmail, response)
                );

            // (3) 写到本地文件中去
            this.reportWriter
                .write(kernalEmail.getSubject(), htmlText);

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