package com.jesse.analyzer.components.kernel_email_pusher;

/** Linux 内核补丁邮件推送器接口。*/
public interface KernelEmailPusher
{
    /**
     * 手动的将邮箱中的未读内核补丁邮件推送到消息队列，
     * 返回推送的邮件数量。
     */
    void push();
}
