package com.jesse.response_audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.core.dto.AIModelAnswerUsageDTO;
import com.jesse.core.entity.AIModelAnswerUsageEntity;
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

    /**
     * 按模型分组，汇总一天内所有模型的 Token 消耗明细。
     *
     * <p><b>2026.07.17 新增</b></p>
     *
     * DeepSeek 模型 7 月中旬开始采用峰谷策略对 Token 进行计费，
     * 峰期为 [09:00, 12:00) 和 [14：00, 18:00)，在此期间双倍收费。
     * 所以需要判断每个明细的时间是否在峰期区间内，再按此分组。
     *
     * <p><b>2026.09.01 新增</b></p>
     *
     * DeepSeek 模型从 8 月 23 日晚 12 点开始，
     * 对峰谷策略计费进行调整，具体如下所示：
     *
     * <ol>
     *     <li>工作日（周一至周五）：继续执行原有峰谷分段计费</li>
     *     <li>周末（周六、周日）：统一按照低谷时段价格收取调整费用</li>
     * </ol>
     */
    @Select("""
        SELECT
            audit.model                               AS model,
            SUM(token_usage.prompt_cache_hit_tokens)  AS promptCacheHitTokens,
            SUM(token_usage.prompt_cache_miss_tokens) AS promptCacheMissTokens,
            SUM(token_usage.completion_tokens)        AS completionTokens,
            CASE WHEN
                DAYOFWEEK(audit.create_at) IN (1, 7)
            THEN FALSE
            WHEN
                (HOUR(audit.create_at) >= 9 AND HOUR(audit.create_at) < 12)
                OR (HOUR(audit.create_at) >= 14 AND HOUR(audit.create_at) < 18)
            THEN TRUE
            ELSE FALSE
            END AS isPeak
        FROM
            ai_model_answer_audit AS audit
        INNER JOIN
            ai_model_answer_usage AS token_usage
        ON
            audit.task_id = token_usage.task_id
        WHERE
            audit.create_at BETWEEN #{startTime} AND #{endTime}
        GROUP BY
           audit.model,
           isPeak
    """)
    List<AIModelAnswerUsageDTO> getDailyUsageGroupByModel(
        @Param("startTime")
        final LocalDateTime startTime,
        @Param("endTime")
        final LocalDateTime endTime
    );
}