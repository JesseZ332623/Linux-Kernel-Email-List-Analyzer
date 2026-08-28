package com.jesse.response_audit.controller;

import com.jesse.core.response.CustomizedResponse;
import com.jesse.response_audit.service.AIModelDailyBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(path = "/api/ai-model-audit")
@Tag(name = "AI 模型 token 资费消耗每日汇总")
public class AIModelDailyBillingController
{
    /** 控制器实例响应文档相对路径。*/
    private static final
    String SPRINGDOC_EXAMPLES_PATH = "/springdoc-examples/responses/model_response_audit/";

    /** AI 模型 token 资费消耗每日汇总表服务类接口。*/
    private final
    AIModelDailyBillingService aiModelDailyBillingService;

    /** 手动结算每天所有模型 token 资费消耗数据。*/
    @PostMapping(path = "/daily-billing")
    @Operation(summary = "手动结算每天所有模型 token 资费消耗数据")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content      = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "手动结算昨日所有模型的 token 资费消耗",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "manual-save-200.json"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description  = "在结算时重复执行",
            content      = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name  = "在结算时重复执行时的响应",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "manual-save-409.json"
                )
            )
        )
    })
    public CustomizedResponse<Object>
    manualSave(
        @Parameter(hidden = true)
        final HttpServletResponse response
    )
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
    }
}