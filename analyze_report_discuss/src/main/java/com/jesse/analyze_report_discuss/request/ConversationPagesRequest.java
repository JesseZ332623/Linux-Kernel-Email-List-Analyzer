package com.jesse.analyze_report_discuss.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 会话下指定页大小查询页数的请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ConversationPagesRequest
{
    /** 会话 ID */
    private String sessionId;

    /** 每页条数 */
    private long pageSize;
}