package com.jesse.analyze_report_discuss.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/** 删除选中的会话 ID 请求体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class DeleteSelectedSessionRequest
{
    /** 分析报告 ID */
    private String taskId;

    /** 选中的会话 ID */
    private List<String> sessionIds;
}