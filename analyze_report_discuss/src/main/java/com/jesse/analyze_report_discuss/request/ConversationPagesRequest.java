package com.jesse.analyze_report_discuss.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** 会话下指定页大小查询页数的请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ConversationPagesRequest
{
    /** 会话 ID */
    @Schema(
        description  = "会话 ID",
        example      = "70462e7e-7269-4f03-9f72-81b04ae5c0a4",
        requiredMode = REQUIRED
    )
    private String sessionId;

    /** 每页条数 */
    @Schema(
        description  = "每页条数",
        example      = "5",
        requiredMode = REQUIRED
    )
    private long pageSize;
}