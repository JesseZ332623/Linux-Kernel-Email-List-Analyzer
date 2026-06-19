package com.jesse.linux_kernel_email_list_analyzer.components;

import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;

/** LKML 内核补丁邮件分析结果生成器接口。*/
public interface LKMLAnalyzeTemplateGanerator
{
    /** 填充模板，返回 HTML 文本字符串。*/
    String generate(AnalyzeResultTemplateData data);
}
