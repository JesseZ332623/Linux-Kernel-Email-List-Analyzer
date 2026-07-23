package com.jesse.linux_kernel_email_list_analyzer.components.report_persistence;

import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;

import java.io.IOException;

/** 内核补丁邮件分析报告持久化器接口。*/
public interface LKMLAnalyzeReportWriter
{
    void write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText) throws IOException;
}
