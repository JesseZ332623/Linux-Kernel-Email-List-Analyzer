package com.jesse.analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.analyzer.entity.AIModelAnswerAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 模型 LKML 分析任务响应审计表仓库类。*/
@Mapper
public interface AIModelAnswerAuditRepository
    extends BaseMapper<AIModelAnswerAuditEntity> {}