package com.jesse.core.config;

import com.jesse.core.properties.EmailReceiverProperties;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/** 邮箱服务会话配置类。*/
@Configuration
@RequiredArgsConstructor
public class EmailSessionConfig
{
    /** 邮箱服务属性配置类。*/
    private final EmailReceiverProperties properties;

    @Bean(name = "gmail-session")
    public Session session()
    {
        final Properties props = new Properties();

        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", properties.getHost());
        props.setProperty("mail.imaps.port", String.valueOf(properties.getPort()));
        props.setProperty("mail.imaps.ssl.enable", String.valueOf(properties.isSsl()));
        props.setProperty("mail.imaps.ssl.protocols", properties.getSslProtocols());
        props.setProperty("mail.imaps.connectiontimeout", String.valueOf(properties.getConnectionTimeout()));
        props.setProperty("mail.imaps.timeout", String.valueOf(properties.getTimeout()));
        props.setProperty("mail.debug", String.valueOf(properties.isDebug()));
        props.setProperty("mail.imaps.proxy.host", properties.getProxyHost());
        props.setProperty("mail.imaps.proxy.port", properties.getProxyPort());

        return Session.getInstance(props);
    }
}