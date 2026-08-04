package com.jesse.analyze_report_discuss.service;

import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 内核邮件分析报告讨论服务接口。*/
public interface AnalyzeReportDiscussService
{
    /** 在某个内核邮件分析报告下发起一次讨论。*/
    void discuss(ReportDiscussRequest discussRequest, SseEmitter sseEmitter);
}