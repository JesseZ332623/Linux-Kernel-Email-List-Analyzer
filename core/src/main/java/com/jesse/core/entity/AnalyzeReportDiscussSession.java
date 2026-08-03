package com.jesse.core.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/** Linux 内核邮件分析报告疑惑解答会话表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_analyze_discuss_session")
public class AnalyzeReportDiscussSession
{
    @TableId
    private Long id;

    /** 会话 UUID 字符串 */
    private String sessionId;

    /**
     * 该会话对应的内核邮寄和分析报告，
     * 一封邮件的分析报告下可以发起多次会话讨论（一对多）
     */
    private String taskId;

    /** 会话标题 */
    private String title;

    /** 会话创建时间 */
    private LocalDateTime createAt;
}