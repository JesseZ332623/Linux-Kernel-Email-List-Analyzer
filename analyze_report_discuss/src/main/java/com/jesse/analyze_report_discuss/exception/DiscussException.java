package com.jesse.analyze_report_discuss.exception;

import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 在 {@link AnalyzeReportDiscussService#discuss(ReportDiscussRequest, SseEmitter)}} 出现错误时抛本异常。*/
public class DiscussException extends RuntimeException
{
    public DiscussException(String message) {
        super(message);
    }
}