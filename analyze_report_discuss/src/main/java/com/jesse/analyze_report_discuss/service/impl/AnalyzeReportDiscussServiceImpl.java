package com.jesse.analyze_report_discuss.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyze_report_discuss.components.discuss_abstract.AnalyzeReportDiscussAbstractor;
import com.jesse.analyze_report_discuss.components.discuss_session_lock.DiscussSessionLockGurard;
import com.jesse.analyze_report_discuss.components.prompt_reader.ModelPromptReader;
import com.jesse.analyze_report_discuss.components.report_cache.AnalyzerReportDiscussAbstractCacher;
import com.jesse.analyze_report_discuss.components.report_cache.KenelEmailAnalyzeReportCacher;
import com.jesse.analyze_report_discuss.components.sse_callback.SSECallBack;
import com.jesse.analyze_report_discuss.dto.KenelEmailAnalyzeReport;
import com.jesse.analyze_report_discuss.exception.DiscussException;
import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussService;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionDetailsService;
import com.jesse.core.utils.HttpClientUtils;
import com.jesse.core.properties.DeepSeekAnalyzerReportDiscussProperties;
import com.jesse.core.properties.DeepSeekChatProperties;
import com.jesse.core.repository.ApplicationApiKeysRepository;
import com.jesse.response_audit.service.AIModelAnswerAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;

import static java.lang.String.format;

/** 内核邮件分析报告讨论服务实现。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeReportDiscussServiceImpl
    implements AnalyzeReportDiscussService
{
    /** 模型提示词读取器。*/
    private final ModelPromptReader modelPromptReader;

    /** 通用 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository apiKeysRepository;

    /** AI 模型响应审计表服务类接口。*/
    private final
    AIModelAnswerAuditService aiModelAnswerAuditService;

    /** Linux 内核邮件分析报告疑惑解答会话对话内容表服务接口。*/
    private final
    AnalyzeReportDiscussSessionDetailsService discussSessionDetailsService;

    /** 内核邮件分析报告缓存器接口。*/
    private final
    KenelEmailAnalyzeReportCacher analyzeReportCacher;

    /** 内核邮件分析报告讨论记录摘要缓存器接口。*/
    private final
    AnalyzerReportDiscussAbstractCacher discussAbstractCacher;

    /** 分析报告讨论上下文摘要器接口。*/
    private final
    AnalyzeReportDiscussAbstractor analyzeReportDiscussAbstractor;

    /** OK HTTP 客户端，专用与处理 SSE 协议的响应数据。*/
    private final OkHttpClient okHttpClient;

    /** 自定义 OK HTTP 工具类。*/
    private final HttpClientUtils httpClientUtils;

    /** DeepSeek 分析报告答疑解惑模块模型属性配置类。*/
    private final
    DeepSeekAnalyzerReportDiscussProperties properties;

    /** 内核邮件分析报告讨论专用虚拟线程池执行器。*/
    @Qualifier("analyze-report-discuss-executor")
    private final
    ExecutorService analyzeReportDiscussExecutor;

    /** 讨论会话锁管理器接口。*/
    private final
    DiscussSessionLockGurard discussSessionLockGurard;

    /** 构建异步响应处理回调实例。*/
    private Callback newSSECallback(
        final Long                 sessionDetailId,
        final ReportDiscussRequest discussRequest,
        final SseEmitter           sseEmitter
    )
    {
        return
        SSECallBack.create(
            sessionDetailId,
            discussRequest,
            sseEmitter,
            this.objectMapper,
            this.discussSessionDetailsService,
            this.aiModelAnswerAuditService,
            this.analyzeReportDiscussAbstractor,
            this.analyzeReportDiscussExecutor,
            this.discussSessionLockGurard
        );
    }

    /** 在某个内核邮件分析报告下发起一次讨论。*/
    @Override
    public void
    discuss(ReportDiscussRequest discussRequest, SseEmitter sseEmitter)
    {
        // 2026.08.06 修复，对于同一个对话下，讨论的发起必须是串行的，
        // 这也是所有 AI 产品的设计标准，所以此处需要使用锁控制并发。
        if (!this.discussSessionLockGurard.tryAcquire(discussRequest.getSessionId()))
        {
            throw new
            DiscussException(
                format(
                    "Session %s is currently generating response. " +
                    "Please wait for completion or cancel ongoing request.",
                    discussRequest.getSessionId()
                )
            );
        }

        final DeepSeekChatProperties chatProperties
            = this.properties.getAnalyzerReportChatProp();

        final String taskId    = discussRequest.getTaskId();
        final String sessionId = discussRequest.getSessionId();
        final String question  = discussRequest.getQuestion();

        // (1) 插入一条新的会话明细
        long sessionDetailId
            = this.discussSessionDetailsService
                  .insertNewSessionDetail(sessionId, question);

        // (2) 从缓存中拿分析报告文本
        final KenelEmailAnalyzeReport analyzeReport
            = this.analyzeReportCacher.getOrLoad(taskId)
                  .orElseThrow(() -> {
                      log.error("Analyze report (which id = {}) not exist.", discussRequest.getTaskId());
                      return new DiscussException("Analyze report not exist.");
                  });

        // (3) 读取讨论任务系统提示词
        final String chatSysPrompt
            = this.modelPromptReader.read(chatProperties.getSysPromptsClasspath())
                  .orElseThrow(() -> new DiscussException("Discuss system prompt file not exist."));

        // (4) 读取并拼接讨论任务用户提示词
        final String chatUsrPrompt
            = this.modelPromptReader
                  .read(chatProperties.getUsrPromptsClasspath())
                  .orElseThrow(() -> new DiscussException("Discuss user prompt file not exist."))
                  .formatted(
                      analyzeReport.getEmailSubject(),
                      analyzeReport.getEmailContent(),
                      analyzeReport.getReportContent(),
                      this.discussAbstractCacher.get(discussRequest.getSessionId()).orElse("[暂无]"),
                      question
                  );

        // (5) 获取讨论任务大模型的 API Key
        final String apiKey
            = this.apiKeysRepository
                  .findByAppName(chatProperties.getAuthorizationName());

        // (6) 构造请求体
        final Request request
            = this.httpClientUtils
                  .makeOkRequest(apiKey, chatSysPrompt, chatUsrPrompt, chatProperties);

        // (7) 发起请求，异步的往前端推送数据流
        this.okHttpClient.newCall(request)
            .enqueue(this.newSSECallback(sessionDetailId, discussRequest, sseEmitter));
    }
}