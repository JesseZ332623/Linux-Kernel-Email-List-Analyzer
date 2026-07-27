package com.jesse.analyzer.components.state_machine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** 引发邮件状态流转的事件枚举类。*/
@Getter
@ToString
@RequiredArgsConstructor
public enum KernelEmailEvents
{
    /** 投递成功事件（FETCHED -> PUSHED） */
    PUSH_SUCCESS,

    /** 投递失败事件（FETCHED -> PUSH_FAILED）*/
    PUSH_FAILURE,

    /** 拉取成功事件（PUSHED -> ANALYSIS_PENDING）*/
    PULL_SUCCESS,

    /** 分析开始事件（ANALYSIS_PENDING -> ANALYZING）*/
    START_ANALYSIS,

    /** 分析成功事件（ANALYZING -> ANALYSIS_SUCCESS）*/
    ANALYSIS_SUCCESS,

    /** 分析失败事件（ANALYZING -> ANALYSIS_FAILED）*/
    ANALYSIS_FAILURE,

    /** 邮件分析报告生成开始事件（ANALYSIS_SUCCESS -> GENERATING）*/
    START_GENERATE,

    /** 邮件分析报告生成成功事件（GENERATING -> GENERATE_SUCCESS）*/
    GENERATE_SUCCESS,

    /** 邮件分析报告生成失败事件（GENERATING -> GENERATE_FAILED）*/
    GENERATE_FAILURE,

    /** 邮件分析报告持久化开始事件（GENERATE_SUCCESS -> REPORT_PESISTING）*/
    START_PESISTING,

    /**
     * 邮件分析报告持久化成功事件
     *（REPORT_PESISTING -> REPORT_PERSISTENCE_SUCCESS）
     */
    PERSISTENCE_SUCCESS,

    /**
     * 邮件分析报告持久化失败事件
     *（REPORT_PESISTING -> REPORT_PERSISTENCE_FAILED）
     */
    PERSISTENCE_FAILURE
}