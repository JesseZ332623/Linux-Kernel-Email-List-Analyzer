package com.jesse.analyze_report_discuss.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.core.entity.AnalyzeReportDiscussSessionDetails;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

/** Linux 内核邮件分析报告疑惑解答会话对话内容表仓储类。*/
@Mapper
public interface AnalyzeReportDiscussSessionDetailsRepository
    extends BaseMapper<AnalyzeReportDiscussSessionDetails>
{
    /**
     * 将会话下的某个对话记录与大模型回复关联，
     * 代表大模型已经回答了在这个问题。
     */
    @Update("""
        UPDATE
            ai_analyze_discuss_session_details
        SET
            model_response_id = #{modelResponseId}
        WHERE
            id = #{id}
            AND session_id = #{sessionId}
    """)
    int updateModelResponseIdBySessionId(
        @Param("id")              Long id,
        @Param("sessionId")       String sessionId,
        @Param("modelResponseId") String modelResponseId
    );

    /** 查询某个会话下指定对话的大模型回复内容文本。*/
    @Select("""
        SELECT
            content
        FROM
            ai_analyze_discuss_session_details AS session_details
        INNER JOIN
            ai_model_answer_content AS model_answer
        ON
            session_details.model_response_id = model_answer.task_id
        WHERE
            session_details.session_id = #{sessionId}
            AND
            session_details.model_response_id = #{modelResponseId}
    """)
    Optional<String>
    getModelAnswerContentByModelResponseId(
        @Param("sessionId")       String sessionId,
        @Param("modelResponseId") String modelResponseId
    );

    /** 删除一个对话下的所有对话明细。*/
    @Delete("""
        DELETE FROM
            ai_analyze_discuss_session_details
        WHERE
            session_id = #{sessionId}
    """)
    int deleteBySessionId(@Param("sessionId") String sessionId);
}