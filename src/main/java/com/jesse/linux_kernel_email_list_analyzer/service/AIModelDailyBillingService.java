package com.jesse.linux_kernel_email_list_analyzer.service;

import java.time.LocalDate;

/** AI 模型 token 资费消耗每日汇总表服务类接口。*/
public interface AIModelDailyBillingService
{
    /** 保存每天所有模型 token 资费消耗汇总数据，返回汇总日期。*/
    LocalDate save();
}