package com.jesse.analyze_report_discuss.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 分页查询会话下对话记录的请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class PaginateConversationRequest
{
    /** 会话 ID */
    private String sessionId;

    /** 页号 */
    private long pageNo;

    /** 每页条数 */
    private long pageSize;
}
