package com.jesse.linux_kernel_email_list_analyzer.components.analyze_report_generator.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.analyze_report_generator.LKMLAnalyzeTemplateGenerator;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailStateMachine;
import com.jesse.linux_kernel_email_list_analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

import java.util.Locale;

/** LKML 内核补丁邮件分析结果生成器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class LKMLAnalyzeTemplateGeneratorImpl implements LKMLAnalyzeTemplateGenerator
{
    /** 模板名常量。*/
    private static final
    String TEMPLATE_NAME = "analyze-result";

    /** Thymeleaf 模板引擎。*/
    private final TemplateEngine templateEngine;

    /** 内核邮件状态机接口。*/
    private final
    KernelEmailStateMachine kernelEmailStateMachine;

    /** 填充模板，返回 HTML 文本字符串。*/
    @Override
    public String generate(AnalyzeResultTemplateData data)
    {
        log.info("Generate report for email: {}", data.getKernelEmail().getMessageId());

        final Long kernelEmailId = data.getKernelEmailId();

        this.kernelEmailStateMachine
            .fireEvent(kernelEmailId, KernelEmailEvents.START_GENERATE);

        try
        {
            final Context context = new Context();

            context.setLocale(Locale.getDefault());
            context.setVariable("data", data);

            final String analyzeReeportContent
                = this.templateEngine.process(TEMPLATE_NAME, context);

            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.GENERATE_SUCCESS);

            return analyzeReeportContent;
        }
        catch (TemplateEngineException exception)
        {
            this.kernelEmailStateMachine
                .fireEvent(kernelEmailId, KernelEmailEvents.GENERATE_FAILURE);

            throw exception;
        }

    }
}