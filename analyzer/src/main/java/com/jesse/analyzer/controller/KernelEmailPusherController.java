package com.jesse.analyzer.controller;

import com.jesse.analyzer.components.kernel_email_pusher.KernelEmailPusher;
import com.jesse.core.response.CustomizedResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 内核补丁邮件推送控制器类。*/
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/analyzer/kernel-email")
@Tag(name = "内核补丁邮件拉取 -> 推送到消息队列")
public class KernelEmailPusherController
{
    /** 控制器实例响应文档相对路径。*/
    private static final
    String SPRINGDOC_EXAMPLES_PATH = "/springdoc-examples/responses/analyzer/";

    /** Linux 内核补丁邮件推送器接口。*/
    private final KernelEmailPusher kernelEmailPusher;

    /** 手动的推送内核补丁邮件到消息队列。*/
    @PostMapping(path = "/push")
    @Operation(summary = "手动的推送内核补丁邮件到消息队列")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "推送完成的响应（未来会细化）",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "manual-push-200.json"
                )
            )
        )
    })
    public CustomizedResponse<Object>
    manualPush(
        @Parameter(hidden = true)
        final HttpServletResponse response
    )
    {
        this.kernelEmailPusher.push();

        return
        CustomizedResponse.responseOf(
            response,
            HttpStatus.OK,
            "Push kernel email complete, " +
            "please refer to the service log for specific details.",
            null
        );
    }
}