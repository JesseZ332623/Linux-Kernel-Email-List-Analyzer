package com.jesse.analyze_report_discuss.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 记录每个任务下的会话数量。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class SessionCountByTaskId
{
    private String taskId;

    private Integer count;
}
