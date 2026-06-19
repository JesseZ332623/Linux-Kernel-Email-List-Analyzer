package com.jesse.linux_kernel_email_list_analyzer.pojo;

import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import lombok.*;

/** 内核邮件分析模板数据类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResultTemplateData
{
    /** 一封内核补丁邮件。*/
    private PlainTextEmail kernelEmail;

    /** AI 模型回答响应体。*/
    private AIModelAnswerResponse aiModelAnswerResponse;
}