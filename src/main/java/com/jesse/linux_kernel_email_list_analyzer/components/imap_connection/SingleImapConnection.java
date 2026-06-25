package com.jesse.linux_kernel_email_list_analyzer.components.imap_connection;

import jakarta.mail.MessagingException;

/** 单邮件服务 IMAP 连接实例管理接口。*/
public interface SingleImapConnection
{
    /** 在锁和自动重连保护下执行任意 Store 操作。*/
    <T> T execute(final StoreOperator<T> operation)
        throws MessagingException;
}