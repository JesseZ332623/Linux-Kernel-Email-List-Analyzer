package com.jesse.analyze_report_discuss.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 一封完整的内核邮件分析报告。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class KenelEmailAnalyzeReport
{
    /** 内核邮件标题 */
    private String emailSubject;

    /** 邮件文本内容 */
    private String emailContent;

    /** 分析报告文本内容 */
    private String reportContent;
}