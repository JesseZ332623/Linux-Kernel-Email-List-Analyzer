package com.jesse.linux_kernel_email_list_analyzer.controller;

import com.jesse.linux_kernel_email_list_analyzer.response.CustomizedResponse;
import com.jesse.linux_kernel_email_list_analyzer.service.AIModelDailyBillingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static java.lang.String.format;

/** AI 模型 token 资费消耗每日汇总表控制器类。*/
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/ai-model")
public class AIModelDailyBillingController
{
    /** AI 模型 token 资费消耗每日汇总表服务类接口。*/
    private final
    AIModelDailyBillingService aiModelDailyBillingService;

    /** 手动保存每天所有模型 token 资费消耗汇总数据。*/
    @PostMapping(path = "/daily-billing")
    public CustomizedResponse<Object>
    manualSave(final HttpServletResponse response)
    {
        try
        {
            final LocalDate yesterday
                = this.aiModelDailyBillingService.save();

            return
            CustomizedResponse.responseOf(
                response, HttpStatus.OK,
                format(
                    "Save AI model daily bill of token usage complete. " +
                    "(billing date: %s)",
                    yesterday
                ),
                null
            );
        }
        catch (IllegalStateException illegalState)
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.CONFLICT,
                illegalState.getMessage(),
                null
            );
        }
        catch (Exception exception)
        {
            log.error("", exception);

            return
            CustomizedResponse.responseOf(
                response, HttpStatus.INTERNAL_SERVER_ERROR,
                "Save AI model daily bill of token usage failed, please check log file.",
                null
            );
        }
    }
}