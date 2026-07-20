package com.jesse.linux_kernel_email_list_analyzer.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;

/** 内核邮件 -> AI 模型 分析器接口类。*/
public interface KernelEmailAIModelAnalyzer
{
    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果响应实例。*/
    AIModelAnswerResponse
    doAnalyze(long kernelEmailId, PlainTextEmail kernelEmail) throws JsonProcessingException;
}