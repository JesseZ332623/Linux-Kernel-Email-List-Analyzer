package com.jesse.linux_kernel_email_list_analyzer.entity;

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
public class AIModelAnswerAudit
{
    @TableId
    private Long id;

    private String taskId;

    private String object;

    private Long created;

    private String model;

    private String reasoningContent;

    private String content;

    private String systemFingerprint;

    private Long promptTokens;

    private Long completionTokens;

    private Long totalTokens;

    private String promptTokensDetails;

    private String completionTokensDetails;

    private Long promptCacheHitTokens;

    private Long promptCacheMissTokens;

    private LocalDateTime createTime;
}