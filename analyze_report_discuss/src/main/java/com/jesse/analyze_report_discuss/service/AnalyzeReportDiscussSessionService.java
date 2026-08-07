package com.jesse.analyze_report_discuss.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jesse.analyze_report_discuss.dto.ConversationsBySessionId;
import com.jesse.analyze_report_discuss.response.DeleteDiscussSessionResponse;
import com.jesse.analyze_report_discuss.response.DiscussSessionResponse;
import com.jesse.core.entity.AnalyzeReportDiscussSession;

import java.util.List;
import java.util.UUID;

/** Linux 内核邮件分析报告疑惑解答会话表服务接口。*/
public interface AnalyzeReportDiscussSessionService
    extends IService<AnalyzeReportDiscussSession>
{
    /** 查询一篇分析报告下所有的会话记录。*/
    List<DiscussSessionResponse> getDiscussSessionByTaskId(String taskId);

    /** 查询一个会话下指定页大小时的页数。*/
    long getConversationPagesBySessionId(String sessionId, long pageSize);

    /** 分页查询一个会话下所有的对话记录。*/
    Page<ConversationsBySessionId>
    getConversationsBySessionId(String sessionId, long pageNum, long pageSize);

    /** 在指定分析报告下创建一个新的会话。*/
    UUID createNewDiscussSession(String taskId);

    /** 删除一篇分析报告下指定的会话记录。*/
    DeleteDiscussSessionResponse deleteBySessionId(String taskId, List<String> sessionIds);

    /** 删除一篇分析报告下所有的会话记录。*/
    DeleteDiscussSessionResponse deleteByTaskId(String taskId);
}