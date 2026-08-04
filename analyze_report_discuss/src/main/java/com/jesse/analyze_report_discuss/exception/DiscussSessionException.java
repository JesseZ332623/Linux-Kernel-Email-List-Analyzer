package com.jesse.analyze_report_discuss.exception;

import static java.lang.String.format;

/** 讨论会话管理出现的错误抛本异常。*/
public class DiscussSessionException extends RuntimeException
{
    public DiscussSessionException(String message) {
        super(message);
    }

    public static DiscussSessionException make(String taskId)
    {
        return new
        DiscussSessionException(
            format("Create discuss session for %s failed.", taskId)
        );
    }
}
