package com.jesse.analyze_report_discuss.components.report_cache;

import java.util.Optional;

/** 内核邮件分析报告讨论记录摘要缓存器接口。*/
public interface AnalyzerReportDiscussAbstractCacher
{
    Optional<String> get(String sessionId);

    void set(String sessionId, String abstractText);
}