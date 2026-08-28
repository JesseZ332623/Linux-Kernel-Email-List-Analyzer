package com.jesse.analyze_report_discuss.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** 对某个分析报告发起讨论的请求体。*/
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class ReportDiscussRequest
{
    /** 内核邮件分析报告 ID */
    @Schema(
        description  = "内核邮件分析报告 ID",
        example      = "2082002037282996224",
        requiredMode = REQUIRED
    )
    private String taskId;

    /** 会话 ID */
    @Schema(
        description  = "会话 ID",
        example      = "70462e7e-7269-4f03-9f72-81b04ae5c0a4",
        requiredMode = REQUIRED
    )
    private String sessionId;

    /** 提出的问题 */
    @Schema(
        description  = "提出的问题",
        example      = "在 Linux 内核维护中，使用条件编译为什么是草率的、业余的？",
        requiredMode = REQUIRED
    )
    private String question;

    public String toString()
    {
        return
        String.format(
            "task id: %s | session id: %s | question: %s",
            this.taskId, this.sessionId, this.question
        );
    }
}