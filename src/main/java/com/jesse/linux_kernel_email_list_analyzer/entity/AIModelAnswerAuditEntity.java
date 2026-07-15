package com.jesse.linux_kernel_email_list_analyzer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/** AI 模型 LKML 分析任务响应审计表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("ai_model_answer_audit")
public class AIModelAnswerAuditEntity
{
    /** 表主键 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 本次大模型请求的唯一标识符，用于追踪和问题排查 */
    private String taskId;

    /** 对象类型，表示这是一个完整的对话生成结果 */
    private String object;

    /** Unix 时间戳，表示响应的生成时间 */
    private Long created;

    /** 实际处理请求的模型名称 */
    private String model;

    /** 后端环境标识，表示处理该请求的具体系统版本 / 配置，调试用 */
    private String systemFingerprint;

    /** 审计记录创建时间 */
    private LocalDateTime createAt;
}