package com.jesse.linux_kernel_email_list_analyzer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/** 正则表达式工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class RegexUtils
{
    /** Windows 非法文件名字符正则表达式匹配。*/
    public static final Pattern ILLEGAL_CHARACTOR_PATTERN
        = Pattern.compile("[<>:\"/\\\\|?*=()]");
}