package com.jesse.analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.core.enums.KernelEmailStatus;
import com.jesse.analyzer.dto.KernelEmailStatusOnly;
import com.jesse.core.entity.LinuxKernelEmailEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 内核邮件数据表仓储类。*/
@Mapper
public interface LinuxKernelEmailRepository
    extends BaseMapper<LinuxKernelEmailEntity>
{
    /** 修改指定 taskId 的内核邮件的状态。*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            status = #{status}
        WHERE
            task_id = #{taskId}
    """)
    int updateStatusByTaskId(String taskId, KernelEmailStatus status);

    /** 修改指定 id 的内核邮件的状态。*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            status = #{status}
        WHERE
            id = #{id}
    """)
    int updateStatusById(Long id, KernelEmailStatus status);

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

    /** 通过 ID 查询邮件的状态和乐观锁版本号。*/
    @Select("SELECT status, version FROM linux_kernal_email WHERE id = #{id}")
    KernelEmailStatusOnly selectStatusAndVersion(@Param("id") Long id);

    /** 更新指定 ID 对应邮件的状态（手动乐观锁）*/
    @Update("""
        UPDATE
            linux_kernal_email
        SET
            status  = #{newStatus},
            version = version + 1
        WHERE
            id     = #{id}
            AND
            status = #{currentStatus}
            AND
            version = #{version}
    """)
    int updateStatusWithOptimisticLock(
        @Param("id")            Long id,
        @Param("currentStatus") Integer currentStatus,
        @Param("newStatus")     Integer newStatus,
        @Param("version")       Integer version
    );
}