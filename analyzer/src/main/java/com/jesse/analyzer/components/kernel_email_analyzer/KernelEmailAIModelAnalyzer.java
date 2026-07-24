package com.jesse.analyzer.components.kernel_email_analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.analyzer.response.AIModelAnswerResponse;
import com.jesse.core.pojo.PlainTextEmail;

/** 内核邮件 -> AI 模型 分析器接口类。*/
public interface KernelEmailAIModelAnalyzer
{
    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果响应实例。*/
    AIModelAnswerResponse
    doAnalyze(long kernelEmailId, PlainTextEmail kernelEmail) throws JsonProcessingException;
}