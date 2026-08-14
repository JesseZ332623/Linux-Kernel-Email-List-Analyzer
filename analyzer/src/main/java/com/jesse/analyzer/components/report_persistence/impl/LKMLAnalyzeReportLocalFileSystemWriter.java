package com.jesse.analyzer.components.report_persistence.impl;

import com.jesse.analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.analyzer.components.report_persistence.LKMLAnalyzeReportWriter;
import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.core.pojo.PlainTextEmail;
import com.jesse.core.properties.AnalyzeReportPersistenceProperties;
import com.jesse.core.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** LKML 内核补丁邮件分析结果本地文件系统持久化器实现。*/
@Slf4j
@Component("analyze-report-filesystem-writer")
@RequiredArgsConstructor
public class LKMLAnalyzeReportLocalFileSystemWriter implements LKMLAnalyzeReportWriter
{
    /** 内核邮件分类器接口。*/
    private final KernelEmailClassifier kernelEmailClassifier;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** LKML 分析报告持久化配置属性类。*/
    private final
    AnalyzeReportPersistenceProperties reportPersistenceProperties;

    /** 通过内核补丁邮件标题构建报告文件名。*/
    private String makeReportName(String subject)
    {
        if (!StringUtils.hasText(subject)) {
            return "unknown-report.html";
        }

        final String reportName
            = RegexUtils.ILLEGAL_CHARACTOR_PATTERN
                        .matcher(subject).replaceAll("_");

        final int maxFileNameLen
            = this.reportPersistenceProperties.getMaxFileNameLen();

        return
        (reportName.length() > maxFileNameLen)
            ? reportName.substring(0, maxFileNameLen) + ".html"
            : reportName + ".html";
    }

    @Override
    public void
    write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText) throws IOException
    {
        // (1) 流转邮件的状态为 “开始持久化”
        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_PESISTING);

        final String from    = plainTextEmail.getFrom();
        final String subject = plainTextEmail.getSubject();
        final String localPathPrefix
            = this.reportPersistenceProperties.getReportPathPrefix();

        // (2) 拼接报告的完整路径
        final Path finalReportPath
            = Path.of(localPathPrefix)
                  .resolve(this.kernelEmailClassifier.classify(from, subject))
                  .resolve(this.makeReportName(subject))
                  .normalize();

        try
        {
            // (3) 确保父目录存在，不存在则创建反之跳过
            Files.createDirectories(finalReportPath.getParent());

            // (4) 将报告写入指定目录下
            Files.writeString(
                finalReportPath, htmlText,
                StandardOpenOption.CREATE
            );

            // (5) 流转邮件状态为 “持久化成功”
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_SUCCESS);

            log.info("Save analyze report to {}", finalReportPath);
        }
        catch (IOException exception)
        {
            // 期间出现任何异常则流转状态为 “持久化失败”
            // 并向外传递异常
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_FAILURE);

            throw exception;
        }
    }
}