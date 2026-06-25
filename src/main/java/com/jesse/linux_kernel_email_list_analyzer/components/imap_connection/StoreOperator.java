package com.jesse.linux_kernel_email_list_analyzer.components.imap_connection;

import jakarta.mail.MessagingException;
import jakarta.mail.Store;

/**
 * 由于 {@link jakarta.mail.Store} 自身有状态的设计，
 * 导致并发的操作这个实例会导致数据竞争，所以最佳时间就是让每一个收件操作
 * 独占 Store 的实例，这种串行的设计避免了数据竞争也符合 jakarta.mail 的设计初衷，
 * 本函数式接口就是 “独占 Store 实例进行收件处理操作” 的操作的抽象。
 */
@FunctionalInterface
public interface StoreOperator <T>
{
    /** 独占 Store 实例进行收件处理操作。*/
    T execute(final Store store) throws MessagingException;
}