package com.jesse.analyzer.service;

import com.jesse.analyzer.response.AIModelAnswerResponse;
import com.jesse.core.pojo.PlainTextEmail;

/** AI 模型 LKML 分析任务响应审计表服务类接口。*/
public interface AIModelAnswerAuditService
{
    /** 执行完一封内核邮件的分析后，存储本次分析的审计信息。*/
    void save(PlainTextEmail email, AIModelAnswerResponse response);
}