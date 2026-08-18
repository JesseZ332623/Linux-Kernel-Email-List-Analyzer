package com.jesse.analyze_report_discuss.repository;

import com.jesse.analyze_report_discuss.dto.KernelEmailAnalyzeReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/** 内核邮件分析报告仓储类。*/
@Mapper
public interface KernelEmailAnalyzeReportRepository
{
    /** 通过 taskId 查询对应的内核邮件文本和它的分析报告。*/
    @Select("""
        SELECT
        	lkml.subject 	  AS email_subject,
            lkml.text_content AS email_content,
            report.content	  AS report_content
        FROM
        	linux_kernal_email AS lkml
        INNER JOIN
        	ai_model_answer_content AS report
        ON
        	lkml.task_id = report.task_id
        WHERE
        	lkml.task_id = #{taskId}
    """)
    Optional<KernelEmailAnalyzeReport>
    getReport(@Param("taskId") String taskId);

    /** 查询所有完成了分析的内核邮件的 task_id */
    @Select("SELECT task_id FROM linux_kernal_email WHERE task_id != ''")
    List<String> getAllExistTaskIds();
}