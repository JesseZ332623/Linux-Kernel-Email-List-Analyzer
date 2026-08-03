package com.jesse.response_audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.core.entity.AIModelAnswerContentEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 模型回答内容表仓储类。*/
@Mapper
public interface AIModelAnswerContentRepository
    extends BaseMapper<AIModelAnswerContentEntity> {}