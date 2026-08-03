package com.jesse.analyzer.dto;

import com.jesse.core.enums.KernelEmailStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 内核邮件服务内状态 DTO。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class KernelEmailStatusOnly
{
    /** 这份邮件在服务内的状态 */
    private KernelEmailStatus status;

    /** 乐观锁版本号字段 */
    private Integer version;
}