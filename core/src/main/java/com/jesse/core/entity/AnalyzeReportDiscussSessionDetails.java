package com.jesse.core.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/** Linux 内核邮件分析报告疑惑解答会话对话内容表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_analyze_discuss_session_details")
public class AnalyzeReportDiscussSessionDetails
{
    @TableId
    private Long id;

    /** 会话 ID */
    private String sessionId;

    /** 对分析报告提出的问题 */
    private String question;

    /** 大模型响应唯一 ID，通过这个 ID 可以查询审计信息 */
    private String modelResponseId;

    /** 创建时间 */
    private LocalDateTime createAt;
}