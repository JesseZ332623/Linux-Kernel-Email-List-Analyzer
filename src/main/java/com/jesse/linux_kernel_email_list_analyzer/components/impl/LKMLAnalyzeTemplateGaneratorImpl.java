package com.jesse.linux_kernel_email_list_analyzer.components.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeTemplateGanerator;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/** LKML 内核补丁邮件分析结果生成器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class LKMLAnalyzeTemplateGaneratorImpl implements LKMLAnalyzeTemplateGanerator
{
    /** 模板名常量。*/
    private static final
    String TEMPLATE_NAME = "analyze-result";

    /** Thymeleaf 模板引擎。*/
    private final TemplateEngine templateEngine;

    /** 填充模板，返回 HTML 文本字符串。*/
    @Override
    public String generate(AnalyzeResultTemplateData data)
    {
        log.info("Generate report for email: {}", data.getKernelEmail().getMessageId());

        final Context context = new Context();

        context.setLocale(Locale.getDefault());
        context.setVariable("data", data);

        return
        this.templateEngine.process(TEMPLATE_NAME, context);
    }
}