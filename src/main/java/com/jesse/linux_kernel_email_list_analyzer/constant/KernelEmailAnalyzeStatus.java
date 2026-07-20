package com.jesse.linux_kernel_email_list_analyzer.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 表示邮件分析任务执行状态的枚举。*/
@Getter
@RequiredArgsConstructor
public enum KernelEmailAnalyzeStatus
{
    /** 未开始（默认值）*/
    NOT_START(0, "Not Start"),

    /** 分析进行中 */
    IN_PROGRESS(1, "In Progress"),

    /** 分析已完成 */
    COMPLETE(2, "Complete");

    @EnumValue
    private final int code;

    private final String desc;
}