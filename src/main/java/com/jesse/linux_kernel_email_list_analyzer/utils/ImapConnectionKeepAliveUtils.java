package com.jesse.linux_kernel_email_list_analyzer.utils;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.iap.Protocol;
import org.eclipse.angus.mail.iap.ProtocolException;
import org.eclipse.angus.mail.iap.Response;
import org.eclipse.angus.mail.imap.IMAPFolder;

import java.util.Arrays;
import java.util.stream.Collectors;

/** IMAP 连接保活工具类。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class ImapConnectionKeepAliveUtils
{
    /** 往邮箱服务发送 NOOP 命令并处理响应操作的实现。*/
    private static Object
    noop(Protocol protocol) throws ProtocolException
    {
        final Response[] responses
            = protocol.command("NOOP", null);

        /*
         * 通知 jakarta.mail 内部注册的的 ResponseHandler，比如：
         *
         * 监听新邮件到达（EXISTS、RECENT 等 untagged 响应）
         *
         * 处理 EXPUNGE（邮件被删除）
         *
         * IDLE 模式下的消息推送
         *
         * 其他内部状态更新
         */
        protocol.notifyResponseHandlers(responses);

        // 处理末尾的结果响应（成功、失败、结束等）
        protocol.handleResult(responses[responses.length - 1]);

        // 拼接响应消息字符串
        return
            Arrays.stream(responses)
                .map(Response::toString)
                .collect(Collectors.joining(" | "));
    }

    /** 邮箱服务连接保活操作的实现。*/
    public static Object
    keepAlive(Store store) throws MessagingException
    {
        final Folder folder = store.getFolder("INBOX");

        if (folder instanceof IMAPFolder imapFolder)
        {
            log.debug(
                "IMAP NOOP keep-alive execute success. (Response message: {})",
                imapFolder.doCommand(ImapConnectionKeepAliveUtils::noop)
            );
        }
        else
        {
            // 如果没有使用 IMAPFolder，
            // 可以用一个轻量级操作代替进行连接保活。
            log.debug(
                "IMAP NOOP keep-alive execute success. (Folder message count: {})",
                folder.getMessageCount()
            );
        }

        return null;
    }
}