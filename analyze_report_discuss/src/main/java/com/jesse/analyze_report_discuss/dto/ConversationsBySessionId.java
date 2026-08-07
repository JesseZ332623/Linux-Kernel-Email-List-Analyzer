package com.jesse.analyze_report_discuss.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 会话下的一条对话记录。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class ConversationsBySessionId
{
    /** 提出的问题 */
    private String question;

    /** 模型结合上下文给出的回答。*/
    private String content;
}
