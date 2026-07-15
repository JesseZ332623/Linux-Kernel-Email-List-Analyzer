package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.AIModelAnswerContentEntity;
import org.apache.ibatis.annotations.Mapper;

/** AI 模型回答内容表仓储类。*/
@Mapper
public interface AIModelAnswerContentRepository
    extends BaseMapper<AIModelAnswerContentEntity> {}