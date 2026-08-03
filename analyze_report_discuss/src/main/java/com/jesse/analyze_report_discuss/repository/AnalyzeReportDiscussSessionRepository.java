package com.jesse.analyze_report_discuss.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.analyze_report_discuss.dto.SessionCountByTaskId;
import com.jesse.analyze_report_discuss.response.DiscussSessionResponse;
import com.jesse.core.entity.AnalyzeReportDiscussSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Linux 内核邮件分析报告疑惑解答会话表仓储类。*/
@Mapper
public interface AnalyzeReportDiscussSessionRepository
    extends BaseMapper<AnalyzeReportDiscussSession>
{
    /** 查询一篇分析报告下所有的会话记录。*/
    @Select("""
        SELECT
            session_id, title
        FROM
            ai_analyze_discuss_session
        WHERE
            task_id = #{taskId}
    """)
    List<DiscussSessionResponse>
    getDiscussSessionByTaskId(@Param("taskId") String taskId);

    /** 查询每个任务下的会话数量。*/
    @Select("""
        SELECT
            task_id,
            COUNT(*) AS count
        FROM
            ai_analyze_discuss_session
        GROUP BY
            task_id
    """)
    List<SessionCountByTaskId> getSessionCountByTaskId();

    /** 删除一篇分析报告下指定的会话记录。*/
    int deleteBySessionId(
        @Param("taskId")     String       taskId,
        @Param("sessionIds") List<String> sessionId
    );

    /** 删除一篇分析报告下所有的会话记录。*/
    @Delete("""
        DELETE FROM
            ai_analyze_discuss_session
        WHERE
            task_id = #{taskId}
    """)
    int deleteByTaskId(@Param("taskId") String taskId);
}