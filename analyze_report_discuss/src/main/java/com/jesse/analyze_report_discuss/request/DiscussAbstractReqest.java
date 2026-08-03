package com.jesse.analyze_report_discuss.request;

import lombok.*;

/** 讨论上下文摘要请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DiscussAbstractReqest
{
    /** 内核邮件分析报告 ID。*/
    private String taskId;

    /** 会话 ID，用于拼接缓存键。*/
    private String sessionId;

    /** 本论对话模型响应 ID，用于查询模型的回复文本。*/
    private String modelResponseId;

    /** 本轮对话提出的问题。*/
    private String question;
}