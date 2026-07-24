package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** 项目业务时间段配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app")
public class TimePeriodProperties
{
    /** 不同业务场景下的时间段列表。*/
    private Map<String, List<TimeRange>> timePeriods;

    /** 表示一个时间范围。*/
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class TimeRange
    {
        private LocalTime start;
        private LocalTime end;

        /** time 是否在时间范围内？*/
        public boolean contains(LocalTime time)
        {
            // 处理不跨天的情况，比如：[09：00 ~ 12：00)
            if (!this.start.isAfter(end)) {
                return !time.isBefore(start) && time.isBefore(end);
            }
            else {
                // 处理跨天的情况 [start, 24:00) U [00:00, end)
                return !time.isBefore(start) || time.isBefore(end);
            }
        }
    }

    /** 一个时间点是否在某个业务的时间段内。*/
    public boolean isInPeriod(String key, LocalTime time)
    {
        return
        this.timePeriods.getOrDefault(key, List.of())
            .stream()
            .anyMatch((timeRange) -> timeRange.contains(time));
    }
}