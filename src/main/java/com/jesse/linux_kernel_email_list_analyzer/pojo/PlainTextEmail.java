package com.jesse.linux_kernel_email_list_analyzer.pojo;

import lombok.*;

/** 表示一封纯文本邮件的 POJO。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class PlainTextEmail
{
    /**
     * RFC 822 消息 ID，由 发件邮件服务器 生成，
     * 用于去重、引用、追踪，可能的格式示例如下：
     *
     * <pre>
     * # Git send-email 格式
     * <20260616022715.5739-7-acme@kernel.org>
     *          |         |    |  |     └── 域名：kernel.org（组织标识）
     *          |         |    |  └── 用户名：acme
     *          |         |    └── 补丁序号：7（这是系列补丁的第7个）
     *          |         └── 随机数/序列号：5739
     *          └── 时间戳：2026-06-16 02:27:15
     *
     * # PR Tracker Bot 格式
     * <178157943496.413763.1174494062218662923.pr-tracker-bot@kernel.org>
     *          |           |              |              └── 域名：kernel.org
     *          |           |              └── 服务名：pr-tracker-bot（PR 追踪机器人）
     *          |           └── 邮件序号：1174494062218662923（发件服务器生成的唯一标识）
     *          └── 可能是毫秒级时间戳：178157943496（对应 1975-08-24 约 04:19:03，但邮件协议中通常不严格对应实际时间）
     *          └── 也可能是原子递增的序列号（由内核 org 的邮件系统内部生成）
     * </pre>
     */
    private String messageId;

    /** 邮件发送人 */
    private String from;

    /** 邮件标题 */
    private String subject;

    /** 邮件发送时间（UTC 时区）*/
    private String utcTime;

    /** 邮件发送时间（LKML 常用时区）*/
    private String kernelTime;

    /** 邮件正文（纯文本）*/
    private String textContent;
}
