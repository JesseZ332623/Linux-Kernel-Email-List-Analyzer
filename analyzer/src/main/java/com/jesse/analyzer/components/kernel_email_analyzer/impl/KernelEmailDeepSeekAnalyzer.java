package com.jesse.analyzer.components.kernel_email_analyzer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyzer.components.kernel_email_analyzer.KernelEmailAIModelAnalyzer;
import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.analyzer.pojo.ai.AIModelChatMessage;
import com.jesse.analyzer.pojo.ai.AIModelChatThinking;
import com.jesse.analyzer.request.AIModelChatRequest;
import com.jesse.analyzer.response.AIModelAnswerResponse;
import com.jesse.analyzer.service.LinuxKernerlEmailService;
import com.jesse.core.annotation.TimeMonitor;
import com.jesse.core.pojo.PlainTextEmail;
import com.jesse.core.properties.DeepSeekChatProperties;
import com.jesse.core.repository.ApplicationApiKeysRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 内核邮件 -> DeepSeek 模型分析器实现类。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailDeepSeekAnalyzer implements KernelEmailAIModelAnalyzer
{
    /** DeepSeek 模型对话属性配置类。*/
    private final DeepSeekChatProperties deepSeekChatProperties;

    /** Spring 封装的 HTTP 客户端。*/
    private final RestTemplate restTemplate;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository applicationApiKeysRepository;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** 内核邮件数据表服务实现类。*/
    private final
    LinuxKernerlEmailService linuxKernerlEmailService;

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
                    kernalEmail.getKernelTime(),
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

            return new
            AIModelChatMessage("user", "");
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

    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果响应实例。*/
    @Override
    @TimeMonitor(
        logArgs       = true,
        warnThreshold = 300L,
        timeunit      = TimeUnit.SECONDS
    )
    public AIModelAnswerResponse
    doAnalyze(long kernelEmailId, PlainTextEmail kernelEmail) throws JsonProcessingException
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

        // (1) 流转本邮件的状态为 正在分析中
        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_ANALYSIS);

        try
        {
            // (2) 向 DeepSeek 模型发起 POST 请求
            final String responseJSON
                = this.restTemplate
                      .postForObject(
                          this.deepSeekChatProperties.getModelEndpointUrl(),
                          new HttpEntity<>(request, httpHeaders),
                          String.class
                      );

            // (3) 解析响应体
            final AIModelAnswerResponse analyzeResponse
                = this.objectMapper
                .readValue(responseJSON, AIModelAnswerResponse.class);

            // (4) 将指定 id 的邮件与指定的分析任务关联
            this.linuxKernerlEmailService
                .updateTaskIdById(kernelEmailId, analyzeResponse.getId());

            // (5) 流转本邮件的状态为 分析成功
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.ANALYSIS_SUCCESS);

            // (6) 返回响应体
            return analyzeResponse;
        }
        catch (JsonProcessingException | RestClientException exception)
        {
            // 如果出现 API 调用错误或者响应体 JSON 解析错误，
            // 则重置分析执行状态为分析失败
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.ANALYSIS_FAILURE);

            throw exception;
        }
    }
}