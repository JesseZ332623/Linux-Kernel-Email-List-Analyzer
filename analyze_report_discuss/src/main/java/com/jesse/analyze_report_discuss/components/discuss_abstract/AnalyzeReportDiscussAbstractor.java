package com.jesse.analyze_report_discuss.components.discuss_abstract;

import com.jesse.analyze_report_discuss.request.DiscussAbstractRequest;

/** 分析报告讨论上下文摘要器接口。*/
public interface AnalyzeReportDiscussAbstractor
{
    /**
     * 一轮问答结束后，
     * 将本轮问答的信息交给轻量级模型做摘要并缓存，作为下一次对话的上下文。
     */
    void discussAbstract(DiscussAbstractRequest request);
}