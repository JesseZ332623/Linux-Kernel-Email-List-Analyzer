package com.jesse.analyzer.pojo;

import com.jesse.analyzer.response.AIModelAnswerResponse;
import com.jesse.core.pojo.PlainTextEmail;
import lombok.*;

/** 内核邮件分析模板数据类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResultTemplateData
{
    /** 内核邮件在数据库中的 ID */
    private Long kernelEmailId;

    /** 一封内核补丁邮件。*/
    private PlainTextEmail kernelEmail;

    /** AI 模型回答响应体。*/
    private AIModelAnswerResponse aiModelAnswerResponse;
}