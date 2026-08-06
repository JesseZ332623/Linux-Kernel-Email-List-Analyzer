package com.jesse.analyze_report_discuss.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 对某个分析报告发起讨论的请求体。*/
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class ReportDiscussRequest
{
    /** 内核邮件分析报告 ID */
    private String taskId;

    /** 会话 ID */
    private String sessionId;

    /** 提出的问题 */
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