package com.jesse.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link ZoneUtils} 服务时区常量测试。*/
class ZoneUtilsTest
{
    @Test
    @DisplayName("暴露 UTC / 内核时区(洛杉矶) / 本地时区(上海) 三个约定时区")
    void exposesTheThreeAgreedZones()
    {
        assertThat(ZoneUtils.UTC).isEqualTo(ZoneId.of("UTC"));
        assertThat(ZoneUtils.KERNEL_TIMEZONE).isEqualTo(ZoneId.of("America/Los_Angeles"));
        assertThat(ZoneUtils.LOCAL_TIMEZONE).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    @DisplayName("三个时区彼此不同")
    void theThreeZonesAreDistinct()
    {
        assertThat(ZoneUtils.UTC).isNotEqualTo(ZoneUtils.KERNEL_TIMEZONE);
        assertThat(ZoneUtils.UTC).isNotEqualTo(ZoneUtils.LOCAL_TIMEZONE);
        assertThat(ZoneUtils.KERNEL_TIMEZONE).isNotEqualTo(ZoneUtils.LOCAL_TIMEZONE);
    }
}
