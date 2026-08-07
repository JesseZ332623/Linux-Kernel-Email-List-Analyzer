package com.jesse.core.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/** 通用的分页响应体。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class PageResponse <T>
{
    /** 分页数据 */
    private List<T> data;

    /** 页号 */
    private long pageNo;

    /** 页大小 */
    private long pageSize;

    /** 总记录数 */
    private long count;
}
