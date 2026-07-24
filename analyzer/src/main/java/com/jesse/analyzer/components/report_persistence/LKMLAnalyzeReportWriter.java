package com.jesse.analyzer.components.report_persistence;

import com.jesse.core.pojo.PlainTextEmail;

import java.io.IOException;

/** 内核补丁邮件分析报告持久化器接口。*/
public interface LKMLAnalyzeReportWriter
{
    void write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText) throws IOException;
}
