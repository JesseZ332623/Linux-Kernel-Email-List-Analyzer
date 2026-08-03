package com.jesse.analyze_report_discuss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jesse.core.entity.AnalyzeReportDiscussSessionDetails;

/** Linux 内核邮件分析报告疑惑解答会话对话内容表服务接口。*/
public interface AnalyzeReportDiscussSessionDetailsService
    extends IService<AnalyzeReportDiscussSessionDetails>
{
    /** 插入一条新的会话明细。*/
    long insertNewSessionDetail(String sessionId, String question);

    /** 将会话下的某个对话记录与大模型回复关联，代表大模型已经回答了这个问题。*/
    int updateModelResponseIdBySessionId(Long id, String sessionId, String modelResponseId);
}