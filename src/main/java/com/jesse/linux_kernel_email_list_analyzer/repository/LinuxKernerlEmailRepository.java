package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.LinuxKernerlEmailEntiy;
import org.apache.ibatis.annotations.Mapper;

/** 内核邮件数据表仓储类。*/
@Mapper
public interface LinuxKernerlEmailRepository
    extends BaseMapper<LinuxKernerlEmailEntiy> {}