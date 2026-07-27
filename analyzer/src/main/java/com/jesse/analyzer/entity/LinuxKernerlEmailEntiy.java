package com.jesse.analyzer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jesse.analyzer.components.state_machine.KernelEmailStatus;
import com.jesse.core.pojo.PlainTextEmail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/** 内核邮件数据表实体类。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@TableName("linux_kernal_email")
public class LinuxKernerlEmailEntiy
{
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 本次大模型请求的唯一标识符，用于追踪和问题排查 */
    private String taskId;

    /** 这份邮件在服务内的状态 */
    private KernelEmailStatus status;

    /** RFC 822 消息 ID，由 发件邮件服务器 生成 */
    private String messageId;

    /** 邮件发送人 */
    @TableField("`from`")
    private String from;

    /** 邮件标题 */
    private String subject;

    /** 邮件发送时间（UTC 时区）*/
    @TableField("`utc_time`")
    private String utcTime;

    /** 邮件发送时间（LKML 常用时区）*/
    private String kernelTime;

    /** 邮件正文（纯文本）*/
    private String textContent;

    /** 乐观锁版本号字段 */
    @Version
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createAt;

    /** 插入一条新内核邮件数据调用本方法构造实体。*/
    public static LinuxKernerlEmailEntiy
    fromPlainTextEmail(Long nextId, PlainTextEmail email)
    {
        final LinuxKernerlEmailEntiy lkml = new LinuxKernerlEmailEntiy();

        lkml.setId(nextId);
        lkml.setTaskId("");

        // 默认状态
        lkml.setStatus(KernelEmailStatus.FETCHED);

        BeanUtils.copyProperties(email, lkml, LinuxKernerlEmailEntiy.class);

        return lkml;
    }
}