package com.jesse.analyze_report_discuss.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jesse.analyze_report_discuss.repository.AnalyzeReportDiscussSessionDetailsRepository;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionDetailsService;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.entity.AnalyzeReportDiscussSessionDetails;
import com.jesse.core.utils.ZoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Linux 内核邮件分析报告疑惑解答会话对话内容表服务实现类。*/
@Service
@RequiredArgsConstructor
public class AnalyzeReportDiscussSessionDetailsServiceImpl
    extends    ServiceImpl<AnalyzeReportDiscussSessionDetailsRepository, AnalyzeReportDiscussSessionDetails>
    implements AnalyzeReportDiscussSessionDetailsService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 插入一条新的会话明细。*/
    @Override
    public long
    insertNewSessionDetail(String sessionId, String question)
    {
        final long nextId
            = this.globalIdConsumer.nextId();

        final AnalyzeReportDiscussSessionDetails
            newSessionDetails = new AnalyzeReportDiscussSessionDetails();

        newSessionDetails.setId(nextId);
        newSessionDetails.setSessionId(sessionId);
        newSessionDetails.setQuestion(question);
        newSessionDetails.setModelResponseId(null);
        newSessionDetails.setCreateAt(LocalDateTime.now(ZoneUtils.LOCAL_TIMEZONE));

        this.baseMapper.insert(newSessionDetails);

        return nextId;
    }

    /**
     * 将会话下的某个对话记录与大模型回复关联，
     * 代表大模型已经回答了在这个问题。
     */
    @Override
    public int
    updateModelResponseIdBySessionId(Long id, String sessionId, String modelResponseId)
    {
        return
        this.baseMapper
            .updateModelResponseIdBySessionId(id, sessionId, modelResponseId);
    }
}