package com.jesse.response_audit.service;

import com.jesse.core.response.AIModelAnswerResponse;

/** AI 模型响应审计表服务类接口。*/
public interface AIModelAnswerAuditService
{
    /** 在向大模型发起一次调用后，存储本次调用的审计信息。*/
    void save(AIModelAnswerResponse response);
}