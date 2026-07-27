package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 邮箱服务属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailReceiverProperties
{
    /** 邮件服务器域名 */
    private String host;

    /** 邮件服务端口号 */
    private int port;

    /** 是否启用 SSL */
    private boolean ssl;

    /** SSL 传输协议名 */
    private String sslProtocols;

    /** 建立连接的超时时间限制（单位：毫秒）*/
    private long connectionTimeout;

    /** 连接建立后，等待服务器响应的最大时间（单位：毫秒）*/
    private long timeout;

    /** 是否启用调试模式 */
    private boolean debug;

    /** 代理地址 */
    private String proxyHost;

    /** 代理端口号 */
    private String proxyPort;

    /** 连接邮箱服务的用户名 */
    private String username;

    /** 连接邮箱服务的十六位 App 专用密码 */
    private String password;

    /** 各个线程争抢 {@link jakarta.mail.Store} 实例的等待超时时间 */
    private Duration storeLockWaitTimeout;

    /** 连接保活操作时间间隔 */
    private Duration keepAliveInterval;

    /** 连接保活操作线程池关闭超时时间 */
    private Duration keepAliveShutdownWaitTimeout;
}