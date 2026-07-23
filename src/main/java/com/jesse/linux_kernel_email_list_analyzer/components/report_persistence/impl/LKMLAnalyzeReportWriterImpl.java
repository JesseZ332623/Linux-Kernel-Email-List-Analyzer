package com.jesse.linux_kernel_email_list_analyzer.components.report_persistence.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.report_persistence.LKMLAnalyzeReportWriter;
import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** LKML 内核补丁邮件分析结果持久化器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class LKMLAnalyzeReportWriterImpl implements LKMLAnalyzeReportWriter
{
    /** 内核邮件分类器接口。*/
    private final KernelEmailClassifier kernelEmailClassifier;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    @Value("${app.report-path-prefix}")
    private String REPORT_PATH_PREFIX;

    @Value("${app.max-file-name-len}")
    private int MAX_FILENAME_LEN;

    /** 通过内核补丁邮件标题构建报告文件名。*/
    private String makeReportName(String subject)
    {
        final String reportName
            = RegexUtils.ILLEGAL_CHARACTOR_PATTERN
                        .matcher(subject).replaceAll("_");

        return
        (reportName.length() > MAX_FILENAME_LEN)
            ? reportName.substring(0, MAX_FILENAME_LEN) + ".html"
            : reportName + ".html";
    }

    @Override
    public void
    write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText) throws IOException
    {
        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_PESISTING);

        final String from    = plainTextEmail.getFrom();
        final String subject = plainTextEmail.getSubject();

        // (1) 拼接报告的完整路径
        final Path finalReportPath
            = Path.of(REPORT_PATH_PREFIX)
                  .resolve(this.kernelEmailClassifier.classify(from, subject))
                  .resolve(this.makeReportName(subject))
                  .normalize();

        log.info("Save analyze report to {}", finalReportPath);

        try
        {
            // (2) 确保父目录存在，不存在则创建反之跳过
            Files.createDirectories(finalReportPath.getParent());

            // (3) 将报告写入指定目录下
            Files.writeString(
                finalReportPath, htmlText,
                StandardOpenOption.CREATE
            );

            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_SUCCESS);
        }
        catch (IOException exception)
        {
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_FAILURE);

            throw exception;
        }
    }
}