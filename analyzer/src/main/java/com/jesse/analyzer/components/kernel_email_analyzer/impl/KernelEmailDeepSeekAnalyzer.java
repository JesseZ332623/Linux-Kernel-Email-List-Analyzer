package com.jesse.analyzer.components.kernel_email_analyzer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.analyzer.components.kernel_email_analyzer.KernelEmailAIModelAnalyzer;
import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.core.components.llm_client.LLMClient;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.properties.DeepSeekChatPropertiesMap;
import com.jesse.core.response.AIModelAnswerResponse;
import com.jesse.analyzer.service.LinuxKernelEmailService;
import com.jesse.core.pojo.PlainTextEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 内核邮件 -> DeepSeek 模型分析器实现类。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailDeepSeekAnalyzer implements KernelEmailAIModelAnalyzer
{
    /** DeepSeek 模型（业务域 -> 模型属性）映射表配置类。*/
    private final DeepSeekChatPropertiesMap properties;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** 内核邮件数据表服务实现类。*/
    private final
    LinuxKernelEmailService linuxKernelEmailService;

    /** Spring 封装的资源加载器。*/
    private final ResourceLoader resourceLoader;

    /** LLM 大模型 API 对接客户端接口。*/
    private final LLMClient llmClient;

    /** 用内核补丁邮件的内容格式化用户指令提示词。*/
    private AIModelChatMessage
    formatUserRolePrompt(PlainTextEmail kernelEmail)
    {
        final String usrRolePromptClasspath
            = this.properties.getKernelEmailAnalyzerProp().getUsrPromptsClasspath();

        try
        {
            final String userRolePromptPattern
                = this.resourceLoader
                      .getResource(usrRolePromptClasspath)
                      .getContentAsString(StandardCharsets.UTF_8);

            return new
            AIModelChatMessage(
                "user",
                userRolePromptPattern.formatted(
                    kernelEmail.getMessageId(),
                    kernelEmail.getFrom(),
                    kernelEmail.getKernelTime(),
                    kernelEmail.getSubject(),
                    kernelEmail.getTextContent()
                )
            );
        }
        catch (IOException exception)
        {
            log.error("User LKML prompt pattern {} not exist...", usrRolePromptClasspath);

            return new
            AIModelChatMessage("user", "");
        }
    }

    /** 从 classpath 中读取 AI 系统指令提示词。*/
    private AIModelChatMessage readSystemRolePrompt()
    {
        final String sysRolePromptClasspath
            = this.properties.getKernelEmailAnalyzerProp().getSysPromptsClasspath();

        try
        {
            final String userRolePromptPattern
                = this.resourceLoader
                      .getResource(sysRolePromptClasspath)
                      .getContentAsString(StandardCharsets.UTF_8);

            return new
            AIModelChatMessage("system", userRolePromptPattern);
        }
        catch (IOException exception)
        {
            log.error("System LKML prompt {} not exist...", sysRolePromptClasspath);

            return new AIModelChatMessage("system", "");
        }
    }

    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果响应实例。*/
    @Override
    public AIModelAnswerResponse
    doAnalyze(long kernelEmailId, PlainTextEmail kernelEmail) throws JsonProcessingException
    {
        log.info("Analyzing kernel email: {}", kernelEmail.getMessageId());

        // (1) 流转本邮件的状态为 正在分析中
        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_ANALYSIS);

        // (2) 构造提示词列表
        final List<AIModelChatMessage> prompts
            = List.of(this.readSystemRolePrompt(), this.formatUserRolePrompt(kernelEmail));

        try
        {
            // (3) 向 LLM 模型发起请求
            final AIModelAnswerResponse analyzeResponse
                = this.llmClient.complete(this.properties.getKernelEmailAnalyzerProp(), prompts);

            // (4) 将指定 id 的邮件与指定的分析任务关联
            this.linuxKernelEmailService
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