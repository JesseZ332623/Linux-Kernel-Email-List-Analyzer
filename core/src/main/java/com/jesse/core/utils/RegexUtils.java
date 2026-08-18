package com.jesse.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/** 正则表达式工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class RegexUtils
{
    /** Windows 非法文件名字符正则表达式匹配。*/
    public static final
    Pattern ILLEGAL_CHARACTER_PATTERN
        = Pattern.compile("[<>:\"/\\\\|?*=()]");

    /** AWS S3 标准下的非法文件名字符正则。*/
    public static final
    Pattern AWS_S3_ILLEGAL_CHARACTER
        = Pattern.compile("[\\\\/:*?\"<>|]");

    /** 匹配空格的正则。*/
    public static final
    Pattern WHITE_SPACE = Pattern.compile("\\s+");

    /** 匹配连续连字符的正则。*/
    public static final
    Pattern CONTINUOUS_HYPHENS = Pattern.compile("-{2,}");
}