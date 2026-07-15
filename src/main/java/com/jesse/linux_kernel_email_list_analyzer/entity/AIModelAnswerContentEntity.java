package com.jesse.linux_kernel_email_list_analyzer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/** AI 模型回复内容表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("ai_model_answer_content")
public class AIModelAnswerContentEntity
{
    /** 表主键 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 本次大模型请求的唯一标识符，用于追踪和问题排查 */
    private String taskId;

    /** AI 推理文本（数据库采用 zlib 算法压缩）*/
    private String reasoningContent;

    /** AI 输出文本（数据库采用 zlib 算法压缩）*/
    private String content;

    /** 数据行创建时间 */
    private LocalDateTime createAt;
}