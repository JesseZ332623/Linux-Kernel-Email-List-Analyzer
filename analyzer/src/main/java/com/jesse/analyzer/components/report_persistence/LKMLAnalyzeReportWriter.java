package com.jesse.analyzer.components.report_persistence;

import com.jesse.core.pojo.PlainTextEmail;

import java.io.IOException;

/** 内核补丁邮件分析报告持久化器接口。*/
public interface LKMLAnalyzeReportWriter
{
    /**
     * 写入分析报告到指定位置。
     *
     * @param kernelEmailId  邮件在数据库中的 ID
     * @param plainTextEmail 邮件数据实例
     * @param htmlText       thymeleaf 生成的邮件分析报告 HTML 文本
     */
    void write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText) throws IOException;
}