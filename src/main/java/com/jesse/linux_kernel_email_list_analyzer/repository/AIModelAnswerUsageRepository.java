package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.dto.AIModelAnswerUsageDTO;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerUsageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** AI 模型 LKML 分析任务 Token 消耗明细表仓储类。*/
@Mapper
public interface AIModelAnswerUsageRepository
    extends BaseMapper<AIModelAnswerUsageEntity>
{
    /** 根据大模型请求唯一标识符查询对应的 Token 消耗数据。*/
    @Select("""
        SELECT
            audit.model                          AS model,
            token_usage.prompt_cache_hit_tokens  AS prompt_cache_hit_tokens,
            token_usage.prompt_cache_miss_tokens AS prompt_cache_miss_tokens,
            token_usage.completion_tokens 	     AS completion_tokens
        FROM
            ai_model_answer_audit AS audit
        INNER JOIN
            ai_model_answer_usage AS token_usage
        ON
            audit.task_id = token_usage.task_id
        WHERE
            audit.task_id = #{taskId}
    """)
    AIModelAnswerUsageDTO
    getUsageByTaskId(@Param("taskId") String taskId);

    /** 按模型分组，汇总一天内所有模型的 Token 消耗明细。*/
    @Select("""
        SELECT
            audit.model                               AS model,
            SUM(token_usage.prompt_cache_hit_tokens)  AS promptCacheHitTokens,
            SUM(token_usage.prompt_cache_miss_tokens) AS promptCacheMissTokens,
            SUM(token_usage.completion_tokens)        AS completionTokens
        FROM
            ai_model_answer_audit AS audit
        INNER JOIN
            ai_model_answer_usage AS token_usage
        ON
            audit.task_id = token_usage.task_id
        WHERE
            audit.create_at BETWEEN #{startTime} AND #{endTime}
        GROUP BY
           audit.model
    """)
    List<AIModelAnswerUsageDTO> getDailyUsageGroupByModel(
        @Param("startTime")
        final LocalDateTime startTime,
        @Param("endTime")
        final LocalDateTime endTime
    );
}