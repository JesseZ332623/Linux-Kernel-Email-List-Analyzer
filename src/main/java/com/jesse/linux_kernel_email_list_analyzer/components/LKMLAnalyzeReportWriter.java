package com.jesse.linux_kernel_email_list_analyzer.components;

import java.io.IOException;

/** 内核补丁邮件分析报告持久化器接口。*/
public interface LKMLAnalyzeReportWriter
{
    void write(String subject, String htmlText) throws IOException;
}
