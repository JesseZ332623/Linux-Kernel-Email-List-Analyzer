package com.jesse.linux_kernel_email_list_analyzer.response.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** AI 模型回答响应体抽象基类。*/
@Data
@ToString
@EqualsAndHashCode
public abstract class AIModelAnswerBaseResponse
{
    /** 本次请求的唯一标识符，用于追踪和问题排查 */
    private String id;
}