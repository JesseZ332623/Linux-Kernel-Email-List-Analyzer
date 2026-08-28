package com.jesse.analyze_report_discuss.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** 查询会话信息响应。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class DiscussSessionResponse
{
    /**
     * 会话 ID，
     * 一封邮件的分析报告下可以发起多次会话讨论（一对多）
     */
    @Schema(
        description  = "会话 ID",
        example      = "70462e7e-7269-4f03-9f72-81b04ae5c0a4",
        requiredMode = REQUIRED
    )
    private String sessionId;

    /** 会话标题 */
    @Schema(
        description  = "会话标题",
        example      = "Discussion on analyze report Re: [PATCH] crypto: eip93 ...",
        requiredMode = REQUIRED
    )
    private String title;
}