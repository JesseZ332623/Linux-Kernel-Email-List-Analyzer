package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerAudit;
import org.apache.ibatis.annotations.Mapper;

/** AI 模型 LKML 分析任务响应审计表仓库类。*/
@Mapper
public interface AIModelAnswerAuditRepository
    extends BaseMapper<AIModelAnswerAudit> {}