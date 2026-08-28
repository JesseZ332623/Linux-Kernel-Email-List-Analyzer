package com.jesse.analyze_report_discuss.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** 删除选中的会话 ID 请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class DeleteSelectedSessionRequest
{
    /** 分析报告 ID */
    @Schema(
        description  = "内核邮件分析报告 ID",
        example      = "fa97331d-6557-42ca-a222-a2dd31bc6d5e",
        requiredMode = REQUIRED
    )
    private String taskId;

    /** 选中的会话 ID */
    @Schema(
        description = "选中的会话 ID",
        example     = "[70462e7e-7269-4f03-9f72-81b04ae5c0a4, 56c1a52a-95b3-42ce-8b86-fcc81937afc6]",
        requiredMode = REQUIRED
    )
    private List<String> sessionIds;
}