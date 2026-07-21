package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.constant.KernelEmailAnalyzeStatus;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/** 内核邮件数据表仓储类。*/
@Mapper
public interface LinuxKernerlEmailRepository
    extends BaseMapper<LinuxKernerlEmailEntiy>
{
    /** 修改指定 taskId 的内核邮件的分析任务执行状态。*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            analyze_status = #{status}
        WHERE
            task_id = #{taskId}
    """)
    int updateAnalyzeStatusByTaskId(String taskId, KernelEmailAnalyzeStatus status);

    /** 修改指定 id 的内核邮件的分析任务执行状态。*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            analyze_status = #{status}
        WHERE
            id = #{id}
    """)
    int updateAnalyzeStatusById(Long id, KernelEmailAnalyzeStatus status);

    /** 将指定 id 的邮件与指定的分析任务关联。*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            task_id = #{taskId}
        WHERE
            id = #{id}
    """)
    int updateTaskIdById(Long id, String taskId);
}