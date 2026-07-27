package com.jesse.analyzer.components.state_machine;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** 表示一封内核补丁邮件在本服务所有状态的枚举。*/
@Getter
@ToString
@RequiredArgsConstructor
public enum KernelEmailStatus
{
    /** 邮件从邮箱服务被拉取（初始状态）*/
    FETCHED(0, "Fetched by Mail Service"),

    /** 邮件数据已投递到消息队列 */
    PUSHED(1, "Deliver to MQ"),

    /** 邮件数据投递失败 */
    PUSH_FAILED(2, "Deliver to MQ Failed"),

    /** 邮件数据从队列中消费但还未开始分析 */
    ANALYSIS_PENDING(3, "Analyze Not Start"),

    /** 邮件正在分析中 */
    ANALYZING(4, "Analyzing"),

    /** 邮件分析成功 */
    ANALYSIS_SUCCESS(5, "Analyze Success"),

    /** 邮件分析失败 */
    ANALYSIS_FAILED(6, "Analyze Failed"),

    /** 邮件分析报告生成中 */
    GENERATING(7, "Analyze Report Generating"),

    /** 邮件分析报告生成成功 */
    GENERATE_SUCCESS(8, "Analyze Report Generate Success"),

    /** 邮件分析报告生成失败 */
    GENERATE_FAILED(9, "Analyze Report Generate Failed"),

    /** 分析报告持久化中 */
    REPORT_PESISTING(10, "Analyze Report Persisting In"),

    /** 分析报告持久化成功 */
    REPORT_PERSISTENCE_SUCCESS(11, "Analyze Report Persistence Success"),

    /** 分析报告持久化失败 */
    REPORT_PERSISTENCE_FAILED(12, "Analyze Report Persistence Failed");

    /**
     * 状态码，Mybatis-Plus 会取这个值
     * 作为 linux_kernal_email.status 字段的值。
     */
    @EnumValue
    private final int code;

    /** 状态描述文本 */
    private final String desc;
}