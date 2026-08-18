package com.jesse.analyzer.components.report_persistence.impl;

import com.jesse.analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.analyzer.components.report_persistence.LKMLAnalyzeReportWriter;
import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.core.components.oss.OssStorageManager;
import com.jesse.core.pojo.PlainTextEmail;
import com.jesse.core.properties.AnalyzeReportPersistenceProperties;
import com.jesse.core.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;

/** 内核补丁邮件分析报告 OSS 持久化器。*/
@Slf4j
@Primary
@Component("analyze-report-oss-writer")
@RequiredArgsConstructor
public class LKMLAnalyzeReportOssWriter implements LKMLAnalyzeReportWriter
{

    /** OSS 服务对象管理器组件。*/
    private final OssStorageManager ossStorageManager;

    /** LKML 分析报告持久化配置属性类。*/
    private final
    AnalyzeReportPersistenceProperties reportPersistenceProperties;

    /** 内核邮件分类器接口。*/
    private final KernelEmailClassifier kernelEmailClassifier;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** 通过内核补丁邮件标题构建报告文件名。*/
    private String makeReportName(String subject)
    {
        if (!StringUtils.hasText(subject)) {
            return "unknown-report.html";
        }

        final int maxFileNameLen
            = this.reportPersistenceProperties.getMaxFileNameLen();

        // (1) 先替换所有非法字符为下划线
        String finalSubject
            = RegexUtils.AWS_S3_ILLEGAL_CHARACTER
                .matcher(subject).replaceAll("_");

        // (2) 替换空格为连字符
        finalSubject
            = RegexUtils.WHITE_SPACE
                .matcher(finalSubject).replaceAll("-");

        // (3) 替换连续的连字符为单个连字符
        finalSubject
            = RegexUtils.CONTINUOUS_HYPHENS
                .matcher(finalSubject).replaceAll("-");

        // (4) 限制报告名长度并拼接扩展名
        return
        finalSubject.substring(
            0, Math.min(maxFileNameLen, finalSubject.length())
        ) + ".html";
    }

    @Override
    public void
    write(Long kernelEmailId, PlainTextEmail plainTextEmail, String htmlText)
    {
        // (1) 流转邮件的状态为 “开始持久化”
        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_PERSISTING);

        final String from    = plainTextEmail.getFrom();
        final String subject = plainTextEmail.getSubject();
        final String bucketName
            = this.reportPersistenceProperties.getReportBucketName();

        // (2) 拼接报告的完整的对象名
        final Path objectKey
            = this.kernelEmailClassifier.classify(from, subject)
                  .resolve(this.makeReportName(subject));

        try
        {
            // (3) 上传到 OSS 服务
            this.ossStorageManager.uploadObjectByString(
                bucketName,
                objectKey.toString(),
                "text/html; charset=utf-8",
                htmlText
            );

            // (4) 流转邮件状态为 “持久化成功”
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_SUCCESS);

            log.info(
                "Save analyze report {} to bucket {}",
                objectKey, bucketName
            );
        }
        catch (S3Exception s3Exception)
        {
            // 期间出现任何异常则流转状态为 “持久化失败”
            // 并向外传递异常
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.PERSISTENCE_FAILURE);

            log.error(
                "Failed to save report {} to bucket {} (email-id = {})",
                objectKey, bucketName, kernelEmailId,
                s3Exception
            );

            throw s3Exception;
        }
    }
}