package com.jesse.linux_kernel_email_list_analyzer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/** AI 模型 LKML 分析任务 Token 消耗明细表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("ai_model_answer_usage")
public class AIModelAnswerUsageEntity
{
    /** 表主键 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 本次大模型请求的唯一标识符，用于追踪和问题排查 */
    private String taskId;

    /** 输入提示词消耗的 Token 数 */
    private Long promptTokens;

    /** AI 模型输出消耗的 Token 数（推理消耗 + 最终输出的消耗）*/
    private Long completionTokens;

    /** 总消耗 Token 数 */
    private Long totalTokens;

    /** 输入 Token 的详细构成 JSON */
    private String promptTokensDetails;

    /** 输出 Token 的详细构成 JSON */
    private String completionTokensDetails;

    /** 输入提示词缓存命中 Token 数 */
    private Long promptCacheHitTokens;

    /** 输入提示词缓存未命中 Token 数 */
    private Long promptCacheMissTokens;

    /** 数据行创建时间 */
    private LocalDateTime createAt;
}
