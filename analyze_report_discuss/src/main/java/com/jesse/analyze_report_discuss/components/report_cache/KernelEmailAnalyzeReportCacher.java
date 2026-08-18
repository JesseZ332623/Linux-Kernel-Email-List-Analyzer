package com.jesse.analyze_report_discuss.components.report_cache;

import com.jesse.analyze_report_discuss.dto.KenelEmailAnalyzeReport;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** 内核邮件分析报告缓存器接口。*/
public interface KenelEmailAnalyzeReportCacher
{
    /** 从缓存中获取分析报告数据。*/
    Optional<KenelEmailAnalyzeReport> getOrLoad(String taskId);
}