package com.jesse.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link KernelEmailStatus} 状态码语义与唯一性测试。*/
class KernelEmailStatusTest
{
    /**
     * 状态码是持久化到 linux_kernel_email.status(TINYINT) 的值，
     * 一旦重排就会让历史数据错乱，所以必须显式锁定 0..12 的取值。
     */
    @Test
    @DisplayName("状态码固定为 0..12 且全局唯一")
    void codesAreFixedFromZeroToTwelveAndUnique()
    {
        final int[] codes = Arrays.stream(KernelEmailStatus.values())
            .mapToInt(KernelEmailStatus::getCode)
            .sorted()
            .toArray();

        assertThat(codes)
            .isEqualTo(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    }

    @Test
    @DisplayName("FETCHED 是初始状态，状态码为 0")
    void fetchedIsTheInitialState()
    {
        assertThat(KernelEmailStatus.FETCHED.getCode()).isZero();
    }

    @Test
    @DisplayName("每个状态都带有非空描述")
    void everyStatusHasNonBlankDescription()
    {
        for (KernelEmailStatus status : KernelEmailStatus.values())
        {
            assertThat(status.getDesc())
                .as("status %s should have a description", status.name())
                .isNotBlank();
        }
    }
}
