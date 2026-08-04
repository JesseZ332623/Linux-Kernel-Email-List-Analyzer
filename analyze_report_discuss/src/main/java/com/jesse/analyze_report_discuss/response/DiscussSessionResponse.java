package com.jesse.analyze_report_discuss.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 查询会话信息响应。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class DiscussSessionResponse
{
    /**
     * 该会话对应的内核邮寄和分析报告，
     * 一封邮件的分析报告下可以发起多次会话讨论（一对多）
     */
    private String sessionId;

    /** 会话标题 */
    private String title;
}