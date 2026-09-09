package com.jesse.analyze_report_discuss.components.discuss_abstract.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.analyze_report_discuss.components.discuss_abstract.AnalyzeReportDiscussAbstractor;
import com.jesse.analyze_report_discuss.components.prompt_reader.ModelPromptReader;
import com.jesse.analyze_report_discuss.components.report_cache.AnalyzerReportDiscussAbstractCacher;
import com.jesse.analyze_report_discuss.components.report_cache.KernelEmailAnalyzeReportCacher;
import com.jesse.analyze_report_discuss.dto.KernelEmailAnalyzeReport;
import com.jesse.analyze_report_discuss.exception.DiscussException;
import com.jesse.analyze_report_discuss.request.DiscussAbstractRequest;
import com.jesse.core.components.llm_client.LLMClient;
import com.jesse.core.pojo.ai.AIModelChatMessage;
import com.jesse.core.pojo.DeepSeekChatProperties;
import com.jesse.core.properties.DeepSeekChatPropertiesMap;
import com.jesse.core.response.AIModelAnswerResponse;
import com.jesse.response_audit.service.AIModelAnswerAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** 分析报告讨论上下文 DeepSeek 摘要器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeReportDiscussDeepSeekAbstractor
    implements AnalyzeReportDiscussAbstractor
{
    /** AI 模型响应审计表服务类接口。*/
    private final
    AIModelAnswerAuditService aiModelAnswerAuditService;

    /** 内核邮件分析报告缓存器接口。*/
    private final
    KernelEmailAnalyzeReportCacher analyzeReportCacher;

    /** 内核邮件分析报告讨论记录摘要缓存器接口。*/
    private final
    AnalyzerReportDiscussAbstractCacher reportDiscussAbstractCacher;

    /** 模型提示词读取器。*/
    private final ModelPromptReader modelPromptReader;

    /** LLM 大模型 API 对接客户端接口。*/
    private final LLMClient llmClient;

    /** DeepSeek 模型（业务域 -> 模型属性）映射表配置类。*/
    private final DeepSeekChatPropertiesMap analyzerReportDiscussProperties;

    /** 生成摘要任务用户提示词。*/
    private String
    generateAbstractUserPrompt(DiscussAbstractRequest request)
    {
        // (1) 获取 内核邮件分析报告答疑解惑上下文摘要模型配置
        final DeepSeekChatProperties properties
            = this.analyzerReportDiscussProperties
                  .getAnalyzerReportChatAbstractProp();

        final String taskId = request.getTaskId();

        // (2) 读取摘要任务用户提示词模板
        final String promptTemplate
            = this.modelPromptReader
                  .read(properties.getUsrPromptsClasspath())
                  .orElseThrow(() -> new DiscussException("Abstract user prompt file not exist."));

        // (3) 从缓存中获取邮件文本和分析报告信息
        final KernelEmailAnalyzeReport analyzeReport
            = this.analyzeReportCacher.getOrLoad(taskId)
                  .orElseThrow(() -> {
                      log.error("Analyze report (which id = {}) not exist.", taskId);
                      return new DiscussException("Analyze report not exist.");
                  });

        // (4) 模型的回复文本不需要回表查，上游直接传递即可
        final String modelAnswerContent
            = request.getAggregatedResponse().getChoices().getFirst()
                    .getMessage()
                    .getContent();

        // (5) 填充用户提示词模板并返回
        return promptTemplate.formatted(
            analyzeReport.getEmailSubject(),
            analyzeReport.getEmailContent(),
            analyzeReport.getReportContent(),
            request.getQuestion(),
            modelAnswerContent
        );
    }

    /**
     * 一轮问答结束后，
     * 将本轮问答的信息交给轻量级模型做摘要并缓存，作为下一次对话的上下文。
     */
    @Override
    public void
    discussAbstract(DiscussAbstractRequest request)
    {
        // (1) 获取摘要任务的模型属性
        final DeepSeekChatProperties properties
            = this.analyzerReportDiscussProperties
                  .getAnalyzerReportChatAbstractProp();

        // (2) 读取摘要任务的系统提示词
        final String discussAbstractSysPromt
            = this.modelPromptReader
                  .read(properties.getSysPromptsClasspath())
                  .orElseThrow(() -> new DiscussException("Abstract system prompt file not exist."));

        // (3) 读取并填充摘要任务的用户提示词
        final String abstractUserPrompt
            = this.generateAbstractUserPrompt(request);

        // (4) 构造提示词列表
        final List<AIModelChatMessage> prompts
            = List.of(
                new AIModelChatMessage("system", discussAbstractSysPromt),
                new AIModelChatMessage("user", abstractUserPrompt)
            );

        try
        {
            // (5) 发起 HTTP 请求执行上下文摘要任务
            final AIModelAnswerResponse abstractResponse
                = this.llmClient.complete(properties, prompts);

            // (6) 将上下文摘要缓存到会话中
            this.reportDiscussAbstractCacher.set(
                request.getSessionId(),
                abstractResponse.getChoices().getFirst().getMessage().getContent()
            );

            // (7) 保存本次模型的审计信息
            this.aiModelAnswerAuditService.save(abstractResponse);
        }
        catch (JsonProcessingException | RestClientException exception)
        {
            log.error(
                "Abstract context for session: {} failed.",
                request.getSessionId(), exception
            );
        }
    }
}