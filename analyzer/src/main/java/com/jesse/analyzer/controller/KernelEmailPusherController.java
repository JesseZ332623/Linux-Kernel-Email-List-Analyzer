package com.jesse.analyzer.controller;

import com.jesse.analyzer.components.kernel_email_pusher.KernelEmailPusher;
import com.jesse.core.response.CustomizedResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 内核补丁邮件推送控制器类。*/
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/kernel-mail")
public class KernelEmailPusherController
{
    /** Linux 内核补丁邮件推送器接口。*/
    private final KernelEmailPusher kernelEmailPusher;

    /** 手动的推送内核补丁邮件到消息队列。*/
    @PostMapping(path = "/push")
    public CustomizedResponse<Object>
    manualPush(final HttpServletResponse response)
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