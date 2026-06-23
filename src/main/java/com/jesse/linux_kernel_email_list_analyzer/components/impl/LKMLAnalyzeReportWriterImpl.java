package com.jesse.linux_kernel_email_list_analyzer.components.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeReportWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

/** LKML 内核补丁邮件分析结果持久化器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class LKMLAnalyzeReportWriterImpl implements LKMLAnalyzeReportWriter
{
    private final static int MAX_FILENAME_LEN = 200;

    private final static
    Pattern ILLEGAL_PATTERN = Pattern.compile("[<>:\"/\\\\|?*]");

    private final static
    Path REPORT_PATH_PREFIX = Path.of("F:\\LKML-Analyze-Report");


    @Override
    public void write(String subject, String htmlText) throws IOException
    {
         String reportName
            = ILLEGAL_PATTERN.matcher(subject).replaceAll("_");

        if (reportName.length() > MAX_FILENAME_LEN) {
            reportName = reportName.substring(0, MAX_FILENAME_LEN);
        }

        final Path finalReportPath
            = REPORT_PATH_PREFIX.resolve(reportName + ".html").normalize();

        log.info("Save analyze report to {}", finalReportPath);

        Files.writeString(
            finalReportPath, htmlText,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        );
    }
}