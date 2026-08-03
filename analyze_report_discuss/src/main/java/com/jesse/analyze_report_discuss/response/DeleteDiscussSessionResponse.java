package com.jesse.analyze_report_discuss.response;

import lombok.*;

import java.util.Map;

/** 删除会话记录响应体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDiscussSessionResponse
{
    /** 删除的会话数。*/
    private int deletedSessions;

    /** 删除的所有会话下的对话数量 */
    private int deletedSessionDetails;

    /** 每个会话下的对话数表 */
    private Map<String, Integer> sessionDetailsMap;

    /** 如果一封报告下没有任何会话记录，返回本单例。*/
    public static final
    DeleteDiscussSessionResponse EMPTY_INSTANCE
        = new DeleteDiscussSessionResponse(0, 0, Map.of());
}