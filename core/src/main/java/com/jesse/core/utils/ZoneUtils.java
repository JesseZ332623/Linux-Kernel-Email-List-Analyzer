package com.jesse.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

/** 服务时区工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class ZoneUtils
{
    /** UTC 标准时区。*/
    public static final
    ZoneId UTC = ZoneId.of("UTC");

    /** LKML 常用时区（美国太平洋时区 UTC-7 或者 UTC-8）。*/
    public static final
    ZoneId KERNEL_TIMEZONE = ZoneId.of("America/Los_Angeles");

    /** 本地时区（中国上海 UTF+8）*/
    public static final
    ZoneId LOCAL_TIMEZONE = ZoneId.of("Asia/Shanghai");
}
